package com.wkclz.demo.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.UserInfo;
import com.wkclz.core.user.UserContext;
import com.wkclz.demo.entity.User;
import com.wkclz.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserRest {

    @Autowired
    private UserService userService;

    /**
     * 新增用户
     */
    @PostMapping
    public User addUser(@RequestBody User user) {
        setLoginUser();
        int insert = userService.insert(user);
        return user;
    }

    /**
     * 批量新增用户
     */
    @PostMapping("/batch")
    public List<User> addUsers(@RequestBody List<User> users) {
        setLoginUser();
        userService.insertBatch(users);
        return users;
    }

    /**
     * 根据ID删除用户
     */
    @DeleteMapping("/{id}")
    public Integer deleteUser(@PathVariable Long id) {
        setLoginUser();
        User user = new User();
        user.setId(id);
        return userService.deleteById(user);
    }

    /**
     * 根据ID删除用户
     */
    @DeleteMapping
    public Integer deleteUser(@RequestBody User user) {
        Assert.notNull(user.getIds(), "ids 不能为空");
        setLoginUser();
        return userService.deleteByIds(user);
    }

    /**
     * 更新用户信息（全字段）
     */
    @PutMapping
    public Integer updateUser(@RequestBody User user) {
        setLoginUser();
        return userService.updateById(user);
    }

    /**
     * 更新用户信息（部分字段）
     */
    @PatchMapping
    public Integer updateUserSelective(@RequestBody User user) {
        setLoginUser();
        return userService.updateByIdSelective(user);
    }

    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        setLoginUser();
        return userService.selectById(id);
    }

    /**
     * 根据ID列表查询用户
     */
    @GetMapping("/ids")
    public List<User> getUsersByIds(@RequestParam List<Long> ids) {
        setLoginUser();
        return userService.selectByIds(ids);
    }

    /**
     * 查询所有用户
     */
    @GetMapping
    public List<User> getAllUsers() {
        setLoginUser();
        return userService.selectAll();
    }

    /**
     * 根据条件查询用户
     */
    @PostMapping("/list")
    public List<User> getUsersByCondition(@RequestBody User user) {
        setLoginUser();
        return userService.selectByEntity(user);
    }

    /**
     * 分页查询用户
     */
    @PostMapping("/page")
    public PageData<User> getUsersByPage(@RequestBody User user) {
        setLoginUser();
        return userService.selectPage(user);
    }



    private void setLoginUser() {
        UserInfo userinfo = new UserInfo();
        userinfo.setUserCode("userCode");
        userinfo.setUsername("username");
        UserContext.setUserInfo(userinfo);
    }
}
