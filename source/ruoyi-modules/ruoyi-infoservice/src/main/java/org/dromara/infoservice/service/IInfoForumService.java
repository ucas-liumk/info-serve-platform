package org.dromara.infoservice.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.infoservice.domain.bo.InfoForumReplyBo;
import org.dromara.infoservice.domain.bo.InfoForumTopicBo;
import org.dromara.infoservice.domain.vo.ForumTopicDetailVo;
import org.dromara.infoservice.domain.vo.InfoForumReplyVo;
import org.dromara.infoservice.domain.vo.InfoForumTopicVo;

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
}
