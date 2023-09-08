package com.yupi.springbootinit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.constant.RedisKey;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.mapper.TeamMapper;
import com.yupi.springbootinit.model.dto.team.TeamQuery;
import com.yupi.springbootinit.model.entity.Team;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.entity.UserTeam;
import com.yupi.springbootinit.model.enums.TeamStatusEnum;
import com.yupi.springbootinit.model.request.ExitTeamRequest;
import com.yupi.springbootinit.model.request.JoinTeamRequest;
import com.yupi.springbootinit.model.request.UpdateTeamRequest;
import com.yupi.springbootinit.model.vo.TeamUserVO;
import com.yupi.springbootinit.model.vo.UserVO;
import com.yupi.springbootinit.service.TeamService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author happyxianfish
 * 针对表【team(队伍表)】的数据库操作Service实现
 * 2023-06-12 16:45:22
 */
@Slf4j
@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
        implements TeamService {

    /**
     * redisson实现redis分布式锁
     */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 这里使用UserTeamService而不是UserTeamMapper
     * 由于UserTeamService的校验比较充足。
     * mapper的校验比较少，除非是很严格的校验，可以在mapper层进行校验
     */
    @Resource
    private UserTeamService userTeamService;

    @Resource
    private UserService userService;


    @Override
    public Long addTeam(Team team, User loginUser) {
        //业务逻辑校验
        addTeamJudge(team, loginUser);
        //把登录用户/创建人的id作为队长的id
        final long loginUserId = loginUser.getId();
        team.setLeaderId(loginUserId);
        //   * 队长id（用户id）> 0且不为空
        Long leaderId = team.getLeaderId();
        if (leaderId == null || leaderId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //   * 校验用户最多创建5个队伍(作为队长最多拥有5个队伍（超过就vip）)这里的this是TeamMapper对象
        //todo有bug，并发或者多线程的问题，可能同时创建100个队伍【单机锁、分布式锁解决】
        RLock lock = redissonClient.getLock(RedisKey.TEAM_SERVICE_ADD_TEAM);
        try {
            while (true) {
                if(lock.tryLock(0,-1,TimeUnit.MILLISECONDS)) {
                    QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
                    teamQueryWrapper.eq("leaderId", leaderId);
                    long hasTeamNum = this.count(teamQueryWrapper);
                    if (hasTeamNum >= 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "最多创建5个队伍");
                    }
                    /*把下面的代码提取出来，单独使用事务，发现事务没有生效。同类中调用@Transactional事务方法，由于spring AOP代理的原因，注解不生效
                     * 解决：利用spring提供的动态代理机制
                     * 引入aop依赖+启动类加@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)+下面的代码
                     */
                    return ((TeamServiceImpl) AopContext.currentProxy()).addTeamActually(team, leaderId);
                }
            }
        } catch (InterruptedException e) {
            log.error("addTeam error" + e);
            return -1L;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Long addTeamActually(Team team, Long leaderId) {
        //4. 插入队伍信息到队伍表（team） //插入成功，mybaits回写插入队伍的id
        team.setId(null);
        boolean result = this.save(team);
        Long teamId = team.getId();
        if (!result || teamId == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "插入队伍信息失败");
        }
//        if (true) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"中断测试");
//        }
        //5. 插入用户（创建人、也是队长）和队伍到用户_队伍关系表(user_team)
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(leaderId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "插入用户队伍关系失败");
        }
        //注意：第四步创建队伍成功时，就应该有一个用户_队伍关系出现了。而且它们应该**要么都成功，要么都失败**，这就需要用到**事务**。
        return teamId;
    }

    private static void addTeamJudge(Team team, User loginUser) {
        //1. 请求参数是否为空？（基础校验）
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 是否登录，未登录不允许创建队伍（权限校验）
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //3. 信息校验：
        //   * 队伍名称 <= 20
        String teamName = team.getTeamName();
        if (StringUtils.isBlank(teamName) || teamName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //   * 描述 <= 512
        String teamDescription = team.getTeamDescription();
        if (StringUtils.isNotBlank(teamDescription) && teamDescription.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //   * 队伍人数 >=1 且 <=20
        int maxNum = Optional.ofNullable(team.getMaxNum()).orElse(0);
        if (maxNum < 1 || maxNum > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //   * 队伍过期时间 > 当前时间（new Date）
        Date expireTime = team.getExpireTime();
        if (expireTime == null || new Date().after(expireTime)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //   * 队伍状态是否公开（int）,不传默认是公开
        Integer teamStatus = Optional.ofNullable(team.getTeamStatus()).orElse(0);
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByStatus(teamStatus);
        if (teamStatusEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //   * 如果队伍状态是加密的，那一定要有密码，且密码 <= 32
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            String teamPassword = team.getTeamPassword();
            if (StringUtils.isBlank(teamPassword) || teamPassword.length() > 32) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }
        }
    }


    @Override
    public List<TeamUserVO> teamList(TeamQuery teamQuery, User loginUser) {
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>();
        //组合查询条件
        if (teamQuery != null) {
            //队伍id
            Long teamId = teamQuery.getId();
            if (teamId != null && teamId > 0) {
                teamQueryWrapper.eq("id", teamId);
            }
            //队伍id列表
            List<Long> teamIdList = teamQuery.getIdList();
            if (!CollectionUtils.isEmpty(teamIdList)) {
                teamQueryWrapper.in("id", teamIdList);
            }
            //队长id
            Long leaderId = teamQuery.getLeaderId();
            if (leaderId != null && leaderId > 0) {
                teamQueryWrapper.eq("leaderId", leaderId);
            }
            //队伍最大人数
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                teamQueryWrapper.eq("maxNum", maxNum);
            }
            //队伍描述
            String teamDescription = teamQuery.getTeamDescription();
            if (StringUtils.isNotBlank(teamDescription)) {
                teamQueryWrapper.like("teamDescription", teamDescription);
            }
            //队伍名称
            String teamName = teamQuery.getTeamName();
            if (StringUtils.isNotBlank(teamName)) {
                teamQueryWrapper.like("teamName", teamName);
            }
            //关键词查询
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                teamQueryWrapper.and(qw -> qw.like("teamName", searchText).or().like("teamDescription", searchText));
            }

            //根据队伍状态查询
            //想查询的队伍状态枚举-公开、私有、加密、没选择(null)//权限问题
            boolean isAdmin = userService.isAdmin(loginUser);
            Integer teamStatus = teamQuery.getTeamStatus();
            TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByStatus(teamStatus);
            //没有选择想查询的队伍状态时(null)，默认选择查询公开队伍
            if (teamStatusEnum == null) {
                teamStatusEnum = TeamStatusEnum.PUBLIC;
            }
            //不是管理员时，选择查看私有的队伍报没权限异常
            if (!isAdmin && TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
                //todo 拼接查询条件抛异常这里需注意
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
            if (teamStatusEnum != TeamStatusEnum.ALL) {
                teamQueryWrapper.eq("teamStatus", teamStatusEnum.getStatus());
            }
            //不展示已过期的队伍
            /*方式一：查询处所有队伍，剔除掉过期队伍
            方式二：只查询未过期的队伍(没有过期时间的、未过期的)
            sql实现方式二：expire_Time is null or expire_Time > now()
             */
            teamQueryWrapper.and(qw -> qw.gt("expireTime", new Date()).or().isNull("expireTime"));
        }
        //查询队伍信息
        List<Team> teamList = this.list(teamQueryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
//            return new ArrayList<>();
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "队伍不存在");
        }
        //脱敏
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        /* 关联查询-
        查询加入队伍的用户信息
        方式一：(sql),查询team表（各种条件），关联查询team_user表，再关联查询user表，关联查询两次
        select * from team t left join team_user tu on t.id = tu.team_Id left join user u on tu.user_Id = u.id;
        方式二：(代码实现sql查询)，由于会关联查询两次，这种代码实现方式效率较低
        */
        /*
        【通过teamId，查询team_user表，得到userId，再关联查询user表，得到user信息。
        即：先代码实现查出teamId，再查询team_user，关联查询user,关联查询一次.能行???】
        本项目为方便，查出队长信息即可。根据team表的队长id，关联查询一次user表
        select * from team t left join user u on t.leader_Id = u.id
        */
        for (Team team : teamList) {
            Long leaderId = team.getLeaderId();
            if (leaderId == null) {
                //continue跳过本次循环
                continue;
            }
            //查询队长的用户信息
            User leader = userService.getById(leaderId);
            //队伍信息脱敏
            TeamUserVO teamUserVO = new TeamUserVO();
            BeanUtils.copyProperties(team, teamUserVO);
            //队长用户信息脱敏
            if (leader != null) {
                //【查询的队长信息可能为null，但不用抛异常。由于这个符合条件的队伍还是可以展示的】
                UserVO leaderVO = new UserVO();
                BeanUtils.copyProperties(leader, leaderVO);
                //将脱敏的队长信息封装到队伍用户VO里
                teamUserVO.setLeaderVO(leaderVO);
            }
            teamUserVOList.add(teamUserVO);
        }
        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(UpdateTeamRequest updateTeamRequest, User loginUser) {
        if (updateTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = updateTeamRequest.getId();
        //被修改的队伍是否存在
        Team oldTeam = getTeamByTeamId(teamId);
        //非加密的队伍改为加密队伍时，修改队伍请求体必须附带密码(加密=>加密时，不需要有密码)
        TeamStatusEnum oldEnum = TeamStatusEnum.getEnumByStatus(oldTeam.getTeamStatus());
        TeamStatusEnum newEnum = TeamStatusEnum.getEnumByStatus(updateTeamRequest.getTeamStatus());
        if (!TeamStatusEnum.SECRET.equals(oldEnum) && TeamStatusEnum.SECRET.equals(newEnum)) {
            if (StringUtils.isBlank(updateTeamRequest.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "改为加密队伍必须设置密码");
            }
        }
        //权限校验，管理员可以修改所有队伍，队长可以修改自己的队伍
        //不是管理员，也不是队长
        if (!userService.isAdmin(loginUser) && !loginUser.getId().equals(oldTeam.getLeaderId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        Team updateTeam = new Team();
        BeanUtils.copyProperties(updateTeamRequest, updateTeam);
        boolean result = this.updateById(updateTeam);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "修改队伍信息失败");
        }
        return true;
    }

    @Override
    public boolean joinTeam(JoinTeamRequest joinTeamRequest, User loginUser) {
        //业务逻辑校验
        joinTeamJudge(joinTeamRequest, loginUser);
        Long joinTeamId = joinTeamRequest.getTeamId();
        Team team = getTeamByTeamId(joinTeamId);
        Long teamId = team.getId();
        long loginUserId = loginUser.getId();
        //使用分布式锁（redis+redisson）
        //todo 不使用大锁
        RLock lock = redissonClient.getLock(RedisKey.TEAM_SERVICE_JOIN_TEAM);
        try {
            //这里的try catch在整个循环外面，还是在while循环里面应该都是可以的
            //while (true)不断获取锁【try catch finally这只是一种捕获异常的结构，主代码还是看try里的代码】
            while (true) {
                //只有一个线程能获取到锁
                if (lock.tryLock(0L, -1, TimeUnit.MILLISECONDS)) {
                    System.out.println("getLock" + Thread.currentThread().getId());
                    //查询已加入该队伍的用户人数
                    //对队伍加锁
                    long num = this.countUserTeamByTeamId(teamId);
                    if (num >= team.getMaxNum()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "该队伍人数已满");
                    }
                    //   * userId不能重复加入已加入的队伍teamId（已加入的队伍包括自己的队伍）【幂等性】{别人的队伍（已加入）+别人的队伍（未加入）+自己的队伍（只有已加入）}
                    //队伍_用户关系表
                    QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("teamId", teamId);
                    queryWrapper.eq("userId", loginUserId);
                    long count = userTeamService.count(queryWrapper);
                    if (count > 0) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能重复加入已加入的队伍");
                    }
                    //   * 用户最多加入5个队伍
                    //对用户加锁
                    queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("userId", loginUserId);
                    long hasTeamNum = userTeamService.count(queryWrapper);
                    if (hasTeamNum >= 5) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "每个普通用户最多加入5个队伍");
                    }
                    //   * 修改队伍信息，补充人数{本项目这里采用的是队伍表、用户表、队伍_用户关系表这样的形式，加入队伍后，对于队伍的信息没有影响，不需要修改}
                    //   * 新增一条队伍_用户关系
                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(loginUserId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    boolean save = userTeamService.save(userTeam);
                    if (!save) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "保存信息失败，加入队伍失败");
                    }
                    //抢到锁，并加入队伍成功（会直接返回给方法，并且finally里的代码也会执行）
                    return true;
                } else {
                    //没抢到锁（继续while循环）
                    log.info(Thread.currentThread().getId() + "未获取到锁");
                }
            }
        } catch (InterruptedException e) {
            //这里不是捕获那些业务异常的。当然finally里的代码也会执行（如果不return false,会报错缺少返回）
            log.error("join team error", e);
            //try范围内，如果某处代码出现InterruptedException异常，会中断，之后的代码不会执行，且异常会被捕获到这里处理。由于是捕获异常处理，try范围外的代码还是可以执行的
            //如果是出现这些自定义的业务异常，会中断代码执行并直接返回，但finally里的代码还是会执行的（由于这里没有捕获业务异常，所以try范围外的代码也不会正常执行的）
            return false;
        } finally {
            //只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unLock" + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    private void joinTeamJudge(JoinTeamRequest joinTeamRequest, User loginUser) {
        //1. 基础校验-请求参数非空
        if (joinTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 权限校验-登录
        //3. 信息校验
        Long joinTeamId = joinTeamRequest.getTeamId();
        //   * 加入的队伍必须存在，队伍未过期，未满
        Team team = getTeamByTeamId(joinTeamId);
        //队伍未过期 sql: expire_Time is null or expire_Time > now()
        Date expireTime = team.getExpireTime();
        if (expireTime != null && expireTime.before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "加入队伍已过期");
        }
        //   * 禁止加入私有的队伍
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByStatus(team.getTeamStatus());
        if (teamStatusEnum == null || teamStatusEnum.equals(TeamStatusEnum.PRIVATE)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入私有的队伍");
        }
        //   * 如果加入的队伍是加密的，必须密码匹配才可以
        if (teamStatusEnum.equals(TeamStatusEnum.SECRET)) {
            String inputPassword = joinTeamRequest.getTeamPassword();
            if (StringUtils.isBlank(inputPassword) || !inputPassword.equals(team.getTeamPassword())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
            }
        }
        //   * 加入的队伍必须是其他人的队伍（不能加入自己的队伍）{队伍-对用户自己而言：别人的队伍+自己的队伍}
        long loginUserId = loginUser.getId();
        if (team.getLeaderId() == loginUserId) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不能加入自己的队伍");
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean exitTeam(ExitTeamRequest exitTeamRequest, User loginUser) {
        //1. 校验请求参数
        if (exitTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = exitTeamRequest.getTeamId();
        //2. 校验队伍是否存在
        Team team = getTeamByTeamId(teamId);
        //3. 校验用户是否已加入队伍
        //队伍_用户关系表
        long loginUserId = loginUser.getId();
        /*方式一
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("team_Id", teamId);
        queryWrapper.eq("user_Id", loginUserId);
         */
        //方式二
        UserTeam userTeamFirst = new UserTeam();
        userTeamFirst.setTeamId(teamId);
        userTeamFirst.setUserId(loginUserId);
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>(userTeamFirst);
        long count = userTeamService.count(queryWrapper);
        if (count == 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户未加入该队伍");
        }
        //4. 如果队伍
        long num = this.countUserTeamByTeamId(teamId);
        //   * 只剩一人，解散队伍
        if (num == 1) {
            //移除队伍、移除用户_队伍关系
            boolean result = this.removeById(teamId);
            if (!result) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
            }
        } else {
            //   * 还有其他人
            //     * 如果是队长退出队伍，队长权限自动转移给第二早加入的用户--先来后到
            if (loginUserId == team.getLeaderId()) {
                //根据加入时间或者用户_队伍id排序都行
                QueryWrapper<UserTeam> queryWrapperThird = new QueryWrapper<>();
                queryWrapperThird.eq("teamId", teamId);
                //在sql语句最后面拼接下面的sql字符串
                queryWrapperThird.last("order by joinTime asc limit 2");
                List<UserTeam> userTeamList = userTeamService.list(queryWrapperThird);
                if (CollectionUtils.isEmpty(userTeamList) || userTeamList.size() != 2) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR);
                }
                UserTeam userTeam = userTeamList.get(1);
                Long newLeaderId = userTeam.getUserId();
                //更新当前队伍的队长
                Team updateTeam = new Team();
                updateTeam.setId(teamId);
                updateTeam.setLeaderId(newLeaderId);
                boolean result = this.updateById(updateTeam);
                if (!result) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
                }
            }
        }
        //移除用户_队伍关系
        boolean remove = userTeamService.remove(queryWrapper);
        if (!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "退出队伍失败");
        }
        return true;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean deleteTeam(DeleteRequest deleteRequest, User loginUser) {
        //1. 校验请求参数
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long teamId = deleteRequest.getId();
        //2. 校验队伍是否存在
        Team team = getTeamByTeamId(teamId);
        //3. 校验用户是否是该队伍的队长
        if (!loginUser.getId().equals(team.getLeaderId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "非队长不可解散队伍");
        }
        //4. 移除所有加入该队伍的队伍_用户关系
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("teamId", teamId);
        boolean remove = userTeamService.remove(queryWrapper);
        if (!remove) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        //5. 删除该队伍
        boolean result = this.removeById(teamId);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return true;
    }

    /**
     * 根据队伍id获取队伍信息(校验队伍是否存在)
     *
     * @param teamId
     * @return
     */
    private Team getTeamByTeamId(Long teamId) {
        if (teamId == null || teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //2. 校验队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "该队伍不存在");
        }
        return team;
    }


    /**
     * 根据已存在队伍的队伍id查询已加入该队伍的用户人数
     *
     * @param teamId
     * @return
     */
    @Override
    public long countUserTeamByTeamId(Long teamId) {
        QueryWrapper<UserTeam> queryWrapperSecond = new QueryWrapper<>();
        queryWrapperSecond.eq("teamId", teamId);
        return userTeamService.count(queryWrapperSecond);
    }
}




