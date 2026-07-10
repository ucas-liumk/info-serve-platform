package org.dromara.portal.resources.support;

import org.dromara.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 门户列表 categoryCode 多值解析。
 * 逗号分隔 → trim → 去空白 → 去 'all' 哨兵 → 去重（保持出现顺序）。
 * 返回不可变列表；空列表表示"未按分类筛选"。
 */
public final class CategoryCodes {

    /** 与列表筛选一致的"全部"哨兵值（见 InfoResourceServiceImpl#isActiveFilter） */
    private static final String ALL_SENTINEL = "all";

    private CategoryCodes() {
    }

    public static List<String> parse(String categoryCode) {
        if (StringUtils.isBlank(categoryCode)) {
            return List.of();
        }
        return Arrays.stream(categoryCode.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .filter(code -> !ALL_SENTINEL.equals(code))
            .distinct()
            .toList();
    }
}
