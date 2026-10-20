
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
package com.enjoyiot.module.eiot.api.enums;

import com.enjoyiot.framework.common.exception.ErrorCode;

public interface ErrorCodeConstants {
    // ========== 产品分类 2_006_000_000 补充编号 ==========
    ErrorCode CATEGORY_NOT_EXISTS = new ErrorCode(2_006_000_000, "产品分类不存在");
    ErrorCode CATEGORY_EXITS_CHILDREN = new ErrorCode(2_006_000_000, "存在下级产品分类，无法删除");
    ErrorCode CATEGORY_PARENT_NOT_EXITS = new ErrorCode(2_006_000_000, "父级产品分类不存在");
    ErrorCode CATEGORY_PARENT_ERROR = new ErrorCode(2_006_000_000, "不能设置自己为父产品分类");
    ErrorCode CATEGORY_NAME_DUPLICATE = new ErrorCode(2_006_000_000, "已经存在该分类名称的产品分类");
    ErrorCode CATEGORY_PARENT_IS_CHILD = new ErrorCode(2_006_000_000, "不能设置自己的子Category为父Category");
    ErrorCode CATEGORY_EXITS_PRODUCT = new ErrorCode(2_006_000_000, "分类下存在产品");

    // ========== 产品信息 2_006_001_000 ==========

    ErrorCode PRODUCT_NOT_EXISTS = new ErrorCode(2_006_001_000, "物联网产品不存在");
    ErrorCode PRODUCT_KEY_REPEAT = new ErrorCode(2_006_001_000, "productKey重复");
    ErrorCode PRODUCT_DEVICE_EXISTS = new ErrorCode(2_006_001_000, "产品下存在设备");;

    // ========== 设备信息 2_006_002_000 ==========
    ErrorCode DEVICE_INFO_NOT_EXISTS = new ErrorCode(2_006_002_000, "设备信息不存在");
    ErrorCode DEVICE_DN_REPEAT = new ErrorCode(2_006_002_000, "设备DN重复");
    ErrorCode DEVICE_SERIAL_REPEAT = new ErrorCode(2_006_002_000, "设备序列号重复");

    // ========== 产品物模型 2_006_003_000 ==========
    ErrorCode NOT_EXISTS = new ErrorCode(2_006_003_000, "信息不存在");
    // ========== 规则引擎 2_006_004_000 ==========

    ErrorCode RULE_INFO_NOT_EXISTS = new ErrorCode(2_006_004_000, "规则引擎不存在");
    // ========== 通道配置 2_006_004_000 ==========
    ErrorCode CHANNEL_CONFIG_NOT_EXISTS = new ErrorCode(2_006_004_000, "规则引擎不存在");
    ErrorCode CHANNEL_CONFIG_USED = new ErrorCode(2_006_004_000, "通道配置已被使用");
    ErrorCode CHANNEL_TEMPLATE_USED = new ErrorCode(2_006_004_000, "通道模板已被使用");


    ErrorCode FILE_NOT_NULL = new ErrorCode(2_006_004_000, "文件不许为空");
    ErrorCode DATA_NOT_EXIST = new ErrorCode(2_006_004_000, "数据不存在");

    ErrorCode TEMPLATE_NAME_ALREADY = new ErrorCode(2_006_004_000, "模板名称已存在");
    ErrorCode RELATED_PRODUCTS_EXIST = new ErrorCode(2_006_004_000, "关联产品已存在");
    
    // ========== SMS模板相关 2_006_005_000 ==========
    ErrorCode CHANNEL_CONFIG_PARAM_ERROR = new ErrorCode(2_006_005_000, "通道配置参数错误");
    ErrorCode SMS_TEMPLATE_CREATE_FAILED = new ErrorCode(2_006_005_001, "短信模板创建失败");
    ErrorCode SMS_TEMPLATE_UPDATE_FAILED = new ErrorCode(2_006_005_002, "短信模板更新失败");
    ErrorCode SMS_TEMPLATE_DELETE_FAILED = new ErrorCode(2_006_005_003, "短信模板删除失败");
    ErrorCode SMS_TEMPLATE_UPDATE_ERROR_IN_AUDIT = new ErrorCode(2_006_005_004, "待审核状态无法更新模板内容");
    ErrorCode SMS_TEMPLATE_DELETE_ERROR_IN_AUDIT = new ErrorCode(2_006_005_004, "待审核状态无法删除");
    ErrorCode SMS_SEND_FAILED = new ErrorCode(2_006_005_005, "短信发送失败");

    // ========== 摄像头报警 2_006_006_000 ==========
    ErrorCode CAMERA_DEVICE_NOT_EXISTS = new ErrorCode(2_006_006_000, "摄像头设备不存在");
    ErrorCode CAMERA_ALERT_ALREADY_ACTIVE = new ErrorCode(2_006_006_001, "该报警已处于活跃状态");

    // ========== 告警配置 2_006_008_000 ==========
    ErrorCode ALERT_CONFIG_ALGORITHM_TYPE_INVALID = new ErrorCode(2_006_008_000, "算法报警类型不合法（可选值：1高温 2热成像 3烟火 4离岗 5消防通道占用）");
    ErrorCode ALERT_CONFIG_RULE_REQUIRED = new ErrorCode(2_006_008_001, "未选择算法报警类型时，规则不能为空");

    // ========== 消防值守-座机号码 2_006_007_000 ==========
    ErrorCode DUTY_PHONE_NOT_SYSTEM_TENANT = new ErrorCode(2_006_007_000, "仅值守平台（系统租户）可维护座机号码");
    ErrorCode DUTY_PHONE_EXISTS = new ErrorCode(2_006_007_001, "该租户下座机号码已存在");
    ErrorCode DUTY_PHONE_NOT_EXISTS = new ErrorCode(2_006_007_002, "座机号码不存在");

    // ========== 消防值守-通话记录 2_006_007_100 ==========
    ErrorCode CALL_RECORD_NOT_EXISTS = new ErrorCode(2_006_007_100, "通话记录不存在");
    ErrorCode CALL_RECORD_NO_VOICE = new ErrorCode(2_006_007_101, "该通话无对应录音文件");
    ErrorCode CALL_RECORD_VOICE_FETCH_FAILED = new ErrorCode(2_006_007_102, "录音文件拉取失败，请稍后重试");
    ErrorCode CALL_PUSH_SESSION_ID_EMPTY = new ErrorCode(2_006_007_103, "话单推送缺少必填参数 sessionid");

    // ========== 消防值守-空间管理 2_006_007_200 ==========
    ErrorCode SPACE_EXISTS = new ErrorCode(2_006_007_200, "该租户下相同楼栋/楼层/区域/详细位置的空间已存在");
    ErrorCode SPACE_NOT_EXISTS = new ErrorCode(2_006_007_201, "空间不存在");
    ErrorCode SPACE_USED_BY_DEVICE = new ErrorCode(2_006_007_202, "该空间下存在设备，无法删除");
}
