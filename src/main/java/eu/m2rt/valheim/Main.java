package eu.m2rt.valheim;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static net.dv8tion.jda.api.MessageBuilder.Formatting.BOLD;
import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;

public final class Main {

    private final static Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting up");

        final var token = Objects.requireNonNull(System.getenv("BOT_TOKEN"), "BOT_TOKEN env variable must be set");
        final var huginnUrl = Objects.requireNonNull(System.getenv("HUGINN_URL"), "HUGINN_URL env variable must be set");
        log.info("Loaded BOT_TOKEN={}", token);
        log.info("Loaded HUGINN_URL={}", huginnUrl);

        final var huginn = Huginn.instance(huginnUrl);

        try {
            JDABuilder
                    .createLight(token)
                    .addEventListeners(new HuginnStatusListener(huginn))
                    .addEventListeners(new GuildListener())
                    .setEnabledIntents(DIRECT_MESSAGES, GUILD_MESSAGES)
                    .build();
        } catch (LoginException e) {
            log.error("Fatal: {}", e.getMessage(), e);
        }
    }

    public static class GuildListener extends ListenerAdapter {
        @Override
        public void onGuildReady(@NotNull GuildReadyEvent event) {
            event.getGuild().upsertCommand("valheim-status", "Gets the status of the valheim server").queue();
        }
    }

    public static class HuginnStatusListener extends ListenerAdapter {

        private final Huginn huginn;

        public HuginnStatusListener(Huginn huginn) {
            this.huginn = huginn;
        }

        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) {
                return;
            }

            if (!event.getMessage().isMentioned(event.getJDA().getSelfUser())) {
                return;
            }

            if (!event.getMessage().getContentStripped().toLowerCase().contains("status")) {
                event.getMessage().reply("Beep Boop I'm a bot and I'm kinda dumb xD. Try asking me for server status :)").submit();
                return;
            }

           event.getMessage().reply(statusMessage()).submit();
        }

        @Override
        public void onSlashCommand(@NotNull SlashCommandEvent event) {
            if (event.getName().equals("valheim-status")) {
                event.reply(statusMessage()).queue();
            } else {
                event.reply("What?").setEphemeral(true).queue();
            }
        }

        private Message statusMessage() {
            return huginn.fetchStatus()
                    .map(PrettyDiscordServerStatus::new)
                    .map(PrettyDiscordServerStatus::toMessage)
                    .orElse(new MessageBuilder().append("Could not connect to server. The server is ").append("OFFLINE", BOLD).append("!").build());
        }
    }

    public static class PrettyDiscordServerStatus {

        private static final CronParser cronParser = new CronParser(
                CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
        );

        private final StatusDto server;

        private MessageBuilder builder;

        public PrettyDiscordServerStatus(StatusDto server) {
            this.server = server;
        }

        public Message toMessage() {
            this.builder = new MessageBuilder();
            addStatusLine();
            addVersionLine();
            installedMods().ifPresent(this::addInstalledModsLine);
            addActivePlayersLine();
            builder.append('\n');
            autoUpdateJob().ifPresent(this::addAutoUpdateLine);
            autoBackupJob().ifPresent(this::addAutoBackupLine);
            return builder.build();
        }

        private void addStatusLine() {
            builder.append(server.name(), BOLD)
                    .append(" is ")
                    .append(server.online() ? "ONLINE" : "OFFLINE", BOLD)
                    .append('\n');
        }

        private void addVersionLine() {
            builder.append("Version: ").append(server.version(), BOLD).append('\n');
        }

        private Optional<List<StatusDto.ModDto>> installedMods() {
            if (server.bepinex().enabled() && !server.bepinex().mods().isEmpty()) {
                return Optional.of(server.bepinex().mods());
            } else {
                return Optional.empty();
            }
        }

        private void addInstalledModsLine(List<StatusDto.ModDto> mods) {
            builder.append("Installed mods: ");
            for (int i = 0; i < mods.size(); i++) {
                if (i != 0 && i != mods.size() - 1) {
                    builder.append(", ");
                }

                final var mod = mods.get(i);

                builder.append(mod.name().replace(".dll", ""), BOLD);
            }
            builder.append('\n');
        }

        private void addActivePlayersLine() {
            builder.append("There are currently ").append(server.players().toString(), BOLD).append(" active players").append('\n');
        }

        private Optional<StatusDto.JobDto> autoUpdateJob() {
            return server.jobs().stream().filter(StatusDto.JobDto::enabled).filter(job -> job.name().equals("AUTO_UPDATE")).findFirst();
        }

        private void addAutoUpdateLine(StatusDto.JobDto job) {
            ExecutionTime.forCron(cronParser.parse(job.schedule()))
                    .timeToNextExecution(ZonedDateTime.now())
                    .map(PrettyDiscordServerStatus::humanReadableFormat)
                    .ifPresent(timeToNextExecution -> builder.append("Time to next server update check: ").append(timeToNextExecution).append('\n'));
        }

        private Optional<StatusDto.JobDto> autoBackupJob() {
            return server.jobs().stream().filter(StatusDto.JobDto::enabled).filter(job -> job.name().equals("AUTO_BACKUP")).findFirst();
        }

        private void addAutoBackupLine(StatusDto.JobDto job) {
            ExecutionTime.forCron(cronParser.parse(job.schedule()))
                    .timeToNextExecution(ZonedDateTime.now())
                    .map(PrettyDiscordServerStatus::humanReadableFormat)
                    .ifPresent(timeToNextExecution -> builder.append("Time to next server backup: ").append(timeToNextExecution).append('\n'));
        }

        private static String humanReadableFormat(Duration duration) {
            return duration
                    .truncatedTo(ChronoUnit.SECONDS)
                    .toString()
                    .substring(2)
                    .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                    .toLowerCase();
        }
    }
}
