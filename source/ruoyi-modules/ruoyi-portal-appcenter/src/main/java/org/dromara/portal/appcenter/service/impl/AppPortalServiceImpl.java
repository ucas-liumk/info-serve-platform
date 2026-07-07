package org.dromara.portal.appcenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.dromara.portal.appcenter.domain.*;
import org.dromara.portal.appcenter.domain.bo.AppDemandSubmitBo;
import org.dromara.portal.appcenter.domain.vo.*;
import org.dromara.portal.appcenter.mapper.*;
import org.dromara.portal.appcenter.service.IAppDemandService;
import org.dromara.portal.appcenter.service.IAppPortalService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.core.utils.file.FileUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.oss.core.OssClient;
import org.dromara.common.oss.factory.OssFactory;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.file.api.RemoteFileService;
import org.dromara.file.api.domain.RemoteFile;
import org.dromara.system.api.model.LoginUser;
import org.dromara.system.api.model.RoleDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AppPortalServiceImpl implements IAppPortalService {

    private static final String ACCESS_MODE_ALL = "all";
    private static final String ACCESS_MODE_ROLE = "role";
    private static final String ACCESS_MODE_USER = "user";
    private static final String TARGET_ROLE = "role";
    private static final String TARGET_USER = "user";

    private final AppApplicationMapper applicationMapper;
    private final AppAccessScopeMapper accessScopeMapper;
    private final AppCategoryMapper categoryMapper;
    private final AppFavoriteMapper favoriteMapper;
    private final AppRecommendMapper recommendMapper;
    private final IAppDemandService demandService;

    @DubboReference
    private RemoteFileService remoteFileService;

    @Override
    public List<AppCategoryVo> categories() {
        LambdaQueryWrapper<AppApplication> appCountWrapper = Wrappers.<AppApplication>lambdaQuery()
            .eq(AppApplication::getStatus, "0");
        applyCurrentUserAccessFilter(appCountWrapper);
        Map<Long, Long> appCounts = applicationMapper.selectList(appCountWrapper)
            .stream()
            .collect(Collectors.groupingBy(AppApplication::getCategoryId, Collectors.counting()));
        return categoryMapper.selectList(
            Wrappers.<AppCategory>lambdaQuery()
                .eq(AppCategory::getStatus, "0")
                .orderByAsc(AppCategory::getOrderNum))
            .stream().map(category -> toCategoryVo(category, appCounts.getOrDefault(category.getCategoryId(), 0L))).toList();
    }

    @Override
    public TableDataInfo<PortalAppVo> apps(String categoryCode, String keyword, String appType, String sort, PageQuery pageQuery) {
        LambdaQueryWrapper<AppApplication> w = Wrappers.lambdaQuery();
        w.eq(AppApplication::getStatus, "0");
        applyCurrentUserAccessFilter(w);
        if (StringUtils.isNotBlank(appType) && !"all".equals(appType)) {
            w.eq(AppApplication::getAppType, appType);
        }
        if (StringUtils.isNotBlank(categoryCode) && !"all".equals(categoryCode)) {
            AppCategory c = categoryMapper.selectOne(
                Wrappers.<AppCategory>lambdaQuery().eq(AppCategory::getCategoryCode, categoryCode));
            w.eq(AppApplication::getCategoryId, c == null ? -1L : c.getCategoryId());
        }
        if (StringUtils.isNotBlank(keyword)) {
            w.and(q -> q.like(AppApplication::getAppName, keyword)
                .or().like(AppApplication::getDescription, keyword)
                .or().like(AppApplication::getTags, keyword));
        }
        if ("hot".equals(sort)) {
            w.orderByDesc(AppApplication::getRecommendCount);
        } else if ("use".equals(sort)) {
            w.orderByDesc(AppApplication::getUseCount);
        } else {
            w.orderByDesc(AppApplication::getCreateTime);
        }
        Page<AppApplication> page = applicationMapper.selectPage(pageQuery.build(), w);
        List<PortalAppVo> rows = toPortalVos(page.getRecords());
        TableDataInfo<PortalAppVo> dataInfo = new TableDataInfo<>();
        dataInfo.setRows(rows);
        dataInfo.setTotal(page.getTotal());
        return dataInfo;
    }

    private List<PortalAppVo> toPortalVos(List<AppApplication> apps) {
        if (apps.isEmpty()) {
            return Collections.emptyList();
        }
        Long userId = LoginHelper.getUserId();
        List<Long> appIds = apps.stream().map(AppApplication::getAppId).collect(Collectors.toList());
        Set<Long> favs = favoriteMapper.selectList(Wrappers.<AppFavorite>lambdaQuery()
                .eq(AppFavorite::getUserId, userId).in(AppFavorite::getAppId, appIds))
            .stream().map(AppFavorite::getAppId).collect(Collectors.toSet());
        Map<Long, Long> favoriteCounts = favoriteMapper.selectList(Wrappers.<AppFavorite>lambdaQuery()
                .in(AppFavorite::getAppId, appIds))
            .stream()
            .collect(Collectors.groupingBy(AppFavorite::getAppId, Collectors.counting()));
        Set<Long> recs = recommendMapper.selectList(Wrappers.<AppRecommend>lambdaQuery()
                .eq(AppRecommend::getUserId, userId).in(AppRecommend::getAppId, appIds))
            .stream().map(AppRecommend::getAppId).collect(Collectors.toSet());
        Map<Long, String> catNames = categoryMapper.selectList(null).stream()
            .collect(Collectors.toMap(AppCategory::getCategoryId, AppCategory::getCategoryName, (a, b) -> a));
        return apps.stream().map(a -> {
            PortalAppVo v = new PortalAppVo();
            v.setAppId(a.getAppId());
            v.setAppName(a.getAppName());
            v.setVersion(a.getVersion());
            v.setCategoryId(a.getCategoryId());
            v.setIcon(a.getIcon());
            v.setAccent(a.getAccent());
            v.setDescription(a.getDescription());
            v.setTags(a.getTags());
            v.setAccessUrl(a.getAccessUrl());
            v.setAppType(resolveAppType(a.getAppType()));
            v.setPackageName(a.getPackageName());
            v.setPackageSize(a.getPackageSize());
            v.setIsSecurity(a.getIsSecurity());
            v.setUseCount(Optional.ofNullable(a.getUseCount()).orElse(0L));
            v.setFavoriteCount(favoriteCounts.getOrDefault(a.getAppId(), 0L));
            v.setRecommendCount(Optional.ofNullable(a.getRecommendCount()).orElse(0L));
            v.setCategoryName(catNames.get(a.getCategoryId()));
            v.setFavorited(favs.contains(a.getAppId()));
            v.setRecommended(recs.contains(a.getAppId()));
            return v;
        }).collect(Collectors.toList());
    }

    @Override
    public String use(Long appId) {
        AppApplication app = applicationMapper.selectById(appId);
        if (app == null || !"0".equals(app.getStatus())) {
            throw new ServiceException("应用不存在或已下架");
        }
        assertCurrentUserCanAccess(app);
        if ("offline".equals(resolveAppType(app.getAppType()))) {
            throw new ServiceException("离线应用请下载安装包后使用");
        }
        applicationMapper.update(null, Wrappers.<AppApplication>lambdaUpdate()
            .setSql("use_count = COALESCE(use_count, 0) + 1").eq(AppApplication::getAppId, appId));
        return app.getAccessUrl();
    }

    @Override
    public void downloadPackage(Long appId, HttpServletResponse response) throws IOException {
        AppApplication app = applicationMapper.selectById(appId);
        if (app == null || !"0".equals(app.getStatus())) {
            throw new ServiceException("应用不存在或已下架");
        }
        assertCurrentUserCanAccess(app);
        if (!"offline".equals(resolveAppType(app.getAppType())) || app.getPackageOssId() == null) {
            throw new ServiceException("该应用未配置离线安装包");
        }
        List<RemoteFile> files = remoteFileService.selectByIds(String.valueOf(app.getPackageOssId()));
        if (files == null || files.isEmpty() || StringUtils.isBlank(files.get(0).getName())) {
            throw new ServiceException("离线安装包不存在或已被删除");
        }
        RemoteFile remoteFile = files.get(0);
        String fileName = StringUtils.defaultIfBlank(app.getPackageName(), remoteFile.getOriginalName());
        fileName = StringUtils.defaultIfBlank(fileName, app.getAppName());
        response.setContentType(StringUtils.defaultIfBlank(FileUtils.getMimeType(fileName), "application/octet-stream"));
        FileUtils.setAttachmentResponseHeader(response, fileName);
        response.addHeader("Access-Control-Expose-Headers", "download-filename, Content-Disposition");
        resolveStorage(remoteFile).download(remoteFile.getName(), response.getOutputStream(), response::setContentLengthLong);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long appId, boolean add) {
        if (add) {
            AppApplication app = applicationMapper.selectById(appId);
            if (app == null || !"0".equals(app.getStatus())) {
                throw new ServiceException("应用不存在或已下架");
            }
            assertCurrentUserCanAccess(app);
        }
        Long userId = LoginHelper.getUserId();
        Long exist = favoriteMapper.selectCount(Wrappers.<AppFavorite>lambdaQuery()
            .eq(AppFavorite::getUserId, userId).eq(AppFavorite::getAppId, appId));
        if (add && exist == 0) {
            AppFavorite f = new AppFavorite();
            f.setAppId(appId);
            f.setUserId(userId);
            f.setCreateTime(new Date());
            favoriteMapper.insert(f);
        } else if (!add && exist > 0) {
            favoriteMapper.delete(Wrappers.<AppFavorite>lambdaQuery()
                .eq(AppFavorite::getUserId, userId).eq(AppFavorite::getAppId, appId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recommend(Long appId, boolean add) {
        if (add) {
            AppApplication app = applicationMapper.selectById(appId);
            if (app == null || !"0".equals(app.getStatus())) {
                throw new ServiceException("应用不存在或已下架");
            }
            assertCurrentUserCanAccess(app);
        }
        Long userId = LoginHelper.getUserId();
        Long exist = recommendMapper.selectCount(Wrappers.<AppRecommend>lambdaQuery()
            .eq(AppRecommend::getUserId, userId).eq(AppRecommend::getAppId, appId));
        if (add && exist == 0) {
            AppRecommend r = new AppRecommend();
            r.setAppId(appId);
            r.setUserId(userId);
            r.setCreateTime(new Date());
            recommendMapper.insert(r);
            applicationMapper.update(null, Wrappers.<AppApplication>lambdaUpdate()
                .setSql("recommend_count = recommend_count + 1").eq(AppApplication::getAppId, appId));
        } else if (!add && exist > 0) {
            recommendMapper.delete(Wrappers.<AppRecommend>lambdaQuery()
                .eq(AppRecommend::getUserId, userId).eq(AppRecommend::getAppId, appId));
            applicationMapper.update(null, Wrappers.<AppApplication>lambdaUpdate()
                .setSql("recommend_count = GREATEST(recommend_count - 1, 0)").eq(AppApplication::getAppId, appId));
        }
    }

    @Override
    public TableDataInfo<PortalAppVo> favorites(PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        List<Long> appIds = favoriteMapper.selectList(Wrappers.<AppFavorite>lambdaQuery()
                .eq(AppFavorite::getUserId, userId).orderByDesc(AppFavorite::getCreateTime))
            .stream().map(AppFavorite::getAppId).collect(Collectors.toList());
        TableDataInfo<PortalAppVo> info = new TableDataInfo<>();
        if (appIds.isEmpty()) {
            info.setRows(Collections.emptyList());
            info.setTotal(0);
            return info;
        }
        LambdaQueryWrapper<AppApplication> wrapper = Wrappers.<AppApplication>lambdaQuery()
            .in(AppApplication::getAppId, appIds)
            .eq(AppApplication::getStatus, "0");
        applyCurrentUserAccessFilter(wrapper);
        Page<AppApplication> page = applicationMapper.selectPage(pageQuery.build(), wrapper);
        info.setRows(toPortalVos(page.getRecords()));
        info.setTotal(page.getTotal());
        return info;
    }

    @Override
    public Boolean submitDemand(AppDemandSubmitBo bo) {
        return demandService.submitFromPortal(bo);
    }

    @Override
    public TableDataInfo<AppDemandVo> myDemands(PageQuery pageQuery) {
        return demandService.queryMyPageList(pageQuery);
    }

    @Override
    public Boolean deleteMyDemand(Long demandId) {
        return demandService.deleteOwnById(demandId);
    }

    private AppCategoryVo toCategoryVo(AppCategory entity, Long appCount) {
        if (entity == null) {
            return null;
        }
        AppCategoryVo vo = new AppCategoryVo();
        vo.setCategoryId(entity.getCategoryId());
        vo.setCategoryName(entity.getCategoryName());
        vo.setCategoryCode(entity.getCategoryCode());
        vo.setIcon(entity.getIcon());
        vo.setOrderNum(entity.getOrderNum());
        vo.setStatus(entity.getStatus());
        vo.setAppCount(appCount);
        return vo;
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

    private void assertCurrentUserCanAccess(AppApplication app) {
        if (currentUserCanAccess(app)) {
            return;
        }
        throw new ServiceException("暂无该应用使用权限");
    }

    private boolean currentUserCanAccess(AppApplication app) {
        if (app == null) {
            return false;
        }
        if (LoginHelper.isSuperAdmin() || LoginHelper.isTenantAdmin()) {
            return true;
        }
        String accessMode = resolveAccessMode(app.getAccessMode());
        if (ACCESS_MODE_ALL.equals(accessMode)) {
            return true;
        }
        Long userId = LoginHelper.getUserId();
        if (ACCESS_MODE_USER.equals(accessMode)) {
            return accessScopeMapper.exists(Wrappers.<AppAccessScope>lambdaQuery()
                .eq(AppAccessScope::getAppId, app.getAppId())
                .eq(AppAccessScope::getTargetType, TARGET_USER)
                .eq(AppAccessScope::getTargetId, userId));
        }
        if (ACCESS_MODE_ROLE.equals(accessMode)) {
            List<Long> roleIds = currentRoleIds();
            return !roleIds.isEmpty() && accessScopeMapper.exists(Wrappers.<AppAccessScope>lambdaQuery()
                .eq(AppAccessScope::getAppId, app.getAppId())
                .eq(AppAccessScope::getTargetType, TARGET_ROLE)
                .in(AppAccessScope::getTargetId, roleIds));
        }
        return false;
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

    private OssClient resolveStorage(RemoteFile remoteFile) {
        return StringUtils.isBlank(remoteFile.getService())
            ? OssFactory.instance()
            : OssFactory.instance(remoteFile.getService());
    }
}
