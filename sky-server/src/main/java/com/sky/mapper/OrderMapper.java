package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.GoodsSalesDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {

    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息
     * @param orders
     */
    void update(Orders orders);

    @Select("select * from orders where number = #{orderNumber} and user_id= #{userId}")
    Orders getByNumberAndUserId(String orderNumber, Long userId);

    /**
     * 历史订单查询
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getById(Long id);

    /**
     * 订单状态统计
     * @param status
     * @return
     */
    @Select("select count(id) from orders where status = #{status}")
    Integer countStatus(Integer status);

    /**
     * 根据订单状态和下单时间查询订单
     * @param status
     * @param orderTimeLT
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTimeLT}")
    List<Orders> getByStatusAndOrderTimeLT(Integer status, LocalDateTime orderTimeLT);

    /**
     * 根据动态条件统计营业额数据
     * @param map
     * @return
     */
    @MapKey("")
    List<Map<String, Object>> sumByMap(Map<String, Object> map);

    /**
     * 根据动态条件查询订单数据
     * @param start
     * @param end
     * @param status
     * @return
     */
    Integer countByDateAndStatus(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("status") Integer status);

    /**
     * 根据动态条件查询销量前十
     * @param start
     * @param end
     * @return
     */
    List<GoodsSalesDTO> getSalesTop(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
