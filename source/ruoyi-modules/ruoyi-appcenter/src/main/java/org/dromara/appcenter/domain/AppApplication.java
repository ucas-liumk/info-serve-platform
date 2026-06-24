package org.dromara.appcenter.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/** 应用 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_application")
public class AppApplication extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "app_id")
    private Long appId;
    private String appName;
    private String appCode;
    private String version;
    private Long categoryId;
    private String icon;
    private String accent;
    private String description;
    private String tags;
    private String accessUrl;
    private String status;
    private String isSecurity;
    private Long useCount;
    private Long recommendCount;
    private Integer orderNum;
    @TableLogic
    private String delFlag;
}
