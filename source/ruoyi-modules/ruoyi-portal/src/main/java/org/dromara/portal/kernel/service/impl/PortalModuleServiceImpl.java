package org.dromara.portal.kernel.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.portal.kernel.domain.PortalModule;
import org.dromara.portal.kernel.domain.PortalUserModulePreference;
import org.dromara.portal.kernel.mapper.PortalModuleMapper;
import org.dromara.portal.kernel.mapper.PortalUserModulePreferenceMapper;
import org.dromara.portal.kernel.service.IPortalModuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class PortalModuleServiceImpl implements IPortalModuleService {

    private static final String STATUS_HIDDEN = "2";

    private final PortalModuleMapper moduleMapper;
    private final PortalUserModulePreferenceMapper preferenceMapper;

    @Override
    public List<PortalModule> listAll() {
        return moduleMapper.selectList(Wrappers.<PortalModule>lambdaQuery()
            .orderByAsc(PortalModule::getSortOrder));
    }

    @Override
    public List<PortalModule> listVisible() {
        return applyUserOrder(listVisibleByRegistryOrder());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisibleOrder(List<String> moduleCodes) {
        Long userId = LoginHelper.getUserId();
        if (userId == null) {
            throw new ServiceException("用户未登录");
        }

        List<PortalModule> visibleModules = listVisibleByRegistryOrder();
        Map<String, PortalModule> visibleModuleMap = visibleModules.stream()
            .collect(Collectors.toMap(PortalModule::getModuleCode, Function.identity(), (left, right) -> left));
        Set<String> normalizedCodes = new LinkedHashSet<>();
        if (moduleCodes != null) {
            moduleCodes.stream()
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .filter(visibleModuleMap::containsKey)
                .forEach(normalizedCodes::add);
        }
        visibleModules.stream()
            .map(PortalModule::getModuleCode)
            .filter(code -> !normalizedCodes.contains(code))
            .forEach(normalizedCodes::add);
        if (normalizedCodes.isEmpty()) {
            throw new ServiceException("没有可保存的模块");
        }

        preferenceMapper.delete(Wrappers.<PortalUserModulePreference>lambdaQuery()
            .eq(PortalUserModulePreference::getUserId, userId));

        Date now = new Date();
        int index = 1;
        for (String moduleCode : normalizedCodes) {
            PortalUserModulePreference preference = new PortalUserModulePreference();
            preference.setUserId(userId);
            preference.setTenantId(LoginHelper.getTenantId());
            preference.setModuleCode(moduleCode);
            preference.setSortOrder(index++);
            preference.setCreateTime(now);
            preference.setUpdateTime(now);
            preferenceMapper.insert(preference);
        }
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

    private List<PortalModule> listVisibleByRegistryOrder() {
        return moduleMapper.selectList(Wrappers.<PortalModule>lambdaQuery()
                .ne(PortalModule::getStatus, STATUS_HIDDEN)
                .orderByAsc(PortalModule::getSortOrder))
            .stream()
            .filter(m -> StringUtils.isBlank(m.getPerms()) || StpUtil.hasPermission(m.getPerms()))
            .toList();
    }

    private List<PortalModule> applyUserOrder(List<PortalModule> modules) {
        Long userId = LoginHelper.getUserId();
        if (userId == null || modules.isEmpty()) {
            return modules;
        }

        Map<String, Integer> orderMap = preferenceMapper.selectList(Wrappers.<PortalUserModulePreference>lambdaQuery()
                .eq(PortalUserModulePreference::getUserId, userId)
                .orderByAsc(PortalUserModulePreference::getSortOrder))
            .stream()
            .collect(Collectors.toMap(
                PortalUserModulePreference::getModuleCode,
                PortalUserModulePreference::getSortOrder,
                Math::min
            ));

        if (orderMap.isEmpty()) {
            return modules;
        }

        Comparator<PortalModule> comparator = Comparator
            .comparing((PortalModule module) -> orderMap.containsKey(module.getModuleCode()) ? 0 : 1)
            .thenComparing(module -> orderMap.getOrDefault(module.getModuleCode(), Integer.MAX_VALUE))
            .thenComparing(module -> module.getSortOrder() == null ? Integer.MAX_VALUE : module.getSortOrder())
            .thenComparing(module -> module.getModuleId() == null ? Long.MAX_VALUE : module.getModuleId());
        return modules.stream().sorted(comparator).toList();
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
