# Task 5: 单体内解耦③ —— 首页统计 Dubbo 化（3 提供方 + kernel 降级聚合）

kernel 首页统计当前直接注入 3 个内容 BC 的 service 接口（`InfoPortalServiceImpl`）。改为：appcenter/forum/resources 各自以 `@DubboService(group=<模块>)` 提供 `RemoteModuleStatsService`，kernel 以 `@DubboReference(check=false, timeout=2000)` 消费并对每个模块 try/catch 降级——**某 BC 宕机时首页只是该卡片归 0，不再整页失败**。RK 不参与聚合（与现状一致）。单体阶段提供方与消费方同 JVM，Dubbo 走 injvm 本地调用，行为可即时验证。

**Files:**
- Test: `source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/kernel/service/InfoPortalServiceStatsTest.java`（新建）
- Create: `.../appcenter/dubbo/RemoteAppcenterStatsServiceImpl.java`
- Create: `.../forum/dubbo/RemoteForumStatsServiceImpl.java`
- Create: `.../resources/dubbo/RemoteResourcesStatsServiceImpl.java`
- Modify: `.../kernel/service/impl/InfoPortalServiceImpl.java`（重写为 Dubbo 聚合）
- Modify: `.../RuoYiPortalApplication.java`（+`@EnableDubbo`）
- Modify: `source/ruoyi-modules/ruoyi-portal/pom.xml`（显式 +`ruoyi-api-portal-kernel`）
- Modify: `.../arch/BcBoundaryTest.java`（规则 5 收紧至终态）

**Interfaces:**
- Consumes: T1 的 `RemoteModuleStatsService` / `RemoteModuleStatsVo` / `PortalStatsMetrics` / `PortalEventConstants.STATS_GROUP_*`。
- Produces: 三个 Dubbo 提供方（group=appcenter/forum/resources），T7-T10 拆分后自动变为跨进程调用，代码零改动。`PortalStatsVo` 字段与取值语义完全不变。

- [ ] **Step 1: portal pom 显式声明契约依赖**

`source/ruoyi-modules/ruoyi-portal/pom.xml` `<dependencies>` 追加（虽经 portal-event 传递可得，直接使用必须显式声明）：
```xml
        <dependency>
            <groupId>org.dromara</groupId>
            <artifactId>ruoyi-api-portal-kernel</artifactId>
        </dependency>
```

- [ ] **Step 2: 写失败测试（RED）**

`src/test/java/org/dromara/portal/kernel/service/InfoPortalServiceStatsTest.java`：
```java
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
```

- [ ] **Step 3: 跑测试确认失败**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：编译失败（`InfoPortalServiceImpl` 尚无无参构造与 `appcenterStats` 字段）。

- [ ] **Step 4: 重写 kernel 聚合实现**

`kernel/service/impl/InfoPortalServiceImpl.java` **整文件替换**：
```java
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
```

- [ ] **Step 5: 三个提供方**

`appcenter/dubbo/RemoteAppcenterStatsServiceImpl.java`：
```java
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
```

`forum/dubbo/RemoteForumStatsServiceImpl.java`：
```java
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
```

`resources/dubbo/RemoteResourcesStatsServiceImpl.java`：
```java
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
```

- [ ] **Step 6: 启动类加 @EnableDubbo**

`RuoYiPortalApplication.java`（现只有 `@SpringBootApplication`；成为 Dubbo 提供方后需要显式开启，参照 `RuoYiSystemApplication`）：
```java
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;

@EnableDubbo
@SpringBootApplication
public class RuoYiPortalApplication { ... }
```

- [ ] **Step 7: ArchUnit 规则 5 收紧至终态（kernel 对内容 BC 零依赖）**

`BcBoundaryTest.java` 的 `kernel_should_only_touch_content_bc_service_interfaces` 整个方法替换为：
```java
    @Test
    void kernel_should_not_depend_on_content_bcs() {
        noClasses().that().resideInAPackage("org.dromara.portal.kernel..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.dromara.portal.appcenter..", "org.dromara.portal.resources..",
                "org.dromara.portal.forum..", "org.dromara.portal.requiredknowledge..")
            .check(classes);
    }
```
至此 kernel↔BC **双向零依赖**（对 BC 侧的零依赖由 T4 规则保证），物理拆分只剩纯搬迁。

- [ ] **Step 8: 跑测试（GREEN）+ 提交**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：`BUILD SUCCESS` 全绿。

```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 首页统计改走 Dubbo 模块契约并支持单模块降级"
```
