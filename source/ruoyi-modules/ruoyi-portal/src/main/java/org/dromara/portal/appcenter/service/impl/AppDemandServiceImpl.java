package org.dromara.portal.appcenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.portal.appcenter.domain.AppApplication;
import org.dromara.portal.appcenter.domain.AppDemand;
import org.dromara.portal.appcenter.domain.bo.AppDemandBo;
import org.dromara.portal.appcenter.domain.bo.AppDemandSubmitBo;
import org.dromara.portal.appcenter.domain.vo.AppDemandVo;
import org.dromara.portal.appcenter.mapper.AppApplicationMapper;
import org.dromara.portal.appcenter.mapper.AppDemandMapper;
import org.dromara.portal.appcenter.service.IAppDemandService;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AppDemandServiceImpl implements IAppDemandService {

    private final AppDemandMapper baseMapper;
    private final AppApplicationMapper applicationMapper;
    private final IPortalNotificationService notificationService;

    private LambdaQueryWrapper<AppDemand> buildWrapper(AppDemandBo bo) {
        LambdaQueryWrapper<AppDemand> w = Wrappers.lambdaQuery();
        w.eq(StringUtils.isNotBlank(bo.getDemandType()), AppDemand::getDemandType, bo.getDemandType());
        w.eq(StringUtils.isNotBlank(bo.getStatus()), AppDemand::getStatus, bo.getStatus());
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            w.and(q -> q.like(AppDemand::getAppName, bo.getKeyword())
                .or().like(AppDemand::getContent, bo.getKeyword())
                .or().like(AppDemand::getRequesterName, bo.getKeyword()));
        }
        w.orderByAsc(AppDemand::getStatus).orderByDesc(AppDemand::getCreateTime);
        return w;
    }

    @Override
    public TableDataInfo<AppDemandVo> queryPageList(AppDemandBo bo, PageQuery pageQuery) {
        Page<AppDemand> page = baseMapper.selectPage(pageQuery.build(), buildWrapper(bo));
        List<AppDemandVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public AppDemandVo queryById(Long demandId) {
        return toVo(baseMapper.selectById(demandId));
    }

    @Override
    public TableDataInfo<AppDemandVo> queryMyPageList(PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        Page<AppDemand> page = baseMapper.selectPage(pageQuery.build(),
            Wrappers.<AppDemand>lambdaQuery()
                .eq(AppDemand::getRequesterId, userId)
                .orderByDesc(AppDemand::getCreateTime));
        List<AppDemandVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public Boolean submitFromPortal(AppDemandSubmitBo bo) {
        String demandType = StringUtils.trimToEmpty(bo.getDemandType());
        String appName = StringUtils.trimToEmpty(bo.getAppName());
        Long appId = bo.getAppId();
        if (!"new_app".equals(demandType) && !"suggestion".equals(demandType)) {
            throw new ServiceException("需求类型不正确");
        }
        if ("suggestion".equals(demandType)) {
            if (appId == null) {
                throw new ServiceException("请选择已上线应用");
            }
            AppApplication app = applicationMapper.selectOne(Wrappers.<AppApplication>lambdaQuery()
                .select(AppApplication::getAppId, AppApplication::getAppName)
                .eq(AppApplication::getAppId, appId)
                .eq(AppApplication::getStatus, "0"));
            if (app == null) {
                throw new ServiceException("应用不存在或已下架");
            }
            appName = app.getAppName();
        } else {
            appId = null;
            if (StringUtils.isBlank(appName)) {
                throw new ServiceException("应用名称不能为空");
            }
        }

        AppDemand add = new AppDemand();
        add.setDemandType(demandType);
        add.setAppId(appId);
        add.setAppName(appName);
        add.setReferenceUrl(bo.getReferenceUrl());
        add.setContent(bo.getContent());
        add.setContact(bo.getContact());
        add.setRequesterId(LoginHelper.getUserId());
        add.setRequesterName(LoginHelper.getUsername());
        add.setStatus("0");
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean deleteOwnById(Long demandId) {
        if (demandId == null) {
            throw new ServiceException("需求ID不能为空");
        }
        Long userId = LoginHelper.getUserId();
        return baseMapper.delete(Wrappers.<AppDemand>lambdaQuery()
            .eq(AppDemand::getDemandId, demandId)
            .eq(AppDemand::getRequesterId, userId)) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateHandle(AppDemandBo bo) {
        if (bo.getDemandId() == null) {
            throw new ServiceException("需求ID不能为空");
        }
        AppDemand demand = baseMapper.selectById(bo.getDemandId());
        if (demand == null) {
            throw new ServiceException("需求反馈不存在");
        }
        String oldRemark = StringUtils.trimToEmpty(demand.getHandleRemark());
        String newRemark = StringUtils.trimToEmpty(bo.getHandleRemark());

        AppDemand up = new AppDemand();
        up.setDemandId(bo.getDemandId());
        up.setStatus(StringUtils.blankToDefault(bo.getStatus(), "2"));
        up.setHandleRemark(newRemark);
        up.setHandledBy(LoginHelper.getUserId());
        up.setHandledTime(new Date());
        boolean updated = baseMapper.updateById(up) > 0;
        if (updated
            && demand.getRequesterId() != null
            && StringUtils.isNotBlank(newRemark)
            && !StringUtils.equals(oldRemark, newRemark)) {
            sendHandleMessage(demand, newRemark);
        }
        return updated;
    }

    private void sendHandleMessage(AppDemand demand, String handleRemark) {
        notificationService.sendToUser(
            demand.getRequesterId(),
            "需求反馈已回复",
            "你提交的“" + StringUtils.blankToDefault(demand.getAppName(), "需求反馈") + "”已有管理员回复：" + handleRemark,
            "demand"
        );
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    private AppDemandVo toVo(AppDemand entity) {
        if (entity == null) {
            return null;
        }
        AppDemandVo vo = new AppDemandVo();
        vo.setDemandId(entity.getDemandId());
        vo.setDemandType(entity.getDemandType());
        vo.setAppId(entity.getAppId());
        vo.setAppName(entity.getAppName());
        vo.setReferenceUrl(entity.getReferenceUrl());
        vo.setContent(entity.getContent());
        vo.setContact(entity.getContact());
        vo.setRequesterId(entity.getRequesterId());
        vo.setRequesterName(entity.getRequesterName());
        vo.setStatus(entity.getStatus());
        vo.setHandleRemark(entity.getHandleRemark());
        vo.setHandledBy(entity.getHandledBy());
        vo.setHandledTime(entity.getHandledTime());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }
}
