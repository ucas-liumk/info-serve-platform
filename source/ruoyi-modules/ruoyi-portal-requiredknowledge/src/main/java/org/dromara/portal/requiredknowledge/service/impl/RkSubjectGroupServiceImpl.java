package org.dromara.portal.requiredknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.requiredknowledge.domain.RkKnowledge;
import org.dromara.portal.requiredknowledge.domain.RkSubject;
import org.dromara.portal.requiredknowledge.domain.RkSubjectGroup;
import org.dromara.portal.requiredknowledge.domain.bo.RkSubjectGroupBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectGroupVo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectVo;
import org.dromara.portal.requiredknowledge.mapper.RkKnowledgeMapper;
import org.dromara.portal.requiredknowledge.mapper.RkSubjectGroupMapper;
import org.dromara.portal.requiredknowledge.mapper.RkSubjectMapper;
import org.dromara.portal.requiredknowledge.service.IRkSubjectGroupService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RkSubjectGroupServiceImpl implements IRkSubjectGroupService {

    private final RkSubjectGroupMapper baseMapper;
    private final RkSubjectMapper subjectMapper;
    private final RkKnowledgeMapper knowledgeMapper;

    private LambdaQueryWrapper<RkSubjectGroup> buildWrapper(RkSubjectGroupBo bo) {
        RkSubjectGroupBo query = bo == null ? new RkSubjectGroupBo() : bo;
        LambdaQueryWrapper<RkSubjectGroup> w = Wrappers.lambdaQuery();
        w.eq(StringUtils.isNotBlank(query.getStatus()), RkSubjectGroup::getStatus, query.getStatus());
        if (StringUtils.isNotBlank(query.getKeyword())) {
            w.and(q -> q.like(RkSubjectGroup::getGroupName, query.getKeyword())
                .or().like(RkSubjectGroup::getGroupCode, query.getKeyword())
                .or().like(RkSubjectGroup::getDescription, query.getKeyword()));
        }
        w.orderByAsc(RkSubjectGroup::getOrderNum).orderByDesc(RkSubjectGroup::getCreateTime);
        return w;
    }

    private void fillSubjectCount(List<RkSubjectGroupVo> rows) {
        rows.forEach(row -> row.setSubjectCount(subjectMapper.selectCount(
            Wrappers.<RkSubject>lambdaQuery().eq(RkSubject::getGroupId, row.getGroupId()))));
    }

    @Override
    public TableDataInfo<RkSubjectGroupVo> queryPageList(RkSubjectGroupBo bo, PageQuery pageQuery) {
        Page<RkSubjectGroupVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        fillSubjectCount(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<RkSubjectGroupVo> queryList(RkSubjectGroupBo bo) {
        List<RkSubjectGroupVo> rows = baseMapper.selectVoList(buildWrapper(bo));
        fillSubjectCount(rows);
        return rows;
    }

    @Override
    public List<RkSubjectGroupVo> portalCatalog() {
        RkSubjectGroupBo bo = new RkSubjectGroupBo();
        bo.setStatus("0");
        List<RkSubjectGroupVo> groups = baseMapper.selectVoList(buildWrapper(bo));
        List<Long> groupIds = groups.stream().map(RkSubjectGroupVo::getGroupId).toList();
        if (groupIds.isEmpty()) {
            return groups;
        }
        List<RkSubjectVo> subjects = subjectMapper.selectVoList(
            Wrappers.<RkSubject>lambdaQuery()
                .in(RkSubject::getGroupId, groupIds)
                .eq(RkSubject::getStatus, "0")
                .orderByAsc(RkSubject::getOrderNum)
                .orderByDesc(RkSubject::getCreateTime));
        List<Long> subjectIds = subjects.stream().map(RkSubjectVo::getSubjectId).toList();
        Map<Long, Long> knowledgeCounts = subjectIds.isEmpty() ? Map.of() : knowledgeMapper.selectList(
                Wrappers.<RkKnowledge>lambdaQuery()
                    .in(RkKnowledge::getSubjectId, subjectIds)
                    .eq(RkKnowledge::getStatus, "0"))
            .stream().collect(Collectors.groupingBy(RkKnowledge::getSubjectId, Collectors.counting()));
        subjects.forEach(subject -> subject.setKnowledgeCount(Math.toIntExact(knowledgeCounts.getOrDefault(subject.getSubjectId(), 0L))));
        Map<Long, List<RkSubjectVo>> subjectsByGroup = subjects.stream().collect(Collectors.groupingBy(RkSubjectVo::getGroupId));
        groups.forEach(group -> {
            List<RkSubjectVo> groupSubjects = subjectsByGroup.getOrDefault(group.getGroupId(), List.of());
            group.setSubjects(groupSubjects);
            group.setSubjectCount((long) groupSubjects.size());
        });
        return groups.stream().filter(group -> group.getSubjectCount() > 0).toList();
    }

    @Override
    public RkSubjectGroupVo queryById(Long groupId) {
        return baseMapper.selectVoById(groupId);
    }

    @Override
    public Boolean insertByBo(RkSubjectGroupBo bo) {
        RkSubjectGroup add = MapstructUtils.convert(bo, RkSubjectGroup.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(RkSubjectGroupBo bo) {
        RkSubjectGroup up = MapstructUtils.convert(bo, RkSubjectGroup.class);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeStatus(Long groupId, String status) {
        RkSubjectGroup up = new RkSubjectGroup();
        up.setGroupId(groupId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        long subjectCount = subjectMapper.selectCount(Wrappers.<RkSubject>lambdaQuery().in(RkSubject::getGroupId, ids));
        if (subjectCount > 0) {
            throw new ServiceException("栏目下存在科目，不能删除");
        }
        return baseMapper.deleteByIds(ids) > 0;
    }
}
