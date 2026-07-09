package org.dromara.portal.kernel.service;

import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.kernel.domain.vo.AppMessageVo;

/** 门户消息中心（当前登录用户视角） */
public interface IPortalMessageService {

    TableDataInfo<AppMessageVo> pageMyMessages(String isRead, PageQuery pageQuery);

    long countMyUnread();

    void markRead(Long messageId);

    void deleteRead(Long messageId);

    void clearRead();
}
