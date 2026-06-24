package org.dromara.appcenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.appcenter.domain.*;
import org.dromara.appcenter.domain.vo.*;
import org.dromara.appcenter.mapper.*;
import org.dromara.appcenter.service.IAppPortalService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class AppPortalServiceImpl implements IAppPortalService {

    private final AppApplicationMapper applicationMapper;
    private final AppCategoryMapper categoryMapper;
    private final AppFavoriteMapper favoriteMapper;
    private final AppRecommendMapper recommendMapper;
    private final AppMessageMapper messageMapper;

    @Override
    public List<AppCategoryVo> categories() {
        return categoryMapper.selectVoList(
            Wrappers.<AppCategory>lambdaQuery()
                .eq(AppCategory::getStatus, "0")
                .orderByAsc(AppCategory::getOrderNum));
    }

    @Override
    public TableDataInfo<PortalAppVo> apps(String categoryCode, String keyword, String sort, PageQuery pageQuery) {
        LambdaQueryWrapper<AppApplication> w = Wrappers.lambdaQuery();
        w.eq(AppApplication::getStatus, "0");
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

    List<PortalAppVo> toPortalVos(List<AppApplication> apps) {
        if (apps.isEmpty()) {
            return Collections.emptyList();
        }
        Long userId = LoginHelper.getUserId();
        List<Long> appIds = apps.stream().map(AppApplication::getAppId).collect(Collectors.toList());
        Set<Long> favs = favoriteMapper.selectList(Wrappers.<AppFavorite>lambdaQuery()
                .eq(AppFavorite::getUserId, userId).in(AppFavorite::getAppId, appIds))
            .stream().map(AppFavorite::getAppId).collect(Collectors.toSet());
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
            v.setIsSecurity(a.getIsSecurity());
            v.setUseCount(a.getUseCount());
            v.setRecommendCount(a.getRecommendCount());
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
        applicationMapper.update(null, Wrappers.<AppApplication>lambdaUpdate()
            .setSql("use_count = use_count + 1").eq(AppApplication::getAppId, appId));
        return app.getAccessUrl();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favorite(Long appId, boolean add) {
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
        Page<AppApplication> page = applicationMapper.selectPage(pageQuery.build(),
            Wrappers.<AppApplication>lambdaQuery()
                .in(AppApplication::getAppId, appIds)
                .eq(AppApplication::getStatus, "0"));
        info.setRows(toPortalVos(page.getRecords()));
        info.setTotal(page.getTotal());
        return info;
    }

    @Override
    public TableDataInfo<AppMessageVo> messages(String isRead, PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        Page<AppMessageVo> page = messageMapper.selectVoPage(pageQuery.build(),
            Wrappers.<AppMessage>lambdaQuery()
                .eq(AppMessage::getUserId, userId)
                .eq(StringUtils.isNotBlank(isRead), AppMessage::getIsRead, isRead)
                .orderByDesc(AppMessage::getCreateTime));
        return TableDataInfo.build(page);
    }

    @Override
    public long unreadCount() {
        Long userId = LoginHelper.getUserId();
        return messageMapper.selectCount(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getUserId, userId).eq(AppMessage::getIsRead, "0"));
    }

    @Override
    public void readMessage(Long messageId) {
        Long userId = LoginHelper.getUserId();
        AppMessage up = new AppMessage();
        up.setMessageId(messageId);
        up.setIsRead("1");
        messageMapper.update(up, Wrappers.<AppMessage>lambdaUpdate()
            .eq(AppMessage::getMessageId, messageId).eq(AppMessage::getUserId, userId));
    }
}
