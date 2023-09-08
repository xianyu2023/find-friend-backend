package com.yupi.springbootinit.service.impl;
import static com.yupi.springbootinit.constant.UserConstant.USER_LOGIN_STATE;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.exception.BusinessException;

import com.yupi.springbootinit.mapper.UserMapper;
import com.yupi.springbootinit.model.dto.user.UserQueryRequest;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.UserRoleEnum;
import com.yupi.springbootinit.model.vo.LoginUserVO;
import com.yupi.springbootinit.model.vo.UserVO;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.AlgorithmUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StopWatch;

/**
 * 用户服务实现
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private RedisTemplate<String,Object> redisTemplate;

    @Resource
    private UserMapper userMapper;
    /**
     * 盐值，混淆密码
     */
    private static final String SALT = "yupi";

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 密码和校验密码相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        synchronized (userAccount.intern()) {
            // 账户不能重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();
        // 单机锁
        synchronized (unionId.intern()) {
            // 查询用户是否已存在
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("unionId", unionId);
            User user = this.getOne(queryWrapper);
            // 被封号，禁止登录
            if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
            }
            // 用户不存在则创建
            if (user == null) {
                user = new User();
                user.setUnionId(unionId);
                user.setMpOpenId(mpOpenId);
                user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                user.setUserName(wxOAuth2UserInfo.getNickname());
                boolean result = this.save(user);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败");
                }
            }
            // 记录用户的登录态
            request.getSession().setAttribute(USER_LOGIN_STATE, user);
            return getLoginUserVO(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setTagsList(tagsToTagsList(user));
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        userVO.setTagsList(tagsToTagsList(user));
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String unionId = userQueryRequest.getUnionId();
        String mpOpenId = userQueryRequest.getMpOpenId();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(unionId), "unionId", unionId);
        queryWrapper.eq(StringUtils.isNotBlank(mpOpenId), "mpOpenId", mpOpenId);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StringUtils.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    // region 业务相关
    @Override
    public List<UserVO> listUserByTagMemory(List<String> tagNameList) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数tagNameList为null");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //查询数据库，先查询所有用户，再在内存中过滤【能少查数据就少查】
        // 只查询需要的字段，tags、userId
        queryWrapper.select("id","tags");
        //tags为空的就没必要查询了
        queryWrapper.isNotNull("tags");
        List<User> userList = userMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(userList)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未查询到符合条件的用户");
        }
        Gson gson = new Gson();
        //语法糖（stream的api方法filter）//全部条件都符合的用户id列表
        List<Long> idList = userList.stream().filter(user -> {
            //标签列表json字符串=>java对象（内存查询，String类型的对象）
            String tagNamesJs = user.getTags();
            //tagNamesJs可能为null,google gson的方法fromJson应该有应对传入参数为null的处理=>看源码得
//            if (StringUtils.isBlank(tagNamesJs)) {
//                return false;
//            }
            //如何传入Set<String>类型。set【无序、去重、判断value是否存在】
            Set<String> tagNamesJava = gson.fromJson(tagNamesJs, new TypeToken<Set<String>>() {
            }.getType());
            //tagNamesJava可能为null,用Optional处理
            tagNamesJava = Optional.ofNullable(tagNamesJava).orElse(new HashSet<>());
            for (String tagName : tagNameList) {
                if (!tagNamesJava.contains(tagName)) {
                    return false;
                }
            }
            return true;
        }).map(User::getId).collect(Collectors.toList());
        queryWrapper = new QueryWrapper<>();
        queryWrapper.in("id",idList);
        List<User> userList1 = userMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(userList1)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        stopWatch.stop();
        log.info("内存查询 所需总时间：" + stopWatch.getTotalTimeMillis());
        return getUserVO(userList1);
    }

    @Override
    public List<UserVO> listUserByTagSql(List<String> tagNameList) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数tagNameList为null");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //tags为空的就没必要查询了
        queryWrapper.isNotNull("tags");
        for (String tagName : tagNameList) {
            //叠加查询条件，同时满足
            //测试反向验证（tags LIKE ? AND tags LIKE ? AND tags LIKE ? AND tags LIKE ?）
            queryWrapper = queryWrapper.like("tags", tagName);
        }
        //查询数据库
        List<User> userList = userMapper.selectList(queryWrapper);
        if (CollectionUtils.isEmpty(userList)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未查询到符合条件的用户");
        }
        // userList.forEach(this::getSafetyUser);传入方法的参数是user对象时，lambda表达式的简写
        //集合=>流（遍历处理，脱敏）=>集合
        stopWatch.stop();
        log.info("SQL查询 所需总时间：" + stopWatch.getTotalTimeMillis());
        return getUserVO(userList);
    }

    @Override
    public List<UserVO> listUserByTagSqlOrMemory(List<String> tagNameList) {
        //改异步化，并发
        //添加异步任务1，内存查询
        CompletableFuture<List<UserVO>> task1 = CompletableFuture.supplyAsync(() -> listUserByTagMemory(tagNameList));
        //添加异步任务2，SQL查询
        CompletableFuture<List<UserVO>> task2 = CompletableFuture.supplyAsync(() -> listUserByTagSql(tagNameList));
        long begin = System.currentTimeMillis();
        long end = 0;
        long cycleTime = 0;
        List<UserVO> defaultUserVOList = new ArrayList<>();//用于getNow未完成任务的默认值
        while (true) {
            //isDone能检测任务是否完成，但轮询耗CPU
            //getNow非阻塞。任务未完成，获取默认值；任务完成，获取真正结果
            List<UserVO> now = task1.getNow(defaultUserVOList);
            List<UserVO> now2 = task2.getNow(defaultUserVOList);
            if (!defaultUserVOList.equals(now)) {
                log.info("内存查询先返回");
                return now;
            }
            if (!defaultUserVOList.equals(now2)) {
                log.info("SQL查询先返回");
                return now2;
            }
            end = System.currentTimeMillis();
            cycleTime = end -begin;
            //log.info("已过去"+cycleTime+"ms");//验证while循环是否正常执行
            if (cycleTime>7000) {
                //防止now、now2永远拿到的是默认值而无限循环。循环时间不超过7s
                log.error("查询超时，返回默认值空列表");
                return defaultUserVOList;
            }
        }
    };


    /**
     * tags标签列表json字符串转数组（java对象），方便前端页面展示
     * @param user
     * @return
     */
    private List<String> tagsToTagsList(User user) {
        if (user == null) {
            return null;
        }
        String tags = user.getTags();//json字符串
        Gson gson = new Gson();
        return gson.fromJson(tags, new TypeToken<ArrayList<String>>() {
        }.getType());
    }

    public Page<UserVO> readRedisCache(Long userId) {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        String redisKey = String.format("find-friend:user:recommendUsers:%s",userId);
        Page<UserVO> userVOPage = (Page<UserVO>)valueOperations.get(redisKey);//读缓存
        //读缓存可能会失败，有异常，但由于本接口的业务关系（即使读缓存失败也还可以查数据获取），所以不能抛出异常，只能捕获异常
        if (userVOPage==null) {
            return null;
        }
        return userVOPage;
    }


    public Page<UserVO> writeRedisCache(long current, long pageSize, Long userId, long timeout, TimeUnit unit) {
        //1.redis（缓存）的数据结构选用string
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        //2.设计redis缓存的key
        String redisKey = String.format("find-friend:user:recommendUsers:%s",userId);
        //3.过期时间+预估redis内存+自定义redis的淘汰策略
        //业务：没有缓存时，先查询数据库把查询结果返回，并把查询数据写入到缓存
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        Page<User> userAllPage = this.page(new Page<>(current, pageSize), queryWrapper);//查询数据库
        if (userAllPage == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        List<UserVO> userAllVO = getUserVO(userAllPage.getRecords());
        Page<UserVO> userVOPage = new Page<>(current, pageSize);
        userVOPage.setRecords(userAllVO);
        try {
            //这里的业务是先查询数据库，再把从数据库中查出的查询结果返回给前端，没有用到缓存。
            //所以这里可能存在的写缓存失败的异常，只能捕获，不能抛出异常去影响业务的正常运行
            valueOperations.set(redisKey,userVOPage,timeout,unit);//把查询数据写到redis缓存
        } catch (Exception e) {
            log.error("set redisKey error");//日志处理
        }
        return userVOPage;
    }


    @Override
    public List<UserVO> matchUser(Integer num,User loginUser) {
        //取出所有的用户，然后依次和当前用户进行匹配（匹配：通过标签进行一个相似度计算（算法实现）)
        //solved查询数据库很慢。查询所有，内存不足.
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        //不查询tags为null的。只查询id、tags字段。
        queryWrapper.isNotNull("tags");
        queryWrapper.select("id","tags");
        List<User> userList = this.list(queryWrapper);
        //gson可用于：json字符串=>json对象，java对象
        Gson gson = new Gson();
        //标签列表：json字符串 => 标签列表对象：java对象，List<String>对象
        String tags = loginUser.getTags();
        //loginUser可能是一个新用户，没有设置tags标签列表
        if (StringUtils.isBlank(tags)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"当前用户没有设置自身标签");
        }
        List<String> tagList1 = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
        //用户列表下标——相似度（距离）
        /*
        这个map的默认排序规则是什么？设想它是根据value已经把map中的所有entry给排序好了的
        实际结果：压根没有排序好。且map比较占用空间
         */
//        SortedMap<Integer, Long> indexDistacneMap = new TreeMap<>();
        //todo 这里的indexDistacneList其实是存储了所有符合条件用户的下标-相似度分数，包括那些相似度分数比较低的（没必要存储相似度低的），比较占用内存空间
        //解决方法：维护固定长度的数组或者有序集合，以时间换空间，就是耗费更多时间
        List<Pair<Long,Long>> indexDistacneList = new ArrayList<>();
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String strTags = user.getTags();
            Long userId = user.getId();
            //1.排除和自己匹配。2.用户没有标签列表，没必要进行标签的相似度匹配
            if (loginUser.getId().equals(userId) || StringUtils.isBlank(strTags)) {
                continue;
            }
            List<String> tagList2 = gson.fromJson(strTags, new TypeToken<List<String>>() {
            }.getType());
            //编辑距离算法。距离越小，相似度分数越高。把距离从小到低排序。取top N个用户（相似度最高）
            //todo yupi为啥选用long接收
            long distance = AlgorithmUtils.minDistance(tagList1, tagList2);
            //想办法暂时保存distance和对应的用户（用索引代替用户，节省内存空间。刚好这里的list用户列表的下标就可以当作用户的索引）
            //下标——相似度
            indexDistacneList.add(new Pair<>(userId,distance));
        }
        //给List列表排序(a-b，从小到大)，并取TOP N
        List<Long> sortedIndexList = indexDistacneList.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .map(Pair::getKey)
                .collect(Collectors.toList());
