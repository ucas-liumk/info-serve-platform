package org.dromara.portal.appcenter.service;

import org.dromara.portal.appcenter.domain.bo.AppDemandBo;
import org.dromara.portal.appcenter.domain.bo.AppDemandSubmitBo;
import org.dromara.portal.appcenter.domain.vo.AppDemandVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

import java.util.Collection;
import java.util.List;

public interface IAppDemandService {
    TableDataInfo<AppDemandVo> queryPageList(AppDemandBo bo, PageQuery pageQuery);
    AppDemandVo queryById(Long demandId);
    TableDataInfo<AppDemandVo> queryMyPageList(PageQuery pageQuery);
    Boolean submitFromPortal(AppDemandSubmitBo bo);
    Boolean deleteOwnById(Long demandId);
    Boolean updateHandle(AppDemandBo bo);
    Boolean deleteWithValidByIds(Collection<Long> ids);

    /** 需求反馈状态统计（应用态势聚合用） */
    List<DemandStatusStat> listStatusStats();

    /** 需求反馈类型统计（应用态势聚合用） */
    List<DemandTypeStat> listTypeStats();

    /** 待处理需求反馈数（应用态势聚合用） */
    Long countPending();

    /** 需求反馈提交用户（应用态势聚合用） */
    List<Long> listRequesterIds();

    /** 按创建部门聚合的反馈量（应用态势聚合用） */
    List<DeptDemandStat> listDeptDemandStats();

    record DemandStatusStat(String status, Long value) {
    }

    record DemandTypeStat(String demandType, Long value) {
    }

    record DeptDemandStat(Long deptId, Long value) {
    }
}
