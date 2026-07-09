package org.dromara.portal.kernel.service;

import org.dromara.portal.api.PortalStatsMetrics;
import org.dromara.portal.api.RemoteModuleStatsService;
import org.dromara.portal.api.domain.vo.RemoteModuleStatsVo;
import org.dromara.portal.kernel.domain.vo.PortalStatsVo;
import org.dromara.portal.kernel.service.impl.InfoPortalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfoPortalServiceStatsTest {

    private final RemoteModuleStatsService appcenterStats = mock(RemoteModuleStatsService.class);
    private final RemoteModuleStatsService forumStats = mock(RemoteModuleStatsService.class);
    private final RemoteModuleStatsService resourcesStats = mock(RemoteModuleStatsService.class);

    private InfoPortalServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new InfoPortalServiceImpl();
        ReflectionTestUtils.setField(service, "appcenterStats", appcenterStats);
        ReflectionTestUtils.setField(service, "forumStats", forumStats);
        ReflectionTestUtils.setField(service, "resourcesStats", resourcesStats);
    }

    private RemoteModuleStatsVo vo(Map<String, Long> metrics) {
        RemoteModuleStatsVo v = new RemoteModuleStatsVo();
        v.setMetrics(metrics);
        return v;
    }

    @Test
    void aggregates_metrics_from_three_modules() {
        when(appcenterStats.stats()).thenReturn(vo(Map.of(PortalStatsMetrics.APP_COUNT, 4L)));
        when(forumStats.stats()).thenReturn(vo(Map.of(
            PortalStatsMetrics.TOPIC_COUNT, 7L, PortalStatsMetrics.ACTIVE_AUTHOR_COUNT, 3L)));
        when(resourcesStats.stats()).thenReturn(vo(Map.of(
            PortalStatsMetrics.RESOURCE_COUNT, 11L, PortalStatsMetrics.VISIT_COUNT, 99L)));

        PortalStatsVo stats = service.stats();

        assertEquals(4L, stats.getToolCount());
        assertEquals(7L, stats.getTopicCount());
        assertEquals(3L, stats.getActiveUserCount());
        assertEquals(11L, stats.getResourceCount());
        assertEquals(99L, stats.getTodayVisitCount());
    }

    @Test
    void module_failure_degrades_to_zero_without_breaking_others() {
        when(appcenterStats.stats()).thenThrow(new RuntimeException("provider down"));
        when(forumStats.stats()).thenReturn(vo(Map.of(PortalStatsMetrics.TOPIC_COUNT, 7L)));
        when(resourcesStats.stats()).thenReturn(vo(Map.of(PortalStatsMetrics.RESOURCE_COUNT, 11L)));

        PortalStatsVo stats = service.stats();

        assertEquals(0L, stats.getToolCount());
        assertEquals(7L, stats.getTopicCount());
        assertEquals(0L, stats.getActiveUserCount());
        assertEquals(11L, stats.getResourceCount());
        assertEquals(0L, stats.getTodayVisitCount());
    }
}
