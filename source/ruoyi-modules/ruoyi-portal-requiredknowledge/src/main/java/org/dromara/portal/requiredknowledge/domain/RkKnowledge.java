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
@TableName("rk_knowledge")
public class RkKnowledge extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "knowledge_id")
    private Long knowledgeId;
    private Long subjectId;
    private String title;
    private String summary;
    private String content;
    private Integer orderNum;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
