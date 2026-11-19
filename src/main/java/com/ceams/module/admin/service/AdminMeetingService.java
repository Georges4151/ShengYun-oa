package com.ceams.module.admin.service;

import com.ceams.module.admin.dto.MeetingApplyRequest;
import com.ceams.module.admin.dto.MeetingRoomCreateRequest;
import com.ceams.module.admin.dto.MeetingRoomUpdateRequest;
import com.ceams.module.admin.vo.MeetingApplyVO;
import com.ceams.module.admin.vo.MeetingAvailabilityVO;
import com.ceams.module.admin.vo.MeetingRoomVO;
import java.time.LocalDate;
import java.util.List;

/** 行政会议室服务。 */
public interface AdminMeetingService {

    /** 会议室台账列表。 */
    List<MeetingRoomVO> listRooms();

    /** 会议室建档，返回会议室 ID。 */
    String createRoom(MeetingRoomCreateRequest request);

    /** 编辑会议室基础信息。 */
    void updateRoom(Long id, MeetingRoomUpdateRequest request);

    /** 发起会议室预定（BPM），返回预定单 ID。 */
    String apply(MeetingApplyRequest request);

    /** 本人会议室预定记录。 */
    List<MeetingApplyVO> myApplies();

    /** 指定会议室指定日期的已占用区间（按开始时间升序）。 */
    MeetingAvailabilityVO availability(Long roomId, LocalDate date);
}
