package com.enjoyiot.module.eiot.service.video;

import com.enjoyiot.module.eiot.dal.dataobject.video.IccMediaServerDO;

public interface MediaServerService {

    IccMediaServerDO getCurrentTenantConfig();

    Long createOrUpdate(IccMediaServerDO config);

    void delete(Long id);
}
