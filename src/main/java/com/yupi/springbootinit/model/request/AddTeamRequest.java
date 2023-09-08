package com.yupi.springbootinit.model.request;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**创建队伍请求体
 * @author happyxianfish
 */
@Data
public class AddTeamRequest implements Serializable {
    private static final long serialVersionUID = -5500966377030916243L;

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
     * 加密队伍的密码
     */
    private String teamPassword;

}
