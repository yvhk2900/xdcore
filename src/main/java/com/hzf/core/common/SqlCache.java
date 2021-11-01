package com.hzf.core.common;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hzf.core.base.*;
import com.hzf.core.toolkit.StrUtil;

import javax.persistence.Column;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//缓存管理,单系统暂时用内存管理,多系统,则必须用redis
public class SqlCache {


    public static Map<String, Class<?>> hashMapClasses = new ConcurrentHashMap<>();

    public static Map<String, Class<?>> hashMapController = new ConcurrentHashMap<>();
    public static Map<String, Class<?>> hashMapStatistic = new ConcurrentHashMap<>();
    public static Map<String, Class<?>> hashMapTask = new ConcurrentHashMap<>();

    private static Map<Class<?>, String> tableNameMap = new ConcurrentHashMap<>();

    private static Map<Class<?>, String> insertSqlMap = new ConcurrentHashMap<>();
    private static Map<Class<?>, List<String>> insertSqlColumnMap = new ConcurrentHashMap<>();

    private static Map<Class<?>, String> updateSqlMap = new ConcurrentHashMap<>();
    private static Map<Class<?>, List<String>> updateSqlColumnMap = new ConcurrentHashMap<>();

    public static Map<Class<?>, Map<String, Field>> columnFieldMap = new ConcurrentHashMap<>();// 记录着本对象所有字段和它对应的类型



    public static void RemoveClass(Class clazz) {
        insertSqlMap.remove(clazz);
        insertSqlColumnMap.remove(clazz);
        updateSqlMap.remove(clazz);
        updateSqlColumnMap.remove(clazz);
        columnFieldMap.remove(clazz);
    }

    public static void AddClass(Class<?> aClass) {
        Class<BaseModel> ac = (Class<BaseModel>) aClass;
        hashMapClasses.put(BaseModel.GetTableName(ac), aClass);
    }
    public static void AddController(Class<?> aClass){
        Class<BaseModelController> ac = (Class<BaseModelController>) aClass;
        String sn = ac.getSimpleName();
        String n = sn.replace("Controller","");
        XController xc = ac.getAnnotation(XController.class);
        if(xc!=null){
            hashMapController.put(n,ac);
        }
    }
    public static void AddStatistic(Class<?> aClass){
        Class<BaseStatistic> ac = (Class<BaseStatistic>) aClass;
        String sn = ac.getSimpleName();
        XStatistic xc = ac.getAnnotation(XStatistic.class);
        if(xc!=null){
        }
        hashMapController.put(sn,ac);
    }
    public static void AddTask(Class<?> aClass){
        Class<BaseTask> ac = (Class<BaseTask>) aClass;
        String sn = ac.getSimpleName();
        XTask xc = ac.getAnnotation(XTask.class);
        if(xc!=null){
        }
        hashMapController.put(sn,ac);
    }

    public static Class<?> GetClassByTableName(String table) {
        if (hashMapClasses.containsKey(table)) {
            return hashMapClasses.get(table);
        }
        return null;
    }

    public static void SetTableName(Class<?> aClass, String tableName) {
        tableNameMap.put(aClass, tableName);
    }

    public static String GetTableName(Class<?> aClass) {
        if (tableNameMap.containsKey(aClass)) {
            return tableNameMap.get(aClass);
        }
        return null;
    }

    public static void AddInsertSql(Class clazz, String sql, List<String> insertColumns) {
        insertSqlMap.put(clazz, sql);
        insertSqlColumnMap.put(clazz, insertColumns);
    }

    public static String GetInsertSql(Class clazz) {
        if (!insertSqlMap.containsKey(clazz)) {
            return null;
        }
        return insertSqlMap.get(clazz);
    }

    public static List<String> GetInsertColumns(Class clazz) {
        if (!insertSqlColumnMap.containsKey(clazz)) {
            return null;
        }
        return insertSqlColumnMap.get(clazz);
    }


    public static void AddUpdateSql(Class clazz, String sql, List<String> insertColumns) {
        updateSqlMap.put(clazz, sql);
        updateSqlColumnMap.put(clazz, insertColumns);
    }

    public static String GetUpdateSql(Class clazz) {
        if (!updateSqlMap.containsKey(clazz)) {
            return null;
        }
        return updateSqlMap.get(clazz);
    }

    public static List<String> GetUpdateColumns(Class clazz) {
        if (!updateSqlColumnMap.containsKey(clazz)) {
            return null;
        }
        return updateSqlColumnMap.get(clazz);
    }


    // 获取本对象所有字段名称和它对应的类型Map
    public static <T extends BaseModel> Map<String, Field> GetColumnFieldMap(Class<T> clazz) {
        if (columnFieldMap.containsKey(clazz)) {
            return columnFieldMap.get(clazz);
        }
        ConcurrentHashMap<String, Field> typeHashMap = new ConcurrentHashMap<>();
        ArrayList<Field> list = new ArrayList<>();
        list.addAll(Arrays.asList(clazz.getFields()));
        list.addAll(Arrays.asList(clazz.getDeclaredFields()));
        for (Field field : list) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            XColumn xc = field.getAnnotation(XColumn.class);

            String columnName = field.getName();
            if (xc != null) {
                if (StrUtil.isNotEmpty(xc.name())) {
                    columnName = xc.name();
                }
                typeHashMap.put(columnName, field);
            } else {
                try {
                    Method method = clazz.getMethod("get" + columnName);
                    if (method != null) {
                        Column column = method.getAnnotation(Column.class);
                        if (StrUtil.isNotEmpty(column.name())) {
                            columnName = column.name();
                        }
                        typeHashMap.put(columnName, field);
                    }
                } catch (NoSuchMethodException e) {
                }
            }
        }
        columnFieldMap.put(clazz, typeHashMap);
        return columnFieldMap.getOrDefault(clazz, null);
    }




}
