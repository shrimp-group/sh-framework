package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Provider 单元测试实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TestEntity extends BaseEntity {

    private String name;

}
