package org.dromara.portal.forum.controller.portal;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.core.utils.ValidatorUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.forum.domain.bo.InfoForumReplyBo;
import org.dromara.portal.forum.domain.bo.InfoForumTopicBo;
import org.dromara.portal.forum.domain.vo.ForumTopicDetailVo;
import org.dromara.portal.forum.domain.vo.InfoForumBoardVo;
import org.dromara.portal.forum.domain.vo.InfoForumReplyVo;
import org.dromara.portal.forum.domain.vo.InfoForumTopicVo;
import org.dromara.portal.forum.service.IInfoForumBoardService;
import org.dromara.portal.forum.service.IInfoForumService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/forum")
public class InfoForumPortalController {

    private final IInfoForumBoardService boardService;
    private final IInfoForumService forumService;

    @GetMapping("/boards")
    public R<List<InfoForumBoardVo>> boards() {
        return R.ok(boardService.portalBoards());
    }

    @GetMapping("/topics")
    public TableDataInfo<InfoForumTopicVo> topics(InfoForumTopicBo bo, PageQuery pageQuery) {
        bo.setStatus("0");
        return forumService.queryTopicPage(bo, pageQuery);
    }

    @GetMapping("/topics/{topicId}")
    public R<ForumTopicDetailVo> detail(@PathVariable Long topicId) {
        return R.ok(forumService.queryPortalTopicDetail(topicId));
    }

    @PostMapping("/topics")
    public R<InfoForumTopicVo> createTopic(@Validated @RequestBody InfoForumTopicBo bo) {
        return R.ok(forumService.createTopic(bo));
    }

    @PostMapping("/topics/{topicId}/replies")
    public R<InfoForumReplyVo> reply(@PathVariable Long topicId, @RequestBody InfoForumReplyBo bo) {
        bo.setTopicId(topicId);
        ValidatorUtils.validate(bo);
        return R.ok(forumService.reply(bo));
    }

    @PostMapping("/topics/{topicId}/like")
    public R<Void> like(@PathVariable Long topicId) {
        forumService.likeTopic(topicId, true);
        return R.ok();
    }

    @DeleteMapping("/topics/{topicId}/like")
    public R<Void> unlike(@PathVariable Long topicId) {
        forumService.likeTopic(topicId, false);
        return R.ok();
    }
}
