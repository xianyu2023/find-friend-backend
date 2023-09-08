package com.yupi.springbootinit.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.model.dto.team.TeamQuery;
import com.yupi.springbootinit.model.entity.Team;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.request.ExitTeamRequest;
import com.yupi.springbootinit.model.request.JoinTeamRequest;
import com.yupi.springbootinit.model.request.UpdateTeamRequest;
import com.yupi.springbootinit.model.vo.TeamUserVO;

import java.util.List;


/**
* @author happyxianfish
* @description 针对表【team(队伍表)】的数据库操作Service
* @createDate 2023-06-12 16:45:22
*/
public interface TeamService extends IService<Team> {

    /**
     * 创建队伍
     *
     * @param team 创建的队伍
     * @param loginUser 当前的登录用户
     * @return 新建队伍的id
     */
    Long addTeam(Team team, User loginUser);

    /**创建队伍-插入队伍信息、用户队伍关系
     * 使用事务
     * @param team 创建的队伍
     * @param leaderId 队长id
     * @return 新建队伍的id
     */
    Long addTeamActually(Team team, Long leaderId);

    /**
     * 查询所有符合条件的队伍
     * @param teamQuery 查询条件
     * @param loginUser 登录用户
     * @return 所有符合条件的队伍及加入队伍的用户信息
     */
    List<TeamUserVO> teamList(TeamQuery teamQuery, User loginUser);

    /**
     * 修改队伍信息
     *
     * @param updateTeamRequest 修改队伍请求体
     * @param loginUser 登录用户信息
     * @return 返回修改成功
     */
    boolean updateTeam(UpdateTeamRequest updateTeamRequest, User loginUser);

    /**
     * 加入队伍
     * @param joinTeamRequest 加入队伍请求体
     * @param loginUser 登录用户信息
     * @return 加入成功
     */
    boolean joinTeam(JoinTeamRequest joinTeamRequest, User loginUser);

    /**
     * 退出队伍
     * @param exitTeamRequest
     * @param loginUser
     * @return
     */
    boolean exitTeam(ExitTeamRequest exitTeamRequest, User loginUser);

    /**
     * 解散队伍
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    boolean deleteTeam(DeleteRequest deleteRequest, User loginUser);


    /**
     * 根据已存在队伍的队伍id查询已加入该队伍的用户人数
     * @param teamId
     * @return
     */
    long countUserTeamByTeamId(Long teamId);
}
