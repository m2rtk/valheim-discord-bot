package eu.m2rt.valheim;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mizosoft.methanol.adapter.ForwardingDecoder;
import com.github.mizosoft.methanol.adapter.ForwardingEncoder;
import com.github.mizosoft.methanol.adapter.jackson.JacksonAdapterFactory;

/**
 * https://mizosoft.github.io/methanol/adapters/jackson/
 */
public final class JacksonAdapters {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static class JacksonEncoder extends ForwardingEncoder {
        public JacksonEncoder() {
            super(JacksonAdapterFactory.createEncoder(mapper));
        }
    }

    public static class JacksonDecoder extends ForwardingDecoder {
        public JacksonDecoder() {
            super(JacksonAdapterFactory.createDecoder(mapper));
        }
    }
}
