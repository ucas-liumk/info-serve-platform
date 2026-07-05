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
import org.dromara.portal.requiredknowledge.domain.bo.RkKnowledgeBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkKnowledgeVo;
import org.dromara.portal.requiredknowledge.mapper.RkKnowledgeMapper;
import org.dromara.portal.requiredknowledge.mapper.RkSubjectMapper;
import org.dromara.portal.requiredknowledge.service.IRkKnowledgeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RkKnowledgeServiceImpl implements IRkKnowledgeService {

    private final RkKnowledgeMapper baseMapper;
    private final RkSubjectMapper subjectMapper;

    private LambdaQueryWrapper<RkKnowledge> buildWrapper(RkKnowledgeBo bo) {
        RkKnowledgeBo query = bo == null ? new RkKnowledgeBo() : bo;
        LambdaQueryWrapper<RkKnowledge> w = Wrappers.lambdaQuery();
        Long subjectId = resolveSubjectId(query);
        w.eq(subjectId != null, RkKnowledge::getSubjectId, subjectId);
        w.eq(StringUtils.isNotBlank(query.getStatus()), RkKnowledge::getStatus, query.getStatus());
        if (StringUtils.isNotBlank(query.getKeyword())) {
            w.and(q -> q.like(RkKnowledge::getTitle, query.getKeyword())
                .or().like(RkKnowledge::getSummary, query.getKeyword())
                .or().like(RkKnowledge::getContent, query.getKeyword()));
        }
        w.orderByAsc(RkKnowledge::getOrderNum).orderByDesc(RkKnowledge::getCreateTime);
        return w;
    }

    private Long resolveSubjectId(RkKnowledgeBo bo) {
        if (bo.getSubjectId() != null) {
            return bo.getSubjectId();
        }
        if (StringUtils.isBlank(bo.getSubjectCode())) {
            return null;
        }
        RkSubject subject = subjectMapper.selectOne(
            Wrappers.<RkSubject>lambdaQuery().eq(RkSubject::getSubjectCode, bo.getSubjectCode()));
        return subject == null ? -1L : subject.getSubjectId();
    }

    private RkSubject requireSubject(Long subjectId) {
        RkSubject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new ServiceException("所属科目不存在");
        }
        return subject;
    }

    private void fillSubjectInfo(List<RkKnowledgeVo> rows) {
        List<Long> subjectIds = rows.stream().map(RkKnowledgeVo::getSubjectId).filter(Objects::nonNull).distinct().toList();
        if (subjectIds.isEmpty()) {
            return;
        }
        Map<Long, RkSubject> subjects = subjectMapper.selectBatchIds(subjectIds)
            .stream().collect(Collectors.toMap(RkSubject::getSubjectId, item -> item));
        rows.forEach(row -> {
            RkSubject subject = subjects.get(row.getSubjectId());
            if (subject != null) {
                row.setSubjectName(subject.getSubjectName());
                row.setSubjectCode(subject.getSubjectCode());
            }
        });
    }

    private void refreshSubjectKnowledgeCount(Collection<Long> subjectIds) {
        subjectIds.stream().filter(Objects::nonNull).distinct().forEach(subjectId -> {
            Long count = baseMapper.selectCount(Wrappers.<RkKnowledge>lambdaQuery()
                .eq(RkKnowledge::getSubjectId, subjectId)
                .eq(RkKnowledge::getStatus, "0"));
            RkSubject update = new RkSubject();
            update.setSubjectId(subjectId);
            update.setKnowledgeCount(Math.toIntExact(count));
            subjectMapper.updateById(update);
        });
    }

    @Override
    public TableDataInfo<RkKnowledgeVo> queryPageList(RkKnowledgeBo bo, PageQuery pageQuery) {
        Page<RkKnowledgeVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        fillSubjectInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<RkKnowledgeVo> queryList(RkKnowledgeBo bo) {
        List<RkKnowledgeVo> rows = baseMapper.selectVoList(buildWrapper(bo));
        fillSubjectInfo(rows);
        return rows;
    }

    @Override
    public List<RkKnowledgeVo> portalListBySubjectCode(String subjectCode) {
        RkSubject subject = subjectMapper.selectOne(Wrappers.<RkSubject>lambdaQuery()
            .eq(RkSubject::getSubjectCode, subjectCode)
            .eq(RkSubject::getStatus, "0"));
        if (subject == null) {
            return List.of();
        }
        List<RkKnowledgeVo> rows = baseMapper.selectVoList(Wrappers.<RkKnowledge>lambdaQuery()
            .eq(RkKnowledge::getSubjectId, subject.getSubjectId())
            .eq(RkKnowledge::getStatus, "0")
            .orderByAsc(RkKnowledge::getOrderNum)
            .orderByDesc(RkKnowledge::getCreateTime));
        fillSubjectInfo(rows);
        return rows;
    }

    @Override
    public RkKnowledgeVo queryById(Long knowledgeId) {
        RkKnowledgeVo row = baseMapper.selectVoById(knowledgeId);
        if (row != null) {
            fillSubjectInfo(List.of(row));
        }
        return row;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(RkKnowledgeBo bo) {
        requireSubject(bo.getSubjectId());
        RkKnowledge add = MapstructUtils.convert(bo, RkKnowledge.class);
        boolean result = baseMapper.insert(add) > 0;
        refreshSubjectKnowledgeCount(List.of(bo.getSubjectId()));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(RkKnowledgeBo bo) {
        requireSubject(bo.getSubjectId());
        RkKnowledge old = baseMapper.selectById(bo.getKnowledgeId());
        RkKnowledge up = MapstructUtils.convert(bo, RkKnowledge.class);
        boolean result = baseMapper.updateById(up) > 0;
        Set<Long> subjectIds = new HashSet<>();
        if (old != null) {
            subjectIds.add(old.getSubjectId());
        }
        subjectIds.add(bo.getSubjectId());
        refreshSubjectKnowledgeCount(subjectIds);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean changeStatus(Long knowledgeId, String status) {
        RkKnowledge old = baseMapper.selectById(knowledgeId);
        RkKnowledge up = new RkKnowledge();
        up.setKnowledgeId(knowledgeId);
        up.setStatus(status);
        boolean result = baseMapper.updateById(up) > 0;
        if (old != null) {
            refreshSubjectKnowledgeCount(List.of(old.getSubjectId()));
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        List<Long> subjectIds = baseMapper.selectList(Wrappers.<RkKnowledge>lambdaQuery().in(RkKnowledge::getKnowledgeId, ids))
            .stream().map(RkKnowledge::getSubjectId).distinct().toList();
        boolean result = baseMapper.deleteByIds(ids) > 0;
        refreshSubjectKnowledgeCount(subjectIds);
        return result;
    }
}
