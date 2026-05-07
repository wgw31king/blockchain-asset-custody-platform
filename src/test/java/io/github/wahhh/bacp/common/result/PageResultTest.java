package io.github.wahhh.bacp.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @Test
    void ofNullReturnsEmptyShell() {
        PageResult<String> p = PageResult.of(null);
        assertEquals(0, p.getTotal());
        assertEquals(1, p.getCurrent());
        assertTrue(p.getRecords().isEmpty());
    }
}
