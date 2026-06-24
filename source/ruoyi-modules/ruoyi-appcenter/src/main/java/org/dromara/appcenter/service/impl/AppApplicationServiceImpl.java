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
import org.dromara.common.core.utils.MapstructUtils;
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
        Page<AppApplicationVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        return TableDataInfo.build(page);
    }

    @Override
    public List<AppApplicationVo> queryList(AppApplicationBo bo) {
        return baseMapper.selectVoList(buildWrapper(bo));
    }

    @Override
    public AppApplicationVo queryById(Long appId) {
        return baseMapper.selectVoById(appId);
    }

    @Override
    public Boolean insertByBo(AppApplicationBo bo) {
        AppApplication add = MapstructUtils.convert(bo, AppApplication.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(AppApplicationBo bo) {
        AppApplication up = MapstructUtils.convert(bo, AppApplication.class);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeStatus(Long appId, String status) {
        AppApplication up = new AppApplication();
        up.setAppId(appId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }
}
