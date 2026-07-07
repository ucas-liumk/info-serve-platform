package org.dromara.portal.kernel.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.portal.kernel.domain.PortalModule;
import org.dromara.portal.kernel.service.IPortalModuleService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门户模块注册表管理（后台可配：启停/排序/权限可见性）
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/module")
public class PortalModuleController {

    private final IPortalModuleService moduleService;

    @SaCheckPermission("portal:module:list")
    @GetMapping("/list")
    public R<List<PortalModule>> list() {
        return R.ok(moduleService.listAll());
    }

    @SaCheckPermission("portal:module:add")
    @Log(title = "门户模块", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@RequestBody PortalModule module) {
        return moduleService.insert(module) ? R.ok() : R.fail();
    }

    @SaCheckPermission("portal:module:edit")
    @Log(title = "门户模块", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody PortalModule module) {
        return moduleService.update(module) ? R.ok() : R.fail();
    }

    @SaCheckPermission("portal:module:remove")
    @Log(title = "门户模块", businessType = BusinessType.DELETE)
    @DeleteMapping("/{moduleId}")
    public R<Void> remove(@PathVariable Long moduleId) {
        return moduleService.deleteById(moduleId) ? R.ok() : R.fail();
    }
}
