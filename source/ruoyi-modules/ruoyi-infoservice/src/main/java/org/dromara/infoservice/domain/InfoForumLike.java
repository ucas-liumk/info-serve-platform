package org.dromara.infoservice.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("info_forum_like")
public class InfoForumLike implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId(value = "like_id")
    private Long likeId;
    private String targetType;
    private Long targetId;
    private Long userId;
    private Date createTime;
}
