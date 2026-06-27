package org.dromara.infoservice.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.infoservice.domain.bo.InfoForumBoardBo;
import org.dromara.infoservice.domain.vo.InfoForumBoardVo;

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
