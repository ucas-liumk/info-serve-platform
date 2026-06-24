package org.dromara.appcenter.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.appcenter.domain.AppCategory;
import org.dromara.appcenter.domain.bo.AppCategoryBo;
import org.dromara.appcenter.domain.vo.AppCategoryVo;
import org.dromara.appcenter.mapper.AppCategoryMapper;
import org.dromara.appcenter.service.IAppCategoryService;
import org.dromara.common.core.utils.MapstructUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AppCategoryServiceImpl implements IAppCategoryService {

    private final AppCategoryMapper baseMapper;

    @Override
    public List<AppCategoryVo> queryList() {
        return baseMapper.selectVoList(
            Wrappers.<AppCategory>lambdaQuery()
                .eq(AppCategory::getStatus, "0")
                .orderByAsc(AppCategory::getOrderNum));
    }

    @Override
    public AppCategoryVo queryById(Long categoryId) {
        return baseMapper.selectVoById(categoryId);
    }

    @Override
    public Boolean insertByBo(AppCategoryBo bo) {
        AppCategory add = MapstructUtils.convert(bo, AppCategory.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(AppCategoryBo bo) {
        AppCategory up = MapstructUtils.convert(bo, AppCategory.class);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }
}
