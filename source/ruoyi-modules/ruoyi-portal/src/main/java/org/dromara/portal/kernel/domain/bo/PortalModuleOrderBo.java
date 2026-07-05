package org.dromara.portal.kernel.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PortalModuleOrderBo {

    @NotEmpty(message = "模块排序不能为空")
    private List<String> moduleCodes;
}
