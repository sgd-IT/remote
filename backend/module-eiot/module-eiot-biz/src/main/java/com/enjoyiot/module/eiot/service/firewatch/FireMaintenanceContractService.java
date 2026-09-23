package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.contract.ContractUpdateReqVO;

import java.util.List;

/**
 * 维保合同管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireMaintenanceContractService {

    /**
     * 创建维保合同
     *
     * @param reqVO 创建信息
     * @return 合同ID
     */
    Long createContract(ContractCreateReqVO reqVO);

    /**
     * 更新维保合同
     *
     * @param reqVO 更新信息
     */
    void updateContract(ContractUpdateReqVO reqVO);

    /**
     * 删除维保合同
     *
     * @param id 合同ID
     */
    void deleteContract(Long id);

    /**
     * 获取维保合同详情
     *
     * @param id 合同ID
     * @return 合同详情
     */
    ContractRespVO getContract(Long id);

    /**
     * 分页查询维保合同
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<ContractRespVO> pageContracts(ContractPageReqVO pageReqVO);

    /**
     * 查询30天内即将到期的合同
     *
     * @return 即将到期的合同列表
     */
    List<ContractRespVO> getExpiringContracts();

    /**
     * 统计30天内即将到期的合同数
     *
     * @return 即将到期的合同数
     */
    Integer countExpiring();
}
