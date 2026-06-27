package org.dromara.infoservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.infoservice.domain.InfoForumTopic;
import org.dromara.infoservice.domain.InfoResource;
import org.dromara.infoservice.domain.vo.PortalStatsVo;
import org.dromara.infoservice.mapper.InfoForumTopicMapper;
import org.dromara.infoservice.mapper.InfoPortalMapper;
import org.dromara.infoservice.mapper.InfoResourceMapper;
import org.dromara.infoservice.service.IInfoPortalService;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class InfoPortalServiceImpl implements IInfoPortalService {

    private final InfoResourceMapper resourceMapper;
    private final InfoForumTopicMapper topicMapper;
    private final InfoPortalMapper portalMapper;

    @Override
    public PortalStatsVo stats() {
        PortalStatsVo vo = new PortalStatsVo();
        vo.setResourceCount(resourceMapper.selectCount(Wrappers.<InfoResource>lambdaQuery().eq(InfoResource::getStatus, "0")));
        vo.setToolCount(defaultLong(portalMapper.countOnlineTools()));
        vo.setTopicCount(topicMapper.selectCount(Wrappers.<InfoForumTopic>lambdaQuery().eq(InfoForumTopic::getStatus, "0")));
        vo.setActiveUserCount(defaultLong(portalMapper.countForumActiveUsers()));
        vo.setTodayVisitCount(defaultLong(portalMapper.countResourceVisits()));
        return vo;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
