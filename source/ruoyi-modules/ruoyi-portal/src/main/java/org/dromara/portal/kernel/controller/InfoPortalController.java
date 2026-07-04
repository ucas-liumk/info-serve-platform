package org.dromara.portal.kernel.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.portal.kernel.domain.PortalModule;
import org.dromara.portal.kernel.domain.vo.PortalStatsVo;
import org.dromara.portal.kernel.service.IInfoPortalService;
import org.dromara.portal.kernel.service.IPortalModuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/portal")
public class InfoPortalController {

    private final IInfoPortalService portalService;
    private final IPortalModuleService moduleService;

    @GetMapping("/stats")
    public R<PortalStatsVo> stats() {
        return R.ok(portalService.stats());
    }

    /** 门户首页模块卡片：从注册表读取，按当前用户权限过滤 */
    @GetMapping("/modules")
    public R<List<PortalModule>> modules() {
        return R.ok(moduleService.listVisible());
    }
}
