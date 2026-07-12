# 资料阅读工作台一期 · Plan 1（后端地基）实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地阅读版本固化（`res_document_version`）、转换状态机 API、标注/笔记 CRUD 三组后端能力，为前端三栏阅读工作台（Plan 2）提供全部契约。

**Architecture:** 在 `ruoyi-portal-resources` 服务内新增 reader 子域：转换链路在既有 MQ 消费点扩展「固化为 OSS 工件 + 写版本行」（版本行**只有 listener 一个写者**）；会话 API 是纯读 + 幂等入队；标注/笔记是常规 MyBatis-Plus CRUD + 行级归属校验。锚点/笔记 JSON 服务端只做校验与透传，存 `text` 列（不查 JSON 内部，避免 PG jsonb 的 JDBC 摩擦——对 spec §3 的记录性偏差①）。

**Tech Stack:** Spring Boot 3 + MyBatis-Plus + RabbitMQ（既有队列）+ MinIO OSS（经 `RemoteFileService` Dubbo）+ Apache PDFBox 3.0.3（新增，页数/文本层探测）。

**Spec:** `docs/superpowers/specs/2026-07-11-resources-reader-notes-redesign-design.md`（本分支）

**对 spec 的三处记录性偏差**（实现更稳，语义不变）：
① `content`/`anchor` 存 `text` 而非 `jsonb`（理由见上；三期检索用 `content_text`）；
② 笔记乐观锁用显式 `revision int8` 而非 `update_time` 比对（时间戳精度不可靠）；
③ `res_note` 增加 `author_name varchar(80)`（公开笔记列表展示需要，与旧表对齐）。

## Global Constraints（逐条来自 AGENTS.md / spec）

- 分支：从最新 `origin/main` 开 `feature/resources-reader-phase1-backend`；每个 Task 一次 commit；收工 `git push -u origin <branch>`；提交格式 `<type>: <中文描述>`，**禁止任何署名尾注**
- 编译门禁：`cd source && mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-resources -am compile`（Mac 内存紧：`MAVEN_OPTS="-Xmx1024m"`）。**Task 2 新增 PDFBox 依赖后首次编译必须去掉 `-o` 联网拉包一次**，之后恢复离线
- 单测门禁：`cd source && mvn -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test`
- schema 三件套：改种子 `source/script/sql/postgres/postgres_info_service.sql` + 增量 `deploy/updates/0.3.9-resources-reader-phase1.sql` + 登记 `deploy/updates/MANIFEST.md`
- 新表全部带 `tenant_id varchar(20) DEFAULT '000000'`（不入租户 excludes 名单）
- 单文件 ≤800 行硬限，目标 ≤400 行；系统边界必须校验输入；错误不静默
- 门户端点鉴权模式与既有 `InfoResourcePortalController` 一致：登录（Sa-Token）+ 资料可读校验，无新增权限码
- 版本行只有 MQ listener 一个写者（会话线程不写 `res_document_version`，避免并发写竞态）
- 一期作为一个发布单元交付：旧 `info_resource_note` 端点在 Plan 2 切换 UI 前保持可用，本计划不动它们

---

### Task 1: 数据库三件套（种子 + 增量迁移 + MANIFEST）

**Files:**
- Modify: `source/script/sql/postgres/postgres_info_service.sql`（文件末尾追加）
- Create: `deploy/updates/0.3.9-resources-reader-phase1.sql`
- Modify: `deploy/updates/MANIFEST.md`（版本映射表追加一行）

**Interfaces:**
- Produces: 表 `res_document_version` / `res_annotation` / `res_note`（列名即后续实体字段的 snake_case 对应）

- [ ] **Step 1: 在种子文件末尾追加三张表 DDL**

在 `postgres_info_service.sql` 末尾（现有 INSERT 种子之后）追加：

```sql
-- ============================================================
-- 资料阅读工作台一期（res_ 前缀，spec: 2026-07-11-resources-reader-notes-redesign）
-- ============================================================

CREATE TABLE IF NOT EXISTS res_document_version (
    version_id        int8         NOT NULL,
    resource_id       int8         NOT NULL,
    source_oss_id     int8         NOT NULL,
    pdf_oss_id        int8         DEFAULT NULL,
    pdf_sha256        varchar(64)  DEFAULT NULL,
    page_count        int4         DEFAULT NULL,
    text_layer_status varchar(20)  DEFAULT 'unknown' NOT NULL,
    converter         varchar(80)  DEFAULT NULL,
    status            varchar(20)  DEFAULT 'converting' NOT NULL,
    fail_reason       varchar(500) DEFAULT NULL,
    is_current        char(1)      DEFAULT '0' NOT NULL,
    tenant_id         varchar(20)  DEFAULT '000000',
    del_flag          char(1)      DEFAULT '0',
    create_dept       int8         DEFAULT NULL,
    create_by         int8         DEFAULT NULL,
    create_time       timestamp    DEFAULT NULL,
    update_by         int8         DEFAULT NULL,
    update_time       timestamp    DEFAULT NULL,
    CONSTRAINT pk_res_document_version PRIMARY KEY (version_id)
);
CREATE INDEX IF NOT EXISTS idx_res_document_version_resource ON res_document_version (resource_id, status) WHERE del_flag = '0';
CREATE UNIQUE INDEX IF NOT EXISTS uk_res_document_version_current ON res_document_version (resource_id) WHERE is_current = '1' AND del_flag = '0';
COMMENT ON TABLE res_document_version IS '资料阅读版本（固化转换产物，锚点绑定基准）';

CREATE TABLE IF NOT EXISTS res_annotation (
    annotation_id int8          NOT NULL,
    resource_id   int8          NOT NULL,
    version_id    int8          NOT NULL,
    user_id       int8          NOT NULL,
    anno_type     varchar(20)   DEFAULT 'highlight' NOT NULL,
    color         varchar(20)   DEFAULT 'yellow' NOT NULL,
    page          int4          NOT NULL,
    anchor        text          NOT NULL,
    comment_text  varchar(1000) DEFAULT NULL,
    note_id       int8          DEFAULT NULL,
    tenant_id     varchar(20)   DEFAULT '000000',
    del_flag      char(1)       DEFAULT '0',
    create_dept   int8          DEFAULT NULL,
    create_by     int8          DEFAULT NULL,
    create_time   timestamp     DEFAULT NULL,
    update_by     int8          DEFAULT NULL,
    update_time   timestamp     DEFAULT NULL,
    CONSTRAINT pk_res_annotation PRIMARY KEY (annotation_id)
);
CREATE INDEX IF NOT EXISTS idx_res_annotation_version_user ON res_annotation (version_id, user_id, page) WHERE del_flag = '0';
COMMENT ON TABLE res_annotation IS '资料阅读标注（高亮/下划线，双 selector 锚点）';

CREATE TABLE IF NOT EXISTS res_note (
    note_id      int8         NOT NULL,
    resource_id  int8         NOT NULL,
    user_id      int8         NOT NULL,
    author_name  varchar(80)  DEFAULT NULL,
    title        varchar(160) DEFAULT '默认笔记' NOT NULL,
    content      text         NOT NULL,
    content_text text         DEFAULT NULL,
    paper_style  varchar(30)  DEFAULT 'blank' NOT NULL,
    visibility   varchar(20)  DEFAULT 'private' NOT NULL,
    revision     int8         DEFAULT 0 NOT NULL,
    tenant_id    varchar(20)  DEFAULT '000000',
    del_flag     char(1)      DEFAULT '0',
    create_dept  int8         DEFAULT NULL,
    create_by    int8         DEFAULT NULL,
    create_time  timestamp    DEFAULT NULL,
    update_by    int8         DEFAULT NULL,
    update_time  timestamp    DEFAULT NULL,
    CONSTRAINT pk_res_note PRIMARY KEY (note_id)
);
CREATE INDEX IF NOT EXISTS idx_res_note_resource_user ON res_note (resource_id, user_id, update_time) WHERE del_flag = '0';
CREATE INDEX IF NOT EXISTS idx_res_note_public ON res_note (resource_id, visibility, update_time) WHERE del_flag = '0';
COMMENT ON TABLE res_note IS '资料阅读笔记（Tiptap JSON 正本）';
```

- [ ] **Step 2: 写增量迁移（同 DDL + 旧笔记数据迁移，全幂等）**

创建 `deploy/updates/0.3.9-resources-reader-phase1.sql`，内容 = Step 1 的全部 DDL（`IF NOT EXISTS` 保证幂等）+ 追加迁移段：

```sql
-- 旧笔记迁移：info_resource_note → res_note（保留主键，幂等可重跑）
INSERT INTO res_note (note_id, resource_id, user_id, author_name, title, content, content_text,
                      paper_style, visibility, revision, tenant_id, del_flag,
                      create_dept, create_by, create_time, update_by, update_time)
SELECT n.note_id, n.resource_id, n.user_id, n.author_name, '默认笔记',
       CASE WHEN coalesce(n.content, '') = ''
            THEN '{"type":"doc","content":[{"type":"paragraph"}]}'
            ELSE jsonb_build_object('type', 'doc', 'content', jsonb_build_array(
                     jsonb_build_object('type', 'paragraph', 'content', jsonb_build_array(
                         jsonb_build_object('type', 'text', 'text', n.content)))))::text
       END,
       n.content, 'blank', n.visibility, 0, n.tenant_id, n.del_flag,
       n.create_dept, n.create_by, n.create_time, n.update_by, n.update_time
FROM info_resource_note n
ON CONFLICT (note_id) DO NOTHING;
```

- [ ] **Step 3: MANIFEST 登记**

在 `deploy/updates/MANIFEST.md` 版本映射表 0.3.8 行之后追加：

