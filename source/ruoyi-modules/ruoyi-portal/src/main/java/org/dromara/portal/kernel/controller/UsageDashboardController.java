package org.dromara.portal.kernel.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.portal.kernel.domain.vo.UsageDashboardOverviewVo;
import org.dromara.portal.kernel.service.IUsageDashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/usage-dashboard")
public class UsageDashboardController {

    private final IUsageDashboardService dashboardService;

    @GetMapping("/overview")
    public R<UsageDashboardOverviewVo> overview() {
        return R.ok(dashboardService.overview());
    }
}
