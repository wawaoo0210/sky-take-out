package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 插入数据
     * @param user
     */
    void insert(User user);

    /**
     * 根据id查询用户
     * @param userId
     * @return
     */
    @Select("select * from user where id = #{id}")
    User getById(Long userId);

    /**
     * 统计指定时间之前的累计用户数（不包括指定时间当天）
     *
     * @param start 指定时间的起始时间，通常为当天00:00:00
     * @return 用户数量
     */
    @Select("SELECT COUNT(*) FROM user WHERE create_time < #{start}")
    int countUsersBefore(@Param("start") LocalDateTime start);

    /**
     * 统计指定时间段内的新增用户数
     *
     * @param start 当天起始时间
     * @param end 当天结束时间
     * @return 当天新增用户数
     */
    @Select("SELECT COUNT(*) FROM user WHERE create_time BETWEEN #{start} AND #{end}")
    int countNewUsers(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

}
