package com.hzf.core.base;

import com.alibaba.fastjson.JSON;
import com.hzf.core.common.*;
import com.hzf.core.toolkit.RequestUtil;
import com.hzf.core.toolkit.StrUtil;
import com.hzf.core.toolkit.TypeConvert;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapHandler;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;


public class BaseQuery {
    private static Logger log = Logger.getLogger(BaseQuery.class);
    public static boolean OUT_LOG = true;

    public static final String F_PAGE_SIZE = "pageSize";
    public static final String F_PAGE_INDEX = "pageIndex";
    public static final String F_ORDER_BY = "orderBy";
    public static final String F_TotalMode = "totalMode";

    public boolean hasDataRight = true;
    public BaseQuery(){}
    public BaseQuery(BaseModel bm) {
        this.model = bm;
        if (bm != null) {
            String orderfield = bm.GetOrderField();
            if (StrUtil.isNotEmpty(orderfield)) {
                this.OrderBy = SqlInfo.CreateOrderBy(BaseModel.GetTableName(bm.getClass()), orderfield, true);
            }
        }
    }

    @XQuery(table = "BaseQuery", type = XQuery.QueryType.equal)
    public String id;
    @XQuery(table = "BaseQuery", type = XQuery.QueryType.equal)
    public String Parentid;
    @XQuery(table = "BaseQuery", type = XQuery.QueryType.equal)
    public Boolean IsTreeLeaf;

    @XQuery(table = "BaseQuery", column = BaseModelTree.F_id, type = XQuery.QueryType.in)
    public String[] ids;
    @XQuery(table = "BaseQuery", column = BaseModelTree.F_Parentid, type = XQuery.QueryType.in)
    public String[] Parentids;
    @XQuery(table = "BaseQuery", column = BaseModelTree.F_TreeName, type = XQuery.QueryType.like)
    public String TreeName;

    @XQuery(type = XQuery.QueryType.none)
    public String[] KeywordFields;
    @XQuery(type = XQuery.QueryType.none)
    public String KeywordValue = "";

    public Boolean IsTree;
    public boolean IsSearch = false;
    public Boolean IsTable;

    public Long Total = 0L;//总行数
    public Long PageIndex = -1L;//当前行数
    public Long PageSize = -1L;// 10L;//每页行数
    public boolean UseOrderBy = true;
    public String OrderBy = "";//排序字段

    public transient HttpServletRequest request = null;//不反射这个字段

    public String selectSql;
    public String totalSql;//根据上面生成的SQL语句

    public Map<String, Object> totalMode;

    public enum StatTypeEnum {
        sum, count, avg, max, min
    }

    public String StatType;
    public String StatField;
    public String QueryField;
    public String SaveValue;
    public String GroupField;


    BaseModel model;

    public BaseModel getModel() {
        return model;
    }

    public boolean isTreeModal() {
        return model instanceof BaseModelTree;
    }


    List<SqlInfo> listCustomSqlCond = new ArrayList<>();

    public void ClearCustomSqlCond() {
        listCustomSqlCond.clear();
    }

    public void AddCustomSqlCond(SqlInfo su) {
        listCustomSqlCond.add(su);
    }

    public String toJson() {
        Field[] fields = this.getClass().getFields();
        Map<String, Object> m = new HashMap();
        try {
            for (Field f : fields) {
                m.put(f.getName(), f.get(this));
            }
        } catch (Exception ex) {

        }
        String json = TypeConvert.ToJson(m);
        return json;
    }


    public void CreateSql(SqlInfo su) throws Exception {
        BaseUser bu = GlobalValues.GetSessionUser();
        if (bu != null && this.hasDataRight) {
            bu.SetQueryListDataRight(this, su);
        }

        CreateQueryCond(su);

        for (SqlInfo sucond : listCustomSqlCond) {
            String where = sucond.ToWhere();
            if (StrUtil.isNotEmpty(where)) {
                su.And(where);
            }
            su.AddParams(sucond.GetParamsList());
        }

        if (StrUtil.isNotEmpty(this.KeywordValue)) {
            CreatKeyWordCond(su);
        }
        if (su.listQueryColumn.size() > 0) {
            CreatQueryColumnCond(su);
        }
        if (StrUtil.isEmpty(this.OrderBy) && su.sborderby.length() == 0) {
            this.OrderBy = SqlInfo.CreateOrderBy(su.CurrentMainTable, BaseModel.F_CreateTime, false);
        }
    }


