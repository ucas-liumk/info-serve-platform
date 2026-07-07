package org.dromara.portal.requiredknowledge.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.requiredknowledge.domain.RkSubjectGroup;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@AutoMapper(target = RkSubjectGroup.class)
public class RkSubjectGroupVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long groupId;
    private String groupName;
    private String groupCode;
    private String description;
    private Integer orderNum;
    private String status;
    private String remark;
    private Date createTime;
    private Long subjectCount;
    private List<RkSubjectVo> subjects;
}
