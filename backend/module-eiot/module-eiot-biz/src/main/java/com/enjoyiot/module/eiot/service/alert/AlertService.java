
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
package com.enjoyiot.module.eiot.service.alert;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.alert.dto.AlertRecord;
import com.enjoyiot.module.eiot.controller.admin.alertconfig.vo.AlertRecordPageReq;

import java.util.List;

public interface AlertService {

    PageResult<AlertRecord> selectAlertRecordPage(AlertRecordPageReq request);

    /**
     * 确认处理告警记录（标记已读）
     */
    void confirmAlertRecord(Long id);

    /**
     * 查询近 N 分钟内未处置的告警
     */
    List<AlertRecord> getRecentUnprocessedAlerts(int minutes);

    /**
     * 稍后处置告警（状态设为 1）
     */
    void deferAlertRecord(Long id);

    /**
     * 去处置告警（状态设为 2=处置中）
     */
    void handleAlertRecord(Long id);

    /**
     * 查询指定父设备（含其子设备）近 N 分钟内未处置的告警
     *
     * @param parentDeviceId 父设备ID
     * @param minutes 时间范围（分钟）
     * @return 未处置告警列表
     */
    List<AlertRecord> getUnprocessedAlertsByParentDeviceId(Long parentDeviceId, int minutes);

    /**
     * 根据报警设备ID查找对应的用传设备ID
     * 如果是用传设备直接返回，否则递归查找父设备
     *
     * @return 用传设备ID，找不到时返回原始设备ID
     */
    Long findTransferDeviceId(Long alertDeviceId);

}
