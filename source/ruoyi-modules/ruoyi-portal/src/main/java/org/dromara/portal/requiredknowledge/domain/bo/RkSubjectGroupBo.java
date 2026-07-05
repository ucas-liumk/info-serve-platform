package org.dromara.portal.requiredknowledge.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.portal.requiredknowledge.domain.RkSubjectGroup;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = RkSubjectGroup.class)
public class RkSubjectGroupBo extends BaseEntity {
    private Long groupId;
    @NotBlank(message = "栏目名称不能为空")
    private String groupName;
    @NotBlank(message = "栏目编码不能为空")
    private String groupCode;
    private String description;
    private Integer orderNum;
    private String status;
    private String remark;
    private String keyword;
}
