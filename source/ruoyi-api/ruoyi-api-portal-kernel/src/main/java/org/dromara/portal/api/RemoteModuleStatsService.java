package org.dromara.portal.api;

import org.dromara.portal.api.domain.vo.RemoteModuleStatsVo;

/**
 * 门户模块统计契约：各内容 BC 以 @DubboService(group=模块名) 提供，kernel 聚合首页统计。
 * 指标键见 {@link PortalStatsMetrics}。
 */
public interface RemoteModuleStatsService {

    RemoteModuleStatsVo stats();
}
