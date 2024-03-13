package com.ytl.vos.gateway.sbc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.ytl.common.base.utils.DateUtils;
import com.ytl.common.redis.service.RedisCacheService;
import com.ytl.vos.channel.api.dto.ChannelGroupChannelConfigRespDTO;
import com.ytl.vos.channel.api.dto.ChannelInfoDetailResDTO;
import com.ytl.vos.channel.enums.ProvinceSupportFlagEnum;
import com.ytl.vos.channel.enums.RecordStatusEnum;
import com.ytl.vos.customer.api.dto.base.CustomerUserInfoDTO;
import com.ytl.vos.gateway.sbc.service.ChannelService;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.gateway.sbc.service.bo.ChannelConfigBO;
import com.ytl.vos.persistence.dataservice.bo.CallRequestDataBO;
import com.ytl.vos.persistence.enums.WholeCountryFlagEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 路由服务
 */
@Service
@Slf4j
public class ChannelServiceImpl implements ChannelService {

    @Resource
    private RedisCacheService redisCacheService;
    @Resource
    private DataService dataService;

    /**
     * 通道路由
     * @param userInfo
     * @param callReqBO
     */
    @Override
    public ChannelInfoDetailResDTO checkRoute(CustomerUserInfoDTO userInfo, CallRequestDataBO callReqBO) {
        //获取通道组
        String groupNo = userInfo.getChannelGroupNo();

        //获取通道 通道组下面的所有通道
        List<ChannelGroupChannelConfigRespDTO> channelConfigList = getChannelList(groupNo);
        if (CollUtil.isEmpty(channelConfigList)) {
            log.warn("通道组下没有配通道: {}", groupNo);
            return null;
        }

        //省份通道筛选 流速控制、日限、月限
        Map<String, ChannelConfigBO> channelConfigMap = new HashMap<>();
        List<ChannelInfoDetailResDTO> channelList = channelFilterWithProvince(channelConfigList, channelConfigMap, callReqBO);

        if (CollUtil.isEmpty(channelList)) {
            //非省份通道筛选
            channelConfigMap.clear();
            channelList = channelFilter(channelConfigList, channelConfigMap, callReqBO);

            if (CollUtil.isEmpty(channelList)) {
                log.warn("路由过滤后无可用通道, 过滤前通道数量: {}", channelConfigList.size());
                return null;
            }
        }

        //通道路由 优先级 权重
        return channelRoute(channelList, channelConfigMap);
    }

    @Override
    public void channelLimitIncrement(ChannelInfoDetailResDTO channelInfo, String calleeid) {
        //流速增加
        channelFlowIncrement(channelInfo);
        //日限增加
        channelDayLimitIncrement(channelInfo, calleeid);
        //月限增加
        channelMonthLimitIncrement(channelInfo, calleeid);
    }

    //通道路由
    private ChannelInfoDetailResDTO channelRoute(List<ChannelInfoDetailResDTO> channelList, Map<String, ChannelConfigBO> channelConfigMap) {
        int minPriority = getMinPriority(channelList, channelConfigMap);
        List<ChannelInfoDetailResDTO> minChannelList = channelList.stream()
                .filter(channelInfo -> channelConfigMap.get(channelInfo.getChannelNo()).getPriority() == minPriority)
                .collect(Collectors.toList());
        if (minChannelList.size() <= 1) {
            return minChannelList.get(0);
        }
        return randomWeight(channelList, channelConfigMap);
    }

    //随机权重
    private ChannelInfoDetailResDTO randomWeight(List<ChannelInfoDetailResDTO> channelInfoList, Map<String, ChannelConfigBO> channelConfigMap) {
        List<Integer> weightList = channelInfoList.stream().map(channelInfo -> {
            String channelNo = channelInfo.getChannelNo();
            ChannelConfigBO channelConfig = channelConfigMap.get(channelNo);
            return channelConfig.getWeight();
        }).collect(Collectors.toList());

        int index = randomWeightIndex(weightList);
        return channelInfoList.get(index);
    }

    //随机权重选择下标
    private static int randomWeightIndex(List<Integer> weightList) {
        int allWeight = weightList.stream().mapToInt(Integer::intValue).sum();
        int randomVal = RandomUtil.randomInt(allWeight);
        int sum = 0;
        for (int i = 0; i < weightList.size(); i++) {
            Integer weigth = weightList.get(i);
            if (randomVal > sum && randomVal < sum + weigth) {
                return i;
            }
            sum += weigth;
        }
        return 0;
    }