```markdown
| 0.3.9 | `0.3.9-resources-reader-phase1.sql` | PG `ry-cloud` | 待投放。阅读工作台一期：res_document_version/res_annotation/res_note 三表 + info_resource_note 存量迁移（幂等；与 postgres_info_service.sql 同步维护） |
```

- [ ] **Step 4: 本地 PG 演练迁移（幂等验证：连跑两遍）**

```bash
docker ps --format '{{.Names}}' | grep -i postgres   # 找到 PG 容器名，记为 $PG
PG=<上一步输出>
docker exec -i $PG psql -U $(grep -m1 '^POSTGRES_USER' deploy/.env | cut -d= -f2) -d ry-cloud \
  < deploy/updates/0.3.9-resources-reader-phase1.sql
docker exec -i $PG psql -U $(grep -m1 '^POSTGRES_USER' deploy/.env | cut -d= -f2) -d ry-cloud \
  < deploy/updates/0.3.9-resources-reader-phase1.sql   # 第二遍必须 0 报错
docker exec -i $PG psql -U $(grep -m1 '^POSTGRES_USER' deploy/.env | cut -d= -f2) -d ry-cloud \
  -c "SELECT count(*) FROM res_note; SELECT count(*) FROM info_resource_note WHERE del_flag='0';"
```

Expected: 两遍执行均无 ERROR；两个 count 中 `res_note` ≥ `info_resource_note`（含 del_flag='1' 迁移行）。若 `.env` 无 `POSTGRES_USER`，用 `grep -i 'postgres' deploy/.env` 找实际变量名替换。

- [ ] **Step 5: Commit**

```bash
git add source/script/sql/postgres/postgres_info_service.sql deploy/updates/0.3.9-resources-reader-phase1.sql deploy/updates/MANIFEST.md
git commit -m "feat: 阅读工作台一期三表与旧笔记迁移（三件套）"
```

---

### Task 2: 实体 / Mapper / PDFBox 依赖

**Files:**
- Modify: `source/ruoyi-modules/ruoyi-portal-resources/pom.xml`
- Create: `source/ruoyi-modules/ruoyi-portal-resources/src/main/java/org/dromara/portal/resources/domain/ResDocumentVersion.java`
- Create: `.../domain/ResAnnotation.java`
- Create: `.../domain/ResNote.java`
- Create: `.../mapper/ResDocumentVersionMapper.java`、`.../mapper/ResAnnotationMapper.java`、`.../mapper/ResNoteMapper.java`

**Interfaces:**
- Produces: 三个实体类（字段名即后续所有 Task 的引用基准）+ 三个 `BaseMapperPlus` Mapper
- 参照模式: `domain/InfoResourceNote.java`（TenantEntity + @TableLogic）、`mapper/InfoResourceNoteMapper.java`

- [ ] **Step 1: pom 增加 PDFBox**

在 `ruoyi-portal-resources/pom.xml` 的 `<dependencies>` 中（`ruoyi-api-file` 之后）加：

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.3</version>
</dependency>
```

- [ ] **Step 2: 写三个实体**

`ResDocumentVersion.java`：

```java
package org.dromara.portal.resources.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("res_document_version")
public class ResDocumentVersion extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;

    public static final String STATUS_CONVERTING = "converting";
    public static final String STATUS_READY = "ready";
    public static final String STATUS_FAILED = "failed";
    public static final String TEXT_HAS = "has_text";
    public static final String TEXT_NONE = "no_text";
    public static final String TEXT_UNKNOWN = "unknown";
    public static final String CONVERTER_NATIVE_PDF = "native-pdf";

    @TableId(value = "version_id")
    private Long versionId;
    private Long resourceId;
    private Long sourceOssId;
    private Long pdfOssId;
    private String pdfSha256;
    private Integer pageCount;
    private String textLayerStatus;
    private String converter;
    private String status;
    private String failReason;
    private String isCurrent;
    @TableLogic
    private String delFlag;
}
```

`ResAnnotation.java`：

```java
package org.dromara.portal.resources.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("res_annotation")
public class ResAnnotation extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(value = "annotation_id")
    private Long annotationId;
    private Long resourceId;
    private Long versionId;
    private Long userId;
    private String annoType;
    private String color;
    private Integer page;
    private String anchor;
    private String commentText;
    private Long noteId;
    @TableLogic
    private String delFlag;
}
```

`ResNote.java`：

```java
package org.dromara.portal.resources.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.tenant.core.TenantEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("res_note")
public class ResNote extends TenantEntity {
    @Serial private static final long serialVersionUID = 1L;

    @TableId(value = "note_id")
    private Long noteId;
    private Long resourceId;
    private Long userId;
    private String authorName;
    private String title;
    private String content;
    private String contentText;
    private String paperStyle;
    private String visibility;
    private Long revision;
    @TableLogic
    private String delFlag;
}
```

- [ ] **Step 3: 写三个 Mapper**（与 `InfoResourceNoteMapper` 同模式，各一个文件）

```java
package org.dromara.portal.resources.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.portal.resources.domain.ResDocumentVersion;

public interface ResDocumentVersionMapper extends BaseMapperPlus<ResDocumentVersion, ResDocumentVersion> {
}
```

`ResAnnotationMapper` / `ResNoteMapper` 同构（替换泛型为 `ResAnnotation` / `ResNote`）。若 `InfoResourceNoteMapper` 的泛型写法不同（先打开看一眼），以仓库现状为准。

- [ ] **Step 4: 编译验证（联网一次拉 PDFBox）**

```bash
cd source && MAVEN_OPTS="-Xmx1024m" mvn -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-resources -am compile
```

Expected: BUILD SUCCESS（此后可恢复 `-o` 离线）。

- [ ] **Step 5: Commit**

```bash
git add source/ruoyi-modules/ruoyi-portal-resources
git commit -m "feat: 阅读版本/标注/笔记实体与 Mapper，引入 PDFBox"
```

---

### Task 3: PdfMetadataExtractor（TDD）

**Files:**
- Create: `source/ruoyi-modules/ruoyi-portal-resources/src/main/java/org/dromara/portal/resources/support/PdfMetadataExtractor.java`
- Test: `source/ruoyi-modules/ruoyi-portal-resources/src/test/java/org/dromara/portal/resources/support/PdfMetadataExtractorTest.java`

**Interfaces:**
- Produces: `PdfMetadataExtractor.extract(Path pdfFile)` → `PdfMetadata(String sha256, int pageCount, String textLayerStatus)`；`textLayerStatus` 取 `ResDocumentVersion.TEXT_HAS/TEXT_NONE`

- [ ] **Step 1: 写失败测试**（fixture 用 PDFBox 现场生成，不提交二进制）

```java
package org.dromara.portal.resources.support;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.dromara.portal.resources.domain.ResDocumentVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfMetadataExtractorTest {

    @TempDir Path tempDir;

    private Path createPdf(boolean withText, int pages) throws Exception {
        Path file = tempDir.resolve((withText ? "text" : "blank") + pages + ".pdf");
        try (PDDocument doc = new PDDocument()) {
            for (int i = 0; i < pages; i++) {
                PDPage page = new PDPage();
                doc.addPage(page);
                if (withText) {
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.beginText();
                        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                        cs.newLineAtOffset(50, 700);
                        cs.showText("The quick brown fox jumps over the lazy dog. 0123456789 anchor text sample.");
                        cs.endText();
                    }
                }
            }
            doc.save(file.toFile());
        }
        return file;
    }

    @Test
    void extractsPageCountShaAndTextStatus() throws Exception {
        Path pdf = createPdf(true, 2);
        PdfMetadataExtractor.PdfMetadata meta = PdfMetadataExtractor.extract(pdf);
        assertEquals(2, meta.pageCount());
        assertEquals(64, meta.sha256().length());
        assertEquals(ResDocumentVersion.TEXT_HAS, meta.textLayerStatus());
        // sha 确定性：同一文件两次提取一致
        assertEquals(meta.sha256(), PdfMetadataExtractor.extract(pdf).sha256());
    }

    @Test
    void detectsMissingTextLayer() throws Exception {
        Path pdf = createPdf(false, 1);
        PdfMetadataExtractor.PdfMetadata meta = PdfMetadataExtractor.extract(pdf);
        assertEquals(1, meta.pageCount());
        assertEquals(ResDocumentVersion.TEXT_NONE, meta.textLayerStatus());
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd source && mvn -o -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test -Dtest=PdfMetadataExtractorTest
```

Expected: 编译失败 `cannot find symbol: PdfMetadataExtractor`。

- [ ] **Step 3: 最小实现**

```java
package org.dromara.portal.resources.support;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.portal.resources.domain.ResDocumentVersion;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

/** 阅读版本元数据提取：sha256 / 页数 / 文本层探测（前 N 页抽样） */
public final class PdfMetadataExtractor {

    private static final int TEXT_SAMPLE_PAGES = 5;
    private static final int TEXT_MIN_CHARS = 50;

    private PdfMetadataExtractor() {
    }

    public record PdfMetadata(String sha256, int pageCount, String textLayerStatus) {
    }

    public static PdfMetadata extract(Path pdfFile) {
        try (PDDocument doc = Loader.loadPDF(pdfFile.toFile())) {
            int pageCount = doc.getNumberOfPages();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(Math.min(TEXT_SAMPLE_PAGES, Math.max(1, pageCount)));
            String sample = stripper.getText(doc);
            String textStatus = sample.replaceAll("\\s", "").length() >= TEXT_MIN_CHARS
                ? ResDocumentVersion.TEXT_HAS
                : ResDocumentVersion.TEXT_NONE;
            return new PdfMetadata(sha256Of(pdfFile), pageCount, textStatus);
        } catch (IOException e) {
            throw new ServiceException("PDF 元数据提取失败：" + e.getMessage());
        }
    }

    private static String sha256Of(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new ServiceException("SHA-256 不可用");
        }
    }
}
```

- [ ] **Step 4: 跑测试确认通过**（同 Step 2 命令）Expected: `Tests run: 2, Failures: 0`。

- [ ] **Step 5: Commit**

```bash
git add source/ruoyi-modules/ruoyi-portal-resources
git commit -m "feat: PDF 元数据提取器（sha256/页数/文本层探测）"
```

---

### Task 4: AnchorValidator + NoteContentValidator（TDD，系统边界校验）

**Files:**
- Create: `.../support/AnchorValidator.java`
- Create: `.../support/NoteContentValidator.java`
- Test: `.../test/java/org/dromara/portal/resources/support/AnchorValidatorTest.java`
- Test: `.../test/java/org/dromara/portal/resources/support/NoteContentValidatorTest.java`

**Interfaces:**
- Produces: `AnchorValidator.validate(String anchorJson, Integer pageCount)` → 返回合法的 `page`（int）；非法抛 `ServiceException`
- Produces: `NoteContentValidator.validate(String contentJson)` → 校验白名单与大小；`NoteContentValidator.extractText(String contentJson)` → 纯文本（供 `content_text`）
- 白名单（spec §5.4）：节点 `doc,paragraph,text,heading,bulletList,orderedList,listItem,blockquote,codeBlock,hardBreak,horizontalRule,sourceQuote`；mark `bold,italic,strike,underline,highlight,link,code`

- [ ] **Step 1: 写失败测试**

`AnchorValidatorTest.java`：

```java
package org.dromara.portal.resources.support;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnchorValidatorTest {

    private String anchor(int page, String exact, String rects) {
        return """
            {"v":1,"sha256":"%s","page":%d,
             "quote":{"exact":"%s","prefix":"p","suffix":"s"},
             "position":{"startItem":1,"startOffset":0,"endItem":1,"endOffset":4},
             "rects":%s}
            """.formatted("a".repeat(64), page, exact, rects);
    }

    @Test
    void acceptsValidAnchorAndReturnsPage() {
        int page = AnchorValidator.validate(anchor(3, "选中文字", "[{\"x\":0.1,\"y\":0.2,\"w\":0.3,\"h\":0.02}]"), 10);
        assertEquals(3, page);
    }

    @Test
    void rejectsPageOutOfRange() {
        assertThrows(ServiceException.class, () -> AnchorValidator.validate(anchor(11, "文字", "[]"), 10));
        assertThrows(ServiceException.class, () -> AnchorValidator.validate(anchor(0, "文字", "[]"), 10));
    }

    @Test
    void rejectsOversizeQuoteAndBadRects() {
        assertThrows(ServiceException.class, () -> AnchorValidator.validate(anchor(1, "长".repeat(1001), "[]"), 10));
        assertThrows(ServiceException.class,
            () -> AnchorValidator.validate(anchor(1, "文字", "[{\"x\":1.5,\"y\":0,\"w\":0.1,\"h\":0.1}]"), 10));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(ServiceException.class, () -> AnchorValidator.validate("{not json", 10));
        assertThrows(ServiceException.class, () -> AnchorValidator.validate(null, 10));
    }
}
```

`NoteContentValidatorTest.java`：

```java
package org.dromara.portal.resources.support;

