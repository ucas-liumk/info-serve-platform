# Task 3: 单体内解耦① —— 消息中心归位 kernel + 昵称解析器下放

背景：`app_message` 表与写服务在 kernel，但读端点（消息中心 REST）在 appcenter 且**直连 kernel 的 mapper/domain**（最深耦合）。本任务在单体内把消息中心整体归位 kernel，并把 `kernel.support.InfoUserDisplayNameResolver` 下放为 forum/resources 各自的内部组件（它只是 `RemoteUserService` 的薄封装，两份 ~70 行副本换取 BC 对 kernel 的零依赖，决策见 spec §3 红线 2）。

**Files:**
- Test: `source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/kernel/service/PortalMessageServiceTest.java`（新建）
- Create: `.../ruoyi-portal/src/main/java/org/dromara/portal/kernel/service/IPortalMessageService.java`
- Create: `.../kernel/service/impl/PortalMessageServiceImpl.java`
- Create: `.../kernel/controller/PortalMessageController.java`
- Create: `.../forum/support/ForumUserDisplayNameResolver.java`
- Create: `.../resources/support/ResourceUserDisplayNameResolver.java`
- Delete: `.../kernel/support/InfoUserDisplayNameResolver.java`（连同空的 support 包）
- Modify: `.../appcenter/controller/portal/AppPortalController.java`（删 5 个消息端点）
- Modify: `.../appcenter/service/IAppPortalService.java`（删 5 个消息方法）
- Modify: `.../appcenter/service/impl/AppPortalServiceImpl.java`（删消息实现与 kernel imports）
- Modify: `.../forum/service/impl/InfoForumServiceImpl.java`、`.../resources/service/impl/InfoResourceServiceImpl.java`、`.../resources/service/impl/InfoResourceInteractionServiceImpl.java`（换用本地 resolver）
- Modify: `source/ruoyi-modules/ruoyi-portal/src/test/java/org/dromara/portal/arch/BcBoundaryTest.java`（收紧规则 6）

**Interfaces:**
- Consumes: 无。
- Produces: `IPortalMessageService{ pageMyMessages(String,PageQuery)→TableDataInfo<AppMessageVo>; countMyUnread()→long; markRead(Long); deleteRead(Long); clearRead() }`；HTTP 端点 `/portal/messages`（GET）、`/portal/messages/unreadCount`（GET）、`/portal/messages/{id}/read`（POST）、`/portal/messages/{id}`（DELETE）、`/portal/messages/history/clear`（DELETE）——T7 拆 kernel 时随包整体搬走。

- [ ] **Step 1: 写失败测试（RED）**

