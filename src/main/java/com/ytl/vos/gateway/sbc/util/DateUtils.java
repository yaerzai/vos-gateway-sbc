package com.ytl.vos.gateway.sbc.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ytl.vos.persistence.dataservice.SysParamsDataService;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DateUtils {

    /**
     * 是否在时段内
     * @param time
     * @param timeFrameBegin
     * @param timeFrameEnd
     * @return
     */
    public static boolean inTimeFrame(Date time, String timeFrameBegin, String timeFrameEnd) {
        String dateHHmm = DateUtil.date(time).toString("HH:mm");
        DateTime dateTime = DateUtil.parse(dateHHmm, "HH:mm");
        if (StrUtil.isNotEmpty(timeFrameBegin)) {
            DateTime dateBegin = DateUtil.parse(timeFrameBegin, "HH:mm");
            if (dateTime.isBefore(dateBegin)) {
                return false;
            }
        }
        if (StrUtil.isNotEmpty(timeFrameEnd)) {
            DateTime dateEnd = DateUtil.parse(timeFrameEnd, "HH:mm");
            if (dateTime.isAfter(dateEnd)) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @return
     */
    public static boolean isHolidays(Date date, SysParamsDataService sysParamsDataService) {
        DateTime dateTime = DateUtil.date(date);
        String year = dateTime.toString("yyyy");
        String weekendWorkStr = sysParamsDataService.getWithCache("WEEKEND_WORK_" + year, "");
        String workdayRestStr = sysParamsDataService.getWithCache("WORKDAY_REST_" + year, "");
        return isHolidays(date, weekendWorkStr, workdayRestStr);
    }
    
    public static boolean isHolidays(Date date, String weekendWorkStr, String workdayRestStr) {
        DateTime dateTime = DateUtil.date(date);
        String dateStr = dateTime.toString("yyyyMMdd");
        List<String> weekendWork = StrUtil.isNotEmpty(weekendWorkStr) ? Arrays.asList(weekendWorkStr.split(",")) : ListUtil.empty();
        List<String> workdayRest = StrUtil.isNotEmpty(workdayRestStr) ? Arrays.asList(workdayRestStr.split(",")) : ListUtil.empty();
        if (!dateTime.isWeekend()) { //工作日
            if (CollUtil.isNotEmpty(workdayRest) && workdayRest.contains(dateStr)) { //工作日休息
                return true;
            }
            return false;
        }
        if (CollUtil.isNotEmpty(weekendWork) && weekendWork.contains(dateStr)) { //周末补班
            return false;
        }
        return true;
    }


    public static void main(String[] args) {

        DateTime now = DateUtil.date();
//        System.out.println(inTimeFrame(now, "09:52", "10:00"));
//        System.out.println(inTimeFrame(now, "09:56", "10:00"));
//        System.out.println(inTimeFrame(now, "09:56", ""));
//        System.out.println(inTimeFrame(now, "10:00", "11:00"));
//        System.out.println(inTimeFrame(now, "", "11:00"));
//        System.out.println(isHolidays(now, "", ""));
//        System.out.println(isHolidays(now.offsetNew(DateField.DAY_OF_MONTH, 1), "20230819", ""));

    }


}
