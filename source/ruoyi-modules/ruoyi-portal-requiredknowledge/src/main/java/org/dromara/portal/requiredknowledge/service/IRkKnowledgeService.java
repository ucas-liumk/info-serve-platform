package org.dromara.portal.requiredknowledge.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.requiredknowledge.domain.bo.RkKnowledgeBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkKnowledgeVo;

import java.util.Collection;
import java.util.List;

public interface IRkKnowledgeService {

    TableDataInfo<RkKnowledgeVo> queryPageList(RkKnowledgeBo bo, PageQuery pageQuery);

    List<RkKnowledgeVo> queryList(RkKnowledgeBo bo);

    List<RkKnowledgeVo> portalListBySubjectCode(String subjectCode);

    RkKnowledgeVo queryById(Long knowledgeId);

    Boolean insertByBo(RkKnowledgeBo bo);

    Boolean updateByBo(RkKnowledgeBo bo);

    Boolean changeStatus(Long knowledgeId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