import org.dromara.common.core.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoteContentValidatorTest {

    private static final String VALID = """
        {"type":"doc","content":[{"type":"paragraph","content":[
          {"type":"text","text":"读书","marks":[{"type":"bold"}]},
          {"type":"text","text":"笔记"}]}]}
        """;

    @Test
    void acceptsWhitelistedDocAndExtractsText() {
        assertDoesNotThrow(() -> NoteContentValidator.validate(VALID));
        assertEquals("读书笔记", NoteContentValidator.extractText(VALID));
    }

    @Test
    void rejectsNonWhitelistedNodeAndMark() {
        String iframe = "{\"type\":\"doc\",\"content\":[{\"type\":\"iframe\"}]}";
        assertThrows(ServiceException.class, () -> NoteContentValidator.validate(iframe));
        String badMark = """
            {"type":"doc","content":[{"type":"paragraph","content":[
              {"type":"text","text":"x","marks":[{"type":"script"}]}]}]}
            """;
        assertThrows(ServiceException.class, () -> NoteContentValidator.validate(badMark));
    }

    @Test
    void rejectsOversizeAndMalformed() {
        String big = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\""
            + "字".repeat(200_000) + "\"}]}]}";
        assertThrows(ServiceException.class, () -> NoteContentValidator.validate(big));
        assertThrows(ServiceException.class, () -> NoteContentValidator.validate("{"));
        assertThrows(ServiceException.class, () -> NoteContentValidator.validate(null));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd source && mvn -o -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test -Dtest='AnchorValidatorTest,NoteContentValidatorTest'
```

Expected: 编译失败（两个类不存在）。

- [ ] **Step 3: 实现两个校验器**（JSON 解析用仓库已有的 Jackson，经 `ObjectMapper`）

`AnchorValidator.java`：

```java
package org.dromara.portal.resources.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;

/** 锚点边界校验（spec §3.4）：结构/长度/取值范围。quote 为主锚必填，position/rects 可缺省。 */
public final class AnchorValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_ANCHOR_BYTES = 16 * 1024;
    private static final int MAX_EXACT = 1000;
    private static final int MAX_AFFIX = 64;
    private static final int MAX_RECTS = 64;

    private AnchorValidator() {
    }

    public static int validate(String anchorJson, Integer pageCount) {
        if (anchorJson == null || anchorJson.isBlank()) {
            throw new ServiceException("锚点不能为空");
        }
        if (anchorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_ANCHOR_BYTES) {
            throw new ServiceException("锚点数据过大");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(anchorJson);
        } catch (Exception e) {
            throw new ServiceException("锚点格式非法");
        }
        if (root.path("v").asInt() != 1) {
            throw new ServiceException("锚点版本不支持");
        }
        String sha = root.path("sha256").asText("");
        if (!sha.matches("[0-9a-f]{64}")) {
            throw new ServiceException("锚点缺少有效的文档指纹");
        }
        int page = root.path("page").asInt(0);
        if (page < 1 || (pageCount != null && page > pageCount)) {
            throw new ServiceException("锚点页码超出范围");
        }
        JsonNode quote = root.path("quote");
        String exact = quote.path("exact").asText("");
        if (exact.isEmpty() || exact.length() > MAX_EXACT) {
            throw new ServiceException("锚点引文缺失或过长");
        }
        if (quote.path("prefix").asText("").length() > MAX_AFFIX
            || quote.path("suffix").asText("").length() > MAX_AFFIX) {
            throw new ServiceException("锚点前后文过长");
        }
        JsonNode rects = root.path("rects");
        if (rects.isArray()) {
            if (rects.size() > MAX_RECTS) {
                throw new ServiceException("锚点矩形数量过多");
            }
            for (JsonNode rect : rects) {
                for (String key : new String[]{"x", "y", "w", "h"}) {
                    double v = rect.path(key).asDouble(-1);
                    if (v < 0 || v > 1) {
                        throw new ServiceException("锚点矩形坐标必须为 0~1 归一化值");
                    }
                }
            }
        }
        return page;
    }
}
```

`NoteContentValidator.java`：

```java
package org.dromara.portal.resources.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dromara.common.core.exception.ServiceException;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/** 笔记正文校验（spec §5.4）：Tiptap JSON 节点/mark 白名单 + 512KB 上限；extractText 供 content_text。 */
public final class NoteContentValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int MAX_CONTENT_BYTES = 512 * 1024;
    private static final Set<String> NODE_WHITELIST = Set.of(
        "doc", "paragraph", "text", "heading", "bulletList", "orderedList", "listItem",
        "blockquote", "codeBlock", "hardBreak", "horizontalRule", "sourceQuote");
    private static final Set<String> MARK_WHITELIST = Set.of(
        "bold", "italic", "strike", "underline", "highlight", "link", "code");

    private NoteContentValidator() {
    }

    public static void validate(String contentJson) {
        JsonNode root = parse(contentJson);
        if (!"doc".equals(root.path("type").asText())) {
            throw new ServiceException("笔记内容必须是 Tiptap doc");
        }
        walk(root);
    }

    public static String extractText(String contentJson) {
        StringBuilder sb = new StringBuilder();
        collectText(parse(contentJson), sb);
        return sb.toString();
    }

    private static JsonNode parse(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            throw new ServiceException("笔记内容不能为空");
        }
        if (contentJson.getBytes(StandardCharsets.UTF_8).length > MAX_CONTENT_BYTES) {
            throw new ServiceException("笔记内容超出 512KB 上限");
        }
        try {
            return MAPPER.readTree(contentJson);
        } catch (Exception e) {
            throw new ServiceException("笔记内容格式非法");
        }
    }

    private static void walk(JsonNode node) {
        String type = node.path("type").asText("");
        if (!NODE_WHITELIST.contains(type)) {
            throw new ServiceException("笔记包含不允许的节点类型：" + type);
        }
        for (JsonNode mark : node.path("marks")) {
            if (!MARK_WHITELIST.contains(mark.path("type").asText(""))) {
                throw new ServiceException("笔记包含不允许的样式类型");
            }
        }
        for (JsonNode child : node.path("content")) {
            walk(child);
        }
    }

    private static void collectText(JsonNode node, StringBuilder sb) {
        if (node.hasNonNull("text")) {
            sb.append(node.get("text").asText());
        }
        for (JsonNode child : node.path("content")) {
            collectText(child, sb);
        }
    }
}
```

- [ ] **Step 4: 跑测试确认通过**（同 Step 2 命令）Expected: `Tests run: 7, Failures: 0`。

- [ ] **Step 5: Commit**

```bash
git add source/ruoyi-modules/ruoyi-portal-resources
git commit -m "feat: 锚点与笔记内容边界校验器"
```

---

### Task 5: 阅读版本服务 + 转换链路扩展

**Files:**
- Create: `.../service/IReaderVersionService.java`、`.../service/impl/ReaderVersionServiceImpl.java`
- Create: `.../support/ReaderFormats.java`
- Modify: `.../service/IInfoResourceService.java`（暴露 2 个既有私有能力）
- Modify: `.../service/impl/InfoResourceServiceImpl.java`（`getPortalReadableResource`/`enqueueConversion` 改公开实现接口方法；`PDF_CONVERTIBLE_SUFFIXES` 移至 `ReaderFormats`）
- Modify: `.../mq/ResourceConvertListener.java`
- Modify: `.../src/test/java/org/dromara/portal/resources/mq/ResourceConvertListenerTest.java`

**Interfaces:**
- Consumes: `PdfMetadataExtractor.extract(Path)`（Task 3）、`DocumentPreviewConverter.ensurePdfPreviewFile(InfoResource, RemoteFile, String)`（既有）、`RemoteFileService.upload(String name, String originalFilename, String contentType, byte[] file)`（既有 Dubbo）
- Produces:
  - `IInfoResourceService.requirePortalReadable(Long resourceId)` → `InfoResource`（原私有 `getPortalReadableResource` 直接改名公开）
  - `IInfoResourceService.enqueueConversion(Long resourceId)` → `void`（原私有方法公开；Redis 幂等键逻辑不变）
  - `IReaderVersionService.buildCurrentVersion(Long resourceId)` → `ResDocumentVersion`（listener 专用，唯一写者）
  - `IReaderVersionService.markFailed(Long resourceId, String reason)` → `void`
  - `IReaderVersionService.findCurrent(Long resourceId)` → `Optional<ResDocumentVersion>`（status=ready 且 is_current='1'）
  - `ReaderFormats.resolveSuffix(InfoResource)` → `String`；`ReaderFormats.isNativePdf(String)`；`ReaderFormats.isConvertible(String)`

- [ ] **Step 1: `ReaderFormats` 支撑类**（把 `InfoResourceServiceImpl.PDF_CONVERTIBLE_SUFFIXES` 常量整体搬来，原处改引用 `ReaderFormats.PDF_CONVERTIBLE_SUFFIXES`）

```java
package org.dromara.portal.resources.support;

