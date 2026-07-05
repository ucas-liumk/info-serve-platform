package org.dromara.portal.resources.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.resources.domain.InfoResourceViewRecord;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InfoResourceViewRecord.class)
public class InfoResourceViewRecordVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private Long recordId;
    private Long resourceId;
    private Long userId;
    private String userName;
    private String actionType;
    private Date createTime;
}
