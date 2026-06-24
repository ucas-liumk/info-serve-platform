package org.dromara.appcenter.domain.bo;

import lombok.Data;
import java.io.Serializable;

@Data
public class AppMessageBo implements Serializable {
    private String isRead;   // 过滤:未读/已读,空为全部
    private String msgType;
}
