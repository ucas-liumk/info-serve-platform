package org.dromara.portal.appcenter.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

/** 应用开放范围 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_access_scope")
public class AppAccessScope extends TenantEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "scope_id")
    private Long scopeId;
    private Long appId;
    private String targetType;
    private Long targetId;
}
