package com.example.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC通用工具类
 * 包含5个泛型数据库操作方法，实现通用数据库访问
 * 方法1~5严格按照作业题目定义；附带私有辅助方法
 */
public class JDBCTool {

    /**
     * 方法1：将ResultSet结果集转为实体对象List集合
     * @param rs 查询结果集
     * @param clazz 目标实体类Class对象
     * @param <T> 泛型，实体类型
     * @return List<T> 对象列表，属性与数据库字段一一对应
     * @throws Exception 反射、数据库异常抛出
     */
    public static <T> List<T> resultSetToList(ResultSet rs, Class<T> clazz) throws Exception {
        List<T> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        while (rs.next()) {
            T obj = clazz.newInstance();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = metaData.getColumnLabel(i);
                Object value = rs.getObject(i);
                String fieldName = columnToField(columnName);
                setFieldValue(obj, fieldName, value, clazz);
            }
            list.add(obj);
        }
        return list;
    }

    /**
     * 方法2：保存数据，新增记录
     * @param obj 待保存实体对象
     * @param connection 数据库连接
     * @param <T> 泛型实体类型
     * @return int 受影响行数
     * @throws Exception 异常向外抛出
     */
    public static <T> int save(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(getTableName(clazz)).append("(");
        Field[] fields = clazz.getDeclaredFields();
        List<String> colList = new ArrayList<>();
        List<Object> valList = new ArrayList<>();
        for (Field field : fields) {
            field.setAccessible(true);
            Object val = field.get(obj);
            if ("id".equals(field.getName())) continue;
            colList.add(fieldToColumn(field.getName()));
            valList.add(val);
        }
        sql.append(String.join(",", colList));
        sql.append(") VALUES (");
        for (int i = 0; i < colList.size(); i++) {
            if(i>0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        PreparedStatement ps = connection.prepareStatement(sql.toString());
        for (int i = 0; i < valList.size(); i++) {
            ps.setObject(i+1, valList.get(i));
        }
        int affectRows = ps.executeUpdate();
        ps.close();
        return affectRows;
    }

    /**
     * 方法3：更新数据库记录，依据id更新
     * @param obj 封装数据的实体对象（id必须赋值）
     * @param connection 数据库连接
     * @param <T> 泛型实体类型
     * @return int 受影响行数
     * @throws Exception 异常向外抛出
     */
    public static <T> int update(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(getTableName(clazz)).append(" SET ");
        Field[] fields = clazz.getDeclaredFields();
        List<String> setSqlList = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        Long idValue = null;
        for (Field field : fields) {
            field.setAccessible(true);
            Object val = field.get(obj);
            if("id".equals(field.getName())){
                idValue = (Long)val;
                continue;
            }
            setSqlList.add(fieldToColumn(field.getName()) + "=?");
            params.add(val);
        }
        sql.append(String.join(",", setSqlList));
        sql.append(" WHERE id = ?");
        params.add(idValue);
        PreparedStatement ps = connection.prepareStatement(sql.toString());
        for(int i=0;i<params.size();i++){
            ps.setObject(i+1, params.get(i));
        }
        int affectRows = ps.executeUpdate();
        ps.close();
        return affectRows;
    }

    /**
     * 方法4：根据实体id删除记录
     * @param obj 实体对象（id必须赋值）
     * @param connection 数据库连接
     * @param <T> 泛型实体类型
     * @return int 受影响行数
     * @throws Exception 异常向外抛出
     */
    public static <T> int delete(T obj, Connection connection) throws Exception {
        Class<?> clazz = obj.getClass();
        Field idField = clazz.getDeclaredField("id");
        idField.setAccessible(true);
        Long id = (Long) idField.get(obj);
        String sql = "DELETE FROM " + getTableName(clazz) + " WHERE id=?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setObject(1, id);
        int affectRows = ps.executeUpdate();
        ps.close();
        return affectRows;
    }

    /**
     * 方法5：根据id查询单个对象
     * @param id 记录id字符串
     * @param clazz 实体Class对象
     * @param connection 数据库连接
     * @param <T> 泛型实体类型
     * @return T 查询到的对象，找不到返回null
     * @throws Exception 异常向外抛出
     */
    public static <T> T getOneById(String id, Class<T> clazz, Connection connection) throws Exception {
        String sql = "SELECT * FROM " + getTableName(clazz) + " WHERE id = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setObject(1, Long.parseLong(id));
        ResultSet rs = ps.executeQuery();
        List<T> list = resultSetToList(rs, clazz);
        rs.close();
        ps.close();
        return list.size()>0 ? list.get(0) : null;
    }

    // ---------------- 私有辅助方法 ----------------
    /**
     * 辅助：数据库下划线列名转Java驼峰属性名 例 enroll_date → enrollDate
     */
    private static String columnToField(String column) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = false;
        for (char c : column.toCharArray()) {
            if(c == '_'){
                nextUpper = true;
            }else{
                if(nextUpper){
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                }else{
                    sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    /**
     * 辅助：Java驼峰转数据库下划线 例 enrollDate → enroll_date
     */
    private static String fieldToColumn(String fieldName) {
        StringBuilder sb = new StringBuilder();
        for(char c : fieldName.toCharArray()){
            if(Character.isUpperCase(c)){
                sb.append("_").append(Character.toLowerCase(c));
            }else{
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 辅助：获取表名，类名首字母小写 Student → student
     */
    private static String getTableName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    /**
     * 辅助：反射调用setter给对象属性赋值
     */
    private static void setFieldValue(Object obj, String fieldName, Object value, Class<?> clazz) throws Exception {
        Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            return;
        }
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        Method setter = clazz.getDeclaredMethod(setterName, field.getType());
        setter.invoke(obj, value);
    }
}
