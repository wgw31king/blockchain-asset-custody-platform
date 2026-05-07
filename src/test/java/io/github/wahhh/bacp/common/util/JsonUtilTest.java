package io.github.wahhh.bacp.common.util;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonUtilTest {

    @Test
    void roundTripMap() {
        String json = JsonUtil.toJson(Map.of("k", "v"));
        Map<?, ?> back = JsonUtil.fromJson(json, Map.class);
        assertEquals("v", back.get("k"));
    }
}
