# Task 6: 单体内解耦④ —— 文档转换队列化 + 转换逻辑抽类（解 800 行红线）

现状：LibreOffice 转换同步阻塞在 HTTP 请求线程（`InfoResourceServiceImpl.previewPdf:259 → ensurePdfPreviewFile:505 → convertOfficeToPdf:532`），无并发控制；且 `InfoResourceServiceImpl` 恰好 800 行触顶。改造：①转换逻辑抽到 `DocumentPreviewConverter`；②转换执行改经 `portal-resources.convert` 工作队列（prefetch=1 串行化 soffice，请求线程只轮询缓存文件出现）；③上传成功即预热转换，正常访问基本缓存命中。**HTTP 语义不变**（同样的等待上限与错误文案），前端零改动。

**Files:**
- Test: `source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/resources/mq/ResourceConvertListenerTest.java`（新建）
- Test: `source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/resources/support/DocumentPreviewConverterTest.java`（新建）
- Create: `.../resources/support/DocumentPreviewConverter.java`
- Create: `.../resources/mq/ResourceConvertMessage.java`
- Create: `.../resources/mq/ResourcesMqConfig.java`
- Create: `.../resources/mq/ResourceConvertListener.java`
- Modify: `.../resources/service/IInfoResourceService.java`（+`ensurePreviewConverted`）
- Modify: `.../resources/service/impl/InfoResourceServiceImpl.java`（删转换私有方法、previewPdf 改队列等待、加入队/预热）

**Interfaces:**
- Consumes: T1 `PortalEventConstants.QUEUE_RESOURCES_CONVERT(_DLQ)`、T1 自动装配的 `portalEventMessageConverter`；`RedisUtils.setObjectIfAbsent/deleteObject`。
- Produces: `IInfoResourceService.ensurePreviewConverted(Long resourceId)`（幂等：缓存命中直接返回）；`DocumentPreviewConverter{ cacheFileFor(InfoResource,RemoteFile)→Path; isReady(Path)→boolean; awaitReady(Path)→Path; ensurePdfPreviewFile(InfoResource,RemoteFile,String)→Path }`；Redis 挂起键前缀 `resources:convert:pending:`。T9 拆分时全部随包搬迁。

- [ ] **Step 1: 写失败测试（RED）**

`resources/mq/ResourceConvertListenerTest.java`：
```java
package org.dromara.portal.resources.mq;

import com.rabbitmq.client.Channel;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ResourceConvertListenerTest {

    @Mock
    private IInfoResourceService resourceService;

    @Mock
    private Channel channel;

    @InjectMocks
    private ResourceConvertListener listener;

    private ResourceConvertMessage msg() {
        ResourceConvertMessage m = new ResourceConvertMessage();
        m.setMessageId("m-1");
        m.setResourceId(42L);
        return m;
    }

    @Test
    void success_converts_acks_and_clears_pending_flag() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            listener.onConvert(msg(), channel, 5L);
            verify(resourceService).ensurePreviewConverted(42L);
            verify(channel).basicAck(5L, false);
            redis.verify(() -> RedisUtils.deleteObject("resources:convert:pending:42"));
        }
    }

    @Test
    void failure_nacks_to_dlq_and_clears_pending_flag() throws Exception {
        try (MockedStatic<RedisUtils> redis = mockStatic(RedisUtils.class)) {
            doThrow(new RuntimeException("soffice missing")).when(resourceService).ensurePreviewConverted(42L);
            listener.onConvert(msg(), channel, 6L);
            verify(channel).basicNack(6L, false, false);
            verify(channel, never()).basicAck(anyLong(), anyBoolean());
            redis.verify(() -> RedisUtils.deleteObject("resources:convert:pending:42"));
        }
    }
}
```

