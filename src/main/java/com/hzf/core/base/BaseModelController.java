package com.hzf.core.base;

import com.hzf.core.common.AjaxResult;
import com.hzf.core.common.DataTable;
import com.hzf.core.common.GlobalValues;
import com.hzf.core.common.SqlCache;
import com.hzf.core.toolkit.*;
import org.apache.log4j.Logger;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

public abstract class BaseModelController<T extends BaseModel> extends BaseController {
    public static Logger log = Logger.getLogger(BaseModelController.class);
    /**
     * 快速获取泛型的类型的方法
     *
     * @param objectClass 要取泛型的对应的Class
     * @param i           要取第几个泛型(0开始)
     * @param <T>
     * @return 对应的泛型
     */
    public static <T> Class<T> GetGenericClass(Class<? extends Object> objectClass, int i) {
        ParameterizedType type = (ParameterizedType) objectClass.getGenericSuperclass();
        if(type==null){
            return null;
        }
        Type[] types = type.getActualTypeArguments();
        return (Class<T>) types[i];
    }

    public Class<T> GetClass() {
        return GetGenericClass(this.getClass(), 0);
    }

    @XController(name = "统计值")
    @RequestMapping(value = "/statvalue", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String statvalue(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        T model = GetClass().newInstance();
        Object s = model.GetStat(model.CreateQueryModel().InitFromRequest(request));
        if (StrUtil.isEmpty(s)) {
            s = "0";
        }
        return AjaxResult.True(s).ToJson();
    }

    @XController(name = "统计列表")
    @RequestMapping(value = "/statlist", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String statlist(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        T model = GetClass().newInstance();
        DataTable dt = model.GetStatGroup(model.CreateQueryModel().InitFromRequest(request));
        return dt.ToJson();
    }

    @XController(name = "列表")
    @RequestMapping(value = "/querylist", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String querylist(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        T model = GetClass().newInstance();
        DataTable dt = model.GetList(model.CreateQueryModel().InitFromRequest(request));
        return dt.ToJson();
    }

    @XController(name = "列表")
    @RequestMapping(value = "/queryvalue", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String queryvalue(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        T model = GetClass().newInstance();
        BaseQuery bq = model.CreateQueryModel().InitFromRequest(request);
        DataTable dt = model.GetList(bq);
        if (dt.Data.size() > 0) {
            Map m = dt.Data.get(0);
            if (StrUtil.isNotEmpty(bq.QueryField)) {
                return AjaxResult.True(m.get(bq.QueryField)).ToJson();
            }
            return AjaxResult.True(m).ToJson();
        }
        return AjaxResult.False("数据为空").ToJson();
    }

    @XController(name = "详情")
    @RequestMapping(value = "/queryinfo", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String queryinfo(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        String id = RequestUtil.GetString(request, BaseModel.F_id);
        T t = BaseModel.GetObjectById(GetClass(), id);
        if (t != null) {
            return AjaxResult.True(t.ToMap()).ToJson();
        } else {
            t =  GetClass().newInstance();
            t.SetValuesByRequest(request);
            if(t.AutoSetOrder()) {
                String of = t.GetOrderField();
                if (StrUtil.isNotEmpty(of)) {
                    BaseQuery bq = t.CreateQueryModel();
                    if(bq!=null){
                        t.SetValue(of, t.GetMaxOrder(bq.InitFromRequest(request)));
                    }else{
                        t.SetValue(of, t.GetMaxOrder());
                    }

                }
            }
            return AjaxResult.True(t.ToMap()).ToJson();
        }
    }


    @XController(name = "详情")
    @RequestMapping(value = "/querycolumns", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String querycolumns(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        Map<String, Field> columnTypeMap = SqlCache.GetColumnFieldMap(GetClass());
        DataTable dt = new DataTable();
        List<Map> DataColumns = new ArrayList<>();
        Collection<String> lm = columnTypeMap.keySet();
        for (String columnName : lm) {
            if(columnName.equals(BaseModel.F_CreateTime)||columnName.equals(BaseModel.F_UpdateTime)||
                    columnName.equals(BaseModel.F_CreateUser)||columnName.equals(BaseModel.F_UpdateUser)){
                continue;
            }
            Field field = columnTypeMap.get(columnName);
            Map m = DataTable.CreateColumnMap(columnName,field.getType(),true);
            XColumn xc = field.getAnnotation(XColumn.class);
            if(xc!=null){
                if(StrUtil.isNotEmpty(xc.foreignTable())){
                    m.put("foreignTable",xc.foreignTable());
                }
            }
            DataColumns.add(m);
        }
        return AjaxResult.True(DataColumns).ToJson();
    }

    @XController(name = "树详情")
    @RequestMapping(value = "/treeinfo", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String treeinfo(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        T bm = GetClass().newInstance();
        if (bm instanceof BaseModelTree) {
            BaseModelTree bmt = (BaseModelTree) bm;
            String id = RequestUtil.GetParameter(request, BaseModel.F_id);
            DataTable dt = bmt.GetTreePathInfo(id);
            return dt.ToJson();
        }
        return AjaxResult.Error("读取树详情失败").ToJson();
    }

    @XController(name = "保存")
    @RequestMapping(value = "/save", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String save(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        String id = RequestUtil.GetString(request, BaseModel.F_id);
        T model = BaseModel.GetObjectById(GetClass(), id);
        if (model == null) {
            model = GetClass().newInstance();
        }
        model.SetValuesByRequest(request);
        model.Save();
        AjaxResult ar = AjaxResult.True(model);
        return ar.ToJson();
    }

    @XController(name = "列表")
    @RequestMapping(value = "/savevalue", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String savevalue(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        T model = GetClass().newInstance();
        BaseQuery bq = model.CreateQueryModel().InitFromRequest(request);
        DataTable dt = model.GetList(bq);
        if (dt.Data.size() == 1) {
            Map m = dt.Data.get(0);
            if (StrUtil.isNotEmpty(bq.SaveValue)) {
                Map mv = TypeConvert.FromMapJson(bq.SaveValue);
                model.Update(mv, BaseModel.F_id, m.get(BaseModel.F_id));
                return AjaxResult.True().ToJson();
            }
        }
        return AjaxResult.False("数据保存失败，数据数量不唯一" + dt.Data.size()).ToJson();
    }

    @XController(name = "删除和批量删除")
    @RequestMapping(value = "/delete", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String delete(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        String id = RequestUtil.GetParameter(request, BaseModel.F_id);
        if (StrUtil.isNotEmpty(id)) {
            T t = BaseModel.GetObjectById(GetClass(), id);
            if (t != null && !t.IsNew()) {
                t.Delete();
                return AjaxResult.True().ToJson();
            }
            return AjaxResult.Error("记录不存在!").ToJson();
        } else {
            String[] ids = RequestUtil.GetStringArray(request, BaseModel.F_ids);
            if (ids != null && ids.length > 0) {
                for (String d : ids) {
                    T t = BaseModel.GetObjectById(GetClass(), d);
                    if (t != null && !t.IsNew()) {
                        t.Delete();
                    }
                }
                return AjaxResult.True().ToJson();
            } else {
                String deleteAll = RequestUtil.GetParameter(request, "deleteAll");
                if (StrUtil.isNotEmpty(deleteAll) && TypeConvert.ToBoolean(deleteAll)) {
                    BaseModel.Delete(GetClass(), new HashMap<>());
                    return AjaxResult.True().ToJson();
                }
                return AjaxResult.Error("无法删除!").ToJson();
            }
        }
    }

    @XController(name = "唯一值验证")
    @RequestMapping(value = "/unique", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String unique(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {

        String name = RequestUtil.GetParameter(request, "name");
        if (StrUtil.isEmpty(name)) {
            name = RequestUtil.GetParameter(request, "field");
        }
        String value = RequestUtil.GetParameter(request, "value");
        String id = RequestUtil.GetParameter(request, "id");
        boolean b = BaseModel.IsUnique(GetClass(), id, name, value);
        return new AjaxResult(b, null).ToJson();
    }

    @XController(name = "上传文件", isLogin = XController.LoginState.No)
    @RequestMapping(value = "/upload", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String upload(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        List<String> files = HttpUtil.uploadRequest(request);
        return AjaxResult.True(files).ToJson();
    }

    @XController(name = "下载文件", isLogin = XController.LoginState.No)
    @RequestMapping(value = "/download", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String download(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        String filename = RequestUtil.GetParameter(request, "filename");
        Boolean isImage = RequestUtil.GetBooleanParameter(request, "isImage");
        if (StrUtil.isEmpty(filename) || "undefined".equals(filename)) {
            return null;
        }
        if(FileUtil.isExistFile(filename)) {
            if (isImage) {
                HttpUtil.exportImage(response, filename);
            } else {
                HttpUtil.exportFile(response, filename);
            }
            return null;
        }else{
            return AjaxResult.Error("文件不存在").ToJson();
        }
    }

    @XController(name = "下载模板")
    @RequestMapping(value = "/download_excel_template", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String download_excel_template(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        T bm = GetClass().newInstance();
        String path = bm.ExportExcelTemplate(request);
        HttpUtil.exportFile(response, path);
        return null;
    }

    @XController(name = "导出数据")
    @RequestMapping(value = "/export_excel", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String export_excel(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        T bm = GetClass().newInstance();
        String path = bm.ExportExcel(request);
        HttpUtil.exportFile(response, path);
        return null;
    }


    @XController(name = "导入数据")
    @RequestMapping(value = "/import_excel", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String import_excel(HttpServletRequest request, HttpServletResponse response, HttpSession session, Model model) throws Exception {
        T bm = GetClass().newInstance();
        boolean 预览模式 = TypeConvert.ToBoolean(RequestUtil.GetParameter(request, "预览模式"));
        String[] 上传文件列表 = TypeConvert.ToTypeValue(String[].class, RequestUtil.GetParameter(request, "上传文件列表"));
        int 标题行 = TypeConvert.ToTypeValue(Integer.class, RequestUtil.GetParameter(request, "标题行"));
        String 唯一列 = TypeConvert.ToTypeValue(String.class, RequestUtil.GetParameter(request, "唯一列"));

        if (标题行 < 1) {
            return AjaxResult.False("请输入正确的标题行").ToJson();
        }
        if (上传文件列表 != null && 上传文件列表.length > 0) {
            if (预览模式) {
                DataTable dataTable = bm.ImportExcelDataPreview(request, GlobalValues.GetFilePath(上传文件列表[0]), 标题行 - 1, 唯一列);
                BaseQuery queryModel = bm.CreateQueryModel().InitFromRequest(request);
                dataTable = queryModel.FilterTable(dataTable);
                return dataTable.ToPageJson(queryModel);
            }
            String state = "";
            String file = GlobalValues.GetFilePath(上传文件列表[0]);
            state += bm.ImportExcelData(request, file, 标题行 - 1, 唯一列);
            if (StrUtil.isEmpty(state)) {
                return AjaxResult.True().ToJson();
            } else {
                return AjaxResult.False(state).ToJson();
            }
        } else {
            if (预览模式) {
                return new DataTable().ToJson();
            }
        }
        return AjaxResult.False("导入文件不存在").ToJson();
    }


    @XController(name = "初始化测试数据", isLogin = XController.LoginState.No)
    @RequestMapping(value = "/init_test_data", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String init_test_data(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        boolean b = RequestUtil.GetBooleanParameter("all");
        if(!b){
            T model = GetClass().newInstance();
            model.InitTestData();
        }else{
            for (String c : SqlCache.hashMapClasses.keySet()) {
                Class<?> clazz = SqlCache.hashMapClasses.get(c);
                BaseModel model = (BaseModel) clazz.newInstance();
                model.InitTestData();
            }
        }
        return AjaxResult.True().ToJson();
    }

    @XController(name = "删除测试数据", isLogin = XController.LoginState.No)
    @RequestMapping(value = "/delete_test_data", method = RequestMethod.POST, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String delete_test_data(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws Exception {
        boolean b = RequestUtil.GetBooleanParameter("all");
        if(!b){
            T model = GetClass().newInstance();
            model.DeleteTestData();
        }else{
            for (String c : SqlCache.hashMapClasses.keySet()) {
                Class<?> clazz = SqlCache.hashMapClasses.get(c);
                BaseModel model = (BaseModel)clazz.newInstance();
                model.DeleteTestData();
            }
        }
        return AjaxResult.True().ToJson();
    }
}
