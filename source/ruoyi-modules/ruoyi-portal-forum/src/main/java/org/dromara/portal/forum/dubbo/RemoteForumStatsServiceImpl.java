package org.dromara.portal.forum.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.portal.api.PortalStatsMetrics;
import org.dromara.portal.api.RemoteModuleStatsService;
import org.dromara.portal.api.domain.vo.RemoteModuleStatsVo;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.forum.service.IInfoForumService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
@DubboService(group = PortalEventConstants.STATS_GROUP_FORUM)
public class RemoteForumStatsServiceImpl implements RemoteModuleStatsService {

    private final IInfoForumService forumService;

    @Override
    public RemoteModuleStatsVo stats() {
        RemoteModuleStatsVo vo = new RemoteModuleStatsVo();
        vo.setModuleCode(PortalEventConstants.STATS_GROUP_FORUM);
        Map<String, Long> metrics = new HashMap<>();
        metrics.put(PortalStatsMetrics.TOPIC_COUNT, nvl(forumService.countPortalVisibleTopics()));
        metrics.put(PortalStatsMetrics.ACTIVE_AUTHOR_COUNT, nvl(forumService.countActiveAuthors()));
        vo.setMetrics(metrics);
        return vo;
    }

    private Long nvl(Long value) {
        return value == null ? 0L : value;
    }
}
