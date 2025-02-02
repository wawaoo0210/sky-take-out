package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
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

    @Autowired
    private WorkspaceService workspaceService;

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

        List<Map<String, Object>> turnovers = orderMapper.sumByMapList(map);

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
     * 导出运营数据报表
     *
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {

        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        BusinessDataVO businessData = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");
        try {
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);

            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(1).getCell(1).setCellValue(dateBegin + " 至 " + dateEnd);

            sheet.getRow(3).getCell(2).setCellValue(businessData.getTurnover());
            sheet.getRow(3).getCell(4).setCellValue(businessData.getOrderCompletionRate());
            sheet.getRow(3).getCell(6).setCellValue(businessData.getNewUsers());

            sheet.getRow(4).getCell(2).setCellValue(businessData.getValidOrderCount());
            sheet.getRow(4).getCell(4).setCellValue(businessData.getUnitPrice());

            for(int i = 0; i < 30; i++){
                LocalDate date = dateBegin.plusDays(i);
                BusinessDataVO data = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));

                XSSFRow row = sheet.getRow(i + 7);
                row.getCell(1).setCellValue(date.toString());
                row.getCell(2).setCellValue(data.getTurnover());
                row.getCell(3).setCellValue(data.getValidOrderCount());
                row.getCell(4).setCellValue(data.getOrderCompletionRate());
                row.getCell(5).setCellValue(data.getUnitPrice());
                row.getCell(6).setCellValue(data.getNewUsers());
            }

            ServletOutputStream outputStream = response.getOutputStream();
            excel.write(outputStream);

            outputStream.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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
