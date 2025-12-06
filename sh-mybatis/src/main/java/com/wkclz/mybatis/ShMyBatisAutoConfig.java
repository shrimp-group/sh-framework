package com.wkclz.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@MapperScan({"com.wkclz.mybatis.mapper"})
@ComponentScan(basePackages = {"com.wkclz.mybatis"})
public class ShMyBatisAutoConfig {
}


