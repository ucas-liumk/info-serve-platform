package org.dromara.infoservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.infoservice.domain.InfoResource;
import org.dromara.infoservice.domain.InfoResourceCategory;
import org.dromara.infoservice.domain.bo.InfoResourceBo;
import org.dromara.infoservice.domain.vo.InfoResourceVo;
import org.dromara.infoservice.domain.vo.ResourceUploadVo;
import org.dromara.infoservice.mapper.InfoResourceCategoryMapper;
import org.dromara.infoservice.mapper.InfoResourceMapper;
import org.dromara.infoservice.service.IInfoResourceService;
import org.dromara.resource.api.RemoteFileService;
import org.dromara.resource.api.domain.RemoteFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InfoResourceServiceImpl implements IInfoResourceService {

    private static final String SCOPE_MINE = "mine";
    private static final long ONE_MB = 1024L * 1024L;

    private final InfoResourceMapper baseMapper;
    private final InfoResourceCategoryMapper categoryMapper;

    @DubboReference
    private RemoteFileService remoteFileService;

    private LambdaQueryWrapper<InfoResource> buildWrapper(InfoResourceBo bo, boolean portal) {
        LambdaQueryWrapper<InfoResource> w = Wrappers.lambdaQuery();
        boolean mineScope = portal && SCOPE_MINE.equals(bo.getScope());
        w.eq(bo.getCategoryId() != null, InfoResource::getCategoryId, bo.getCategoryId());
        if (portal) {
            if (mineScope) {
                w.eq(InfoResource::getCreateBy, requireLogin());
                if (isActiveFilter(bo.getStatus())) {
                    w.eq(InfoResource::getStatus, bo.getStatus());
                }
            } else {
                w.eq(InfoResource::getStatus, "0");
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
        List<Long> ids = rows.stream().map(InfoResourceVo::getCategoryId).distinct().toList();
        Map<Long, String> categoryNames = categoryMapper.selectList(Wrappers.<InfoResourceCategory>lambdaQuery().in(InfoResourceCategory::getCategoryId, ids))
            .stream().collect(Collectors.toMap(InfoResourceCategory::getCategoryId, InfoResourceCategory::getCategoryName, (a, b) -> a));
        Long currentUserId = LoginHelper.getUserId();
        String currentUsername = StringUtils.defaultIfBlank(LoginHelper.getUsername(), "我");
        rows.forEach(row -> {
            row.setCategoryName(categoryNames.get(row.getCategoryId()));
            boolean mine = currentUserId != null && currentUserId.equals(row.getCreateBy());
            row.setCanManage(mine);
            row.setOwnerName(mine ? currentUsername : "平台用户");
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
            throw new ServiceException("上传文件失败");
        }
    }

    @Override
    public ResourceUploadVo portalUpload(MultipartFile file) {
        requireLogin();
        return upload(file);
    }

    @Override
    public String resolveFileUrl(Long resourceId, boolean download) {
        InfoResource resource = getPortalReadableResource(resourceId);
        String url = remoteFileService.selectUrlByIds(String.valueOf(resource.getOssId()));
        if (StringUtils.isBlank(url)) {
            throw new ServiceException("文件不存在或暂不可访问");
        }
        if (download) {
            baseMapper.update(null, Wrappers.<InfoResource>lambdaUpdate()
                .setSql("download_count = download_count + 1")
                .eq(InfoResource::getResourceId, resourceId));
        }
        return url;
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
        return baseMapper.insert(add) > 0;
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
        return baseMapper.deleteById(resourceId) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
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
}
