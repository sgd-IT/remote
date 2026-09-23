
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
package com.enjoyiot.module.eiot.api.device;

import com.enjoyiot.eiot.common.thing.ThingService;
import com.enjoyiot.framework.common.pojo.CommonResult;
import com.enjoyiot.module.eiot.api.alert.dto.AlertRecord;
import com.enjoyiot.module.eiot.api.device.dto.*;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertConfig;
import com.enjoyiot.module.eiot.api.devicealert.dto.DeviceAlertRecord;

import java.util.List;
import java.util.Map;

public interface DeviceApi {

    DeviceInfo getDeviceByPkDnByCache(String pk, String dn);

    /**
     * 仅按设备名称(dn)查询设备，忽略租户隔离
     * 用于协议组件在设备接入时识别已预注册的设备
     *
     * @param dn 设备名称
     * @return 设备信息
     */
    DeviceInfo getDeviceByDn(String dn);

    /**
     * 根据父设备ID和设备名称(dn)查询子设备，忽略租户隔离
     * 用于协议组件在设备接入时按父设备精准识别子设备
     *
     * @param parentId 父设备ID
     * @param dn 设备名称
     * @return 子设备信息
     */
    DeviceInfo getDeviceByParentIdAndDn(Long parentId, String dn);

    DeviceInfo getDeviceInfoFromCache(Long deviceId);

    // TODO(eiot-udp-device-info) 缺“更新设备信息”入口：registerDevice 对已存在设备直接 return，
    //  所以自动注册过的设备（id=11、112）补不了报文里的 SN/型号/版本。补一个窄口径方法：
    //    void updateDeviceIdentity(RegisterDevice registerDevice);
    //  实现只 set 非空字段（避免把已有值刷空）+ @CacheEvict(iot:device / iot:deviceId)，
    //  否则 Redis 里旧值最长残留 1 小时；调用方条件“只补不刷”（如 name 仍等于 dn），不然设备每小时一包都写库。
    //  不要把 DeviceInfoService 整个注进组件，会把一堆 bean 拖进 iot-component-* 模块。
    DeviceInfo registerDevice(RegisterDevice registerDevice);

    CommonResult<DeviceInfo> auth(DeviceAuth deviceAuth);

    Map<String, DevicePropertyCache> getPropertiesFromCache(Long deviceId);

    void updateDeviceLastTimeCache(Long deviceId, long lastTime);

    Boolean updateDeviceState(Long deviceId, boolean online);

    void savePropertiesCache(Long deviceId, Map<String, DevicePropertyCache> properties);

    void clearPropertiesCache(String productKey);

    DeviceConfig getDeviceConfig(Long deviceId);

    DeviceConfig getDeviceConfig(String productKey, String dn);

    /**
     * 调用设备服务
     *
     * @param service 服务
     */
    void invoke(ThingService<?> service);

    List<DeviceInfo> getSubDevicesByProductKeAndDeviceName(String pk, String dn);

    Boolean deregisterSubDevice(String pk, String dn, String model, String subPkDeregister, String subDnDeregister);

    void addDeviceAlertRecord(DeviceAlertRecord record);

    void recoverDeviceAlertRecord(Long deviceId, String alertName);

    List<DeviceAlertConfig> getDeviceAlertConfigListByDeviceId(Long deviceId);

    List<DeviceAlertConfig> getDeviceAlertConfigListByProductKey(String productKey);
}
