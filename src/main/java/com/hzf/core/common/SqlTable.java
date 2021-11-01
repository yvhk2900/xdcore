package com.hzf.core.common;

import com.hzf.core.base.*;
import com.hzf.core.toolkit.StrUtil;
import com.hzf.core.toolkit.TypeConvert;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.log4j.Logger;
import org.hibernate.dialect.Dialect;

import javax.persistence.Table;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class SqlTable {
    private static Logger log = Logger.getLogger(SqlTable.class);

    private static void ExeSql(String sql, Object... params) throws SQLException {
        log.info(sql);
        SystemSessionManager.getSession().doWork(conn -> {
            new QueryRunner().update(conn, sql, params);
        });
    }

    private static DataTable ListSql(String sql, Object... params) throws SQLException {
        final ArrayList<DataTable> retList = new ArrayList<>();
        SystemSessionManager.getSession().doWork(conn -> {
            QueryRunner qr = new QueryRunner();
            TableSqlHandler list = new TableSqlHandler();
            List<Map<String, Object>> lm;
            lm = qr.query(conn, sql, list, params);
            DataTable dt = new DataTable(lm, list.DataSchema);
            dt.DbDataSchema = list.DbDataSchema;
            retList.add(dt);
        });
        if (retList.size() > 0) {
            return retList.get(0);
        }
        return null;
    }

    private static Dialect defalutDialect = null;

    static {
        try {
            defalutDialect = SystemSpringConfig.getBean(Dialect.class);
        } catch (Exception e) {
        }
    }

    public static Dialect GetDialect(){
        Dialect dialect = defalutDialect;
        BaseDataSource dataSource = SystemSessionManager.getThreadDataSource();
        if (dataSource != null && dataSource.dialect != null) {
            dialect = dataSource.dialect;
        }
        return dialect;
    }


    // 获取一个引用
    public static String getQuote(String name) {
        Dialect dialect = GetDialect();
        return dialect.openQuote() + name + dialect.closeQuote();
    }

    public static String getDbType(Class clazz, XColumn xc) {
        Dialect dialect = GetDialect();
        if (clazz.equals(String.class) && xc.columnDefinition().equals("text")) {
            return dialect.getTypeName(Types.CLOB);
        }
        if (clazz.equals(String.class) && xc.text()) {
            return dialect.getTypeName(Types.CLOB);
        }
        if (clazz.equals(String.class) && xc.mediumtext()) {
            return dialect.getTypeName(Types.CLOB);
        }
        if (clazz.equals(Byte[].class)) {
            return dialect.getTypeName(Types.BLOB);
        }
        if (clazz.equals(Date.class)) {
            return dialect.getTypeName(Types.TIMESTAMP);
        }
        if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
            return dialect.getTypeName(Types.INTEGER);
        }
        if (clazz.equals(Float.class) || clazz.equals(float.class)) {
            return dialect.getTypeName(Types.FLOAT);
        }
        if (clazz.equals(Long.class) || clazz.equals(long.class)) {
            return dialect.getTypeName(Types.BIGINT);
        }
        if (clazz.equals(Double.class) || clazz.equals(double.class)) {
            return dialect.getTypeName(Types.DOUBLE);
        }
        if (clazz.equals(Boolean.class) || clazz.equals(boolean.class)) {
            return dialect.getTypeName(Types.BIT);
        }
        return dialect.getTypeName(Types.VARCHAR, xc.length(), xc.precision(), xc.scale());
    }

    public static <T extends BaseModel> void CheckTable(Class<T> clazz) {
        if (!clazz.isAnnotationPresent(XTable.class) && !clazz.isAnnotationPresent(Table.class)) {
            return;
        }
        SqlCache.RemoveClass(clazz);
        SqlCache.AddClass(clazz);
        String tablename = BaseModel.GetTableName(clazz);
        String sql = "select * from " + tablename + " limit 1";
        DataTable dt = null;
        try {
            dt = ListSql(sql);
            if (dt == null) {
                CreateTable(clazz, tablename);
                dt = ListSql(sql);
            }
            if (dt != null) {
                AlterTable(clazz, tablename, dt);
            }
        } catch (Exception e) {
            String message = TypeConvert.ToString(e.getMessage()).trim();
            if (message.toLowerCase().contains("error executing work")) {
                message = TypeConvert.ToString(e.getCause().getMessage()).trim();
            }
            if (message.toLowerCase().contains("doesn't exist") || message.toLowerCase().contains("does not exist")) {
                try {
                    CreateTable(clazz, tablename);
                    dt = ListSql(sql);
                    AlterTable(clazz, tablename, dt);
                } catch (SQLException sqlException) {
                    sqlException.printStackTrace();
                }
                return;
            }
            e.printStackTrace();
        }
    }

    public static List<Class> GetSuperclass(Class clazz, List<Class> list) {
        if (clazz != null && clazz != Object.class) {
            GetSuperclass(clazz.getSuperclass(), list);
            list.add(clazz);
        }
        return list;
    }

    public static void CreateTable(Class clazz, String tableName) throws SQLException {
        boolean hasDefaultValue = true;
        boolean hasIndex = true;
        BaseDataSource baseDataSource = SystemSessionManager.getThreadDataSource();
        if (baseDataSource != null) {
            hasDefaultValue = baseDataSource.hasDefaultValue;
            hasIndex = baseDataSource.hasIndex;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(getQuote(tableName)).append(" (");
        Field[] fields = clazz.getFields();
        List<Class> list = GetSuperclass(clazz, new ArrayList<>());//按照顺序创建列名
        for (Class _super : list) {
            for (Field field : fields) {
                if (field.getDeclaringClass() != _super) {
                    continue;
                }
                XColumn xc = field.getAnnotation(XColumn.class);
                String columnName = field.getName();
                if (xc != null) {
                    if (StrUtil.isNotEmpty(xc.name())) {
                        columnName = xc.name();
                    }
                    sql.append(getQuote(columnName));
                    sql.append(' ').append(getDbType(field.getType(), xc));
                    if (hasDefaultValue) {
                        if (columnName.equals(BaseModel.F_id) || !xc.nullable()) {
                            sql.append(' ').append(" NOT NULL ");
                        } else {
                            sql.append(' ').append(" DEFAULT NULL ");
                        }
                    }
                    sql.append(',');
                }
            }
        }
        if (hasIndex) {
            sql.append(" PRIMARY KEY (" + BaseModel.F_id + ")");
            sql.append(',');
            sql.append(" UNIQUE INDEX id_index(" + BaseModel.F_id + ")");
            sql.append(")  DEFAULT CHARSET=utf8");
        } else {
            sql.deleteCharAt(sql.length() - 1);
            sql.append(")");
        }
        ExeSql(sql.toString());
    }

    public static void AlterTableColumn(Class clazz, String tableName, String columnName) throws SQLException {
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            XColumn xc = field.getAnnotation(XColumn.class);
            if (field.getName().equals(columnName)) {
                StringBuilder sql = new StringBuilder();
                sql.append("ALTER TABLE ").append(getQuote(tableName)).append(" MODIFY COLUMN  ");
                sql.append(getQuote(columnName));
                sql.append(' ').append(getDbType(field.getType(), xc));
                if (!xc.nullable()) {
                    sql.append(' ').append(" NOT NULL ");
                } else {
                    sql.append(' ').append(" DEFAULT NULL ");
                }
                ExeSql(sql.toString());
            }
        }
    }

    public static void AlterTable(Class clazz, String tableName, DataTable dt) throws SQLException {
        BaseDataSource baseDataSource = SystemSessionManager.getThreadDataSource();
        if (baseDataSource != null && !baseDataSource.hasIndex) {
            return;
        }
        String sqlindex = "show index from " + tableName;
        DataTable dtindex = ListSql(sqlindex);
        List<String> listindex = new ArrayList<>();
        //        show index from mytable;// Table  Key_name Column_name index_type

        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            XColumn xc = field.getAnnotation(XColumn.class);
            String columnName = field.getName();
            if (xc != null) {
                if (StrUtil.isNotEmpty(xc.name())) {
                    columnName = xc.name();
                }
                boolean has = false;
                boolean sametype = false;
                for (String col : dt.DataSchema.keySet()) {
                    if (columnName.equals(col)) {
                        has = true;
                        //todo:: 需要保存并验证上一次的xc
                        if (getDbType(field.getType(), xc).equals(getDbType(dt.DataSchema.get(col), xc))) {
                            sametype = true;
                        } else {
                            sametype = false;
                        }
                    }
                }
                if (!has) {
                    sametype = true;
                    StringBuilder sql = new StringBuilder();
                    sql.append("ALTER TABLE ").append(getQuote(tableName)).append("   ADD COLUMN  ");
                    sql.append(getQuote(columnName));
                    sql.append(' ').append(getDbType(field.getType(), xc));
                    if (!xc.nullable()) {
                        sql.append(' ').append(" NOT NULL ");
                    } else {
                        sql.append(' ').append(" DEFAULT NULL ");
                    }
                    ExeSql(sql.toString());
                }
                //类型不同，修改类型
                if (!sametype) {
                    StringBuilder sql = new StringBuilder();
                    sql.append("ALTER TABLE ").append(getQuote(tableName)).append(" MODIFY COLUMN  ");
                    sql.append(getQuote(columnName));
                    sql.append(' ').append(getDbType(field.getType(), xc));
                    if (!xc.nullable()) {
                        sql.append(' ').append(" NOT NULL ");
                    } else {
                        sql.append(' ').append(" DEFAULT NULL ");
                    }
                    ExeSql(sql.toString());
                }

            }
            XIndex xi = field.getAnnotation(XIndex.class);
            if (xi != null) {
                String icolumns = StrUtil.join(xi.columns());
                if (StrUtil.isEmpty(icolumns)) {
                    icolumns = columnName;
                }
                String ikey = icolumns + "_index";
                boolean has = false;
                for (Map mi : dtindex.Data) {
                    String iname = TypeConvert.ToString(mi.get("Key_name"));
                    if (ikey.equals(iname)) {
                        has = true;
                        listindex.add(ikey);
                    }
                }
                if (!has) {
                    StringBuilder sindex = new StringBuilder();
                    // create index 索引名 using hash on 表名(列名);
                    if (xi.unique()) {
                        sindex.append(" create UNIQUE index ");
                    } else {
                        sindex.append(" create index ");
                    }
                    sindex.append(ikey);
                    sindex.append(" on ").append(tableName).append("(").append(icolumns).append(")");
                    ExeSql(sindex.toString());
                }

            }
        }
        //        alter table mytable drop index mdl_tag_use_ix;//mdl_tag_use_ix是上表查出的索引名，key_name
    }


}
