package com.ceams.service;

import com.ceams.model.PageResult;
import com.ceams.model.Pagination;
import com.ceams.model.entity.User;

import java.util.List;

/**
 * 用户服务层接口
 */
public interface UserService {
    /**
     * 根据用户名查找用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    User findByUsername(String username);

    /**
     * 验证密码
     * 
     * @param user        用户信息
     * @param rawPassword 明文密码
     * @return 是否匹配
     */
    boolean checkPassword(User user, String rawPassword);

    /**
     * 注册用户
     * 
     * @param user 用户信息
     */
    void registerUser(User user);

    /**
     * 添加用户
     * 
     * @param user 用户信息
     * @return 是否成功
     */
    boolean addUser(User user);

    /**
     * 更新用户
     * 
     * @param user 用户信息
     * @return 是否成功
     */
    boolean updateUser(User user);

    /**
     * 删除用户
     * 
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Integer userId);

    /**
     * 根据ID获取用户详情
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserDetail(Integer userId);

    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    List<User> getAllUsers();

    /**
     * 分页获取所有用户
     * 
     * @param pagination 分页参数
     * @return 分页结果
     */
    PageResult<User> getUsersByPage(Pagination pagination);

    /**
     * 分页根据关键字搜索用户
     * 
     * @param pagination 分页参数
     * @return 分页结果
     */
    PageResult<User> searchUsersByPage(Pagination pagination);

    /**
     * 根据名称查找用户
     * 
     * @param name 用户名称
     * @return 用户信息
     */
    User findByName(String name);

    /**
     * 根据角色查询用户列表
     * 
     * @param role 用户角色
     * @return 用户列表
     */
    List<User> getUsersByRole(String role);

    /**
     * 用户认证
     * 
     * @param username 用户名
     * @param password 密码
     * @return 认证成功返回用户信息，失败返回null
     */
    User authenticate(String username, String password);

    /**
     * 根据ID获取用户
     * 
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Integer userId);
}
