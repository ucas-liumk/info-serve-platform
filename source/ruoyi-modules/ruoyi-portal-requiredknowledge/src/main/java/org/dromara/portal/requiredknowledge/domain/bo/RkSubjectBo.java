package org.dromara.portal.requiredknowledge.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.portal.requiredknowledge.domain.RkSubject;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = RkSubject.class)
public class RkSubjectBo extends BaseEntity {
    private Long subjectId;
    @NotNull(message = "所属栏目不能为空")
    private Long groupId;
    @NotBlank(message = "科目名称不能为空")
    private String subjectName;
    @NotBlank(message = "科目编码不能为空")
    private String subjectCode;
    private String description;
    private String icon;
    private Integer knowledgeCount;
    private Integer questionCount;
    private Integer examCount;
    private Integer orderNum;
    private String status;
    private String remark;
    private String keyword;
}
