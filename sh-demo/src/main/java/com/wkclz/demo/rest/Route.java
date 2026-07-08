package com.wkclz.demo.rest;

import com.wkclz.core.annotation.Router;

@Router(module = Route.PREFIX, prefix = Route.PREFIX)
public interface Route {

    String PREFIX = "/sh-demo";

    String USER_PAGE = "/user/page";
    String USER_INFO = "/user/info";
    String USER_CREATE = "/user/create";
    String USER_CREATE_BATCH = "/user/create/batch";
    String USER_UPDATE = "/user/update";
    String USER_REMOVE = "/user/remove";

}