    private int getMinPriority(List<ChannelInfoDetailResDTO> channelInfoList, Map<String, ChannelConfigBO> channelConfigMap) {
        int minPriority = 0;
        for (ChannelInfoDetailResDTO channelInfo : channelInfoList) {
            String channelNo = channelInfo.getChannelNo();
            ChannelConfigBO channelConfig = channelConfigMap.get(channelNo);
            Integer priority = channelConfig.getPriority();
            if (minPriority == 0) {
                minPriority = priority;
            } else {
                minPriority = Math.min(priority, minPriority);
            }
        }
        return minPriority;
    }

    //获取通道列表
    private List<ChannelGroupChannelConfigRespDTO> getChannelList(String groupNo) {
        //使用本地缓存
        return dataService.getChannelGroupChannelConfig(groupNo);
    }

    //通道筛选-省份配置
    private List<ChannelInfoDetailResDTO> channelFilterWithProvince(List<ChannelGroupChannelConfigRespDTO> channelList, Map<String, ChannelConfigBO> channelConfigMap, CallRequestDataBO callReqBO) {
        String calledNumber = callReqBO.getCalledNumber();
        String calledProvince = callReqBO.getCalledProvince();

        return channelList.stream().filter(channelConfig-> {
            if (channelConfig == null) {
                log.info("[省份筛选] 过滤无效通道配置");
                return false;
            }
            String channelNo = channelConfig.getChannelNo();
            if (RecordStatusEnum.OK.getCodeId() != channelConfig.getStatus()) {
                log.info("[省份筛选] 过滤停用通道配置: {}", channelNo);
                return false;
            }
            //非通配全国
            if (WholeCountryFlagEnum.YES.getCodeId() != channelConfig.getWholeCountryFlag()) {
                log.info("[省份筛选] 非通配全国, {}", channelNo);
                return false;
            }
            if (StrUtil.isEmpty(channelConfig.getSupportProvince())) {
                log.info("[省份筛选] 未配置省份, {}", channelNo);
                return false;
            }

            ChannelConfigBO channelConfigBO = getChannelConfig(channelConfig.getSupportProvince(), calledProvince);
            if (channelConfigBO == null) {
                log.info("[省份筛选] 不支持该省份, 通道:{}, 省份: {}", channelNo, calledProvince);
                return false;
            }
            channelConfigMap.put(channelNo, channelConfigBO);
            return true;
        }).map(channel-> getChannelInfo(channel.getChannelNo()))
                .filter(channelInfo-> channelFilter(channelInfo, calledNumber))
                .collect(Collectors.toList());
    }

    private List<ChannelInfoDetailResDTO> channelFilter(List<ChannelGroupChannelConfigRespDTO> channelList, Map<String, ChannelConfigBO> channelConfigMap, CallRequestDataBO callReqBO) {
        String calledNumber = callReqBO.getCalledNumber(); //TODO 主号号码日限？？？
        String calledProvince = callReqBO.getCalledProvince();
        return channelList.stream().filter(channelConfig-> {
            if (channelConfig == null) {
                log.info("过滤无效通道配置");
                return false;
            }
            String channelNo = channelConfig.getChannelNo();
            if (RecordStatusEnum.OK.getCodeId() != channelConfig.getStatus()) {
                log.info("过滤停用通道配置: {}", channelNo);
                return false;
            }
//            if (WholeCountryFlagEnum.YES.getCodeId() == channelConfig.getWholeCountryFlag() && StrUtil.isNotEmpty(channelConfig.getSupportProvince())) {
//                log.info("过滤省通道配置: {}", channelNo);
//                return false;
//            }
            if (WholeCountryFlagEnum.YES.getCodeId() != channelConfig.getWholeCountryFlag()) {
                if (ProvinceSupportFlagEnum.SUPPORT.getCodeId() == channelConfig.getSupportFlag()) {
                    //支持省份
                    boolean matchProvince = isMatchProvince(channelConfig.getSupportProvince(), calledProvince);
                    if (!matchProvince) {
                        log.info("不支持的省份, 通道: {}, 省份: {}", channelNo, calledProvince);
                        return false;
                    }
                } else {
                    //屏蔽省份
                    boolean matchProvince = isMatchProvince(channelConfig.getSupportProvince(), calledProvince);
                    if (matchProvince) {
                        log.info("屏蔽的省份, 通道: {}, 省份: {}", channelNo, calledProvince);
                        return false;
                    }
                }
            }
            //支持和屏蔽省份
            channelConfigMap.put(channelNo, ChannelConfigBO.builder().priority(channelConfig.getPriority()).weight(channelConfig.getWeight()).build());
            return true;
        }).map(channel-> getChannelInfo(channel.getChannelNo()))
                .filter(channelInfo-> channelFilter(channelInfo, calledNumber))
                .collect(Collectors.toList());
    }

