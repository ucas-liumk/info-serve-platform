package org.dromara.portal.forum.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.forum.domain.bo.InfoForumBoardBo;
import org.dromara.portal.forum.domain.vo.InfoForumBoardVo;

import java.util.Collection;
import java.util.List;

public interface IInfoForumBoardService {

    TableDataInfo<InfoForumBoardVo> queryPageList(InfoForumBoardBo bo, PageQuery pageQuery);

    List<InfoForumBoardVo> queryList(InfoForumBoardBo bo);

    List<InfoForumBoardVo> portalBoards();

    InfoForumBoardVo queryById(Long boardId);

    Boolean insertByBo(InfoForumBoardBo bo);

    Boolean updateByBo(InfoForumBoardBo bo);

    Boolean changeStatus(Long boardId, String status);

    Boolean deleteWithValidByIds(Collection<Long> ids);
}
