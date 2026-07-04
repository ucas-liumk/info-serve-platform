package org.dromara.portal.kernel.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 门户模块注册表：模块启停/排序/权限可见性由后台配置。
 * 统计口径不入表（由各 BC 契约按 moduleCode 约定提供）。
 */
@Data
@TableName("portal_module")
public class PortalModule implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(value = "module_id")
    private Long moduleId;

    /** 唯一编码: resources/appcenter/forum/qa/news */
    private String moduleCode;

    /** 展示名: 资料共享/工具即用/… */
    private String moduleName;

    /** 卡片副标题 */
    private String description;

    /** 卡片配图(OSS 地址) */
    private String image;

    /** 入口路由: /portal/resources */
    private String entryPath;

    /** 权限码，NULL=登录即可见 */
    private String perms;

    /** 0启用 / 1敬请期待 / 2隐藏 */
    private String status;

    private Integer sortOrder;

    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
