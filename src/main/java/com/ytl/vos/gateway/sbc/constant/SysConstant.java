package com.ytl.vos.gateway.sbc.constant;


import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SysConstant {
    /**
     * 第三方黑名单结果值
     */
    public static final int THIRD_BLACK_RESULT = 60;

    public static final String VOS_NUMBER_FREQUENCY = "NumberFrequency";

    public static final ThreadPoolExecutor monitorExecutors = new ThreadPoolExecutor(10, 10, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("callOver"));

    public static final ThreadPoolExecutor processExecutors = new ThreadPoolExecutor(50, 50, 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), new DefaultThreadFactory("callOver"));

    static {
        monitorExecutors.allowCoreThreadTimeOut(true);

        processExecutors.allowCoreThreadTimeOut(true);
    }
}
