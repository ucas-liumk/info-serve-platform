package org.dromara.portal.requiredknowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.dromara.portal.requiredknowledge.domain.RkSubject;
import org.dromara.portal.requiredknowledge.domain.RkSubjectGroup;
import org.dromara.portal.requiredknowledge.domain.bo.RkSubjectBo;
import org.dromara.portal.requiredknowledge.domain.vo.RkSubjectVo;
import org.dromara.portal.requiredknowledge.mapper.RkSubjectGroupMapper;
import org.dromara.portal.requiredknowledge.mapper.RkSubjectMapper;
import org.dromara.portal.requiredknowledge.service.IRkSubjectService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class RkSubjectServiceImpl implements IRkSubjectService {

    private final RkSubjectMapper baseMapper;
    private final RkSubjectGroupMapper groupMapper;

    private LambdaQueryWrapper<RkSubject> buildWrapper(RkSubjectBo bo) {
        RkSubjectBo query = bo == null ? new RkSubjectBo() : bo;
        LambdaQueryWrapper<RkSubject> w = Wrappers.lambdaQuery();
        w.eq(query.getGroupId() != null, RkSubject::getGroupId, query.getGroupId());
        w.eq(StringUtils.isNotBlank(query.getStatus()), RkSubject::getStatus, query.getStatus());
        if (StringUtils.isNotBlank(query.getKeyword())) {
            w.and(q -> q.like(RkSubject::getSubjectName, query.getKeyword())
                .or().like(RkSubject::getSubjectCode, query.getKeyword())
                .or().like(RkSubject::getDescription, query.getKeyword()));
        }
        w.orderByAsc(RkSubject::getOrderNum).orderByDesc(RkSubject::getCreateTime);
        return w;
    }

    private void fillGroupInfo(List<RkSubjectVo> rows) {
        List<Long> groupIds = rows.stream().map(RkSubjectVo::getGroupId).filter(Objects::nonNull).distinct().toList();
        if (groupIds.isEmpty()) {
            return;
        }
        Map<Long, RkSubjectGroup> groups = groupMapper.selectBatchIds(groupIds)
            .stream().collect(Collectors.toMap(RkSubjectGroup::getGroupId, item -> item));
        rows.forEach(row -> {
            RkSubjectGroup group = groups.get(row.getGroupId());
            if (group != null) {
                row.setGroupName(group.getGroupName());
                row.setGroupCode(group.getGroupCode());
            }
        });
    }

    @Override
    public TableDataInfo<RkSubjectVo> queryPageList(RkSubjectBo bo, PageQuery pageQuery) {
        Page<RkSubjectVo> page = baseMapper.selectVoPage(pageQuery.build(), buildWrapper(bo));
        fillGroupInfo(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public List<RkSubjectVo> queryList(RkSubjectBo bo) {
        List<RkSubjectVo> rows = baseMapper.selectVoList(buildWrapper(bo));
        fillGroupInfo(rows);
        return rows;
    }

    @Override
    public RkSubjectVo queryById(Long subjectId) {
        RkSubjectVo row = baseMapper.selectVoById(subjectId);
        if (row != null) {
            fillGroupInfo(List.of(row));
        }
        return row;
    }

    @Override
    public Boolean insertByBo(RkSubjectBo bo) {
        RkSubject add = MapstructUtils.convert(bo, RkSubject.class);
        return baseMapper.insert(add) > 0;
    }

    @Override
    public Boolean updateByBo(RkSubjectBo bo) {
        RkSubject up = MapstructUtils.convert(bo, RkSubject.class);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean changeStatus(Long subjectId, String status) {
        RkSubject up = new RkSubject();
        up.setSubjectId(subjectId);
        up.setStatus(status);
        return baseMapper.updateById(up) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids) {
        return baseMapper.deleteByIds(ids) > 0;
    }
}
