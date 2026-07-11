package org.dromara.portal.resources.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.resources.domain.InfoResource;
import org.dromara.portal.resources.domain.InfoResourceCategory;
import org.dromara.portal.resources.domain.bo.InfoResourceBo;
import org.dromara.portal.resources.domain.bo.InfoResourceCategoryBo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryCountVo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryTreeVo;
import org.dromara.portal.resources.domain.vo.InfoResourceCategoryVo;
import org.dromara.portal.resources.mapper.InfoResourceCategoryLinkMapper;
import org.dromara.portal.resources.mapper.InfoResourceCategoryMapper;
import org.dromara.portal.resources.mapper.InfoResourceMapper;
import org.dromara.common.tenant.helper.TenantHelper;
import org.dromara.portal.resources.service.IInfoResourceCategoryService;
import org.dromara.portal.resources.support.ResourceFacetParams;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InfoResourceCategoryServiceImpl implements IInfoResourceCategoryService {

    /** 编码格式：与多值 categoryCode 逗号协议及门户 URL 传参安全对齐 */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,80}$");

    private final InfoResourceCategoryMapper baseMapper;
    private final InfoResourceMapper resourceMapper;
    private final InfoResourceCategoryLinkMapper categoryLinkMapper;

    private LambdaQueryWrapper<InfoResourceCategory> buildWrapper(InfoResourceCategoryBo bo) {
        LambdaQueryWrapper<InfoResourceCategory> w = Wrappers.lambdaQuery();
        w.eq(StringUtils.isNotBlank(bo.getStatus()), InfoResourceCategory::getStatus, bo.getStatus());
        if (StringUtils.isNotBlank(bo.getKeyword())) {
            w.and(q -> q.like(InfoResourceCategory::getCategoryName, bo.getKeyword())
                .or().like(InfoResourceCategory::getCategoryCode, bo.getKeyword())
                .or().like(InfoResourceCategory::getDescription, bo.getKeyword()));
        }
        w.orderByAsc(InfoResourceCategory::getOrderNum).orderByDesc(InfoResourceCategory::getCreateTime);
        return w;
    }

    private void fillResourceCount(List<InfoResourceCategoryVo> rows) {
        rows.forEach(row -> row.setResourceCount(resourceMapper.selectCount(
            Wrappers.<InfoResource>lambdaQuery()
                .eq(InfoResource::getCategoryId, row.getCategoryId())
                .eq(InfoResource::getStatus, "0"))));
    }

    @Override
    public TableDataInfo<InfoResourceCategoryVo> queryPageList(InfoResourceCategoryBo bo, PageQuery pageQuery) {
        Page<InfoResourceCategoryVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        fillResourceCount(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<InfoResourceCategoryVo> queryList(InfoResourceCategoryBo bo) {
        // C3：平铺接口（上传弹窗 / 资料编辑下拉）只返回二级分类，结构与字段零变化
        List<InfoResourceCategoryVo> rows = baseMapper.selectVoList(
            buildWrapper(bo).isNotNull(InfoResourceCategory::getParentId));
        if ("0".equals(bo.getStatus())) {
            // 停用栏目整组隐藏（与 C1 树语义对齐）：防止资料被挂进门户左栏不可见的分类
            Set<Long> activeSections = selectActiveSectionIds();
            rows = rows.stream()
                .filter(row -> row.getParentId() != null && activeSections.contains(row.getParentId()))
                .toList();
        }
        fillResourceCount(rows);
        return rows;
    }

    private Set<Long> selectActiveSectionIds() {
        return baseMapper.selectList(Wrappers.<InfoResourceCategory>lambdaQuery()
                .isNull(InfoResourceCategory::getParentId)
                .eq(InfoResourceCategory::getStatus, "0"))
            .stream()
            .map(InfoResourceCategory::getCategoryId)
            .collect(Collectors.toSet());
    }

    @Override
    public List<InfoResourceCategoryVo> portalCategories() {
        InfoResourceCategoryBo bo = new InfoResourceCategoryBo();
        bo.setStatus("0");
        return queryList(bo);
    }

    @Override
    public List<InfoResourceCategoryTreeVo> portalCategoryTree(InfoResourceBo bo) {
        List<InfoResourceCategory> categories = baseMapper.selectList(
            Wrappers.<InfoResourceCategory>lambdaQuery()
                .eq(InfoResourceCategory::getStatus, "0")
                .orderByAsc(InfoResourceCategory::getOrderNum)
                .orderByDesc(InfoResourceCategory::getCreateTime));
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        Map<Long, List<InfoResourceCategory>> childrenByParent = categories.stream()
            .filter(c -> c.getParentId() != null)
            .collect(Collectors.groupingBy(InfoResourceCategory::getParentId,
                LinkedHashMap::new, Collectors.toList()));
        if (childrenByParent.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = queryFacetCounts(bo);
        return categories.stream()
            .filter(c -> c.getParentId() == null)
            .map(section -> toSectionNode(section,
                childrenByParent.getOrDefault(section.getCategoryId(), List.of()), counts))
            .filter(node -> !node.getChildren().isEmpty())
            .toList();
    }

    /** 分面计数：一次 GROUP BY 聚合（禁 N+1），入参映射见 ResourceFacetParams */
    private Map<Long, Long> queryFacetCounts(InfoResourceBo bo) {
        ResourceFacetParams facets = ResourceFacetParams.from(bo);
        return resourceMapper.countActiveByCategory(facets.getKeyword(), facets.getPreviewType(),
                facets.getUploadedSince(), facets.getMinFileSize(), facets.getMaxFileSize())
            .stream()
            .collect(Collectors.toMap(InfoResourceCategoryCountVo::getCategoryId,
                InfoResourceCategoryCountVo::getResourceCount, (a, b) -> a));
    }

    private InfoResourceCategoryTreeVo toSectionNode(InfoResourceCategory section,
                                                     List<InfoResourceCategory> children,
                                                     Map<Long, Long> counts) {
        InfoResourceCategoryTreeVo node = toTreeNode(section);
        node.setChildren(children.stream()
            .map(child -> {
                InfoResourceCategoryTreeVo leaf = toTreeNode(child);
                leaf.setResourceCount(counts.getOrDefault(child.getCategoryId(), 0L));
                leaf.setChildren(List.of());
                return leaf;
            })
            .toList());
        return node;
    }

    private InfoResourceCategoryTreeVo toTreeNode(InfoResourceCategory category) {
        InfoResourceCategoryTreeVo node = new InfoResourceCategoryTreeVo();
        node.setCategoryId(category.getCategoryId());
        node.setCategoryCode(category.getCategoryCode());
        node.setCategoryName(category.getCategoryName());
        node.setOrderNum(category.getOrderNum());
        return node;
    }

    @Override
    public List<InfoResourceCategoryVo> queryTreeList() {
        List<InfoResourceCategoryVo> rows = baseMapper.selectVoList(
            Wrappers.<InfoResourceCategory>lambdaQuery()
                .orderByAsc(InfoResourceCategory::getOrderNum)
                .orderByDesc(InfoResourceCategory::getCreateTime));
        fillResourceCountAggregated(rows);
        return rows;
    }

    /** 树表计数：一次 GROUP BY 聚合填充。口径=全部未删（含已下架），与删除校验一致，避免"资料数 0 却拒删"的矛盾反馈 */
    private void fillResourceCountAggregated(List<InfoResourceCategoryVo> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Long, Long> counts = resourceMapper.countUndeletedByCategory()
            .stream()
            .collect(Collectors.toMap(InfoResourceCategoryCountVo::getCategoryId,
                InfoResourceCategoryCountVo::getResourceCount, (a, b) -> a));
        rows.forEach(row -> row.setResourceCount(counts.getOrDefault(row.getCategoryId(), 0L)));
    }

    @Override
    public InfoResourceCategoryVo queryById(Long categoryId) {
        return baseMapper.selectVoById(categoryId);
    }

    @Override
    public Boolean insertByBo(InfoResourceCategoryBo bo) {
        validateCodeFormat(bo);
        validateHierarchy(bo);
        validateCodeUnique(bo);
        InfoResourceCategory add = MapstructUtils.convert(bo, InfoResourceCategory.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(InfoResourceCategoryBo bo) {
        validateCodeFormat(bo);
        validateHierarchy(bo);
        validateCodeUnique(bo);
        validatePromotionToSection(bo);
        InfoResourceCategory up = MapstructUtils.convert(bo, InfoResourceCategory.class);
        // 全局 update-strategy=NOT_NULL 会跳过实体上的 null 字段：parent_id 统一经 wrapper 显式落库，
        // 否则「分类改回栏目」（parentId=null）静默不生效却返回成功（C5：parentId 为空=栏目）
        up.setParentId(null);
        return baseMapper.update(up, Wrappers.<InfoResourceCategory>lambdaUpdate()
            .eq(InfoResourceCategory::getCategoryId, bo.getCategoryId())
            .set(InfoResourceCategory::getParentId, bo.getParentId())) > 0;
    }

    /** 分类改回栏目（parentId 置空）前置校验：已挂资料的分类不能升级为栏目（资料只允许挂二级分类） */
    private void validatePromotionToSection(InfoResourceCategoryBo bo) {
        if (bo.getCategoryId() == null || bo.getParentId() != null) {
            return;
        }
        InfoResourceCategory current = baseMapper.selectById(bo.getCategoryId());
        if (current == null || current.getParentId() == null) {
            return;
        }
        if (countAttachedResources(List.of(bo.getCategoryId())) > 0) {
            throw new ServiceException("该分类下仍有资料，不能调整为栏目");
        }
    }

    /** 编码格式校验：'all' 为门户筛选保留哨兵；逗号/空白会破坏多值 categoryCode 协议，一律拒绝 */
    private void validateCodeFormat(InfoResourceCategoryBo bo) {
        String code = bo.getCategoryCode();
        if (StringUtils.isBlank(code)) {
            return;
        }
        if ("all".equalsIgnoreCase(code)) {
            throw new ServiceException("编码 all 为系统保留值，请更换编码");
        }
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new ServiceException("分类编码仅支持字母、数字、中划线、下划线，长度不超过 80");
        }
    }

    /**
     * C5 两级封顶校验：parentId 为空=栏目；非空=分类，且父必须存在、未删、本身是栏目；
     * 不允许自挂；已有子分类的栏目不允许降为分类（否则出现三级）。
     */
    private void validateHierarchy(InfoResourceCategoryBo bo) {
        Long parentId = bo.getParentId();
        if (parentId == null) {
            return;
        }
        if (parentId.equals(bo.getCategoryId())) {
            throw new ServiceException("上级栏目不能选择自己");
        }
        InfoResourceCategory parent = baseMapper.selectById(parentId);
        if (parent == null) {
            throw new ServiceException("上级栏目不存在或已删除");
        }
        if (parent.getParentId() != null) {
            throw new ServiceException("分类只能挂在栏目下，栏目分类最多两级");
        }
        if (bo.getCategoryId() != null && countChildren(bo.getCategoryId()) > 0) {
            throw new ServiceException("该栏目下已有分类，不能调整为分类");
        }
    }

    private long countChildren(Long categoryId) {
        Long count = baseMapper.selectCount(Wrappers.<InfoResourceCategory>lambdaQuery()
            .eq(InfoResourceCategory::getParentId, categoryId));
        return count == null ? 0L : count;
    }

    /** C5 编码唯一校验：栏目与分类共用编码空间（活跃行，del_flag 由逻辑删除自动过滤） */
    private void validateCodeUnique(InfoResourceCategoryBo bo) {
        if (StringUtils.isBlank(bo.getCategoryCode())) {
            return;
        }
        boolean duplicated = baseMapper.exists(Wrappers.<InfoResourceCategory>lambdaQuery()
            .eq(InfoResourceCategory::getCategoryCode, bo.getCategoryCode())
            .ne(bo.getCategoryId() != null, InfoResourceCategory::getCategoryId, bo.getCategoryId()));
        if (duplicated) {
            throw new ServiceException("分类编码已存在，请更换编码");
        }
    }

    @Override
    public Boolean changeStatus(Long categoryId, String status) {
        InfoResourceCategory up = new InfoResourceCategory();
        up.setCategoryId(categoryId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    /**
     * C5 删除校验：栏目下有未删子分类（含停用）不可删；分类下有未删资料不可删。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        Long childCount = baseMapper.selectCount(Wrappers.<InfoResourceCategory>lambdaQuery()
            .in(InfoResourceCategory::getParentId, ids));
        if (childCount != null && childCount > 0) {
            throw new ServiceException("所选栏目下仍有分类，请先删除或迁移其分类");
        }
        if (countAttachedResources(ids) > 0) {
            throw new ServiceException("所选分类下仍有资料（含已下架或其他租户的资料），请先删除或迁移其资料");
        }
        return baseMapper.deleteByIds(ids) > 0;
    }

    /** 分类为全租户共享字典而资料/关联表带租户列：守卫计数必须跨租户，防止 A 租户绕过 B 租户的挂接。
     * 走关联表（多分类事实源），只计未删资料 */
    private long countAttachedResources(Collection<Long> ids) {
        Long count = TenantHelper.ignore(() -> categoryLinkMapper.countUndeletedResourcesByCategoryIds(ids));
        return count == null ? 0L : count;
    }
}
