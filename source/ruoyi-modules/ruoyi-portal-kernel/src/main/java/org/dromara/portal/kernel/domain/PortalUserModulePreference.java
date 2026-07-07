package org.dromara.portal.kernel.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 当前用户的门户首页模块排序偏好。
 */
@Data
@TableName("portal_user_module_preference")
public class PortalUserModulePreference implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(value = "preference_id")
    private Long preferenceId;

    private Long userId;

    private String tenantId;

    private String moduleCode;

    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;
}
