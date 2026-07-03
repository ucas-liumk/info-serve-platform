#!/usr/bin/env python3
"""生成离线部署所需的数据库初始化文件。

迁移后架构：
- MySQL 仅作为 Nacos 配置库（ry-config），输出到 deploy/initdb-mysql/
- PostgreSQL 承载业务库（ry-cloud），输出到 deploy/initdb-postgres/

PG 业务库由 deploy/initdb-postgres/00-init.sh 在容器首次启动时创建并导入
deploy/initdb-postgres/dumps/ 下的官方 postgres_ry_*.sql。
"""
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "source"
OUT_MYSQL = ROOT / "deploy" / "initdb-mysql"
OUT_PG = ROOT / "deploy" / "initdb-postgres"

# PG 业务库：库名 -> 官方 PG 脚本文件名
PG_DATABASES = [
    ("ry-cloud", "postgres_ry_cloud.sql"),
]


def write_mysql_config_db() -> None:
    """MySQL 仅保留 Nacos 配置库 ry-config。"""
    source_file = SOURCE / "script" / "sql" / "ry-config.sql"
    content = source_file.read_text(encoding="utf-8")
    (OUT_MYSQL / "04-ry-config.sql").write_text(
        "SET NAMES utf8mb4;\n"
        "CREATE DATABASE IF NOT EXISTS `ry-config` DEFAULT CHARACTER SET utf8mb4 "
        "COLLATE utf8mb4_general_ci;\n"
        "USE `ry-config`;\n\n"
        f"{content}\n",
        encoding="utf-8",
    )


def sql_string(value: str) -> str:
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def normalize_nacos_content(name: str, content: str, tenant: str) -> str:
    """生成 Nacos 配置。

    dev 面向本机 Java 调试，使用宿主机 81xx 端口。
    prod 面向 Docker 容器部署，使用容器服务名和容器内部默认端口。
    """
    content = content.replace("username: ruoyi\n    password: password", "username: ruoyi\n    password: ruoyi123")
    content = content.replace("store.db.user=root\nstore.db.password=password", "store.db.user=root\nstore.db.password=ruoyi123")

    if tenant == "prod":
        # Nacos 自身配置库仍在 MySQL，db.url 在 compose 中以 JAVA_OPTS 注入，这里仅处理业务数据源
        content = content.replace("jdbc:postgresql://localhost:8132/", "jdbc:postgresql://postgres:5432/")
        content = content.replace("jdbc:postgresql://127.0.0.1:8132/", "jdbc:postgresql://postgres:5432/")
        content = content.replace("jdbc:mysql://localhost:8136/", "jdbc:mysql://mysql:3306/")
        content = content.replace("jdbc:mysql://127.0.0.1:8136/", "jdbc:mysql://mysql:3306/")
        if name == "application-common.yml":
            content = content.replace("rabbitmq:\n    host: localhost\n    port: 8172", "rabbitmq:\n    host: rabbitmq\n    port: 5672")
            content = content.replace("redis:\n      host: localhost\n      port: 8179", "redis:\n      host: redis\n      port: 6379")
        if name == "ruoyi-auth.yml":
            content = content.replace("address: http://localhost:7018", "address: http://nginx-web")
            content = content.replace("captcha:\n    # 是否开启验证码\n    enabled: true", "captcha:\n    # 是否开启验证码\n    enabled: false")
    return content


