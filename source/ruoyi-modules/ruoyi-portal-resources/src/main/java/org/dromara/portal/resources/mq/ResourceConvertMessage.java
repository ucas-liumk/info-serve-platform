package org.dromara.portal.resources.mq;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ResourceConvertMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String messageId;
    private Long resourceId;
}
