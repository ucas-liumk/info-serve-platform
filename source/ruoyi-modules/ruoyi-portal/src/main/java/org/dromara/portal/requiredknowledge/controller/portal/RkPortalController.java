package org.dromara.portal.requiredknowledge.controller.portal;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.portal.requiredknowledge.domain.vo.RkKnowledgeVo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectGroupVo;
import org.dromara.portal.requiredknowledge.service.IRkKnowledgeService;
import org.dromara.portal.requiredknowledge.service.IRkSubjectGroupService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/portal/required-knowledge")
public class RkPortalController {

    private final IRkSubjectGroupService groupService;
    private final IRkKnowledgeService knowledgeService;

    @GetMapping("/catalog")
    public R<List<RkSubjectGroupVo>> catalog() {
        return R.ok(groupService.portalCatalog());
    }

    @GetMapping("/subject/{subjectCode}/knowledge")
    public R<List<RkKnowledgeVo>> knowledge(@PathVariable String subjectCode) {
        return R.ok(knowledgeService.portalListBySubjectCode(subjectCode));
    }
}
