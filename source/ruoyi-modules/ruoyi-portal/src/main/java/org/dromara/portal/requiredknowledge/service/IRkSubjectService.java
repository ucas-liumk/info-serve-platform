package org.dromara.portal.requiredknowledge.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.requiredknowledge.domain.bo.RkSubjectBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectVo;

import java.util.Collection;
import java.util.List;

public interface IRkSubjectService {

    TableDataInfo<RkSubjectVo> queryPageList(RkSubjectBo bo, PageQuery pageQuery);

    List<RkSubjectVo> queryList(RkSubjectBo bo);

    RkSubjectVo queryById(Long subjectId);

    Boolean insertByBo(RkSubjectBo bo);

    Boolean updateByBo(RkSubjectBo bo);

    Boolean changeStatus(Long subjectId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
