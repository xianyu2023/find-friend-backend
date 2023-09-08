package com.yupi.springbootinit.constant;


/**
 * redis分布式锁名 常量
 */
public interface RedisKey {
    /**
     * 用于定时任务（实现主页推荐的缓存预热）的分布式锁
     */
    String REDIS_PRECACHE_DOCACHE = "find-friend:job:doCacheRecommendUsers:lock";

    /**
     * 队伍业务层加入队伍 分布式锁
     */
    String TEAM_SERVICE_JOIN_TEAM = "find-friend:teamService:joinTeam:lock";

    /**
     * 队伍业务层创建队伍 分布式锁
     */
    String TEAM_SERVICE_ADD_TEAM = "find-friend:teamService:addTeam:lock";
}
