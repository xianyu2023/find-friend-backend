package com.yupi.springbootinit.service;
import com.yupi.springbootinit.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.StopWatch;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 数据导入--模拟百万用户
 */
@SpringBootTest
public class ImportUsersTest {
    @Resource
    private UserService userService;

    /**
     * 自定义线程池
     * 对于CPU密集型，分配的核心线程数=CPU-1【1：主程序】
     * 对于IO密集型，分配的核心线程数 > CPU【CPU的2倍也可以】
     */
    private ExecutorService executorService = new ThreadPoolExecutor(40,1000,10, TimeUnit.MINUTES,new ArrayBlockingQueue<>(10000));


    /**
     * 批量插入用户
     */
//    @Test
    void insertUsers() {
        StopWatch stopWatch = new StopWatch();
        System.out.println("begin begin 开始计时");
        stopWatch.start();
        final int INSERT_NUM = 100000;
        ArrayList<User> userList = new ArrayList<>();
        for (int i = 0; i < INSERT_NUM; i++) {
            User user = new User();
            user.setUserAccount("mock"+i);
            user.setUserPassword("123456789");
            user.setUserName("mock"+i);
            user.setUserAvatar("https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png");
            user.setUserProfile("kun java" +i);
            user.setGender(0);
            user.setPhone("10086123");
            user.setEmail("dogYupi@qq.com");
            user.setUserRole("user");
            userList.add(user);
        }
        //批量插入用户
        userService.saveBatch(userList,10000);
        stopWatch.stop();
        System.out.println("总耗时"+stopWatch.getTotalTimeMillis());
    }

    /**
     * 并发批量插入10w用户
     *
     */
//    @Test
    void concurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        System.out.println("begin begin 开始计时");
        stopWatch.start();
        final int INSERT_NUM = 100000;
        final int INSERT_SIZE = 10;
        int j = 0;
        //创建一个异步任务列表
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        //10w数据，先分成10组
        for (int i = 0; i < INSERT_SIZE; i++) {
            //ArrayList线程不安全
            List<User> userList = Collections.synchronizedList(new ArrayList<>());
            while (true) {
                //j++不是原子性的，不要放到异步任务里
                j++;
                User user = new User();
                user.setUserAccount("mock"+j);
                user.setUserPassword("123456789");
                user.setUserName("mock"+j);
                user.setUserAvatar("https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png");
                user.setUserProfile("kun java" +j);
                user.setGender(0);
                user.setPhone("10086123");
                user.setEmail("dogYupi@qq.com");
                user.setUserRole("user");
                userList.add(user);
                if(j % (INSERT_NUM/INSERT_SIZE) == 0) {
                    break;
                }
            }
            //创建一个异步任务，交给一个线程来处理(共10个任务)
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList,INSERT_NUM/INSERT_SIZE);
                System.out.println("threadName:"+Thread.currentThread().getName());
            });//默认线程池
            //添加任务到任务列表
            futureList.add(future);
        }
        //执行所有异步任务（并发）,join阻塞后续代码立刻执行
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println("总耗时"+stopWatch.getTotalTimeMillis());

    }

    /**
     * 并发批量插入10w用户
     *自定义线程池
     */
//    @Test
    void MyConcurrencyInsertUsers() {
        StopWatch stopWatch = new StopWatch();
        System.out.println("begin begin 开始计时");
        stopWatch.start();
        final int INSERT_NUM = 100000;
        final int INSERT_SIZE = 40;
        int j = 0;
        //创建一个异步任务列表
        List<CompletableFuture<Void>> futureList = new ArrayList<>();
        //10w数据，先分成10组
        for (int i = 0; i < INSERT_SIZE; i++) {
            //ArrayList线程不安全
            List<User> userList = Collections.synchronizedList(new ArrayList<>());
            while (true) {
                //j++不是原子性的，不要放到异步任务里
                j++;
                User user = new User();
                user.setUserAccount("mock"+j);
                user.setUserPassword("123456789");
                user.setUserName("mock"+j);
                user.setUserAvatar("https://636f-codenav-8grj8px727565176-1256524210.tcb.qcloud.la/img/logo.png");
                user.setUserProfile("kun java" +j);
                user.setGender(0);
                user.setPhone("10086123");
                user.setEmail("dogYupi@qq.com");
                user.setUserRole("user");
                userList.add(user);
                if(j % (INSERT_NUM/INSERT_SIZE) == 0) {
                    break;
                }
            }
            //创建一个异步任务，交给一个线程来处理(共10个任务)
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                userService.saveBatch(userList,INSERT_NUM/INSERT_SIZE);
                System.out.println("threadName:"+Thread.currentThread().getName());
            },executorService);//自定义线程池
            //添加任务到任务列表
            futureList.add(future);
        }
        //执行所有异步任务（并发）,join阻塞后续代码立刻执行
        CompletableFuture.allOf(futureList.toArray(new CompletableFuture[]{})).join();
        stopWatch.stop();
        System.out.println("总耗时"+stopWatch.getTotalTimeMillis());
    }
}
