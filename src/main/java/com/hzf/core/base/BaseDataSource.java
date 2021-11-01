package com.hzf.core.base;

import com.hzf.core.common.SqlTable;
import com.hzf.core.toolkit.ScanUtil;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.jdbc.Work;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 *使用其他连接池
 */
public abstract class BaseDataSource {

    public Dialect dialect;
    public String[] basePackages;
    public boolean hasDefaultValue = true;
    public boolean hasIndex = true;

    public BaseDataSource(Dialect dialect, String... basePackages) {
        if (dialect != null) {
            this.dialect = dialect;
        }
        this.basePackages = basePackages;
    }

    public String GetLimitString(BaseQuery pageInfo, String sql) {
        String _sql_ = "_sql_";
        RowSelection rowSelection = new RowSelection();
        rowSelection.setFirstRow(pageInfo.PageIndex.intValue());
        rowSelection.setFetchSize(pageInfo.PageSize.intValue());
        String limitSql = dialect.getLimitHandler().processSql(_sql_, rowSelection);
//        String limitSql = dialect.getLimitString(_sqlKey, pageInfo.PageIndex.intValue(), pageInfo.PageSize.intValue());
        int length = limitSql.indexOf("?");
        if (length >= 0) {
            if (limitSql.lastIndexOf("?") == length) {//只有一个参数
                limitSql = limitSql.replace("?", pageInfo.PageSize + "");
            } else {
                limitSql = limitSql.replace("?", pageInfo.PageSize + "");
                limitSql = limitSql.replace("?", pageInfo.PageIndex + "");
            }
        }
        return limitSql.replace(_sql_, sql);
    }

    //扫描包名
    public void ScanPackages() {
        if (basePackages != null) {
            for (String basePackage : basePackages) {
                Set<Class<?>> classes = ScanUtil.getClasses(basePackage);
                for (Class<?> aClass : classes) {
                    if (BaseModel.class.isAssignableFrom(aClass)) {
                        SqlTable.CheckTable((Class<BaseModel>) aClass);
                    }
                }
            }
        }
    }

    private Map<Long, Connection> threadConnectionMap = new HashMap<>();

    public void setAutoCommit() throws SQLException {
        Connection connection = getConnection();
        connection.setAutoCommit(false);
        threadConnectionMap.put(Thread.currentThread().getId(), connection);
    }

    public Connection GetThreadConnection() throws SQLException {
        Connection connection = getConnection();
        threadConnectionMap.put(Thread.currentThread().getId(), connection);
        return connection;
    }

    public void CloseThreadConnection() {
        try {
            Connection connection = threadConnectionMap.remove(Thread.currentThread().getId());
            this.close(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void commit() throws SQLException {
        Connection connection = threadConnectionMap.remove(Thread.currentThread().getId());
        if (connection != null) {
            connection.commit();
            this.close(connection);
        }
    }

    public void rollback() throws SQLException {
        Connection connection = threadConnectionMap.remove(Thread.currentThread().getId());
        if (connection != null) {
            connection.rollback();
            this.close(connection);
        }
    }

    public void close(Connection connection) throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public void closeDataSource() {
    }

    public Connection getConnection() throws SQLException {
        Connection connection = threadConnectionMap.get(Thread.currentThread().getId());
        if (connection != null) {
            return connection;
        } else {
            return newConnection();
        }
    }

    public void doWork(Work work) throws SQLException {
        Connection con = null;
        boolean autoCommit = true;
        boolean existsConnection = false;
        try {
            existsConnection = threadConnectionMap.containsKey(Thread.currentThread().getId());
            con = getConnection();
            autoCommit = con.getAutoCommit();
            work.execute(con);
        } catch (SQLException e) {
            throw e;
        } finally {
            if (!existsConnection && autoCommit) {
                close(con);
            }
        }
    }

    public abstract Connection newConnection() throws SQLException;

}
