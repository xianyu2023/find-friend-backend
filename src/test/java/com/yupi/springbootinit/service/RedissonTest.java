package com.yupi.springbootinit.service;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@SpringBootTest
public class RedissonTest {

    @Resource
    private RedissonClient redissonClient;

    /**
     * redisson像操作本地集合一样操作redis
     */
    @Test
    void test() {
        //list,数据在JVM内存中
        List<String> list = new ArrayList<>();
        list.add("yupi");
        System.out.println("list:" + list.get(0));
        list.remove(0);

        //redisson操作redis，数据在redis内存中
        RList<String> rList = redissonClient.getList("list-test");
        rList.add("yupi");
        System.out.println("rList:" + rList.get(0));
        rList.remove(0);

        //map
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("map","dogyupi");
        System.out.println("map"+hashMap.get("map"));
        hashMap.remove("map");

        //redisson操作redis，数据在redis内存中
        RMap<String, String> rMap = redissonClient.getMap("map-test");
        rMap.put("rMap","dogyupi");
        System.out.println("rMap"+rMap.get("rMap"));
        rMap.remove("rMap");

        //set
        //stack栈
    }

    @Test
    void watchDogTest() {
        RLock lock = redissonClient.getLock("yupao:preCacheJob:doCache:lock");
        try {
            //只有一个线程能获取到锁
            if(lock.tryLock(0L,-1, TimeUnit.MILLISECONDS)) {
                System.out.println("getLock" + Thread.currentThread().getId());
                log.info("执行定时任务，缓存预热-推荐页");
//                Thread.sleep(5000000);
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUsers error",e);
        } finally {
            //只能释放自己的锁
            if(lock.isHeldByCurrentThread()) {
                System.out.println("unLock" + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }
}
