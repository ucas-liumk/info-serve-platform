package org.dromara.portal.resources.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * C2 多值 categoryCode 解析：逗号分隔 → trim → 去空 → 去 'all' 哨兵 → 去重。
 */
class CategoryCodesTest {

    @Test
    void blank_or_null_yields_empty() {
        assertTrue(CategoryCodes.parse(null).isEmpty());
        assertTrue(CategoryCodes.parse("").isEmpty());
        assertTrue(CategoryCodes.parse("   ").isEmpty());
    }

    @Test
    void all_sentinel_yields_empty() {
        assertTrue(CategoryCodes.parse("all").isEmpty());
        assertTrue(CategoryCodes.parse(" all , all ").isEmpty());
    }

    @Test
    void parse_caps_entry_count_and_drops_overlong_codes() {
        String overlong = "x".repeat(81);
        StringBuilder input = new StringBuilder(overlong);
        for (int i = 1; i <= 60; i++) {
            input.append(",c").append(i);
        }
        List<String> parsed = CategoryCodes.parse(input.toString());
        assertEquals(50, parsed.size());
        assertEquals("c1", parsed.get(0));
        assertFalse(parsed.contains(overlong));
    }

    @Test
    void single_value_kept_as_is() {
        assertEquals(List.of("policy"), CategoryCodes.parse("policy"));
    }

    @Test
    void multi_values_trimmed_all_dropped_blank_dropped() {
        assertEquals(List.of("policy", "tech"), CategoryCodes.parse(" policy , all , , tech "));
    }

    @Test
    void duplicates_removed_keeping_first_order() {
        assertEquals(List.of("policy", "tech"), CategoryCodes.parse("policy,tech,policy"));
    }
}
