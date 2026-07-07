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
import org.dromara.portal.forum.domain.bo.InfoForumTopicBo;
import org.dromara.portal.forum.domain.vo.InfoForumTopicVo;
import org.dromara.portal.forum.service.IInfoForumService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/forum/topic")
public class InfoForumTopicController extends BaseController {

    private final IInfoForumService forumService;

    @SaCheckPermission("infoservice:forumTopic:list")
    @GetMapping("/list")
    public TableDataInfo<InfoForumTopicVo> list(InfoForumTopicBo bo, PageQuery pageQuery) {
        return forumService.queryTopicPage(bo, pageQuery);
    }

    @SaCheckPermission("infoservice:forumTopic:query")
    @GetMapping("/{topicId}")
    public R<InfoForumTopicVo> getInfo(@PathVariable Long topicId) {
        return R.ok(forumService.queryTopicById(topicId));
    }

    @SaCheckPermission("infoservice:forumTopic:add")
    @Log(title = "论坛帖子", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody InfoForumTopicBo bo) {
        return toAjax(forumService.insertTopicByBo(bo));
    }

    @SaCheckPermission("infoservice:forumTopic:edit")
    @Log(title = "论坛帖子", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody InfoForumTopicBo bo) {
        return toAjax(forumService.updateTopicByBo(bo));
    }

    @SaCheckPermission("infoservice:forumTopic:edit")
    @Log(title = "论坛帖子", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody InfoForumTopicBo bo) {
        return toAjax(forumService.changeTopicStatus(bo.getTopicId(), bo.getStatus()));
    }

    @SaCheckPermission("infoservice:forumTopic:remove")
    @Log(title = "论坛帖子", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(forumService.deleteTopicByIds(Arrays.asList(ids)));
    }
}