import org.dromara.common.core.utils.StringUtils;
import org.dromara.portal.resources.domain.InfoResource;

import java.util.Set;

/** 阅读格式判定：与旧预览链路共用同一份可转换后缀集合 */
public final class ReaderFormats {

    public static final Set<String> PDF_CONVERTIBLE_SUFFIXES = Set.of(
        "doc", "docx", "docm", "dot", "dotx", "dotm",
        "xls", "xlsx", "xlsm", "xlt", "xltx", "xltm", "csv", "tsv",
        "ppt", "pptx", "pptm", "pps", "ppsx",
        "wps", "wpt", "et", "ett", "dps", "dpt",
        "odt", "ods", "odp", "rtf");

    private ReaderFormats() {
    }

    public static String resolveSuffix(InfoResource resource) {
        String suffix = StringUtils.defaultString(resource.getFileSuffix()).toLowerCase().replace(".", "");
        if (!suffix.isEmpty()) {
            return suffix;
        }
        String name = StringUtils.defaultString(resource.getOriginalName()).toLowerCase();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1) : "";
    }

    public static boolean isNativePdf(String suffix) {
        return "pdf".equals(suffix);
    }

    public static boolean isConvertible(String suffix) {
        return PDF_CONVERTIBLE_SUFFIXES.contains(suffix);
    }
}
```

- [ ] **Step 2: 暴露既有私有能力**

`IInfoResourceService.java` 追加两个方法声明（javadoc 注明「reader 子域使用」）；`InfoResourceServiceImpl` 中：
- `private InfoResource getPortalReadableResource(Long)` → `public InfoResource requirePortalReadable(Long)`（加 `@Override`，全部原调用点同步改名）
- `private void enqueueConversion(Long)` → `public void enqueueConversion(Long)`（加 `@Override`）
- 删除类内 `PDF_CONVERTIBLE_SUFFIXES` 常量，引用 `ReaderFormats.PDF_CONVERTIBLE_SUFFIXES`

- [ ] **Step 3: 版本服务实现**

`IReaderVersionService.java`：

```java
package org.dromara.portal.resources.service;

import org.dromara.portal.resources.domain.ResDocumentVersion;

import java.util.Optional;

public interface IReaderVersionService {

    /** MQ listener 专用（版本行唯一写者）：转换→固化 OSS→写版本行，幂等 */
    ResDocumentVersion buildCurrentVersion(Long resourceId);

    void markFailed(Long resourceId, String reason);

