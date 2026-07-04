package org.dromara.portal.kernel.mapper;

import org.apache.ibatis.annotations.Select;
import org.dromara.portal.kernel.domain.AppMessage;
import org.dromara.portal.kernel.domain.vo.AppMessageVo;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

public interface AppMessageMapper extends BaseMapperPlus<AppMessage, AppMessageVo> {

    /**
     * 门户广播收件人 = 全部有效系统用户。
     * TODO(3c): 门户身份模型显式化后改经 RemoteUserService 获取，消除对 sys_user 的直接读。
     */
    @Select("select user_id from sys_user where del_flag = '0' and status = '0'")
    List<Long> selectActiveUserIds();
}
