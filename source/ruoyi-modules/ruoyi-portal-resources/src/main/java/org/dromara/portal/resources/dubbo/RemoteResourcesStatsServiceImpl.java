package org.dromara.portal.resources.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.portal.api.PortalStatsMetrics;
import org.dromara.portal.api.RemoteModuleStatsService;
import org.dromara.portal.api.domain.vo.RemoteModuleStatsVo;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@DubboService(group = PortalEventConstants.STATS_GROUP_RESOURCES)
public class RemoteResourcesStatsServiceImpl implements RemoteModuleStatsService {

    private final IInfoResourceService resourceService;

    @Override
    public RemoteModuleStatsVo stats() {
        RemoteModuleStatsVo vo = new RemoteModuleStatsVo();
        vo.setModuleCode(PortalEventConstants.STATS_GROUP_RESOURCES);
        Map<String, Long> metrics = new HashMap<>();
        metrics.put(PortalStatsMetrics.RESOURCE_COUNT, nvl(resourceService.countPortalVisible()));
        metrics.put(PortalStatsMetrics.VISIT_COUNT, nvl(resourceService.sumPortalVisits()));
        vo.setMetrics(metrics);
        return vo;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
