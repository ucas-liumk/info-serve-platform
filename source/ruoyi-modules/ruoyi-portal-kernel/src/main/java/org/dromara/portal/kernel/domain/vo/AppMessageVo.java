package org.dromara.portal.kernel.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class AppMessageVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long messageId;
    private String title;
    private String content;
    private String msgType;
    private String isRead;
    private Date createTime;
}
