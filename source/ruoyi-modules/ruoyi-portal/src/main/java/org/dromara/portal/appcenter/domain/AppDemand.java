package org.dromara.portal.appcenter.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;
import java.util.Date;

/** 应用需求反馈 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_demand")
public class AppDemand extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "demand_id")
    private Long demandId;
    private String demandType;
    private Long appId;
    private String appName;
    private String referenceUrl;
    private String content;
    private String contact;
    private Long requesterId;
    private String requesterName;
    private String status;
    private String handleRemark;
    private Long handledBy;
    private Date handledTime;
    @TableLogic
    private String delFlag;
}
