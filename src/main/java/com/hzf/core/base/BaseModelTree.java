package com.hzf.core.base;

import com.hzf.core.common.*;
import com.hzf.core.toolkit.RequestUtil;
import com.hzf.core.toolkit.StrUtil;
import com.hzf.core.toolkit.TypeConvert;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseModelTree extends BaseModel {
    public static final String RootParentId = "0";
    public static final String TreePathSplit = "/";

    public static final String F_Children = "children";
    public static final String F_Parent = "parent";
    public static final String F_Parentids = "Parentids";

    @XColumn
    @XIndex
    public String Parentid;
    public static final String F_Parentid = "Parentid";

    @Column(name = F_Parentid)
    public String getParentid() {
        return Parentid;
    }
    public void setParentid(String value) {
        this.Parentid = value;
    }

    BaseModelTree parent;

    public BaseModelTree GetParent() throws Exception {
        if (parent == null) {
            parent = GetObjectById(this.getClass(), this.Parentid);
        }
        return parent;
    }

    //TreePath是树的一个重要属性,会带来很大便利,任何树的数据都有存这个属性,但主要作为冗余字段存在
    @XColumn(length = 1000)
    @XIndex
    public String TreePath;
    public static final String F_TreePath = "TreePath";


    @Column(name = F_TreePath, length = 1000)
    public String getTreePath() {
        return TreePath;
    }

    public void setTreePath(String value) {
        this.TreePath = value;
    }

    @XColumn(length = 1000)
    @XIndex
    public String TreeName;
    public static final String F_TreeName = "TreeName";

    @Column(name = F_TreeName, length = 1000)
    public String getTreeName() {
        return TreeName;
    }
    public void setTreeName(String value) {
        this.TreeName = value;
    }

    //0000001，0000001000002，0000011，这样的结构才能排序清楚吧
    @XColumn(length = 1000)
    @XIndex
    public String TreeOrder;
    public static final String F_TreeOrder = "TreeOrder";

    @Column(name = F_TreeOrder, length = 1000)
    public String getTreeOrder() {
        return TreeOrder;
    }
    public void setTreeOrder(String value) {
        this.TreeOrder = value;
    }

    //树的层级，供查询使用
    @XColumn
    @XIndex
    public int TreeLevel;
    public static final String F_TreeLevel = "TreeLevel";
    @Column(name = F_TreeLevel)
    public int getTreeLevel() {
        return TreeLevel;
    }
    public void setTreeLevel(int value) {
        this.TreeLevel = value;
    }

    public String[] GetParentids() {
        return StrUtil.split(this.getTreePath(), BaseModelTree.TreePathSplit);
    }

    @XColumn
    public boolean IsTreeLeaf = true;
    public final static String F_IsTreeLeaf = "IsTreeLeaf";

    @Column(name = F_IsTreeLeaf)
    public boolean getIsTreeLeaf() {
        return IsTreeLeaf;
    }
    public void setIsTreeLeaf(boolean value) {
        this.IsTreeLeaf = value;
    }

    public Boolean IsRoot(){
        return this.TreeLevel==0;
    }

    @Override
    public void Save() throws Exception {
        if (StrUtil.isEmpty(this.getParentid())) this.setParentid(RootParentId);
        this.SaveTreeValidate();
        boolean isNew = this.IsNew();
        BaseModelTree old = null;
        if (isNew) {
            this.id = GetUniqueId();
        } else {
            old = GetObjectById(this.getClass(), this.id);
        }
        BaseModelTree pjg = GetParent();
        String nameField = GetNameField();
        String orderField = GetOrderField();

        if (pjg != null) {
            if ((pjg.TreePath+"/").contains("/" + this.id+"/")) {
                throw new XException(this.GetValue(nameField)+"的父节点不能选择自己或者下级!");
            }
            pjg.Update(BaseModelTree.F_IsTreeLeaf, false);
            if (StrUtil.isNotEmpty(nameField)) {
                this.TreeName = pjg.TreeName + TreePathSplit + this.GetValue(nameField);
            }
            this.TreePath = pjg.TreePath + TreePathSplit + this.id;
            this.TreeLevel = pjg.TreeLevel + 1;
            if (StrUtil.isNotEmpty(orderField)) {
                int order = TypeConvert.ToInteger(this.GetValue(orderField));
                if (order <= 0 && this.AutoSetOrder()) {
                    order = GetMaxOrder(new SqlInfo().WhereEqual(F_Parentid).AddParam(pjg.id));
                    this.SetValue(orderField, order);
                }
                this.TreeOrder = pjg.TreeOrder + TreePathSplit + GetFormatTreeOrder(order);
            }
        } else {
            if (StrUtil.isNotEmpty(nameField)) {
                this.TreeName = TreePathSplit + this.GetValue(nameField);
            }
            this.TreePath = TreePathSplit + this.id;
            this.TreeLevel = 0;
            if (StrUtil.isNotEmpty(orderField)) {
                int order =TypeConvert.ToInteger(this.GetValue(orderField));
                if (order <= 0 && this.AutoSetOrder()) {
                    order = GetMaxOrder(new SqlInfo().WhereEqual(F_Parentid).AddParam(RootParentId));
                    this.SetValue(orderField, order);
                }
                this.TreeOrder = TreePathSplit + GetFormatTreeOrder(order);
            }
        }
        if (!isNew) {
            if (old != null&&old.TreePath!=null) {
                if (!old.TreePath.equals(this.TreePath)) {
                    int level = this.TreeLevel - old.TreeLevel;
                    String table = GetTableName(this.getClass());
                    SqlInfo su = new SqlInfo().CreateUpdate(table);
                    su.Set(table + "." + BaseModelTree.F_TreeLevel + "=" + table + "." + BaseModelTree.F_TreeLevel + "+" + level);
                    su.Append(table + "." + BaseModelTree.F_TreePath + " = REPLACE(" + table + "." + BaseModelTree.F_TreePath + ",?,?)")
                            .AddParam(old.getTreePath() + "/").AddParam(this.getTreePath() + "/");
                    if (StrUtil.isNotEmpty(nameField)) {
                        su.Append(table + "." + BaseModelTree.F_TreeName + " = REPLACE(" + table + "." + BaseModelTree.F_TreeName + ",?,?)")
                                .AddParam(old.getTreeName() + "/").AddParam(this.getTreeName() + "/");
                    }
                    if (StrUtil.isNotEmpty(orderField)) {
                        su.Append(table + "." + BaseModelTree.F_TreeOrder + " = REPLACE(" + table + "." + BaseModelTree.F_TreeOrder + ",?,?)")
                                .AddParam(old.getTreeOrder() + "/").AddParam(this.getTreeOrder() + "/");
                    }
                    su.Where(table + "." + BaseModelTree.F_TreePath + " like ?")
                            .AddParam(old.getTreePath() + "/%");

                    BaseQuery.ExecuteSql(su);
                } else {    //修改所有的下级
                    if (StrUtil.isNotEmpty(nameField) && !this.TreeName.equals(old.TreeName)) {
                        String table = GetTableName(this.getClass());
                        SqlInfo su = new SqlInfo().CreateUpdate(table);
                        su.Set(table + "." + BaseModelTree.F_TreeName + " = REPLACE(" + table + "." + BaseModelTree.F_TreeName + ",?,?)")
                                .AddParam(old.getTreeName() + "/").AddParam(this.getTreeName() + "/");
                        su.Where(table + "." + BaseModelTree.F_TreePath + " like ?")
                                .AddParam(old.getTreePath() + "/%");

                        BaseQuery.ExecuteSql(su);
                    }
                    if (StrUtil.isNotEmpty(orderField) && !this.TreeOrder.equals(old.TreeOrder)) {
                        String table = GetTableName(this.getClass());
                        SqlInfo su = new SqlInfo().CreateUpdate(table);
                        su.Set(table + "." + BaseModelTree.F_TreeOrder + " = REPLACE(" + table + "." + BaseModelTree.F_TreeOrder + ",?,?)")
                                .AddParam(old.getTreeOrder() + "/").AddParam(this.getTreeOrder() + "/");
                        su.Where(table + "." + BaseModelTree.F_TreePath + " like ?")
                                .AddParam(old.getTreePath() + "/%");
                        BaseQuery.ExecuteSql(su);
                    }
                }
            }
        }

        super.Save();
    }
    public AjaxResult SaveTreeValidate() throws Exception {

        if (Parentid.equals(id)) {
            throw new XException("自己不能做自己的父节点!");
        }
        //不是很必要
//        if (!RootParentId.equals(this.getParentid())) {
//            BaseModelTree bm = GetObjectById(this.getClass(), this.Parentid);
//            if (IsNew(bm)) {
//                throw new MsgException("父节点丢失!");
//            }
//        }
        String err = "";
        List<Map> listunique = this.GetUniqueFields();
        if (listunique.size() > 0) {
            for (Map f : listunique) {
                if (!f.containsKey(F_Parentid)) {
                    f.put(F_Parentid, this.getParentid());
                }
                if (!IsUnique(this.getClass(), this.id, f)) {
                    err = f + "已存在";
                    break;
                }
            }
        }
        if (StrUtil.isNotEmpty(err)) {
            throw new XException(err);
        }
        return AjaxResult.True();
    }
    public String GetFormatTreeOrder(int order) {  //TreeOrder ：10000001,算数字和0的个数
        String o = Integer.toString(order);
        int l = 6 -o.length();
        String s = "";
        for(int i=0;i<l;i++){
            s+="0";
        }
        return this.TreeLevel+s+o;
    }
    @Override
    public boolean Delete() throws Exception {
        if (StrUtil.isEmpty(Parentid)) {
            BaseModel old = GetObjectById(getClass(), id);
            if (old != null) {
                this.SetValuesByMap(old.ToMap());
            }else{
                return  false;
            }
        }
        // 这里不直接批量删除子类第一是因为要递归向下删除,且删除之前要进行是否可删除的校验.
        String table = GetTableName(this.getClass());

        SqlInfo su = new SqlInfo().CreateSelect().AppendColumn(table,F_id).From(table)
                .WhereEqual(F_Parentid).AddParam(this.id);
        DataTable dt = BaseQuery.ListSql(su, null);
        for (Map m : dt.Data) {
            BaseModelTree model = this.getClass().newInstance();
            model.id = (TypeConvert.ToString(m.get(F_id)));
            model.Delete();
        }
        boolean ret = super.Delete();
        if (ret && !RootParentId.equals(Parentid)) {      //将父级设为叶子节点
            SqlInfo su2 = new SqlInfo().CreateSelect();
            su2.AppendCountColumn(table, F_id ,F_id);
            su2.From(table);
            su2.Where(F_Parentid + "=?").AddParam(Parentid);
            if (BaseQuery.LongSql(su2) == 0) {
                BaseModelTree model = this.getClass().newInstance();
                model.id=(Parentid);
                model.Update(BaseModelTree.F_IsTreeLeaf, true);
            }
        }
        return ret;
    }

    @Override
    public int GetMaxOrder(SqlInfo sqlWhere) throws SQLException {
        String orderField = GetOrderField();
        int order = 0;
        if(StrUtil.isNotEmpty(orderField)) {
            SqlInfo su = new SqlInfo().CreateSelect(" max("+orderField+") " )
                    .From(GetTableName(this.getClass()));
            if(sqlWhere!=null){
                su.Where(sqlWhere.ToWhere()).AddParams(sqlWhere.GetParamsList());
            }else {
                if (StrUtil.isEmpty(this.Parentid)) {
                    su.Where(F_Parentid + "=?").AddParam(RootParentId);
                } else {
                    su.Where(F_Parentid + "=?").AddParam(this.Parentid);
                }
            }
            order =TypeConvert.ToInteger(BaseQuery.ObjectSql(su));
        }
        return order+1;
    }

    //继承,多态
    public Map<String, Object> GetTreeList(HttpServletRequest request) throws Exception {
        BaseQuery qm = CreateQueryModel();
        qm.InitFromRequest(request);
        Map<String, Object> m = new HashMap<>();
        String id = TypeConvert.ToString(RequestUtil.GetParameter(request, F_id));
        if (StrUtil.isNotEmpty(id)) {
            qm.id = id;
            DataTable dt = this.GetList(qm);
            if (dt.Data.size() > 0) {
                m.put("id" + id, dt.Data.get(0));
            }
        }
        String pid = TypeConvert.ToString(RequestUtil.GetParameter(request, F_Parentid));
        if (StrUtil.isNotEmpty(pid)) {
            qm.id = "";
            qm.Parentid = pid;
            qm.NotPagination();
            DataTable dt = this.GetList(qm);
            m.put(pid, dt.Data);
        }
        String[] pids = (String[]) TypeConvert.ToType(String[].class, RequestUtil.GetParameter(request, F_Parentids));
        if (pids != null) {
            for (String s : pids) {
                qm.id = "";
                qm.Parentid = s;
                qm.Parentids = null;
                qm.NotPagination();
                DataTable dt = this.GetList(qm);
                m.put(s, dt.Data);
            }
        }
        return m;
    }
    public DataTable GetListChildren() throws Exception {
        BaseQuery bq = this.CreateQueryModel();
        bq.Parentid=this.id;
        return this.GetList(bq);
    }
    public DataTable GetTreePathInfo(String id) throws Exception {
        DataTable dt = new DataTable();
        BaseModelTree info = GetObjectById(this.getClass(), id);
        if (info != null) {
            String treepath = info.getTreePath();
            String[] trees = treepath.split(TreePathSplit);
            for (String t : trees) {
                BaseModelTree bmt = GetObjectById(this.getClass(), t);
                if (bmt != null) {
                    dt.AddRow(bmt.ToMap());
                }
            }
        }
        return dt;
    }
    public static <T extends BaseModelTree> T GetObjectByTreePath(Class<T> type, String treePath) throws Exception {
        if (StrUtil.isEmpty(treePath)) {
            return null;
        }
        String[] ids = treePath.split(BaseModelTree.TreePathSplit);
        if(ids.length>0){
            return GetObjectById(type,ids[ids.length-1]);
        }
        return null;
    }
    public static int GetMaxLevelFromTreeTable(DataTable dt){
        int maxlevel=1;
        for(Map m :dt.Data){
            int l = TypeConvert.ToInteger(m.get(BaseModelTree.F_TreeLevel));
            if(maxlevel<l){
                maxlevel = l;
            }
        }
        maxlevel = maxlevel+1;
        return maxlevel;
    }

    public void SetParentRowsForQueryTree(DataTable dt) throws Exception {
        HashMap hash = new HashMap();
        for (Map m : dt.Data) {
            String treepath = TypeConvert.ToString(m.get(BaseModelTree.F_TreePath));
            String[] trees = treepath.split(BaseModelTree.TreePathSplit);
            for (String t : trees) {
                if (!hash.containsKey(t)&&dt.GetRowByIDField(t)==null) {
                    BaseModelTree bmt = (BaseModelTree) BaseModelTree.GetObjectById(this.getClass(), t);
                    if (bmt != null) {
                        dt.AddRow(bmt.ToMap());
                    }
                    hash.put(t, bmt);
                }
            }
        }
    }

    @Override
    public String ImportFromExcelCustomSetValue(HttpServletRequest request, Map excelDataRow) throws Exception {
        String 上级列名 = TypeConvert.ToTypeValue(String.class, RequestUtil.GetParameter(request, "上级列名"));
        if(StrUtil.isNotEmpty(上级列名)){
            BaseModel p = BaseModel.GetObjectByFieldValue(this.getClass(),上级列名,excelDataRow.get(上级列名));
            if(p!=null){
                this.Parentid = p.id;
            }else{
                return "上级列名:"+上级列名+excelDataRow.get(上级列名)+"不存在";
            }
        }
        return "";
    }

}
