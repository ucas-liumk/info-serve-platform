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
import org.dromara.common.redis.utils.RedisUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.InfoResourceCategory;
import org.dromara.portal.resources.domain.InfoResourceFavorite;
import org.dromara.portal.resources.domain.InfoResourceViewRecord;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.vo.InfoResourceVo;
import org.dromara.portal.resources.domain.vo.ResourceUploadVo;
import org.dromara.portal.resources.mapper.InfoResourceCategoryMapper;
import org.dromara.portal.resources.mapper.InfoResourceFavoriteMapper;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.portal.resources.mapper.InfoResourceViewRecordMapper;
import org.dromara.common.portalevent.publisher.PortalEventPublisher;
import org.dromara.portal.api.event.PortalEventConstants;
import org.dromara.portal.api.event.PortalNotificationEvent;
import org.dromara.portal.resources.mq.ResourceConvertListener;
import org.dromara.portal.resources.mq.ResourceConvertMessage;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.dromara.portal.resources.support.DocumentPreviewConverter;
import org.dromara.portal.resources.support.ResourceUserDisplayNameResolver;
import org.dromara.file.api.RemoteFileService;
import org.dromara.file.api.domain.RemoteFile;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private final InfoResourceViewRecordMapper viewRecordMapper;
    private final PortalEventPublisher eventPublisher;
    private final ResourceUserDisplayNameResolver userDisplayNameResolver;
    private final DocumentPreviewConverter previewConverter;
    private final RabbitTemplate rabbitTemplate;

    @DubboReference
    private RemoteFileService remoteFileService;

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
        recordResourceView(resourceId);
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

        Path targetFile = previewConverter.cacheFileFor(resource, remoteFile);
        if (!previewConverter.isReady(targetFile)) {
            enqueueConversion(resource.getResourceId());
            previewConverter.awaitReady(targetFile);
        }
        writePdfResponse(targetFile, resolvePreviewPdfName(resource, remoteFile), response);
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
            enqueueConversionIfConvertible(add);
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

    private void recordResourceView(Long resourceId) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) { return; }
        InfoResourceViewRecord record = new InfoResourceViewRecord();
        record.setResourceId(resourceId);
        record.setUserId(userId);
        record.setUserName(userDisplayNameResolver.currentUserDisplayName("我"));
        record.setActionType("view");
        record.setTenantId(LoginHelper.getTenantId());
        try {
            viewRecordMapper.insert(record);
        } catch (RuntimeException e) {
            log.warn("记录资料阅看失败，已跳过阅看记录写入，resourceId={}, userId={}", resourceId, userId, e);
        }
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
        eventPublisher.publishNotification(
            PortalEventConstants.RK_RESOURCES_ITEM_PUBLISHED,
            PortalNotificationEvent.toAll(
                "resource",
                "新增资源：" + resource.getTitle(),
                uploader + " 上传了新资源“" + resource.getTitle() + "”（" + fileName + "），可前往资料共享中查看。"
            ));
    }

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
}
