package com.yupi.springbootinit.model.dto.team;

import com.yupi.springbootinit.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 *队伍查询封装类
 * @author happyxianfish
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQuery extends PageRequest {
    private static final long serialVersionUID = 2988610936391470385L;
    /**
     * 搜索关键词（同时对队伍名称和描述进行搜索）
     */
    private String searchText;

    /**
     * 队伍id
     */
    private Long id;

    /**
     * 队伍id列表
     */
    private List<Long> idList;

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
     * 队长id
     */
    private Long leaderId;

    /**
     * 队伍状态，0-公开 1-私有 2-加密
     */
    private Integer teamStatus;
}
