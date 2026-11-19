package com.ceams.module.admin.support;

import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.common.mask.SensitiveMaskUtil;
import com.ceams.module.admin.entity.AdminAsset;
import com.ceams.module.admin.entity.AdminAssetBorrow;
import com.ceams.module.admin.entity.AdminMaterial;
import com.ceams.module.admin.entity.AdminMaterialApply;
import com.ceams.module.admin.entity.AdminMeetingApply;
import com.ceams.module.admin.entity.AdminMeetingRoom;
import com.ceams.module.admin.entity.AdminRepair;
import com.ceams.module.admin.entity.AdminVehicle;
import com.ceams.module.admin.entity.AdminVehicleApply;
import com.ceams.module.admin.entity.AdminVehicleExpense;
import com.ceams.module.admin.entity.AdminVisitor;
import com.ceams.module.admin.vo.AssetBorrowVO;
import com.ceams.module.admin.vo.AssetVO;
import com.ceams.module.admin.vo.MaterialApplyVO;
import com.ceams.module.admin.vo.MaterialVO;
import com.ceams.module.admin.vo.MeetingApplyVO;
import com.ceams.module.admin.vo.MeetingRoomVO;
import com.ceams.module.admin.vo.RepairVO;
import com.ceams.module.admin.vo.VehicleApplyVO;
import com.ceams.module.admin.vo.VehicleExpenseVO;
import com.ceams.module.admin.vo.VehicleVO;
import com.ceams.module.admin.vo.VisitorVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** 行政模块实体 → VO 转换（静态方法，避免引入额外映射框架）。 */
public final class AdminConverter {

    /** 缺口比例保留小数位。 */
    private static final int RATIO_SCALE = 4;

    /** JSON 解析器（仅用于报修图片文件名数组）。 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 图片文件名数组类型。 */
    private static final TypeReference<List<String>> IMAGE_LIST_TYPE = new TypeReference<>() {};

    private AdminConverter() {}

    /** 资产台账转展示对象。 */
    public static AssetVO toAssetVO(AdminAsset asset) {
        if (asset == null) {
            return null;
        }
        AssetVO vo = new AssetVO();
        vo.setId(asset.getId());
        vo.setAssetNo(asset.getAssetNo());
        vo.setName(asset.getName());
        vo.setCategory(asset.getCategory());
        vo.setSpecification(asset.getSpecification());
        vo.setPurchaseDate(asset.getPurchaseDate());
        vo.setPurchasePrice(asset.getPurchasePrice());
        vo.setDeptId(asset.getDeptId());
        vo.setKeeperId(asset.getKeeperId());
        vo.setLocation(asset.getLocation());
        vo.setStatus(asset.getStatus());
        vo.setDepreciationMonths(asset.getDepreciationMonths());
        vo.setRemark(asset.getRemark());
        vo.setCreateTime(asset.getCreateTime());
        return vo;
    }

    /** 领用记录转展示对象（资产与借用人可能为空，按空值安全处理）。 */
    public static AssetBorrowVO toBorrowVO(
            AdminAssetBorrow record, AdminAsset asset, String borrowerName) {
        if (record == null) {
            return null;
        }
        AssetBorrowVO vo = new AssetBorrowVO();
        vo.setId(record.getId());
        vo.setAssetId(record.getAssetId());
        if (asset != null) {
            vo.setAssetNo(asset.getAssetNo());
            vo.setAssetName(asset.getName());
        }
        vo.setBorrowerId(record.getBorrowerId());
        vo.setBorrowerName(borrowerName);
        vo.setBorrowDate(record.getBorrowDate());
        vo.setExpectedReturnDate(record.getExpectedReturnDate());
        vo.setActualReturnDate(record.getActualReturnDate());
        vo.setBpmInstanceId(record.getBpmInstanceId());
        vo.setStatus(record.getStatus());
        vo.setRemark(record.getRemark());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }

    /** 易耗品转展示对象，并计算库存缺口比例。 */
    public static MaterialVO toMaterialVO(AdminMaterial material) {
        if (material == null) {
            return null;
        }
        MaterialVO vo = new MaterialVO();
        vo.setId(material.getId());
        vo.setCode(material.getCode());
        vo.setName(material.getName());
        vo.setSpecification(material.getSpecification());
        vo.setUnit(material.getUnit());
        vo.setStockQty(material.getStockQty());
        vo.setSafetyStock(material.getSafetyStock());
        vo.setUnitPrice(material.getUnitPrice());
        vo.setStatus(material.getStatus());
        vo.setShortageRatio(shortageRatio(material.getStockQty(), material.getSafetyStock()));
        return vo;
    }

