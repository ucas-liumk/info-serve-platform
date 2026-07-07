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
@TableName("info_resource")
public class InfoResource extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "resource_id")
    private Long resourceId;
    private String title;
    private String description;
    private Long categoryId;
    private Long ossId;
    private String originalName;
    private String fileSuffix;
    private String mimeType;
    private Long fileSize;
    private String previewType;
    private Long downloadCount;
    private Long viewCount;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
