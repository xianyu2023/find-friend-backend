package com.yupi.springbootinit.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.model.dto.team.TeamQuery;
import com.yupi.springbootinit.model.entity.Team;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.entity.UserTeam;
import com.yupi.springbootinit.model.enums.TeamStatusEnum;
import com.yupi.springbootinit.model.request.AddTeamRequest;
import com.yupi.springbootinit.model.request.ExitTeamRequest;
import com.yupi.springbootinit.model.request.JoinTeamRequest;
import com.yupi.springbootinit.model.request.UpdateTeamRequest;
import com.yupi.springbootinit.model.vo.TeamUserVO;
import com.yupi.springbootinit.service.TeamService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.service.UserTeamService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;



/**
 * 队伍接口
 *
 * @author happyxianfish
 */

@CrossOrigin(origins = { "http://localhost:5173","http://localhost:3000","https://find-friend-front-67336-4-1318159870.sh.run.tcloudbase.com"},allowCredentials="true")
@RequestMapping("/team")
@RestController
@Slf4j
public class TeamController {
    @Resource
    private TeamService teamService;

    @Resource
    private UserService userService;

    @Resource
    private UserTeamService userTeamService;

    // region 增删改

    /**
     * 创建队伍
     * @param addTeamRequest 创建队伍请求体(前端传入的队伍参数)
     * @return 新建队伍的id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addTeam(@RequestBody AddTeamRequest addTeamRequest, HttpServletRequest request) {
        //基础校验
        if (addTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        //暂时这样？
        Team team = new Team();
        BeanUtils.copyProperties(addTeamRequest,team);
        Long teamId = teamService.addTeam(team, loginUser);
        return ResultUtils.success(teamId);
    }

    /**
     * 删除队伍（解散队伍）
     * @param deleteRequest 删除请求
     * @return 成功删除
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        //基础校验
        if (deleteRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(teamService.deleteTeam(deleteRequest,loginUser));
    }



    /**
     * 修改队伍
     * @param updateTeamRequest 修改队伍请求体/封装类(新的队伍信息)
     * @return 修改成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateTeam(@RequestBody UpdateTeamRequest updateTeamRequest, HttpServletRequest request) {
        //基础校验
        if (updateTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(teamService.updateTeam(updateTeamRequest,loginUser));
    }

    /**
     * 加入队伍
     * @param joinTeamRequest 加入队伍请求体
     * @param request 请求
     * @return 加入成功
     */
    @PostMapping("/join")
    public BaseResponse<Boolean> joinTeam(@RequestBody JoinTeamRequest joinTeamRequest, HttpServletRequest request) {
        //基础校验
        if (joinTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //休眠测试一下多并发的情况
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(teamService.joinTeam(joinTeamRequest, loginUser));
    }

    /**
     * 退出队伍
     * @param exitTeamRequest 退出队伍请求体
     * @param request 请求
     * @return 退出成功
     */
    @PostMapping("/exit")
    public BaseResponse<Boolean> exitTeam(@RequestBody ExitTeamRequest exitTeamRequest, HttpServletRequest request) {
        //基础校验
        if (exitTeamRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(teamService.exitTeam(exitTeamRequest, loginUser));
    }

    // endregion

    //region 查询



    /**
     * 根据队伍id查询单个队伍信息
     * @param teamId 队伍id
     * @return 单个队伍信息
     */
    @GetMapping("/get")
    public BaseResponse<Team> getTeamById(long teamId) {
        //基础校验
        if (teamId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Team result = teamService.getById(teamId);
        if(result == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(result);
    }


    /**
     * 根据 队伍查询条件 查询全部队伍
     * 注意：查询全部队伍接口要根据实际情况来决定是否开发
     * @param teamQuery DTO业务封装类：队伍查询封装类（TeamQuery队伍查询条件）
     * @return 查询到的队伍列表
     */
    @GetMapping("/list")
    public BaseResponse<List<TeamUserVO>> teamList(TeamQuery teamQuery, HttpServletRequest request) {
        if(teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        //展示的队伍
        List<TeamUserVO> teamList = teamService.teamList(teamQuery,loginUser);
        //标记已加入的队伍
        markTeamList(loginUser, teamList);
        //设置队伍已加入人数
        joinNumForTeam(teamList);
        return ResultUtils.success(teamList);
    }

    /**设置队伍列表的已加入人数
     * 展示每个队伍已加入的人数（teamList中每个队伍必存在）
     * @param teamList
     */
    private void joinNumForTeam(List<TeamUserVO> teamList) {
        teamList.forEach(teamUserVO -> {
            long joinNum = teamService.countUserTeamByTeamId(teamUserVO.getId());
            teamUserVO.setJoinNum(joinNum);
        });
    }

    /**
     * 给展示队伍标记一个当前用户已加入的标识
     * @param loginUser 当前登录用户
     * @param teamList 展示的队伍列表
     */
    private void markTeamList(User loginUser, List<TeamUserVO> teamList) {
        //展示的队伍的队伍id列表
        List<Long> teamIdList = teamList.stream().map(TeamUserVO::getId).collect(Collectors.toList());
        //给展示的队伍加个当前用户已加入标识
        //1.查询当前用户已加入的队伍并且队伍还是展示的队伍
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        queryWrapper.in("teamId",teamIdList);
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
        //set集合更好判断值是否存在
        Set<Long> joinTeamList = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toSet());
        //2.遍历展示的队伍列表，判断每个队伍是否已加入，如果已加入就添加已加入标识
        teamList.forEach(teamUserVO -> {
            boolean hasJoin = joinTeamList.contains(teamUserVO.getId());
            teamUserVO.setHasJoin(hasJoin);
        });
    }


    /**
     * 根据 队伍查询条件 分页查询
     *  service层未实现
     * @param teamQuery DTO队伍查询封装类【再创建一个通用的分页请求参数】
     * @return 分页查询到的队伍列表
     */
    @GetMapping("/list/page")
    public BaseResponse<Page<Team>> teamListPage(TeamQuery teamQuery) {
        if(teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //先暂时这样将TeamQuery的数据赋给Team
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery,team);
        //queryWrapper会根据传入的Team对象的属性来进行查询。缺点：默认不支持模糊查询。需要自己设置支持。
        QueryWrapper<Team> teamQueryWrapper = new QueryWrapper<>(team);
        /*  teamQuery.getPageNum(), teamQuery.getPageSize()可能为null
        方式一：可用Optional处理null并设置默认值
        方式二：可以在通用分页请求参数类里给它们设置默认值
        */
        Page<Team> teamPage = new Page<>(teamQuery.getCurrent(), teamQuery.getPageSize());
        Page<Team> pageResult = teamService.page(teamPage, teamQueryWrapper);
        if(pageResult == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(pageResult);
    }

    /**
     * 我是队长的队伍
     * 我拥有的队伍。不给条件默认查询公有的队伍
     * 前端给队伍状态条件，可查询公开、私有（管理员。本接口手动给与管理员权限）、加密的队伍
     * 【开闭原则：只增加代码，不修改原代码】
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/leader")
    public BaseResponse<List<TeamUserVO>> teamListMyLeader(TeamQuery teamQuery,HttpServletRequest request) {
        if(teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        //自己是队长
        teamQuery.setLeaderId(loginUser.getId());
        //暂时给与管理员权限，允许查询自己是队长的私有队伍
        loginUser.setUserRole(UserConstant.ADMIN_ROLE);
        //给予查询所有队伍状态
        teamQuery.setTeamStatus(TeamStatusEnum.ALL.getStatus());
        List<TeamUserVO> teamList = teamService.teamList(teamQuery,loginUser);
        //标记已加入队伍
        markTeamList(loginUser,teamList);
        //设置队伍已加入人数
        joinNumForTeam(teamList);
        return ResultUtils.success(teamList);
    }

    /**
     * 查询我加入的队伍（别人的队伍）
     * 默认查询公有。前端给队伍状态条件，可查询公开、私有（管理员。本接口手动给与管理员权限）、加密的队伍
     * 【开闭原则：只增加代码，不修改原代码】
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("/list/my/join")
    public BaseResponse<List<TeamUserVO>> teamListMyJoin(TeamQuery teamQuery,HttpServletRequest request) {
        if(teamQuery == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        //获取当前用户已加入的队伍id列表
        List<Long> idList = getJoinTeamList(loginUser);
        if (idList.size()==0) {
            return null;
        }
        //根据已加入的队伍id列表去查询这些已加入的队伍
        teamQuery.setIdList(idList);
        List<TeamUserVO> teamList = teamService.teamList(teamQuery,loginUser);
        //过滤掉自己是队长的队伍
        List<TeamUserVO> filterTeamList = teamList.stream().filter(teamUserVO -> {
            return !loginUser.getId().equals(teamUserVO.getLeaderId());
        }).collect(Collectors.toList());
        //标记已加入队伍
        markTeamList(loginUser,filterTeamList);
        //设置队伍已加入人数
        joinNumForTeam(filterTeamList);
        return ResultUtils.success(filterTeamList);
    }

    /**
     * 获取当前用户所有的已加入的队伍id列表
     * @param loginUser
     * @return
     */
    @NotNull
    private List<Long> getJoinTeamList(User loginUser) {
        //1.获取自己已加入的队伍id列表
        QueryWrapper<UserTeam> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", loginUser.getId());
        List<UserTeam> userTeamList = userTeamService.list(queryWrapper);
//    方式一：    List<Long> idList = userTeamList.stream().map(UserTeam::getTeamId).collect(Collectors.toList());
        //方式二：不用stream的map，直接按teamId进行分组，得到一个key是teamId的集合，然后把这些key抽取出来变成一个列表
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        return new ArrayList<>(listMap.keySet());
    }


}
