package org.dromara.portal.resources.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("info_resource_view_record")
public class InfoResourceViewRecord extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(value = "record_id")
    private Long recordId;
    private Long resourceId;
    private Long userId;
    private String userName;
    private String actionType;
}
