package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.framework.tenant.core.util.TenantUtils;
import com.enjoyiot.module.eiot.dal.dataobject.video.VideoCameraDO;
import com.enjoyiot.module.eiot.dal.mysql.video.VideoCameraMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class VideoCameraServiceImpl implements VideoCameraService {

    @Resource
    private VideoCameraMapper videoCameraMapper;

    @Override
    public Long create(VideoCameraDO camera) {
        videoCameraMapper.insert(camera);
        return camera.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchCreate(Long tenantId, List<VideoCameraDO> cameras) {
        int successCount = 0;
        int skippedCount = 0;
        for (VideoCameraDO camera : cameras) {
            // 忽略租户上下文跨租户查询，按 tenantId + deviceId + channelId 查重，已存在则跳过
            Long exists = TenantUtils.executeIgnoreWithResult(() -> videoCameraMapper.selectCount(
                    new LambdaQueryWrapperX<VideoCameraDO>()
                            .eq(VideoCameraDO::getTenantId, tenantId)
                            .eq(VideoCameraDO::getDeviceId, camera.getDeviceId())
                            .eq(VideoCameraDO::getChannelId, camera.getChannelId())));
            if (exists != null && exists > 0) {
                skippedCount++;
                continue;
            }
            // 手动指定归属租户，忽略租户拦截器自动填充当前租户
            camera.setTenantId(tenantId);
            TenantUtils.executeIgnore(() -> videoCameraMapper.insert(camera));
            successCount++;
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("skippedCount", skippedCount);
        return result;
    }

    @Override
    public void update(VideoCameraDO camera) {
        videoCameraMapper.updateById(camera);
    }

    @Override
    public void delete(Long id) {
        videoCameraMapper.deleteById(id);
    }

    @Override
    public VideoCameraDO getById(Long id) {
        return videoCameraMapper.selectById(id);
    }

    @Override
    public List<VideoCameraDO> getList() {
        LambdaQueryWrapperX<VideoCameraDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.orderByDesc(VideoCameraDO::getCreateTime);
        return videoCameraMapper.selectList(wrapper);
    }

    @Override
    public PageResult<VideoCameraDO> getPage(int pageNo, int pageSize, String keyword) {
        LambdaQueryWrapperX<VideoCameraDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.likeIfPresent(VideoCameraDO::getChannelName, keyword)
                .orderByDesc(VideoCameraDO::getCreateTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<VideoCameraDO> mpPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNo, pageSize);
        videoCameraMapper.selectPage(mpPage, wrapper);
        return new PageResult<>(mpPage.getRecords(), mpPage.getTotal());
    }
}
