package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;

import java.util.List;
import java.util.Map;

public interface VideoCameraService {

    Long create(VideoCameraDO camera);

    /**
     * 批量创建摄像头（自动按租户 + deviceId + channelId 去重）
     *
     * @param tenantId 目标归属租户编号
     * @param cameras  摄像头列表
     * @return 创建结果：successCount 成功数，skippedCount 重复跳过数
     */
    Map<String, Object> batchCreate(Long tenantId, List<VideoCameraDO> cameras);

    void update(VideoCameraDO camera);

    void delete(Long id);

    VideoCameraDO getById(Long id);

    List<VideoCameraDO> getList();

    PageResult<VideoCameraDO> getPage(int pageNo, int pageSize, String keyword);
}
