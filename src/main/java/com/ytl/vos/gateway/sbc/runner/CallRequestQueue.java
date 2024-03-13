package com.ytl.vos.gateway.sbc.runner;

import com.ytl.common.base.exception.BusinessException;
import com.ytl.common.queue.runner.BatchDBRunner;
import com.ytl.vos.gateway.sbc.enums.VosErrCodeEnum;
import com.ytl.vos.gateway.sbc.service.DataService;
import com.ytl.vos.persistence.dataservice.CallRequestDataService;
import com.ytl.vos.persistence.dataservice.bo.CallRequestDataBO;
import com.ytl.vos.persistence.enums.AxbBindStatusEnum;
import com.ytl.vos.persistence.enums.SysParamEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class CallRequestQueue extends BatchDBRunner<CallRequestDataBO> {

    @Resource
    private DataService dataService;
    @Resource
    private CallRequestDataService callRequestDataService;

    @Override
    protected void batchDB(List<CallRequestDataBO> list) {
        Map<String, List<CallRequestDataBO>> map = splitTable(list);
        map.keySet().forEach(tableTime -> {
            List<CallRequestDataBO> insert = map.get(tableTime);

            int n = callRequestDataService.batchInsertCallRequest(tableTime, insert);
            if (n != insert.size()) {
                log.info("[InsertCallReq] DB异常,插入条数不等于队列数 插入:{}, 应该插入:{}", n, list.size());
                throw new BusinessException(VosErrCodeEnum.DB_Insert_Error, "插入条数不等于队列数");
            }
        });
    }

    @Override
    protected String getRedisQueueName() {
        return "InsertCallRequestQueue";
    }

    @Override
    protected int getBatchNumber() {
        return dataService.getSysParamInt(SysParamEnum.BATCH_INSERT_NUMBER);
    }

    @Override
    protected int getRetryTimes() {
        return dataService.getSysParamInt(SysParamEnum.DB_BATCH_RETRY_TIMES);
    }

    @Override
    protected BatchDBRunner getInstance() {
        return this;
    }

    @Override
    protected String getTableTime(CallRequestDataBO callRequestDataBO) {
        return callRequestDataBO.getTableTime();
    }

    @Override
    protected void handleNullFields(CallRequestDataBO requestDataBO) {
        requestDataBO.setCustomerNo(StringUtils.defaultString(requestDataBO.getCustomerNo(), StringUtils.EMPTY));
        requestDataBO.setUserNo(StringUtils.defaultString(requestDataBO.getUserNo(), StringUtils.EMPTY));
        requestDataBO.setGroupNo(StringUtils.defaultString(requestDataBO.getGroupNo(), StringUtils.EMPTY));
        requestDataBO.setChannelNo(StringUtils.defaultString(requestDataBO.getChannelNo(), StringUtils.EMPTY));
        requestDataBO.setCallingNumber(StringUtils.defaultString(requestDataBO.getCallingNumber(), StringUtils.EMPTY));
        requestDataBO.setCalledNumber(StringUtils.defaultString(requestDataBO.getCalledNumber(), StringUtils.EMPTY));
        requestDataBO.setCallingProvince(StringUtils.defaultString(requestDataBO.getCallingProvince(), StringUtils.EMPTY));
        requestDataBO.setCallingCity(StringUtils.defaultString(requestDataBO.getCallingCity(), StringUtils.EMPTY));
        requestDataBO.setCalledProvince(StringUtils.defaultString(requestDataBO.getCalledProvince(), StringUtils.EMPTY));
        requestDataBO.setCalledCity(StringUtils.defaultString(requestDataBO.getCalledCity(), StringUtils.EMPTY));
        requestDataBO.setRouteStatus(requestDataBO.getRouteStatus() != null ? requestDataBO.getRouteStatus() : 0);
        requestDataBO.setNeedRecord(requestDataBO.getNeedRecord() != null ? requestDataBO.getNeedRecord() : 0);
        requestDataBO.setErrCode(StringUtils.defaultString(requestDataBO.getErrCode(), StringUtils.EMPTY));
        requestDataBO.setErrMsg(StringUtils.defaultString(requestDataBO.getErrMsg(), StringUtils.EMPTY));
        requestDataBO.setBindId(StringUtils.defaultString(requestDataBO.getBindId(), StringUtils.EMPTY));
        requestDataBO.setNumberA(StringUtils.defaultString(requestDataBO.getNumberA(), StringUtils.EMPTY));
        requestDataBO.setNumberX(StringUtils.defaultString(requestDataBO.getNumberX(), StringUtils.EMPTY));
        requestDataBO.setNumberY(StringUtils.defaultString(requestDataBO.getNumberY(), StringUtils.EMPTY));
        requestDataBO.setBindTime(StringUtils.defaultString(requestDataBO.getBindTime(), StringUtils.EMPTY));
        requestDataBO.setSendType(requestDataBO.getSendType() != null ? requestDataBO.getSendType() : 0);
        requestDataBO.setVosMode(requestDataBO.getVosMode() != null ? requestDataBO.getVosMode() : 0);
        requestDataBO.setBindStatus(Optional.ofNullable(requestDataBO.getBindStatus()).orElse(AxbBindStatusEnum.UnBind.getCodeId()));
        requestDataBO.setBindRequestTime(Optional.ofNullable(requestDataBO.getBindRequestTime()).orElse(""));
//        requestDataBO.setTableTime("");

    }
}
