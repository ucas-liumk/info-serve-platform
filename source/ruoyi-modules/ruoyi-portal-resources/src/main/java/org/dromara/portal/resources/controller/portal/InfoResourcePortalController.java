package org.dromara.portal.resources.controller.portal;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.idempotent.annotation.RepeatSubmit;
import org.dromara.common.log.annotation.Log;
import org.dromara.common.log.enums.BusinessType;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.bo.InfoResourceNoteBo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryTreeVo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryVo;
import org.dromara.portal.resources.domain.vo.InfoResourceNoteVo;
import org.dromara.portal.resources.domain.vo.InfoResourceVo;
import org.dromara.portal.resources.domain.vo.InfoResourceViewRecordVo;
import org.dromara.portal.resources.domain.vo.ResourceUploadVo;
import org.dromara.portal.resources.service.IInfoResourceCategoryService;
import org.dromara.portal.resources.service.IInfoResourceInteractionService;
import org.dromara.portal.resources.service.IInfoResourceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/resources")
public class InfoResourcePortalController {

    private final IInfoResourceCategoryService categoryService;
    private final IInfoResourceService resourceService;
    private final IInfoResourceInteractionService interactionService;

    @GetMapping("/categories")
    public R<List<InfoResourceCategoryVo>> categories() {
        return R.ok(categoryService.portalCategories());
    }

    /** 栏目/分类两级树（C1）：分类节点带分面计数（响应关键词+工具条筛选，不含分类维度自身） */
    @GetMapping("/category-tree")
    public R<List<InfoResourceCategoryTreeVo>> categoryTree(InfoResourceBo bo) {
        return R.ok(categoryService.portalCategoryTree(bo));
    }

    @GetMapping
    public TableDataInfo<InfoResourceVo> list(InfoResourceBo bo, PageQuery pageQuery) {
        return resourceService.portalPage(bo, pageQuery);
    }

    @GetMapping("/{resourceId}")
    public R<InfoResourceVo> detail(@PathVariable Long resourceId) {
        return R.ok(resourceService.queryPortalDetail(resourceId));
    }

    @Log(title = "门户资料上传", businessType = BusinessType.INSERT)
    @PostMapping("/upload")
    public R<ResourceUploadVo> upload(@RequestPart("file") MultipartFile file) {
        return R.ok(resourceService.portalUpload(file));
    }

    @Log(title = "门户资料发布", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping
    public R<Void> add(@Validated @RequestBody InfoResourceBo bo) {
        return Boolean.TRUE.equals(resourceService.insertPortalByBo(bo)) ? R.ok() : R.fail("发布资料失败");
    }

    @Log(title = "门户资料编辑", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{resourceId}")
    public R<Void> edit(@PathVariable Long resourceId, @Validated @RequestBody InfoResourceBo bo) {
        bo.setResourceId(resourceId);
        return Boolean.TRUE.equals(resourceService.updateOwnByBo(bo)) ? R.ok() : R.fail("保存资料失败");
    }

    @Log(title = "门户资料状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{resourceId}/status")
    public R<Void> changeStatus(@PathVariable Long resourceId, @RequestBody InfoResourceBo bo) {
        return Boolean.TRUE.equals(resourceService.changeOwnStatus(resourceId, bo.getStatus())) ? R.ok() : R.fail("更新状态失败");
    }

    @Log(title = "门户资料删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{resourceId}")
    public R<Void> remove(@PathVariable Long resourceId) {
        return Boolean.TRUE.equals(resourceService.deleteOwnById(resourceId)) ? R.ok() : R.fail("删除资料失败");
    }

    @Log(title = "门户资料收藏", businessType = BusinessType.INSERT)
    @PostMapping("/{resourceId}/favorite")
    public R<Void> favorite(@PathVariable Long resourceId) {
        resourceService.favorite(resourceId, true);
        return R.ok();
    }

    @Log(title = "门户资料取消收藏", businessType = BusinessType.DELETE)
    @DeleteMapping("/{resourceId}/favorite")
    public R<Void> unfavorite(@PathVariable Long resourceId) {
        resourceService.favorite(resourceId, false);
        return R.ok();
    }

    @GetMapping("/{resourceId}/notes/my")
    public TableDataInfo<InfoResourceNoteVo> myNotes(@PathVariable Long resourceId, PageQuery pageQuery) {
        return interactionService.myNotes(resourceId, pageQuery);
    }

    @GetMapping("/{resourceId}/notes/public")
    public TableDataInfo<InfoResourceNoteVo> publicNotes(@PathVariable Long resourceId, PageQuery pageQuery) {
        return interactionService.publicNotes(resourceId, pageQuery);
    }

    @Log(title = "门户资料笔记", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/{resourceId}/notes")
    public R<InfoResourceNoteVo> createNote(@PathVariable Long resourceId, @Validated @RequestBody InfoResourceNoteBo bo) {
        return R.ok(interactionService.createNote(resourceId, bo));
    }

    @Log(title = "门户资料笔记", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/{resourceId}/notes/{noteId}")
    public R<InfoResourceNoteVo> updateNote(@PathVariable Long resourceId, @PathVariable Long noteId, @Validated @RequestBody InfoResourceNoteBo bo) {
        return R.ok(interactionService.updateNote(resourceId, noteId, bo));
    }

    @Log(title = "门户资料笔记", businessType = BusinessType.DELETE)
    @DeleteMapping("/{resourceId}/notes/{noteId}")
    public R<Void> deleteNote(@PathVariable Long resourceId, @PathVariable Long noteId) {
        return Boolean.TRUE.equals(interactionService.deleteNote(resourceId, noteId)) ? R.ok() : R.fail("删除笔记失败");
    }

    @GetMapping("/{resourceId}/view-records")
    public TableDataInfo<InfoResourceViewRecordVo> viewRecords(@PathVariable Long resourceId, PageQuery pageQuery) {
        return interactionService.viewRecords(resourceId, pageQuery);
    }

    @GetMapping("/{resourceId}/preview")
    public void preview(@PathVariable Long resourceId, HttpServletResponse response) throws IOException {
        resourceService.previewFile(resourceId, response);
    }

    @GetMapping("/{resourceId}/pdf-preview")
    public void pdfPreview(@PathVariable Long resourceId, HttpServletResponse response) throws IOException {
        resourceService.previewPdf(resourceId, response);
    }

    @GetMapping("/{resourceId}/download")
    public void download(@PathVariable Long resourceId, HttpServletResponse response) throws IOException {
        resourceService.downloadFile(resourceId, response);
    }
}