`resources/support/DocumentPreviewConverterTest.java`：
```java
package org.dromara.portal.resources.support;

import org.dromara.file.api.domain.RemoteFile;
import org.dromara.portal.resources.domain.InfoResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentPreviewConverterTest {

    @TempDir
    Path tempDir;

    private DocumentPreviewConverter converter() {
        DocumentPreviewConverter c = new DocumentPreviewConverter();
        ReflectionTestUtils.setField(c, "previewCacheDir", tempDir.resolve("cache").toString());
        ReflectionTestUtils.setField(c, "sofficeCommand", "soffice");
        ReflectionTestUtils.setField(c, "sofficeTimeoutSeconds", 30L);
        return c;
    }

    private InfoResource resource() {
        InfoResource r = new InfoResource();
        r.setResourceId(42L);
        r.setFileSize(1024L);
        r.setUpdateTime(new Date(1720000000000L));
        return r;
    }

    private RemoteFile remoteFile() {
        RemoteFile f = new RemoteFile();
        f.setOssId(77L);
        return f;
    }

    @Test
    void cache_file_name_is_deterministic_and_dir_created() throws Exception {
        Path p1 = converter().cacheFileFor(resource(), remoteFile());
        Path p2 = converter().cacheFileFor(resource(), remoteFile());
        assertEquals(p1, p2);
        assertEquals("42-77-1024-1720000000000.pdf", p1.getFileName().toString());
        assertTrue(Files.isDirectory(p1.getParent()));
    }

    @Test
    void isReady_false_for_missing_true_for_nonempty() throws Exception {
        DocumentPreviewConverter c = converter();
        Path p = c.cacheFileFor(resource(), remoteFile());
        assertFalse(c.isReady(p));
        Files.write(p, new byte[]{1});
        assertTrue(c.isReady(p));
    }
}
```
> 若 `InfoResource`/`RemoteFile` 的 setter 名与上文不符（以实际 domain 为准），按实际字段调整测试构造。

- [ ] **Step 2: 跑测试确认失败**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：编译失败（新类不存在）。

- [ ] **Step 3: DocumentPreviewConverter（转换逻辑平移）**

`resources/support/DocumentPreviewConverter.java`——字段与方法体**逐字平移**自 `InfoResourceServiceImpl`（字段 :83-91，方法 :495-611），外加 `cacheFileFor/isReady/awaitReady` 三个新方法：
```java
package org.dromara.portal.resources.support;

import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.file.api.domain.RemoteFile;
import org.dromara.portal.resources.domain.InfoResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

/** LibreOffice PDF 预览转换器：缓存命名/就绪判断/转换执行（由 MQ 消费线程串行调用） */
@Slf4j
@Component
public class DocumentPreviewConverter {

    @Value("${infoservice.preview-cache.dir:/ruoyi/infoservice/preview-cache}")
    private String previewCacheDir;

    @Value("${infoservice.preview-cache.soffice:soffice}")
    private String sofficeCommand;

    @Value("${infoservice.preview-cache.timeout-seconds:120}")
    private long sofficeTimeoutSeconds;

    public Path cacheFileFor(InfoResource resource, RemoteFile remoteFile) throws IOException {
        Path cacheDir = Path.of(previewCacheDir);
        Files.createDirectories(cacheDir);
        return cacheDir.resolve(buildPdfCacheFileName(resource, remoteFile));
    }

    public boolean isReady(Path targetFile) {
        try {
            return Files.exists(targetFile) && Files.size(targetFile) > 0;
        } catch (IOException e) {
            return false;
        }
    }

    /** 请求线程轮询等待转换产物出现；超时文案与旧同步实现一致 */
    public Path awaitReady(Path targetFile) {
        long deadline = System.currentTimeMillis() + Math.max(30, sofficeTimeoutSeconds) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (isReady(targetFile)) {
                return targetFile;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ServiceException("PDF 转换被中断，请下载后查看");
            }
        }
        throw new ServiceException("PDF 转换超时，请下载后查看");
    }

    public Path ensurePdfPreviewFile(InfoResource resource, RemoteFile remoteFile, String suffix) throws IOException {
        // ← InfoResourceServiceImpl.ensurePdfPreviewFile(:505-530) 方法体逐字平移，
        //    其中 cacheDir/targetFile 三行改为调用 this.cacheFileFor(...)：
        Path targetFile = cacheFileFor(resource, remoteFile);
        if (isReady(targetFile)) {
            return targetFile;
        }
        Path downloadedFile = null;
        Path workDir = Files.createTempDirectory(targetFile.getParent(), "convert-");
        try {
            downloadedFile = downloadRemoteFile(remoteFile);
            Path sourceFile = workDir.resolve("source." + suffix);
            Files.copy(downloadedFile, sourceFile, StandardCopyOption.REPLACE_EXISTING);
            Path convertedFile = convertOfficeToPdf(sourceFile, workDir);
            Path tempTarget = Files.createTempFile(targetFile.getParent(), targetFile.getFileName().toString(), ".tmp");
            Files.move(convertedFile, tempTarget, StandardCopyOption.REPLACE_EXISTING);
            Files.move(tempTarget, targetFile, StandardCopyOption.REPLACE_EXISTING);
            return targetFile;
        } finally {
            if (downloadedFile != null) {
                Files.deleteIfExists(downloadedFile);
            }
            FileUtils.del(workDir.toFile());
        }
    }

    // ↓ 以下四个私有方法自 InfoResourceServiceImpl 逐字平移（行号：resolveStorage:495、
    //   downloadRemoteFile:501、convertOfficeToPdf:532-590、buildPdfCacheFileName:592-603），
    //   impl 中 previewFile/previewPdf 直连路径仍在用 resolveStorage/downloadRemoteFile，
    //   故 impl 保留原件，此处为转换链私有副本（同 BC 内 8 行工具复制，允许）。
    private OssClient resolveStorage(RemoteFile remoteFile) { /* 平移原文 */ }
    private Path downloadRemoteFile(RemoteFile remoteFile) { /* 平移原文 */ }
    private Path convertOfficeToPdf(Path sourceFile, Path outputDir) throws IOException { /* 平移原文 */ }
    private String buildPdfCacheFileName(InfoResource resource, RemoteFile remoteFile) { /* 平移原文 */ }
}
```
> 「平移原文」= 从 `InfoResourceServiceImpl.java` 对应行号剪切粘贴，不做任何逻辑修改（这是搬迁不是重写；上述行号在 T3/T4 改动后可能漂移 ±10 行，以方法名定位为准）。

