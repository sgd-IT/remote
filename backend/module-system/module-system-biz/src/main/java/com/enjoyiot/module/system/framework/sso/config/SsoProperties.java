
/*
 *
 *  * | Licensed 未经许可不能去掉「Enjoy-iot」相关版权
 *  * +----------------------------------------------------------------------
 *  * | Author: xw2sy@163.com | Tel: 19918996474
 *  *
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
package com.enjoyiot.module.system.framework.sso.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

/**
 * SSO 免登录配置项
 * <p>
 * 第三方系统通过 appKey + secret 换取一次性 ticket，ticket 对应到绑定的用户名。
 *
 * @author EnjoyIot
 */
@ConfigurationProperties(prefix = "platform.sso")
@Validated
@Data
public class SsoProperties {

    /**
     * 第三方接入应用列表
     */
    @Valid
    private List<SsoApp> apps = new ArrayList<>();

    /**
     * 第三方接入应用
     */
    @Data
    public static class SsoApp {

        /**
         * 应用标识（第三方系统凭据）
         */
        @NotEmpty(message = "应用标识不能为空")
        private String appKey;

        /**
         * 应用密钥
         */
        @NotEmpty(message = "应用密钥不能为空")
        private String secret;

        /**
         * 绑定的消防系统用户名
         */
        @NotEmpty(message = "绑定用户名不能为空")
        private String username;

    }

}
