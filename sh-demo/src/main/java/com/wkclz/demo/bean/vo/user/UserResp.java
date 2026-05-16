package com.wkclz.demo.bean.vo.user;

import com.wkclz.web.bean.EntityResp;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "用户响应")
public class UserResp extends EntityResp {

    @Schema(description = "用户编码", example = "U001")
    private String userCode;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @Schema(description = "用户状态：1-启用，0-禁用", example = "1")
    private Integer userStatus;


}
