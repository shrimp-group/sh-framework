package com.wkclz.demo.rest;

import com.wkclz.core.annotation.ApiDesc;
import com.wkclz.core.annotation.Router;

@Router(module = Route.PREFIX, prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/sh-demo";

    @ApiDesc("1. 用户-分页查询")
    String USER_PAGE = "/user/page";
    @ApiDesc("2. 用户-详情")
    String USER_INFO = "/user/info";
    @ApiDesc("3. 用户-新增")
    String USER_CREATE = "/user/create";
    @ApiDesc("4. 用户-批量新增")
    String USER_CREATE_BATCH = "/user/create/batch";
    @ApiDesc("5. 用户-更新")
    String USER_UPDATE = "/user/update";
    @ApiDesc("6. 用户-删除")
    String USER_REMOVE = "/user/remove";

}
