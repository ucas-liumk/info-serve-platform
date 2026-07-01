package org.dromara.infoservice.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.infoservice.domain.InfoResource;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InfoResource.class)
public class InfoResourceVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long resourceId;
    private String title;
    private String description;
    private Long categoryId;
    private String categoryName;
    private Long ossId;
    private String originalName;
    private String fileSuffix;
    private String mimeType;
    private Long fileSize;
    private String previewType;
    private Long downloadCount;
    private Long viewCount;
    private Long favoriteCount;
    private Boolean favorited;
    private String status;
    private String remark;
    private Long createBy;
    private String ownerName;
    private Boolean canManage;
    private Date createTime;
    private Date updateTime;
}