    public DataTable GetList(SqlInfo su) throws Exception {
        this.CreateSql(su);
        this.totalSql = su.ToTotal();
        DataTable dt = ListSql(su, this);
        return ToTreeFirstLevel(dt);
    }

    public DataTable GetListNoPage(SqlInfo su) throws Exception {
        this.CreateSql(su);
        return ListSql(su, null);
    }

    public Object GetValue(SqlInfo su) throws Exception {
        this.CreateSql(su);
        return ObjectSql(su);
    }


    public void InitComplete() {

    }

    public DataTable ToTreeFirstLevel(DataTable dt) {
        if (this.IsTree != null) {
            if (this.IsTree && this.Parentids == null && !this.IsSearch && dt != null) {     // 对树的第一次查询，只返回根节点
                dt.ToTreeFirstLevel();
            }
        }
        return dt;
    }

    public DataTable FilterTable(DataTable dt) {
        if (this.IsTree == null) {
            this.IsTree = false;
        }
        DataTable result = new DataTable();
        if (this.IsTree) {
            if (this.Parentids == null && !this.IsSearch) {
                dt.ToTreeFirstLevel();
            }
        }
        for (Map m : dt.Data) {
            boolean has = CheckMap(m);
            if (has) {
                result.Data.add(m);
            }
        }
        result.setValue(dt.GetMapValue());
        result.DataColumns = dt.DataColumns;
        return result;
    }

    boolean CheckMap(Map m) {
        boolean has = true;
        if (this.Parentids != null && this.Parentids.length > 0) {
            has = false;
            for (String pid : this.Parentids) {
                if (pid.equals(m.get(BaseModelTree.F_Parentid)) || pid.equals(m.get(BaseModelTree.F_id))) {
                    has = true;
                    break;
                }
            }
        }
        if (this.KeywordFields != null && StrUtil.isNotEmpty(this.KeywordValue)) {
            has = false;
            for (String kfc : this.KeywordFields) {
                if (m.containsKey(kfc)) {
                    String v = TypeConvert.ToString(m.get(kfc));
                    if (v.contains(this.KeywordValue)) {
                        has = true;
                        break;
                    }
                }
            }
        }
        return has;
    }

    public BaseQuery NotPagination() {
        this.PageSize = -1L;
        this.PageIndex = -1L;
        return this;
    }

    public BaseQuery InitFromMap(Map params) throws IllegalAccessException {
        if (params == null) {
            return this;
        }
        Field[] fields = this.getClass().getFields();
        for (Field f : fields) {
            if (f.getType().equals(String[].class)) {
                String[] ss = StrUtil.split(TypeConvert.ToString(params.get(f.getName())));
                if (ss != null && ss.length > 0) {
                    if (ss.length == 1 && StrUtil.isEmpty(ss[0])) {
                        continue;
                    }
                    f.set(this, ss);
                }
            } else {
                Object o = params.get(f.getName());
                if (o != null && StrUtil.isNotEmpty(o) && !"undefined".equals(TypeConvert.ToString(o))) {
                    if (f.getType().equals(Date.class)) {
                        o = TypeConvert.ToDate(o);
                    } else {
                        o = TypeConvert.ToType(f.getType(), o);
                    }
                    f.set(this, o);
                }
            }
        }
        this.InitComplete();
        String pz = TypeConvert.ToString(params.get(F_PAGE_SIZE));
        if (StrUtil.isNotEmpty(pz)) {
            this.PageSize = TypeConvert.ToLong(pz);
        }
        String pi = TypeConvert.ToString(params.get(F_PAGE_INDEX));
        if (StrUtil.isNotEmpty(pi)) {
            this.PageIndex = TypeConvert.ToLong(pi);
        }
        String orderBy = TypeConvert.ToString(params.get(F_ORDER_BY));
        if (StrUtil.isNotEmpty(orderBy)) {
            this.OrderBy = orderBy;
        }
        String totalMode = TypeConvert.ToString(params.get(F_TotalMode));
        if (StrUtil.isNotEmpty(totalMode)) {
            this.totalMode = TypeConvert.FromMapJson(totalMode);
        }
        return this;
    }

