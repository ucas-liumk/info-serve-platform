package org.dromara.appcenter.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.appcenter.domain.bo.AppDemandBo;
import org.dromara.appcenter.domain.vo.AppDemandVo;
import org.dromara.appcenter.service.IAppDemandService;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demand")
public class AppDemandController extends BaseController {

    private final IAppDemandService demandService;

    @SaCheckPermission("appcenter:demand:list")
    @GetMapping("/list")
    public TableDataInfo<AppDemandVo> list(AppDemandBo bo, PageQuery pageQuery) {
        return demandService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("appcenter:demand:query")
    @GetMapping("/{demandId}")
    public R<AppDemandVo> getInfo(@PathVariable Long demandId) {
        return R.ok(demandService.queryById(demandId));
    }

    @SaCheckPermission("appcenter:demand:edit")
    @Log(title = "需求反馈", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/handle")
    public R<Void> handle(@RequestBody AppDemandBo bo) {
        return toAjax(demandService.updateHandle(bo));
    }

    @SaCheckPermission("appcenter:demand:remove")
    @Log(title = "需求反馈", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(demandService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
