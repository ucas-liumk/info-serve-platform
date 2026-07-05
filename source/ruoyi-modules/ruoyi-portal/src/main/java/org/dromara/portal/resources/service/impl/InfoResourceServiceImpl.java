package org.dromara.portal.resources.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.InfoResourceCategory;
import org.dromara.portal.resources.domain.InfoResourceFavorite;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.vo.InfoResourceVo;
import org.dromara.portal.resources.domain.vo.ResourceUploadVo;
import org.dromara.portal.resources.mapper.InfoResourceCategoryMapper;
import org.dromara.portal.resources.mapper.InfoResourceFavoriteMapper;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.dromara.portal.kernel.support.InfoUserDisplayNameResolver;
import org.dromara.file.api.RemoteFileService;
import org.dromara.file.api.domain.RemoteFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class InfoResourceServiceImpl implements IInfoResourceService {

    private static final String SCOPE_MINE = "mine";
    private static final String SCOPE_FAVORITES = "favorites";
    private static final long ONE_MB = 1024L * 1024L;
    private static final Set<String> PDF_CONVERTIBLE_SUFFIXES = Set.of(
        "doc", "docx", "docm", "dot", "dotx", "dotm",
        "xls", "xlsx", "xlsm", "xlt", "xltx", "xltm", "csv", "tsv",
        "ppt", "pptx", "pptm", "pps", "ppsx",
        "wps", "wpt", "et", "ett", "dps", "dpt",
        "odt", "ods", "odp", "rtf"
    );

    private final InfoResourceMapper baseMapper;
    private final InfoResourceCategoryMapper categoryMapper;
    private final InfoResourceFavoriteMapper favoriteMapper;
    private final IPortalNotificationService notificationService;
    private final InfoUserDisplayNameResolver userDisplayNameResolver;

    @DubboReference
    private RemoteFileService remoteFileService;

    @Value("${infoservice.preview-cache.dir:/ruoyi/infoservice/preview-cache}")
    private String previewCacheDir;

    @Value("${infoservice.preview-cache.soffice:soffice}")
    private String sofficeCommand;

    @Value("${infoservice.preview-cache.timeout-seconds:120}")
    private long sofficeTimeoutSeconds;

    private LambdaQueryWrapper<InfoResource> buildWrapper(InfoResourceBo bo, boolean portal) {
        LambdaQueryWrapper<InfoResource> w = Wrappers.lambdaQuery();
        boolean mineScope = portal && SCOPE_MINE.equals(bo.getScope());
        boolean favoritesScope = portal && SCOPE_FAVORITES.equals(bo.getScope());
        w.eq(bo.getCategoryId() != null, InfoResource::getCategoryId, bo.getCategoryId());
        if (portal) {
            if (mineScope) {
                w.eq(InfoResource::getCreateBy, requireLogin());
                if (isActiveFilter(bo.getStatus())) {
                    w.eq(InfoResource::getStatus, bo.getStatus());
                }
            } else {
                w.eq(InfoResource::getStatus, "0");
                if (favoritesScope) {
                    List<Long> resourceIds = favoriteMapper.selectList(Wrappers.<InfoResourceFavorite>lambdaQuery()
                            .eq(InfoResourceFavorite::getUserId, requireLogin())
                            .orderByDesc(InfoResourceFavorite::getCreateTime))
                        .stream().map(InfoResourceFavorite::getResourceId).toList();
                    if (resourceIds.isEmpty()) {
                        w.eq(InfoResource::getResourceId, -1L);
                    } else {
                        w.in(InfoResource::getResourceId, resourceIds);
                    }
                }
            }
        } else if (isActiveFilter(bo.getStatus())) {
            w.eq(InfoResource::getStatus, bo.getStatus());
        }
        if (StringUtils.isNotBlank(bo.getCategoryCode()) && !"all".equals(bo.getCategoryCode())) {
            InfoResourceCategory category = categoryMapper.selectOne(
                Wrappers.<InfoResourceCategory>lambdaQuery()
                    .eq(InfoResourceCategory::getCategoryCode, bo.getCategoryCode())
                    .eq(InfoResourceCategory::getStatus, "0"));
            w.eq(InfoResource::getCategoryId, category == null ? -1L : category.getCategoryId());
        }
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            w.and(q -> q.like(InfoResource::getTitle, bo.getKeyword())
                .or().like(InfoResource::getDescription, bo.getKeyword())
                .or().like(InfoResource::getOriginalName, bo.getKeyword()));
        }
        String previewType = StringUtils.defaultIfBlank(bo.getPreviewType(), bo.getFileType());
        if (isActiveFilter(previewType)) {
            w.eq(InfoResource::getPreviewType, previewType);
        }
        Date uploadedSince = resolveUploadedSince(bo.getUploadedWithin());
        w.ge(uploadedSince != null, InfoResource::getCreateTime, uploadedSince);
        applySizeRange(w, bo.getSizeRange());
        applySort(w, bo.getSort());
        return w;
    }

    private void fillResourceExt(List<InfoResourceVo> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> categoryIds = rows.stream().map(InfoResourceVo::getCategoryId).filter(id -> id != null).distinct().toList();
        Map<Long, String> categoryNames = categoryIds.isEmpty() ? Collections.emptyMap() : categoryMapper.selectList(Wrappers.<InfoResourceCategory>lambdaQuery().in(InfoResourceCategory::getCategoryId, categoryIds))
            .stream().collect(Collectors.toMap(InfoResourceCategory::getCategoryId, InfoResourceCategory::getCategoryName, (a, b) -> a));
        Long currentUserId = LoginHelper.getUserId();
        String currentDisplayName = userDisplayNameResolver.currentUserDisplayName("我");
        Map<Long, String> ownerNames = userDisplayNameResolver.resolveDisplayNames(rows.stream().map(InfoResourceVo::getCreateBy).toList());
        List<Long> resourceIds = rows.stream().map(InfoResourceVo::getResourceId).filter(id -> id != null).distinct().toList();
        Set<Long> favoritedIds = currentUserId == null || resourceIds.isEmpty()
            ? Collections.emptySet()
            : favoriteMapper.selectList(Wrappers.<InfoResourceFavorite>lambdaQuery()
                    .eq(InfoResourceFavorite::getUserId, currentUserId)
                    .in(InfoResourceFavorite::getResourceId, resourceIds))
                .stream().map(InfoResourceFavorite::getResourceId).collect(Collectors.toSet());
        Map<Long, Long> favoriteCounts = resourceIds.isEmpty()
            ? Collections.emptyMap()
            : favoriteMapper.selectList(Wrappers.<InfoResourceFavorite>lambdaQuery().in(InfoResourceFavorite::getResourceId, resourceIds))
                .stream().collect(Collectors.groupingBy(InfoResourceFavorite::getResourceId, Collectors.counting()));
        rows.forEach(row -> {
            row.setCategoryName(categoryNames.get(row.getCategoryId()));
            boolean mine = currentUserId != null && currentUserId.equals(row.getCreateBy());
            row.setCanManage(mine);
            row.setOwnerName(StringUtils.defaultIfBlank(ownerNames.get(row.getCreateBy()), mine ? currentDisplayName : "平台用户"));
            row.setFavorited(favoritedIds.contains(row.getResourceId()));
            row.setFavoriteCount(favoriteCounts.getOrDefault(row.getResourceId(), 0L));
        });
    }

    @Override
    public TableDataInfo<InfoResourceVo> queryPageList(InfoResourceBo bo, PageQuery pageQuery) {
        Page<InfoResourceVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo, false));
        fillResourceExt(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<InfoResourceVo> portalPage(InfoResourceBo bo, PageQuery pageQuery) {
        Page<InfoResourceVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo, true));
        fillResourceExt(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public InfoResourceVo queryById(Long resourceId) {
        InfoResourceVo vo = baseMapper.selectVoById(resourceId);
        fillResourceExt(vo == null ? Collections.emptyList() : List.of(vo));
        return vo;
    }

    @Override
    public InfoResourceVo queryPortalDetail(Long resourceId) {
        InfoResource resource = getPortalReadableResource(resourceId);
        baseMapper.update(null, Wrappers.<InfoResource>lambdaUpdate()
            .setSql("view_count = view_count + 1")
            .eq(InfoResource::getResourceId, resourceId));
        return convertPortalResource(resource);
    }

    @Override
    public InfoResourceVo queryPortalReadableDetail(Long resourceId) {
        return convertPortalResource(getPortalReadableResource(resourceId));
    }

    private InfoResourceVo convertPortalResource(InfoResource resource) {
        InfoResourceVo vo = MapstructUtils.convert(resource, InfoResourceVo.class);
        fillResourceExt(List.of(vo));
        return vo;
    }

    @Override
    public ResourceUploadVo upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        try {
            RemoteFile remoteFile = remoteFileService.upload(file.getName(), file.getOriginalFilename(), file.getContentType(), file.getBytes());
            ResourceUploadVo vo = new ResourceUploadVo();
            vo.setOssId(remoteFile.getOssId());
            vo.setFileName(remoteFile.getName());
            vo.setUrl(remoteFile.getUrl());
            vo.setOriginalName(remoteFile.getOriginalName());
            vo.setFileSuffix(remoteFile.getFileSuffix());
            vo.setMimeType(file.getContentType());
            vo.setFileSize(file.getSize());
            vo.setPreviewType(resolvePreviewType(file.getContentType(), remoteFile.getOriginalName()));
            return vo;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传资料文件失败", e);
            throw new ServiceException("上传文件失败");
        }
    }

    @Override
    public ResourceUploadVo portalUpload(MultipartFile file) {
        requireLogin();
        return upload(file);
    }

    @Override
    public void previewFile(Long resourceId, HttpServletResponse response) throws IOException {
        InfoResource resource = getPortalReadableResource(resourceId);
        RemoteFile remoteFile = resolveRemoteFile(resource);
        String fileName = resolveDownloadFileName(resource, remoteFile);
        response.setContentType(resolveInlineContentType(resource, remoteFile));
        response.setHeader("Cache-Control", "private, max-age=3600");
        response.setHeader("Content-Disposition", buildInlineContentDisposition(fileName));
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
        resolveStorage(remoteFile).download(remoteFile.getName(), response.getOutputStream(), response::setContentLengthLong);
    }

    @Override
    public void previewPdf(Long resourceId, HttpServletResponse response) throws IOException {
        InfoResource resource = getPortalReadableResource(resourceId);
        RemoteFile remoteFile = resolveRemoteFile(resource);
        String suffix = resolveFileSuffix(resource, remoteFile);
        if ("ofd".equals(suffix) || "ofd".equals(StringUtils.defaultString(resource.getPreviewType()).toLowerCase())) {
            throw new ServiceException("OFD 文件暂不支持在线预览，请下载后查看");
        }

        if ("pdf".equals(suffix) || "pdf".equals(StringUtils.defaultString(resource.getPreviewType()).toLowerCase())) {
            Path tempFile = downloadRemoteFile(remoteFile);
            try {
                writePdfResponse(tempFile, resolvePreviewPdfName(resource, remoteFile), response);
            } finally {
                Files.deleteIfExists(tempFile);
            }
            return;
        }

        if (!PDF_CONVERTIBLE_SUFFIXES.contains(suffix)) {
            throw new ServiceException("当前文件暂不支持在线预览，请下载后查看");
        }

        Path pdfFile = ensurePdfPreviewFile(resource, remoteFile, suffix);
        writePdfResponse(pdfFile, resolvePreviewPdfName(resource, remoteFile), response);
    }

    @Override
    public void downloadFile(Long resourceId, HttpServletResponse response) throws IOException {
        InfoResource resource = getPortalReadableResource(resourceId);
        RemoteFile remoteFile = resolveRemoteFile(resource);
        String fileName = resolveDownloadFileName(resource, remoteFile);
        FileUtils.setAttachmentResponseHeader(response, fileName);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE + "; charset=UTF-8");
        OssClient storage = StringUtils.isBlank(remoteFile.getService())
            ? OssFactory.instance()
            : OssFactory.instance(remoteFile.getService());
        storage.download(remoteFile.getName(), response.getOutputStream(), response::setContentLengthLong);
        baseMapper.update(null, Wrappers.<InfoResource>lambdaUpdate()
            .setSql("download_count = download_count + 1")
            .eq(InfoResource::getResourceId, resourceId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long resourceId, boolean add) {
        Long userId = requireLogin();
        if (add) {
            InfoResource resource = baseMapper.selectById(resourceId);
            if (resource == null || !"0".equals(resource.getStatus())) {
                throw new ServiceException("资料不存在或已下架");
            }
        }
        Long exist = favoriteMapper.selectCount(Wrappers.<InfoResourceFavorite>lambdaQuery()
            .eq(InfoResourceFavorite::getUserId, userId)
            .eq(InfoResourceFavorite::getResourceId, resourceId));
        if (add && exist == 0) {
            InfoResourceFavorite favorite = new InfoResourceFavorite();
            favorite.setResourceId(resourceId);
            favorite.setUserId(userId);
            favorite.setTenantId(LoginHelper.getTenantId());
            favorite.setCreateTime(new Date());
            favoriteMapper.insert(favorite);
        } else if (!add && exist > 0) {
            favoriteMapper.delete(Wrappers.<InfoResourceFavorite>lambdaQuery()
                .eq(InfoResourceFavorite::getUserId, userId)
                .eq(InfoResourceFavorite::getResourceId, resourceId));
        }
    }

    @Override
    public Boolean insertByBo(InfoResourceBo bo) {
        InfoResource add = MapstructUtils.convert(bo, InfoResource.class);
        assertCategoryUsable(add.getCategoryId());
        fillResourceDefaults(add);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean insertPortalByBo(InfoResourceBo bo) {
        requireLogin();
        InfoResource add = MapstructUtils.convert(bo, InfoResource.class);
        assertCategoryUsable(add.getCategoryId());
        fillResourceDefaults(add);
        assertStatusUsable(add.getStatus());
        add.setStatus(StringUtils.defaultIfBlank(add.getStatus(), "0"));
        boolean inserted = baseMapper.insert(add) > 0;
        if (inserted && "0".equals(add.getStatus())) {
            sendResourceCreatedNotification(add);
        }
        return inserted;
    }

    @Override
    public Boolean updateByBo(InfoResourceBo bo) {
        InfoResource up = MapstructUtils.convert(bo, InfoResource.class);
        assertCategoryUsable(up.getCategoryId());
        if (StringUtils.isBlank(up.getPreviewType())) {
            up.setPreviewType(resolvePreviewType(up.getMimeType(), up.getOriginalName()));
        }
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean updateOwnByBo(InfoResourceBo bo) {
        InfoResource current = assertOwnedResource(bo.getResourceId());
        InfoResource up = MapstructUtils.convert(bo, InfoResource.class);
        up.setResourceId(current.getResourceId());
        up.setStatus(null);
        up.setDownloadCount(null);
        up.setViewCount(null);
        assertCategoryUsable(up.getCategoryId());
        if (up.getOssId() == null) {
            up.setOssId(current.getOssId());
            up.setOriginalName(current.getOriginalName());
            up.setFileSuffix(current.getFileSuffix());
            up.setMimeType(current.getMimeType());
            up.setFileSize(current.getFileSize());
            up.setPreviewType(current.getPreviewType());
        } else if (StringUtils.isBlank(up.getPreviewType())) {
            up.setPreviewType(resolvePreviewType(up.getMimeType(), up.getOriginalName()));
        }
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeStatus(Long resourceId, String status) {
        InfoResource up = new InfoResource();
        up.setResourceId(resourceId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeOwnStatus(Long resourceId, String status) {
        assertOwnedResource(resourceId);
        assertStatusUsable(status);
        InfoResource up = new InfoResource();
        up.setResourceId(resourceId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteOwnById(Long resourceId) {
        assertOwnedResource(resourceId);
        favoriteMapper.delete(Wrappers.<InfoResourceFavorite>lambdaQuery().eq(InfoResourceFavorite::getResourceId, resourceId));
        return baseMapper.deleteById(resourceId) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids != null && !ids.isEmpty()) {
            favoriteMapper.delete(Wrappers.<InfoResourceFavorite>lambdaQuery().in(InfoResourceFavorite::getResourceId, ids));
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    private InfoResource getPortalReadableResource(Long resourceId) {
        InfoResource resource = baseMapper.selectById(resourceId);
        if (resource == null) {
            throw new ServiceException("资料不存在或已下架");
        }
        if ("0".equals(resource.getStatus())) {
            return resource;
        }
        Long userId = LoginHelper.getUserId();
        if (userId != null && userId.equals(resource.getCreateBy())) {
            return resource;
        }
        throw new ServiceException("资料不存在或已下架");
    }

    private RemoteFile resolveRemoteFile(InfoResource resource) {
        if (resource.getOssId() == null) {
            throw new ServiceException("文件不存在或暂不可访问");
        }
        List<RemoteFile> files = remoteFileService.selectByIds(String.valueOf(resource.getOssId()));
        if (files == null || files.isEmpty() || StringUtils.isBlank(files.get(0).getName())) {
            throw new ServiceException("文件不存在或暂不可访问");
        }
        return files.get(0);
    }

    private String resolveDownloadFileName(InfoResource resource, RemoteFile remoteFile) {
        String fileName = StringUtils.defaultIfBlank(resource.getOriginalName(), remoteFile.getOriginalName());
        fileName = StringUtils.defaultIfBlank(fileName, resource.getTitle());
        if (StringUtils.isBlank(fileName)) {
            return "资料下载";
        }
        String suffix = StringUtils.defaultIfBlank(resource.getFileSuffix(), remoteFile.getFileSuffix());
        if (StringUtils.isNotBlank(suffix) && !fileName.contains(".")) {
            return fileName + "." + suffix.replaceFirst("^\\.", "");
        }
        return fileName;
    }

    private String resolvePreviewPdfName(InfoResource resource, RemoteFile remoteFile) {
        String fileName = resolveDownloadFileName(resource, remoteFile);
        if (fileName.toLowerCase().endsWith(".pdf")) {
            return fileName;
        }
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            return fileName.substring(0, extensionIndex) + ".pdf";
        }
        return fileName + ".pdf";
    }

    private String resolveFileSuffix(InfoResource resource, RemoteFile remoteFile) {
        String suffix = StringUtils.defaultIfBlank(resource.getFileSuffix(), remoteFile.getFileSuffix());
        if (StringUtils.isBlank(suffix)) {
            String fileName = StringUtils.defaultIfBlank(resource.getOriginalName(), remoteFile.getOriginalName());
            suffix = StrUtil.subAfter(StringUtils.defaultString(fileName), ".", true);
        }
        return StringUtils.defaultString(suffix).replaceFirst("^\\.", "").toLowerCase();
    }

    private String resolveInlineContentType(InfoResource resource, RemoteFile remoteFile) {
        if (StringUtils.isNotBlank(resource.getMimeType())) {
            return resource.getMimeType();
        }
        return switch (resolveFileSuffix(resource, remoteFile)) {
            case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
            case "txt", "log", "md", "csv" -> MediaType.TEXT_PLAIN_VALUE + "; charset=UTF-8";
            case "html", "htm" -> MediaType.TEXT_HTML_VALUE + "; charset=UTF-8";
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "gif" -> MediaType.IMAGE_GIF_VALUE;
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "mp3" -> "audio/mpeg";
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private OssClient resolveStorage(RemoteFile remoteFile) {
        return StringUtils.isBlank(remoteFile.getService())
            ? OssFactory.instance()
            : OssFactory.instance(remoteFile.getService());
    }

    private Path downloadRemoteFile(RemoteFile remoteFile) {
        return resolveStorage(remoteFile).fileDownload(remoteFile.getName());
    }

    private Path ensurePdfPreviewFile(InfoResource resource, RemoteFile remoteFile, String suffix) throws IOException {
        Path cacheDir = Path.of(previewCacheDir);
        Files.createDirectories(cacheDir);
        Path targetFile = cacheDir.resolve(buildPdfCacheFileName(resource, remoteFile));
        if (Files.exists(targetFile) && Files.size(targetFile) > 0) {
            return targetFile;
        }

        Path downloadedFile = null;
        Path workDir = Files.createTempDirectory(cacheDir, "convert-");
        try {
            downloadedFile = downloadRemoteFile(remoteFile);
            Path sourceFile = workDir.resolve("source." + suffix);
            Files.copy(downloadedFile, sourceFile, StandardCopyOption.REPLACE_EXISTING);
            Path convertedFile = convertOfficeToPdf(sourceFile, workDir);
            Path tempTarget = Files.createTempFile(cacheDir, targetFile.getFileName().toString(), ".tmp");
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

    private void writePdfResponse(Path pdfFile, String fileName, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_PDF_VALUE);
        response.setContentLengthLong(Files.size(pdfFile));
        response.setHeader("Cache-Control", "private, max-age=3600");
        response.setHeader("Content-Disposition", buildInlineContentDisposition(fileName));
        Files.copy(pdfFile, response.getOutputStream());
    }

    private String buildInlineContentDisposition(String fileName) {
        String safeFileName = StringUtils.defaultIfBlank(fileName, "preview.pdf");
        String percentEncodedFileName = FileUtils.percentEncode(safeFileName);
        String asciiFileName = toAsciiFallbackFileName(safeFileName);
        return "inline; filename=\"%s\"; filename*=UTF-8''%s".formatted(asciiFileName, percentEncodedFileName);
    }

    private String toAsciiFallbackFileName(String fileName) {
        StringBuilder builder = new StringBuilder(fileName.length());
        for (int i = 0; i < fileName.length(); i++) {
            char ch = fileName.charAt(i);
            if (ch == '"' || ch == '\\' || ch == '\r' || ch == '\n') {
                builder.append('_');
            } else if (ch >= 0x20 && ch <= 0x7E) {
                builder.append(ch);
            } else {
                builder.append('_');
            }
        }
        String fallback = builder.toString().trim();
        return fallback.isEmpty() ? "preview.pdf" : fallback;
    }

    private InfoResource assertOwnedResource(Long resourceId) {
        Long userId = requireLogin();
        InfoResource resource = baseMapper.selectById(resourceId);
        if (resource == null) {
            throw new ServiceException("资料不存在");
        }
        if (!userId.equals(resource.getCreateBy())) {
            throw new ServiceException("只能管理自己上传的资料");
        }
        return resource;
    }

    private Long requireLogin() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        return userId;
    }

    private void assertCategoryUsable(Long categoryId) {
        if (categoryId == null) {
            throw new ServiceException("资料分类不能为空");
        }
        InfoResourceCategory category = categoryMapper.selectById(categoryId);
        if (category == null || !"0".equals(category.getStatus())) {
            throw new ServiceException("资料分类不存在或已停用");
        }
    }

    private void assertStatusUsable(String status) {
        if (!"0".equals(status) && !"1".equals(status)) {
            throw new ServiceException("资料状态不正确");
        }
    }

    private void fillResourceDefaults(InfoResource resource) {
        if (StringUtils.isBlank(resource.getStatus())) {
            resource.setStatus("0");
        }
        if (StringUtils.isBlank(resource.getPreviewType())) {
            resource.setPreviewType(resolvePreviewType(resource.getMimeType(), resource.getOriginalName()));
        }
        resource.setDownloadCount(0L);
        resource.setViewCount(0L);
    }

    private void sendResourceCreatedNotification(InfoResource resource) {
        if (resource == null || StringUtils.isBlank(resource.getTitle())) {
            return;
        }
        String uploader = userDisplayNameResolver.currentUserDisplayName("用户");
        String fileName = StringUtils.defaultIfBlank(resource.getOriginalName(), resource.getTitle());
        notificationService.sendToAllUsers(
            "新增资源：" + resource.getTitle(),
            uploader + " 上传了新资源“" + resource.getTitle() + "”（" + fileName + "），可前往资料共享中查看。",
            "resource"
        );
    }

    private boolean isActiveFilter(String value) {
        return StringUtils.isNotBlank(value) && !"all".equals(value);
    }

    private Date resolveUploadedSince(String uploadedWithin) {
        if (!isActiveFilter(uploadedWithin)) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        switch (uploadedWithin) {
            case "week" -> calendar.add(Calendar.DATE, -7);
            case "month" -> calendar.add(Calendar.MONTH, -1);
            case "quarter" -> calendar.add(Calendar.MONTH, -3);
            case "year" -> calendar.add(Calendar.YEAR, -1);
            default -> {
                return null;
            }
        }
        return calendar.getTime();
    }

    private void applySizeRange(LambdaQueryWrapper<InfoResource> w, String sizeRange) {
        if (!isActiveFilter(sizeRange)) {
            return;
        }
        switch (sizeRange) {
            case "small" -> w.lt(InfoResource::getFileSize, ONE_MB);
            case "medium" -> w.ge(InfoResource::getFileSize, ONE_MB).lt(InfoResource::getFileSize, 10 * ONE_MB);
            case "large" -> w.ge(InfoResource::getFileSize, 10 * ONE_MB);
            default -> {
            }
        }
    }

    private void applySort(LambdaQueryWrapper<InfoResource> w, String sort) {
        if ("downloads".equals(sort)) {
            w.orderByDesc(InfoResource::getDownloadCount).orderByDesc(InfoResource::getCreateTime);
        } else if ("views".equals(sort)) {
            w.orderByDesc(InfoResource::getViewCount).orderByDesc(InfoResource::getCreateTime);
        } else if ("hot".equals(sort)) {
            w.orderByDesc(InfoResource::getDownloadCount).orderByDesc(InfoResource::getViewCount).orderByDesc(InfoResource::getCreateTime);
        } else {
            w.orderByDesc(InfoResource::getCreateTime);
        }
    }

    private String resolvePreviewType(String contentType, String originalName) {
        String lowerType = StringUtils.defaultString(contentType).toLowerCase();
        String suffix = StrUtil.subAfter(StringUtils.defaultString(originalName), ".", true).toLowerCase();
        if (lowerType.startsWith("image/") || List.of("jpg", "jpeg", "png", "gif", "webp", "bmp").contains(suffix)) {
            return "image";
        }
        if (lowerType.contains("pdf") || "pdf".equals(suffix)) {
            return "pdf";
        }
        if ("ofd".equals(suffix)) {
            return "ofd";
        }
        if (List.of(
            "doc", "docx", "docm", "dot", "dotx", "dotm",
            "xls", "xlsx", "xlsm", "xlt", "xltx", "xltm", "csv", "tsv",
            "ppt", "pptx", "pptm", "pps", "ppsx",
            "wps", "wpt", "et", "ett", "dps", "dpt",
            "odt", "ods", "odp", "rtf"
        ).contains(suffix)) {
            return "office";
        }
        if (lowerType.startsWith("text/") || List.of("txt", "md", "csv", "json", "xml").contains(suffix)) {
            return "text";
        }
        if (lowerType.startsWith("video/")) {
            return "video";
        }
        if (lowerType.startsWith("audio/")) {
            return "audio";
        }
        return "file";
    }

    @Override
    public Long countPortalVisible() {
        return baseMapper.selectCount(Wrappers.<InfoResource>lambdaQuery()
            .eq(InfoResource::getStatus, "0"));
    }

    @Override
    public Long sumPortalVisits() {
        return baseMapper.sumPortalVisits();
    }

    @Override
    public List<ResourceUsageRank> listPortalUsageTop(int limit) {
        int size = Math.max(0, Math.min(limit, 20));
        if (size == 0) {
            return List.of();
        }
        Map<Long, String> categoryNames = categoryMapper.selectList(null).stream()
            .collect(Collectors.toMap(InfoResourceCategory::getCategoryId, InfoResourceCategory::getCategoryName, (left, right) -> left));
        return baseMapper.selectList(Wrappers.<InfoResource>lambdaQuery()
                .eq(InfoResource::getStatus, "0"))
            .stream()
            .sorted(Comparator.comparing((InfoResource resource) -> usageValue(resource)).reversed())
            .limit(size)
            .map(resource -> new ResourceUsageRank(
                resource.getTitle(),
                categoryNames.getOrDefault(resource.getCategoryId(), "未分类"),
                usageValue(resource)))
            .toList();
    }

    @Override
    public List<Long> listPortalActorIds() {
        Set<Long> actorIds = new LinkedHashSet<>();
        baseMapper.selectList(Wrappers.<InfoResource>lambdaQuery()
                .select(InfoResource::getCreateBy)
                .eq(InfoResource::getStatus, "0")
                .isNotNull(InfoResource::getCreateBy))
            .forEach(resource -> actorIds.add(resource.getCreateBy()));
        favoriteMapper.selectList(null).stream()
            .map(InfoResourceFavorite::getUserId)
            .filter(Objects::nonNull)
            .forEach(actorIds::add);
        return new ArrayList<>(actorIds);
    }

    @Override
    public List<DeptResourceStat> listDeptResourceStats() {
        return baseMapper.selectList(Wrappers.<InfoResource>lambdaQuery()
                .select(InfoResource::getCreateDept, InfoResource::getViewCount, InfoResource::getDownloadCount)
                .eq(InfoResource::getStatus, "0"))
            .stream()
            .filter(resource -> resource.getCreateDept() != null)
            .collect(Collectors.groupingBy(
                InfoResource::getCreateDept,
                Collectors.summingLong(this::usageValue)))
            .entrySet()
            .stream()
            .map(entry -> new DeptResourceStat(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(DeptResourceStat::value).reversed())
            .toList();
    }

    private Long usageValue(InfoResource resource) {
        if (resource == null) {
            return 0L;
        }
        return Optional.ofNullable(resource.getViewCount()).orElse(0L)
            + Optional.ofNullable(resource.getDownloadCount()).orElse(0L);
    }
}
