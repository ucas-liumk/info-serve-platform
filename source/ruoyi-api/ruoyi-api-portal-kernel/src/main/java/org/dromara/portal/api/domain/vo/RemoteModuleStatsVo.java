package org.dromara.portal.api.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

@Data
public class RemoteModuleStatsVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 模块编码：appcenter / forum / resources */
    private String moduleCode;

    /** 指标键 → 数值，键见 PortalStatsMetrics */
    private Map<String, Long> metrics;
}
