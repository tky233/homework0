package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import com.example.entity.Department;
import com.example.entity.Student;
import com.example.util.JDBCTool;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
public class DateTest {
    public static void main(String[] args) throws Exception{
        String url = "jdbc:mysql://localhost:3306/DateTest?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true";

        String user="root";
        String password="123456";

        Connection conn = DriverManager.getConnection(url,user,password);

        // ====================== 【学生表 student 增删改查】 ======================
        System.out.println("----------学生表操作----------");
        //1新增
        Student s = new Student();
        s.setName("张三");
        s.setMajor("计算机科学与技术");
        s.setAge(20);
        s.setEnrollDate(new java.util.Date());
        s.setIsGraduated(0);
        s.setTuition(new BigDecimal("5800.00"));
        int saveStu = JDBCTool.save(s, conn);
        System.out.println("学生新增，受影响行数：" + saveStu);

        //2按id查询单个
        Student stuOne = JDBCTool.getOneById("1", Student.class, conn);
        System.out.println("按id查询学生：" + stuOne);

        //3更新
        stuOne.setAge(21);
        stuOne.setMajor("计科2402");
        int updateStu = JDBCTool.update(stuOne, conn);
        System.out.println("学生更新，受影响行数：" + updateStu);

        //4查询全部
        Statement stmt1 = conn.createStatement();
        ResultSet rsStu = stmt1.executeQuery("select * from student");
        List<Student> stuList = JDBCTool.resultSetToList(rsStu, Student.class);
        System.out.println("查询全部学生：" + stuList);
        rsStu.close();
        stmt1.close();

        //5删除
        int delStu = JDBCTool.delete(stuOne, conn);
        System.out.println("学生删除，受影响行数：" + delStu);


        // ======================【学院表 department 增删改查】 ======================
        System.out.println("\n----------学院表操作----------");
        //1新增
        Department dept = new Department();
        dept.setName("信息科学与工程学院");
        dept.setCode("DX001");
        int saveDept = JDBCTool.save(dept, conn);
        System.out.println("学院新增，受影响行数：" + saveDept);

        //2按id查询单个
        Department deptOne = JDBCTool.getOneById("1", Department.class, conn);
        System.out.println("按id查询学院：" + deptOne);

        //3更新
        deptOne.setName("信息学院");
        int updateDept = JDBCTool.update(deptOne, conn);
        System.out.println("学院更新，受影响行数：" + updateDept);

        //4查询全部
        Statement stmt2 = conn.createStatement();
        ResultSet rsDept = stmt2.executeQuery("select * from department");
        List<Department> deptList = JDBCTool.resultSetToList(rsDept, Department.class);
        System.out.println("查询全部学院：" + deptList);
        rsDept.close();
        stmt2.close();

        //5删除
        int delDept = JDBCTool.delete(deptOne, conn);
        System.out.println("学院删除，受影响行数：" + delDept);
        conn.close();
    }
}
