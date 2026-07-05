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
@TableName("rk_subject_group")
public class RkSubjectGroup extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "group_id")
    private Long groupId;
    private String groupName;
    private String groupCode;
    private String description;
    private Integer orderNum;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
