package org.dromara.infoservice.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 门户通知接收人 */
@Data
@TableName("sys_user")
public class InfoPortalUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id")
    private Long userId;
    private String status;
    private String delFlag;
}
