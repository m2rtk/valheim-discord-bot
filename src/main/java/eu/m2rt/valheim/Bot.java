package eu.m2rt.valheim;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.util.Objects;

import static net.dv8tion.jda.api.requests.GatewayIntent.DIRECT_MESSAGES;
import static net.dv8tion.jda.api.requests.GatewayIntent.GUILD_MESSAGES;

public final class Bot {

    private final static Logger log = LoggerFactory.getLogger(Bot.class);

    public static void main(String[] args) throws Exception {
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
            throw e;
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
                    .orElseGet(PrettyDiscordServerStatus::statusNotAvailableMessage);
        }
    }
}
