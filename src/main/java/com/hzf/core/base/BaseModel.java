package com.hzf.core.base;

import com.hzf.core.common.*;
import com.hzf.core.toolkit.ExcelReadUtil;
import com.hzf.core.toolkit.ExcelWriteUtil;
import com.hzf.core.toolkit.RequestUtil;
import com.hzf.core.toolkit.StrUtil;
import com.hzf.core.toolkit.TypeConvert;
import org.apache.log4j.Logger;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseModel {
    public static Logger log = Logger.getLogger(BaseModel.class);

    //默认的数据库字段们
    @XColumn(name = F_id, unique = true, nullable = false, length = 128)
//    @XIndex(unique = true)
    public String id;
    public final static String F_id = "id";
    public final static String F_ids = "ids";

    @Id
    @GeneratedValue(generator = "shortUid")
    @GenericGenerator(name = "shortUid", strategy = "com.hzf.core.common.ShortUUIDIncrementGenerator")
    @Column(name = F_id, unique = true, nullable = false, length = 128)
    public String getId() {
        return id;
    }
    public void setId(String id){
        this.id = id;
    }

    // 获得一个唯一id,一般用于生成id
    public static String GetUniqueId() {
        return ShortUUIDIncrementGenerator.getUUID();
    }

    // 判断这个对象是否被保存(若未保存的情况下,手动给id赋值了, 会造成误认为对象已被保存,而实际并未保存)
    public boolean IsNew() {
        return StrUtil.isEmpty(id);
    }

    public static boolean IsNew(BaseModel bm) {
        return bm == null || bm.IsNew();
    }

    @XColumn
    @XIndex
    public Date CreateTime;
    public final static String F_CreateTime = "CreateTime";

    @Column(name = F_CreateTime)
    public Date getCreateTime() {
        return CreateTime;
    }
    public void setCreateTime(Date value) {
        this.CreateTime = value;
    }

    @XColumn
    public String CreateUser;
    public final static String F_CreateUser = "CreateUser";

    @Column(name = F_CreateUser)
    public String getCreateUser() {
        return CreateUser;
    }

    public void setCreateUser(String value) {
        this.CreateUser = value;
    }

    @XColumn
    public Date UpdateTime;
    public final static String F_UpdateTime = "UpdateTime";

    @Column(name = F_UpdateTime)
    public Date getUpdateTime() {
        return UpdateTime;
    }

    public void setUpdateTime(Date value) {
        this.UpdateTime = value;
    }

    @XColumn
    public String UpdateUser;
    public final static String F_UpdateUser = "UpdateUser";

    @Column(name = F_UpdateUser)
    public String getUpdateUser() {
        return UpdateUser;
    }

    public void setUpdateUser(String value) {
        this.UpdateUser = value;
    }

    // 保存之前的校验, 若唯一校验方法还不够的话则重载这个方法做自定义校验.
    public String SaveValidate() throws Exception {
        return "";
    }

    // 删除之前的校验
    public String DeleteValidate() throws Exception {
        return "";
    }


    public boolean HasOtherTableValue(String table, String field) throws SQLException {
        SqlInfo su = new SqlInfo().CreateSelect().AppendColumn(table,F_id).From(table)
                .WhereEqual(field).AddParam(this.id).AppendLimitOffset(1,0);
        DataTable dt = BaseQuery.ListSql(su, null);
        return dt.Data.size() > 0;
    }

    //endregion



    // 字段名称 给对象的字段赋值
    public void SetValue(String columnName, Object value) {

        Map<String, Field> columnTypeMap = SqlCache.GetColumnFieldMap(this.getClass());
        if (!columnTypeMap.containsKey(columnName)) return;
        Field m = columnTypeMap.get(columnName);
        if (m != null) {
            try {
                m.setAccessible(true);
                m.set(this, TypeConvert.ToType(m.getType(), value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 字段名称 获取对象的值
    public Object GetValue(String columnName) {
        Map<String, Field> columnTypeMap = SqlCache.GetColumnFieldMap(this.getClass());
        if (!columnTypeMap.containsKey(columnName)) return null;
        Field m = columnTypeMap.get(columnName);
        if (m != null) {
            try {
                m.setAccessible(true);
                return m.get(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


    //  map 设置对象中每个字段的值
    public void SetValuesByMap(Map<String, Object> map) {
        for (String keyName : map.keySet()) {
            this.SetValue(keyName, map.get(keyName));
        }
    }

    public void SetValuesByMap(Map<String, Object> map, List<String> listNotSetRequestValueFileds) {
        List<String> listnotset = new ArrayList<>();
        listnotset.addAll(listNotSetRequestValueFileds);
        for (String keyName : map.keySet()) {
            if (listnotset.size() > 0 && listnotset.contains(keyName)) {
                continue;
            }
            this.SetValue(keyName, map.get(keyName));
        }
    }
    //
    public void SetValuesByRequest(HttpServletRequest request) {
        SetValuesByRequest(request, new ArrayList<>());
    }

    public void SetValuesByRequest(HttpServletRequest request, List<String> listNotSetRequestValueFileds) {
        Map map = request.getParameterMap();
        List<String> listnotset = new ArrayList<>();
        listnotset.addAll(listNotSetRequestValueFileds);
        for (String columnName : SqlCache.GetColumnFieldMap(this.getClass()).keySet()) {
            if (listnotset.size() > 0 && listnotset.contains(columnName)) {
                continue;
            }
            if (map.containsKey(columnName)) {
                Object value = RequestUtil.GetString(request, columnName);
                if (value != null && !"undefined".equals(value)) {
                    this.SetValue(columnName, value);
                } else {
                    this.SetValue(columnName, null);
                }
            }
        }
    }

    // 将本对象转为Json
    public String ToJson() {
        return TypeConvert.ToJson(this.ToMap());
    }


    public Map<String, Object> ToMap() {
        Map<String, Field> columnTypeMap = SqlCache.GetColumnFieldMap(this.getClass());
        Map<String, Object> m = new HashMap<>();
        Collection<String> lm = columnTypeMap.keySet();
        for (String columnName : lm) {
            m.put(columnName, this.GetValue(columnName));
        }
        m.remove(F_CreateTime);
        m.remove(F_UpdateTime);
        m.remove(F_CreateUser);
        m.remove(F_UpdateUser);
        return m;
    }
    public String GetOrderField(){return "";};
    public String GetNameField() {
        return "";
    }
    public boolean AutoSetOrder() {
        return true;
    }

    public int GetMaxOrder() throws SQLException {
        return GetMaxOrder((SqlInfo) null);
    }

    public int GetMaxOrder(BaseQuery bq) throws Exception {
        String orderField = GetOrderField();
        int order = 0;
        if(StrUtil.isNotEmpty(orderField)) {
            SqlInfo su = new SqlInfo().CreateSelect(" max(" + orderField + ") ")
                    .From(GetTableName(this.getClass()));
            bq.CreateSql(su);
            order = TypeConvert.ToInteger(BaseQuery.ObjectSql(su));
        }
        return order+1;
    }

    public int GetMaxOrder(SqlInfo sqlWhere) throws SQLException {
        String orderField = GetOrderField();
        int order = 0;
        if(StrUtil.isNotEmpty(orderField)) {
            SqlInfo su = new SqlInfo().CreateSelect(" max("+orderField+") " )
                    .From(GetTableName(this.getClass()));
            if(sqlWhere!=null){
                su.Where(sqlWhere.ToWhere()).AddParams(sqlWhere.GetParamsList());
            }
            order =TypeConvert.ToInteger(BaseQuery.ObjectSql(su));
        }
        return order+1;
    }

    public List<Map> GetUniqueFields() {
        return new ArrayList<>();
    }

    public static boolean IsUnique(Class type, String id, Map map) throws Exception {

        BaseModel bm = GetObjectByMapValue(type, map);
        if (bm != null) {
            if (bm.id.equals(id)) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    public static boolean IsUnique(Class type, String id, String field, Object value) throws Exception {
        BaseModel bm = GetObjectByFieldValue(type, field, value);
        if (bm != null) {
            if (bm.id.equals(id)) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }


    public void Save() throws Exception {
        // 先进行唯一值校验,校验通过之后再进行子级的保存校验
        String err = "";
        List<Map> listunique = this.GetUniqueFields();
        if (listunique.size() > 0) {
            for (Map m : listunique) {
                if (!BaseModel.IsUnique(this.getClass(), this.id, m)) {
                    for (Object s : m.keySet()) {
                        err += TypeConvert.ToString(s).replace("id", "") + "、";
                    }
                    err += " 记录已存在";
                    break;
                }
            }
        }
        if (StrUtil.isNotEmpty(err)) {
            throw new XException(err);
        }
        err = this.SaveValidate();
        if (StrUtil.isNotEmpty(err)) {
            throw new XException(err);
        }
        if (IsNew(this)) {
            if (!Insert()) throw new XException("插入数据失败!");
        } else {
            if (!Update()) {
                if (!Insert()) throw new XException("更新数据失败!");
            }
        }

    }

    private boolean Insert() throws SQLException {
        if (StrUtil.isEmpty(this.id)) {
            this.id=(BaseModel.GetUniqueId());
        }
        this.CreateTime = new Date();
        if (!BaseModel.IsNew(GlobalValues.GetSessionUser())) {
            this.CreateUser = (GlobalValues.GetSessionUser().id);
        }
        this.UpdateTime=this.CreateTime;
        this.UpdateUser=this.CreateUser;

        Class clazz = this.getClass();
        String sql = SqlCache.GetInsertSql(clazz);
        List<String> columns = SqlCache.GetInsertColumns(clazz);
        if(StrUtil.isEmpty(sql)||columns==null){
            Map<String, Field> columnTypeMap = SqlCache.GetColumnFieldMap(this.getClass());
            String tableName = BaseModel.GetTableName(clazz);
            SqlInfo su = new SqlInfo().CreateInsertInto(tableName);
            columns = new ArrayList<>();
            for(String col :columnTypeMap.keySet()){
                su.Values(col);
                columns.add(col);
            }
            sql = su.ToSql();
            SqlCache.AddInsertSql(clazz,sql,columns);
        }
        SqlInfo su = new SqlInfo().SetMainTable(BaseModel.GetTableName(clazz)).Append(sql);
        for (String c : columns) {
            Object value = this.GetValue(c);
            if (value != null && value.getClass() == Date.class) {
                Timestamp time = new Timestamp(((Date) value).getTime());
                su.AddParam(time);
            } else {
                su.AddParam(value);
            }
        }
        return BaseQuery.ExecuteSql(su) > 0;
    }

    private boolean Update() throws SQLException {
        this.UpdateTime=(new Date());
        if (!BaseModel.IsNew(GlobalValues.GetSessionUser())) {
            this.UpdateUser=(GlobalValues.GetSessionUser().id);
        }
        Class clazz = this.getClass();
        String sql = SqlCache.GetUpdateSql(clazz);
        List<String> columns = SqlCache.GetUpdateColumns(clazz);
        if(StrUtil.isEmpty(sql)||columns==null) {
            String tableName = BaseModel.GetTableName(clazz);
            SqlInfo su = new SqlInfo().CreateUpdate(tableName);
            columns = new ArrayList<>();
            Map<String, Field> columnTypeMap = SqlCache.GetColumnFieldMap(this.getClass());
            for(String col :columnTypeMap.keySet()){
                if (!col.equals(BaseModel.F_id)) {
                    su.SetEqual(col);
                    columns.add(col);
                }
            }
            su.WhereEqual(F_id);
            columns.add(F_id);
            sql = su.ToSql();
            SqlCache.AddUpdateSql(clazz,sql,columns);
        }
        SqlInfo su = new SqlInfo().SetMainTable(BaseModel.GetTableName(clazz)).Append(sql);
        for (String c : columns) {
            Object value = this.GetValue(c);
            if (value != null && value.getClass() == Date.class) {
                Timestamp time = new Timestamp(((Date) value).getTime());
                su.AddParam(time);
            } else {
                su.AddParam(value);
            }
        }
        return BaseQuery.ExecuteSql(su) > 0;
    }


    public int Update(String field, Object value) throws SQLException {
        Map<String, Object> m = new HashMap<>();
        m.put(field, value);
        return Update(m);
    }

    public int Update(Map<String, Object> m) throws SQLException {
        return Update(m, F_id, this.id);
    }

    public int Update(Map<String, Object> m, String field, Object value) throws SQLException {
        if (m.keySet().size() == 0) {
            return 0;
        }
        m.put(F_UpdateTime, new Date());
        return UpdateOnly(m, field, value);
    }

    public int UpdateOnly(Map<String, Object> m, String field, Object value) throws SQLException {
        if (m.keySet().size() == 0) {
            return 0;
        }
        int i=0;
        String tableName = BaseModel.GetTableName(this.getClass());
        SqlInfo su = new SqlInfo().CreateUpdate(tableName);
        for(String key:m.keySet()){
            su.SetEqual(key).AddParam(m.get(key));
        }
        su.WhereEqual(field).AddParam(value);

        try {
             i= BaseQuery.ExecuteSql(su);
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error(su.ToSql());
            String v = "";
            for (Object o : su.GetParams()) {
                v += o.toString() + ",";
            }
            log.error(v);
            throw ex;
        }
        return i;
    }



    public boolean Delete() throws Exception {
        String err = DeleteValidate();
        if (StrUtil.isNotEmpty(err)) {
            throw new XException(err);
        }
        return BaseModel.Delete(this.getClass(), this.id);
    }

    public static <T extends BaseModel> boolean Delete(Class<T> type, String id) throws Exception {
        SqlInfo su = new SqlInfo().CreateDelete(BaseModel.GetTableName(type))
                .WhereEqual(F_id).AddParam(id);
        return BaseQuery.ExecuteSql(su) > 0;
    }

    public static <T extends BaseModel> boolean Delete(Class<T> type, String field, Object v) throws Exception {
        SqlInfo su = new SqlInfo().CreateDelete(BaseModel.GetTableName(type))
                .WhereEqual(field).AddParam(v);
        return BaseQuery.ExecuteSql(su) > 0;
    }

    public static <T extends BaseModel> boolean Delete(Class<T> type, Map<String, Object> v) throws Exception {

        SqlInfo su = new SqlInfo().CreateDelete(BaseModel.GetTableName(type));
        for (String key : v.keySet()) {
            su.AndEqual(key).AddParam(v.get(key));
        }
        return BaseQuery.ExecuteSql(su) > 0;
    }

    public static <T extends BaseModel> T GetObjectById(Class<T> type, String id) throws SQLException {
        if (StrUtil.isEmpty(id)) {
            return null;
        }
        SqlInfo su = new SqlInfo().CreateSelectAll(GetTableName(type))
                .WhereEqual(F_id).AddParam(id);
        T bm = BaseQuery.InfoSql(type, su);
        return bm;
    }


    public static <T extends BaseModel> T GetObjectByFieldValue(Class<T> type, String field, Object v) throws Exception {
        SqlInfo su = new SqlInfo().CreateSelectAll(GetTableName(type))
                .WhereEqual(field).AddParam(v);
        T bm = BaseQuery.InfoSql(type, su);
        return bm;
    }

    public static <T extends BaseModel> T GetObjectByTwoFieldValue(Class<T> type, String field1, Object v1, String field2, Object v2) throws Exception {

        SqlInfo su = new SqlInfo().CreateSelectAll(GetTableName(type))
                .WhereEqual(field1).AddParam(v1).AndEqual(field2).AddParam(v2);

        T bm = BaseQuery.InfoSql(type, su);
        return bm;
    }

    public static <T extends BaseModel> T GetObjectByMapValue(Class<T> type, Map<String, Object> map) throws Exception {
        SqlInfo su = new SqlInfo().CreateSelectAll(GetTableName(type));
        for (String s : map.keySet()) {
            su.AndEqual(s).AddParam(map.get(s));
        }
        T bm = BaseQuery.InfoSql(type, su);
        return bm;
    }

    public static <T extends BaseModel> String GetTableName(Class<T> clazz) {
        String tb = SqlCache.GetTableName(clazz);
        if(StrUtil.isEmpty(tb)){
            tb = clazz.getSimpleName();
            XTable table = clazz.getAnnotation(XTable.class);
            if (table!=null&&StrUtil.isNotEmpty(table.name())) {
                tb = table.name();
            }
            Table table2 = clazz.getAnnotation(Table.class);
            if (table2!=null&&StrUtil.isNotEmpty(table2.name())) {
                tb = table2.name();
            }
            SqlCache.SetTableName(clazz,tb);
        }
        return tb;
    }

    public static <T extends BaseModel> T SetObjectByMap(Class<T> type, Map<String, Object> map) throws Exception {
        T bm = type.newInstance();
        bm.SetValuesByMap(map);
        return bm;
    }

    public <T extends BaseQuery> T CreateQueryModel(){
        return null;
    }

    //生成本表的查询sql
    public SqlInfo CreateSqlInfo(){
        String table =GetTableName(this.getClass());
        SqlInfo su = new SqlInfo().CreateSelect();
        Map<String, Field> columnTypeMap = SqlCache.GetColumnFieldMap(this.getClass());
        Collection<String> lm = columnTypeMap.keySet();
        for (String columnName : lm) {
            if(columnName.equals(F_CreateTime)||columnName.equals(F_UpdateTime)||
                    columnName.equals(F_CreateUser)||columnName.equals(F_UpdateUser)){
                continue;
            }
            su.AppendColumn(table, columnName);
        }
        su.From(table);
        return su;
    }

    public DataTable GetList(BaseQuery bq) throws Exception {
        SqlInfo su = CreateSqlInfo();
        return bq.GetList(su);
    }

    public DataTable GetListExportExcel(BaseQuery bq) throws Exception {
        return this.GetList(bq);
    }

    public Object GetStat(BaseQuery bq) throws Exception {
        String table =GetTableName(this.getClass());
        SqlInfo su = new SqlInfo().CreateSelect().AppendStat(table,bq.StatType,bq.StatField,"value");
        su.From(table);
        return bq.GetValue(su);
    }

    public DataTable GetStatGroup(BaseQuery bq) throws Exception {
        String table =GetTableName(this.getClass());
        SqlInfo su = new SqlInfo().CreateSelect()
                .AppendStat(table,bq.StatType,bq.StatField,"value")
                .AppendColumn(table,bq.GroupField)
                .From(table)
                .AppendGroupBy(bq.GroupField).AppendOrderBy(table,bq.GroupField,true);
        return bq.GetListNoPage(su);
    }


    public String ExportExcelTemplate(HttpServletRequest request) throws Exception {
        String[] cols = TypeConvert.ToTypeValue(String[].class, RequestUtil.GetParameter(request, "columns"));
        String name = GetTableName(this.getClass()) + "导入数据模板";
        return ExportExcelTemplate(cols, name);
    }

    public String ExportExcelTemplate(String[] cols, String name) throws Exception {
        String filename = GetTableName(this.getClass());
        String file = GlobalValues.GetTempFilePath() + "/" + filename + ".xls";
        BaseQuery bq = CreateQueryModel();
        bq.id = "-1";
        DataTable dt = GetList(bq);
        dt.NewRow(); //增加一条空数据
        ArrayList<String> listcol = null;
        if (cols != null && cols.length > 0) {
            listcol = new ArrayList<>();
            for (String c : cols) {
                listcol.add(c);
            }
        }
        ExcelWriteUtil.writeXls(file, name, dt, listcol);
        return file;
    }

    public String ExportExcel(HttpServletRequest request) throws Exception {
        String[] cols = TypeConvert.ToTypeValue(String[].class, RequestUtil.GetParameter(request, "columns"));
        String filename = GetTableName(this.getClass());
        String file = GlobalValues.GetTempFilePath() + "/" + filename + ".xls";
        File f = new File(file);
        if (f.exists()) {
            f.delete();
        }
        BaseQuery bq = CreateQueryModel();
        bq.InitFromRequest(request);
        bq.PageSize = -1L;
        bq.PageIndex = -1L;
        DataTable dt = GetList(bq);
        String name = filename + "导出数据";
        ArrayList<String> listcol = null;
        if (cols != null && cols.length > 0) {
            listcol = new ArrayList<>();
            for (String c : cols) {
                listcol.add(c);
            }
        }
        ExcelWriteUtil.writeXls(file, name, dt, listcol);
        return file;
    }

    public String ExportExcel(HttpServletRequest request, DataTable dt) throws Exception {
        String[] cols = TypeConvert.ToTypeValue(String[].class, RequestUtil.GetParameter(request, "columns"));
        String filename = GetTableName(this.getClass());
        String file = GlobalValues.GetTempFilePath() + "/" + filename + ".xls";
        File f = new File(file);
        if (f.exists()) {
            f.delete();
        }
        String name = filename + "导出数据";
        ArrayList<String> listcol = null;
        if (cols != null && cols.length > 0) {
            listcol = new ArrayList<>();
            for (String c : cols) {
                listcol.add(c);
            }
        }
        ExcelWriteUtil.writeXls(file, name, dt, listcol);
        return file;
    }

    public String ExportExcel(HttpServletRequest request, DataTable dt, String fileName) throws Exception {
        String[] cols = TypeConvert.ToTypeValue(String[].class, RequestUtil.GetParameter(request, "columns"));
//        String filename = ModelUtil.GetTableName(this.getClass());
        String file = GlobalValues.GetTempFilePath() + "/" + fileName + ".xls";
        File f = new File(file);
        if (f.exists()) {
            f.delete();
        }
        String name = fileName + "导出数据";
        ArrayList<String> listcol = null;
        if (cols != null && cols.length > 0) {
            listcol = new ArrayList<>();
            for (String c : cols) {
                listcol.add(c);
            }
        }
        ExcelWriteUtil.writeXls(file, name, dt, listcol);
        return file;
    }

    public String ImportFromExcelCustomSetValue(HttpServletRequest request, Map excelDataRow) throws Exception {
        return "";
    }

    public BaseModel ImportDataGetObjectByUniqueColumns(String[] uniques, Map m) throws Exception {
        Map mm = new HashMap();
        for (String s : uniques) {
            mm.put(s, m.get(s));
        }
        return GetObjectByMapValue(this.getClass(), mm);
    }

    public String ImportExcelDataValidate(DataTable dt, String uniqecolumns) throws Exception {
        if (dt.Data.size() > 0) {
            if (StrUtil.isNotEmpty(uniqecolumns)) {
                String[] uns = StrUtil.split(uniqecolumns, ",");
                for (String uniqecolumn : uns) {
                    if (!dt.Data.get(0).containsKey(uniqecolumn)) {
                        return "文件中不存在唯一列(" + uniqecolumn + ")";
                    }
                }
            } else {
                return "请输入唯一列";
            }
        } else {
            return "文件中数据为空";
        }
        if (StrUtil.isNotEmpty(uniqecolumns)) {
            List<String> listu = new ArrayList<>();
            String repeat = "";
            for (Map m : dt.Data) {
                String u = "";
                String[] uns = StrUtil.split(uniqecolumns, ",");
                for (String uniqecolumn : uns) {
                    u += TypeConvert.ToString(m.get(uniqecolumn));
                }
                if (!listu.contains(u)) {
                    listu.add(u);
                } else {
                    repeat += u + ", ";
                }
            }
            if (StrUtil.isNotEmpty(repeat)) {
                return "在唯一列中有重复项" + repeat + "";
            }
        }
        return "";
    }

    public DataTable ImportExcelDataPreview(HttpServletRequest request, String file, int rowno, String uniqecolumns) throws Exception {
        DataTable dt = ExcelReadUtil.readExcel(file, rowno);
        return dt;
    }

    public String ImportExcelData(HttpServletRequest request, String file, int rowno, String uniqecolumns) throws Exception {
        String error = "";
        int i = 0;
        try {
            DataTable dt = ExcelReadUtil.readExcel(file, rowno);
            error = ImportExcelDataValidate(dt, uniqecolumns);
            if (StrUtil.isNotEmpty(error)) {
                return error;
            }
            String[] ImportColumns = RequestUtil.GetStringArray(request, "导入列");

            String[] uns = StrUtil.split(uniqecolumns, ",");
            String uniqecolumn = "";
            if (uns.length == 1) {
                uniqecolumn = uns[0];
            }
            for (Map m : dt.Data) {
                boolean hasvalue = false;
                for (String u : uns) {
                    String v = TypeConvert.ToString(m.get(u));
                    if (StrUtil.isNotEmpty(v)) {
                        hasvalue = true;
                    }
                }
                if (!hasvalue) {
                    continue;
                }
                BaseModel bm = null;
                if (StrUtil.isNotEmpty(uniqecolumn)) {
                    bm = GetObjectByFieldValue(this.getClass(), uniqecolumn, m.get(uniqecolumn));
                } else {
                    bm = ImportDataGetObjectByUniqueColumns(uns, m);
                }
                if (bm == null) {
                    bm = this.getClass().newInstance();
                }
                if (ImportColumns != null && ImportColumns.length > 0) {
                    for (String c : ImportColumns) {
                        if (m.containsKey(c)) {
                            bm.SetValue(c, m.get(c));
                        }
                    }
                } else {
                    bm.SetValuesByMap(m);
                }
                bm.SetValuesByRequest(request);
                String e = bm.ImportFromExcelCustomSetValue(request, m);
                if (StrUtil.isEmpty(e)) {
                    bm.Save();
                    i++;
                } else {
                    error += e + "\r\n";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            error += e.getMessage();
        }
        return error;
    }
    ///业务类必须初始化测试数据
    public void InitTestData() throws Exception {

    }
    public void DeleteTestData() throws Exception {

    }

}