    public BaseQuery InitFromRequest(HttpServletRequest _request) throws IllegalAccessException {
        if (_request == null) {
            return this;
        }
        request = _request;
        Field[] fields = this.getClass().getFields();
        for (Field f : fields) {
            if (f.getType().equals(String[].class)) {
                String[] ss = RequestUtil.GetStringArray(request, f.getName());
                if (ss != null && ss.length > 0) {
                    if (ss.length == 1 && StrUtil.isEmpty(ss[0])) {
                        continue;
                    }
                    f.set(this, ss);
                }
            } else {
                Object o = RequestUtil.GetParameter(request, f.getName());
                if (o != null && StrUtil.isNotEmpty(o) && !"undefined".equals(TypeConvert.ToString(o))) {
                    if (f.getType().equals(Date.class)) {
                        o = TypeConvert.ToDate(o);
                    } else {
                        o = TypeConvert.ToType(f.getType(), o);
                    }
                    f.set(this, o);
                }
            }
        }
        this.InitComplete();
        String pz = RequestUtil.GetParameter(request, F_PAGE_SIZE);
        if (StrUtil.isNotEmpty(pz)) {
            this.PageSize = TypeConvert.ToLong(pz);
        }
        String pi = RequestUtil.GetParameter(request, F_PAGE_INDEX);
        if (StrUtil.isNotEmpty(pi)) {
            this.PageIndex = TypeConvert.ToLong(pi);
        }
        String orderBy = RequestUtil.GetParameter(request, F_ORDER_BY);
        if (StrUtil.isNotEmpty(orderBy)) {
            this.OrderBy = orderBy;
        }
        String totalMode = RequestUtil.GetString(request, F_TotalMode);
        if (StrUtil.isNotEmpty(totalMode)) {
            this.totalMode = TypeConvert.FromMapJson(totalMode);
        }
        return this;
    }


    public void CreatKeyWordCond(SqlInfo su) {
        if (this.KeywordFields != null && StrUtil.isNotEmpty(this.KeywordValue)) {
            List<String> kfc = Arrays.asList(this.KeywordFields);
            SqlInfo suc = new SqlInfo();
            for (SqlInfo.QueryColumn qc : su.listQueryColumn) {
                if (kfc.contains(qc.column) && StrUtil.isNotEmpty(qc.column)) {
                    suc.OrLike(qc.table, qc.column);
                    suc.AddParam("%%" + this.KeywordValue + "%%");
                } else if (kfc.contains(qc.asColumn) && StrUtil.isNotEmpty(qc.asColumn)) {
                    suc.OrLike(qc.table, qc.column);
                    suc.AddParam("%%" + this.KeywordValue + "%%");
                }
            }
            String where = suc.ToWhere();
            if (StrUtil.isNotEmpty(where)) {
                su.And(where);
            }
            su.AddParams(suc.GetParamsList());
        }
    }

