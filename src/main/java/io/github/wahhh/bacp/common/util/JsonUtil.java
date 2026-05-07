package io.github.wahhh.bacp.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Shared Jackson {@link ObjectMapper} for JSON serialization.
 */
public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModule(new JavaTimeModule());
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private JsonUtil() {
    }

    /**
     * Returns the shared mapper instance.
     *
     * @return configured mapper
     */
    public static ObjectMapper mapper() {
        return MAPPER;
    }

    /**
     * Serializes object to JSON string.
     *
     * @param obj value
     * @return JSON text
     */
    public static String toJson(Object obj) {
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON serialize failed", ex);
        }
    }

    /**
     * Deserializes JSON string to Java type.
     *
     * @param json JSON text
     * @param type target class
     * @param <T>  type parameter
     * @return parsed object
     */
    public static <T> T fromJson(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON deserialize failed", ex);
        }
    }

    /**
     * Deserializes JSON using {@link TypeReference} (generics).
     *
     * @param json JSON text
     * @param ref  type reference
     * @param <T>  type parameter
     * @return parsed object
     */
    public static <T> T fromJson(String json, TypeReference<T> ref) {
        try {
            return MAPPER.readValue(json, ref);
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON deserialize failed", ex);
        }
    }
}
