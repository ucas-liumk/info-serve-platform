package org.dromara.appcenter.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.appcenter.domain.AppMessage;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = AppMessage.class)
public class AppMessageVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long messageId;
    private String title;
    private String content;
    private String msgType;
    private String isRead;
    private Date createTime;
}
