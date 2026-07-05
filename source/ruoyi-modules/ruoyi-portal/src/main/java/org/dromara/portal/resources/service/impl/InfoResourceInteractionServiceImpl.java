package org.dromara.portal.resources.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.kernel.support.InfoUserDisplayNameResolver;
import org.dromara.portal.resources.domain.InfoResourceNote;
import org.dromara.portal.resources.domain.InfoResourceViewRecord;
import org.dromara.portal.resources.domain.bo.InfoResourceNoteBo;
import org.dromara.portal.resources.domain.vo.InfoResourceNoteVo;
import org.dromara.portal.resources.domain.vo.InfoResourceViewRecordVo;
import org.dromara.portal.resources.mapper.InfoResourceNoteMapper;
import org.dromara.portal.resources.mapper.InfoResourceViewRecordMapper;
import org.dromara.portal.resources.service.IInfoResourceInteractionService;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class InfoResourceInteractionServiceImpl implements IInfoResourceInteractionService {

    private static final String NOTE_PUBLIC = "public";
    private static final String NOTE_PRIVATE = "private";

    private final InfoResourceNoteMapper noteMapper;
    private final InfoResourceViewRecordMapper viewRecordMapper;
    private final IInfoResourceService resourceService;
    private final InfoUserDisplayNameResolver userDisplayNameResolver;

    @Override
    public TableDataInfo<InfoResourceNoteVo> myNotes(Long resourceId, PageQuery pageQuery) {
        Long userId = requireLogin();
        resourceService.queryPortalReadableDetail(resourceId);
        Page<InfoResourceNoteVo> page = noteMapper.selectVoPage(pageQuery.build(), Wrappers.<InfoResourceNote>lambdaQuery()
            .eq(InfoResourceNote::getResourceId, resourceId)
            .eq(InfoResourceNote::getUserId, userId)
            .orderByDesc(InfoResourceNote::getCreateTime));
        fillMine(page.getRecords(), userId);
        return TableDataInfo.build(page);
    }

    @Override
    public TableDataInfo<InfoResourceNoteVo> publicNotes(Long resourceId, PageQuery pageQuery) {
        Long userId = requireLogin();
        resourceService.queryPortalReadableDetail(resourceId);
        Page<InfoResourceNoteVo> page = noteMapper.selectVoPage(pageQuery.build(), Wrappers.<InfoResourceNote>lambdaQuery()
            .eq(InfoResourceNote::getResourceId, resourceId)
            .eq(InfoResourceNote::getVisibility, NOTE_PUBLIC)
            .orderByDesc(InfoResourceNote::getCreateTime));
        fillMine(page.getRecords(), userId);
        return TableDataInfo.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InfoResourceNoteVo createNote(Long resourceId, InfoResourceNoteBo bo) {
        Long userId = requireLogin();
        resourceService.queryPortalReadableDetail(resourceId);

        InfoResourceNote note = new InfoResourceNote();
        note.setResourceId(resourceId);
        note.setUserId(userId);
        note.setAuthorName(userDisplayNameResolver.currentUserDisplayName("我"));
        note.setContent(StringUtils.trim(bo.getContent()));
        note.setVisibility(normalizeVisibility(bo.getVisibility()));
        note.setTenantId(LoginHelper.getTenantId());
        noteMapper.insert(note);

        InfoResourceNoteVo vo = MapstructUtils.convert(note, InfoResourceNoteVo.class);
        vo.setMine(true);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InfoResourceNoteVo updateNote(Long resourceId, Long noteId, InfoResourceNoteBo bo) {
        Long userId = requireLogin();
        InfoResourceNote current = assertOwnNote(resourceId, noteId, userId);

        InfoResourceNote update = new InfoResourceNote();
        update.setNoteId(current.getNoteId());
        update.setContent(StringUtils.trim(bo.getContent()));
        update.setVisibility(normalizeVisibility(bo.getVisibility()));
        noteMapper.updateById(update);

        InfoResourceNoteVo vo = noteMapper.selectVoById(noteId);
        vo.setMine(true);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteNote(Long resourceId, Long noteId) {
        Long userId = requireLogin();
        assertOwnNote(resourceId, noteId, userId);
        return noteMapper.deleteById(noteId) > 0;
    }

    @Override
    public TableDataInfo<InfoResourceViewRecordVo> viewRecords(Long resourceId, PageQuery pageQuery) {
        resourceService.queryPortalReadableDetail(resourceId);
        Page<InfoResourceViewRecordVo> page = viewRecordMapper.selectVoPage(pageQuery.build(), Wrappers.<InfoResourceViewRecord>lambdaQuery()
            .eq(InfoResourceViewRecord::getResourceId, resourceId)
            .orderByDesc(InfoResourceViewRecord::getCreateTime));
        return TableDataInfo.build(page);
    }

    private InfoResourceNote assertOwnNote(Long resourceId, Long noteId, Long userId) {
        InfoResourceNote note = noteMapper.selectById(noteId);
        if (note == null || !resourceId.equals(note.getResourceId())) {
            throw new ServiceException("笔记不存在");
        }
        if (!userId.equals(note.getUserId())) {
            throw new ServiceException("只能维护自己的笔记");
        }
        return note;
    }

    private Long requireLogin() {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("请先登录");
        }
        return userId;
    }

    private String normalizeVisibility(String visibility) {
        return NOTE_PUBLIC.equals(visibility) ? NOTE_PUBLIC : NOTE_PRIVATE;
    }

    private void fillMine(List<InfoResourceNoteVo> rows, Long userId) {
        rows.forEach(row -> row.setMine(userId != null && userId.equals(row.getUserId())));
    }
}
