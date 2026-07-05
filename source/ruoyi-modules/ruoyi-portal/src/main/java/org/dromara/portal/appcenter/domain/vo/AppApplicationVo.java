package org.dromara.portal.appcenter.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
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
    private String appType;
    private Long packageOssId;
    private String packageName;
    private Long packageSize;
    private String packageUrl;
    private String status;
    private String isSecurity;
    private String accessMode;
    private List<Long> roleIds;
    private List<Long> userIds;
    private Long useCount;
    private Long recommendCount;
    private Integer orderNum;
    private String remark;
    private Date createTime;
}
