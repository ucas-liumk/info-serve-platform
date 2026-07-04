package org.dromara.portal.kernel.service;

import org.dromara.portal.kernel.domain.PortalModule;

import java.util.List;

public interface IPortalModuleService {

    /** 管理端：全部模块 */
    List<PortalModule> listAll();

    /** 门户端：当前用户可见模块（启用+敬请期待，按权限过滤，隐藏不返回） */
    List<PortalModule> listVisible();

    Boolean insert(PortalModule module);

    Boolean update(PortalModule module);

    Boolean deleteById(Long moduleId);
}
