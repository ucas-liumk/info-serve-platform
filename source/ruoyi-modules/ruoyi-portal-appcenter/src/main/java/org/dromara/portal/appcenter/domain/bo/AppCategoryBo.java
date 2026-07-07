package org.dromara.portal.appcenter.domain.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppCategoryBo extends BaseEntity {
    private Long categoryId;
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;
    private String icon;
    private Integer orderNum;
    private String status;
    private String remark;
}
