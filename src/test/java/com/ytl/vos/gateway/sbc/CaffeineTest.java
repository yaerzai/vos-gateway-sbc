package com.ytl.vos.gateway.sbc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class CaffeineTest {

    @Test
    public void test() {
        Cache<Object, Object> cache = Caffeine.newBuilder()
                .initialCapacity(100) //初始1w条
                .maximumSize(10000) //最大缓存100w条
                .expireAfterWrite(24, TimeUnit.HOURS) //最后一次写入后24小时过期
                .expireAfterAccess(24, TimeUnit.HOURS) //最后一次读取后24小时过期
                .removalListener((key, value, removalCause) -> {
                }) //移出监听
                .recordStats() //记录命中
                .build();

        cache.put("111", "aaa");

        System.out.println(cache.getIfPresent("111"));

        cache.invalidate("111");

        System.out.println(cache.getIfPresent("111"));
    }
}