//        List<Integer> matchIndexList = indexDistacneMap.keySet().stream().limit(num).collect(Collectors.toList());
        //todo getSafetyUser待更新
        //userList是直接从数据库查询出来的，未经过脱敏处理
        //匹配好的用户id列表（有匹配顺序）
//        List<Long> matchIdList = sortedIndexList.stream()
//                .map(index -> userList.get(index).getId())
//                .collect(Collectors.toList());
        QueryWrapper<User> queryWrapper1 = new QueryWrapper<>();
        queryWrapper1.in("id",sortedIndexList);
        //重新这样查询后的用户列表不具有匹配顺序,通过stream的分组功能给每个用户添加一个用户id作为key，形成一个map
        //由于id是唯一的，所以分组后，每个key（id）对应的List<User>其实只有一个User对象
        //无序的map，但它可以通过有序的key来取值，通过流最终形成一个有序的value列表
        Map<Long, List<UserVO>> idUserMap = this.list(queryWrapper1).stream()
                .map(user -> getUserVO(user))
                .collect(Collectors.groupingBy(UserVO::getId));
        List<UserVO> finalUserList = sortedIndexList.stream().map(sortedId -> {
            return idUserMap.get(sortedId).get(0);
        }).collect(Collectors.toList());
        return finalUserList;
    }



    // endregion

    // region 参数校验
    public void updateUserJudge(User user) {
        //todo补充校验。传入的userUpdateMyRequest是不为空，但是这是update语句，修改某个用户信息字段时，用户可能什么都不传，导致后台sql拼写 语法异常，泄露信息
        //更新用户时，除了id，其他字段不能都为空
//        Long id = user.getId();
        String userAccount = user.getUserAccount();
        String userPassword = user.getUserPassword();
        String userName = user.getUserName();
        String userAvatar = user.getUserAvatar();
        String userProfile = user.getUserProfile();
        Integer gender = user.getGender();
        String phone = user.getPhone();
        String email = user.getEmail();
        String userRole = user.getUserRole();
//        Integer isDelete = user.getIsDelete();
        String tags = user.getTags();
        if (StringUtils.isAllBlank(userAccount,userPassword,userName,userAvatar,userProfile,phone,email,userRole,tags) && gender==null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
    }


}
