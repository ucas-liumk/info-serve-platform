package org.dromara.appcenter.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.appcenter.domain.AppApplication;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = AppApplication.class)
public class AppApplicationVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long appId;
    private String appName;
    private String appCode;
    private String version;
    private Long categoryId;
    private String icon;
    private String accent;
    private String description;
    private String tags;
    private String accessUrl;
    private String status;
    private String isSecurity;
    private Long useCount;
    private Long recommendCount;
    private Integer orderNum;
    private String remark;
    private Date createTime;
}