- [ ] **Step 4: MQ 消息体 / 拓扑 / 监听器**

`resources/mq/ResourceConvertMessage.java`：
```java
package org.dromara.portal.resources.mq;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ResourceConvertMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String messageId;
    private Long resourceId;
}
```

`resources/mq/ResourcesMqConfig.java`：
```java
package org.dromara.portal.resources.mq;

import org.dromara.portal.api.event.PortalEventConstants;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 资料转换工作队列拓扑：prefetch=1 串行化 LibreOffice，失败入死信 */
@Configuration
public class ResourcesMqConfig {

    @Bean
    public DirectExchange resourcesDlxExchange() {
        return ExchangeBuilder.directExchange(PortalEventConstants.DLX).durable(true).build();
    }

    @Bean
    public Queue resourcesConvertQueue() {
        return QueueBuilder.durable(PortalEventConstants.QUEUE_RESOURCES_CONVERT)
            .deadLetterExchange(PortalEventConstants.DLX)
            .deadLetterRoutingKey(PortalEventConstants.QUEUE_RESOURCES_CONVERT_DLQ)
            .build();
    }

    @Bean
    public Queue resourcesConvertDlq() {
        return QueueBuilder.durable(PortalEventConstants.QUEUE_RESOURCES_CONVERT_DLQ).build();
    }

    @Bean
    public Binding resourcesConvertDlqBinding() {
        return BindingBuilder.bind(resourcesConvertDlq()).to(resourcesDlxExchange())
            .with(PortalEventConstants.QUEUE_RESOURCES_CONVERT_DLQ);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory resourcesConvertContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter portalEventMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(portalEventMessageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setPrefetchCount(1);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
```
> 单体阶段 `portalDlxExchange`（T4，kernel 包）与 `resourcesDlxExchange` 声明同一 DLX——bean 名不同、声明幂等，无冲突；拆分后各服务各自声明，正是所需。

`resources/mq/ResourceConvertListener.java`：
```java
package org.dromara.portal.resources.mq;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
public class ResourceConvertListener {

    public static final String PENDING_KEY_PREFIX = "resources:convert:pending:";

    private final IInfoResourceService resourceService;

    @RabbitListener(queues = PortalEventConstants.QUEUE_RESOURCES_CONVERT,
                    containerFactory = "resourcesConvertContainerFactory")
    public void onConvert(ResourceConvertMessage message, Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        try {
            resourceService.ensurePreviewConverted(message.getResourceId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("资料转换失败，转入死信 message={}", message, e);
            channel.basicNack(deliveryTag, false, false);
        } finally {
            RedisUtils.deleteObject(PENDING_KEY_PREFIX + message.getResourceId());
        }
    }
}
```

