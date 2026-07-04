package org.dromara.portal.resources.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ResourceUploadVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long ossId;
    private String url;
    private String fileName;
    private String originalName;
    private String fileSuffix;
    private String mimeType;
    private Long fileSize;
    private String previewType;
}
