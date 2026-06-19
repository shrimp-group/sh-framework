package com.wkclz.tool.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 泛型类型信息
 * 用于描述泛型类型的结构信息
 *
 * @author shrimp
 */
@Data
public class GenericTypeInfo implements Serializable {

    /**
     * 原始类型（完整类名）
     */
    private String rawType;

    /**
     * 类型参数列表
     */
    private List<GenericTypeInfo> typeArgs;

    public GenericTypeInfo() {
        this.typeArgs = new ArrayList<>();
    }

}
