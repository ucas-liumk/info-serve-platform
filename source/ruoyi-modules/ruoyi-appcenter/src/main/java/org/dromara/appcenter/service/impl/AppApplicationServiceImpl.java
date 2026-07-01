package org.dromara.appcenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.appcenter.domain.AppApplication;
import org.dromara.appcenter.domain.bo.AppApplicationBo;
import org.dromara.appcenter.domain.vo.AppApplicationVo;
import org.dromara.appcenter.mapper.AppApplicationMapper;
import org.dromara.appcenter.service.IAppApplicationService;
import org.dromara.appcenter.service.IPortalNotificationService;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AppApplicationServiceImpl implements IAppApplicationService {

    private final AppApplicationMapper baseMapper;
    private final IPortalNotificationService notificationService;

    private LambdaQueryWrapper<AppApplication> buildWrapper(AppApplicationBo bo) {
        LambdaQueryWrapper<AppApplication> w = Wrappers.lambdaQuery();
        w.eq(bo.getCategoryId() != null, AppApplication::getCategoryId, bo.getCategoryId());
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
        AppApplication add = toEntity(bo);
        boolean inserted = baseMapper.insert(add) > 0;
        if (inserted && isOnlineOnCreate(add.getStatus())) {
            sendAppOnlineNotification(add);
        }
        return inserted;
    }

    @Override
    public Boolean updateByBo(AppApplicationBo bo) {
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
        entity.setAccessUrl(bo.getAccessUrl());
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

    private void sendAppOnlineNotification(AppApplication app) {
        if (app == null || StringUtils.isBlank(app.getAppName())) {
            return;
        }
        String version = StringUtils.isBlank(app.getVersion()) ? "" : " " + app.getVersion();
        notificationService.sendToAllUsers(
            "应用上架：" + app.getAppName(),
            "应用中心已上架“" + app.getAppName() + "”" + version + "，可前往工具即用中打开使用。",
            "app"
        );
    }
}
