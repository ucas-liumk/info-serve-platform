package org.dromara.portal.resources.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.resources.domain.bo.InfoResourceCategoryBo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryVo;

import java.util.Collection;
import java.util.List;

public interface IInfoResourceCategoryService {

    TableDataInfo<InfoResourceCategoryVo> queryPageList(InfoResourceCategoryBo bo, PageQuery pageQuery);

    List<InfoResourceCategoryVo> queryList(InfoResourceCategoryBo bo);

    List<InfoResourceCategoryVo> portalCategories();

    InfoResourceCategoryVo queryById(Long categoryId);

    Boolean insertByBo(InfoResourceCategoryBo bo);

    Boolean updateByBo(InfoResourceCategoryBo bo);

    Boolean changeStatus(Long categoryId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