    private boolean channelFilter(ChannelInfoDetailResDTO channelInfo, String callingNumber) {
        String channelNo = channelInfo.getChannelNo();
        if (RecordStatusEnum.OK.getCodeId() != channelInfo.getStatus()) {
            log.info("[省份筛选] 过滤停用通道: {}", channelNo);
            return false;
        }
        Date beginDate = channelInfo.getMaintenanceTimeBegin();
        Date endDate = channelInfo.getMaintenanceTimeEnd();
        if (beginDate != null && endDate != null && DateUtil.isIn(DateUtil.date(), beginDate, endDate)) {
            String beginDateStr = DateUtil.date(beginDate).toString("yyyy-MM-dd HH:mm:ss");
            String endDateStr = DateUtil.date(endDate).toString("yyyy-MM-dd HH:mm:ss");
            log.info("通道({}) 维护期间 ({}-{})", channelNo, beginDateStr, endDateStr);
            return false;
        }

        //检查流速
        if (checkFlow(channelInfo)) {
            return false;
        }
        //检查通道月限
        if (checkChannelMonthLimit(channelInfo, callingNumber)) {
            return false;
        }
        //检查通道日限
        if (checkChannelDayLimit(channelInfo, callingNumber)) {
            return false;
        }
        return true;
    }

    private boolean isMatchProvince(String supportProvinceStr, String province) {
        if (StrUtil.isEmpty(supportProvinceStr)) {
            return false;
        }
        String[] provinceConfigSplit = supportProvinceStr.split(",");
        for (String provinceConfigStr : provinceConfigSplit) {
            if (StrUtil.isEmpty(provinceConfigStr)) {
                continue;
            }
            String[] provinceConfigs = provinceConfigStr.split(":");
            String supportProvince = provinceConfigs[0];
            if (province.equals(supportProvince)) {
                return true;
            }
        }
        return false;
    }

    private ChannelConfigBO getChannelConfig(String supportProvinceStr, String province) {
        if (StrUtil.isEmpty(supportProvinceStr)) {
            return null;
        }
        String[] provinceConfigSplit = supportProvinceStr.split(",");
        for (String provinceConfigStr : provinceConfigSplit) {
            if (StrUtil.isEmpty(provinceConfigStr)) {
                continue;
            }
            String[] provinceConfigs = provinceConfigStr.split(":");
            if (provinceConfigs.length < 3) {
                continue;
            }
            String supportProvince = provinceConfigs[0];
            String priority = provinceConfigs[1];
            String weight = provinceConfigs[2];
            if (province.equals(supportProvince)) {
                return ChannelConfigBO.builder().priority(Integer.valueOf(priority)).weight(Integer.valueOf(weight)).build();
            }
        }
        return null;
    }

    //检查流速
    private boolean checkFlow(ChannelInfoDetailResDTO channelInfo) {
        String channelNo = channelInfo.getChannelNo();
        Integer limitNum = channelInfo.getLimitNum();
        if (limitNum == null || limitNum <= 0) {
            return false;
        }

        String time = DateUtil.date().toString("HHmmss");
        String redisKeyName = StrUtil.format("ChannelFlowLimit:{}:{}", channelNo, time);

        //通道提交流速
        long channelFlow = redisCacheService.incrementAndGet(redisKeyName, Integer.MAX_VALUE, 0, 1);
        if (channelFlow >= limitNum) {
            log.info("通道超流速, 账号: {}, 限制流速: {}, 当前流速: {}", channelNo, limitNum, channelFlow);
            return true;
        }
        return false;
    }

    //检查日限
    private boolean checkChannelDayLimit(ChannelInfoDetailResDTO channelInfo, String calleeid) {
        //日限笔数
        Integer daySuccessLimit = channelInfo.getDaySuccessLimit();
        if (daySuccessLimit == null || daySuccessLimit <= 0) {
            return false;
        }
        String prefix = calleeid.length() > 3 ? calleeid.substring(0, 3) : calleeid;
        String channelNo = channelInfo.getChannelNo();
        String currDay = DateUtil.date().toString("yyyyMMdd");
        String redisKeyName = StrUtil.format("ChannelDayLimit:{}:{}:{}", channelNo, currDay, prefix);

        int expTime = DateUtils.getCurrent2TodayEndSecondTime();

        long callCount = redisCacheService.incrementHash(redisKeyName, calleeid, 0, Integer.MAX_VALUE, expTime);
        if (callCount >= daySuccessLimit) {
            log.warn("超过通道日限, 通道: {}, 号码: {}, 日限: {}/{}", channelNo, calleeid, callCount, daySuccessLimit);
            return true;
        }
        return false;
    }

