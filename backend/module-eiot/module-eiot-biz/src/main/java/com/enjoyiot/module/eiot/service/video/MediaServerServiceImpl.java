package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.enjoyiot.module.eiot.dal.dataobject.video.IccMediaServerDO;
import com.enjoyiot.module.eiot.dal.mysql.video.IccMediaServerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Slf4j
@Service
public class MediaServerServiceImpl implements MediaServerService {

    @Resource
    private IccMediaServerMapper mediaServerMapper;

    @Override
    public IccMediaServerDO getCurrentTenantConfig() {
        LambdaQueryWrapperX<IccMediaServerDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(IccMediaServerDO::getEnabled, true);
        wrapper.orderByDesc(IccMediaServerDO::getId);
        return mediaServerMapper.selectOne(wrapper);
    }

    @Override
    public Long createOrUpdate(IccMediaServerDO config) {
        if (config.getId() == null) {
            mediaServerMapper.insert(config);
        } else {
            mediaServerMapper.updateById(config);
        }
        return config.getId();
    }

    @Override
    public void delete(Long id) {
        mediaServerMapper.deleteById(id);
    }
}
