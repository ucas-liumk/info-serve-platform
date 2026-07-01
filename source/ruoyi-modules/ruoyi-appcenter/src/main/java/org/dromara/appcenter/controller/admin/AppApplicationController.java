package org.dromara.appcenter.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import lombok.RequiredArgsConstructor;
import org.dromara.appcenter.domain.bo.AppApplicationBo;
import org.dromara.appcenter.domain.vo.AppApplicationVo;
import org.dromara.appcenter.domain.vo.AppPackageUploadVo;
import org.dromara.appcenter.service.IAppApplicationService;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/application")
public class AppApplicationController extends BaseController {

    private final IAppApplicationService appService;

    @SaCheckPermission("appcenter:application:list")
    @GetMapping("/list")
    public TableDataInfo<AppApplicationVo> list(AppApplicationBo bo, PageQuery pageQuery) {
        return appService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("appcenter:application:query")
    @GetMapping("/{appId}")
    public R<AppApplicationVo> getInfo(@PathVariable Long appId) {
        return R.ok(appService.queryById(appId));
    }

    @SaCheckPermission("appcenter:application:add")
    @Log(title = "应用管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody AppApplicationBo bo) {
        return toAjax(appService.insertByBo(bo));
    }

    @SaCheckPermission("appcenter:application:edit")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody AppApplicationBo bo) {
        return toAjax(appService.updateByBo(bo));
    }

    @SaCheckPermission("appcenter:application:edit")
    @Log(title = "应用管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody AppApplicationBo bo) {
        return toAjax(appService.changeStatus(bo.getAppId(), bo.getStatus()));
    }

    @SaCheckPermission(value = {"appcenter:application:add", "appcenter:application:edit"}, mode = SaMode.OR)
    @Log(title = "离线安装包上传", businessType = BusinessType.INSERT)
    @PostMapping(value = "/package/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<AppPackageUploadVo> uploadPackage(@RequestPart("file") MultipartFile file) {
        return R.ok(appService.uploadPackage(file));
    }

    @SaCheckPermission("appcenter:application:remove")
    @Log(title = "应用管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(appService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
