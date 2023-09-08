package com.yupi.springbootinit.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.model.dto.user.UserQueryRequest;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.vo.LoginUserVO;
import com.yupi.springbootinit.model.vo.UserVO;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;

/**
 * 用户服务
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户登录（微信开放平台）
     *
     * @param wxOAuth2UserInfo 从微信获取的用户信息
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    // region 业务相关

    /**
     * 通过标签查询朋友-内存查询-选择的标签都满足
     * 有一点内存查询和SQL查询相结合的状况
     * @param tagNameList
     * @return
     */
    List<UserVO> listUserByTagMemory(List<String> tagNameList);

    /**
     * 通过标签查询朋友-SQL查询-选择的标签都满足
     * @param tagNameList
     * @return
     */
    List<UserVO> listUserByTagSql(List<String> tagNameList);

    /**
     * 通过标签查询朋友-SQL查询或者内存查询-选择的标签都满足
     * 谁先返回就用谁【异步任务1和异步任务2谁先执行完成，就返回哪个任务的List<UserVO>结果】
     * 注意：future的get()、join()方法都会阻塞当前线程直到结果返回。isDone可检测。getNow不阻塞
     * @param tagNameList
     * @return
     */
    List<UserVO> listUserByTagSqlOrMemory(List<String> tagNameList);


    /**
     * userId用于生成redisKey，用来读缓存
     *
     * @param userId 登录用户的id
     * @return
     */
    Page<UserVO> readRedisCache(Long userId);


    /**
     * 业务：没有缓存时，先查询数据库把查询结果返回，并把查询数据写入到缓存
     *
     * redis缓存的具体代码实现方式：①选择redis数据结构②设计redis缓存的key③过期时间+预估redis内存+自定义redis的淘汰策略
     *
     * @param current 第几页
     * @param pageSize 每页有几条数据
     * @param userId 登录用户id
     * @param timeout redis失效时间
     * @param unit 单位
     * @return
     */
    Page<UserVO> writeRedisCache(long current,long pageSize,Long userId,long timeout, TimeUnit unit);


    /**
     * 找朋友
     * @param num 找多少个兴趣最接近的朋友
     * @param loginUser
     * @return
     */
    List<UserVO> matchUser(Integer num,User loginUser);

    // endregion

    // region 参数校验

    /**
     * 更新用户时，除了id，其他字段不能都为空
     * 补充校验。更新的内容不能都为空，否则后端会报sql拼写 语法错误，泄露信息
     * @param user
     */
    void updateUserJudge(User user);
}
