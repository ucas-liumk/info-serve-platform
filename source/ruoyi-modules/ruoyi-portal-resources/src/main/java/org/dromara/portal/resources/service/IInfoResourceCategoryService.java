package org.dromara.portal.resources.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.bo.InfoResourceCategoryBo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryTreeVo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryVo;

import java.util.Collection;
import java.util.List;

public interface IInfoResourceCategoryService {

    TableDataInfo<InfoResourceCategoryVo> queryPageList(InfoResourceCategoryBo bo, PageQuery pageQuery);

    /** 平铺分类列表：只返回二级分类（C3），结构与历史版本零变化 */
    List<InfoResourceCategoryVo> queryList(InfoResourceCategoryBo bo);

    List<InfoResourceCategoryVo> portalCategories();

    /** 门户栏目/分类两级树（C1）：分类节点带分面计数，入参为关键词+工具条筛选 */
    List<InfoResourceCategoryTreeVo> portalCategoryTree(InfoResourceBo bo);

    /** 管理端树表全量平铺（C4）：含停用行、不分页，供前端 handleTree 组树 */
    List<InfoResourceCategoryVo> queryTreeList();

    InfoResourceCategoryVo queryById(Long categoryId);

    Boolean insertByBo(InfoResourceCategoryBo bo);

    Boolean updateByBo(InfoResourceCategoryBo bo);

    Boolean changeStatus(Long categoryId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
