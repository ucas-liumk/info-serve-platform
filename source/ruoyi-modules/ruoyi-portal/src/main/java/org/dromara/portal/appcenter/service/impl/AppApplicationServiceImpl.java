package org.dromara.portal.appcenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.portal.appcenter.domain.AppApplication;
import org.dromara.portal.appcenter.domain.AppAccessScope;
import org.dromara.portal.appcenter.domain.bo.AppApplicationBo;
import org.dromara.portal.appcenter.domain.vo.AppApplicationVo;
import org.dromara.portal.appcenter.domain.vo.AppPackageUploadVo;
import org.dromara.portal.appcenter.mapper.AppAccessScopeMapper;
import org.dromara.portal.appcenter.mapper.AppApplicationMapper;
import org.dromara.portal.appcenter.service.IAppApplicationService;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.file.api.RemoteFileService;
import org.dromara.file.api.domain.RemoteFile;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.api.model.RoleDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AppApplicationServiceImpl implements IAppApplicationService {

    private static final String ACCESS_MODE_ALL = "all";
    private static final String ACCESS_MODE_ROLE = "role";
    private static final String ACCESS_MODE_USER = "user";
    private static final String TARGET_ROLE = "role";
    private static final String TARGET_USER = "user";

    private final AppApplicationMapper baseMapper;
    private final AppAccessScopeMapper accessScopeMapper;
    private final IPortalNotificationService notificationService;

    @DubboReference
    private RemoteFileService remoteFileService;

    private LambdaQueryWrapper<AppApplication> buildWrapper(AppApplicationBo bo) {
        LambdaQueryWrapper<AppApplication> w = Wrappers.lambdaQuery();
        w.eq(bo.getCategoryId() != null, AppApplication::getCategoryId, bo.getCategoryId());
        w.eq(StringUtils.isNotBlank(bo.getAppType()), AppApplication::getAppType, bo.getAppType());
        w.eq(StringUtils.isNotBlank(bo.getStatus()), AppApplication::getStatus, bo.getStatus());
        w.eq(StringUtils.isNotBlank(bo.getIsSecurity()), AppApplication::getIsSecurity, bo.getIsSecurity());
        w.eq(StringUtils.isNotBlank(bo.getAccessMode()), AppApplication::getAccessMode, bo.getAccessMode());
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
        fillAccessScopes(rows);
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public List<AppApplicationVo> queryList(AppApplicationBo bo) {
        List<AppApplicationVo> rows = baseMapper.selectList(buildWrapper(bo)).stream().map(this::toVo).toList();
        fillAccessScopes(rows);
        return rows;
    }

    @Override
    public AppApplicationVo queryById(Long appId) {
        AppApplicationVo vo = toVo(baseMapper.selectById(appId));
        if (vo != null) {
            fillAccessScopes(List.of(vo));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(AppApplicationBo bo) {
        validateApplication(bo);
        AppApplication add = toEntity(bo);
        boolean inserted = baseMapper.insert(add) > 0;
        if (inserted) {
            syncAccessScopes(add.getAppId(), bo);
        }
        if (inserted && isOnlineOnCreate(add.getStatus()) && isAllUsers(add.getAccessMode())) {
            sendAppOnlineNotification(add);
        }
        return inserted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(AppApplicationBo bo) {
        validateApplication(bo);
        AppApplication before = bo == null || bo.getAppId() == null ? null : baseMapper.selectById(bo.getAppId());
        AppApplication up = toEntity(bo);
        boolean updated = baseMapper.updateById(up) > 0;
        if (updated) {
            syncAccessScopes(up.getAppId(), bo);
        }
        if (updated && before != null && !isOnline(before.getStatus()) && isOnline(up.getStatus()) && isAllUsers(up.getAccessMode())) {
            AppApplication current = baseMapper.selectById(up.getAppId());
            sendAppOnlineNotification(current == null ? up : current);
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changeStatus(Long appId, String status) {
        AppApplication before = baseMapper.selectById(appId);
        AppApplication up = new AppApplication();
        up.setAppId(appId);
        up.setStatus(status);
        boolean updated = baseMapper.updateById(up) > 0;
        if (updated && before != null && !isOnline(before.getStatus()) && isOnline(status) && isAllUsers(before.getAccessMode())) {
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
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        accessScopeMapper.delete(Wrappers.<AppAccessScope>lambdaQuery().in(AppAccessScope::getAppId, ids));
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
        entity.setAccessMode(resolveAccessMode(bo.getAccessMode()));
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
        vo.setAccessMode(resolveAccessMode(entity.getAccessMode()));
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

    private boolean isAllUsers(String accessMode) {
        return ACCESS_MODE_ALL.equals(resolveAccessMode(accessMode));
    }

    private String resolveAppType(String appType) {
        if (StringUtils.isBlank(appType)) {
            return "online";
        }
        return appType;
    }

    private String resolveAccessMode(String accessMode) {
        if (StringUtils.isBlank(accessMode)) {
            return ACCESS_MODE_ALL;
        }
        return accessMode;
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
            throw new ServiceException("自研应用和开源应用必须配置访问地址");
        }
        if ("offline".equals(appType) && bo.getPackageOssId() == null) {
            throw new ServiceException("离线应用必须上传安装包");
        }
        String accessMode = resolveAccessMode(bo.getAccessMode());
        if (!List.of(ACCESS_MODE_ALL, ACCESS_MODE_ROLE, ACCESS_MODE_USER).contains(accessMode)) {
            throw new ServiceException("开放范围不正确");
        }
        if (ACCESS_MODE_ROLE.equals(accessMode) && normalizeIds(bo.getRoleIds()).isEmpty()) {
            throw new ServiceException("指定角色开放时至少选择一个角色");
        }
        if (ACCESS_MODE_USER.equals(accessMode) && normalizeIds(bo.getUserIds()).isEmpty()) {
            throw new ServiceException("指定用户开放时至少选择一个用户");
        }
    }

    private void syncAccessScopes(Long appId, AppApplicationBo bo) {
        if (appId == null) {
            return;
        }
        accessScopeMapper.delete(Wrappers.<AppAccessScope>lambdaQuery().eq(AppAccessScope::getAppId, appId));
        String accessMode = resolveAccessMode(bo.getAccessMode());
        if (ACCESS_MODE_ROLE.equals(accessMode)) {
            insertAccessScopes(appId, TARGET_ROLE, normalizeIds(bo.getRoleIds()));
        } else if (ACCESS_MODE_USER.equals(accessMode)) {
            insertAccessScopes(appId, TARGET_USER, normalizeIds(bo.getUserIds()));
        }
    }

    private void insertAccessScopes(Long appId, String targetType, List<Long> targetIds) {
        Date now = new Date();
        for (Long targetId : targetIds) {
            AppAccessScope scope = new AppAccessScope();
            scope.setAppId(appId);
            scope.setTargetType(targetType);
            scope.setTargetId(targetId);
            scope.setCreateTime(now);
            accessScopeMapper.insert(scope);
        }
    }

    private List<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
            .filter(id -> id != null && id > 0)
            .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    private void fillAccessScopes(List<AppApplicationVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> appIds = rows.stream().filter(row -> row != null && row.getAppId() != null).map(AppApplicationVo::getAppId).toList();
        if (appIds.isEmpty()) {
            return;
        }
        List<AppAccessScope> scopes = accessScopeMapper.selectList(
            Wrappers.<AppAccessScope>lambdaQuery().in(AppAccessScope::getAppId, appIds));
        for (AppApplicationVo row : rows) {
            if (row == null || row.getAppId() == null) {
                continue;
            }
            row.setRoleIds(filterScopeIds(scopes, row.getAppId(), TARGET_ROLE));
            row.setUserIds(filterScopeIds(scopes, row.getAppId(), TARGET_USER));
        }
    }

    private List<Long> filterScopeIds(List<AppAccessScope> scopes, Long appId, String targetType) {
        return scopes.stream()
            .filter(scope -> appId.equals(scope.getAppId()) && targetType.equals(scope.getTargetType()))
            .map(AppAccessScope::getTargetId)
            .toList();
    }

    private void applyCurrentUserAccessFilter(LambdaQueryWrapper<AppApplication> wrapper) {
        if (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin()) {
            return;
        }
        Long userId = LoginHelper.getUserId();
        List<Long> roleIds = currentRoleIds();
        String userExists = "SELECT 1 FROM app_access_scope aas WHERE aas.app_id = app_application.app_id"
            + " AND aas.tenant_id = app_application.tenant_id AND aas.target_type = 'user' AND aas.target_id = " + userId;
        wrapper.and(q -> {
            q.isNull(AppApplication::getAccessMode)
                .or().eq(AppApplication::getAccessMode, ACCESS_MODE_ALL)
                .or(s -> s.eq(AppApplication::getAccessMode, ACCESS_MODE_USER).exists(userExists));
            if (!roleIds.isEmpty()) {
                String roleExists = "SELECT 1 FROM app_access_scope aas WHERE aas.app_id = app_application.app_id"
                    + " AND aas.tenant_id = app_application.tenant_id AND aas.target_type = 'role' AND aas.target_id IN ("
                    + roleIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
                q.or(s -> s.eq(AppApplication::getAccessMode, ACCESS_MODE_ROLE).exists(roleExists));
            }
        });
    }

    private List<Long> currentRoleIds() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null || loginUser.getRoles() == null) {
            return Collections.emptyList();
        }
        return loginUser.getRoles().stream()
            .map(RoleDTO::getRoleId)
            .filter(id -> id != null && id > 0)
            .toList();
    }

    private void sendAppOnlineNotification(AppApplication app) {
        if (app == null || StringUtils.isBlank(app.getAppName())) {
            return;
        }
        String version = StringUtils.isBlank(app.getVersion()) ? "" : " " + app.getVersion();
        notificationService.sendToAllUsers(
            "应用上架：" + app.getAppName(),
            "应用中心已上架“" + app.getAppName() + "”" + version + "，可前往应用中心打开使用。",
            "app"
        );
    }

    @Override
    public Long countPortalVisible() {
        LambdaQueryWrapper<AppApplication> wrapper = Wrappers.<AppApplication>lambdaQuery()
            .eq(AppApplication::getStatus, "0");
        applyCurrentUserAccessFilter(wrapper);
        return baseMapper.selectCount(wrapper);
    }
}
