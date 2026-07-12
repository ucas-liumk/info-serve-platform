package org.dromara.portal.resources.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 资料按分类聚合计数结果（单条 GROUP BY category_id）。
 */
@Data
public class InfoResourceCategoryCountVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long categoryId;
    private Long resourceCount;
}
