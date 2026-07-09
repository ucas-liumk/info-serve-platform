package org.dromara.portal.requiredknowledge.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.portal.requiredknowledge.domain.bo.RkSubjectGroupBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectGroupVo;
import org.dromara.portal.requiredknowledge.service.IRkSubjectGroupService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/required-knowledge/group")
public class RkSubjectGroupController extends BaseController {

    private final IRkSubjectGroupService groupService;

    @SaCheckPermission("requiredKnowledge:group:list")
    @GetMapping("/list")
    public TableDataInfo<RkSubjectGroupVo> list(RkSubjectGroupBo bo, PageQuery pageQuery) {
        return groupService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("requiredKnowledge:group:list")
    @GetMapping("/options")
    public R<List<RkSubjectGroupVo>> options(RkSubjectGroupBo bo) {
        return R.ok(groupService.queryList(bo));
    }

    @SaCheckPermission("requiredKnowledge:group:query")
    @GetMapping("/{groupId}")
    public R<RkSubjectGroupVo> getInfo(@PathVariable Long groupId) {
        return R.ok(groupService.queryById(groupId));
    }

    @SaCheckPermission("requiredKnowledge:group:add")
    @Log(title = "应知应会栏目", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody RkSubjectGroupBo bo) {
        return toAjax(groupService.insertByBo(bo));
    }

    @SaCheckPermission("requiredKnowledge:group:edit")
    @Log(title = "应知应会栏目", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RkSubjectGroupBo bo) {
        return toAjax(groupService.updateByBo(bo));
    }

    @SaCheckPermission("requiredKnowledge:group:edit")
    @Log(title = "应知应会栏目", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody RkSubjectGroupBo bo) {
        return toAjax(groupService.changeStatus(bo.getGroupId(), bo.getStatus()));
    }

    @SaCheckPermission("requiredKnowledge:group:remove")
    @Log(title = "应知应会栏目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(groupService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
