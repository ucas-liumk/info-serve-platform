package org.dromara.portal.kernel.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class UsageDashboardOverviewVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private String title = "系统应用情况洞察看板";
    private String subtitle = "看模块活跃、资料流动、应用热度、部门覆盖与反馈响应";
    private String periodLabel = "全量数据";
    private String scopeLabel = "全单位";
    private String generatedAt;

    private List<KpiCard> kpis = new ArrayList<>();
    private List<MetricItem> moduleActivity = new ArrayList<>();
    private List<RankItem> resourceRanking = new ArrayList<>();
    private List<RankItem> appRanking = new ArrayList<>();
    private Heatmap heatmap = new Heatmap();
    private FeedbackSummary feedback = new FeedbackSummary();
    private List<SignalItem> signals = new ArrayList<>();

    @Data
    public static class KpiCard implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String code;
        private String label;
        private Long value;
        private String unit;
        private String hint;

        public KpiCard(String code, String label, Long value, String unit, String hint) {
            this.code = code;
            this.label = label;
            this.value = value;
            this.unit = unit;
            this.hint = hint;
        }
    }

    @Data
    public static class MetricItem implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String code;
        private String label;
        private Long value;
        private String hint;

        public MetricItem(String code, String label, Long value, String hint) {
            this.code = code;
            this.label = label;
            this.value = value;
            this.hint = hint;
        }
    }

    @Data
    public static class RankItem implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String name;
        private String category;
        private Long value;
        private String unit;

        public RankItem(String name, String category, Long value, String unit) {
            this.name = name;
            this.category = category;
            this.value = value;
            this.unit = unit;
        }
    }

    @Data
    public static class Heatmap implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private boolean ready;
        private String emptyReason = "暂无足够部门归集数据";
        private List<HeatmapColumn> columns = new ArrayList<>();
        private List<HeatmapRow> rows = new ArrayList<>();
    }

    @Data
    public static class HeatmapColumn implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String code;
        private String label;

        public HeatmapColumn(String code, String label) {
            this.code = code;
            this.label = label;
        }
    }

    @Data
    public static class HeatmapRow implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long deptId;
        private String deptName;
        private Long total;
        private List<HeatmapCell> cells = new ArrayList<>();
    }

    @Data
    public static class HeatmapCell implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String columnCode;
        private Long value;

        public HeatmapCell(String columnCode, Long value) {
            this.columnCode = columnCode;
            this.value = value;
        }
    }

    @Data
    public static class FeedbackSummary implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private Long total = 0L;
        private Long pending = 0L;
        private Long processing = 0L;
        private Long resolved = 0L;
        private Long closed = 0L;
        private List<MetricItem> typeStats = new ArrayList<>();
    }

    @Data
    public static class SignalItem implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String level;
        private String title;
        private String action;

        public SignalItem(String level, String title, String action) {
            this.level = level;
            this.title = title;
            this.action = action;
        }
    }
}
