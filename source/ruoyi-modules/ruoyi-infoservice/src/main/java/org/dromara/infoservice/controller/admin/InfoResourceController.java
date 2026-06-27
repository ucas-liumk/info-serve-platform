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
import org.dromara.infoservice.domain.bo.InfoResourceBo;
import org.dromara.infoservice.domain.vo.InfoResourceVo;
import org.dromara.infoservice.domain.vo.ResourceUploadVo;
import org.dromara.infoservice.service.IInfoResourceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/resource")
public class InfoResourceController extends BaseController {

    private final IInfoResourceService resourceService;

    @SaCheckPermission("infoservice:resource:list")
    @GetMapping("/list")
    public TableDataInfo<InfoResourceVo> list(InfoResourceBo bo, PageQuery pageQuery) {
        return resourceService.queryPageList(bo, pageQuery);
    }

    @SaCheckPermission("infoservice:resource:query")
    @GetMapping("/{resourceId}")
    public R<InfoResourceVo> getInfo(@PathVariable Long resourceId) {
        return R.ok(resourceService.queryById(resourceId));
    }

    @SaCheckPermission("infoservice:resource:upload")
    @Log(title = "资料上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public R<ResourceUploadVo> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(resourceService.upload(file));
    }

    @SaCheckPermission("infoservice:resource:add")
    @Log(title = "资料管理", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody InfoResourceBo bo) {
        return toAjax(resourceService.insertByBo(bo));
    }

    @SaCheckPermission("infoservice:resource:edit")
    @Log(title = "资料管理", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping
    public R<Void> edit(@Validated @RequestBody InfoResourceBo bo) {
        return toAjax(resourceService.updateByBo(bo));
    }

    @SaCheckPermission("infoservice:resource:edit")
    @Log(title = "资料管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public R<Void> changeStatus(@RequestBody InfoResourceBo bo) {
        return toAjax(resourceService.changeStatus(bo.getResourceId(), bo.getStatus()));
    }

    @SaCheckPermission("infoservice:resource:remove")
    @Log(title = "资料管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(resourceService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
