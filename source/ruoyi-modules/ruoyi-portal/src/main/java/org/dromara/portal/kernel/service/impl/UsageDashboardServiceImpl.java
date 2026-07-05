package org.dromara.portal.kernel.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.portal.appcenter.service.IAppApplicationService;
import org.dromara.portal.appcenter.service.IAppDemandService;
import org.dromara.portal.forum.service.IInfoForumService;
import org.dromara.portal.kernel.domain.AppMessage;
import org.dromara.portal.kernel.domain.vo.UsageDashboardOverviewVo;
import org.dromara.portal.kernel.mapper.AppMessageMapper;
import org.dromara.portal.kernel.service.IUsageDashboardService;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.dromara.system.api.RemoteDeptService;
import org.dromara.system.api.domain.vo.RemoteDeptVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class UsageDashboardServiceImpl implements IUsageDashboardService {

    private static final int RANK_LIMIT = 4;
    private static final int HEATMAP_LIMIT = 8;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IInfoResourceService resourceService;
    private final IInfoForumService forumService;
    private final IAppApplicationService appApplicationService;
    private final IAppDemandService demandService;
    private final AppMessageMapper messageMapper;

    @DubboReference
    private RemoteDeptService remoteDeptService;

    @Override
    public UsageDashboardOverviewVo overview() {
        Long toolCount = defaultLong(appApplicationService.countPortalVisible());
        Long resourceCount = defaultLong(resourceService.countPortalVisible());
        Long appUseCount = defaultLong(appApplicationService.sumPortalUseCount());
        Long resourceUseCount = defaultLong(resourceService.sumPortalVisits());
        Long forumInteractionCount = defaultLong(forumService.sumPortalInteractions());
        Long messageCount = defaultLong(messageMapper.selectCount(Wrappers.<AppMessage>lambdaQuery()));
        Long pendingFeedbackCount = defaultLong(demandService.countPending());
        Long activeUserCount = (long) collectActorIds().size();
        UsageDashboardOverviewVo.Heatmap heatmap = buildHeatmap();

        UsageDashboardOverviewVo vo = new UsageDashboardOverviewVo();
        vo.setGeneratedAt(DATE_TIME_FORMATTER.format(LocalDateTime.now()));
        vo.setKpis(buildKpis(toolCount, resourceCount, activeUserCount, appUseCount + resourceUseCount + forumInteractionCount, pendingFeedbackCount, heatmap));
        vo.setModuleActivity(buildModuleActivity(appUseCount, resourceUseCount, forumInteractionCount, messageCount));
        vo.setAppRanking(buildAppRanking());
        vo.setResourceRanking(buildResourceRanking());
        vo.setFeedback(buildFeedback());
        vo.setHeatmap(heatmap);
        vo.setSignals(buildSignals(pendingFeedbackCount, appApplicationService.countPortalLowUsage(0L), forumInteractionCount, vo.getHeatmap()));
        return vo;
    }

    private List<UsageDashboardOverviewVo.KpiCard> buildKpis(Long toolCount, Long resourceCount, Long activeUserCount, Long totalUsage, Long pendingFeedback,
                                                             UsageDashboardOverviewVo.Heatmap heatmap) {
        long activeDeptCount = heatmap.getRows().size();
        long totalDeptCount = countAllDepartments();
        long coverage = totalDeptCount == 0 ? 0L : Math.round(activeDeptCount * 100.0 / totalDeptCount);
        return List.of(
            new UsageDashboardOverviewVo.KpiCard("tools", "可用工具", toolCount, "个", "上架可访问"),
            new UsageDashboardOverviewVo.KpiCard("resources", "资料资产", resourceCount, "份", "公开可用"),
            new UsageDashboardOverviewVo.KpiCard("activeUsers", "活跃用户", activeUserCount, "人", "资料/论坛/反馈参与"),
            new UsageDashboardOverviewVo.KpiCard("deptCoverage", "部门覆盖", coverage, "%", activeDeptCount + "/" + totalDeptCount + " 个部门有数据"),
            new UsageDashboardOverviewVo.KpiCard("usage", "使用次数", totalUsage, "次", "工具+资料+论坛"),
            new UsageDashboardOverviewVo.KpiCard("feedback", "待处理反馈", pendingFeedback, "条", "需求/建议")
        );
    }

    private List<UsageDashboardOverviewVo.MetricItem> buildModuleActivity(Long appUseCount, Long resourceUseCount, Long forumInteractionCount, Long messageCount) {
        return List.of(
            new UsageDashboardOverviewVo.MetricItem("appcenter", "工具即用", appUseCount, "应用打开次数"),
            new UsageDashboardOverviewVo.MetricItem("resources", "资料共享", resourceUseCount, "浏览+下载"),
            new UsageDashboardOverviewVo.MetricItem("forum", "服务论坛", forumInteractionCount, "浏览+回复+点赞"),
            new UsageDashboardOverviewVo.MetricItem("message", "通知消息", messageCount, "已产生通知")
        );
    }

    private List<UsageDashboardOverviewVo.RankItem> buildAppRanking() {
        return appApplicationService.listPortalUsageTop(RANK_LIMIT)
            .stream()
            .map(item -> new UsageDashboardOverviewVo.RankItem(item.name(), item.categoryName(), defaultLong(item.value()), "次"))
            .toList();
    }

    private List<UsageDashboardOverviewVo.RankItem> buildResourceRanking() {
        return resourceService.listPortalUsageTop(RANK_LIMIT)
            .stream()
            .map(item -> new UsageDashboardOverviewVo.RankItem(item.name(), item.categoryName(), defaultLong(item.value()), "次"))
            .toList();
    }

    private UsageDashboardOverviewVo.FeedbackSummary buildFeedback() {
        Map<String, Long> statusMap = demandService.listStatusStats()
            .stream()
            .collect(Collectors.toMap(IAppDemandService.DemandStatusStat::status, IAppDemandService.DemandStatusStat::value, Long::sum));
        UsageDashboardOverviewVo.FeedbackSummary feedback = new UsageDashboardOverviewVo.FeedbackSummary();
        feedback.setPending(defaultLong(statusMap.get("0")));
        feedback.setProcessing(defaultLong(statusMap.get("1")));
        feedback.setResolved(defaultLong(statusMap.get("2")));
        feedback.setClosed(defaultLong(statusMap.get("3")));
        feedback.setTotal(feedback.getPending() + feedback.getProcessing() + feedback.getResolved() + feedback.getClosed());
        feedback.setTypeStats(demandService.listTypeStats().stream()
            .map(item -> new UsageDashboardOverviewVo.MetricItem(item.demandType(), demandTypeLabel(item.demandType()), defaultLong(item.value()), "反馈类型"))
            .toList());
        return feedback;
    }

    private UsageDashboardOverviewVo.Heatmap buildHeatmap() {
        UsageDashboardOverviewVo.Heatmap heatmap = new UsageDashboardOverviewVo.Heatmap();
        heatmap.setColumns(List.of(
            new UsageDashboardOverviewVo.HeatmapColumn("appcenter", "应用"),
            new UsageDashboardOverviewVo.HeatmapColumn("resources", "资料"),
            new UsageDashboardOverviewVo.HeatmapColumn("forum", "论坛"),
            new UsageDashboardOverviewVo.HeatmapColumn("feedback", "反馈")
        ));

        Map<Long, DeptRowBuilder> rows = new LinkedHashMap<>();
        appApplicationService.listDeptUsageStats().forEach(item -> addDeptMetric(rows, item.deptId(), "appcenter", item.value()));
        resourceService.listDeptResourceStats().forEach(item -> addDeptMetric(rows, item.deptId(), "resources", item.value()));
        forumService.listDeptForumStats().forEach(item -> addDeptMetric(rows, item.deptId(), "forum", item.value()));
        demandService.listDeptDemandStats().forEach(item -> addDeptMetric(rows, item.deptId(), "feedback", item.value()));

        List<DeptRowBuilder> sorted = rows.values().stream()
            .filter(row -> row.total() > 0)
            .sorted(Comparator.comparing(DeptRowBuilder::total).reversed())
            .limit(HEATMAP_LIMIT)
            .toList();
        Map<Long, String> deptNames = resolveDeptNames(sorted.stream().map(row -> row.deptId).toList());

        heatmap.setRows(sorted.stream()
            .map(row -> row.toVo(deptNames.getOrDefault(row.deptId, "部门" + row.deptId), heatmap.getColumns()))
            .toList());
        heatmap.setReady(!heatmap.getRows().isEmpty());
        if (!heatmap.isReady()) {
            heatmap.setEmptyReason("暂无足够部门归集数据，待用户行为日志完善后展示部门热力");
        }
        return heatmap;
    }

    private List<UsageDashboardOverviewVo.SignalItem> buildSignals(Long pendingFeedbackCount, Long lowUsageApps, Long forumInteractionCount,
                                                                   UsageDashboardOverviewVo.Heatmap heatmap) {
        List<UsageDashboardOverviewVo.SignalItem> signals = new ArrayList<>();
        if (defaultLong(lowUsageApps) > 0) {
            signals.add(new UsageDashboardOverviewVo.SignalItem("warning", lowUsageApps + " 个上架工具暂无使用记录", "复核"));
        }
        if (defaultLong(pendingFeedbackCount) > 0) {
            signals.add(new UsageDashboardOverviewVo.SignalItem("warning", pendingFeedbackCount + " 条应用需求仍处于待处理", "处理"));
        }
        if (defaultLong(forumInteractionCount) == 0) {
            signals.add(new UsageDashboardOverviewVo.SignalItem("info", "服务论坛暂无互动数据", "引导"));
        }
        if (!heatmap.isReady()) {
            signals.add(new UsageDashboardOverviewVo.SignalItem("info", "部门热力口径待完善", "接入"));
        }
        if (signals.isEmpty()) {
            signals.add(new UsageDashboardOverviewVo.SignalItem("success", "当前未发现明显低活跃信号", "保持"));
        }
        return signals;
    }

    private Set<Long> collectActorIds() {
        Set<Long> actorIds = new LinkedHashSet<>();
        actorIds.addAll(resourceService.listPortalActorIds());
        actorIds.addAll(forumService.listActiveAuthorIds());
        actorIds.addAll(demandService.listRequesterIds());
        actorIds.remove(null);
        return actorIds;
    }

    private long countAllDepartments() {
        try {
            List<RemoteDeptVo> depts = remoteDeptService.selectDeptsByList();
            return depts == null ? 0L : depts.size();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private Map<Long, String> resolveDeptNames(List<Long> deptIds) {
        List<Long> ids = deptIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Map<Long, String> names = remoteDeptService.selectDeptNamesByIds(ids);
            return names == null ? Collections.emptyMap() : names;
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private void addDeptMetric(Map<Long, DeptRowBuilder> rows, Long deptId, String columnCode, Long value) {
        if (deptId == null || defaultLong(value) <= 0) {
            return;
        }
        rows.computeIfAbsent(deptId, DeptRowBuilder::new).put(columnCode, value);
    }

    private String demandTypeLabel(String demandType) {
        if ("new_app".equals(demandType)) {
            return "新应用";
        }
        if ("suggestion".equals(demandType)) {
            return "改进建议";
        }
        return "其他反馈";
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private static final class DeptRowBuilder {
        private final Long deptId;
        private final Map<String, Long> values = new HashMap<>();

        private DeptRowBuilder(Long deptId) {
            this.deptId = deptId;
        }

        private void put(String columnCode, Long value) {
            values.merge(columnCode, value == null ? 0L : value, Long::sum);
        }

        private Long total() {
            return values.values().stream().reduce(0L, Long::sum);
        }

        private UsageDashboardOverviewVo.HeatmapRow toVo(String deptName, List<UsageDashboardOverviewVo.HeatmapColumn> columns) {
            UsageDashboardOverviewVo.HeatmapRow row = new UsageDashboardOverviewVo.HeatmapRow();
            row.setDeptId(deptId);
            row.setDeptName(deptName);
            row.setTotal(total());
            row.setCells(columns.stream()
                .map(column -> new UsageDashboardOverviewVo.HeatmapCell(column.getCode(), values.getOrDefault(column.getCode(), 0L)))
                .toList());
            return row;
        }
    }
}
