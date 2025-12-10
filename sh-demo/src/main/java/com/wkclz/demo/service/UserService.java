package com.wkclz.demo.service;

import com.wkclz.demo.entity.User;
import com.wkclz.demo.mapper.UserMapper;
import com.wkclz.mybatis.service.BaseService;
import org.springframework.stereotype.Service;

@Service
public class UserService extends BaseService<User, UserMapper> {

}
