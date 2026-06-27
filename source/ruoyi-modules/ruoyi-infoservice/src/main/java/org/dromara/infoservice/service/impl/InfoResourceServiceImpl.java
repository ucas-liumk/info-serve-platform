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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InfoResourceServiceImpl implements IInfoResourceService {

    private final InfoResourceMapper baseMapper;
    private final InfoResourceCategoryMapper categoryMapper;

    @DubboReference
    private RemoteFileService remoteFileService;

    private LambdaQueryWrapper<InfoResource> buildWrapper(InfoResourceBo bo, boolean portal) {
        LambdaQueryWrapper<InfoResource> w = Wrappers.lambdaQuery();
        w.eq(bo.getCategoryId() != null, InfoResource::getCategoryId, bo.getCategoryId());
        w.eq(StringUtils.isNotBlank(bo.getStatus()), InfoResource::getStatus, bo.getStatus());
        if (portal) {
            w.eq(InfoResource::getStatus, "0");
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
        w.orderByDesc(InfoResource::getCreateTime);
        return w;
    }

    private void fillCategoryName(List<InfoResourceVo> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> ids = rows.stream().map(InfoResourceVo::getCategoryId).distinct().toList();
        Map<Long, String> categoryNames = categoryMapper.selectList(Wrappers.<InfoResourceCategory>lambdaQuery().in(InfoResourceCategory::getCategoryId, ids))
            .stream().collect(Collectors.toMap(InfoResourceCategory::getCategoryId, InfoResourceCategory::getCategoryName, (a, b) -> a));
        rows.forEach(row -> row.setCategoryName(categoryNames.get(row.getCategoryId())));
    }

    @Override
    public TableDataInfo<InfoResourceVo> queryPageList(InfoResourceBo bo, PageQuery pageQuery) {
        Page<InfoResourceVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo, false));
        fillCategoryName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<InfoResourceVo> portalPage(InfoResourceBo bo, PageQuery pageQuery) {
        Page<InfoResourceVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo, true));
        fillCategoryName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public InfoResourceVo queryById(Long resourceId) {
        InfoResourceVo vo = baseMapper.selectVoById(resourceId);
        fillCategoryName(vo == null ? Collections.emptyList() : List.of(vo));
        return vo;
    }

    @Override
    public InfoResourceVo queryPortalDetail(Long resourceId) {
        InfoResource resource = getUsableResource(resourceId);
        baseMapper.update(null, Wrappers.<InfoResource>lambdaUpdate()
            .setSql("view_count = view_count + 1")
            .eq(InfoResource::getResourceId, resourceId));
        InfoResourceVo vo = MapstructUtils.convert(resource, InfoResourceVo.class);
        fillCategoryName(List.of(vo));
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
    public String resolveFileUrl(Long resourceId, boolean download) {
        InfoResource resource = getUsableResource(resourceId);
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
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("0");
        }
        if (StringUtils.isBlank(add.getPreviewType())) {
            add.setPreviewType(resolvePreviewType(add.getMimeType(), add.getOriginalName()));
        }
        add.setDownloadCount(0L);
        add.setViewCount(0L);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(InfoResourceBo bo) {
        InfoResource up = MapstructUtils.convert(bo, InfoResource.class);
        if (StringUtils.isBlank(up.getPreviewType())) {
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
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    private InfoResource getUsableResource(Long resourceId) {
        InfoResource resource = baseMapper.selectById(resourceId);
        if (resource == null || !"0".equals(resource.getStatus())) {
            throw new ServiceException("资料不存在或已下架");
        }
        return resource;
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
