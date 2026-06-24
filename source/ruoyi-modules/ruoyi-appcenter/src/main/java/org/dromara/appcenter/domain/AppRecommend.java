package org.dromara.appcenter.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 应用推荐 */
@Data
@TableName("app_recommend")
public class AppRecommend implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "id")
    private Long id;
    private Long appId;
    private Long userId;
    private Date createTime;
}