    //检查通道月限
    private boolean checkChannelMonthLimit(ChannelInfoDetailResDTO channelInfo, String calleeid) {
        //月限笔数
        Integer monthSuccessLimit = channelInfo.getMonthSuccessLimit();
        if (monthSuccessLimit == null || monthSuccessLimit <= 0) {
            return false;
        }

        String prefix = calleeid.length() > 4 ? calleeid.substring(0, 4) : calleeid;
        String channelNo = channelInfo.getChannelNo();
        String currMonth = DateUtil.date().toString("yyyyMM");
        String redisKeyName = StrUtil.format("ChannelMonthLimit:{}:{}:{}", channelNo, currMonth, prefix);

        DateTime monthEnd = DateUtil.endOfMonth(DateUtil.date());
        int expTime = (int) DateUtil.between(DateUtil.date(), monthEnd, DateUnit.SECOND);

        long callCount = redisCacheService.incrementHash(redisKeyName, calleeid, 0, Integer.MAX_VALUE, expTime);
        if (callCount >= monthSuccessLimit) {
            log.warn("超过通道月限, 通道: {}, 号码: {}, 月限: {}/{}", channelNo, calleeid, callCount, monthSuccessLimit);
            return true;
        }
        return false;
    }

    //检查流速
    private void channelFlowIncrement(ChannelInfoDetailResDTO channelInfo) {
        String channelNo = channelInfo.getChannelNo();
        String time = DateUtil.date().toString("HHmmss");
        String redisKeyName = StrUtil.format("ChannelFlowLimit:{}:{}", channelNo, time);

        //通道提交流速
        redisCacheService.incrementAndGet(redisKeyName, Integer.MAX_VALUE, 1, 1);
    }

    //检查日限
    private void channelDayLimitIncrement(ChannelInfoDetailResDTO channelInfo, String calleeid) {
        Integer daySuccessLimit = channelInfo.getDaySuccessLimit();
        if (daySuccessLimit == null || daySuccessLimit <= 0) {
            return;
        }
        String prefix = calleeid.length() > 3 ? calleeid.substring(0, 3) : calleeid;
        //日限笔数
        String channelNo = channelInfo.getChannelNo();
        String currDay = DateUtil.date().toString("yyyyMMdd");
        String redisKeyName = StrUtil.format("ChannelDayLimit:{}:{}:{}", channelNo, currDay, prefix);

        int expTime = DateUtils.getCurrent2TodayEndSecondTime();

        redisCacheService.incrementHash(redisKeyName, calleeid, 1, Integer.MAX_VALUE, expTime);
    }

    //检查通道月限
    private void channelMonthLimitIncrement(ChannelInfoDetailResDTO channelInfo, String calleeid) {
        //月限笔数
        Integer monthSuccessLimit = channelInfo.getMonthSuccessLimit();
        if (monthSuccessLimit == null || monthSuccessLimit <= 0) {
            return;
        }
        String prefix = calleeid.length() > 4 ? calleeid.substring(0, 4) : calleeid;
        //月限笔数
        String channelNo = channelInfo.getChannelNo();
        String currMonth = DateUtil.date().toString("yyyyMM");
        String redisKeyName = StrUtil.format("ChannelMonthLimit:{}:{}:{}", channelNo, currMonth, prefix);

        DateTime monthEnd = DateUtil.endOfMonth(DateUtil.date());
        int expTime = (int) DateUtil.between(DateUtil.date(), monthEnd, DateUnit.SECOND);

        redisCacheService.incrementHash(redisKeyName, calleeid, 1, Integer.MAX_VALUE, expTime);
    }

    //获取通道信息
    private ChannelInfoDetailResDTO getChannelInfo(String channelNo) {
        return dataService.getChannelInfo(channelNo);
    }


//    public static void main(String[] args) {
//        List<Integer> weigth = ListUtil.of(100, 100, 200);
//
//        List<AtomicInteger> countList = new ArrayList<>();
//        for (int i = 0; i < weigth.size(); i++) {
//            countList.add(new AtomicInteger());
//        }
//
//        for (int i = 0; i < 1000; i++) {
//            int index = randomWeightIndex(weigth);
//            countList.get(index).incrementAndGet();
//        }
//
//        for (AtomicInteger count : countList) {
//            System.out.println(count.get());
//        }
//    }
}
