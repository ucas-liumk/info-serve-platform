package org.dromara.infoservice.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.dromara.infoservice.domain.bo.InfoResourceCategoryBo;
import org.dromara.infoservice.domain.vo.InfoResourceCategoryVo;
import org.dromara.infoservice.service.IInfoResourceCategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/resource/category")
public class InfoResourceCategoryController extends BaseController {

    private final IInfoResourceCategoryService categoryService;

    @SaCheckPermission("infoservice:resourceCategory:list")
    @GetMapping("/list")
    public TableDataInfo<InfoResourceCategoryVo> list(InfoResourceCategoryBo bo, PageQuery pageQuery) {
        return categoryService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("infoservice:resourceCategory:list")
    @GetMapping("/options")
    public R<List<InfoResourceCategoryVo>> options(InfoResourceCategoryBo bo) {
        return R.ok(categoryService.queryList(bo));
    }

    @SaCheckPermission("infoservice:resourceCategory:query")
    @GetMapping("/{categoryId}")
    public R<InfoResourceCategoryVo> getInfo(@PathVariable Long categoryId) {
        return R.ok(categoryService.queryById(categoryId));
    }

    @SaCheckPermission("infoservice:resourceCategory:add")
    @Log(title = "资料分类", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody InfoResourceCategoryBo bo) {
        return toAjax(categoryService.insertByBo(bo));
    }

    @SaCheckPermission("infoservice:resourceCategory:edit")
    @Log(title = "资料分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody InfoResourceCategoryBo bo) {
        return toAjax(categoryService.updateByBo(bo));
    }

    @SaCheckPermission("infoservice:resourceCategory:edit")
    @Log(title = "资料分类", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody InfoResourceCategoryBo bo) {
        return toAjax(categoryService.changeStatus(bo.getCategoryId(), bo.getStatus()));
    }

    @SaCheckPermission("infoservice:resourceCategory:remove")
    @Log(title = "资料分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(categoryService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
