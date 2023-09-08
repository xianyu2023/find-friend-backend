package com.yupi.springbootinit.model.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import lombok.Data;

/**
 * 用户视图（脱敏）
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@Data
public class UserVO implements Serializable {
    /**
     * 用户 id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 性别 0-男 1-女
     */
    private Integer gender;

    /**
     * 用户电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;


    /**
     * 用户角色：user/admin/ban
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 标签名列表;格式为json字符串=>数组对象（ArrayList）
     */
    private List<String> tagsList;

    /**
     * 用户状态。0-正常 1-异常
     */
    private Integer userStatus;

    private static final long serialVersionUID = 1L;
}