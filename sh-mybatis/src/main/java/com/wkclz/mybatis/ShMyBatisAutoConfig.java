package com.wkclz.mybatis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.mybatis"})
@MapperScan(basePackages = {"com.wkclz.mybatis.mapper"})
public class ShMyBatisAutoConfig {
}


