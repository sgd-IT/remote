package com.enjoyiot.module.eiot.api;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Entity基类
 *
 * @author Lion Li
 */

@Data
public class BaseModel implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 最后更新时间
     */
    private LocalDateTime updateTime;
    /**
     * 创建者，目前使用 SysUser 的 id 编号
     *
     * 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。
     */
    private String creator;
    /**
     * 更新者，目前使用 SysUser 的 id 编号
     *
     * 使用 String 类型的原因是，未来可能会存在非数值的情况，留好拓展性。
     */
    private String updater;
    /**
     * 是否删除
     */
    private Boolean deleted;


}
