package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhonePageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.dutyphone.DutyPhoneUpdateReqVO;

import java.util.List;

/**
 * 值班中心座机号码 Service
 * <p>
 * 仅系统租户可录入/维护；企业租户可查询本租户的座机号码。
 *
 * @author EnjoyIot
 */
public interface FireDutyPhoneService {

    /**
     * 创建座机号码（仅系统租户）
     */
    Long createPhone(DutyPhoneCreateReqVO reqVO);

    /**
     * 更新座机号码（仅系统租户）
     */
    void updatePhone(DutyPhoneUpdateReqVO reqVO);

    /**
     * 删除座机号码（仅系统租户）
     */
    void deletePhone(Long id);

    /**
     * 获取座机号码详情
     */
    DutyPhoneRespVO getPhone(Long id);

    /**
     * 分页查询：系统租户看全部（可按租户筛选），企业租户仅看本租户
     */
    PageResult<DutyPhoneRespVO> pagePhones(DutyPhonePageReqVO pageReqVO);

    /**
     * 列表查询：系统租户看全部，企业租户仅看本租户
     */
    List<DutyPhoneRespVO> listPhones();
}
