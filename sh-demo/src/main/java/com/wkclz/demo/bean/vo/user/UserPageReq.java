package com.wkclz.demo.bean.vo.user;

import com.wkclz.web.bean.PageReq;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户分页查询请求")
public class UserPageReq extends PageReq {

    @Schema(description = "用户编码,模糊搜索", example = "U001")
    private String userCode;

    @Schema(description = "用户名,模糊搜索", example = "admin")
    private String username;

    @Schema(description = "昵称,模糊搜索", example = "管理员")
    private String nickname;

    @Schema(description = "用户状态：1-启用，0-禁用", example = "1")
    private Integer userStatus;

}
