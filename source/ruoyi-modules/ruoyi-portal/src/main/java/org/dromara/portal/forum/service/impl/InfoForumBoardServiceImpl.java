package org.dromara.portal.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.forum.domain.InfoForumBoard;
import org.dromara.portal.forum.domain.InfoForumTopic;
import org.dromara.portal.forum.domain.bo.InfoForumBoardBo;
import org.dromara.portal.forum.domain.vo.InfoForumBoardVo;
import org.dromara.portal.forum.mapper.InfoForumBoardMapper;
import org.dromara.portal.forum.mapper.InfoForumTopicMapper;
import org.dromara.portal.forum.service.IInfoForumBoardService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@RequiredArgsConstructor
@Service
public class InfoForumBoardServiceImpl implements IInfoForumBoardService {

    private final InfoForumBoardMapper baseMapper;
    private final InfoForumTopicMapper topicMapper;

    private LambdaQueryWrapper<InfoForumBoard> buildWrapper(InfoForumBoardBo bo) {
        LambdaQueryWrapper<InfoForumBoard> w = Wrappers.lambdaQuery();
        w.eq(StringUtils.isNotBlank(bo.getStatus()), InfoForumBoard::getStatus, bo.getStatus());
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            w.and(q -> q.like(InfoForumBoard::getBoardName, bo.getKeyword())
                .or().like(InfoForumBoard::getBoardCode, bo.getKeyword())
                .or().like(InfoForumBoard::getDescription, bo.getKeyword()));
        }
        w.orderByAsc(InfoForumBoard::getOrderNum).orderByDesc(InfoForumBoard::getCreateTime);
        return w;
    }

    private void fillTopicCount(List<InfoForumBoardVo> rows) {
        rows.forEach(row -> row.setTopicCount(topicMapper.selectCount(
            Wrappers.<InfoForumTopic>lambdaQuery()
                .eq(InfoForumTopic::getBoardId, row.getBoardId())
                .eq(InfoForumTopic::getStatus, "0"))));
    }

    @Override
    public TableDataInfo<InfoForumBoardVo> queryPageList(InfoForumBoardBo bo, PageQuery pageQuery) {
        Page<InfoForumBoardVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        fillTopicCount(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<InfoForumBoardVo> queryList(InfoForumBoardBo bo) {
        List<InfoForumBoardVo> rows = baseMapper.selectVoList(buildWrapper(bo));
        fillTopicCount(rows);
        return rows;
    }

    @Override
    public List<InfoForumBoardVo> portalBoards() {
        InfoForumBoardBo bo = new InfoForumBoardBo();
        bo.setStatus("0");
        return queryList(bo);
    }

    @Override
    public InfoForumBoardVo queryById(Long boardId) {
        return baseMapper.selectVoById(boardId);
    }

    @Override
    public Boolean insertByBo(InfoForumBoardBo bo) {
        InfoForumBoard add = MapstructUtils.convert(bo, InfoForumBoard.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(InfoForumBoardBo bo) {
        InfoForumBoard up = MapstructUtils.convert(bo, InfoForumBoard.class);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeStatus(Long boardId, String status) {
        InfoForumBoard up = new InfoForumBoard();
        up.setBoardId(boardId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }
}
