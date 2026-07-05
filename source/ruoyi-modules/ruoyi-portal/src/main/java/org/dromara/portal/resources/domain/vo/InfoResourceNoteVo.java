package org.dromara.portal.resources.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.portal.resources.domain.InfoResourceNote;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@AutoMapper(target = InfoResourceNote.class)
public class InfoResourceNoteVo implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private Long noteId;
    private Long resourceId;
    private Long userId;
    private String authorName;
    private String content;
    private String visibility;
    private Boolean mine;
    private Date createTime;
    private Date updateTime;
}
