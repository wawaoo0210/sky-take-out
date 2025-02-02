package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    /**
     * 营业额统计
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public TurnoverReportVO getTurnoverStatistics(LocalDate startDate, LocalDate endDate) {

        List<LocalDate> dateList = getDateList(startDate, endDate);

        Map<String, Object> map = new HashMap<>();
        map.put("start", LocalDateTime.of(startDate, LocalTime.MIN));
        map.put("end", LocalDateTime.of(endDate, LocalTime.MAX));
        map.put("status", Orders.COMPLETED);

        List<Map<String, Object>> turnovers = orderMapper.sumByMap(map);

        Map<LocalDate, Double> turnoverMap = new HashMap<>();
        for (Map<String, Object> turnover : turnovers) {
            java.sql.Date sqlDate = (java.sql.Date) turnover.get("date");
            LocalDate date = sqlDate.toLocalDate();
            BigDecimal amountBigDecimal = (BigDecimal) turnover.get("amount");
            Double amount = amountBigDecimal.doubleValue();
            turnoverMap.put(date, amount);
        }

        List<Double> turnoverList = dateList.stream()
                .map(date -> turnoverMap.getOrDefault(date, 0.0))
                .collect(Collectors.toList());

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();
    }

    /**
     * 统计指定时间区间内的用户数据
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public UserReportVO getUserStatistics(LocalDate startDate, LocalDate endDate) {

        // 生成日期列表
        List<LocalDate> dateList = getDateList(startDate, endDate);

        // 查询开始日期之前的累计用户数（不包含startDate当天）
        int cumulativeUserCount = userMapper.countUsersBefore(startDate.atStartOfDay());

        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime dayStart = date.atTime(LocalTime.MIN);
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            // 查询当天新增用户数
            int newUsers = userMapper.countNewUsers(dayStart, dayEnd);
            newUserList.add(newUsers);

            // 计算当天累计用户数：初始用户数 + 累加每天新增的用户
            cumulativeUserCount += newUsers;
            totalUserList.add(cumulativeUserCount);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .build();
    }

    /**
     * 订单统计
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate startDate, LocalDate endDate) {

        // 生成日期列表
        List<LocalDate> dateList = getDateList(startDate, endDate);

        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        Integer totalOrderCount = 0;
        Integer totalValidOrderCount = 0;

        for (LocalDate date : dateList) {
            LocalDateTime start = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime end = LocalDateTime.of(date, LocalTime.MAX);

            Integer orderCount = orderMapper.countByDateAndStatus(start, end, null);
            Integer validOrderCount = orderMapper.countByDateAndStatus(start, end, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
            totalOrderCount += orderCount;
            totalValidOrderCount += validOrderCount;
        }

        Double orderCompletionRate = totalOrderCount == 0 ? 0.0 : (double) totalValidOrderCount / totalOrderCount;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(totalValidOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }

    /**
     * 统计指定时间区间内的销量前十
     *
     * @param startDate
     * @param endDate
     * @return
     */
    public SalesTop10ReportVO getSalesTop10(LocalDate startDate, LocalDate endDate) {

        LocalDateTime start = LocalDateTime.of(startDate, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(endDate, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop = orderMapper.getSalesTop(start, end);
        List<String> names = salesTop.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList());
        List<Integer> numbers = salesTop.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList());

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }


    /**
     * 根据起止日期计算日期列表
     *
     * @param startDate
     * @param endDate
     * @return
     */
    private List<LocalDate> getDateList(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dateList = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dateList.add(date);
        }
        return dateList;
    }

}
