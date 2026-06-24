package org.dromara.appcenter.controller.portal;

import lombok.RequiredArgsConstructor;
import org.dromara.appcenter.domain.vo.AppCategoryVo;
import org.dromara.appcenter.domain.vo.AppMessageVo;
import org.dromara.appcenter.domain.vo.PortalAppVo;
import org.dromara.appcenter.service.IAppPortalService;
import org.dromara.common.core.domain.R;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.common.web.core.BaseController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/portal")
public class AppPortalController extends BaseController {

    private final IAppPortalService portalService;

    @GetMapping("/categories")
    public R<List<AppCategoryVo>> categories() {
        return R.ok(portalService.categories());
    }

    @GetMapping("/apps")
    public TableDataInfo<PortalAppVo> apps(@RequestParam(required = false) String categoryCode,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false, defaultValue = "latest") String sort,
                                           PageQuery pageQuery) {
        return portalService.apps(categoryCode, keyword, sort, pageQuery);
    }

    @PostMapping("/apps/{id}/use")
    public R<String> use(@PathVariable Long id) {
        return R.ok("操作成功", portalService.use(id));
    }

    @PostMapping("/apps/{id}/favorite")
    public R<Void> favorite(@PathVariable Long id) {
        portalService.favorite(id, true);
        return R.ok();
    }

    @DeleteMapping("/apps/{id}/favorite")
    public R<Void> unfavorite(@PathVariable Long id) {
        portalService.favorite(id, false);
        return R.ok();
    }

    @PostMapping("/apps/{id}/recommend")
    public R<Void> recommend(@PathVariable Long id) {
        portalService.recommend(id, true);
        return R.ok();
    }

    @DeleteMapping("/apps/{id}/recommend")
    public R<Void> unrecommend(@PathVariable Long id) {
        portalService.recommend(id, false);
        return R.ok();
    }

    @GetMapping("/favorites")
    public TableDataInfo<PortalAppVo> favorites(PageQuery pageQuery) {
        return portalService.favorites(pageQuery);
    }

    @GetMapping("/messages")
    public TableDataInfo<AppMessageVo> messages(@RequestParam(required = false) String isRead, PageQuery pageQuery) {
        return portalService.messages(isRead, pageQuery);
    }

    @GetMapping("/messages/unreadCount")
    public R<Long> unreadCount() {
        return R.ok(portalService.unreadCount());
    }

    @PostMapping("/messages/{id}/read")
    public R<Void> read(@PathVariable Long id) {
        portalService.readMessage(id);
        return R.ok();
    }
}
