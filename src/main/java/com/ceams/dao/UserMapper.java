package com.ceams.dao;

import com.ceams.model.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    /**
     * 根据ID查询用户
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    User selectById(Integer id);

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    User selectByUsername(String username);

    /**
     * 插入用户
     * 
     * @param user 用户信息
     * @return 影响行数
     */
    int insert(User user);

    /**
     * 更新用户
     * 
     * @param user 用户信息
     * @return 影响行数
     */
    int update(User user);

    /**
     * 删除用户
     * 
     * @param id 用户ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 查询所有用户
     * 
     * @return 用户列表
     */
    List<User> selectAll();

    /**
     * 根据姓名查询用户
     * 
     * @param name 用户名
     * @return 用户信息
     */
    User selectByName(String name);

    /**
     * 分页查询所有用户
     * 
     * @param pagination 分页参数
     * @return 用户列表
     */
    List<User> selectByPage(com.ceams.model.Pagination pagination);

    /**
     * 统计所有用户数量
     * 
     * @return 用户数量
     */
    long count();

    /**
     * 分页根据关键字搜索用户
     * 
     * @param pagination 分页参数
     * @return 用户列表
     */
    List<User> searchByPage(com.ceams.model.Pagination pagination);

    /**
     * 统计根据关键字搜索的用户数量
     * 
     * @param keyword 搜索关键字
     * @return 用户数量
     */
    long countByKeyword(String keyword);

    /**
     * 根据角色查询用户列表
     * 
     * @param role 用户角色
     * @return 用户列表
     */
    List<User> selectByRole(String role);
}