def generate_nacos_update_sql() -> None:
    """把 nacos 配置内容回写进 MySQL 的 ry-config.config_info。"""
    config_dir = SOURCE / "script" / "config" / "nacos"
    statements = ["SET NAMES utf8mb4;\nUSE `ry-config`;\n"]
    for path in sorted(config_dir.iterdir()):
        if path.name == "README.md" or not path.is_file():
            continue
        for tenant in ("dev", "prod"):
            content = normalize_nacos_content(path.name, path.read_text(encoding="utf-8"), tenant)
            literal = sql_string(content)
            statements.append(
                "UPDATE config_info "
                f"SET content={literal}, md5=MD5({literal}), gmt_modified=NOW() "
                f"WHERE data_id={sql_string(path.name)} AND group_id='DEFAULT_GROUP' AND tenant_id='{tenant}';\n"
            )
    # 新模块配置在基线中无行,需 INSERT(MySQL ry-config 为 Nacos 存储)
    new_configs = ["ruoyi-appcenter.yml", "ruoyi-infoservice.yml"]
    for name in new_configs:
        path = config_dir / name
        for tenant in ("dev", "prod"):
            content = normalize_nacos_content(name, path.read_text(encoding="utf-8"), tenant)
            literal = sql_string(content)
            statements.append(
                "INSERT INTO config_info "
                "(data_id, group_id, content, md5, gmt_create, gmt_modified, src_user, src_ip, app_name, tenant_id, c_desc, c_use, effect, type, c_schema, encrypted_data_key) "
                f"SELECT {sql_string(name)}, 'DEFAULT_GROUP', {literal}, MD5({literal}), NOW(), NOW(), NULL, NULL, '', '{tenant}', '', '', '', 'yaml', '', '' "
                "WHERE NOT EXISTS (SELECT 1 FROM config_info WHERE data_id="
                f"{sql_string(name)} AND group_id='DEFAULT_GROUP' AND tenant_id='{tenant}');\n"
            )
            statements.append(
                "UPDATE config_info "
                f"SET content={literal}, md5=MD5({literal}), gmt_modified=NOW() "
                f"WHERE data_id={sql_string(name)} AND group_id='DEFAULT_GROUP' AND tenant_id='{tenant}';\n"
            )
    (OUT_MYSQL / "90-nacos-config-content.sql").write_text("".join(statements), encoding="utf-8")


def write_pg_business_db() -> None:
    """复制官方 PG 业务脚本 + 一期业务扩展脚本,并生成首启初始化脚本。"""
    pg_src = SOURCE / "script" / "sql" / "postgres"
    dumps_dir = OUT_PG / "dumps"
    dumps_dir.mkdir(parents=True, exist_ok=True)
    extras = ["postgres_app_center.sql", "postgres_info_service.sql"]  # 追加到 ry-cloud
    for _, filename in PG_DATABASES:
        shutil.copyfile(pg_src / filename, dumps_dir / filename)
    for extra in extras:
        shutil.copyfile(pg_src / extra, dumps_dir / extra)

    pairs = " ".join(f'"{db}:{filename}"' for db, filename in PG_DATABASES)
    init_sh = (
        "#!/bin/bash\n"
        "set -euo pipefail\n"
        'DUMP_DIR="$(dirname "$0")/dumps"\n'
        f"for pair in {pairs}; do\n"
        '  db="${pair%%:*}"\n'
        '  file="${pair##*:}"\n'
        '  echo ">> creating database $db and importing $file"\n'
        '  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" -c "CREATE DATABASE \\"$db\\";"\n'
        '  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$db" -f "$DUMP_DIR/$file"\n'
        "done\n"
        'echo ">> importing phase1 extension schemas into ry-cloud"\n'
        + "".join(
            f'psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "ry-cloud" -f "$DUMP_DIR/{extra}"\n'
            for extra in extras
        ) +
        'echo ">> postgres business databases initialized"\n'
    )
    target = OUT_PG / "00-init.sh"
    target.write_text(init_sh, encoding="utf-8")
    target.chmod(0o755)


def main() -> None:
    OUT_MYSQL.mkdir(parents=True, exist_ok=True)
    OUT_PG.mkdir(parents=True, exist_ok=True)
    write_mysql_config_db()
    generate_nacos_update_sql()
    write_pg_business_db()
    print(f"MySQL(Nacos) initdb -> {OUT_MYSQL}")
    print(f"PostgreSQL business initdb -> {OUT_PG}")


if __name__ == "__main__":
    main()
