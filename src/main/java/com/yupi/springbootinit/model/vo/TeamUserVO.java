package com.yupi.springbootinit.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**【VO:返回给前端数据的封装类】
 * 队伍信息及加入队伍的用户信息封装类（脱敏）
 * 【这里为了方便，加入队伍的用户信息只选择队长的信息，可扩展】
 * @author happyxianfish
 */
@Data
public class TeamUserVO implements Serializable {
    private static final long serialVersionUID = 5917592178391053277L;
    /**
     * 队伍id
     */
    private Long id;

    /**
     * 队伍名称
     */
    private String teamName;

    /**
     * 队伍描述
     */
    private String teamDescription;

    /**
     * 最大人数
     */
    private Integer maxNum;

    /**
     * 队伍过期时间
     */
    private Date expireTime;

    /**
     * 队长id
     */
    private Long leaderId;

    /**
     * 队伍状态，0-公开 1-私有 2-加密
     */
    private Integer teamStatus;

    /**
     * 数据创建时间
     */
    private Date createTime;

    /**
     * 数据修改时间
     */
    private Date updateTime;

//    /**
//     * 加入队伍的用户信息（脱敏）
//     */
//    private List<UserVO> userList;

    /**
     * 该队伍的队长信息（脱敏）
     */
    private UserVO leaderVO;

    /**
     *当前用户是否已加入该队伍
     */
    private boolean hasJoin = false;

//    private Boolean hasJoin;

    /**
     * 队伍已加入的人数
     */
    private Long joinNum;
}
