package org.dromara.portal.appcenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.portal.appcenter.domain.AppApplication;
import org.dromara.portal.appcenter.domain.bo.AppApplicationBo;
import org.dromara.portal.appcenter.domain.vo.AppApplicationVo;
import org.dromara.portal.appcenter.domain.vo.AppPackageUploadVo;
import org.dromara.portal.appcenter.mapper.AppApplicationMapper;
import org.dromara.portal.appcenter.service.IAppApplicationService;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.file.api.RemoteFileService;
import org.dromara.file.api.domain.RemoteFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AppApplicationServiceImpl implements IAppApplicationService {

    private final AppApplicationMapper baseMapper;
    private final IPortalNotificationService notificationService;

    @DubboReference
    private RemoteFileService remoteFileService;

    private LambdaQueryWrapper<AppApplication> buildWrapper(AppApplicationBo bo) {
        LambdaQueryWrapper<AppApplication> w = Wrappers.lambdaQuery();
        w.eq(bo.getCategoryId() != null, AppApplication::getCategoryId, bo.getCategoryId());
        w.eq(StringUtils.isNotBlank(bo.getAppType()), AppApplication::getAppType, bo.getAppType());
        w.eq(StringUtils.isNotBlank(bo.getStatus()), AppApplication::getStatus, bo.getStatus());
        w.eq(StringUtils.isNotBlank(bo.getIsSecurity()), AppApplication::getIsSecurity, bo.getIsSecurity());
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            w.and(q -> q.like(AppApplication::getAppName, bo.getKeyword())
                .or().like(AppApplication::getDescription, bo.getKeyword())
                .or().like(AppApplication::getTags, bo.getKeyword()));
        }
        w.orderByAsc(AppApplication::getOrderNum).orderByDesc(AppApplication::getCreateTime);
        return w;
    }

    @Override
    public TableDataInfo<AppApplicationVo> queryPageList(AppApplicationBo bo, PageQuery pageQuery) {
        Page<AppApplication> page = baseMapper.selectPage(pageQuery.build(), buildWrapper(bo));
        List<AppApplicationVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public List<AppApplicationVo> queryList(AppApplicationBo bo) {
        return baseMapper.selectList(buildWrapper(bo)).stream().map(this::toVo).toList();
    }

    @Override
    public AppApplicationVo queryById(Long appId) {
        return toVo(baseMapper.selectById(appId));
    }

    @Override
    public Boolean insertByBo(AppApplicationBo bo) {
        validateApplication(bo);
        AppApplication add = toEntity(bo);
        boolean inserted = baseMapper.insert(add) > 0;
        if (inserted && isOnlineOnCreate(add.getStatus())) {
            sendAppOnlineNotification(add);
        }
        return inserted;
    }

    @Override
    public Boolean updateByBo(AppApplicationBo bo) {
        validateApplication(bo);
        AppApplication before = bo == null || bo.getAppId() == null ? null : baseMapper.selectById(bo.getAppId());
        AppApplication up = toEntity(bo);
        boolean updated = baseMapper.updateById(up) > 0;
        if (updated && before != null && !isOnline(before.getStatus()) && isOnline(up.getStatus())) {
            AppApplication current = baseMapper.selectById(up.getAppId());
            sendAppOnlineNotification(current == null ? up : current);
        }
        return updated;
    }

    @Override
    public Boolean changeStatus(Long appId, String status) {
        AppApplication before = baseMapper.selectById(appId);
        AppApplication up = new AppApplication();
        up.setAppId(appId);
        up.setStatus(status);
        boolean updated = baseMapper.updateById(up) > 0;
        if (updated && before != null && !isOnline(before.getStatus()) && isOnline(status)) {
            AppApplication current = baseMapper.selectById(appId);
            sendAppOnlineNotification(current == null ? before : current);
        }
        return updated;
    }

    @Override
    public AppPackageUploadVo uploadPackage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        try {
            RemoteFile remoteFile = remoteFileService.upload(file.getName(), file.getOriginalFilename(), file.getContentType(), file.getBytes());
            AppPackageUploadVo vo = new AppPackageUploadVo();
            vo.setPackageOssId(remoteFile.getOssId());
            vo.setPackageName(StringUtils.defaultIfBlank(remoteFile.getOriginalName(), file.getOriginalFilename()));
            vo.setPackageSize(file.getSize());
            vo.setPackageUrl(remoteFile.getUrl());
            return vo;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("上传离线安装包失败");
        }
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    private AppApplication toEntity(AppApplicationBo bo) {
        if (bo == null) {
            return null;
        }
        AppApplication entity = new AppApplication();
        entity.setAppId(bo.getAppId());
        entity.setAppName(bo.getAppName());
        entity.setAppCode(bo.getAppCode());
        entity.setVersion(bo.getVersion());
        entity.setCategoryId(bo.getCategoryId());
        entity.setIcon(bo.getIcon());
        entity.setAccent(bo.getAccent());
        entity.setDescription(bo.getDescription());
        entity.setTags(bo.getTags());
        entity.setAccessUrl(StringUtils.defaultString(bo.getAccessUrl()));
        entity.setAppType(resolveAppType(bo.getAppType()));
        entity.setPackageOssId(bo.getPackageOssId());
        entity.setPackageName(bo.getPackageName());
        entity.setPackageSize(bo.getPackageSize());
        entity.setPackageUrl(bo.getPackageUrl());
        entity.setStatus(bo.getStatus());
        entity.setIsSecurity(bo.getIsSecurity());
        entity.setOrderNum(bo.getOrderNum());
        return entity;
    }

    private AppApplicationVo toVo(AppApplication entity) {
        if (entity == null) {
            return null;
        }
        AppApplicationVo vo = new AppApplicationVo();
        vo.setAppId(entity.getAppId());
        vo.setAppName(entity.getAppName());
        vo.setAppCode(entity.getAppCode());
        vo.setVersion(entity.getVersion());
        vo.setCategoryId(entity.getCategoryId());
        vo.setIcon(entity.getIcon());
        vo.setAccent(entity.getAccent());
        vo.setDescription(entity.getDescription());
        vo.setTags(entity.getTags());
        vo.setAccessUrl(entity.getAccessUrl());
        vo.setAppType(resolveAppType(entity.getAppType()));
        vo.setPackageOssId(entity.getPackageOssId());
        vo.setPackageName(entity.getPackageName());
        vo.setPackageSize(entity.getPackageSize());
        vo.setPackageUrl(entity.getPackageUrl());
        vo.setStatus(entity.getStatus());
        vo.setIsSecurity(entity.getIsSecurity());
        vo.setUseCount(entity.getUseCount());
        vo.setRecommendCount(entity.getRecommendCount());
        vo.setOrderNum(entity.getOrderNum());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }

    private boolean isOnlineOnCreate(String status) {
        return StringUtils.isBlank(status) || "0".equals(status);
    }

    private boolean isOnline(String status) {
        return "0".equals(status);
    }

    private String resolveAppType(String appType) {
        if (StringUtils.isBlank(appType)) {
            return "online";
        }
        return appType;
    }

    private void validateApplication(AppApplicationBo bo) {
        if (bo == null) {
            throw new ServiceException("应用信息不能为空");
        }
        String appType = resolveAppType(bo.getAppType());
        if (!List.of("business", "online", "offline").contains(appType)) {
            throw new ServiceException("应用类型不正确");
        }
        if (!"offline".equals(appType) && StringUtils.isBlank(bo.getAccessUrl())) {
            throw new ServiceException("业务应用和在线工具必须配置访问地址");
        }
        if ("offline".equals(appType) && bo.getPackageOssId() == null) {
            throw new ServiceException("离线工具必须上传安装包");
        }
    }

    private void sendAppOnlineNotification(AppApplication app) {
        if (app == null || StringUtils.isBlank(app.getAppName())) {
            return;
        }
        String version = StringUtils.isBlank(app.getVersion()) ? "" : " " + app.getVersion();
        notificationService.sendToAllUsers(
            "应用上架：" + app.getAppName(),
            "应用中心已上架“" + app.getAppName() + "”" + version + "，可前往应用中心中打开使用。",
            "app"
        );
    }

    @Override
    public Long countPortalVisible() {
        return baseMapper.selectCount(Wrappers.<AppApplication>lambdaQuery()
            .eq(AppApplication::getStatus, "0"));
    }
}
