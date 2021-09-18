package eu.m2rt.valheim;

import com.github.mizosoft.methanol.Methanol;
import com.github.mizosoft.methanol.MoreBodyHandlers;
import com.github.mizosoft.methanol.MutableRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

public final class Huginn {

    private final Logger log = LoggerFactory.getLogger(Huginn.class);

    private final HttpClient client;

    public Huginn(HttpClient client) {
        this.client = client;
    }

    public static Huginn instance(String uri) {
        return new Huginn(
                Methanol.newBuilder()
                        .baseUri(uri)
                        .connectTimeout(Duration.ofSeconds(1))
                        .build()
        );
    }

    public Optional<StatusDto> fetchStatus() {
        try {
            final var response = client.send(
                    MutableRequest.GET("/status"),
                    MoreBodyHandlers.ofObject(StatusDto.class)
            );
            return Optional.of(response.body());
        } catch (IOException e) {
            log.error("Could not get status", e);
            return Optional.empty();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
