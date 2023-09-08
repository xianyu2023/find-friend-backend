package com.yupi.springbootinit.config;
import lombok.Data;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**用于分布式锁
 * 创建redisson配置文件，并在其中自定义一个Redisson客户端
 * @author xianyu
 */
@Data
@ConfigurationProperties(prefix = "spring.redis")
@Configuration
public class RedissonConfig {

    private String port;

    private String host;

    /**
     * 就离谱，除了yml里要有密码。这里也必须设置一个redis登录的密码
     */
    private String password;


    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置对象
        Config config = new Config();
        String redisAddress = String.format("redis://%s:%s",host,port);
        //redis单机useSingleServer
        config.useSingleServer()
                .setAddress(redisAddress)
                .setPassword(password)
                .setDatabase(2)//redisson分布式锁所在库
                .setTimeout(10000)
                .setConnectTimeout(10000);
        // 2. 创建 Redisson实例
        // Sync and Async API同步和异步库
        RedissonClient redisson = Redisson.create(config);
        return redisson;
    }
}
