package eu.m2rt.valheim;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static net.dv8tion.jda.api.MessageBuilder.Formatting.BOLD;

public class PrettyDiscordServerStatus {

    private static final CronParser cronParser = new CronParser(
            CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    );

    private final StatusDto server;

    private MessageBuilder builder;

    public PrettyDiscordServerStatus(StatusDto server) {
        this.server = server;
    }

    public static Message statusNotAvailableMessage() {
        return new MessageBuilder()
                .append("Could not connect to server. The server is probably ")
                .append("OFFLINE", BOLD)
                .build();
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
