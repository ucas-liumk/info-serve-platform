package org.dromara.portal.forum.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.forum.domain.bo.InfoForumReplyBo;
import org.dromara.portal.forum.domain.bo.InfoForumTopicBo;
import org.dromara.portal.forum.domain.vo.ForumTopicDetailVo;
import org.dromara.portal.forum.domain.vo.InfoForumReplyVo;
import org.dromara.portal.forum.domain.vo.InfoForumTopicVo;

import java.util.Collection;

public interface IInfoForumService {

    TableDataInfo<InfoForumTopicVo> queryTopicPage(InfoForumTopicBo bo, PageQuery pageQuery);

    InfoForumTopicVo queryTopicById(Long topicId);

    ForumTopicDetailVo queryPortalTopicDetail(Long topicId);

    Boolean insertTopicByBo(InfoForumTopicBo bo);

    Boolean updateTopicByBo(InfoForumTopicBo bo);

    Boolean changeTopicStatus(Long topicId, String status);

    Boolean deleteTopicByIds(Collection<Long> ids);

    InfoForumTopicVo createTopic(InfoForumTopicBo bo);

    InfoForumReplyVo reply(InfoForumReplyBo bo);

    Boolean likeTopic(Long topicId, boolean add);

    /** 我的话题（my-* 契约，供门户与个人工作台聚合） */
    TableDataInfo<InfoForumTopicVo> queryMyTopics(PageQuery pageQuery);

    /** 门户可见话题数（内核统计聚合用） */
    Long countPortalVisibleTopics();

    /** 论坛活跃用户数（发帖或回帖去重，内核统计聚合用） */
    Long countActiveAuthors();
}
