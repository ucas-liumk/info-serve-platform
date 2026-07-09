package org.dromara.portal.appcenter.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppPackageUploadVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long packageOssId;
    private String packageName;
    private Long packageSize;
    private String packageUrl;
}
