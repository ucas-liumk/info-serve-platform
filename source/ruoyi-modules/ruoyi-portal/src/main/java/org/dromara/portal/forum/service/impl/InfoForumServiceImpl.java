package org.dromara.portal.forum.service.impl;

import cn.hutool.core.lang.Dict;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.forum.domain.InfoForumBoard;
import org.dromara.portal.forum.domain.InfoForumLike;
import org.dromara.portal.forum.domain.InfoForumReply;
import org.dromara.portal.forum.domain.InfoForumTopic;
import org.dromara.portal.forum.domain.bo.InfoForumReplyBo;
import org.dromara.portal.forum.domain.bo.InfoForumTopicBo;
import org.dromara.portal.forum.domain.vo.ForumTopicDetailVo;
import org.dromara.portal.forum.domain.vo.InfoForumReplyVo;
import org.dromara.portal.forum.domain.vo.InfoForumTopicVo;
import org.dromara.portal.forum.mapper.InfoForumBoardMapper;
import org.dromara.portal.forum.mapper.InfoForumLikeMapper;
import org.dromara.portal.forum.mapper.InfoForumReplyMapper;
import org.dromara.portal.forum.mapper.InfoForumTopicMapper;
import org.dromara.portal.kernel.service.IPortalNotificationService;
import org.dromara.portal.forum.service.IInfoForumService;
import org.dromara.portal.kernel.support.InfoUserDisplayNameResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InfoForumServiceImpl implements IInfoForumService {

    private static final String TARGET_TOPIC = "topic";

    private final InfoForumTopicMapper topicMapper;
    private final InfoForumReplyMapper replyMapper;
    private final InfoForumBoardMapper boardMapper;
    private final InfoForumLikeMapper likeMapper;
    private final IPortalNotificationService notificationService;
    private final InfoUserDisplayNameResolver userDisplayNameResolver;

    private LambdaQueryWrapper<InfoForumTopic> buildTopicWrapper(InfoForumTopicBo bo, boolean portal) {
        LambdaQueryWrapper<InfoForumTopic> w = Wrappers.lambdaQuery();
        w.eq(bo.getBoardId() != null, InfoForumTopic::getBoardId, bo.getBoardId());
        w.eq(StringUtils.isNotBlank(bo.getStatus()), InfoForumTopic::getStatus, bo.getStatus());
        if (portal) {
            w.eq(InfoForumTopic::getStatus, "0");
        }
        if (StringUtils.isNotBlank(bo.getBoardCode()) && !"all".equals(bo.getBoardCode())) {
            InfoForumBoard board = boardMapper.selectOne(Wrappers.<InfoForumBoard>lambdaQuery()
                .eq(InfoForumBoard::getBoardCode, bo.getBoardCode())
                .eq(InfoForumBoard::getStatus, "0"));
            w.eq(InfoForumTopic::getBoardId, board == null ? -1L : board.getBoardId());
        }
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            w.and(q -> q.like(InfoForumTopic::getTitle, bo.getKeyword())
                .or().like(InfoForumTopic::getContent, bo.getKeyword())
                .or().like(InfoForumTopic::getAuthorName, bo.getKeyword()));
        }
        w.orderByDesc(InfoForumTopic::getIsTop).orderByDesc(InfoForumTopic::getCreateTime);
        return w;
    }

    private void fillTopicExt(List<InfoForumTopicVo> rows) {
        if (rows.isEmpty()) {
            return;
        }
        List<Long> boardIds = rows.stream().map(InfoForumTopicVo::getBoardId).distinct().toList();
        Map<Long, String> boardNames = boardMapper.selectList(Wrappers.<InfoForumBoard>lambdaQuery().in(InfoForumBoard::getBoardId, boardIds))
            .stream().collect(Collectors.toMap(InfoForumBoard::getBoardId, InfoForumBoard::getBoardName, (a, b) -> a));
        Map<Long, String> authorNames = userDisplayNameResolver.resolveDisplayNames(rows.stream().map(InfoForumTopicVo::getAuthorId).toList());
        Long userId = LoginHelper.getUserId();
        Set<Long> likedTopicIds = Collections.emptySet();
        if (userId != null) {
            List<Long> topicIds = rows.stream().map(InfoForumTopicVo::getTopicId).toList();
            likedTopicIds = likeMapper.selectList(Wrappers.<InfoForumLike>lambdaQuery()
                    .eq(InfoForumLike::getUserId, userId)
                    .eq(InfoForumLike::getTargetType, TARGET_TOPIC)
                    .in(InfoForumLike::getTargetId, topicIds))
                .stream().map(InfoForumLike::getTargetId).collect(Collectors.toSet());
        }
        Set<Long> finalLikedTopicIds = likedTopicIds;
        rows.forEach(row -> {
            row.setBoardName(boardNames.get(row.getBoardId()));
            row.setAuthorName(StringUtils.defaultIfBlank(authorNames.get(row.getAuthorId()), row.getAuthorName()));
            row.setLiked(finalLikedTopicIds.contains(row.getTopicId()));
        });
    }

    private void fillReplyExt(List<InfoForumReplyVo> rows) {
        if (rows.isEmpty()) {
            return;
        }
        Map<Long, String> authorNames = userDisplayNameResolver.resolveDisplayNames(rows.stream().map(InfoForumReplyVo::getAuthorId).toList());
        rows.forEach(row -> row.setAuthorName(StringUtils.defaultIfBlank(authorNames.get(row.getAuthorId()), row.getAuthorName())));
    }

    @Override
    public TableDataInfo<InfoForumTopicVo> queryTopicPage(InfoForumTopicBo bo, PageQuery pageQuery) {
        Page<InfoForumTopicVo> page = topicMapper.selectVoPage(pageQuery.build(), buildTopicWrapper(bo, false));
        fillTopicExt(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public InfoForumTopicVo queryTopicById(Long topicId) {
        InfoForumTopicVo vo = topicMapper.selectVoById(topicId);
        fillTopicExt(vo == null ? Collections.emptyList() : List.of(vo));
        return vo;
    }

    @Override
    public ForumTopicDetailVo queryPortalTopicDetail(Long topicId) {
        InfoForumTopic topic = getUsableTopic(topicId);
        topicMapper.update(null, Wrappers.<InfoForumTopic>lambdaUpdate()
            .setSql("view_count = view_count + 1")
            .eq(InfoForumTopic::getTopicId, topicId));
        InfoForumTopicVo topicVo = MapstructUtils.convert(topic, InfoForumTopicVo.class);
        fillTopicExt(List.of(topicVo));
        List<InfoForumReplyVo> replies = replyMapper.selectVoList(Wrappers.<InfoForumReply>lambdaQuery()
            .eq(InfoForumReply::getTopicId, topicId)
            .eq(InfoForumReply::getStatus, "0")
            .orderByAsc(InfoForumReply::getCreateTime));
        fillReplyExt(replies);
        ForumTopicDetailVo detail = new ForumTopicDetailVo();
        detail.setTopic(topicVo);
        detail.setReplies(replies);
        return detail;
    }

    @Override
    public Boolean insertTopicByBo(InfoForumTopicBo bo) {
        InfoForumTopic add = MapstructUtils.convert(bo, InfoForumTopic.class);
        if (StringUtils.isBlank(add.getStatus())) {
            add.setStatus("0");
        }
        fillTopicDefaults(add);
        return topicMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateTopicByBo(InfoForumTopicBo bo) {
        InfoForumTopic up = MapstructUtils.convert(bo, InfoForumTopic.class);
        return topicMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeTopicStatus(Long topicId, String status) {
        InfoForumTopic up = new InfoForumTopic();
        up.setTopicId(topicId);
        up.setStatus(status);
        return topicMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteTopicByIds(Collection<Long> ids) {
        return topicMapper.deleteByIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InfoForumTopicVo createTopic(InfoForumTopicBo bo) {
        assertBoardUsable(bo.getBoardId());
        InfoForumTopic topic = MapstructUtils.convert(bo, InfoForumTopic.class);
        fillTopicDefaults(topic);
        topic.setStatus("0");
        topic.setAuthorId(LoginHelper.getUserId());
        topic.setAuthorName(userDisplayNameResolver.currentUserDisplayName("用户"));
        topicMapper.insert(topic);
        return queryTopicById(topic.getTopicId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InfoForumReplyVo reply(InfoForumReplyBo bo) {
        InfoForumTopic topic = getUsableTopic(bo.getTopicId());
        if ("1".equals(topic.getIsClosed())) {
            throw new ServiceException("帖子已关闭，不能回复");
        }
        InfoForumReply reply = MapstructUtils.convert(bo, InfoForumReply.class);
        reply.setAuthorId(LoginHelper.getUserId());
        reply.setAuthorName(userDisplayNameResolver.currentUserDisplayName("用户"));
        reply.setStatus("0");
        replyMapper.insert(reply);
        topicMapper.update(null, Wrappers.<InfoForumTopic>lambdaUpdate()
            .setSql("reply_count = reply_count + 1")
            .eq(InfoForumTopic::getTopicId, bo.getTopicId()));
        sendForumReplyNotification(topic, reply, bo);
        InfoForumReplyVo replyVo = replyMapper.selectVoById(reply.getReplyId());
        fillReplyExt(replyVo == null ? Collections.emptyList() : List.of(replyVo));
        return replyVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean likeTopic(Long topicId, boolean add) {
        getUsableTopic(topicId);
        Long userId = LoginHelper.getUserId();
        Long exist = likeMapper.selectCount(Wrappers.<InfoForumLike>lambdaQuery()
            .eq(InfoForumLike::getUserId, userId)
            .eq(InfoForumLike::getTargetType, TARGET_TOPIC)
            .eq(InfoForumLike::getTargetId, topicId));
        if (add && exist == 0) {
            InfoForumLike like = new InfoForumLike();
            like.setTargetType(TARGET_TOPIC);
            like.setTargetId(topicId);
            like.setUserId(userId);
            like.setCreateTime(new Date());
            likeMapper.insert(like);
            topicMapper.update(null, Wrappers.<InfoForumTopic>lambdaUpdate()
                .setSql("like_count = like_count + 1")
                .eq(InfoForumTopic::getTopicId, topicId));
        } else if (!add && exist > 0) {
            likeMapper.delete(Wrappers.<InfoForumLike>lambdaQuery()
                .eq(InfoForumLike::getUserId, userId)
                .eq(InfoForumLike::getTargetType, TARGET_TOPIC)
                .eq(InfoForumLike::getTargetId, topicId));
            topicMapper.update(null, Wrappers.<InfoForumTopic>lambdaUpdate()
                .setSql("like_count = GREATEST(like_count - 1, 0)")
                .eq(InfoForumTopic::getTopicId, topicId));
        }
        return true;
    }

    private void fillTopicDefaults(InfoForumTopic topic) {
        topic.setViewCount(0L);
        topic.setReplyCount(0L);
        topic.setLikeCount(0L);
        if (StringUtils.isBlank(topic.getIsTop())) {
            topic.setIsTop("0");
        }
        if (StringUtils.isBlank(topic.getIsClosed())) {
            topic.setIsClosed("0");
        }
    }

    private InfoForumTopic getUsableTopic(Long topicId) {
        InfoForumTopic topic = topicMapper.selectById(topicId);
        if (topic == null || !"0".equals(topic.getStatus())) {
            throw new ServiceException("帖子不存在或已下架");
        }
        return topic;
    }

    private void assertBoardUsable(Long boardId) {
        InfoForumBoard board = boardMapper.selectById(boardId);
        if (board == null || !"0".equals(board.getStatus())) {
            throw new ServiceException("论坛版块不存在或已停用");
        }
    }

    private void sendForumReplyNotification(InfoForumTopic topic, InfoForumReply reply, InfoForumReplyBo bo) {
        Long currentUserId = reply.getAuthorId();
        Set<Long> recipients = new LinkedHashSet<>();
        addRecipient(recipients, topic.getAuthorId(), currentUserId);
        addRecipient(recipients, resolveReplyToUserId(bo.getRemark()), currentUserId);
        if (recipients.isEmpty()) {
            return;
        }
        String author = StringUtils.defaultIfBlank(reply.getAuthorName(), "用户");
        String excerpt = StringUtils.substring(StringUtils.trimToEmpty(reply.getContent()), 0, 80);
        notificationService.sendToUsers(
            recipients,
            "论坛有新回复：" + topic.getTitle(),
            author + " 回复了话题“" + topic.getTitle() + "”：" + excerpt,
            "forum"
        );
    }

    private void addRecipient(Set<Long> recipients, Long userId, Long currentUserId) {
        if (userId != null && !userId.equals(currentUserId)) {
            recipients.add(userId);
        }
    }

    private Long resolveReplyToUserId(String remark) {
        try {
            Dict meta = JsonUtils.parseMap(remark);
            Object value = meta == null ? null : meta.get("replyToUserId");
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value instanceof String text && StringUtils.isNotBlank(text)) {
                return Long.valueOf(text);
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }
}
