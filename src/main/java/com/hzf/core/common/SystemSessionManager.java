package com.hzf.core.common;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.hzf.core.base.BaseDataSource;
import com.hzf.core.base.BaseModel;
import com.hzf.core.toolkit.LogUtil;
import com.hzf.core.toolkit.ScanUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;

import javax.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class SystemSessionManager {
    public static Map<Long, BaseDataSource> threadDataSourceMap = new ConcurrentHashMap<>();//线程安全
    public static Map<Long, Boolean> threadMap = new ConcurrentHashMap<>();//线程安全
    public static BaseDataSource defaultDataSource;//默认数据源
    private Session session = null;

    public static void OpenMysqlDataSource(String url, String username, String password) {
        SystemSessionManager.setThreadDataSource(new MySQLDataSource().init(url, username, password));
    }

//    public static void OpenSqliteDataSource(String path) {
//        SystemSessionManager.setThreadDataSource(new SqliteDataSource().init(path));
//    }

    public static void OpenDataSource(BaseDataSource dataSource) {
        SystemSessionManager.setThreadDataSource(dataSource);
    }

    public static void IsSelfConnection(boolean isSelf) {
        threadMap.put(Thread.currentThread().getId(), isSelf);
    }

    public static void CloseSelfDataSource() {
        BaseDataSource dataSource = SystemSessionManager.getThreadDataSource();
        if (dataSource != null) {
            dataSource.closeDataSource();
            SystemSessionManager.removeThreadDataSource();
        }
    }

    public static void setThreadDataSource(BaseDataSource dataSource) {
        threadDataSourceMap.put(Thread.currentThread().getId(), dataSource);
    }

    public static void removeThreadDataSource() {
        threadMap.remove(Thread.currentThread().getId());
        threadDataSourceMap.remove(Thread.currentThread().getId());
    }

    public static BaseDataSource getThreadDataSource() {
        BaseDataSource dataSource = threadDataSourceMap.get(Thread.currentThread().getId());
        if (dataSource != null) {
            Boolean ret = threadMap.get(Thread.currentThread().getId());
            if (ret != null && ret == true) {
                return dataSource;
            }
        }
        return defaultDataSource;
    }

    public SystemSessionManager(Session session) {
        this.session = session;
    }

    public synchronized static SystemSessionManager getSession() {
        BaseDataSource dataSource = getThreadDataSource();
        if (dataSource != null) {
            return new SystemSessionManager(null);
        }

        SystemSessionManager sessionManager = null;
        SessionFactory sessionFactory = getSessionFactory();
        try {
            Session session = sessionFactory.getCurrentSession();
            sessionManager = new SystemSessionManager(session);// 不需要手动关闭
        } catch (Exception e) {
            LogUtil.i("获取自动会话失败!");
            e.printStackTrace();
            //Session session = sessionFactory.openSession();
            //sessionManager = new SystemSessionManager(true, session);// 需要手动关闭
        }
        return sessionManager;
    }

    private static SessionFactory sessionFactory;

    public synchronized static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            //EntityManagerFactory entityManagerFactory = SystemSpringConfig.getBean("entityManagerFactoryPrimary");
            EntityManagerFactory entityManagerFactory = SystemSpringConfig.getBean("entityManagerFactory");
            if (entityManagerFactory.unwrap(SessionFactory.class) == null) {
                throw new NullPointerException("factory is not a hibernate factory");
            }
            sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        }
        return sessionFactory;
    }

    public static void setErrorMessageEncoding(Connection conn) {
        if (GlobalValues.isDebug && conn != null) {
            try {
                if (conn.getClass().isAssignableFrom(DruidPooledConnection.class)) {
                    DruidPooledConnection _conn = (DruidPooledConnection) conn;
                    Connection connection = _conn.getConnection();
                    Field field = connection.getClass().getDeclaredField("connection");
                    field.setAccessible(true);
                    Object obj = field.get(connection);
                    field = obj.getClass().getSuperclass().getDeclaredField("errorMessageEncoding");
                    field.setAccessible(true);
                    field.set(obj, "UTF-8");
                }
            } catch (Exception e) {
            }
        }
    }

    //重新扫描包名
    public static void ReScanPackages() {
        BaseDataSource dataSource = getThreadDataSource();
        if (dataSource != null) {
            dataSource.ScanPackages();
            return;
        }
        for (String basePackage : GlobalValues.baseAppliction.GetScanPackages()) {
            Set<Class<?>> classes = ScanUtil.getClasses(basePackage);
            for (Class<?> aClass : classes) {
                if (BaseModel.class.isAssignableFrom(aClass)) {
                    SqlTable.CheckTable((Class<BaseModel>) aClass);
                }
            }
        }
    }

    public void doWork(Work work) throws SQLException {
        BaseDataSource dataSource = getThreadDataSource();
        if (dataSource != null) {
            dataSource.doWork(work);
            return;
        }
        boolean active = this.session.getTransaction().isActive();
        if (!active) {
            this.beginTransaction();
        }
        //临时的手动事务 end
        try {
            this.session.doWork(connection -> {
                setErrorMessageEncoding(connection);
                work.execute(connection);
            });
            //TODO 临时的手动事务 start
            if (!active) {
                this.commitTransaction();
            }
        } catch (Exception e) {
            if (!active) {
                this.rollbackTransaction();
            } else {
                throw e;
            }
            //临时的手动事务 end
        }
    }

    public Transaction beginTransaction() {
        try {
            return this.session.beginTransaction();
        } catch (Throwable ex) {
//            ex.printStackTrace();
            this.session.close();
            throw ex;
//            this.session.disconnect();
//            this.session = getSessionFactory().openSession();
        }
//        return this.session.beginTransaction();
    }

    public void commitTransaction() {
        try {
            if (this.session.getTransaction().isActive()) {
                this.session.getTransaction().commit();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            this.session.close();
            throw ex;
//            this.session.disconnect();
//            this.session = getSessionFactory().openSession();
        }
    }

    public void rollbackTransaction() {
        try {
            if (this.session.getTransaction().isActive()) {
                this.session.getTransaction().rollback();
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
            this.session.close();
//            this.session.disconnect();
//            this.session = getSessionFactory().openSession();
        }
    }
}
