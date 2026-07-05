package org.dromara.portal.resources.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("info_resource_note")
public class InfoResourceNote extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(value = "note_id")
    private Long noteId;
    private Long resourceId;
    private Long userId;
    private String authorName;
    private String content;
    private String visibility;
    @TableLogic
    private String delFlag;
    private String remark;
}
