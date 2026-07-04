package org.dromara.portal.kernel.service.impl;

import lombok.RequiredArgsConstructor;
import org.dromara.portal.appcenter.service.IAppApplicationService;
import org.dromara.portal.forum.service.IInfoForumService;
import org.dromara.portal.kernel.domain.vo.PortalStatsVo;
import org.dromara.portal.kernel.service.IInfoPortalService;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.springframework.stereotype.Service;

/**
 * 门户首页统计聚合：只依赖各 BC 服务接口（边界纪律由 ArchUnit 强制）。
 */
@RequiredArgsConstructor
@Service
public class InfoPortalServiceImpl implements IInfoPortalService {

    private final IInfoResourceService resourceService;
    private final IInfoForumService forumService;
    private final IAppApplicationService appApplicationService;

    @Override
    public PortalStatsVo stats() {
        PortalStatsVo vo = new PortalStatsVo();
        vo.setResourceCount(defaultLong(resourceService.countPortalVisible()));
        vo.setToolCount(defaultLong(appApplicationService.countPortalVisible()));
        vo.setTopicCount(defaultLong(forumService.countPortalVisibleTopics()));
        vo.setActiveUserCount(defaultLong(forumService.countActiveAuthors()));
        vo.setTodayVisitCount(defaultLong(resourceService.sumPortalVisits()));
        return vo;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
