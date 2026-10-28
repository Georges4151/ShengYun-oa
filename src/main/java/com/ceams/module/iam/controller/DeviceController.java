package com.ceams.module.iam.controller;

import com.ceams.common.response.ApiResponse;
import com.ceams.common.response.PageResult;
import com.ceams.module.iam.dto.device.DeviceBindRequest;
import com.ceams.module.iam.service.DeviceService;
import com.ceams.module.iam.vo.UserDeviceVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备绑定接口。
 */
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ApiResponse<PageResult<UserDeviceVO>> page(@RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size) {
        return ApiResponse.success(deviceService.page(page, size));
    }

    @PostMapping
    public ApiResponse<String> bind(@Valid @RequestBody DeviceBindRequest request) {
        return ApiResponse.success(deviceService.bind(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> unbind(@PathVariable Long id) {
        deviceService.unbind(id);
        return ApiResponse.success();
    }
}