package org.dromara.appcenter.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class AppDemandBo extends BaseEntity {
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
    private String keyword;
}
