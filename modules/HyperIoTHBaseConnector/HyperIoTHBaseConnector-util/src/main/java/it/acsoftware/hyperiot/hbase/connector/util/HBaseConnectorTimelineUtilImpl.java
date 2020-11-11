package it.acsoftware.hyperiot.hbase.connector.util;

import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorTimelineUtil;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseTimelineColumnFamily;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.TreeMap;

@Component(service = HBaseConnectorTimelineUtil.class, immediate = true)
public class HBaseConnectorTimelineUtilImpl implements HBaseConnectorTimelineUtil {

    @Override
    public String buildJsonOutput(TreeMap<Long, Long> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        Iterator<Long> iterator = events.keySet().iterator();
        while (iterator.hasNext()) {
            long timestamp = iterator.next();
            sb.append("{\"timestamp\": ").append(timestamp).append(", \"value\": ").append(events.get(timestamp)).append("}");
            if(iterator.hasNext())
                sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Given a scanning step, it returns next scanning value
     * @param localDateTime LocalDateTime which calculate next step from
     * @param step Scanning step
     * @return Next LocalDateTime, depending on step
     * @throws IOException IOException
     */
    private LocalDateTime getCurrentTime(LocalDateTime localDateTime, HBaseTimelineColumnFamily step) throws IOException {
        switch (step) {
            case YEAR:
                return localDateTime.plusYears(1L);
            case MONTH:
                return localDateTime.plusDays(localDateTime.getMonth().length(localDateTime.toLocalDate().isLeapYear()));
            case DAY:
                return localDateTime.plusDays(1L);
            case HOUR:
                return localDateTime.plusHours(1L);
            case MINUTE:
                return localDateTime.plusMinutes(1L);
            case SECOND:
                return localDateTime.plusSeconds(1L);
            case MILLISECOND:
                return localDateTime.plusSeconds(1000L);
            default:
                throw new IOException("Unknown step");
        }
    }

    @Override
    public String getDayColumnFamily(Instant instant) {
        int day = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getDayOfMonth();
        return day >= 10 ? String.valueOf(day) : "0" + day;
    }

    @Override
    public String getHourColumnFamily(Instant instant) {
        int hour = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getHour();
        return hour >= 10 ? String.valueOf(hour) : "0" + hour;
    }

    @Override
    public String getKeyFromStep(String key, HBaseTimelineColumnFamily step) throws ParseException {
        switch (step) {
            case YEAR:
                return key.substring(0, 4);
            case MONTH:
                return key.substring(0, 7);
            case DAY:
                return key.substring(0, 10);
            case HOUR:
                return key.substring(0, 13);
            case MINUTE:
                return key.substring(0, 16);
            case SECOND:
                return key.substring(0, 19);
            case MILLISECOND:
                return key;
            default:
                throw new ParseException("Wrong key", 0);
        }
    }

    @Override
    public String getMillisecondColumnFamily(Instant instant) {
        int milli = (LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getNano()) / 1_000_000;
        return milli >= 100 ? String.valueOf(milli) : milli >= 10 ? "0" + milli : "00" + milli;
    }

    @Override
    public String getMinuteColumnFamily(Instant instant) {
        int minute = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getMinute();
        return minute >= 10 ? String.valueOf(minute) : "0" + minute;
    }

    @Override
    public String getMonthColumnFamily(Instant instant) {
        int month = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getMonthValue();
        return month >= 10 ? String.valueOf(month) : "0" + month;
    }

    @Override
    public String getRowKeyPrefix(String hPacketId, HBaseTimelineColumnFamily granularity, long timestamp) throws IOException {
        Instant instant = Instant.ofEpochMilli(timestamp);
        switch (granularity) {
            case YEAR:
                return hPacketId;
            case MONTH:
                return String.join("_", hPacketId, getYearColumnFamily(instant));
            case DAY:
                return String.join("_", hPacketId, getYearColumnFamily(instant), getMonthColumnFamily(instant));
            case HOUR:
                return String.join("_", hPacketId, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                        getDayColumnFamily(instant));
            case MINUTE:
                return String.join("_", hPacketId, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                        getDayColumnFamily(instant), getHourColumnFamily(instant));
            case SECOND:
                return String.join("_", hPacketId, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                        getDayColumnFamily(instant), getHourColumnFamily(instant), getMinuteColumnFamily(instant));
            case MILLISECOND:
                return String.join("_", hPacketId, getYearColumnFamily(instant), getMonthColumnFamily(instant),
                        getDayColumnFamily(instant), getHourColumnFamily(instant), getMinuteColumnFamily(instant),
                        getSecondColumnFamily(instant));
            default:
                throw new IOException("Unexpected step received");
        }
    }

    @Override
    public String getSecondColumnFamily(Instant instant) {
        int second = LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getSecond();
        return second >= 10 ? String.valueOf(second) : "0" + second;
    }

    @Override
    public String getStringColumnBound(Instant instant, HBaseTimelineColumnFamily step) throws IOException {
        switch (step) {
            case YEAR:
                return getYearColumnFamily(instant);
            case MONTH:
                return getMonthColumnFamily(instant);
            case DAY:
                return getDayColumnFamily(instant);
            case HOUR:
                return getHourColumnFamily(instant);
            case MINUTE:
                return getMinuteColumnFamily(instant);
            case SECOND:
                return getSecondColumnFamily(instant);
            case MILLISECOND:
                return getMillisecondColumnFamily(instant);
            default:
                throw new IOException("Unexpected step received");
        }
    }

    @Override
    public long getTimestamp(String stringTimestamp, byte[] column, HBaseTimelineColumnFamily step, SimpleDateFormat format) throws ParseException {
        //String tmpKey = Bytes.toString(result.getRow()) + "_" + Bytes.toString(column);
        stringTimestamp = stringTimestamp.substring(stringTimestamp.indexOf("_") + 1);     // remove '<packetId>_', for sum up events on the same timestamp
        stringTimestamp = getKeyFromStep(stringTimestamp, step);
        return format.parse(stringTimestamp).getTime();   // millis in UnixEpochTime
    }

    @Override
    public String getYearColumnFamily(Instant instant) {
        return String.valueOf(LocalDateTime.ofInstant(instant, ZoneOffset.UTC).getYear());
    }

    @Override
    public void initializeEventMap(TreeMap<Long, Long> events, HBaseTimelineColumnFamily step,
            long startTime, long endTime, String timezone) throws IOException {
        long currentTime = startTime;
        while(currentTime < endTime) {
            events.put(currentTime, 0L);
            // get current time in LocalDateTime format
            Instant instant = Instant.ofEpochMilli(currentTime);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.of(timezone));
            // get next LocalDateTime, depending on step value
            LocalDateTime nextLocalDateTime = getCurrentTime(localDateTime, step);
            // get time in millis
            currentTime = nextLocalDateTime
                    .toInstant(    // get Instant object to obtain millis
                            // nextLocalDateTime is current time with given GMT,
                            // retrieve offset in order to get back time in UTC
                            ZoneId.of(timezone).getRules().getOffset(nextLocalDateTime))
                    .toEpochMilli();
        }
    }

}
