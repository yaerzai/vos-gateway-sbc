package com.ytl.vos.gateway.sbc.jms.producer;

import com.ytl.common.jms.producer.BaseJsonJmsProducer;
import com.ytl.vos.jms.code.dto.black.BlackMobileDTO;
import com.ytl.vos.jms.code.enums.TopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *退订/投诉黑名单消息
 * @author kf-zhanghui
 * @date 2023/9/25 17:00
 */
@Slf4j
@Component
public class BlackMobileProducer extends BaseJsonJmsProducer<BlackMobileDTO> {

    @Override
    protected String getTopic() {
        return TopicEnum.TOPIC_RISK_ADD_BLACK_MOBILE.getName();
    }
}
