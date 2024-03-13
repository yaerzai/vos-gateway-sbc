package com.ytl.vos.gateway.sbc.task;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.ytl.common.redis.dto.LocalCacheDTO;
import com.ytl.common.redis.service.LocalCacheService;
import com.ytl.vos.persistence.dataservice.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

@Component
@Slf4j
public class LocalCacheTask {

    //本地缓存服务
    @Resource
    private LocalCacheService localCacheService;
    //号段
    @Resource
    private SysMobileSegmentDataService sysMobileSegmentDataService;
    //携号转网
    @Resource
    private SysMobileTransferDataService sysMobileTransferDataService;
    @Resource
    private NumberCacheService numberCacheService;
    @Resource
    private PriNumberCacheService priNumberCacheService;
    @Resource
    private CustPriNumPoolCacheService custPriNumPoolCacheService;
    @Resource
    private ChnPubNumPoolCacheService chnPubNumPoolCacheService;
    @Resource
    private ProvincePubNumPoolCacheService provincePubNumPoolCacheService;
    @Resource
    private CityPubNumPoolCacheService cityPubNumPoolCacheService;


    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${server.nodeId}")
    private String nodeId;

    /**
     * 每日5点自动刷新本地缓存
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void refreshTask() {
        refreshLocalCache();
    }

    @PostConstruct
    public void init() {
        //刷新
        refreshLocalCache();

        String serverInfo = StrUtil.format("{}-{}", applicationName, nodeId);
        localCacheService.clearQueue(serverInfo);

        log.warn("开启自动刷新缓存: {}", serverInfo);
        ThreadUtil.execute(()-> localCacheService.syncLocalCache(serverInfo, this::syncLocalCache));
    }

    private void refreshLocalCache() {
        log.warn("开始刷新号段本地缓存");
        sysMobileSegmentDataService.refreshLocalCache();

        log.warn("开始刷新携号转网本地缓存");
        sysMobileTransferDataService.refreshLocalCache();

        log.warn("开始刷新号码缓存");
        numberCacheService.refreshLocalCache();

        log.warn("开始刷新私号缓存");
        priNumberCacheService.refreshLocalCache();

        log.warn("开始刷新客户私有号码池");
        custPriNumPoolCacheService.refreshLocalCache();

        log.warn("开始刷新通道公共号码池");
        chnPubNumPoolCacheService.refreshLocalCache();

        log.warn("开始刷新省公共号码池");
        provincePubNumPoolCacheService.refreshLocalCache();

        log.warn("开始刷新市公共号码池");
        cityPubNumPoolCacheService.refreshLocalCache();
    }

    private void syncLocalCache(LocalCacheDTO localCacheDTO) {
        if (localCacheDTO == null) {
            return;
        }
        if (localCacheDTO.isAllRefresh()) {
            switch (localCacheDTO.getLocalCacheEnum()) {
                case NumberInfo:
                    numberCacheService.refreshLocalCache();
                    break;
                case PriNumber:
                    priNumberCacheService.refreshLocalCache();
                    break;
                case CustPriNumPool:
                    custPriNumPoolCacheService.refreshLocalCache();
                    break;
                case ChnPubNumPool:
                    chnPubNumPoolCacheService.refreshLocalCache();
                    break;
                case ProvincePubNumPool:
                    provincePubNumPoolCacheService.refreshLocalCache();
                    break;
                case CityPubNumPool:
                    cityPubNumPoolCacheService.refreshLocalCache();
                    break;
                case MobileSegment:
                    sysMobileSegmentDataService.refreshLocalCache();
                    break;
                case MobileTransfer:
                    sysMobileTransferDataService.refreshLocalCache();
                    break;
            }
        } else {
            String cacheKey = localCacheDTO.getCacheKey();
            switch (localCacheDTO.getLocalCacheEnum()) {
                case NumberInfo:
                    numberCacheService.setLocalCache(cacheKey);
                    break;
                case PriNumber:
                    priNumberCacheService.setLocalCache(cacheKey);
                    break;
                case CustPriNumPool:
                    custPriNumPoolCacheService.setLocalCache(cacheKey);
                    break;
                case ChnPubNumPool:
                    chnPubNumPoolCacheService.setLocalCache(cacheKey);
                    break;
                case ProvincePubNumPool:
                    provincePubNumPoolCacheService.setLocalCache(cacheKey);
                    break;
                case CityPubNumPool:
                    cityPubNumPoolCacheService.setLocalCache(cacheKey);
                    break;
                case MobileSegment:
                    sysMobileSegmentDataService.setLocalCache(cacheKey);
                    break;
                case MobileTransfer:
                    sysMobileTransferDataService.setLocalCache(cacheKey);
                    break;
            }
        }
    }
}
