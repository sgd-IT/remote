package com.enjoyiot.module.eiot.dal.mysql.firewatch;

import com.enjoyiot.framework.mybatis.core.mapper.BaseMapperX;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenanceContractDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 维保合同 Mapper
 *
 * @author EnjoyIot
 */
@Mapper
public interface FireMaintenanceContractMapper extends BaseMapperX<FireMaintenanceContractDO> {

    /**
     * 查询30天内即将到期的合同
     */
    @Select("SELECT * FROM fire_maintenance_contract WHERE tenant_id = #{tenantId} " +
            "AND contract_status = 'valid' AND end_date IS NOT NULL " +
            "AND end_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY) AND deleted = 0")
    List<FireMaintenanceContractDO> selectExpiringContracts(@Param("tenantId") Long tenantId);

    /**
     * 查询30天内即将到期的合同（不按租户过滤，用于管理员）
     */
    @Select("SELECT * FROM fire_maintenance_contract WHERE contract_status = 'valid' AND end_date IS NOT NULL " +
            "AND end_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY) AND deleted = 0")
    List<FireMaintenanceContractDO> selectAllExpiringContracts();

    /**
     * 统计30天内即将到期的合同数
     */
    @Select("SELECT COUNT(*) FROM fire_maintenance_contract WHERE tenant_id = #{tenantId} " +
            "AND contract_status = 'valid' AND end_date IS NOT NULL " +
            "AND end_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY) AND deleted = 0")
    Integer countExpiringContracts(@Param("tenantId") Long tenantId);

    /**
     * 统计30天内即将到期的合同总数（不按租户过滤，用于管理员）
     */
    @Select("SELECT COUNT(*) FROM fire_maintenance_contract WHERE contract_status = 'valid' AND end_date IS NOT NULL " +
            "AND end_date BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY) AND deleted = 0")
    Integer countAllExpiringContracts();
}
