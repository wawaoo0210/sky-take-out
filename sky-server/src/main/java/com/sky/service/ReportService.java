package com.sky.service;

import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;

import java.time.LocalDate;

public interface ReportService {

    /**
     * 营业额统计
     * @param startDate
     * @param endDate
     * @return
     */
    TurnoverReportVO getTurnoverStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * 统计指定时间区间内的用户数据
     * @param startDate
     * @param endDate
     * @return
     */
    UserReportVO getUserStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * 统计指定时间区间内的订单数据
     *
     * @param startDate
     * @param endDate
     * @return
     */
    OrderReportVO getOrderStatistics(LocalDate startDate, LocalDate endDate);

    /**
     * 统计指定时间区间内的销量前十
     *
     * @param startDate
     * @param endDate
     * @return
     */
    SalesTop10ReportVO getSalesTop10(LocalDate startDate, LocalDate endDate);
}
