package com.wkclz.demo.service.impl;

import com.wkclz.demo.entity.User;
import com.wkclz.demo.mapper.UserMapper;
import com.wkclz.demo.service.UserService;
import com.wkclz.mybatis.mapper.BaseMapper;
import com.wkclz.mybatis.service.impl.BaseServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends BaseServiceImpl<User> implements UserService {

    @Resource
    private UserMapper userMapper;

    @Override
    public BaseMapper<User> getMapper() {
        return userMapper;
    }

}
