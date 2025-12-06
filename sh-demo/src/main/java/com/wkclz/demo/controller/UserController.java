package com.wkclz.demo.controller;

import com.wkclz.core.base.PageData;
import com.wkclz.demo.entity.User;
import com.wkclz.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 新增用户
     */
    @PostMapping
    public Integer addUser(@RequestBody User user) {
        return userService.insert(user);
    }

    /**
     * 批量新增用户
     */
    @PostMapping("/batch")
    public Integer addUsers(@RequestBody List<User> users) {
        return userService.insertBatch(users);
    }

    /**
     * 根据ID删除用户
     */
    @DeleteMapping("/{id}")
    public Integer deleteUser(@PathVariable Long id) {
        User user = new User();
        user.setId(id);
        return userService.deleteById(user);
    }

    /**
     * 更新用户信息（全字段）
     */
    @PutMapping
    public Integer updateUser(@RequestBody User user) {
        return userService.updateById(user);
    }

    /**
     * 更新用户信息（部分字段）
     */
    @PatchMapping
    public Integer updateUserSelective(@RequestBody User user) {
        return userService.updateByIdSelective(user);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.selectById(id);
    }

    /**
     * 根据ID列表查询用户
     */
    @GetMapping("/ids")
    public List<User> getUsersByIds(@RequestParam List<Long> ids) {
        return userService.selectByIds(ids);
    }

    /**
     * 查询所有用户
     */
    @GetMapping
    public List<User> getAllUsers() {
        return userService.selectAll();
    }

    /**
     * 根据条件查询用户
     */
    @PostMapping("/list")
    public List<User> getUsersByCondition(@RequestBody User user) {
        return userService.selectByEntity(user);
    }

    /**
     * 分页查询用户
     */
    @PostMapping("/page")
    public PageData<User> getUsersByPage(@RequestBody User user) {
        return userService.selectPage(user);
    }
}
