package org.dromara.appcenter.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
public class AppDemandVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long demandId;
    private String demandType;
    private Long appId;
    private String appName;
    private String referenceUrl;
    private String content;
    private String contact;
    private Long requesterId;
    private String requesterName;
    private String status;
    private String handleRemark;
    private Long handledBy;
    private Date handledTime;
    private Date createTime;
    private Date updateTime;
}
