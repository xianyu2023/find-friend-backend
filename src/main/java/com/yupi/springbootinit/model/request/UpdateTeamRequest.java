package com.yupi.springbootinit.model.request;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**修改队伍请求体
 * @author happyxianfish
 */
@Data
public class UpdateTeamRequest implements Serializable {
    private static final long serialVersionUID = 2031228945476946336L;

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

//    /**
//     * 最大人数(vip)
//     */
//    private Integer maxNum;

    /**
     * 队伍过期时间
     */
    private Date expireTime;

//    /**
//     * 队长id(单独做个转移队长权限的功能)
//     */
//    private Long leaderId;

    /**
     * 队伍状态，0-公开 1-私有 2-加密
     */
    private Integer teamStatus;

    /**
     * 加密队伍的密码
     */
    private String teamPassword;

}