- [ ] **Step 5: InfoResourceServiceImpl 改造**

1. **删除**私有方法 `ensurePdfPreviewFile/convertOfficeToPdf/buildPdfCacheFileName` 与三个 `@Value` 字段（`previewCacheDir/sofficeCommand/sofficeTimeoutSeconds`）及相应 import（`resolveStorage/downloadRemoteFile` **保留**——previewFile:255 与 pdf 直传路径 :268 仍在用）。
2. **新增**字段（`@RequiredArgsConstructor` 注入）：
```java
    private final DocumentPreviewConverter previewConverter;
    private final RabbitTemplate rabbitTemplate;
```
imports：`org.dromara.portal.resources.support.DocumentPreviewConverter`、`org.springframework.amqp.rabbit.core.RabbitTemplate`、`org.dromara.common.redis.utils.RedisUtils`、`org.dromara.portal.api.event.PortalEventConstants`、`org.dromara.portal.resources.mq.ResourceConvertMessage`、`java.time.Duration`、`java.util.UUID`。
3. `previewPdf` 中 office 分支两行：
```java
        Path pdfFile = ensurePdfPreviewFile(resource, remoteFile, suffix);
        writePdfResponse(pdfFile, resolvePreviewPdfName(resource, remoteFile), response);
```
替换为：
```java
        Path targetFile = previewConverter.cacheFileFor(resource, remoteFile);
        if (!previewConverter.isReady(targetFile)) {
            enqueueConversion(resource.getResourceId());
            previewConverter.awaitReady(targetFile);
        }
        writePdfResponse(targetFile, resolvePreviewPdfName(resource, remoteFile), response);
```
4. **新增**三个方法（放在 `sendResourceCreatedNotification` 附近）：
```java
    @Override
    public void ensurePreviewConverted(Long resourceId) {
        InfoResource resource = baseMapper.selectById(resourceId);
        if (resource == null) {
            return;
        }
        RemoteFile remoteFile = resolveRemoteFile(resource);
        String suffix = resolveFileSuffix(resource, remoteFile);
        if (!PDF_CONVERTIBLE_SUFFIXES.contains(suffix)) {
            return;
        }
        try {
            previewConverter.ensurePdfPreviewFile(resource, remoteFile, suffix);
        } catch (IOException e) {
            throw new IllegalStateException("资料预览转换失败 resourceId=" + resourceId, e);
        }
    }

    private void enqueueConversion(Long resourceId) {
        boolean first = RedisUtils.setObjectIfAbsent(
            ResourceConvertListener.PENDING_KEY_PREFIX + resourceId, "1", Duration.ofMinutes(10));
        if (!first) {
            return;
        }
        ResourceConvertMessage message = new ResourceConvertMessage();
        message.setMessageId(UUID.randomUUID().toString());
        message.setResourceId(resourceId);
        rabbitTemplate.convertAndSend(PortalEventConstants.QUEUE_RESOURCES_CONVERT, message);
    }

    private void enqueueConversionIfConvertible(InfoResource resource) {
        String name = StringUtils.defaultString(resource.getOriginalName()).toLowerCase();
        int dot = name.lastIndexOf('.');
        String suffix = dot >= 0 ? name.substring(dot + 1) : "";
        if (PDF_CONVERTIBLE_SUFFIXES.contains(suffix)) {
            enqueueConversion(resource.getResourceId());
        }
    }
```
5. 在 `sendResourceCreatedNotification(add);`（:346 附近）之后追加一行预热：
```java
            enqueueConversionIfConvertible(add);
```
6. `IInfoResourceService.java` 接口追加：
```java
    /** 确保资料的 PDF 预览缓存已生成（MQ 消费线程调用，幂等） */
    void ensurePreviewConverted(Long resourceId);
```

- [ ] **Step 6: 跑测试（GREEN）+ 行数红线核查 + 提交**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
wc -l ruoyi-modules/ruoyi-portal/src/main/java/org/dromara/portal/resources/service/impl/InfoResourceServiceImpl.java
```
预期：测试全绿；行数 **< 800**（预计 ~760；若仍超 800，把 `applyCurrentUserAccessFilter`/wrapper 构建等查询辅助方法抽到 `resources/service/support/` 再收一刀）。

```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "feat: 资料预览转换改走工作队列并抽离转换器，上传预热缓存"
```