`src/test/java/org/dromara/portal/kernel/service/PortalMessageServiceTest.java`：
```java
package org.dromara.portal.kernel.service;

import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.kernel.domain.AppMessage;
import org.dromara.portal.kernel.mapper.AppMessageMapper;
import org.dromara.portal.kernel.service.impl.PortalMessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalMessageServiceTest {

    @Mock
    private AppMessageMapper messageMapper;

    @InjectMocks
    private PortalMessageServiceImpl service;

    @Test
    void deleteRead_missing_message_throws() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            when(messageMapper.selectOne(any())).thenReturn(null);
            ServiceException ex = assertThrows(ServiceException.class, () -> service.deleteRead(9L));
            assertEquals("通知不存在", ex.getMessage());
        }
    }

    @Test
    void deleteRead_unread_message_rejected() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            AppMessage msg = new AppMessage();
            msg.setIsRead("0");
            when(messageMapper.selectOne(any())).thenReturn(msg);
            ServiceException ex = assertThrows(ServiceException.class, () -> service.deleteRead(9L));
            assertEquals("请先标记已读后再删除", ex.getMessage());
        }
    }

    @Test
    void deleteRead_read_message_deleted() {
        try (MockedStatic<LoginHelper> lh = mockStatic(LoginHelper.class)) {
            lh.when(LoginHelper::getUserId).thenReturn(1L);
            AppMessage msg = new AppMessage();
            msg.setIsRead("1");
            when(messageMapper.selectOne(any())).thenReturn(msg);
            service.deleteRead(9L);
            verify(messageMapper).delete(any());
        }
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：**编译失败**（`PortalMessageServiceImpl` 不存在）。

- [ ] **Step 3: kernel 侧实现（代码自 AppPortalServiceImpl 平移）**

`kernel/service/IPortalMessageService.java`：
```java
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
```

`kernel/service/impl/PortalMessageServiceImpl.java`（方法体来自 `AppPortalServiceImpl.java:264-315` 原文，仅改方法名与 VO 映射收敛为本类私有）：
```java
package org.dromara.portal.kernel.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.kernel.domain.AppMessage;
import org.dromara.portal.kernel.domain.vo.AppMessageVo;
import org.dromara.portal.kernel.mapper.AppMessageMapper;
import org.dromara.portal.kernel.service.IPortalMessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PortalMessageServiceImpl implements IPortalMessageService {

    private final AppMessageMapper messageMapper;

    @Override
    public TableDataInfo<AppMessageVo> pageMyMessages(String isRead, PageQuery pageQuery) {
        Long userId = LoginHelper.getUserId();
        Page<AppMessage> page = messageMapper.selectPage(pageQuery.build(),
            Wrappers.<AppMessage>lambdaQuery()
                .eq(AppMessage::getUserId, userId)
                .eq(StringUtils.isNotBlank(isRead), AppMessage::getIsRead, isRead)
                .orderByDesc(AppMessage::getCreateTime));
        List<AppMessageVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return new TableDataInfo<>(rows, page.getTotal());
    }

    @Override
    public long countMyUnread() {
        Long userId = LoginHelper.getUserId();
        return messageMapper.selectCount(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getUserId, userId).eq(AppMessage::getIsRead, "0"));
    }

    @Override
    public void markRead(Long messageId) {
        Long userId = LoginHelper.getUserId();
        AppMessage up = new AppMessage();
        up.setMessageId(messageId);
        up.setIsRead("1");
        messageMapper.update(up, Wrappers.<AppMessage>lambdaUpdate()
            .eq(AppMessage::getMessageId, messageId).eq(AppMessage::getUserId, userId));
    }

    @Override
    public void deleteRead(Long messageId) {
        Long userId = LoginHelper.getUserId();
        AppMessage message = messageMapper.selectOne(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getMessageId, messageId)
            .eq(AppMessage::getUserId, userId));
        if (message == null) {
            throw new ServiceException("通知不存在");
        }
        if (!"1".equals(message.getIsRead())) {
            throw new ServiceException("请先标记已读后再删除");
        }
        messageMapper.delete(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getMessageId, messageId)
            .eq(AppMessage::getUserId, userId)
            .eq(AppMessage::getIsRead, "1"));
    }

    @Override
    public void clearRead() {
        Long userId = LoginHelper.getUserId();
        messageMapper.delete(Wrappers.<AppMessage>lambdaQuery()
            .eq(AppMessage::getUserId, userId)
            .eq(AppMessage::getIsRead, "1"));
    }

    private AppMessageVo toVo(AppMessage message) {
        AppMessageVo vo = new AppMessageVo();
        vo.setMessageId(message.getMessageId());
        vo.setTitle(message.getTitle());
        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setIsRead(message.getIsRead());
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }
}
```
> 注意：原 `AppPortalServiceImpl` 里的 `toMessageVo` 若与上面 `toVo` 字段有出入（以原文件为准核对一遍），按原文件字段拷贝。

`kernel/controller/PortalMessageController.java`（HTTP 方法与路径与原 AppPortalController **逐一一致**，前端零改动）：
```java
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
```

- [ ] **Step 4: appcenter 侧删除**

1. `AppPortalController.java`：删除 5 个方法 `messages/unreadCount/read/deleteReadMessage/clearReadMessages` 及 import `org.dromara.portal.kernel.domain.vo.AppMessageVo`。
2. `IAppPortalService.java`：删除 5 个方法声明 `messages/unreadCount/readMessage/deleteReadMessage/clearReadMessages` 及同一 import。
3. `AppPortalServiceImpl.java`：删除上述 5 个实现方法与 `toMessageVo`、字段 `private final AppMessageMapper messageMapper;`，以及 imports `org.dromara.portal.kernel.domain.AppMessage`、`org.dromara.portal.kernel.domain.vo.AppMessageVo`、`org.dromara.portal.kernel.mapper.AppMessageMapper`。

- [ ] **Step 5: 昵称解析器下放（移动而非共存，kernel 原件删除）**

新建 `forum/support/ForumUserDisplayNameResolver.java`：内容 = 原 `kernel/support/InfoUserDisplayNameResolver.java` 全文，仅改两处——`package org.dromara.portal.forum.support;`、类名 `ForumUserDisplayNameResolver`。
新建 `resources/support/ResourceUserDisplayNameResolver.java`：同法，`package org.dromara.portal.resources.support;`、类名 `ResourceUserDisplayNameResolver`。
（类名必须不同：单体阶段三个同名 `@Component` 会因默认 bean 名冲突启动失败。）

改引用（import + 字段类型 + 构造注入类型同步改）：
- `forum/service/impl/InfoForumServiceImpl.java`（原 import 在 :30）→ `ForumUserDisplayNameResolver`
- `resources/service/impl/InfoResourceServiceImpl.java`（:33）→ `ResourceUserDisplayNameResolver`
- `resources/service/impl/InfoResourceInteractionServiceImpl.java`（:12）→ `ResourceUserDisplayNameResolver`

删除 `kernel/support/InfoUserDisplayNameResolver.java`；若 `kernel/support/` 目录为空一并删除。全仓 grep 确认无残留引用：
```bash
grep -rn "InfoUserDisplayNameResolver" /Users/macmini/windows-info-serve/source/ && echo "有残留" || echo "OK"
```

- [ ] **Step 6: ArchUnit 收紧①**

`BcBoundaryTest.java` 中 `content_bcs_should_not_depend_on_kernel_impl` 整个方法替换为：
```java
    @Test
    void content_bcs_should_only_touch_kernel_service_interfaces() {
        noClasses().that().resideInAnyPackage(
                "org.dromara.portal.appcenter..", "org.dromara.portal.resources..",
                "org.dromara.portal.forum..", "org.dromara.portal.requiredknowledge..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.dromara.portal.kernel.domain..", "org.dromara.portal.kernel.mapper..",
                "org.dromara.portal.kernel.service.impl..", "org.dromara.portal.kernel.controller..",
                "org.dromara.portal.kernel.support..")
            .check(classes);
    }
```
（过渡态：内容 BC 仅剩 `kernel.service.IPortalNotificationService` 接口依赖，T4 消灭。）

- [ ] **Step 7: 跑测试（GREEN）+ 提交**

```bash
cd /Users/macmini/windows-info-serve/source
mvn -ntp -pl ruoyi-modules/ruoyi-portal -am -DskipTests=false test
```
预期：`BUILD SUCCESS`，PortalMessageServiceTest 3 个用例 + BcBoundaryTest 6 条规则全绿。

```bash
cd /Users/macmini/windows-info-serve
git add -A
git commit -m "refactor: 消息中心归位内核并下放昵称解析器，收紧 BC 边界规则"
```
