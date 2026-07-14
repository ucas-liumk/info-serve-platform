-- 应用中心 ARM64 开源应用目录（幂等，可重复执行）
-- 只清理旧 E2E 明确标记的测试数据，不覆盖管理员维护的正式应用。

UPDATE app_application
SET status = '1', del_flag = '1', update_time = now()
WHERE app_code LIKE 'openapps-%'
  AND remark = 'App Center V1 本地开源应用测试数据'
  AND del_flag = '0';

WITH open_source_category AS (
    SELECT category_id
    FROM app_category
    WHERE category_code = 'open_source' AND del_flag = '0'
    ORDER BY category_id
    LIMIT 1
), catalog(app_id, app_name, app_code, version, icon, accent, description, tags, access_url, is_security, order_num) AS (
    VALUES
      (2073015000000000002::int8, 'Stirling PDF', 'stirling-pdf', '2.14.2', 'PDF', '#2563eb', 'PDF 合并、拆分、压缩、转换与页面处理应用。', 'PDF,文档处理,转换', 'http://127.0.0.1:18080', '0', 2),
      (2073015000000000003::int8, 'draw.io', 'drawio', '30.3.11', 'DIO', '#0f766e', '流程图、架构图、网络拓扑和业务图示绘制应用。', '流程图,架构图,绘图', 'http://127.0.0.1:18082/?offline=1&https=0&lang=zh', '0', 3),
      (2073015000000000004::int8, 'Excalidraw', 'excalidraw', '0.18.1', 'EX', '#c2410c', '轻量白板和草图协作应用。', '白板,草图,协作', 'http://127.0.0.1:18090', '0', 4),
      (2073015000000000005::int8, 'Memos', 'memos', '0.29.1', 'M', '#16a34a', '轻量级备忘录与 Markdown 知识记录应用。', '备忘录,Markdown,知识记录', 'http://127.0.0.1:18110', '0', 5),
      (2073015000000000006::int8, 'File Browser', 'filebrowser', '2.63.18', 'F', '#0891b2', '局域网网页文件管理器。', '文件管理,上传下载,局域网', 'http://127.0.0.1:18111', '0', 6),
      (2073015000000000007::int8, 'PairDrop', 'pairdrop', '1.11.2', 'PD', '#2563eb', '无需客户端的局域网跨平台文件互传工具。', '文件互传,局域网,跨平台', 'http://127.0.0.1:18112', '0', 7),
      (2073015000000000008::int8, 'IT-Tools', 'it-tools', '2024.10.22', 'IT', '#7c3aed', '编码转换、JSON、时间戳、网络与开发常用工具箱。', '工具箱,编码转换,开发工具', 'http://127.0.0.1:18113', '0', 8),
      (2073015000000000009::int8, 'Uptime Kuma', 'uptime-kuma', '2.4.0', 'UK', '#16a34a', '网站、端口和服务可用性监控与状态页。', '监控,状态页,运维', 'http://127.0.0.1:18114', '1', 9),
      (2073015000000000010::int8, 'FreshRSS', 'freshrss', '1.29.1', 'RSS', '#f97316', '自托管 RSS 聚合、订阅和阅读应用。', 'RSS,资讯聚合,订阅', 'http://127.0.0.1:18115', '0', 10),
      (2073015000000000011::int8, 'Gitea', 'gitea', '1.27.0', 'gitea', '#111827', '轻量内网 Git 托管、代码评审与协作平台。', 'Git,代码托管,开发协作', 'http://127.0.0.1:18116', '1', 11),
      (2073015000000000012::int8, 'Jellyfin', 'jellyfin', '10.11.11', 'JF', '#7c3aed', '培训视频、音频和内部媒体资料库。', '媒体库,视频,培训资料', 'http://127.0.0.1:18118', '0', 12),
      (2073015000000000013::int8, 'SearXNG', 'searxng', '2026.07.15', 'SX', '#0f766e', '保护隐私的聚合搜索入口。', '搜索,聚合,隐私', 'http://127.0.0.1:18119', '0', 13)
)
INSERT INTO app_application
    (app_id, app_name, app_code, version, category_id, icon, accent,
     description, tags, access_url, app_type, status, is_security,
     access_mode, use_count, recommend_count, order_num, tenant_id,
     del_flag, create_time)
SELECT
    catalog.app_id, catalog.app_name, catalog.app_code, catalog.version,
    open_source_category.category_id, catalog.icon, catalog.accent,
    catalog.description, catalog.tags, catalog.access_url,
    'online', '1', catalog.is_security, 'all', 0, 0, catalog.order_num,
    '000000', '0', now()
FROM catalog
CROSS JOIN open_source_category
ON CONFLICT (tenant_id, app_code) WHERE del_flag = '0' DO NOTHING;

-- 只把项目旧种子中的漂移版本补为锁定版本，不覆盖管理员自定义版本。
UPDATE app_application SET version = '2.14.2', status = '1', update_time = now()
WHERE tenant_id = '000000' AND app_code = 'stirling-pdf' AND version = 'latest' AND del_flag = '0';
UPDATE app_application SET version = '30.3.11', status = '1', update_time = now()
WHERE tenant_id = '000000' AND app_code = 'drawio' AND version = 'latest' AND del_flag = '0';
UPDATE app_application SET version = '0.18.1', status = '1', update_time = now()
WHERE tenant_id = '000000' AND app_code = 'excalidraw' AND version = 'latest' AND del_flag = '0';
