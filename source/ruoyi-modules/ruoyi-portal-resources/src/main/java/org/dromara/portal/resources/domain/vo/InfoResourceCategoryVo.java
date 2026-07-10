package org.dromara.portal.resources.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.resources.domain.InfoResourceCategory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InfoResourceCategory.class)
public class InfoResourceCategoryVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long categoryId;
    /** 父栏目ID（NULL=栏目，非空=分类，两级封顶） */
    private Long parentId;
    private String categoryName;
    private String categoryCode;
    private String description;
    private String icon;
    private Integer orderNum;
    private String status;
    private String remark;
    private Date createTime;
    private Long resourceCount;
}
