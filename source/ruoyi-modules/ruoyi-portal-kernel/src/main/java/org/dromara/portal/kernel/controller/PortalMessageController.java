package org.dromara.portal.kernel.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.kernel.domain.vo.AppMessageVo;
import org.dromara.portal.kernel.service.IPortalMessageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/messages")
public class PortalMessageController {

    private final IPortalMessageService messageService;

    @GetMapping
    public TableDataInfo<AppMessageVo> messages(@RequestParam(required = false) String isRead, PageQuery pageQuery) {
        return messageService.pageMyMessages(isRead, pageQuery);
    }

    @GetMapping("/unreadCount")
    public R<Long> unreadCount() {
        return R.ok(messageService.countMyUnread());
    }

    @PostMapping("/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        messageService.markRead(id);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteReadMessage(@PathVariable Long id) {
        messageService.deleteRead(id);
        return R.ok();
    }

    @DeleteMapping("/history/clear")
    public R<Void> clearReadMessages() {
        messageService.clearRead();
        return R.ok();
    }
}