    public void CreatQueryColumnCond(SqlInfo su) {
        String filterKey = "filter";
        for (SqlInfo.QueryColumn qc : su.listQueryColumn) {
            if (qc.type.equals(XQuery.QueryType.none)) {
                continue;
            }
            if (StrUtil.isNotEmpty(qc.column)) {
                Object o = RequestUtil.GetParameter(this.request, filterKey + qc.column);
                if (o == null && StrUtil.isNotEmpty(qc.asColumn)) {
                    o = RequestUtil.GetParameter(this.request, filterKey + qc.asColumn);
                }
                if (o != null) {
                    Class t = qc.clazz;
                    if (t == null) {
                        try {
                            Class clazz = SqlCache.GetClassByTableName(qc.table);
                            Field f = clazz.getDeclaredField(qc.column);
                            t = f.getType();
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    if (qc.type.equals(XQuery.QueryType.custom)) {
                        if (t.equals(String.class)) {
                            String cond = qc.table + "." + qc.column + " like ? ";
                            su.And(cond).AddParam("%%" + TypeConvert.ToString(o) + "%%");
                        } else if (t.equals(Date.class) || t.equals(Double.class) || t.equals(Integer.class) || t.equals(int.class) || t.equals(double.class)) {
                            String v = TypeConvert.ToString(o);
                            String[] vs = v.split("\\|");
                            if (vs.length > 0 && StrUtil.isNotEmpty(vs[0])) {
                                su.And(qc.table + "." + qc.column + " >= ? ");
                                su.AddParam(TypeConvert.ToType(t, vs[0]));
                            }
                            if (vs.length > 1 && StrUtil.isNotEmpty(vs[1])) {
                                su.And(qc.table + "." + qc.column + " <= ? ");
                                su.AddParam(TypeConvert.ToType(t, vs[1]));
                            }
                        }
                    } else {
                        String op = XQuery.QueryType.GetOperate(qc.type);
                        if (qc.type.equals(XQuery.QueryType.like)) {

                        }
                        if (qc.type.equals(XQuery.QueryType.range)) {

                        }
                    }
                }
            }
        }
    }

    public void CreateQueryCond(SqlInfo su) throws Exception {
        Field[] fields = this.getClass().getFields();
        for (Field f : fields) {
            Object o = f.get(this);
            if (o != null && StrUtil.isNotEmpty(o) && !"undefined".equals(TypeConvert.ToString(o))) {

                XQuery cs = f.getAnnotation(XQuery.class);
                if (cs == null) {
                    continue;
                }
                String column = StrUtil.isEmpty(cs.column()) ? f.getName() : cs.column();
                String table = cs.table();
                if ("BaseQuery".equals(table) || StrUtil.isEmpty(table)) {
                    if(this.model!=null) {
                        table = BaseModel.GetTableName(this.model.getClass());
                    }
                }
                if (StrUtil.isNotEmpty(table)) {
                    column = table + "." + column;
                }
                if (cs.type() == XQuery.QueryType.none) {
                    continue;
                }
                if (!"undefined".equals(o) && !"".equals(o)) {
                    String op = XQuery.QueryType.GetOperate(cs.type());
                    if (cs.type() == XQuery.QueryType.in && (this.IsTree != null && this.IsTree == true) && BaseModelTree.F_Parentid.equals(cs.column())) {
                        String incond = table + "." + BaseModelTree.F_id + " in ( ";
                        String incond2 = column + " in ( ";
                        String[] values = TypeConvert.ToStringArray(o);
                        for (String v : values) {
                            incond += "?,";
                            su.AddParam(v);
                        }
                        for (String v : values) {
                            incond2 += "?,";
                            su.AddParam(v);
                        }
                        incond = StrUtil.CutEnd(incond, ",");
                        incond += ")";

                        incond2 = StrUtil.CutEnd(incond2, ",");
                        incond2 += ")";

                        incond = "(" + incond + " or " + incond2 + ")";
                        su.And(incond);

                    } else if (cs.type() == XQuery.QueryType.in || cs.type() == XQuery.QueryType.notIn) {
                        String incond = "";
                        if (cs.type() == XQuery.QueryType.notIn) {
                            incond = column + " not in ( ";
                        } else {
                            incond = column + " in ( ";
                        }

                        String[] values = TypeConvert.ToStringArray(o);
                        for (String v : values) {
                            incond += "?,";
                            su.AddParam(v);
                        }
                        incond = StrUtil.CutEnd(incond, ",");
                        incond += ")";
                        su.And(incond);
                    } else if (cs.type() == XQuery.QueryType.custom) {
                        su.And(TypeConvert.ToString(o));
                    }else if (cs.type() == XQuery.QueryType.equalOrNull) {
                        su.And(column + " = ? or " + column + " is null ").AddParam(TypeConvert.ToType(f.getType(), o));
                    } else if (cs.type() == XQuery.QueryType.isnullORnot) {
                        if ("是".equals(o) || TypeConvert.ToBoolean(o)) {
                            su.And(column + " is null ");
                        } else {
                            su.And(column + " is not null ");
                        }
                    } else if (cs.type() == XQuery.QueryType.range) {
                        String value = TypeConvert.ToString(o);
                        String[] values = value.split("\\|");
                        if (values.length > 0 && StrUtil.isNotEmpty(values[0])) {
                            su.And(column + " >= ? ").AddParam(TypeConvert.ToType(f.getType(), values[0]));
                        }
                        if (values.length > 1 && StrUtil.isNotEmpty(values[1])) {
                            su.And(column + " <= ? ").AddParam(TypeConvert.ToType(f.getType(), values[1]));
                        }
                    } else {
                        if (o instanceof String[]) {
                            String[] strs = (String[]) o;
                            if (strs.length > 0) {
                                String cc = "(";
                                for (String s : strs) {
                                    cc += column + " " + op + " ?  or ";
                                    if (cs.type() == XQuery.QueryType.like) {
                                        su.AddParam("%%" + s + "%%");
                                    } else if (cs.type() == XQuery.QueryType.leftlike) {
                                        su.AddParam(s + "%%");
                                    } else if (cs.type() == XQuery.QueryType.rightlike) {
                                        su.AddParam("%%" + s);
                                    } else {
                                        su.AddParam(TypeConvert.ToType(f.getType(), s));
                                    }
                                }
                                cc = StrUtil.CutEnd(cc, "or ");
                                cc += ")";
                                su.And(cc);
                            }
                        } else {
                            su.And(column + " " + op + " ? ");
                            if (cs.type() == XQuery.QueryType.like) {
                                su.AddParam("%%" + TypeConvert.ToString(o) + "%%");
                            } else if (cs.type() == XQuery.QueryType.leftlike) {
                                su.AddParam(TypeConvert.ToString(o) + "%%");
                            } else if (cs.type() == XQuery.QueryType.rightlike) {
                                su.AddParam("%%" + TypeConvert.ToString(o));
                            } else {
                                su.AddParam(TypeConvert.ToType(f.getType(), o));
                            }
                        }
                    }
                }

            }
        }
    }

    public static Object ObjectSql(SqlInfo su) throws SQLException {
        try {
            return _ObjectSql(su);
        } catch (Exception e) {
            String message = TypeConvert.ToString(e.getMessage()).trim();
            if (message.toLowerCase().contains("error executing work")) {
                if (e.getCause() != null) {
                    message = TypeConvert.ToString(e.getCause().getMessage()).trim();
                }
            }
            if (message.toLowerCase().contains("doesn't exist") || message.toLowerCase().contains("does not exist")) {
                SystemSessionManager.ReScanPackages();
            }
            return _ObjectSql(su);
        }
    }


    public static Object _ObjectSql(SqlInfo su) throws SQLException {
        Map<String, Object> m = MapSql(su);
        if (m.keySet().size() > 0) {
            return m.get(m.keySet().toArray()[0]);
        }
        return null;
    }

    // 用于统计行数的sql
    public static long LongSql(SqlInfo su) throws SQLException {
        return TypeConvert.ToLong(ObjectSql(su));
    }

    public static double DoubleSql(SqlInfo su) throws SQLException {
        return TypeConvert.ToDouble(ObjectSql(su));
    }


    public static DataTable ListSql(SqlInfo su, BaseQuery pageInfo) throws SQLException {
        try {
            return _ListSql(su, pageInfo);
        } catch (Exception e) {
            String message = TypeConvert.ToString(e.getMessage()).trim();
            if (message.toLowerCase().contains("error executing work")) {
                if (e.getCause() != null) {
                    message = TypeConvert.ToString(e.getCause().getMessage()).trim();
                }
            }
            if (message.toLowerCase().contains("unknown column") || message.toLowerCase().contains("doesn't exist") || message.toLowerCase().contains("does not exist")) {
                for (String table : su.TableList) {
                    Class<BaseModel> _class = (Class<BaseModel>) SqlCache.GetClassByTableName(table);
                    if (_class != null) {
                        SqlTable.CheckTable(_class);
                    }
                }
                return _ListSql(su, pageInfo);
            }
            throw e;
        }
    }

    private static DataTable _ListSql(SqlInfo su, BaseQuery pageInfo) throws SQLException {
        final ArrayList<DataTable> retList = new ArrayList<>();
        SystemSessionManager.getSession().doWork(conn -> {
            String _sql = su.ToSql();
            boolean hasPage = false;
            if (pageInfo != null) {
                if (pageInfo.PageSize > 0 && pageInfo.PageIndex >= 0 && !StrUtil.isEmpty(pageInfo.totalSql)) {
                    hasPage = true;
                    SqlInfo sutotal = new SqlInfo().Append(pageInfo.totalSql).AddParams(Arrays.asList(su.GetParams()));
                    pageInfo.Total = LongSql(sutotal);
                }
                if (pageInfo.UseOrderBy && !StrUtil.isEmpty(pageInfo.OrderBy) && !_sql.toUpperCase().contains("ORDER BY")) {
                    _sql += " ORDER BY " + pageInfo.OrderBy;
                }
                if (pageInfo.PageSize > 0 && pageInfo.PageIndex >= 0 && !_sql.toUpperCase().contains("LIMIT")) {
                    BaseDataSource dataSource = SystemSessionManager.getThreadDataSource();
                    if (dataSource != null) {
                        _sql = dataSource.GetLimitString(pageInfo, _sql);
                    } else {
                        _sql += " LIMIT " + pageInfo.PageSize + " OFFSET " + pageInfo.PageIndex;
                    }
                }
            }
            Map<String, Object> totalResult = null;
            if (pageInfo != null && pageInfo.totalMode != null) {
                totalResult = new HashMap<>();
                for (String f : pageInfo.totalMode.keySet()) {
                    String mode = TypeConvert.ToString(pageInfo.totalMode.get(f));
                    if (mode.equals(StatTypeEnum.count.name())) {
                        totalResult.put(f, LongSql(su.ToCountTotal(f)));
                    } else if (mode.equals(StatTypeEnum.sum.name())) {
                        totalResult.put(f, DoubleSql(su.ToSumTotal(f)));
                    } else if (mode.equals(StatTypeEnum.avg.name())) {
                        totalResult.put(f, DoubleSql(su.ToAvgTotal(f)));
                    } else if (mode.equals(StatTypeEnum.min.name())) {
                        totalResult.put(f, DoubleSql(su.ToMinTotal(f)));
                    } else if (mode.equals(StatTypeEnum.max.name())) {
                        totalResult.put(f, DoubleSql(su.ToMaxTotal(f)));
                    }
                }
            }
            QueryRunner qr = new QueryRunner();
            TableSqlHandler list = new TableSqlHandler();
            List<Map<String, Object>> lm;
            if (OUT_LOG) {
                log.info(_sql);
                log.info(JSON.toJSONString(su.GetParams()));
            }
            lm = qr.query(conn, _sql, list, su.GetParams());
            DataTable dt = new DataTable(lm, list.DataSchema);
            dt.DbDataSchema = list.DbDataSchema;
            for (String dc : list.DataColumns) {
                dt.DataColumns.add(DataTable.CreateColumnMap(dc, list.DataSchema.get(dc), false));
            }
            if (pageInfo != null) {
                if (hasPage) {
                    dt.Total = pageInfo.Total;
                } else {
                    dt.Total = (long) dt.Data.size();
                }
            }
            dt.TotalResult = totalResult;
            retList.add(dt);
        });
        if (retList.size() > 0) {
            return retList.get(0);
        }
        return new DataTable();
    }


    public static <T extends BaseModel> T InfoSql(Class<T> clazz, SqlInfo su) throws SQLException {
        final ArrayList<T> retList = new ArrayList<T>();
        SystemSessionManager.getSession().doWork(conn -> {
            QueryRunner qr = new QueryRunner();
            MapHandler map = new MapHandler();
            Map<String, Object> lm;
            lm = qr.query(conn, su.ToSql(), map, su.GetParams());
            if (lm != null) {
                T bm = null;
                try {
                    bm = clazz.newInstance();
                    bm.SetValuesByMap(lm);
                    retList.add(bm);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        T bm = null;
        if (retList.size() > 0) bm = retList.get(0);
        return bm;
    }

    // 执行一段SQL, 返回一个Map
    public static Map<String, Object> MapSql(SqlInfo su) throws SQLException {
        final ArrayList<Map<String, Object>> retList = new ArrayList<>();
        SystemSessionManager.getSession().doWork(conn -> {
            QueryRunner qr = new QueryRunner();
            MapHandler map = new MapHandler();
            Map<String, Object> lm;
            String sql = su.ToSql();
            if (OUT_LOG) {
                log.info(sql);
                log.info(JSON.toJSONString(su.GetParams()));
            }
            lm = qr.query(conn, sql, map, su.GetParams());
            if (lm == null) lm = new HashMap<>();
            retList.add(lm);
        });
        return retList.get(0);
    }

    // 执行一段SQL, 返回受到影响的行数
    public static int ExecuteSql(SqlInfo su) throws SQLException {
        final ArrayList<Integer> retList = new ArrayList<>();
        SystemSessionManager.getSession().doWork(conn -> {
            String sql = su.ToSql();
            if (OUT_LOG) {
                log.info(sql);
                log.info(JSON.toJSONString(su.GetParams()));
            }
            retList.add(new QueryRunner().update(conn, sql, su.GetParams()));
        });
        try {
            if (GlobalValues.baseAppliction != null) {
                GlobalValues.baseAppliction.ResetCache(su);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (retList.size() > 0) {
            return retList.get(0);
        }
        return 0;
    }

}
