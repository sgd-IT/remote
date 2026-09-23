
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
package com.enjoyiot.module.eiot.dal.dataobject.deviceinfo;

import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.enjoyiot.framework.tenant.core.db.TenantBaseDO;
import lombok.*;

/**
 * 设备信息 DO
 *
 * @author EnjoyIot
 */
@TableName("eiot_device_info")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EiotDeviceInfoDO extends TenantBaseDO {

    /**
     * 设备id
     */
    @TableId
    private Long id;
    /**
     * 设备唯一标识
     */
    private String dn;
    /**
     * 产品key
     */
    private String productKey;
    /**
     * 机构id
     */
    private Long deptId;
    /**
     * 设备属性
     */
    private String properties;
    /**
     * 别名
     */
    private String name;
    /**
     *(0:否, 1:在线, 3-未激活，4-禁用)设备状态
     */
    private Integer state;
    /**
     * 离线时间
     */
    private Long offlineTime;
    /**
     * 在线时间
     */
    private Long onlineTime;
    /**
     * 设备序列号
     */
    private String serialNo;
    /**
     * 备注
     */
    private String remark;
    /**
     * 经纬度
     */
    private Double lat;
    /**
     * 经纬度
     */
    private Double lon;
    
    private String model;

    /**
     * 品牌代码（四信API用）
     */
    private String brandCode;

    /**
     * API协议版本（四信API用）
     */
    private String apiVersion;

    private Long parentId;

    private String secret;

    private Long tenantId;

    private String addr;

    private String firmVersion;

    private Integer nodeType;

    private Boolean transparent;

    /**
     * 设备类型枚举：transfer(用传设备), camera(摄像头), smoke(烟感),
     * temp_humidity(温湿度传感器), door_magnet(门磁传感器) 等
     */
    private String deviceType;

    /**
     * 是否显示：设备控制室查询时根据该字段过滤，为 false 时不返回
     */
    private Boolean visible;

    /**
     * 线路类型：0=多线, 1=总线（用于用传设备子设备分类）
     */
    private Integer lineType;
}
