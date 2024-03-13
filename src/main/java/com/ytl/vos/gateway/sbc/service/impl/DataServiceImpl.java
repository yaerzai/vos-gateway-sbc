package com.ytl.vos.gateway.sbc.service.impl;

import com.ytl.common.base.aop.MonitorSpendTimeAspect;
import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.vos.channel.api.dto.ChannelGroupChannelConfigQueryReqDTO;
import com.ytl.vos.channel.api.dto.ChannelGroupChannelConfigRespDTO;
import com.ytl.vos.channel.api.dto.ChannelInfoDetailReqDTO;
import com.ytl.vos.channel.api.dto.ChannelInfoDetailResDTO;
import com.ytl.vos.channel.api.service.ChannelGroupChannelConfigService;
import com.ytl.vos.channel.api.service.ChannelInfoService;
import com.ytl.vos.customer.api.dto.customer.CustomerBaseInfoGetDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerBaseInfoQueryRespDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoGetDTO;
import com.ytl.vos.customer.api.dto.customer.CustomerUserInfoQueryRespDTO;
import com.ytl.vos.customer.api.service.CustomerBaseInfoService;
import com.ytl.vos.customer.api.service.CustomerUserInfoService;
import com.ytl.vos.gateway.sbc.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.gateway.sbc.service.bo.LocalCacheBO;
import com.ytl.vos.persistence.dataservice.SysParamsDataService;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@Service
@Slf4j
public class DataServiceImpl implements DataService {

    @Resource
    private SysParamsDataService sysParamsDataService;
    @Resource
    private CustomerUserInfoService customerUserInfoService;
    @Resource
    private CustomerBaseInfoService customerBaseInfoService;
    @Resource
    private ChannelGroupChannelConfigService channelGroupChannelConfigService;
    @Resource
    private ChannelInfoService channelInfoService;

    //系统参数获取
    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("SysParam")
    public String getSysParam(SysParamEnum sysParamEnum) {
        String paramName = sysParamEnum.getParamName();
        return (String) getCacheBO("SysParam", paramName,
                ()-> sysParamsDataService.get(paramName, sysParamEnum.getDefaultValue()),
                DateUtils::isSameMinute
                );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("getSysParamInt")
    public int getSysParamInt(SysParamEnum sysParamEnum) {
        String paramValue = getSysParam(sysParamEnum);
        try {
            return Integer.valueOf(paramValue);
        } catch (NumberFormatException e) {
            log.error("系统参数配置错误,非法数字 {}", sysParamEnum);
            throw new BusinessException(VosErrCodeEnum.System_Param_Config_Error, "系统参数配置错误,非法数字");
        }
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("CustomerInfo")
    public CustomerBaseInfoQueryRespDTO getCustomerInfo(String customerNo) {
        CustomerBaseInfoGetDTO custGetDTO = CustomerBaseInfoGetDTO.builder().customerNo(customerNo).build();
        return (CustomerBaseInfoQueryRespDTO) getCacheBO("CustomerInfo", customerNo,
                ()-> customerBaseInfoService.get(custGetDTO).getData(),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("UserInfo")
    public CustomerUserInfoQueryRespDTO getCustomerUserInfo(String userNo) {
        CustomerUserInfoGetDTO userGetDTO = CustomerUserInfoGetDTO.builder().userNo(userNo).build();
        return (CustomerUserInfoQueryRespDTO) getCacheBO("UserInfo", userNo,
                ()-> customerUserInfoService.detail(userGetDTO).getData(),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("MasterUserInfo")
    public CustomerUserInfoQueryRespDTO getCustomerMasterUserInfo(String customerNo) {
        CustomerBaseInfoGetDTO custGetDTO = CustomerBaseInfoGetDTO.builder().customerNo(customerNo).build();
        return (CustomerUserInfoQueryRespDTO) getCacheBO("MasterUserInfo", customerNo,
                ()-> customerUserInfoService.getCustomerMasterUserInfo(custGetDTO).getData(),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("ChannelGroupConfig")
    public List<ChannelGroupChannelConfigRespDTO> getChannelGroupChannelConfig(String groupNo) {
        ChannelGroupChannelConfigQueryReqDTO queryReqDTO = ChannelGroupChannelConfigQueryReqDTO.builder().groupNo(groupNo).build();
        queryReqDTO.setPageSize(Integer.MAX_VALUE);
        return (List<ChannelGroupChannelConfigRespDTO>) getCacheBO("ChannelGroupConfig", groupNo,
                ()-> channelGroupChannelConfigService.query(queryReqDTO).getData().getData(),
                DateUtils::isSameMinute
        );
    }

    @Override
    @MonitorSpendTimeAspect.MonitorSpendTime("ChannelInfo")
    public ChannelInfoDetailResDTO getChannelInfo(String channelNo) {
        ChannelInfoDetailReqDTO reqDTO = ChannelInfoDetailReqDTO.builder().channelNo(channelNo).build();
        return (ChannelInfoDetailResDTO) getCacheBO("ChannelInfo", channelNo,
                ()-> channelInfoService.get(reqDTO).getData(),
                DateUtils::isSameMinute
        );
    }

    /**
     * 通用缓存
     */
    private Map<String, Map<String, LocalCacheBO<Object>>> commonCacheMap = new ConcurrentHashMap<>();

    private Object getCacheBO(String commonCacheKey, String cacheName, Supplier<Object> newCacheBO, BiFunction<Long, Long, Boolean> compare) {
        Map<String, LocalCacheBO<Object>> cacheMap = commonCacheMap.computeIfAbsent(commonCacheKey, key-> new ConcurrentHashMap<>());
        LocalCacheBO<Object> localCacheBO = cacheMap.computeIfAbsent(cacheName, key -> new LocalCacheBO(newCacheBO.get(), new AtomicLong((System.currentTimeMillis()))));
        synchronized (localCacheBO) {
            if (!compare.apply(System.currentTimeMillis(), localCacheBO.getLastLoadTime().get())) {
                localCacheBO.setDataBO(newCacheBO.get());
                localCacheBO.getLastLoadTime().getAndSet(System.currentTimeMillis());
                log.info("[二级缓存] 重载信息 {} {}", commonCacheKey, cacheName);
            }
            return localCacheBO.getDataBO();
        }
    }

    @Scheduled(fixedDelay = 40 * 60 * 1000)
    public void clearLocalCache() {
        log.warn("[ClearLocalCache] 执行");
        long betweenTime = getSysParamInt(SysParamEnum.LOCAL_CACHE_TIME_OUT) * 60 * 1000;
        List<Map<String, LocalCacheBO<Object>>> commonCacheList= new ArrayList<>(commonCacheMap.values());
        for (Map<String, LocalCacheBO<Object>> cacheMap : commonCacheList) {
            List<String> cacheKeyList = new ArrayList<>(cacheMap.keySet());
            cacheKeyList.forEach(cacheKey-> {
                LocalCacheBO<Object> cacheObj = cacheMap.get(cacheKey);
                if (DateUtils.getCurrentTimeMillis() - cacheObj.getLastLoadTime().get() >= betweenTime) {
                    log.warn("[ChannelInfoMap] remove local cache key:{}", cacheKey);
                    cacheMap.remove(cacheKey);
                }
            });
        }
    }
}
