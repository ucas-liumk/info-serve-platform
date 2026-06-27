package org.dromara.infoservice.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.infoservice.domain.InfoResourceCategory;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = InfoResourceCategory.class)
public class InfoResourceCategoryBo extends BaseEntity {
    private Long categoryId;
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;
    private String description;
    private String icon;
    private Integer orderNum;
    private String status;
    private String remark;
    private String keyword;
}
