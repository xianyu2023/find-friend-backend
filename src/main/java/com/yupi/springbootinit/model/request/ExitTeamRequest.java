package com.yupi.springbootinit.model.request;

import lombok.Data;

import java.io.Serializable;

/**加入队伍请求体
 * @author happyxianfish
 */
@Data
public class ExitTeamRequest implements Serializable {

    private static final long serialVersionUID = -335880419194262603L;
    /**
     * 想退出的队伍id
     */
    private Long teamId;

}
