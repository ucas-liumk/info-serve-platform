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
import org.dromara.portal.requiredknowledge.domain.bo.RkKnowledgeBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkKnowledgeVo;
import org.dromara.portal.requiredknowledge.service.IRkKnowledgeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/required-knowledge/knowledge")
public class RkKnowledgeController extends BaseController {

    private final IRkKnowledgeService knowledgeService;

    @SaCheckPermission("requiredKnowledge:knowledge:list")
    @GetMapping("/list")
    public TableDataInfo<RkKnowledgeVo> list(RkKnowledgeBo bo, PageQuery pageQuery) {
        return knowledgeService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("requiredKnowledge:knowledge:query")
    @GetMapping("/{knowledgeId}")
    public R<RkKnowledgeVo> getInfo(@PathVariable Long knowledgeId) {
        return R.ok(knowledgeService.queryById(knowledgeId));
    }

    @SaCheckPermission("requiredKnowledge:knowledge:add")
    @Log(title = "应知应会知识点", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody RkKnowledgeBo bo) {
        return toAjax(knowledgeService.insertByBo(bo));
    }

    @SaCheckPermission("requiredKnowledge:knowledge:edit")
    @Log(title = "应知应会知识点", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody RkKnowledgeBo bo) {
        return toAjax(knowledgeService.updateByBo(bo));
    }

    @SaCheckPermission("requiredKnowledge:knowledge:edit")
    @Log(title = "应知应会知识点", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody RkKnowledgeBo bo) {
        return toAjax(knowledgeService.changeStatus(bo.getKnowledgeId(), bo.getStatus()));
    }

    @SaCheckPermission("requiredKnowledge:knowledge:remove")
    @Log(title = "应知应会知识点", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(knowledgeService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
