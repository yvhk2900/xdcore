package com.hzf.core.common;

import com.hzf.core.base.BaseDataSource;
import org.hibernate.dialect.MySQL55Dialect;
import org.hibernate.dialect.MySQLDialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDataSource extends BaseDataSource {

    //连接打开后一直不关闭
    private Connection conn;

    public MySQLDataSource(String... basePackages) {
        super(new MySQL55Dialect(), basePackages);
    }

    public MySQLDataSource init(String url, String username, String password) {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public Connection newConnection() throws SQLException {
        return conn;
    }

    @Override
    public void close(Connection connection) throws SQLException {
    }

    @Override
    public void closeDataSource() {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
