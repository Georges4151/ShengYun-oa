package com.ceams.module.admin.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/** 会议室指定日期的已占用区间（供前端日历置灰）。 */
@Data
public class MeetingAvailabilityVO {

    /** 会议室 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long roomId;

    /** 查询日期。 */
    private LocalDate date;

    /** 已占用区间，按开始时间升序。 */
    private List<Interval> intervals;

    /** 单个占用区间。 */
    @Data
    public static class Interval {

        /** 开始时间。 */
        private LocalDateTime startTime;

        /** 结束时间。 */
        private LocalDateTime endTime;

        /** 会议主题。 */
        private String subject;

        /** 构造区间。 */
        public Interval() {}

        /** 构造区间。 */
        public Interval(LocalDateTime startTime, LocalDateTime endTime, String subject) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.subject = subject;
        }
    }
}
