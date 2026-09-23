
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
package com.enjoyiot.framework.datapermission.core.rule;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;

import java.util.Set;

/**
 * 数据权限规则接口
 * 通过实现接口，自定义数据规则。例如说，
 *
 * @author EnjoyIot
 */
public interface DataPermissionRule {

    /**
     * 返回需要生效的表名数组
     * 为什么需要该方法？Data Permission 数组基于 SQL 重写，通过 Where 返回只有权限的数据
     *
     * 如果需要基于实体名获得表名，可调用 {@link TableInfoHelper#getTableInfo(Class)} 获得
     *
     * 特殊语义：当返回 {@code null} 时，表示匹配所有表（表级全局过滤规则可用此模式）。
     * 返回 {@code null} 时，可配合 {@link #isMatchTable(String)} 来排除特定表。
     *
     * @return 表名数组，null 表示匹配所有表
     */
    Set<String> getTableNames();

    /**
     * 判断当前表是否匹配本规则
     *
     * 仅当 {@link #getTableNames()} 返回 {@code null}（全表匹配模式）时才会被调用。
     * 默认实现返回 {@code true}（匹配所有表），子类可重写以排除特定表。
     *
     * @param tableName 表名
     * @return 是否匹配
     */
    default boolean isMatchTable(String tableName) {
        return true;
    }

    /**
     * 根据表名和别名，生成对应的 WHERE / OR 过滤条件
     *
     * @param tableName 表名
     * @param tableAlias 别名，可能为空
     * @return 过滤条件 Expression 表达式
     */
    Expression getExpression(String tableName, Alias tableAlias);

}
