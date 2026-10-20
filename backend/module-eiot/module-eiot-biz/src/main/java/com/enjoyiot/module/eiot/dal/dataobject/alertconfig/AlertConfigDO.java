
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
package com.enjoyiot.module.eiot.dal.dataobject.alertconfig;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

import java.util.List;

/**
 * 报警配置 DO
 *
 * @author EnjoyIot
 */
@TableName(value = "eiot_alert_config", autoResultMap = true)
@KeySequence("eiot_alert_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertConfigDO extends TenantBaseDO {

    /**
     * 告警配置id
     */
    @TableId
    private Long id;
    /**
     * 告警名称
     */
    private String name;
    /**
     * 关联消息转发模板ID
     */
    private Long messageTemplateId;
    /**
     * 规则引擎id（未选择算法报警类型时必填）
     */
    private Long ruleInfoId;
    /**
     * 算法报警类型：1=高温事件报警 2=热成像事件报警 3=烟火事件报警
     * 4=离岗事件报警 5=消防通道占用事件报警；NULL=通用规则配置
     */
    private Integer algorithmAlertType;
    /**
     * 告警等级
     */
    private String level;
    /**
     * 备注
     */
    private String remark;
    /**
     * 状态(0启动 1禁用)
     */
    private Integer status;

    /**
     * 消息推送岗位ID数组（最多3个）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> pushPostIds;

}