    /** 申领单转展示对象。 */
    public static MaterialApplyVO toMaterialApplyVO(
            AdminMaterialApply apply, AdminMaterial material, String applicantName) {
        if (apply == null) {
            return null;
        }
        MaterialApplyVO vo = new MaterialApplyVO();
        vo.setId(apply.getId());
        vo.setMaterialId(apply.getMaterialId());
        if (material != null) {
            vo.setMaterialCode(material.getCode());
            vo.setMaterialName(material.getName());
            vo.setUnit(material.getUnit());
        }
        vo.setApplicantId(apply.getApplicantId());
        vo.setApplicantName(applicantName);
        vo.setQuantity(apply.getQuantity());
        vo.setReason(apply.getReason());
        vo.setBpmInstanceId(apply.getBpmInstanceId());
        vo.setStatus(apply.getStatus());
        vo.setIssuedAt(apply.getIssuedAt());
        vo.setIssuedBy(apply.getIssuedBy());
        vo.setCreateTime(apply.getCreateTime());
        return vo;
    }

    /** 车辆台账转展示对象（不含剩余天数）。 */
    public static VehicleVO toVehicleVO(AdminVehicle vehicle) {
        return toVehicleVO(vehicle, (LocalDate) null);
    }

    /**
     * 车辆台账转展示对象，并按参考日期计算保险 / 年检剩余天数。
     *
     * <p>剩余天数为到期日 - 参考日，负数表示已过期；到期日为空时剩余天数为空。
     */
    public static VehicleVO toVehicleVO(AdminVehicle vehicle, LocalDate today) {
        if (vehicle == null) {
            return null;
        }
        VehicleVO vo = new VehicleVO();
        vo.setId(vehicle.getId());
        vo.setPlateNo(vehicle.getPlateNo());
        vo.setBrandModel(vehicle.getBrandModel());
        vo.setSeats(vehicle.getSeats());
        vo.setPurchaseDate(vehicle.getPurchaseDate());
        vo.setInsuranceExpire(vehicle.getInsuranceExpire());
        vo.setInspectionExpire(vehicle.getInspectionExpire());
        vo.setStatus(vehicle.getStatus());
        vo.setRemark(vehicle.getRemark());
        if (today != null) {
            vo.setInsuranceRemainDays(remainDays(today, vehicle.getInsuranceExpire()));
            vo.setInspectionRemainDays(remainDays(today, vehicle.getInspectionExpire()));
        }
        vo.setCreateTime(vehicle.getCreateTime());
        return vo;
    }

    /** 用车申请转展示对象（车辆可能为空，按空值安全处理）。 */
    public static VehicleApplyVO toVehicleApplyVO(
            AdminVehicleApply apply, AdminVehicle vehicle) {
        if (apply == null) {
            return null;
        }
        VehicleApplyVO vo = new VehicleApplyVO();
        vo.setId(apply.getId());
        vo.setVehicleId(apply.getVehicleId());
        if (vehicle != null) {
            vo.setVehiclePlateNo(vehicle.getPlateNo());
        }
        vo.setApplicantId(apply.getApplicantId());
        vo.setPurpose(apply.getPurpose());
        vo.setDestination(apply.getDestination());
        vo.setStartTime(apply.getStartTime());
        vo.setEndTime(apply.getEndTime());
        vo.setBpmInstanceId(apply.getBpmInstanceId());
        vo.setStatus(apply.getStatus());
        vo.setReturnTime(apply.getReturnTime());
        vo.setMileage(apply.getMileage());
        vo.setCreateTime(apply.getCreateTime());
        return vo;
    }

    /** 车辆费用转展示对象。 */
    public static VehicleExpenseVO toVehicleExpenseVO(AdminVehicleExpense expense) {
        if (expense == null) {
            return null;
        }
        VehicleExpenseVO vo = new VehicleExpenseVO();
        vo.setId(expense.getId());
        vo.setVehicleId(expense.getVehicleId());
        vo.setApplyId(expense.getApplyId());
        vo.setExpenseType(expense.getExpenseType());
        vo.setAmount(expense.getAmount());
        vo.setOccurDate(expense.getOccurDate());
        vo.setAttachmentName(expense.getAttachmentName());
        vo.setRemark(expense.getRemark());
        vo.setCreateTime(expense.getCreateTime());
        return vo;
    }

    /** 会议室转展示对象。 */
    public static MeetingRoomVO toMeetingRoomVO(AdminMeetingRoom room) {
        if (room == null) {
            return null;
        }
        MeetingRoomVO vo = new MeetingRoomVO();
        vo.setId(room.getId());
        vo.setName(room.getName());
        vo.setLocation(room.getLocation());
        vo.setCapacity(room.getCapacity());
        vo.setEquipment(room.getEquipment());
        vo.setStatus(room.getStatus());
        vo.setCreateTime(room.getCreateTime());
        return vo;
    }

