package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireSpaceDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 空间管理 Mapper
 *
 * @author sgd
 */
@Mapper
public interface FireSpaceMapper extends BaseMapperX<FireSpaceDO> {

    /**
     * 物理删除空间（唯一索引不含 deleted 列，软删会让同组合无法重新录入）
     */
    @Delete("DELETE FROM eiot_space WHERE id = #{id}")
    int deleteByIdPhysically(@Param("id") Long id);
}
