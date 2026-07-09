package org.dromara.portal.requiredknowledge.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.requiredknowledge.domain.bo.RkSubjectGroupBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectGroupVo;

import java.util.Collection;
import java.util.List;

public interface IRkSubjectGroupService {

    TableDataInfo<RkSubjectGroupVo> queryPageList(RkSubjectGroupBo bo, PageQuery pageQuery);

    List<RkSubjectGroupVo> queryList(RkSubjectGroupBo bo);

    List<RkSubjectGroupVo> portalCatalog();

    RkSubjectGroupVo queryById(Long groupId);

    Boolean insertByBo(RkSubjectGroupBo bo);

    Boolean updateByBo(RkSubjectGroupBo bo);

    Boolean changeStatus(Long groupId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
