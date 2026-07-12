package org.dromara.portal.resources.support;

import org.dromara.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 门户列表 categoryCode 多值解析。
 * 逗号分隔 → trim → 去空白 → 去 'all' 哨兵 → 去超长 → 去重（保持出现顺序）→ 截断到上限。
 * 返回不可变列表；空列表表示"未按分类筛选"。
 */
public final class CategoryCodes {

    /** 与列表筛选一致的"全部"哨兵值（见 InfoResourceServiceImpl#isActiveFilter） */
    private static final String ALL_SENTINEL = "all";

    /** 系统边界防护：条目数上限（超出截断）；单条长度与 category_code varchar(80) 对齐（超长必然无命中，直接丢弃） */
    private static final int MAX_CODES = 50;
    private static final int MAX_CODE_LENGTH = 80;

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
            .filter(code -> code.length() <= MAX_CODE_LENGTH)
            .distinct()
            .limit(MAX_CODES)
            .toList();
    }
}
