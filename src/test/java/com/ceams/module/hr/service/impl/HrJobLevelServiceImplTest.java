package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.hr.converter.HrJobLevelConverter;
import com.ceams.module.hr.dto.JobLevelCreateRequest;
import com.ceams.module.hr.dto.JobLevelUpdateRequest;
import com.ceams.module.hr.entity.HrJobLevel;
import com.ceams.module.hr.mapper.HrJobLevelMapper;
import com.ceams.module.hr.service.HrJobLevelService;
import com.ceams.module.hr.vo.JobLevelVO;
import java.util.List;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 职级字典服务单元测试：默认状态、编码唯一、部分更新与列表排序。 */
@ExtendWith(MockitoExtension.class)
class HrJobLevelServiceImplTest {

    @Mock private HrJobLevelMapper jobLevelMapper;

    private HrJobLevelService jobLevelService;

    /** MyBatis-Plus Lambda 条件依赖 TableInfo 缓存，纯单元测试需手工初始化职级实体元数据。 */
    @BeforeAll
    static void initTableInfoCache() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, HrJobLevel.class);
    }

    @BeforeEach
    void setUp() {
        jobLevelService = new HrJobLevelServiceImpl(jobLevelMapper, Mappers.getMapper(HrJobLevelConverter.class));
    }

    @Test
    void createShouldPersistWithDefaultStatusAndTrimmedFields() {
        JobLevelCreateRequest request = new JobLevelCreateRequest();
        request.setCode(" P5 ");
        request.setName(" 高级工程师 ");
        request.setJobFamily("TECH");
        request.setRankNo(5);
        request.setRemark("技术序列");
        when(jobLevelMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(jobLevelMapper.insert(any(HrJobLevel.class)))
                .thenAnswer(
                        invocation -> {
                            HrJobLevel jobLevel = invocation.getArgument(0);
                            jobLevel.setId(1001L);
                            return 1;
                        });

        String id = jobLevelService.create(request);

        assertThat(id).isEqualTo("1001");
        ArgumentCaptor<HrJobLevel> captor = ArgumentCaptor.forClass(HrJobLevel.class);
        verify(jobLevelMapper).insert(captor.capture());
        HrJobLevel inserted = captor.getValue();
        assertThat(inserted.getCode()).isEqualTo("P5");
        assertThat(inserted.getName()).isEqualTo("高级工程师");
        assertThat(inserted.getJobFamily()).isEqualTo("TECH");
        assertThat(inserted.getRankNo()).isEqualTo(5);
        // 未传排序号默认 0，未传状态默认启用
        assertThat(inserted.getSortOrder()).isZero();
        assertThat(inserted.getStatus()).isEqualTo("ENABLED");
        assertThat(inserted.getRemark()).isEqualTo("技术序列");
    }

    @Test
    void createShouldRejectDuplicateCode() {
        JobLevelCreateRequest request = new JobLevelCreateRequest();
        request.setCode("P5");
        request.setName("高级工程师");
        when(jobLevelMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> jobLevelService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_DUPLICATE_CODE);
        verify(jobLevelMapper, never()).insert(any(HrJobLevel.class));
    }

    @Test
    void updateShouldRejectWhenJobLevelNotExists() {
        when(jobLevelMapper.selectById(99L)).thenReturn(null);

        JobLevelUpdateRequest request = new JobLevelUpdateRequest();
        request.setName("新名称");

        assertThatThrownBy(() -> jobLevelService.update(99L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);
        verify(jobLevelMapper, never()).updateById(any(HrJobLevel.class));
    }

    @Test
    void updateShouldRejectDuplicateCode() {
        when(jobLevelMapper.selectById(10L)).thenReturn(jobLevel(10L, "P5", "高级工程师"));
        when(jobLevelMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        JobLevelUpdateRequest request = new JobLevelUpdateRequest();
        request.setCode(" P6 ");

        assertThatThrownBy(() -> jobLevelService.update(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_DUPLICATE_CODE);
        verify(jobLevelMapper, never()).updateById(any(HrJobLevel.class));
    }

    @Test
    void updateShouldApplyOnlyProvidedFields() {
        HrJobLevel existing = jobLevel(10L, "P5", "高级工程师");
        existing.setJobFamily("TECH");
        existing.setRankNo(5);
        existing.setSortOrder(1);
        when(jobLevelMapper.selectById(10L)).thenReturn(existing);

        JobLevelUpdateRequest request = new JobLevelUpdateRequest();
        request.setName(" 资深工程师 ");
        request.setStatus(" DISABLED ");
        request.setSortOrder(9);

        jobLevelService.update(10L, request);

        ArgumentCaptor<HrJobLevel> captor = ArgumentCaptor.forClass(HrJobLevel.class);
        verify(jobLevelMapper).updateById(captor.capture());
        HrJobLevel updated = captor.getValue();
        assertThat(updated.getName()).isEqualTo("资深工程师");
        assertThat(updated.getStatus()).isEqualTo("DISABLED");
        assertThat(updated.getSortOrder()).isEqualTo(9);
        // 未传字段保持原值
        assertThat(updated.getCode()).isEqualTo("P5");
        assertThat(updated.getJobFamily()).isEqualTo("TECH");
        assertThat(updated.getRankNo()).isEqualTo(5);
    }

    @Test
    void listShouldOrderBySortOrderThenCodeAndMapFields() {
        // 排序由 SQL 保证；当前实现返回全量职级（含停用），不做状态过滤
        when(jobLevelMapper.selectList(any(Wrapper.class)))
                .thenReturn(List.of(jobLevel(2L, "P6", "资深工程师"), jobLevel(1L, "P5", "高级工程师")));

        List<JobLevelVO> result = jobLevelService.list();

        assertThat(result).extracting(JobLevelVO::getCode).containsExactly("P6", "P5");
        assertThat(result.get(0).getName()).isEqualTo("资深工程师");
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(jobLevelMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("sort_order")
                .contains("ORDER BY")
                .contains("code");
    }

    @Test
    void enabledListShouldOnlyQueryEnabledAndOrderBySortOrderThenCode() {
        HrJobLevel p5 = jobLevel(1L, "P5", "高级工程师");
        p5.setStatus("ENABLED");
        HrJobLevel p6 = jobLevel(2L, "P6", "资深工程师");
        p6.setStatus("ENABLED");
        // 排序由 SQL 保证，这里模拟数据库已按 sort_order、code 升序返回
        when(jobLevelMapper.selectList(any(Wrapper.class))).thenReturn(List.of(p5, p6));

        List<JobLevelVO> result = jobLevelService.enabledList();

        assertThat(result).extracting(JobLevelVO::getCode).containsExactly("P5", "P6");
        assertThat(result).extracting(JobLevelVO::getStatus).containsOnly("ENABLED");
        ArgumentCaptor<Wrapper> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(jobLevelMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment())
                .contains("status")
                .contains("ORDER BY")
                .contains("sort_order")
                .contains("code");
        AbstractWrapper<?, ?, ?> wrapper = (AbstractWrapper<?, ?, ?>) captor.getValue();
        assertThat(wrapper.getParamNameValuePairs().values()).contains("ENABLED");
    }

    @Test
    void enabledListShouldReturnEmptyWhenNoEnabledLevel() {
        when(jobLevelMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

        assertThat(jobLevelService.enabledList()).isEmpty();
    }

    private HrJobLevel jobLevel(Long id, String code, String name) {
        HrJobLevel jobLevel = new HrJobLevel();
        jobLevel.setId(id);
        jobLevel.setCode(code);
        jobLevel.setName(name);
        return jobLevel;
    }
}
