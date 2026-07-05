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
import org.dromara.portal.requiredknowledge.domain.bo.RkSubjectBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectVo;
import org.dromara.portal.requiredknowledge.service.IRkSubjectService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/required-knowledge/subject")
public class RkSubjectController extends BaseController {

    private final IRkSubjectService subjectService;

    @SaCheckPermission("requiredKnowledge:subject:list")
    @GetMapping("/list")
    public TableDataInfo<RkSubjectVo> list(RkSubjectBo bo, PageQuery pageQuery) {
        return subjectService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("requiredKnowledge:subject:list")
    @GetMapping("/options")
    public R<List<RkSubjectVo>> options(RkSubjectBo bo) {
        return R.ok(subjectService.queryList(bo));
    }

    @SaCheckPermission("requiredKnowledge:subject:query")
    @GetMapping("/{subjectId}")
    public R<RkSubjectVo> getInfo(@PathVariable Long subjectId) {
        return R.ok(subjectService.queryById(subjectId));
    }

    @SaCheckPermission("requiredKnowledge:subject:add")
    @Log(title = "应知应会科目", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody RkSubjectBo bo) {
        return toAjax(subjectService.insertByBo(bo));
    }

    @SaCheckPermission("requiredKnowledge:subject:edit")
    @Log(title = "应知应会科目", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RkSubjectBo bo) {
        return toAjax(subjectService.updateByBo(bo));
    }

    @SaCheckPermission("requiredKnowledge:subject:edit")
    @Log(title = "应知应会科目", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody RkSubjectBo bo) {
        return toAjax(subjectService.changeStatus(bo.getSubjectId(), bo.getStatus()));
    }

    @SaCheckPermission("requiredKnowledge:subject:remove")
    @Log(title = "应知应会科目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(subjectService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
