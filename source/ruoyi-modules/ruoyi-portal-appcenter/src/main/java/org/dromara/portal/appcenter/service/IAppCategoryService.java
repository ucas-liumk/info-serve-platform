package org.dromara.portal.appcenter.service;

import org.dromara.portal.appcenter.domain.bo.AppCategoryBo;
import org.dromara.portal.appcenter.domain.vo.AppCategoryVo;
import java.util.Collection;
import java.util.List;

public interface IAppCategoryService {
    List<AppCategoryVo> queryList();
    AppCategoryVo queryById(Long categoryId);
    Boolean insertByBo(AppCategoryBo bo);
    Boolean updateByBo(AppCategoryBo bo);
    Boolean deleteByIds(Collection<Long> ids);
}
