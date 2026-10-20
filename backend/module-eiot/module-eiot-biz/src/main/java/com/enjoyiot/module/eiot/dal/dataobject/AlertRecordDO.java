
/*
 *
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
 *  * +----------------------------------------------------------------------
 *
 *  Copyright [2025] [Enjoy-iot] | Tel: 19918996474
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */
package com.enjoyiot.module.eiot.dal.dataobject;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 告警记录 DO
 *
 * @author EnjoyIot
 */
@TableName("eiot_alert_record")
@KeySequence("eiot_alert_record_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRecordDO extends TenantBaseDO {

    /**
     * id
     */
    @TableId
    private Long id;
    /**
     * 告警时间
     */
    private Long alertTime;
    /**
     * 告警详情
     */
    private String details;
    /**
     * 报警类型：0=主机报警 1=高温事件报警 2=热成像事件报警 3=烟火事件报警 4=离岗事件报警 5=消防通道占用事件报警
     */
    private Integer alarmType;
    /**
     * 告警等级: 1-3级
     */
    private String level;
    /**
     * 报警类型（新语义）：1=火警 2=故障 3=隐患 4=离线 5=误报 6=信息；NULL=过渡期旧数据（按 level 旧语义解释）
     */
    private Integer alarmKind;
    /**
     * 确认档位（新语义，仅一级）：1=Ⅰ档 2=Ⅱ档 3=Ⅲ档 4=Ⅳ档；NULL=不适用或过渡期旧数据
     */
    private Integer tier;
    /**
     * 告警名称
     */
    private String name;
    /**
     * 是否已读
     */
    private Boolean readFlg;
    /**
     * 机构id
     */
    private Long deptId;

    private Long deviceId;

    private Long subDeviceId;

    private String productKey;

    /**
     * 报警图片URL
     */
    private String alertImage;

    /**
     * 处置状态：0=待处置, 1=稍后处置, 2=处置中, 3=已处置
     * 兼容旧数据：readFlg=true 等价于 handleStatus>=2
     */
    private Integer handleStatus;

    /**
     * 设备名称（非表字段，大屏统计查询 JOIN eiot_device_info 时填充）
     */
    @TableField(exist = false)
    private String deviceName;

    /**
     * 处置状态：pending/confirmed/handled/closed（对应 order_status 列）
     */
    private String orderStatus;

}
