package org.dromara.portal.requiredknowledge.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.portal.requiredknowledge.domain.RkKnowledge;

@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = RkKnowledge.class)
public class RkKnowledgeBo extends BaseEntity {
    private Long knowledgeId;
    @NotNull(message = "所属科目不能为空")
    private Long subjectId;
    @NotBlank(message = "知识点标题不能为空")
    @Size(max = 160, message = "知识点标题不能超过160字")
    private String title;
    @Size(max = 500, message = "知识点摘要不能超过500字")
    private String summary;
    @NotBlank(message = "知识点正文不能为空")
    private String content;
    private Integer orderNum;
    private String status;
    private String remark;
    private String keyword;
    private String subjectCode;
}
