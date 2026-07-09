package org.dromara.portal.kernel.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.portal.api.PortalStatsMetrics;
import org.dromara.portal.api.RemoteModuleStatsService;
import org.dromara.portal.api.domain.vo.RemoteModuleStatsVo;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.kernel.domain.vo.PortalStatsVo;
import org.dromara.portal.kernel.service.IInfoPortalService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 门户首页统计聚合：经 Dubbo 调各模块统计接口，模块不可用时该模块指标降级为 0，
 * 绝不让单模块故障拖垮首页（spec §6.1）。
 */
@Slf4j
@Service
public class InfoPortalServiceImpl implements IInfoPortalService {

    @DubboReference(group = PortalEventConstants.STATS_GROUP_APPCENTER, timeout = 2000, check = false)
    private RemoteModuleStatsService appcenterStats;

    @DubboReference(group = PortalEventConstants.STATS_GROUP_FORUM, timeout = 2000, check = false)
    private RemoteModuleStatsService forumStats;

    @DubboReference(group = PortalEventConstants.STATS_GROUP_RESOURCES, timeout = 2000, check = false)
    private RemoteModuleStatsService resourcesStats;

    @Override
    public PortalStatsVo stats() {
        Map<String, Long> app = safeMetrics(() -> appcenterStats.stats(), "appcenter");
        Map<String, Long> forum = safeMetrics(() -> forumStats.stats(), "forum");
        Map<String, Long> res = safeMetrics(() -> resourcesStats.stats(), "resources");
        PortalStatsVo vo = new PortalStatsVo();
        vo.setToolCount(metric(app, PortalStatsMetrics.APP_COUNT));
        vo.setTopicCount(metric(forum, PortalStatsMetrics.TOPIC_COUNT));
        vo.setActiveUserCount(metric(forum, PortalStatsMetrics.ACTIVE_AUTHOR_COUNT));
        vo.setResourceCount(metric(res, PortalStatsMetrics.RESOURCE_COUNT));
        vo.setTodayVisitCount(metric(res, PortalStatsMetrics.VISIT_COUNT));
        return vo;
    }

    private Map<String, Long> safeMetrics(Supplier<RemoteModuleStatsVo> call, String module) {
        try {
            RemoteModuleStatsVo vo = call.get();
            return vo == null || vo.getMetrics() == null ? Collections.emptyMap() : vo.getMetrics();
        } catch (Exception e) {
            log.warn("模块统计不可用，降级为 0：module={} reason={}", module, e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Long metric(Map<String, Long> metrics, String key) {
        return metrics.getOrDefault(key, 0L);
    }
}
