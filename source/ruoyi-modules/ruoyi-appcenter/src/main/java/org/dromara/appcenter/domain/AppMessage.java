package org.dromara.appcenter.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 应用中心消息 */
@Data
@TableName("app_message")
public class AppMessage implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "message_id")
    private Long messageId;
    private Long userId;
    private String title;
    private String content;
    private String msgType;
    private String isRead;
    private Date createTime;
}
