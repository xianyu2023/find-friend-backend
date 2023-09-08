package com.yupi.springbootinit.service;
import com.yupi.springbootinit.model.entity.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import javax.annotation.Resource;

@SpringBootTest
public class RedisTemplateTest {
    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    /**
     * 操作redis
     */
    @Test
    void test() {
        //redis 字符串数据结构
        ValueOperations valueOperations = redisTemplate.opsForValue();//valueOperations是操作redis字符串的集合/合集
        //增
        valueOperations.set("str","xianyu");
        valueOperations.set("int",1);
        valueOperations.set("double",2.0);
        User user = new User();
        user.setId(1L);
        user.setUserName("xianyu");
        valueOperations.set("user",user);
        //查
        Object strYuPi = valueOperations.get("str");
        Assertions.assertEquals("xianyu", (String) strYuPi);
        Object strYuPi1 = valueOperations.get("int");
        Assertions.assertTrue(1 == (Integer) strYuPi1);
        Object strYuPi2 = valueOperations.get("double");
        Assertions.assertEquals(2.0, (Double) strYuPi2);
        Object strYuPi3 = valueOperations.get("user");
        System.out.println(strYuPi3);
        //改
        valueOperations.set("str","dog123");
        //删
        redisTemplate.delete("user");
    }


}
