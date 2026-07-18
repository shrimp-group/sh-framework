package com.wkclz.demo.rest;

import com.wkclz.core.base.PageData;
import com.wkclz.core.base.R;
import com.wkclz.core.exception.NotFoundException;
import com.wkclz.core.identity.IdentityContext;
import com.wkclz.core.identity.UserIdentity;
import com.wkclz.demo.bean.entity.User;
import com.wkclz.demo.bean.vo.user.*;
import com.wkclz.demo.service.UserService;
import com.wkclz.web.bean.IdReq;
import com.wkclz.web.bean.RemoveReq;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "1.用户管理", description = "用户增删改查接口")
@RestController
@RequestMapping(Route.PREFIX)
public class UserRest {

    @Autowired
    private UserService userService;

    @Operation(summary = "1.用户-分页查询", description = "根据条件分页查询用户列表")
    @GetMapping(Route.USER_PAGE)
    public R<PageData<UserPageResp>> userPage(@Valid UserPageReq req) {
        setLoginUser();
        User user = new User();
        BeanUtils.copyProperties(req, user);
        PageData<User> page = userService.selectPage(user);
        List<UserPageResp> list = page.getRecords().stream().map(t -> {
            UserPageResp resp = new UserPageResp();
            BeanUtils.copyProperties(t, resp);
            return resp;
        }).toList();
        PageData<UserPageResp> convert = PageData.convert(page, list);
        return R.ok(convert);
    }

    @Operation(summary = "2.用户-详情", description = "根据ID查询用户详情")
    @GetMapping(Route.USER_INFO)
    public R<UserResp> userInfo(@Valid IdReq req) {
        setLoginUser();
        User user = userService.selectById(req.getId());
        if (user == null) {
            throw NotFoundException.of("用户不存在，ID: {}", req.getId());
        }
        UserResp resp = new UserResp();
        BeanUtils.copyProperties(user, resp);
        return R.ok(resp);
    }

    @Operation(summary = "3.用户-创建", description = "创建新用户")
    @PostMapping(Route.USER_CREATE)
    public R<UserResp> userCreate(@Valid @RequestBody UserCreateReq req) {
        setLoginUser();
        User user = new User();
        BeanUtils.copyProperties(req, user);
        userService.insert(user);
        UserResp resp = new UserResp();
        BeanUtils.copyProperties(user, resp);
        return R.ok(resp);
    }

    @Operation(summary = "4.用户-更新", description = "更新用户信息（需要版本号）")
    @PostMapping(Route.USER_UPDATE)
    public R<Integer> userUpdate(@Valid @RequestBody UserUpdateReq req) {
        setLoginUser();
        User user = new User();
        BeanUtils.copyProperties(req, user);
        int i = userService.updateByIdSelective(user);
        return R.ok(i);
    }

    @Operation(summary = "5.用户-删除", description = "根据ID删除用户，支持单个和批量")
    @PostMapping(Route.USER_REMOVE)
    public R<Integer> userRemove(@Valid @RequestBody RemoveReq req) {
        setLoginUser();
        if (req.getIds() != null && !req.getIds().isEmpty()) {
            return R.ok(userService.deleteByIds(req.getIds()));
        }
        return R.ok(userService.deleteById(req.getId()));
    }

    private void setLoginUser() {
        UserIdentity id = new UserIdentity();
        id.setUserCode("userCode");
        id.setUsername("username");
        IdentityContext.set(id, null);
    }
}
