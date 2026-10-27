package com.ceams.service.impl;

import com.ceams.dao.UserMapper;
import com.ceams.model.entity.User;
import com.ceams.service.UserService;
import com.ceams.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 用户服务层实现类
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User findByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public boolean checkPassword(User user, String rawPassword) {
        return PasswordUtil.matches(rawPassword, user.getPassword());
    }

    @Override
    public void registerUser(User user) {
        // 注册方法需要自己处理密码编码，因为调用方可能直接传入明文密码
        String encodedPassword = PasswordUtil.encode(user.getPassword());
        user.setPassword(encodedPassword);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        userMapper.insert(user);
    }

    @Override
    public boolean addUser(User user) {
        // 密码已经在控制器中编码，不需要再次编码
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        return userMapper.insert(user) > 0;
    }

    @Override
    public boolean updateUser(User user) {
        // 密码已经在控制器中编码，不需要再次编码
        user.setUpdateTime(new Date());
        return userMapper.update(user) > 0;
    }

    @Override
    public boolean deleteUser(Integer userId) {
        return userMapper.deleteById(userId) > 0;
    }

    @Override
    public User getUserDetail(Integer userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    @Override
    public User findByName(String name) {
        return userMapper.selectByName(name);
    }

    @Override
    public com.ceams.model.PageResult<User> getUsersByPage(com.ceams.model.Pagination pagination) {
        List<User> list = userMapper.selectByPage(pagination);
        long total = userMapper.count();
        return new com.ceams.model.PageResult<>(list, total, pagination.getPageNum(), pagination.getPageSize());
    }

    @Override
    public com.ceams.model.PageResult<User> searchUsersByPage(com.ceams.model.Pagination pagination) {
        List<User> list = userMapper.searchByPage(pagination);
        long total = userMapper.countByKeyword(pagination.getKeyword());
        return new com.ceams.model.PageResult<>(list, total, pagination.getPageNum(), pagination.getPageSize());
    }

    @Override
    public List<User> getUsersByRole(String role) {
        return userMapper.selectByRole(role);
    }

    @Override
    public User authenticate(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user != null && PasswordUtil.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    public User getUserById(Integer userId) {
        return userMapper.selectById(userId);
    }
}
