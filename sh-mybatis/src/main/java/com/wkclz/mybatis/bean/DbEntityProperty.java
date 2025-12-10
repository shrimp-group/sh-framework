package com.wkclz.mybatis.bean;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.mybatis.annotation.Blob;
import com.wkclz.tool.bean.JavaField;
import com.wkclz.tool.utils.StringUtil;
import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class DbEntityProperty implements Serializable {

    private final static List<String> BASE_IGNORE_FIELDS = List.of("ids");
    private final static List<String> INSERT_IGNORE_FIELDS = List.of("id", "createTime", "updateTime", "version");
    private final static List<String> UPDATE_IGNORE_FIELDS = List.of("id", "createBy", "createTime", "updateTime", "version");


    public static final String PRIMARY_KEY = "id";
    public static final String DELETED_FIELD = "deleted";
    public static final String VERSION_FIELD = "version";
    public static final String CREATE_TIME_FIELD = "create_time";
    public static final String UPDATE_TIME_FIELD = "update_time";
    public static final String UPDATE_BY_FIELD = "update_by";

    private String tableName;
    private String entityName;
    private Class<?> entityClass;
    private List<JavaField> fields;
    private JavaField idField;
    private JavaField idsField;
    private JavaField createByField;
    private JavaField updateByField;
    private JavaField versionByField;
    private List<JavaField> insertFields;
    private List<JavaField> updateFields;
    private List<JavaField> selectObjFields;
    private List<JavaField> selectListFields;

    public static DbEntityProperty createInstance(Class<?> entityClass) {
        if (entityClass == null) {
            return null;
        }

        DbEntityProperty property = new DbEntityProperty();
        property.setEntityClass(entityClass);
        property.setEntityName(entityClass.getSimpleName());
        property.setTableName(StringUtil.camelToUnderline(property.getEntityName()));
        property.setFields(getProperty(entityClass));

        List<JavaField> insertFields = new ArrayList<>();
        List<JavaField> updateFields = new ArrayList<>();
        List<JavaField> selectListFields = new ArrayList<>();
        List<JavaField> selectObjFields = new ArrayList<>();
        for (JavaField field : property.getFields()) {
            String fieldName = field.getFieldName();
            if ("id".equals(fieldName)) {
                property.setIdField( field);
            }
            if ("ids".equals(fieldName)) {
                property.setIdsField( field);
            }
            if ("createBy".equals(fieldName)) {
                property.setCreateByField( field);
            }
            if ("updateBy".equals(fieldName)) {
                property.setUpdateByField( field);
            }
            if ("version".equals(fieldName)) {
                property.setVersionByField( field);
            }

            if (BASE_IGNORE_FIELDS.contains(fieldName)) {
                continue;
            }
            if (!INSERT_IGNORE_FIELDS.contains(fieldName)) {
                insertFields.add(field);
            }
            if (!UPDATE_IGNORE_FIELDS.contains(fieldName)) {
                updateFields.add(field);
            }
            Blob annotation = field.getField().getAnnotation(Blob.class);
            if (annotation == null) {
                selectListFields.add(field);
            }
            selectObjFields.add(field);
        }
        property.setInsertFields(insertFields);
        property.setUpdateFields(updateFields);
        property.setSelectListFields(selectListFields);
        property.setSelectObjFields(selectObjFields);

        return property;
    }



    private static List<JavaField> getProperty(Class<?> entityClass) {

        Map<String, Field> fieldMap = new LinkedHashMap<>();
        // 1. 获取当前类的所有字段（全部保留）
        for (Field field : entityClass.getDeclaredFields()) {
            fieldMap.put(field.getName(), field);
        }
        // 2. 获取所有父类的字段，直到Object.class，但跳过BaseEntity类的字段
        Class<?> superClass = entityClass.getSuperclass();
        while (superClass != null && !superClass.equals(Object.class)) {
            // 跳过BaseEntity类的字段
            if (!superClass.equals(BaseEntity.class)) {
                // 获取当前父类的所有字段
                for (Field field : superClass.getDeclaredFields()) {
                    // 只有当当前字段映射中不存在该字段名时，才添加（子类字段优先）
                    if (!fieldMap.containsKey(field.getName())) {
                        fieldMap.put(field.getName(), field);
                    }
                }
            }
            // BaseEntity 只要 ids
            if (superClass.equals(BaseEntity.class)) {
                // 获取当前父类的所有字段
                for (Field field : superClass.getDeclaredFields()) {
                    // 只有当当前字段映射中不存在该字段名时，才添加（子类字段优先）
                    if ("ids".equals(field.getName())) {
                        fieldMap.put(field.getName(), field);
                    }
                }
            }
            // 继续遍历下一个父类
            superClass = superClass.getSuperclass();
        }

        // 将Field对象转换为JavaField对象
        List<JavaField> javaFields = new ArrayList<>();
        for (Field field : fieldMap.values()) {
            field.setAccessible(true);
            JavaField javaField = new JavaField();
            javaField.setFieldName(field.getName());
            javaField.setColumnName(StringUtil.camelToUnderline(field.getName()));
            javaField.setField(field);
            javaField.setClazz(field.getType());
            
            // 获取getter和setter方法
            try {
                String fieldName = field.getName();
                String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                String setterName = "set" + getterName.substring(3);
                try {
                    Method getter = entityClass.getMethod(getterName);
                    javaField.setGetter(getter);
                } catch (NoSuchMethodException e) {
                    // 忽略没有getter方法的字段
                }
                try {
                    Method setter = entityClass.getMethod(setterName, field.getType());
                    javaField.setSetter(setter);
                } catch (NoSuchMethodException e) {
                    // 忽略没有setter方法的字段
                }
            } catch (SecurityException e) {
                // 忽略安全异常
            }
            javaFields.add(javaField);
        }
        return javaFields;
    }



}
