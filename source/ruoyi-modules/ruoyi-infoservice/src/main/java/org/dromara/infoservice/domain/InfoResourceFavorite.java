package org.dromara.infoservice.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 资料收藏 */
@Data
@TableName("info_resource_favorite")
public class InfoResourceFavorite implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "id")
    private Long id;
    private Long resourceId;
    private Long userId;
    private String tenantId;
    private Date createTime;
}
