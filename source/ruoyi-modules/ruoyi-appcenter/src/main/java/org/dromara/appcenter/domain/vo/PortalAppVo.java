package org.dromara.appcenter.domain.vo;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;

@Data
public class PortalAppVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long appId;
    private String appName;
    private String version;
    private Long categoryId;
    private String categoryName;
    private String icon;
    private String accent;
    private String description;
    private String tags;
    private String accessUrl;
    private String isSecurity;
    private Long useCount;
    private Long recommendCount;
    private Boolean favorited;
    private Boolean recommended;
}
