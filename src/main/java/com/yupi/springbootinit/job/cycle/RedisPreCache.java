package com.yupi.springbootinit.job.cycle;

import com.yupi.springbootinit.constant.RedisKey;
import com.yupi.springbootinit.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务实现redis缓存预热+分布式锁保证定时任务只执行一次
 *
 * 业务：（分页+缓存）主页 首次加载迅速。所以 提前写缓存（缓存预热）
 * 有一百万用户，难道要给每个用户都实现这个缓存预热？
 * 不能。由于redis缓存预热会额外占用redis的内存
 */
@Component
@Slf4j
public class RedisPreCache {
    @Resource
    private UserService userService;

    /**
     * 不可能给百万用户都做缓存预热
     * 暂且写死，指定个固定id。实际开发中，像这种白名单之类的最好不要写死。
     * 实际开发中需要做动态配置【配置中心？？？】或者在数据库中给重点用户加个标识【userRole属性为vip就可以作为一个标识】
     */
    private List<Long> vipUserIdList = Arrays.asList(1L);

    @Resource
    private RedissonClient redissonClient;


    /**
     * 每天仅一台服务器执行1次，给vip用户的主页缓存预热
     * crontab表达式可网上生成
     * 定时任务初始化空指针异常（npe）导致定时任务不执行，try catch捕获即可
     */
    @Scheduled(cron = "0 0 1 ? * * ")
    public void doCacheRecommendUsers() {
        //设置分布式锁
        RLock lock = redissonClient.getLock(RedisKey.REDIS_PRECACHE_DOCACHE);
        try {
            //抢锁【设置为-1，让redisson的看门狗机制生效，自动续期30s，每10s刷新一次】
            if (lock.tryLock(0, -1, TimeUnit.MILLISECONDS)) {
                log.info(Thread.currentThread().getId()+"抢到了锁");
                //todo分布式锁控制该缓存预热定时任务只执行一次
                for (Long vipUserId : vipUserIdList) {
                    userService.writeRedisCache(1,10,vipUserId,30000, TimeUnit.MILLISECONDS);
                }
//                Thread.sleep(60000);
            } else {
                log.info(Thread.currentThread().getId()+"未抢到锁");
            }
        } catch (InterruptedException e) {
            log.error("doCacheRecommendUsers error"+e);
        } finally {
            //验证是否是自己的锁，并手动释放锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();//手动释放了锁。redisson在其内设置了lua脚本，是原子操作
                log.info(Thread.currentThread().getId() + "释放锁");
            }
        }
    }
}
