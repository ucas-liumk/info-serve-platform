package org.dromara.infoservice.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 门户通知消息 */
@Data
@TableName("app_message")
public class InfoPortalMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "message_id")
    private Long messageId;
    private Long userId;
    private String title;
    private String content;
    private String msgType;
    private String isRead;
    private Date createTime;
}
