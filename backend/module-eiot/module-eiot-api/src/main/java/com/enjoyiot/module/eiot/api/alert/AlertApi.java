
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
package com.enjoyiot.module.eiot.api.alert;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfig;
import com.enjoyiot.module.eiot.api.alert.dto.AlertConfigPageReqVO;
import com.enjoyiot.module.eiot.api.alert.dto.Message;

import java.util.List;

public interface AlertApi {

    PageResult<AlertConfig> getAlertConfigPage(AlertConfigPageReqVO reqVO);

    Message getNotifyMessage(AlertConfig alertConfig);

    /**
     * 根据租户ID获取企业联系人手机号
     * 用于自动发送短信时，从企业信息中获取联系人电话
     *
     * @param tenantId 租户ID
     * @return 联系人手机号（多个用逗号分隔），不存在返回 null
     */
    String getPropertyContactPhone(Long tenantId);

    /**
     * 获取报警配置对应的消息推送手机号列表（已去重）
     *
     * 推送接收人规则：
     * 1. 读取报警配置绑定的【消息推送岗位】（未配置时跳过）
     * 2. 筛选同一租户下归属上述岗位、且开启【消息推送】的用户
     * 3. 追加当前租户下当前时段生效的值班人员
     *
     * @param alertConfigId 报警配置ID
     * @return 手机号列表，无有效接收人时返回空列表
     */
    List<String> getAlertPushPhones(Long alertConfigId);


}
