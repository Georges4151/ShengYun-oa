package com.ceams.module.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ceams.module.bank.entity.BankStatement;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 银行流水 Mapper。 */
@Mapper
public interface BankStatementMapper extends BaseMapper<BankStatement> {

    /** 加行锁读取账户下未对账流水，串行化并发对账，避免重复匹配。 */
    @Select(
            "SELECT * FROM biz_bank_statement "
                    + "WHERE account_id = #{accountId} AND status = 'UNRECONCILED' "
                    + "AND deleted = 0 FOR UPDATE")
    List<BankStatement> selectUnreconciledForUpdate(@Param("accountId") Long accountId);
}
