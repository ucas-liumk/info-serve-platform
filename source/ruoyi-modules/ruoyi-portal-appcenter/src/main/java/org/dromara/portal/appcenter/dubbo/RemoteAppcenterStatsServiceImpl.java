package org.dromara.portal.appcenter.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.portal.api.PortalStatsMetrics;
import org.dromara.portal.api.RemoteModuleStatsService;
import org.dromara.portal.api.domain.vo.RemoteModuleStatsVo;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.appcenter.service.IAppApplicationService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@DubboService(group = PortalEventConstants.STATS_GROUP_APPCENTER)
public class RemoteAppcenterStatsServiceImpl implements RemoteModuleStatsService {

    private final IAppApplicationService applicationService;

    @Override
    public RemoteModuleStatsVo stats() {
        RemoteModuleStatsVo vo = new RemoteModuleStatsVo();
        vo.setModuleCode(PortalEventConstants.STATS_GROUP_APPCENTER);
        Map<String, Long> metrics = new HashMap<>();
        metrics.put(PortalStatsMetrics.APP_COUNT, nvl(applicationService.countPortalVisible()));
        vo.setMetrics(metrics);
        return vo;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
