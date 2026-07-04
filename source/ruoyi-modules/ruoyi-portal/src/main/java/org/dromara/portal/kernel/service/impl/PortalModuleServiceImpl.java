package org.dromara.portal.kernel.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.portal.kernel.domain.PortalModule;
import org.dromara.portal.kernel.mapper.PortalModuleMapper;
import org.dromara.portal.kernel.service.IPortalModuleService;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class PortalModuleServiceImpl implements IPortalModuleService {

    private static final String STATUS_HIDDEN = "2";

    private final PortalModuleMapper moduleMapper;

    @Override
    public List<PortalModule> listAll() {
        return moduleMapper.selectList(Wrappers.<PortalModule>lambdaQuery()
            .orderByAsc(PortalModule::getSortOrder));
    }

    @Override
    public List<PortalModule> listVisible() {
        return moduleMapper.selectList(Wrappers.<PortalModule>lambdaQuery()
                .ne(PortalModule::getStatus, STATUS_HIDDEN)
                .orderByAsc(PortalModule::getSortOrder))
            .stream()
            .filter(m -> StringUtils.isBlank(m.getPerms()) || StpUtil.hasPermission(m.getPerms()))
            .toList();
    }

    @Override
    public Boolean insert(PortalModule module) {
        validate(module);
        return moduleMapper.insert(module) > 0;
    }

    @Override
    public Boolean update(PortalModule module) {
        if (module.getModuleId() == null) {
            throw new ServiceException("模块ID不能为空");
        }
        validate(module);
        return moduleMapper.updateById(module) > 0;
    }

    @Override
    public Boolean deleteById(Long moduleId) {
        return moduleMapper.deleteById(moduleId) > 0;
    }

    private void validate(PortalModule module) {
        if (module == null || StringUtils.isBlank(module.getModuleCode()) || StringUtils.isBlank(module.getModuleName())) {
            throw new ServiceException("模块编码与名称不能为空");
        }
        Long conflict = moduleMapper.selectCount(Wrappers.<PortalModule>lambdaQuery()
            .eq(PortalModule::getModuleCode, module.getModuleCode())
            .ne(module.getModuleId() != null, PortalModule::getModuleId, module.getModuleId()));
        if (conflict != null && conflict > 0) {
            throw new ServiceException("模块编码已存在：" + module.getModuleCode());
        }
    }
}
