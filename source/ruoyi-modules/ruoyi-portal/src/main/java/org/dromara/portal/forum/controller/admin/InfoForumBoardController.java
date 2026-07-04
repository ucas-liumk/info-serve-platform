package org.dromara.portal.forum.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.portal.forum.domain.bo.InfoForumBoardBo;
import org.dromara.portal.forum.domain.vo.InfoForumBoardVo;
import org.dromara.portal.forum.service.IInfoForumBoardService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/forum/board")
public class InfoForumBoardController extends BaseController {

    private final IInfoForumBoardService boardService;

    @SaCheckPermission("infoservice:forumBoard:list")
    @GetMapping("/list")
    public TableDataInfo<InfoForumBoardVo> list(InfoForumBoardBo bo, PageQuery pageQuery) {
        return boardService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("infoservice:forumBoard:list")
    @GetMapping("/options")
    public R<List<InfoForumBoardVo>> options(InfoForumBoardBo bo) {
        return R.ok(boardService.queryList(bo));
    }

    @SaCheckPermission("infoservice:forumBoard:query")
    @GetMapping("/{boardId}")
    public R<InfoForumBoardVo> getInfo(@PathVariable Long boardId) {
        return R.ok(boardService.queryById(boardId));
    }

    @SaCheckPermission("infoservice:forumBoard:add")
    @Log(title = "论坛版块", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody InfoForumBoardBo bo) {
        return toAjax(boardService.insertByBo(bo));
    }

    @SaCheckPermission("infoservice:forumBoard:edit")
    @Log(title = "论坛版块", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody InfoForumBoardBo bo) {
        return toAjax(boardService.updateByBo(bo));
    }

    @SaCheckPermission("infoservice:forumBoard:edit")
    @Log(title = "论坛版块", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody InfoForumBoardBo bo) {
        return toAjax(boardService.changeStatus(bo.getBoardId(), bo.getStatus()));
    }

    @SaCheckPermission("infoservice:forumBoard:remove")
    @Log(title = "论坛版块", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(boardService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
