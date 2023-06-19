package cn.aircas.utils.date;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 时间工具类
 */
public class DateUtils {
    /**
     * 字符串转日期
     * @param times
     * @return
     */
    public static Date fromStringToDate(String times,String pattern) throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.parse(times);
    }

    /**
     * 日期转一定格式的字符串
     * @param date
     * @param pattern
     * @return
     */
    public static String fromDateToString(Date date, String pattern){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(date);
    }

    /**
     * 时间戳转字符串 时间戳转字符串
     * @param time
     * @param pattern
     * @return
     */
    public static String fromLongToString(long time, String pattern){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(time);
    }

    /**
     * 获取yyyy-MM-dd HH:mm:SS 格式的现在时间
     * @return 现在时刻
     */
    public static Date nowDate(){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
        Date date = new Date();
        String dateStr = simpleDateFormat.format(date);
        try {
            date = simpleDateFormat.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }


    /**
     * 自定义格式的时间
     * @param pattern
     * @return
     */
    public static String nowDate(String pattern){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        return simpleDateFormat.format(new Date());
    }
}
