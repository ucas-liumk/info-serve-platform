#!/usr/bin/env bash
set -euo pipefail

POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-infosys-ruoyi-cloud-plus-postgres}"
POSTGRES_DB="${POSTGRES_DB:-ry-cloud}"
POSTGRES_USER="${POSTGRES_USER:-ruoyi}"

docker exec "${POSTGRES_CONTAINER}" sh -lc "psql -U '${POSTGRES_USER}' -d '${POSTGRES_DB}' -P pager=off -c \"
select 'portal_analytics_module_daily' as table_name, count(*) as rows from portal_analytics_module_daily where stat_date = current_date
union all
select 'portal_analytics_app_rank', count(*) from portal_analytics_app_rank where stat_date = current_date
union all
select 'portal_analytics_resource_rank', count(*) from portal_analytics_resource_rank where stat_date = current_date
union all
select 'portal_analytics_feedback_summary', count(*) from portal_analytics_feedback_summary where stat_date = current_date
union all
select 'portal_analytics_metric_snapshot', count(*) from portal_analytics_metric_snapshot where stat_time::date = current_date
order by table_name;

select job_run_id, job_name, run_status, affected_rows, duration_ms, started_at, finished_at
from portal_analytics_job_run
where job_code = 'PORTAL_USAGE_ANALYTICS'
order by started_at desc
limit 5;

select metric_code, metric_value, metric_label
from portal_analytics_metric_snapshot
where stat_time::date = current_date
order by metric_code;
\""
