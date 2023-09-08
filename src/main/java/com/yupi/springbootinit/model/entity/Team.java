package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 队伍表
 * @author happyxianfish
 * @TableName team
 */
@TableName(value ="team")
@Data
public class Team implements Serializable {
    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
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
     * 加密队伍的密码
     */
    private String teamPassword;

    /**
     * 数据创建时间
     */
    private Date createTime;

    /**
     * 数据修改时间
     */
    private Date updateTime;

    /**
     * 是否软删除，0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}