    /** 会议室预定转展示对象。 */
    public static MeetingApplyVO toMeetingApplyVO(
            AdminMeetingApply apply, AdminMeetingRoom room) {
        if (apply == null) {
            return null;
        }
        MeetingApplyVO vo = new MeetingApplyVO();
        vo.setId(apply.getId());
        vo.setRoomId(apply.getRoomId());
        if (room != null) {
            vo.setRoomName(room.getName());
        }
        vo.setApplicantId(apply.getApplicantId());
        vo.setSubject(apply.getSubject());
        vo.setStartTime(apply.getStartTime());
        vo.setEndTime(apply.getEndTime());
        vo.setAttendeeIds(parseIds(apply.getAttendeeIds()));
        vo.setBpmInstanceId(apply.getBpmInstanceId());
        vo.setStatus(apply.getStatus());
        vo.setCreateTime(apply.getCreateTime());
        return vo;
    }

    /** 访客登记转展示对象，手机号统一脱敏，禁止输出密文 / 明文。 */
    public static VisitorVO toVisitorVO(AdminVisitor visitor) {
        if (visitor == null) {
            return null;
        }
        VisitorVO vo = new VisitorVO();
        vo.setId(visitor.getId());
        vo.setVisitorName(visitor.getVisitorName());
        vo.setPhone(SensitiveMaskUtil.maskPhone(visitor.getPhoneEnc()));
        vo.setCompany(visitor.getCompany());
        vo.setHostUserId(visitor.getHostUserId());
        vo.setVisitDate(visitor.getVisitDate());
        vo.setPurpose(visitor.getPurpose());
        vo.setStatus(visitor.getStatus());
        vo.setCheckinTime(visitor.getCheckinTime());
        vo.setCheckoutTime(visitor.getCheckoutTime());
        return vo;
    }

    /** 报修工单转展示对象，images_json 解析为文件名列表。 */
    public static RepairVO toRepairVO(AdminRepair repair) {
        if (repair == null) {
            return null;
        }
        RepairVO vo = new RepairVO();
        vo.setId(repair.getId());
        vo.setRepairNo(repair.getRepairNo());
        vo.setReporterId(repair.getReporterId());
        vo.setDeptId(repair.getDeptId());
        vo.setLocation(repair.getLocation());
        vo.setCategory(repair.getCategory());
        vo.setDescription(repair.getDescription());
        vo.setImages(parseImages(repair.getImagesJson()));
        vo.setFinishImages(parseImages(repair.getFinishImagesJson()));
        vo.setAssigneeId(repair.getAssigneeId());
        vo.setStatus(repair.getStatus());
        vo.setAssignedAt(repair.getAssignedAt());
        vo.setProcessedAt(repair.getProcessedAt());
        vo.setFinishedAt(repair.getFinishedAt());
        vo.setAcceptedAt(repair.getAcceptedAt());
        vo.setRemark(repair.getRemark());
        return vo;
    }

    /** 图片文件名列表序列化为 JSON 文本；空列表返回 null，避免写入无意义的空数组。 */
    public static String joinImages(List<String> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(images);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.BIZ_UNPROCESSABLE, "报修图片序列化失败", e);
        }
    }

    /** images_json 解析为文件名列表；空值或历史脏数据返回空列表。 */
    public static List<String> parseImages(String imagesJson) {
        if (imagesJson == null || imagesJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> images = OBJECT_MAPPER.readValue(imagesJson, IMAGE_LIST_TYPE);
            return images == null ? List.of() : images;
        } catch (JsonProcessingException ignore) {
            // 历史脏数据不阻断列表展示
            return List.of();
        }
    }

    /** 逗号分隔的 ID 串解析为列表，非法片段跳过。 */
    public static List<Long> parseIds(String joined) {
        if (joined == null || joined.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : joined.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.valueOf(trimmed));
            } catch (NumberFormatException ignore) {
                // 历史脏数据直接跳过，避免影响列表展示
            }
        }
        return ids;
    }

    /** ID 列表拼接为逗号分隔串（去重保序）。 */
    public static String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private static Long remainDays(LocalDate today, LocalDate expireDate) {
        if (expireDate == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(today, expireDate);
    }

    /**
     * 库存缺口比例：(安全库存 - 当前库存) / 安全库存。
     *
     * <p>安全库存为 0 时缺口比例记为 1，保证仍排在预警清单末尾之前可比较。
     */
    private static BigDecimal shortageRatio(BigDecimal stockQty, BigDecimal safetyStock) {
        BigDecimal stock = stockQty == null ? BigDecimal.ZERO : stockQty;
        if (safetyStock == null || safetyStock.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return safetyStock
                .subtract(stock)
                .divide(safetyStock, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
