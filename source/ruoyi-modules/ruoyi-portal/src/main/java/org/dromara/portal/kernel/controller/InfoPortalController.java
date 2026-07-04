package org.dromara.portal.kernel.controller;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.portal.kernel.domain.vo.PortalStatsVo;
import org.dromara.portal.kernel.service.IInfoPortalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/portal")
public class InfoPortalController {

    private final IInfoPortalService portalService;

    @GetMapping("/stats")
    public R<PortalStatsVo> stats() {
        return R.ok(portalService.stats());
    }
}
