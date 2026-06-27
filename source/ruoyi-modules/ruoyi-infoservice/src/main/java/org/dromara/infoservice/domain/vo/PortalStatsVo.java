package org.dromara.infoservice.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PortalStatsVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    private Long resourceCount;
    private Long toolCount;
    private Long topicCount;
    private Long activeUserCount;
    private Long todayVisitCount;
}
