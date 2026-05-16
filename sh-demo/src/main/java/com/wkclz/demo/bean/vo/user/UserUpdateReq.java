package com.wkclz.demo.bean.vo.user;

import com.wkclz.web.bean.UpdateReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "更新用户请求")
public class UserUpdateReq extends UpdateReq {

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @Schema(description = "用户状态：1-启用，0-禁用", example = "1")
    private Integer userStatus;

}
