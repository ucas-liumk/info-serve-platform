package org.dromara.portal.appcenter.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AppDemandSubmitBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "需求类型不能为空")
    private String demandType;

    private Long appId;

    @Size(max = 100, message = "应用名称不能超过100字")
    private String appName;

    @Size(max = 500, message = "参考地址不能超过500字")
    private String referenceUrl;

    @NotBlank(message = "需求说明不能为空")
    @Size(max = 2000, message = "需求说明不能超过2000字")
    private String content;

    @Size(max = 100, message = "联系方式不能超过100字")
    private String contact;
}
