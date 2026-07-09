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

    private OssClient resolveStorage(RemoteFile remoteFile) {
        return StringUtils.isBlank(remoteFile.getService())
            ? OssFactory.instance()
            : OssFactory.instance(remoteFile.getService());
    }

    private Path downloadRemoteFile(RemoteFile remoteFile) {
        return resolveStorage(remoteFile).fileDownload(remoteFile.getName());
    }

    private Path convertOfficeToPdf(Path sourceFile, Path outputDir) throws IOException {
        Files.createDirectories(outputDir.resolve("home"));
        Path logFile = outputDir.resolve("soffice.log");
        ProcessBuilder processBuilder = new ProcessBuilder(
            sofficeCommand,
            "--headless",
            "--nologo",
            "--nofirststartwizard",
            "--convert-to",
            "pdf",
            "--outdir",
            outputDir.toString(),
            sourceFile.toString()
        );
        processBuilder.environment().put("HOME", outputDir.resolve("home").toString());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(logFile.toFile());

        Process process;
        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new ServiceException("未找到 LibreOffice 转换程序，请检查 soffice 配置");
        }

        boolean finished;
        try {
            finished = process.waitFor(Math.max(30, sofficeTimeoutSeconds), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ServiceException("PDF 转换被中断，请下载后查看");
        }
        if (!finished) {
            process.destroyForcibly();
            throw new ServiceException("PDF 转换超时，请下载后查看");
        }
        if (process.exitValue() != 0) {
            throw new ServiceException("PDF 转换失败，请下载后查看");
        }

        Path expectedFile = outputDir.resolve("source.pdf");
        if (Files.exists(expectedFile) && Files.size(expectedFile) > 0) {
            return expectedFile;
        }
        try (var files = Files.list(outputDir)) {
            return files
                .filter(file -> file.getFileName().toString().toLowerCase().endsWith(".pdf"))
                .filter(file -> {
                    try {
                        return Files.size(file) > 0;
                    } catch (IOException e) {
                        return false;
                    }
                })
                .findFirst()
                .orElseThrow(() -> new ServiceException("PDF 转换未生成有效文件，请下载后查看"));
        }
    }

    private String buildPdfCacheFileName(InfoResource resource, RemoteFile remoteFile) {
        long updatedAt = resource.getUpdateTime() != null
            ? resource.getUpdateTime().getTime()
            : resource.getCreateTime() == null ? 0L : resource.getCreateTime().getTime();
        return "%s-%s-%s-%s.pdf".formatted(
            resource.getResourceId(),
            StringUtils.defaultString(String.valueOf(remoteFile.getOssId())),
            StringUtils.defaultString(String.valueOf(resource.getFileSize())),
            updatedAt
        );
    }
}
