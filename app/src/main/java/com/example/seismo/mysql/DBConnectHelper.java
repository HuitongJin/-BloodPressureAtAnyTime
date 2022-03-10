package com.example.seismo.mysql;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnectHelper {
    private static String driver = "com.mysql.jdbc.Driver";
    // 加入utf-8编码，避免中文乱码
    private static String url = "jdbc:mysql://10.0.2.2:3306/bp?characterEncoding=utf-8" + "&serverTimezone=PDT";
    //private static String url = "jdbc:mysql://172.20.10.2:3306/bp?characterEncoding=utf-8" + "&serverTimezone=PDT";
    private static String user = "root"; // 用户名
    private static String password = "030889pioneer"; // 密码

    /*
     * 连接数据库
     **/
    public static Connection getConn() {
        Connection conn = null;
        try{
            Class.forName(driver);
            conn = (Connection) DriverManager.getConnection(url, user, password);
        }catch (ClassNotFoundException e) {
            e.printStackTrace();
        }catch (SQLException e) {
            e.printStackTrace();
        }
        if (conn == null)
           Log.e("print", "conn is null!!!!!!!");
        return conn;
    }
}