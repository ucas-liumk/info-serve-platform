package org.dromara.portal.requiredknowledge.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.requiredknowledge.domain.RkSubject;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = RkSubject.class)
public class RkSubjectVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long subjectId;
    private Long groupId;
    private String groupName;
    private String groupCode;
    private String subjectName;
    private String subjectCode;
    private String description;
    private String icon;
    private Integer knowledgeCount;
    private Integer questionCount;
    private Integer examCount;
    private Integer orderNum;
    private String status;
    private String remark;
    private Date createTime;
}
