package com.ceams.common.jackson;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化规则统一：
 *
 * <ul>
 *   <li>全部金额 {@link BigDecimal} → 字符串（decimal(18,2)）。
 *   <li>{@link LocalDateTime} → ISO8601（{@code yyyy-MM-dd'T'HH:mm:ss}，无时区偏移）。
 *   <li>{@link LocalDate} → {@code yyyy-MM-dd}。
 *   <li>雪花 ID 已在 {@code BaseEntity.id} 上通过 {@link ToStringSerializer} 转为字符串； 分页 total/page/size
 *       等数值保持原生数字类型输出。
 * </ul>
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
            builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
            builder.serializerByType(LocalDate.class, new LocalDateSerializer(DATE));
            builder.deserializerByType(
                    LocalDateTime.class,
                    new LocalDateTimeDeserializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            builder.deserializerByType(
                    LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ISO_LOCAL_DATE));
        };
    }
}
