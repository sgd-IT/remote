package com.enjoyiot.module.eiot.service.device;

import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendReqVO;
import com.enjoyiot.module.eiot.controller.admin.device.vo.remote.InstructionSendRespVO;

public interface DeviceInstructionService {

    InstructionSendRespVO sendInstruction(InstructionSendReqVO reqVO);
}
