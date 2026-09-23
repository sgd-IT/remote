
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
package com.enjoyiot.module.system.api.dept;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.enjoyiot.framework.common.util.collection.CollectionUtils;
import com.enjoyiot.module.system.api.dept.dto.PostRespDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 岗位 API 接口
 *
 * @author EnjoyIot
 */
public interface PostApi {

    /**
     * 校验岗位们是否有效。如下情况，视为无效：
     * 1. 岗位编号不存在
     * 2. 岗位被禁用
     *
     * @param ids 岗位编号数组
     */
    void validPostList(Collection<Long> ids);

    List<PostRespDTO> getPostList(Collection<Long> ids);

    /**
     * 获得展示在远程控制中心且启用的岗位列表，按岗位排序升序
     *
     * @return 岗位列表
     */
    List<PostRespDTO> getConsoleDisplayPostList();

    /**
     * 获得岗位对应的用户编号列表（按关联表插入顺序）
     *
     * @param postIds 岗位编号数组
     * @return 岗位编号 -> 用户编号列表
     */
    Map<Long, List<Long>> getUserIdsByPostIds(Collection<Long> postIds);

    default Map<Long, PostRespDTO> getPostMap(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return MapUtil.empty();
        }

        List<PostRespDTO> list = getPostList(ids);
        return CollectionUtils.convertMap(list, PostRespDTO::getId);
    }

}
