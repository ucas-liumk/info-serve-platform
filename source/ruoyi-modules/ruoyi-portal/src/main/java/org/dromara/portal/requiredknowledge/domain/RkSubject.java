package org.dromara.portal.requiredknowledge.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rk_subject")
public class RkSubject extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "subject_id")
    private Long subjectId;
    private Long groupId;
    private String subjectName;
    private String subjectCode;
    private String description;
    private String icon;
    private Integer knowledgeCount;
    private Integer questionCount;
    private Integer examCount;
    private Integer orderNum;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
