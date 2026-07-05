package org.dromara.portal.resources.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.resources.domain.bo.InfoResourceNoteBo;
import org.dromara.portal.resources.domain.vo.InfoResourceNoteVo;
import org.dromara.portal.resources.domain.vo.InfoResourceViewRecordVo;

public interface IInfoResourceInteractionService {

    TableDataInfo<InfoResourceNoteVo> myNotes(Long resourceId, PageQuery pageQuery);

    TableDataInfo<InfoResourceNoteVo> publicNotes(Long resourceId, PageQuery pageQuery);

    InfoResourceNoteVo createNote(Long resourceId, InfoResourceNoteBo bo);

    InfoResourceNoteVo updateNote(Long resourceId, Long noteId, InfoResourceNoteBo bo);

    Boolean deleteNote(Long resourceId, Long noteId);

    TableDataInfo<InfoResourceViewRecordVo> viewRecords(Long resourceId, PageQuery pageQuery);
}
