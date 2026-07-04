package org.dromara.portal.resources.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.InfoResourceCategory;
import org.dromara.portal.resources.domain.bo.InfoResourceCategoryBo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryVo;
import org.dromara.portal.resources.mapper.InfoResourceCategoryMapper;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.portal.resources.service.IInfoResourceCategoryService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class InfoResourceCategoryServiceImpl implements IInfoResourceCategoryService {

    private final InfoResourceCategoryMapper baseMapper;
    private final InfoResourceMapper resourceMapper;

    private LambdaQueryWrapper<InfoResourceCategory> buildWrapper(InfoResourceCategoryBo bo) {
        LambdaQueryWrapper<InfoResourceCategory> w = Wrappers.lambdaQuery();
        w.eq(StringUtils.isNotBlank(bo.getStatus()), InfoResourceCategory::getStatus, bo.getStatus());
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            w.and(q -> q.like(InfoResourceCategory::getCategoryName, bo.getKeyword())
                .or().like(InfoResourceCategory::getCategoryCode, bo.getKeyword())
                .or().like(InfoResourceCategory::getDescription, bo.getKeyword()));
        }
        w.orderByAsc(InfoResourceCategory::getOrderNum).orderByDesc(InfoResourceCategory::getCreateTime);
        return w;
    }

    private void fillResourceCount(List<InfoResourceCategoryVo> rows) {
        rows.forEach(row -> row.setResourceCount(resourceMapper.selectCount(
            Wrappers.<InfoResource>lambdaQuery()
                .eq(InfoResource::getCategoryId, row.getCategoryId())
                .eq(InfoResource::getStatus, "0"))));
    }

    @Override
    public TableDataInfo<InfoResourceCategoryVo> queryPageList(InfoResourceCategoryBo bo, PageQuery pageQuery) {
        Page<InfoResourceCategoryVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        fillResourceCount(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<InfoResourceCategoryVo> queryList(InfoResourceCategoryBo bo) {
        List<InfoResourceCategoryVo> rows = baseMapper.selectVoList(buildWrapper(bo));
        fillResourceCount(rows);
        return rows;
    }

    @Override
    public List<InfoResourceCategoryVo> portalCategories() {
        InfoResourceCategoryBo bo = new InfoResourceCategoryBo();
        bo.setStatus("0");
        return queryList(bo);
    }

    @Override
    public InfoResourceCategoryVo queryById(Long categoryId) {
        return baseMapper.selectVoById(categoryId);
    }

    @Override
    public Boolean insertByBo(InfoResourceCategoryBo bo) {
        InfoResourceCategory add = MapstructUtils.convert(bo, InfoResourceCategory.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(InfoResourceCategoryBo bo) {
        InfoResourceCategory up = MapstructUtils.convert(bo, InfoResourceCategory.class);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeStatus(Long categoryId, String status) {
        InfoResourceCategory up = new InfoResourceCategory();
        up.setCategoryId(categoryId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }
}
