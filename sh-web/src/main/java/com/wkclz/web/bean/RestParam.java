package com.wkclz.web.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * REST 接口参数元数据
 *
 * @author shrimp
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestParam implements Serializable {

    /**
     * 参数名称
     */
    private String name;

    /**
     * 参数类型（完整类名）
     */
    private String type;

    /**
     * 参数注解类型（如 RequestBody、PathVariable、RequestParam）
     */
    private String annotationType;

    /**
     * 是否必需
     */
    private Boolean required;

    /**
     * 参数默认值（仅 @RequestParam 支持）
     */
    private String defaultValue;

    /**
     * 泛型参数类型列表（用于复杂类型）
     */
    private List<String> genericTypes;

    /**
     * 参数描述（来自 @Schema.description）
     */
    private String description;

    /**
     * 参数示例值（来自 @Schema.example）
     */
    private String example;

    /**
     * 参数必填模式（来自 @Schema.requiredMode）
     */
    private String requiredMode;

    /**
     * 复杂参数类型的字段结构列表（递归扫描）
     */
    private List<RestField> fields;

}