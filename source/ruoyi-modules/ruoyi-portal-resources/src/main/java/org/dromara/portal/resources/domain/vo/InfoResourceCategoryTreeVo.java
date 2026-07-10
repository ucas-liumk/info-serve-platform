package org.dromara.portal.resources.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 门户资料栏目/分类两级树节点。
 * 一级=栏目（children 为其下分类），二级=分类（带分面计数）。
 */
@Data
public class InfoResourceCategoryTreeVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Integer orderNum;
    /** 分面计数（仅分类节点有值：响应关键词+工具条筛选，不含分类维度自身） */
    private Long resourceCount;
    private List<InfoResourceCategoryTreeVo> children;
}
