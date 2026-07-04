package org.dromara.portal.appcenter.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.portal.appcenter.domain.AppCategory;
import org.dromara.portal.appcenter.domain.bo.AppCategoryBo;
import org.dromara.portal.appcenter.domain.vo.AppCategoryVo;
import org.dromara.portal.appcenter.mapper.AppCategoryMapper;
import org.dromara.portal.appcenter.service.IAppCategoryService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class AppCategoryServiceImpl implements IAppCategoryService {

    private final AppCategoryMapper baseMapper;

    @Override
    public List<AppCategoryVo> queryList() {
        return baseMapper.selectList(
            Wrappers.<AppCategory>lambdaQuery()
                .eq(AppCategory::getStatus, "0")
                .orderByAsc(AppCategory::getOrderNum))
            .stream().map(this::toVo).toList();
    }

    @Override
    public AppCategoryVo queryById(Long categoryId) {
        return toVo(baseMapper.selectById(categoryId));
    }

    @Override
    public Boolean insertByBo(AppCategoryBo bo) {
        AppCategory add = toEntity(bo);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(AppCategoryBo bo) {
        AppCategory up = toEntity(bo);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }

    private AppCategory toEntity(AppCategoryBo bo) {
        if (bo == null) {
            return null;
        }
        AppCategory entity = new AppCategory();
        entity.setCategoryId(bo.getCategoryId());
        entity.setCategoryName(bo.getCategoryName());
        entity.setCategoryCode(bo.getCategoryCode());
        entity.setIcon(bo.getIcon());
        entity.setOrderNum(bo.getOrderNum());
        entity.setStatus(bo.getStatus());
        return entity;
    }

    private AppCategoryVo toVo(AppCategory entity) {
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
        return vo;
    }
}
