package com.wkclz.demo.bean.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "创建用户请求")
public class UserCreateReq implements Serializable {

    @Schema(description = "用户编码，为空时自动生成", example = "U001")
    private String userCode;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Schema(description = "昵称", example = "管理员")
    private String nickname;

    @NotNull(message = "用户状态不能为空")
    @Schema(description = "用户状态：1-启用，0-禁用", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer userStatus;

}
