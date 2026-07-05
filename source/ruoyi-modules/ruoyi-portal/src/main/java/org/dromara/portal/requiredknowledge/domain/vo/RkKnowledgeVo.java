package org.dromara.portal.requiredknowledge.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.requiredknowledge.domain.RkKnowledge;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = RkKnowledge.class)
public class RkKnowledgeVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long knowledgeId;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private String title;
    private String summary;
    private String content;
    private Integer orderNum;
    private String status;
    private String remark;
    private Date createTime;
    private Date updateTime;
}
