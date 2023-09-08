package com.yupi.springbootinit.model.request;

import lombok.Data;

import java.io.Serializable;

/**加入队伍请求体
 * @author happyxianfish
 */
@Data
public class JoinTeamRequest implements Serializable {
    private static final long serialVersionUID = 5226227926206485636L;
    /**
     * 想加入的队伍id
     */
    private Long teamId;
    /**
     * 输入的加密队伍的密码
     */
    private String teamPassword;

}
