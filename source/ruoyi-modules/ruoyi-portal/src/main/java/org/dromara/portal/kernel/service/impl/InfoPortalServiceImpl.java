package org.dromara.portal.kernel.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.portal.appcenter.service.IAppApplicationService;
import org.dromara.portal.forum.domain.InfoForumTopic;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.kernel.domain.vo.PortalStatsVo;
import org.dromara.portal.forum.mapper.InfoForumTopicMapper;
import org.dromara.portal.kernel.mapper.InfoPortalMapper;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.portal.kernel.service.IInfoPortalService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InfoPortalServiceImpl implements IInfoPortalService {

    private final InfoResourceMapper resourceMapper;
    private final InfoForumTopicMapper topicMapper;
    private final InfoPortalMapper portalMapper;
    private final IAppApplicationService appApplicationService;

    @Override
    public PortalStatsVo stats() {
        PortalStatsVo vo = new PortalStatsVo();
        vo.setResourceCount(resourceMapper.selectCount(Wrappers.<InfoResource>lambdaQuery().eq(InfoResource::getStatus, "0")));
        vo.setToolCount(defaultLong(appApplicationService.countPortalVisible()));
        vo.setTopicCount(topicMapper.selectCount(Wrappers.<InfoForumTopic>lambdaQuery().eq(InfoForumTopic::getStatus, "0")));
        vo.setActiveUserCount(defaultLong(portalMapper.countForumActiveUsers()));
        vo.setTodayVisitCount(defaultLong(portalMapper.countResourceVisits()));
        return vo;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
