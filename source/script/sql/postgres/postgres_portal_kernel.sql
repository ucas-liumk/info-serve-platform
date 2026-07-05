-- 门户内核：模块注册表（后台可配置的门户模块启停/排序/权限可见性）
-- 统计口径不入表：由各 BC 契约按 module_code 约定提供

CREATE TABLE IF NOT EXISTS portal_module (
    module_id   int8         NOT NULL,
    module_code varchar(50)  NOT NULL,
    module_name varchar(50)  NOT NULL,
    description varchar(200) DEFAULT NULL,
    image       varchar(255) DEFAULT NULL,
    entry_path  varchar(200) DEFAULT NULL,
    perms       varchar(100) DEFAULT NULL,
    status      char(1)      DEFAULT '0' NOT NULL,
    sort_order  int4         DEFAULT 0 NOT NULL,
    create_by   varchar(64)  DEFAULT NULL,
    create_time timestamp    DEFAULT NULL,
    update_by   varchar(64)  DEFAULT NULL,
    update_time timestamp    DEFAULT NULL,
    CONSTRAINT pk_portal_module PRIMARY KEY (module_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portal_module_code ON portal_module (module_code);
COMMENT ON TABLE portal_module IS '门户模块注册表';

CREATE TABLE IF NOT EXISTS portal_user_module_preference (
    preference_id int8        NOT NULL,
    tenant_id     varchar(20) DEFAULT '000000' NOT NULL,
    user_id       int8        NOT NULL,
    module_code   varchar(50) NOT NULL,
    sort_order    int4        DEFAULT 0 NOT NULL,
    create_time   timestamp   DEFAULT NULL,
    update_time   timestamp   DEFAULT NULL,
    CONSTRAINT pk_portal_user_module_preference PRIMARY KEY (preference_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_portal_user_module_preference_user_code ON portal_user_module_preference (tenant_id, user_id, module_code);
CREATE INDEX IF NOT EXISTS idx_portal_user_module_preference_order ON portal_user_module_preference (tenant_id, user_id, sort_order);
COMMENT ON TABLE portal_user_module_preference IS '门户首页用户模块排序偏好';
COMMENT ON COLUMN portal_user_module_preference.module_code IS '模块编码，对应 portal_module.module_code';
COMMENT ON COLUMN portal_user_module_preference.sort_order IS '用户自定义排序，前 6 项进入首页首屏';

CREATE TABLE IF NOT EXISTS portal_metric_catalog (
    tenant_id      varchar(20)  DEFAULT '000000' NOT NULL,
    metric_code    varchar(80)  NOT NULL,
    metric_name    varchar(100) NOT NULL,
    metric_unit    varchar(30)  DEFAULT NULL,
    metric_type    varchar(30)  DEFAULT 'summary' NOT NULL,
    definition     varchar(1000) DEFAULT NULL,
    owner_dept     varchar(100) DEFAULT NULL,
    refresh_cycle  varchar(50)  DEFAULT NULL,
    source_system  varchar(100) DEFAULT NULL,
    source_version varchar(50)  DEFAULT 'v1' NOT NULL,
    status         char(1)      DEFAULT '0' NOT NULL,
    create_time    timestamp    DEFAULT NULL,
    update_time    timestamp    DEFAULT NULL,
    CONSTRAINT pk_portal_metric_catalog PRIMARY KEY (tenant_id, metric_code)
);
COMMENT ON TABLE portal_metric_catalog IS '门户态势指标目录';
COMMENT ON COLUMN portal_metric_catalog.metric_code IS '指标编码，作为 DataEase 图表、Hop 管道与元数据治理的稳定引用';
COMMENT ON COLUMN portal_metric_catalog.metric_name IS '指标中文名称';
COMMENT ON COLUMN portal_metric_catalog.metric_unit IS '指标单位';
COMMENT ON COLUMN portal_metric_catalog.metric_type IS '指标类型：summary/trend/rank 等';
COMMENT ON COLUMN portal_metric_catalog.definition IS '指标口径说明';
COMMENT ON COLUMN portal_metric_catalog.owner_dept IS '指标责任部门';
COMMENT ON COLUMN portal_metric_catalog.refresh_cycle IS '指标刷新频率';
COMMENT ON COLUMN portal_metric_catalog.source_system IS '指标来源系统或分析表';
COMMENT ON COLUMN portal_metric_catalog.source_version IS '指标口径版本';

CREATE TABLE IF NOT EXISTS portal_analytics_metric_snapshot (
    tenant_id      varchar(20) DEFAULT '000000' NOT NULL,
    stat_time      timestamp   NOT NULL,
    metric_code    varchar(80) NOT NULL,
    metric_value   numeric(20, 4) DEFAULT 0 NOT NULL,
    metric_label   varchar(200) DEFAULT NULL,
    source_version varchar(50) DEFAULT 'v1' NOT NULL,
    create_time    timestamp   DEFAULT NULL,
    CONSTRAINT pk_portal_analytics_metric_snapshot PRIMARY KEY (tenant_id, stat_time, metric_code)
);
CREATE INDEX IF NOT EXISTS idx_portal_analytics_metric_snapshot_code ON portal_analytics_metric_snapshot (tenant_id, metric_code, stat_time);
COMMENT ON TABLE portal_analytics_metric_snapshot IS '门户态势指标快照';
COMMENT ON COLUMN portal_analytics_metric_snapshot.stat_time IS '快照统计时间';
COMMENT ON COLUMN portal_analytics_metric_snapshot.metric_code IS '指标编码，对应 portal_metric_catalog.metric_code';
COMMENT ON COLUMN portal_analytics_metric_snapshot.metric_value IS '指标数值';
COMMENT ON COLUMN portal_analytics_metric_snapshot.metric_label IS '指标展示标签或补充说明';
COMMENT ON COLUMN portal_analytics_metric_snapshot.source_version IS '生成该快照的指标口径版本';

CREATE TABLE IF NOT EXISTS portal_analytics_module_daily (
    tenant_id    varchar(20) DEFAULT '000000' NOT NULL,
    stat_date    date        NOT NULL,
    module_code  varchar(50) NOT NULL,
    module_name  varchar(100) DEFAULT NULL,
    active_count int8        DEFAULT 0 NOT NULL,
    visit_count  int8        DEFAULT 0 NOT NULL,
    action_count int8        DEFAULT 0 NOT NULL,
    user_count   int8        DEFAULT 0 NOT NULL,
    create_time  timestamp   DEFAULT NULL,
    update_time  timestamp   DEFAULT NULL,
    CONSTRAINT pk_portal_analytics_module_daily PRIMARY KEY (tenant_id, stat_date, module_code)
);
COMMENT ON TABLE portal_analytics_module_daily IS '门户态势模块日活跃';
COMMENT ON COLUMN portal_analytics_module_daily.stat_date IS '统计日期';
COMMENT ON COLUMN portal_analytics_module_daily.module_code IS '门户模块编码';
COMMENT ON COLUMN portal_analytics_module_daily.active_count IS '模块活跃次数';
COMMENT ON COLUMN portal_analytics_module_daily.visit_count IS '模块访问次数';
COMMENT ON COLUMN portal_analytics_module_daily.action_count IS '模块业务动作次数';
COMMENT ON COLUMN portal_analytics_module_daily.user_count IS '模块参与用户数';

CREATE TABLE IF NOT EXISTS portal_analytics_app_rank (
    tenant_id      varchar(20)  DEFAULT '000000' NOT NULL,
    stat_date      date         NOT NULL,
    app_code       varchar(100) NOT NULL,
    app_name       varchar(100) NOT NULL,
    category_name  varchar(100) DEFAULT NULL,
    open_count     int8         DEFAULT 0 NOT NULL,
    download_count int8         DEFAULT 0 NOT NULL,
    demand_count   int8         DEFAULT 0 NOT NULL,
    rank_no        int4         DEFAULT 0 NOT NULL,
    create_time    timestamp    DEFAULT NULL,
    update_time    timestamp    DEFAULT NULL,
    CONSTRAINT pk_portal_analytics_app_rank PRIMARY KEY (tenant_id, stat_date, app_code)
);
CREATE INDEX IF NOT EXISTS idx_portal_analytics_app_rank_no ON portal_analytics_app_rank (tenant_id, stat_date, rank_no);
COMMENT ON TABLE portal_analytics_app_rank IS '门户态势应用热度排行';
COMMENT ON COLUMN portal_analytics_app_rank.stat_date IS '统计日期';
COMMENT ON COLUMN portal_analytics_app_rank.app_code IS '应用编码';
COMMENT ON COLUMN portal_analytics_app_rank.open_count IS '应用打开次数';
COMMENT ON COLUMN portal_analytics_app_rank.download_count IS '离线包下载次数';
COMMENT ON COLUMN portal_analytics_app_rank.demand_count IS '应用相关需求反馈数';
COMMENT ON COLUMN portal_analytics_app_rank.rank_no IS '当日排行序号';

CREATE TABLE IF NOT EXISTS portal_analytics_resource_rank (
    tenant_id      varchar(20)  DEFAULT '000000' NOT NULL,
    stat_date      date         NOT NULL,
    resource_id    int8         NOT NULL,
    resource_name  varchar(200) NOT NULL,
    category_name  varchar(100) DEFAULT NULL,
    view_count     int8         DEFAULT 0 NOT NULL,
    download_count int8         DEFAULT 0 NOT NULL,
    rank_no        int4         DEFAULT 0 NOT NULL,
    create_time    timestamp    DEFAULT NULL,
    update_time    timestamp    DEFAULT NULL,
    CONSTRAINT pk_portal_analytics_resource_rank PRIMARY KEY (tenant_id, stat_date, resource_id)
);
CREATE INDEX IF NOT EXISTS idx_portal_analytics_resource_rank_no ON portal_analytics_resource_rank (tenant_id, stat_date, rank_no);
COMMENT ON TABLE portal_analytics_resource_rank IS '门户态势资料流动排行';
COMMENT ON COLUMN portal_analytics_resource_rank.stat_date IS '统计日期';
COMMENT ON COLUMN portal_analytics_resource_rank.resource_id IS '资料 ID';
COMMENT ON COLUMN portal_analytics_resource_rank.resource_name IS '资料名称';
COMMENT ON COLUMN portal_analytics_resource_rank.view_count IS '资料浏览次数';
COMMENT ON COLUMN portal_analytics_resource_rank.download_count IS '资料下载次数';
COMMENT ON COLUMN portal_analytics_resource_rank.rank_no IS '当日排行序号';

CREATE TABLE IF NOT EXISTS portal_analytics_feedback_summary (
    tenant_id          varchar(20) DEFAULT '000000' NOT NULL,
    stat_date          date        NOT NULL,
    feedback_type      varchar(50) NOT NULL,
    feedback_status    varchar(20) NOT NULL,
    total_count        int8        DEFAULT 0 NOT NULL,
    avg_response_hours numeric(10, 2) DEFAULT NULL,
    create_time        timestamp   DEFAULT NULL,
    update_time        timestamp   DEFAULT NULL,
    CONSTRAINT pk_portal_analytics_feedback_summary PRIMARY KEY (tenant_id, stat_date, feedback_type, feedback_status)
);
COMMENT ON TABLE portal_analytics_feedback_summary IS '门户态势需求反馈统计';
COMMENT ON COLUMN portal_analytics_feedback_summary.stat_date IS '统计日期';
COMMENT ON COLUMN portal_analytics_feedback_summary.feedback_type IS '反馈类型';
COMMENT ON COLUMN portal_analytics_feedback_summary.feedback_status IS '反馈处理状态';
COMMENT ON COLUMN portal_analytics_feedback_summary.total_count IS '反馈数量';
COMMENT ON COLUMN portal_analytics_feedback_summary.avg_response_hours IS '平均响应时长，单位小时';

CREATE TABLE IF NOT EXISTS portal_analytics_job_run (
    tenant_id     varchar(20) DEFAULT '000000' NOT NULL,
    job_run_id    varchar(64) NOT NULL,
    job_code      varchar(80) NOT NULL,
    job_name      varchar(100) DEFAULT NULL,
    run_status    varchar(20) DEFAULT 'running' NOT NULL,
    started_at    timestamp   NOT NULL,
    finished_at   timestamp   DEFAULT NULL,
    duration_ms   int8        DEFAULT NULL,
    affected_rows int8        DEFAULT 0 NOT NULL,
    error_message varchar(2000) DEFAULT NULL,
    create_time   timestamp   DEFAULT NULL,
    CONSTRAINT pk_portal_analytics_job_run PRIMARY KEY (tenant_id, job_run_id)
);
CREATE INDEX IF NOT EXISTS idx_portal_analytics_job_run_code ON portal_analytics_job_run (tenant_id, job_code, started_at);
COMMENT ON TABLE portal_analytics_job_run IS '门户态势数据治理任务运行记录';
COMMENT ON COLUMN portal_analytics_job_run.job_run_id IS '任务运行 ID，由 Hop 或调度器生成';
COMMENT ON COLUMN portal_analytics_job_run.job_code IS '任务编码';
COMMENT ON COLUMN portal_analytics_job_run.job_name IS '任务名称';
COMMENT ON COLUMN portal_analytics_job_run.run_status IS '运行状态：running/success/failed';
COMMENT ON COLUMN portal_analytics_job_run.started_at IS '开始时间';
COMMENT ON COLUMN portal_analytics_job_run.finished_at IS '结束时间';
COMMENT ON COLUMN portal_analytics_job_run.duration_ms IS '运行耗时，单位毫秒';
COMMENT ON COLUMN portal_analytics_job_run.affected_rows IS '影响行数';
COMMENT ON COLUMN portal_analytics_job_run.error_message IS '失败错误信息';

INSERT INTO portal_metric_catalog
    (tenant_id, metric_code, metric_name, metric_unit, metric_type, definition, owner_dept, refresh_cycle, source_system, source_version, status, create_time)
VALUES
    ('000000', 'USAGE_TOTAL', '累计使用次数', '次', 'summary', '工具打开、资料流动、论坛互动等应用行为汇总。', '信息中心', '每日', 'ruoyi-portal', 'v1', '0', now()),
    ('000000', 'ACTIVE_USER', '活跃用户数', '人', 'summary', '参与资料、应用、论坛、反馈等任一行为的用户数。', '信息中心', '每日', 'ruoyi-portal', 'v1', '0', now()),
    ('000000', 'MODULE_ACTIVE_DAILY', '模块活跃趋势', '次', 'trend', '按日期和模块统计访问、互动、使用等活跃行为。', '信息中心', '每日', 'portal_analytics_module_daily', 'v1', '0', now()),
    ('000000', 'APP_HOT_RANK', '应用热度排行', '次', 'rank', '按应用打开、下载、需求反馈等行为形成热度排行。', '信息中心', '每日', 'portal_analytics_app_rank', 'v1', '0', now()),
    ('000000', 'RESOURCE_FLOW_RANK', '资料流动排行', '次', 'rank', '按资料浏览和下载形成流动热度排行。', '信息中心', '每日', 'portal_analytics_resource_rank', 'v1', '0', now()),
    ('000000', 'FEEDBACK_SUMMARY', '需求反馈统计', '条', 'summary', '按反馈类型和处理状态汇总需求反馈规模。', '信息中心', '每日', 'portal_analytics_feedback_summary', 'v1', '0', now())
ON CONFLICT (tenant_id, metric_code) DO NOTHING;

INSERT INTO portal_module (module_id, module_code, module_name, description, entry_path, status, sort_order, create_time)
VALUES
    (1, 'resources', '资料共享', '数据汇聚  共享共用', '/portal/resources', '0', 1, now()),
    (2, 'appcenter', '应用中心', '应用聚合  即取即用', '/portal/tools',     '0', 2, now()),
    (3, 'usage-dashboard', '应用态势', '运行洞察  转型透明', '/portal/usage-dashboard', '0', 3, now()),
    (4, 'forum',     '服务论坛', '交流互动  共建共治', '/portal/forum',    '0', 4, now()),
    (5, 'qa',        '智能问答', '智慧问答  快速响应', NULL,               '1', 80, now()),
    (6, 'news',      '时事热点', '热点速递  洞察先机', NULL,               '1', 90, now())
ON CONFLICT (module_id) DO NOTHING;

-- 管理后台菜单：门户配置
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache, menu_type, visible, status, perms, icon, create_dept, create_by, create_time, remark)
VALUES
    (2099, '门户配置', 0, 8, 'portal', NULL, '', '1', '0', 'M', '0', '0', '', 'component', 103, 1, now(), '门户公共配置'),
    (2090, '模块注册表', 2099, 1, 'module', 'admin/portal/module/index', '', '1', '0', 'C', '0', '0', 'portal:module:list', 'component', 103, 1, now(), '门户模块启停/排序/权限配置'),
    (2091, '模块新增', 2090, 1, '', '', '', '1', '0', 'F', '0', '0', 'portal:module:add', '#', 103, 1, now(), ''),
    (2092, '模块修改', 2090, 2, '', '', '', '1', '0', 'F', '0', '0', 'portal:module:edit', '#', 103, 1, now(), ''),
    (2093, '模块删除', 2090, 3, '', '', '', '1', '0', 'F', '0', '0', 'portal:module:remove', '#', 103, 1, now(), '')
ON CONFLICT (menu_id) DO NOTHING;
