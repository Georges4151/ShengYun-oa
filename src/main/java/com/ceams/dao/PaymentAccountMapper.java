package com.ceams.dao;

import com.ceams.model.entity.PaymentAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentAccountMapper {
    /**
     * 根据ID查询账户
     * 
     * @param id 账户ID
     * @return 账户信息
     */
    PaymentAccount selectById(Integer id);

    /**
     * 查询所有账户
     * 
     * @return 账户列表
     */
    List<PaymentAccount> selectAll();

    /**
     * 根据类型查询账户
     * 
     * @param type 账户类型
     * @return 账户列表
     */
    List<PaymentAccount> selectByType(@Param("type") String type);

    /**
     * 插入账户
     * 
     * @param account 账户信息
     * @return 影响行数
     */
    int insert(PaymentAccount account);

    /**
     * 更新账户
     * 
     * @param account 账户信息
     * @return 影响行数
     */
    int update(PaymentAccount account);

    /**
     * 更新账户余额
     * 
     * @param id     账户ID
     * @param amount 变动金额（正数增加，负数减少）
     * @return 影响行数
     */
    int updateBalance(@Param("id") Integer id, @Param("amount") java.math.BigDecimal amount);

    /**
     * 删除账户
     * 
     * @param id 账户ID
     * @return 影响行数
     */
    int deleteById(Integer id);
}