package com.ceams.module.hr.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ceams.common.exception.BusinessException;
import com.ceams.common.exception.ErrorCode;
import com.ceams.module.bpm.service.BpmInstanceService;
import com.ceams.module.hr.converter.HrEmployeeChangeConverter;
import com.ceams.module.hr.converter.HrEmployeeConverter;
import com.ceams.module.hr.dto.EmployeeCreateRequest;
import com.ceams.module.hr.entity.HrEmployee;
import com.ceams.module.hr.enums.EmployeeStatus;
import com.ceams.module.hr.enums.Gender;
import com.ceams.module.hr.mapper.HrEmployeeChangeMapper;
import com.ceams.module.hr.mapper.HrEmployeeMapper;
import com.ceams.module.hr.vo.EmployeeVO;
import com.ceams.module.iam.mapper.DeptMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** 员工档案服务单元测试：唯一绑定冲突、敏感字段脱敏输出、账号未绑定。 */
@ExtendWith(MockitoExtension.class)
class HrEmployeeServiceImplTest {

    @Mock private HrEmployeeMapper employeeMapper;
    @Mock private HrEmployeeChangeMapper changeMapper;
    @Mock private BpmInstanceService bpmInstanceService;
    @Mock private DeptMapper deptMapper;

    private HrEmployeeConverter employeeConverter;
    private HrEmployeeServiceImpl employeeService;

    @BeforeEach
    void setUp() {
        employeeConverter = Mappers.getMapper(HrEmployeeConverter.class);
        HrEmployeeChangeConverter changeConverter = Mappers.getMapper(HrEmployeeChangeConverter.class);
        employeeService =
                new HrEmployeeServiceImpl(
                        employeeMapper,
                        changeMapper,
                        employeeConverter,
                        changeConverter,
                        bpmInstanceService,
                        deptMapper);
    }

    @Test
    void createShouldRejectDuplicateEmployeeNo() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmployeeNo("E0001");
        request.setName("张三");
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_DUPLICATE_CODE);
    }

    @Test
    void createShouldRejectDuplicateUserBinding() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmployeeNo("E0002");
        request.setName("李四");
        request.setUserId(88L);
        // 工号不重复、账号已绑定
        when(employeeMapper.selectCount(any(Wrapper.class))).thenReturn(0L, 1L);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_DUPLICATE_CODE);
    }

    @Test
    void detailShouldMaskSensitiveFields() {
        HrEmployee employee = new HrEmployee();
        employee.setId(100L);
        employee.setEmployeeNo("E0003");
        employee.setName("王五");
        employee.setGender(Gender.MALE);
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setIdCardEnc("110101199003071234");
        employee.setPhoneEnc("13812345678");
        employee.setEmail("wangwu@example.com");
        employee.setEmergencyContact("王六");
        employee.setEmergencyPhoneEnc("13900001111");
        when(employeeMapper.selectById(100L)).thenReturn(employee);

        EmployeeVO vo = employeeService.detail(100L);

        assertThat(vo.getIdCard()).isEqualTo("110101****1234");
        assertThat(vo.getPhone()).isEqualTo("138****5678");
        assertThat(vo.getEmail()).isEqualTo("w****@example.com");
        assertThat(vo.getEmergencyPhone()).isEqualTo("139****1111");
        // 明文与密文都不允许出现在输出中
        assertThat(vo.getPhone()).doesNotContain("13812345678");
        assertThat(vo.getIdCard()).doesNotContain("19900307");
    }

    @Test
    void currentEmployeeShouldFailWhenAccountNotBound() {
        assertThatThrownBy(() -> employeeService.currentEmployee())
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIZ_NOT_FOUND);

        assertThatThrownBy(() -> employeeService.currentEmployeeId())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("当前账号未绑定员工档案");
    }
}
