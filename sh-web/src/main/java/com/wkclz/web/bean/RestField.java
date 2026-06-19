package com.wkclz.web.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * REST 接口字段结构描述
 * 用于描述复杂类型的字段结构，支持递归嵌套
 *
 * @author shrimp
 */
@Data
public class RestField implements Serializable {

    /**
     * 字段名称
     */
    private String name;

    /**
     * 字段类型（完整类名，简单类型如 java.lang.String 或复杂类型类名）
     */
    private String type;

    /**
     * 字段描述（来自 @Schema.description）
     */
    private String description;

    /**
     * 示例值（来自 @Schema.example）
     */
    private String example;

    /**
     * 是否必填（来自 @Schema.requiredMode）
     */
    private Boolean required;

    /**
     * 泛型参数类型列表（如 List&lt;String&gt; 中的 String）
     */
    private List<String> genericTypes;

    /**
     * 子字段（如果是非简单类型，递归扫描其字段）
     */
    private List<RestField> fields;

    /**
     * 是否为自引用类型（用于防止无限递归）
     */
    private Boolean selfReferencing;

    /**
     * 是否为简单类型
     */
    private Boolean simpleType;

    public RestField() {
        this.genericTypes = new ArrayList<>();
        this.fields = new ArrayList<>();
    }

}
