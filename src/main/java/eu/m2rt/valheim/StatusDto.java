package eu.m2rt.valheim;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record StatusDto(String name,
                        String version,
                        Integer players,
                        @JsonProperty("max_players") Integer maxPlayers,
                        String map,
                        Boolean online,
                        BepinexDto bepinex,
                        List<JobDto> jobs) {

    public record BepinexDto(Boolean enabled,
                             List<ModDto> mods) {}

    public record ModDto(String name,
                         String location) {}

    public record JobDto(String name,
                         Boolean enabled,
                         String schedule) {}
}
