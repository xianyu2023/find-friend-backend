package com.yupi.springbootinit.model.enums;

/**
 * 队伍状态枚举
 *
 * @author happyxianfish
 */

public enum TeamStatusEnum {
    /**
     * 枚举
     */
    PUBLIC(0, "公开"),
    PRIVATE(1, "私有"),
    SECRET(2, "加密"),
    ALL(3,"管理员查询所有");
    private final int status;
    private final String text;

    TeamStatusEnum(int status, String text) {
        this.status = status;
        this.text = text;
    }

    public int getStatus() {
        return status;
    }

    public String getText() {
        return text;
    }

    /**
     * 根据队伍状态status获取text
     */
    public static TeamStatusEnum getEnumByStatus(Integer status) {
        if (status == null) {
            return null;
        }
        //遍历所有枚举并取出其status，来和传入的进行对比
        TeamStatusEnum[] teamStatusEnums = TeamStatusEnum.values();
        for (TeamStatusEnum teamStatusEnum : teamStatusEnums) {
            if (status == teamStatusEnum.getStatus()) {
                return teamStatusEnum;
            }
        }
        return null;
    }
}
