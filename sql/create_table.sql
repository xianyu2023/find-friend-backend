# 数据库初始化

-- 创建库
create database if not exists find_friend;

-- 切换库
use find_friend;

-- 用户表
create table user
(
    id           bigint auto_increment comment 'id'
        primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    unionId      varchar(256)                           null comment '微信开放平台id',
    mpOpenId     varchar(256)                           null comment '公众号openId',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    gender       tinyint                                null comment '性别 0-男 1-女',
    phone        varchar(128)                           null comment '用户电话',
    email        varchar(512)                           null comment '邮箱',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin/ban',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    tags         varchar(1024)                          null comment '标签名列表;格式为json字符串',
    userStatus   tinyint      default 0                 null comment '0-正常，1-异常'
)
    comment '用户' collate = utf8mb4_unicode_ci;

create index idx_unionId
    on user (unionId);


create table tag
(
    id          bigint auto_increment comment '主键'
        primary key,
    tag_Name    varchar(256)                       null comment '标签名',
    user_Id     bigint                             null comment '用户id',
    parent_Id   bigint                             null comment '父标签id',
    parent_true tinyint                            not null comment '0-不是父标签，1-是父标签',
    create_Time datetime default CURRENT_TIMESTAMP null comment '该条数据创建时间',
    update_Time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '该记录修改时间',
    deleted     tinyint  default 0                 not null comment '是否软删除 0-未删除',
    constraint tag_tag_Name_uindex
        unique (tag_Name)
)
    comment '标签表';

create index tag_user_Id_index
    on tag (user_Id);



create table team
(
    id            bigint auto_increment comment '主键' primary key,
    teamName     varchar(256)                      not null comment '队伍名称',
    teamDescription  varchar(1024)                null comment '队伍描述',
    maxNum    int default 1           not null comment '最大人数',
    expireTime   datetime  null comment '队伍过期时间',
    leaderId      bigint not null comment '队长id',
    teamStatus   int  default 0                 not null comment '队伍状态，0-公开 1-私有 2-加密',
    teamPassword varchar(512)                       null comment '加密队伍的密码',
    createTime   datetime default CURRENT_TIMESTAMP null comment '数据创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '数据修改时间',
    deleted       tinyint  default 0                 not null comment '是否软删除，0-未删除 1-已删除'
)
    comment '队伍表';

create table user_team
(
    id            bigint auto_increment comment '主键' primary key,
    userId            bigint  comment '用户id' ,
    teamId            bigint  comment '队伍id' ,
    joinTime   datetime  null comment '用户加入时间',
    createTime   datetime default CURRENT_TIMESTAMP null comment '数据创建时间',
    updateTime   datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '数据修改时间',
    deleted       tinyint  default 0                 not null comment '是否软删除，0-未删除 1-已删除'
)
    comment '用户_队伍关系表';


=============================================
-- 帖子表
create table if not exists post
(
    id         bigint auto_increment comment 'id' primary key,
    title      varchar(512)                       null comment '标题',
    content    text                               null comment '内容',
    tags       varchar(1024)                      null comment '标签列表（json 数组）',
    thumbNum   int      default 0                 not null comment '点赞数',
    favourNum  int      default 0                 not null comment '收藏数',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId)
    ) comment '帖子' collate = utf8mb4_unicode_ci;

-- 帖子点赞表（硬删除）
create table if not exists post_thumb
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
    ) comment '帖子点赞';

-- 帖子收藏表（硬删除）
create table if not exists post_favour
(
    id         bigint auto_increment comment 'id' primary key,
    postId     bigint                             not null comment '帖子 id',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_postId (postId),
    index idx_userId (userId)
    ) comment '帖子收藏';
