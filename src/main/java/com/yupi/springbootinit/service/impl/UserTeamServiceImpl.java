package com.yupi.springbootinit.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.springbootinit.mapper.UserTeamMapper;
import com.yupi.springbootinit.model.entity.UserTeam;
import com.yupi.springbootinit.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author happyxianfish
* 针对表【user_team(用户_队伍关系表)】的数据库操作Service实现
* 2023-06-12 16:45:49
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




