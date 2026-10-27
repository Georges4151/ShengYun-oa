package com.ceams.module.project.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 资源 ID 响应（data 内为 {@code {id: "雪花ID字符串"}}）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    public static IdResponse of(Long id) {
        return new IdResponse(id);
    }
}