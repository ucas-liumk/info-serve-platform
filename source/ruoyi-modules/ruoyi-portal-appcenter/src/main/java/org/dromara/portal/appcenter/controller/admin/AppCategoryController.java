package org.dromara.portal.appcenter.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.portal.appcenter.domain.bo.AppCategoryBo;
import org.dromara.portal.appcenter.domain.vo.AppCategoryVo;
import org.dromara.portal.appcenter.service.IAppCategoryService;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/category")
public class AppCategoryController extends BaseController {

    private final IAppCategoryService categoryService;

    @SaCheckPermission("appcenter:category:list")
    @GetMapping("/list")
    public R<List<AppCategoryVo>> list() {
        return R.ok(categoryService.queryList());
    }

    @SaCheckPermission("appcenter:category:query")
    @GetMapping("/{categoryId}")
    public R<AppCategoryVo> getInfo(@PathVariable Long categoryId) {
        return R.ok(categoryService.queryById(categoryId));
    }

    @SaCheckPermission("appcenter:category:add")
    @Log(title = "应用分类", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody AppCategoryBo bo) {
        return toAjax(categoryService.insertByBo(bo));
    }

    @SaCheckPermission("appcenter:category:edit")
    @Log(title = "应用分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody AppCategoryBo bo) {
        return toAjax(categoryService.updateByBo(bo));
    }

    @SaCheckPermission("appcenter:category:remove")
    @Log(title = "应用分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(categoryService.deleteByIds(Arrays.asList(ids)));
    }
}