    Optional<ResDocumentVersion> findCurrent(Long resourceId);
}
```

`ReaderVersionServiceImpl.java`：

```java
package org.dromara.portal.resources.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.file.api.RemoteFileService;
import org.dromara.file.api.domain.RemoteFile;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.ResDocumentVersion;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.portal.resources.mapper.ResDocumentVersionMapper;
import org.dromara.portal.resources.service.IReaderVersionService;
import org.dromara.portal.resources.support.DocumentPreviewConverter;
import org.dromara.portal.resources.support.PdfMetadataExtractor;
import org.dromara.portal.resources.support.ReaderFormats;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReaderVersionServiceImpl implements IReaderVersionService {

    private final ResDocumentVersionMapper versionMapper;
    private final InfoResourceMapper resourceMapper;
    private final DocumentPreviewConverter previewConverter;

    @DubboReference
    private RemoteFileService remoteFileService;

    @Value("${infoservice.reader.converter-tag:libreoffice:24.2}")
    private String converterTag;

    /** 事务放在接口方法上（listener 经 Spring 代理调用才生效；内部自调用不加注解）。
     *  队列 prefetch=1 串行消费，无并发写者，长事务包含文件 IO 在此规模可接受。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResDocumentVersion buildCurrentVersion(Long resourceId) {
        InfoResource resource = resourceMapper.selectById(resourceId);
        if (resource == null) {
            throw new ServiceException("资料不存在：" + resourceId);
        }
        RemoteFile remoteFile = resolveRemoteFile(resource);
        String suffix = ReaderFormats.resolveSuffix(resource);
        String tag = ReaderFormats.isNativePdf(suffix) ? ResDocumentVersion.CONVERTER_NATIVE_PDF : converterTag;

        Optional<ResDocumentVersion> reusable = findReusable(resourceId, remoteFile.getOssId(), tag);
        if (reusable.isPresent()) {
            switchCurrent(resourceId, reusable.get().getVersionId());
            return reusable.get();
        }

        try {
            if (ReaderFormats.isNativePdf(suffix)) {
                Path temp = resolveStorage(remoteFile).fileDownload(remoteFile.getName());
                try {
                    return persistReady(resource, remoteFile, remoteFile.getOssId(), temp, tag);
                } finally {
                    Files.deleteIfExists(temp);
                }
            }
            if (!ReaderFormats.isConvertible(suffix)) {
                throw new ServiceException("该格式暂不支持在线阅读");
            }
            Path pdf = previewConverter.ensurePdfPreviewFile(resource, remoteFile, suffix);
            RemoteFile artifact = remoteFileService.upload(
                "reader-" + resourceId + ".pdf",
                stripSuffix(resource.getOriginalName()) + ".pdf",
                "application/pdf",
                Files.readAllBytes(pdf));
            return persistReady(resource, remoteFile, artifact.getOssId(), pdf, tag);
        } catch (IOException e) {
            throw new ServiceException("转换产物读取失败");
        }
        // 注意：失败落库由 listener 在事务外调用 markFailed（此处 @Transactional 回滚会吞掉 catch 内的写入）
    }

    @Override
    public void markFailed(Long resourceId, String reason) {
        ResDocumentVersion failed = new ResDocumentVersion();
        failed.setResourceId(resourceId);
        failed.setSourceOssId(resolveSourceOssId(resourceId));
        failed.setStatus(ResDocumentVersion.STATUS_FAILED);
        failed.setFailReason(StringUtils.substring(StringUtils.defaultString(reason), 0, 480));
        failed.setIsCurrent("0");
        failed.setTextLayerStatus(ResDocumentVersion.TEXT_UNKNOWN);
        versionMapper.insert(failed);
    }

    @Override
    public Optional<ResDocumentVersion> findCurrent(Long resourceId) {
        return Optional.ofNullable(versionMapper.selectOne(Wrappers.<ResDocumentVersion>lambdaQuery()
            .eq(ResDocumentVersion::getResourceId, resourceId)
            .eq(ResDocumentVersion::getIsCurrent, "1")
            .eq(ResDocumentVersion::getStatus, ResDocumentVersion.STATUS_READY)));
    }

    private Optional<ResDocumentVersion> findReusable(Long resourceId, Long sourceOssId, String tag) {
        return versionMapper.selectList(Wrappers.<ResDocumentVersion>lambdaQuery()
                .eq(ResDocumentVersion::getResourceId, resourceId)
                .eq(ResDocumentVersion::getSourceOssId, sourceOssId)
                .eq(ResDocumentVersion::getConverter, tag)
                .eq(ResDocumentVersion::getStatus, ResDocumentVersion.STATUS_READY))
            .stream().findFirst();
    }

    private ResDocumentVersion persistReady(InfoResource resource, RemoteFile source,
                                            Long pdfOssId, Path pdfFile, String tag) {
        PdfMetadataExtractor.PdfMetadata meta = PdfMetadataExtractor.extract(pdfFile);
        ResDocumentVersion version = new ResDocumentVersion();
        version.setResourceId(resource.getResourceId());
        version.setSourceOssId(source.getOssId());
        version.setPdfOssId(pdfOssId);
        version.setPdfSha256(meta.sha256());
        version.setPageCount(meta.pageCount());
        version.setTextLayerStatus(meta.textLayerStatus());
        version.setConverter(tag);
        version.setStatus(ResDocumentVersion.STATUS_READY);
        version.setIsCurrent("0");
        versionMapper.insert(version);
        switchCurrent(resource.getResourceId(), version.getVersionId());
        version.setIsCurrent("1");
        return version;
    }

    private void switchCurrent(Long resourceId, Long versionId) {
        versionMapper.update(null, Wrappers.<ResDocumentVersion>lambdaUpdate()
            .set(ResDocumentVersion::getIsCurrent, "0")
            .eq(ResDocumentVersion::getResourceId, resourceId)
            .eq(ResDocumentVersion::getIsCurrent, "1"));
        versionMapper.update(null, Wrappers.<ResDocumentVersion>lambdaUpdate()
            .set(ResDocumentVersion::getIsCurrent, "1")
            .eq(ResDocumentVersion::getVersionId, versionId));
    }

    private Long resolveSourceOssId(Long resourceId) {
        InfoResource resource = resourceMapper.selectById(resourceId);
        return resource == null ? -1L : resource.getOssId();
    }

    private RemoteFile resolveRemoteFile(InfoResource resource) {
        var files = remoteFileService.selectByIds(String.valueOf(resource.getOssId()));
        if (files == null || files.isEmpty()) {
            throw new ServiceException("资料文件不存在或已删除");
        }
        return files.get(0);
    }

    private OssClient resolveStorage(RemoteFile remoteFile) {
        return StringUtils.isBlank(remoteFile.getService())
            ? OssFactory.instance()
            : OssFactory.instance(remoteFile.getService());
    }

    private String stripSuffix(String name) {
        String n = StringUtils.defaultString(name, "document");
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
```

注意：`InfoResourceServiceImpl` 中已有同名私有工具（`resolveRemoteFile`/`resolveStorage`），实现时**对照现状**——若其签名可复用则通过接口暴露复用，不重复实现；以上代码为独立实现的兜底形态。

- [ ] **Step 4: listener 扩展 + 更新既有测试**

`ResourceConvertListener.onConvert` 的 try 块改为：

```java
try {
    resourceService.ensurePreviewConverted(message.getResourceId());
    readerVersionService.buildCurrentVersion(message.getResourceId());
    channel.basicAck(deliveryTag, false);
} catch (Exception e) {
    log.error("资料转换失败，转入死信 message={}", message, e);
    tryMarkFailed(message.getResourceId(), e.getMessage());
    channel.basicNack(deliveryTag, false, false);
}
```

同类中追加私有方法（失败落库不能反过来阻断 nack）：

```java
private void tryMarkFailed(Long resourceId, String reason) {
    try {
        readerVersionService.markFailed(resourceId, reason);
    } catch (Exception ex) {
        log.error("记录转换失败状态时出错 resourceId={}", resourceId, ex);
    }
}
```

（构造注入 `IReaderVersionService readerVersionService`；`buildCurrentVersion` 是事务方法，其异常路径的失败落库必须由 listener 在事务外完成。）

`ResourceConvertListenerTest`：先读现有用例结构，追加/调整两个断言场景——成功路径 verify `readerVersionService.buildCurrentVersion` 被调用且 `basicAck`；`buildCurrentVersion` 抛异常路径 verify `markFailed` 被调用且 `basicNack(deliveryTag, false, false)`。mock 风格与现有用例一致（Mockito）。

- [ ] **Step 5: 跑模块全部测试**

```bash
cd source && mvn -o -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test
```

Expected: BUILD SUCCESS，无失败用例（含既有 DocumentPreviewConverterTest）。

- [ ] **Step 6: Commit**

```bash
git add source/ruoyi-modules/ruoyi-portal-resources
git commit -m "feat: 阅读版本固化服务与转换链路扩展"
```

---

### Task 6: 会话状态机 + 版本文件端点

**Files:**
- Create: `.../service/IReaderSessionService.java`、`.../service/impl/ReaderSessionServiceImpl.java`
- Create: `.../domain/vo/ReaderSessionVo.java`
- Create: `.../controller/portal/ReaderSessionController.java`

**Interfaces:**
- Consumes: `IInfoResourceService.requirePortalReadable/enqueueConversion`（Task 5）、`IReaderVersionService.findCurrent`（Task 5）、`ReaderFormats`（Task 5）
- Produces（Plan 2 前端契约）:
  - `GET /portal/resources/reader/{resourceId}/session[?retry=true]` → `R<ReaderSessionVo>`，`ReaderSessionVo{Long resourceId, String status("converting"|"ready"|"failed"|"unsupported"), Long versionId, String sha256, Integer pageCount, String textLayerStatus, String fileUrl, String failReason, String title}`；ready 时 `fileUrl = "/portal/resources/reader/version/{versionId}/file"`
  - `GET /portal/resources/reader/version/{versionId}/file` → PDF 流（`Cache-Control: private, max-age=31536000, immutable`）

- [ ] **Step 1: VO + 服务实现**

`ReaderSessionVo.java`：

```java
package org.dromara.portal.resources.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ReaderSessionVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    public static final String STATUS_UNSUPPORTED = "unsupported";

    private Long resourceId;
    private String title;
    private String status;
    private Long versionId;
    private String sha256;
    private Integer pageCount;
    private String textLayerStatus;
    private String fileUrl;
    private String failReason;
}
```

`IReaderSessionService.java`：

```java
package org.dromara.portal.resources.service;

import jakarta.servlet.http.HttpServletResponse;
import org.dromara.portal.resources.domain.vo.ReaderSessionVo;

import java.io.IOException;

public interface IReaderSessionService {

    ReaderSessionVo openSession(Long resourceId, boolean retry);

    void streamVersionFile(Long versionId, HttpServletResponse response) throws IOException;
}
```

`ReaderSessionServiceImpl.java`：

```java
package org.dromara.portal.resources.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.file.api.RemoteFileService;
import org.dromara.file.api.domain.RemoteFile;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.ResDocumentVersion;
import org.dromara.portal.resources.domain.vo.ReaderSessionVo;
import org.dromara.portal.resources.mapper.ResDocumentVersionMapper;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.dromara.portal.resources.service.IReaderSessionService;
import org.dromara.portal.resources.service.IReaderVersionService;
import org.dromara.portal.resources.support.ReaderFormats;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ReaderSessionServiceImpl implements IReaderSessionService {

    private final IInfoResourceService resourceService;
    private final IReaderVersionService versionService;
    private final ResDocumentVersionMapper versionMapper;

    @DubboReference
    private RemoteFileService remoteFileService;

    @Override
    public ReaderSessionVo openSession(Long resourceId, boolean retry) {
        InfoResource resource = resourceService.requirePortalReadable(resourceId);
        ReaderSessionVo vo = new ReaderSessionVo();
        vo.setResourceId(resourceId);
        vo.setTitle(resource.getTitle());

        Optional<ResDocumentVersion> current = versionService.findCurrent(resourceId);
        if (current.isPresent()) {
            ResDocumentVersion v = current.get();
            vo.setStatus(ResDocumentVersion.STATUS_READY);
            vo.setVersionId(v.getVersionId());
            vo.setSha256(v.getPdfSha256());
            vo.setPageCount(v.getPageCount());
            vo.setTextLayerStatus(v.getTextLayerStatus());
            vo.setFileUrl("/portal/resources/reader/version/" + v.getVersionId() + "/file");
            return vo;
        }

        String suffix = ReaderFormats.resolveSuffix(resource);
        if (!ReaderFormats.isNativePdf(suffix) && !ReaderFormats.isConvertible(suffix)) {
            vo.setStatus(ReaderSessionVo.STATUS_UNSUPPORTED);
            vo.setFailReason("该格式暂不支持在线阅读，请下载后查看");
            return vo;
        }

        ResDocumentVersion latest = versionMapper.selectList(Wrappers.<ResDocumentVersion>lambdaQuery()
                .eq(ResDocumentVersion::getResourceId, resourceId)
                .orderByDesc(ResDocumentVersion::getVersionId))
            .stream().findFirst().orElse(null);
        if (latest != null && ResDocumentVersion.STATUS_FAILED.equals(latest.getStatus()) && !retry) {
            vo.setStatus(ResDocumentVersion.STATUS_FAILED);
            vo.setFailReason(latest.getFailReason());
            return vo;
        }

        resourceService.enqueueConversion(resourceId);
        vo.setStatus(ResDocumentVersion.STATUS_CONVERTING);
        return vo;
    }

    @Override
    public void streamVersionFile(Long versionId, HttpServletResponse response) throws IOException {
        ResDocumentVersion version = versionMapper.selectById(versionId);
        if (version == null || !ResDocumentVersion.STATUS_READY.equals(version.getStatus())) {
            throw new ServiceException("阅读版本不存在或未就绪");
        }
        resourceService.requirePortalReadable(version.getResourceId());
        var files = remoteFileService.selectByIds(String.valueOf(version.getPdfOssId()));
        if (files == null || files.isEmpty()) {
            throw new ServiceException("阅读文件已丢失，请重新打开资料");
        }
        RemoteFile file = files.get(0);
        response.setContentType("application/pdf");
        response.setHeader("Cache-Control", "private, max-age=31536000, immutable");
        resolveStorage(file).download(file.getName(), response.getOutputStream(), response::setContentLengthLong);
    }

    private OssClient resolveStorage(RemoteFile remoteFile) {
        return StringUtils.isBlank(remoteFile.getService())
            ? OssFactory.instance()
            : OssFactory.instance(remoteFile.getService());
    }
}
```

- [ ] **Step 2: 控制器**

`ReaderSessionController.java`：

```java
package org.dromara.portal.resources.controller.portal;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.portal.resources.domain.vo.ReaderSessionVo;
import org.dromara.portal.resources.service.IReaderSessionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/resources/reader")
public class ReaderSessionController {

    private final IReaderSessionService sessionService;

    @GetMapping("/{resourceId}/session")
    public R<ReaderSessionVo> session(@PathVariable Long resourceId,
                                      @RequestParam(defaultValue = "false") boolean retry) {
        return R.ok(sessionService.openSession(resourceId, retry));
    }

    @GetMapping("/version/{versionId}/file")
    public void file(@PathVariable Long versionId, HttpServletResponse response) throws IOException {
        sessionService.streamVersionFile(versionId, response);
    }
}
```

- [ ] **Step 3: 编译 + 全模块测试**

```bash
cd source && mvn -o -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test
```

Expected: BUILD SUCCESS。

- [ ] **Step 4: Commit**

```bash
git add source/ruoyi-modules/ruoyi-portal-resources
git commit -m "feat: 阅读会话状态机与版本文件端点"
```

---

### Task 7: 标注 CRUD

**Files:**
- Create: `.../domain/bo/ResAnnotationBo.java`、`.../domain/vo/ResAnnotationVo.java`
- Create: `.../service/IResourceAnnotationService.java`、`.../service/impl/ResourceAnnotationServiceImpl.java`
- Create: `.../controller/portal/ResourceAnnotationController.java`
- Create: `.../support/WriteRateGuard.java`
- Test: `.../test/java/org/dromara/portal/resources/service/ResourceAnnotationOwnershipTest.java`

**Interfaces:**
- Consumes: `AnchorValidator.validate`（Task 4）、`IReaderVersionService.findCurrent`（Task 5）、`IInfoResourceService.requirePortalReadable`（Task 5）
- Produces（Plan 2 前端契约）:
  - `GET /portal/resources/reader/version/{versionId}/annotations` → `R<List<ResAnnotationVo>>`（当前用户全部标注，按 page,annotation_id 排序）
  - `POST /portal/resources/reader/annotations`（body=`ResAnnotationBo{versionId*, annoType*, color*, anchor*, commentText?, noteId?}`）→ `R<ResAnnotationVo>`
  - `PUT /portal/resources/reader/annotations/{annotationId}`（只允许改 color/commentText/noteId）→ `R<Void>`
  - `DELETE /portal/resources/reader/annotations/{annotationId}` → `R<Void>`
  - `ResAnnotationVo` 字段 = 实体字段去掉 tenant/del_flag（annotationId, resourceId, versionId, annoType, color, page, anchor, commentText, noteId, createTime, updateTime）
- Produces: `WriteRateGuard.check(String scene, Long userId, int perMinuteLimit)`（Redis 固定窗口，超限抛 `ServiceException("操作过于频繁，请稍后再试")`）

- [ ] **Step 1: 写失败测试（归属与输入校验的核心逻辑）**

```java
package org.dromara.portal.resources.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.portal.resources.domain.ResAnnotation;
import org.dromara.portal.resources.service.impl.ResourceAnnotationServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceAnnotationOwnershipTest {

    @Test
    void rejectsForeignAnnotation() {
        ResAnnotation mine = new ResAnnotation();
        mine.setUserId(100L);
        assertDoesNotThrow(() -> ResourceAnnotationServiceImpl.assertOwner(mine, 100L));
        assertThrows(ServiceException.class, () -> ResourceAnnotationServiceImpl.assertOwner(mine, 200L));
        assertThrows(ServiceException.class, () -> ResourceAnnotationServiceImpl.assertOwner(null, 100L));
    }

    @Test
    void allowedEnumsOnly() {
        assertDoesNotThrow(() -> ResourceAnnotationServiceImpl.assertEnums("highlight", "yellow"));
        assertDoesNotThrow(() -> ResourceAnnotationServiceImpl.assertEnums("underline", "purple"));
        assertThrows(ServiceException.class, () -> ResourceAnnotationServiceImpl.assertEnums("blink", "yellow"));
        assertThrows(ServiceException.class, () -> ResourceAnnotationServiceImpl.assertEnums("highlight", "#ff0"));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd source && mvn -o -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test -Dtest=ResourceAnnotationOwnershipTest
```

Expected: 编译失败（类不存在）。

- [ ] **Step 3: 实现**

`WriteRateGuard.java`（先 `grep -rn "incrAtomicValue" source/ruoyi-common/ruoyi-common-redis` 确认方法名，不存在则用 `RAtomicLong`：`RedisUtils.getClient().getAtomicLong(key)`）：

```java
package org.dromara.portal.resources.support;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.redis.utils.RedisUtils;

import java.time.Duration;

/** 写接口基础限流：按用户+场景的分钟固定窗口（spec §5.4 错误矩阵） */
public final class WriteRateGuard {

    private WriteRateGuard() {
    }

    public static void check(String scene, Long userId, int perMinuteLimit) {
        String key = "resources:reader:rate:" + scene + ":" + userId + ":" + (System.currentTimeMillis() / 60_000);
        long count = RedisUtils.incrAtomicValue(key);
        if (count == 1) {
            RedisUtils.expire(key, Duration.ofMinutes(2));
        }
        if (count > perMinuteLimit) {
            throw new ServiceException("操作过于频繁，请稍后再试");
        }
    }
}
```

`ResAnnotationBo.java`：

```java
package org.dromara.portal.resources.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.portal.resources.domain.ResAnnotation;

@Data
@AutoMapper(target = ResAnnotation.class, reverseConvertGenerate = false)
public class ResAnnotationBo {

    private Long annotationId;

    @NotNull(message = "版本不能为空")
    private Long versionId;

    @NotBlank(message = "标注类型不能为空")
    private String annoType;

    @NotBlank(message = "标注颜色不能为空")
    private String color;

    @NotBlank(message = "锚点不能为空")
    private String anchor;

    @Size(max = 1000, message = "批注最多 1000 字")
    private String commentText;

    private Long noteId;
}
```

`ResAnnotationVo.java`（`@AutoMapper(target = ResAnnotation.class)`，字段见 Interfaces 清单，均为简单私有字段 + `@Data`，实现照 `InfoResourceNoteVo` 模式）。

`IResourceAnnotationService.java`：

```java
package org.dromara.portal.resources.service;

import org.dromara.portal.resources.domain.bo.ResAnnotationBo;
import org.dromara.portal.resources.domain.vo.ResAnnotationVo;

import java.util.List;

public interface IResourceAnnotationService {

    List<ResAnnotationVo> listMine(Long versionId);

    ResAnnotationVo create(ResAnnotationBo bo);

    void update(Long annotationId, ResAnnotationBo bo);

    void delete(Long annotationId);
}
```

`ResourceAnnotationServiceImpl.java`：

```java
package org.dromara.portal.resources.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.resources.domain.ResAnnotation;
import org.dromara.portal.resources.domain.ResDocumentVersion;
import org.dromara.portal.resources.domain.bo.ResAnnotationBo;
import org.dromara.portal.resources.domain.vo.ResAnnotationVo;
import org.dromara.portal.resources.mapper.ResAnnotationMapper;
import org.dromara.portal.resources.mapper.ResDocumentVersionMapper;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.dromara.portal.resources.service.IResourceAnnotationService;
import org.dromara.portal.resources.support.AnchorValidator;
import org.dromara.portal.resources.support.WriteRateGuard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ResourceAnnotationServiceImpl implements IResourceAnnotationService {

    private static final Set<String> TYPES = Set.of("highlight", "underline");
    private static final Set<String> COLORS = Set.of("yellow", "green", "blue", "pink", "purple");
    private static final int WRITE_LIMIT_PER_MINUTE = 120;

    private final ResAnnotationMapper annotationMapper;
    private final ResDocumentVersionMapper versionMapper;
    private final IInfoResourceService resourceService;

    @Override
    public List<ResAnnotationVo> listMine(Long versionId) {
        ResDocumentVersion version = requireReadableVersion(versionId);
        return annotationMapper.selectVoList(Wrappers.<ResAnnotation>lambdaQuery()
            .eq(ResAnnotation::getVersionId, version.getVersionId())
            .eq(ResAnnotation::getUserId, LoginHelper.getUserId())
            .orderByAsc(ResAnnotation::getPage)
            .orderByAsc(ResAnnotation::getAnnotationId));
    }

    @Override
    public ResAnnotationVo create(ResAnnotationBo bo) {
        Long userId = LoginHelper.getUserId();
        WriteRateGuard.check("annotation", userId, WRITE_LIMIT_PER_MINUTE);
        assertEnums(bo.getAnnoType(), bo.getColor());
        ResDocumentVersion version = requireReadableVersion(bo.getVersionId());
        int page = AnchorValidator.validate(bo.getAnchor(), version.getPageCount());

        ResAnnotation entity = MapstructUtils.convert(bo, ResAnnotation.class);
        entity.setAnnotationId(null);
        entity.setResourceId(version.getResourceId());
        entity.setUserId(userId);
        entity.setPage(page);
        annotationMapper.insert(entity);
        return MapstructUtils.convert(entity, ResAnnotationVo.class);
    }

    @Override
    public void update(Long annotationId, ResAnnotationBo bo) {
        Long userId = LoginHelper.getUserId();
        ResAnnotation existing = annotationMapper.selectById(annotationId);
        assertOwner(existing, userId);
        if (bo.getColor() != null) {
            assertEnums(existing.getAnnoType(), bo.getColor());
        }
        ResAnnotation patch = new ResAnnotation();
        patch.setAnnotationId(annotationId);
        patch.setColor(bo.getColor());
        patch.setCommentText(bo.getCommentText());
        patch.setNoteId(bo.getNoteId());
        annotationMapper.updateById(patch);
    }

    @Override
    public void delete(Long annotationId) {
        assertOwner(annotationMapper.selectById(annotationId), LoginHelper.getUserId());
        annotationMapper.deleteById(annotationId);
    }

    private ResDocumentVersion requireReadableVersion(Long versionId) {
        ResDocumentVersion version = versionMapper.selectById(versionId);
        if (version == null || !ResDocumentVersion.STATUS_READY.equals(version.getStatus())) {
            throw new ServiceException("阅读版本不存在或未就绪");
        }
        resourceService.requirePortalReadable(version.getResourceId());
        return version;
    }

    public static void assertOwner(ResAnnotation annotation, Long userId) {
        if (annotation == null || !annotation.getUserId().equals(userId)) {
            throw new ServiceException("标注不存在或无权操作");
        }
    }

    public static void assertEnums(String annoType, String color) {
        if (!TYPES.contains(annoType)) {
            throw new ServiceException("不支持的标注类型");
        }
        if (!COLORS.contains(color)) {
            throw new ServiceException("不支持的标注颜色");
        }
    }
}
```

`ResourceAnnotationController.java`：

```java
package org.dromara.portal.resources.controller.portal;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.portal.resources.domain.bo.ResAnnotationBo;
import org.dromara.portal.resources.domain.vo.ResAnnotationVo;
import org.dromara.portal.resources.service.IResourceAnnotationService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/resources/reader")
public class ResourceAnnotationController {

    private final IResourceAnnotationService annotationService;

    @GetMapping("/version/{versionId}/annotations")
    public R<List<ResAnnotationVo>> list(@PathVariable Long versionId) {
        return R.ok(annotationService.listMine(versionId));
    }

    @Log(title = "阅读标注", businessType = BusinessType.INSERT)
    @PostMapping("/annotations")
    public R<ResAnnotationVo> create(@Validated @RequestBody ResAnnotationBo bo) {
        return R.ok(annotationService.create(bo));
    }

    @Log(title = "阅读标注", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/annotations/{annotationId}")
    public R<Void> update(@PathVariable Long annotationId, @RequestBody ResAnnotationBo bo) {
        annotationService.update(annotationId, bo);
        return R.ok();
    }

    @Log(title = "阅读标注", businessType = BusinessType.DELETE)
    @DeleteMapping("/annotations/{annotationId}")
    public R<Void> delete(@PathVariable Long annotationId) {
        annotationService.delete(annotationId);
        return R.ok();
    }
}
```

- [ ] **Step 4: 跑测试确认通过 + 全模块回归**（Task 6 Step 3 同款命令）Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add source/ruoyi-modules/ruoyi-portal-resources
git commit -m "feat: 阅读标注 CRUD 与写限流"
```

---

### Task 8: 笔记 CRUD（乐观锁）

**Files:**
- Create: `.../domain/bo/ResNoteBo.java`、`.../domain/vo/ResNoteVo.java`
- Create: `.../service/IResourceNoteService.java`、`.../service/impl/ResourceNoteServiceImpl.java`
- Create: `.../controller/portal/ResourceNoteController.java`
- Test: `.../test/java/org/dromara/portal/resources/service/ResourceNoteGuardsTest.java`

**Interfaces:**
- Consumes: `NoteContentValidator.validate/extractText`（Task 4）、`IInfoResourceService.requirePortalReadable`（Task 5）、`WriteRateGuard`（Task 7）
- Produces（Plan 2 前端契约）:
  - `GET /portal/resources/reader/{resourceId}/notes` → `R<List<ResNoteVo>>`（我的笔记元信息，content 置 null 省流量）
  - `GET /portal/resources/reader/notes/{noteId}` → `R<ResNoteVo>`（含 content；本人或 public 可读）
  - `POST /portal/resources/reader/notes`（body=`ResNoteBo{resourceId*, title?, paperStyle?, content?}`；content 缺省为空文档）→ `R<ResNoteVo>`
  - `PUT /portal/resources/reader/notes/{noteId}`（body 含 `revision*`；冲突抛「笔记已在别处更新，请刷新后重试」）→ `R<ResNoteVo>`（返回新 revision）
  - `DELETE /portal/resources/reader/notes/{noteId}` → `R<Void>`
  - `GET /portal/resources/reader/{resourceId}/notes/public` → `R<List<ResNoteVo>>`（visibility=public，含 content，authorName 展示）
  - `ResNoteVo` = 实体字段去 tenant/del_flag；空文档常量 = `{"type":"doc","content":[{"type":"paragraph"}]}`
- 枚举：`paperStyle ∈ {blank, ruled, grid, dot, cornell}`；`visibility ∈ {private, public}`

- [ ] **Step 1: 写失败测试**

```java
package org.dromara.portal.resources.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.portal.resources.service.impl.ResourceNoteServiceImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNoteGuardsTest {

    @Test
    void paperStyleAndVisibilityEnumsGuarded() {
        assertDoesNotThrow(() -> ResourceNoteServiceImpl.assertEnums("cornell", "private"));
        assertThrows(ServiceException.class, () -> ResourceNoteServiceImpl.assertEnums("neon", "private"));
        assertThrows(ServiceException.class, () -> ResourceNoteServiceImpl.assertEnums("blank", "friends"));
    }

    @Test
    void emptyContentFallsBackToEmptyDoc() {
        assertEquals("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}",
            ResourceNoteServiceImpl.normalizeContent(null));
        assertEquals("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}",
            ResourceNoteServiceImpl.normalizeContent("  "));
        String valid = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}";
        assertEquals(valid, ResourceNoteServiceImpl.normalizeContent(valid));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**（`-Dtest=ResourceNoteGuardsTest`，同前命令模式）Expected: 编译失败。

- [ ] **Step 3: 实现**

`ResNoteBo.java`：

```java
package org.dromara.portal.resources.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResNoteBo {

    @NotNull(message = "资料不能为空")
    private Long resourceId;

    @Size(max = 160, message = "标题最多 160 字")
    private String title;

    private String content;
    private String paperStyle;
    private String visibility;
    private Long revision;
}
```

`ResNoteVo.java`（`@AutoMapper(target = ResNote.class)` + `@Data`，字段：noteId, resourceId, userId, authorName, title, content, paperStyle, visibility, revision, createTime, updateTime）。

`IResourceNoteService.java`：

```java
package org.dromara.portal.resources.service;

import org.dromara.portal.resources.domain.bo.ResNoteBo;
import org.dromara.portal.resources.domain.vo.ResNoteVo;

import java.util.List;

public interface IResourceNoteService {

    List<ResNoteVo> listMine(Long resourceId);

    List<ResNoteVo> listPublic(Long resourceId);

    ResNoteVo getReadable(Long noteId);

    ResNoteVo create(ResNoteBo bo);

    ResNoteVo update(Long noteId, ResNoteBo bo);

    void delete(Long noteId);
}
```

`ResourceNoteServiceImpl.java`：

```java
package org.dromara.portal.resources.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.resources.domain.ResNote;
import org.dromara.portal.resources.domain.bo.ResNoteBo;
import org.dromara.portal.resources.domain.vo.ResNoteVo;
import org.dromara.portal.resources.mapper.ResNoteMapper;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.dromara.portal.resources.service.IResourceNoteService;
import org.dromara.portal.resources.support.NoteContentValidator;
import org.dromara.portal.resources.support.WriteRateGuard;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class ResourceNoteServiceImpl implements IResourceNoteService {

    public static final String EMPTY_DOC = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}";
    private static final Set<String> PAPER_STYLES = Set.of("blank", "ruled", "grid", "dot", "cornell");
    private static final Set<String> VISIBILITIES = Set.of("private", "public");
    private static final int WRITE_LIMIT_PER_MINUTE = 60;

    private final ResNoteMapper noteMapper;
    private final IInfoResourceService resourceService;

    @Override
    public List<ResNoteVo> listMine(Long resourceId) {
        resourceService.requirePortalReadable(resourceId);
        List<ResNoteVo> rows = noteMapper.selectVoList(Wrappers.<ResNote>lambdaQuery()
            .eq(ResNote::getResourceId, resourceId)
            .eq(ResNote::getUserId, LoginHelper.getUserId())
            .orderByDesc(ResNote::getUpdateTime));
        rows.forEach(row -> row.setContent(null));
        return rows;
    }

    @Override
    public List<ResNoteVo> listPublic(Long resourceId) {
        resourceService.requirePortalReadable(resourceId);
        return noteMapper.selectVoList(Wrappers.<ResNote>lambdaQuery()
            .eq(ResNote::getResourceId, resourceId)
            .eq(ResNote::getVisibility, "public")
            .orderByDesc(ResNote::getUpdateTime));
    }

    @Override
    public ResNoteVo getReadable(Long noteId) {
        ResNote note = noteMapper.selectById(noteId);
        if (note == null) {
            throw new ServiceException("笔记不存在");
        }
        boolean mine = note.getUserId().equals(LoginHelper.getUserId());
        if (!mine && !"public".equals(note.getVisibility())) {
            throw new ServiceException("笔记不存在或无权查看");
        }
        resourceService.requirePortalReadable(note.getResourceId());
        return MapstructUtils.convert(note, ResNoteVo.class);
    }

    @Override
    public ResNoteVo create(ResNoteBo bo) {
        Long userId = LoginHelper.getUserId();
        WriteRateGuard.check("note", userId, WRITE_LIMIT_PER_MINUTE);
        resourceService.requirePortalReadable(bo.getResourceId());
        String paperStyle = StringUtils.defaultIfBlank(bo.getPaperStyle(), "blank");
        String visibility = StringUtils.defaultIfBlank(bo.getVisibility(), "private");
        assertEnums(paperStyle, visibility);
        String content = normalizeContent(bo.getContent());
        NoteContentValidator.validate(content);

        ResNote note = new ResNote();
        note.setResourceId(bo.getResourceId());
        note.setUserId(userId);
        note.setAuthorName(LoginHelper.getLoginUser().getNickname());
        note.setTitle(StringUtils.defaultIfBlank(bo.getTitle(), "默认笔记"));
        note.setContent(content);
        note.setContentText(NoteContentValidator.extractText(content));
        note.setPaperStyle(paperStyle);
        note.setVisibility(visibility);
        note.setRevision(0L);
        noteMapper.insert(note);
        return MapstructUtils.convert(note, ResNoteVo.class);
    }

    @Override
    public ResNoteVo update(Long noteId, ResNoteBo bo) {
        Long userId = LoginHelper.getUserId();
        WriteRateGuard.check("note", userId, WRITE_LIMIT_PER_MINUTE);
        if (bo.getRevision() == null) {
            throw new ServiceException("缺少笔记版本号");
        }
        ResNote existing = noteMapper.selectById(noteId);
        if (existing == null || !existing.getUserId().equals(userId)) {
            throw new ServiceException("笔记不存在或无权操作");
        }
        String paperStyle = StringUtils.defaultIfBlank(bo.getPaperStyle(), existing.getPaperStyle());
        String visibility = StringUtils.defaultIfBlank(bo.getVisibility(), existing.getVisibility());
        assertEnums(paperStyle, visibility);
        String content = bo.getContent() == null ? existing.getContent() : normalizeContent(bo.getContent());
        NoteContentValidator.validate(content);

        long nextRevision = bo.getRevision() + 1;
        int rows = noteMapper.update(null, Wrappers.<ResNote>lambdaUpdate()
            .set(ResNote::getTitle, StringUtils.defaultIfBlank(bo.getTitle(), existing.getTitle()))
            .set(ResNote::getContent, content)
            .set(ResNote::getContentText, NoteContentValidator.extractText(content))
            .set(ResNote::getPaperStyle, paperStyle)
            .set(ResNote::getVisibility, visibility)
            .set(ResNote::getRevision, nextRevision)
            .eq(ResNote::getNoteId, noteId)
            .eq(ResNote::getUserId, userId)
            .eq(ResNote::getRevision, bo.getRevision()));
        if (rows == 0) {
            throw new ServiceException("笔记已在别处更新，请刷新后重试");
        }
        return getReadable(noteId);
    }

    @Override
    public void delete(Long noteId) {
        ResNote note = noteMapper.selectById(noteId);
        if (note == null || !note.getUserId().equals(LoginHelper.getUserId())) {
            throw new ServiceException("笔记不存在或无权操作");
        }
        noteMapper.deleteById(noteId);
    }

    public static void assertEnums(String paperStyle, String visibility) {
        if (!PAPER_STYLES.contains(paperStyle)) {
            throw new ServiceException("不支持的笔记纸样式");
        }
        if (!VISIBILITIES.contains(visibility)) {
            throw new ServiceException("不支持的可见性取值");
        }
    }

    public static String normalizeContent(String content) {
        return StringUtils.isBlank(content) ? EMPTY_DOC : content;
    }
}
```

注意：`LoginHelper.getLoginUser().getNickname()` 需实现时核对（参照 `InfoResourceServiceImpl` 里显示名的现有取法 `ResourceUserDisplayNameResolver`，若有现成 resolver 就复用它取 authorName）。

`ResourceNoteController.java`：

```java
package org.dromara.portal.resources.controller.portal;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.portal.resources.domain.bo.ResNoteBo;
import org.dromara.portal.resources.domain.vo.ResNoteVo;
import org.dromara.portal.resources.service.IResourceNoteService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/resources/reader")
public class ResourceNoteController {

    private final IResourceNoteService noteService;

    @GetMapping("/{resourceId}/notes")
    public R<List<ResNoteVo>> myNotes(@PathVariable Long resourceId) {
        return R.ok(noteService.listMine(resourceId));
    }

    @GetMapping("/{resourceId}/notes/public")
    public R<List<ResNoteVo>> publicNotes(@PathVariable Long resourceId) {
        return R.ok(noteService.listPublic(resourceId));
    }

    @GetMapping("/notes/{noteId}")
    public R<ResNoteVo> detail(@PathVariable Long noteId) {
        return R.ok(noteService.getReadable(noteId));
    }

    @Log(title = "阅读笔记", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/notes")
    public R<ResNoteVo> create(@Validated @RequestBody ResNoteBo bo) {
        return R.ok(noteService.create(bo));
    }

    @Log(title = "阅读笔记", businessType = BusinessType.UPDATE)
    @PutMapping("/notes/{noteId}")
    public R<ResNoteVo> update(@PathVariable Long noteId, @RequestBody ResNoteBo bo) {
        return R.ok(noteService.update(noteId, bo));
    }

    @Log(title = "阅读笔记", businessType = BusinessType.DELETE)
    @DeleteMapping("/notes/{noteId}")
    public R<Void> delete(@PathVariable Long noteId) {
        noteService.delete(noteId);
        return R.ok();
    }
}
```

- [ ] **Step 4: 跑测试确认通过 + 全模块回归**（同前命令）Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add source/ruoyi-modules/ruoyi-portal-resources
git commit -m "feat: 阅读笔记 CRUD（Tiptap JSON 正本 + revision 乐观锁）"
```

---

### Task 9: 验证门禁 + 冒烟 + 收工

**Files:**
- 无新文件（验证与推送）

- [ ] **Step 1: 全量门禁**

```bash
cd source && MAVEN_OPTS="-Xmx1024m" mvn -o -ntp -Pdev -DskipTests -pl ruoyi-modules/ruoyi-portal-resources -am compile
cd source && mvn -o -ntp -pl ruoyi-modules/ruoyi-portal-resources -am -DskipTests=false test
```

Expected: 两条均 BUILD SUCCESS。

- [ ] **Step 2: 本地栈冒烟（按 RUNBOOK 起本地栈后）**

浏览器登录前端（`http://127.0.0.1:7018`）后，从 DevTools 复制请求头 `Authorization` 与 `clientid` 的值，替换下方变量：

```bash
AUTH='Bearer <token>'; CLIENT='<clientid>'
# 1. 会话（<id> 用资料列表中任一 Word/PDF 资料的 resourceId）
curl -s -H "Authorization: $AUTH" -H "clientid: $CLIENT" \
  'http://127.0.0.1:8180/resources/portal/resources/reader/<id>/session'
# 期望：{"code":200,...,"data":{"status":"converting"...}}；数秒后重发返回 status=ready 且带 versionId/sha256/pageCount
# 2. 版本文件
curl -s -o /tmp/reader-smoke.pdf -w '%{http_code} %{content_type}\n' -H "Authorization: $AUTH" -H "clientid: $CLIENT" \
  'http://127.0.0.1:8180/resources/portal/resources/reader/version/<versionId>/file'
# 期望：200 application/pdf，文件可被 `file /tmp/reader-smoke.pdf` 识别为 PDF
# 3. 标注创建（sha256 取 session 返回值）
curl -s -X POST -H "Authorization: $AUTH" -H "clientid: $CLIENT" -H 'Content-Type: application/json' \
  'http://127.0.0.1:8180/resources/portal/resources/reader/annotations' \
  -d '{"versionId":<versionId>,"annoType":"highlight","color":"yellow","anchor":"{\"v\":1,\"sha256\":\"<sha256>\",\"page\":1,\"quote\":{\"exact\":\"测试\",\"prefix\":\"\",\"suffix\":\"\"},\"rects\":[{\"x\":0.1,\"y\":0.1,\"w\":0.2,\"h\":0.02}]}"}'
# 期望：code=200 且返回 annotationId、page=1
# 4. 笔记创建+乐观锁冲突
curl -s -X POST ... '/reader/notes' -d '{"resourceId":<id>,"title":"冒烟笔记"}'          # 期望 revision=0
curl -s -X PUT ... '/reader/notes/<noteId>' -d '{"resourceId":<id>,"revision":0}'        # 期望 code=200 revision=1
curl -s -X PUT ... '/reader/notes/<noteId>' -d '{"resourceId":<id>,"revision":0}'        # 期望报「笔记已在别处更新」
```

（网关路由前缀若非 `/resources`，以 `source/script/config/nacos/ruoyi-gateway.yml` 中 portal-resources 路由为准替换。）

- [ ] **Step 3: 迁移复核** — Task 1 Step 4 的 psql 验证在真实数据上重跑一遍，确认 res_note 行数与旧表一致。

- [ ] **Step 4: 收工推送**

```bash
git log origin/main..HEAD --oneline       # 自查提交序列
git push -u origin feature/resources-reader-phase1-backend
```

Expected: 分支推送成功；随后按 AGENTS.md 建 PR（`Refs` 对应 Issue，附本 Task 的验证输出）。
