package com.hzf.core.toolkit;

import com.alibaba.fastjson.JSONObject;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.hzf.core.base.BaseModel;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import javax.persistence.criteria.CriteriaBuilder;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TypeConvert {
    public static <T> List<T> fromJsonList(String json, Class<T> resClass) throws Exception {
        List<T> list = new ArrayList<>();
        list.addAll(Arrays.asList(new GsonBuilder().registerTypeAdapter(Date.class, new CustomDateAdapter()).create().fromJson(json, TypeToken.getArray(TypeToken.get(resClass).getType()).getType())));
        return list;
    }

    //转List Arrays.asList(TypeConvert.fromJson(now.审核记录JSON, SHJL审核记录[].class))
    public static <T> T fromJson(String json, Class<T> resClass) throws Exception {
        return new GsonBuilder().registerTypeAdapter(Date.class, new CustomDateAdapter()).create().fromJson(json, resClass);
    }

    private static class CustomDateAdapter extends TypeAdapter<Date> {

        @Override
        public void write(JsonWriter out, Date value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(DateUtil.GetDateTimeString(value));
            }
        }

        @Override
        public Date read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            } else {
                return ToDate(in.nextString());
            }
        }
    }

    //region Json的转换
    public static String ToJson(Object o) {
        return ToJson(o, DateUtil.FORMAT_DATETIME);
    }

    public static String ToJson(Object o, String dateFormat) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateFormat);
            GsonBuilder builder = new GsonBuilder().setDateFormat(dateFormat);
            builder.registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (arg0, arg1, arg2) -> new JsonPrimitive(arg0 == null ? "" : arg0.format(formatter)));
            builder.registerTypeAdapter(Time.class, (JsonSerializer<Time>) (arg0, arg1, arg2) -> new JsonPrimitive(arg0 == null ? "" : arg0.toString()));
            builder.registerTypeAdapter(BaseModel.class, (JsonSerializer<BaseModel>) (arg0, arg1, arg2) -> new JsonPrimitive(arg0 == null ? "" : arg0.ToJson()));
            Gson gson = builder.create();
            String json = gson.toJson(o);
            return json;
        } catch (Exception e) {
            return new Gson().toJson(o);
        }
    }

    public static Map<String, Object> FastFromMapJson(String json) {
        return JSONObject.parseObject(json, Map.class);
    }

    public static Map<String, Object> FromMapJson(String json) {
        return FromJson(json, Map.class);
    }


    public static <T extends BaseModel> List<T> FromListMapJson(String json, Class<T> resClass) {
        List<T> ret = new ArrayList<>();
        List<Map<String, Object>> list = FromJson(json, List.class);
        try {
            for (Map<String, Object> row : list) {
                T obj = resClass.newInstance();
                obj.SetValuesByMap(row);
                ret.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<Map<String, Object>> FromListMapJson(String json) {
        return FromJson(json, List.class);
    }

    public static List<Object> FromListJson(String json) {
        return FromJson(json, List.class);
    }

    public static <T> T FromJson(String json, Class<T> resClass) {
        return FromJson(json, DateUtil.FORMAT_DATETIME, resClass);
    }

    public static <T> T FromJson(String json, String dataFormat, Class<T> resClass) {
        if (StrUtil.isEmpty(json)) {
            return null;
        }
        try {
            Gson gson = new GsonBuilder().setDateFormat(dataFormat).create();
            T m = gson.fromJson(json, resClass);
            if (m == null) {
                throw new Exception();
            }
            return m;
        } catch (Exception e) {
            System.err.println(e);
            LogUtil.i(new Date() + ":" + json);
            try {
                return resClass.newInstance();
            } catch (Exception e1) {
                return null;
            }
        }
    }
    //endregion

    //region XML的转换
    public static Map<String, Object> FromMapXML(String xml) {
        Map<String, Object> map = new HashMap<>();
        try {
            Document doc = DocumentHelper.parseText(xml);//将xml转为dom对象
            Element root = doc.getRootElement();//获取根节点
            return _FromMapXML(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private static Map<String, Object> _FromMapXML(Element element) {
        Map<String, Object> map = new HashMap<>();
        String eleName = element.getName();
        String eleValue;
        int attrCount = element.attributeCount();
        if (!StrUtil.isEmpty(element.getText())) {
            eleValue = element.getText();
            map.put(eleName, eleValue);
        }
        if (attrCount > 0) {
            for (int count = attrCount; count > 0; count--) {
                Attribute attribute = element.attribute(count - 1);
                map.put(attribute.getName(), attribute.getValue());
            }
        }
        for (Object o : element.elements()) {
            map.putAll(_FromMapXML((Element) o));
        }
        return map;
    }
    //endregion

    public static <T> T ToTypeValue(Class<T> type, Object value) {
        return (T) TypeConvert.ToType(type, value);
    }

    //region 各种类型的转换, 时间类型转失败会返回null,其他会返回空字符串或0
    // 将数据转换成指定的类型, 以方便其他地方调用
    public static Object ToType(Class<?> type, Object value) {
        Object v = null;
        if (type.equals(String.class)) {
            v = ToString(value);
        } else if (type.equals(String[].class)) {
            if (value instanceof String[]) {
                return value;
            }
            String va = ToString(value);
            v = StrUtil.split(va, ",");
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            v = ToInteger(value);
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            v = ToLong(value);
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            v = ToBoolean(value);
        } else if (type.equals(BigDecimal.class)) {
            v = ToBigDecimal(value);
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            v = ToDouble(value);
        } else if (type.equals(Float.class) || type.equals(float.class)) {
            v = ToFloat(value);
        } else if (type.equals(Date.class)) {
            v = ToDate(value);
        } else if (type.equals(Time.class)) {
            v = ToTime(value);
        }
        return v;
    }

    public static String[] ToStringArray(Object value) {
        if (value instanceof String[]) {
            return (String[]) value;
        }
        String va = ToString(value);
        return StrUtil.split(va, ",");
    }

    // 将Object(String或Time)转为Time类型,若转换失败会返回null
    public static Time ToTime(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(Time.class)) return (Time) value;
            try {
                return Time.valueOf(ToString(value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static Date ToDate(Object value) {
        if (value == null) {
            return null;
        }
        String sd = ToString(value);
        if (sd.equals("null") || sd.equals("")) {
            return null;
        }
        try {
            try {
                return ToDate(value, DateUtil.FORMAT_DATETIME);
            } catch (ParseException e) {
                try {
                    return ToDate(value, DateUtil.FORMAT_DATE);
                } catch (ParseException ee) {
                    return ToDate(value, "yyyy年MM月dd日");
                }
            }
        } catch (ParseException e) {
            String svalue = ToString(value);
            if (svalue.contains("GMT")) {

            }
            try {
                SimpleDateFormat sf1 = new SimpleDateFormat("EEE MMM dd hh:mm:ss z yyyy", Locale.ENGLISH);
                return sf1.parse(svalue);
            } catch (Exception es) {
                //Mon Jun 08 2020 09:53:43 GMT+0800
                SimpleDateFormat sdf = new SimpleDateFormat("E MMM dd yyyy HH:mm:ss z", Locale.US);
                try {
                    return sdf.parse(svalue);
                } catch (Exception ese) {

                }

            }
        }
        try {
            long longTime = ToLong(value);
            if (longTime > 0) {
                return ToDate(longTime);
            }
        } catch (Exception e2) {
        }
        if (sd.contains("/")) {
            try {
                return ToDate(value, DateUtil.FORMAT_DATE2);
            } catch (ParseException e) {
            }
        }
        return null;
    }

    public static Date ToDate(Object value, String format) throws ParseException {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(Date.class)) return (Date) value;
            if (value.getClass().equals(Long.class)) return new Date((Long) value);
            SimpleDateFormat df = new SimpleDateFormat(format);
            return df.parse(ToString(value));
        }
        return null;
    }

    public static Integer ToInteger(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(Integer.class)) return (Integer) value;
            if (value.getClass().equals(Double.class)) return TypeConvert.ToDouble(value).intValue();
            if (value.getClass().equals(Float.class)) return TypeConvert.ToFloat(value).intValue();
            try {
                String str = ToString(value);
                int index = str.indexOf(".");
                if (index >= 0) {
                    str = str.substring(0, index);
                }
                return Integer.valueOf(str);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static Long ToLong(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(Long.class)) return (Long) value;
            if (value.getClass().equals(Double.class)) return TypeConvert.ToDouble(value).longValue();
            if (value.getClass().equals(Integer.class)) return TypeConvert.ToInteger(value).longValue();
            if (value.getClass().equals(Float.class)) return TypeConvert.ToFloat(value).longValue();
            try {
                String v1 = ToString(value);
                if (v1.contains("E")) {
                    try {
                        return new BigDecimal(v1).longValue();
                    } catch (Exception ex) {
                    }
                }
                if (v1.equals("NaN")) {
                    return 0L;
                }
                return Long.valueOf(v1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0L;
    }

    //like 0000011
    public static String FormatNumToString(int num, int length) {
        String o = Integer.toString(num);
        int l = length - o.length();
        String s = "";
        for (int i = 0; i < l; i++) {
            s += "0";
        }
        return s + o;
    }

    public static Double ToDouble(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(Double.class)) return (Double) value;
            try {
                return Double.valueOf(ToString(value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0.0;
    }

    public static Double ToDouble2位小数(Double value) {
        return (double) Math.round(value * 100) / 100;
    }

    public static Double To2Double(Object value) {
        return ToXDouble(value, 2);
    }

    public static Double ToXDouble(Object value, int x) {
        return new BigDecimal(TypeConvert.ToDouble(value)).setScale(x, RoundingMode.HALF_UP).doubleValue();//四舍五入
    }

    public static String To2DoubleString(Object value) {
        return ToXDoubleString(value, 2);
    }

    public static String ToXDoubleString(Object value, int x) {
        Double val = ToXDouble(value, x);
        return String.format("%." + x + "f", val);
    }

    public static Float ToFloat(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(Float.class)) return (Float) value;
            try {
                return Float.valueOf(ToString(value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 0.0f;
    }

    public static BigDecimal ToBigDecimal(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(BigDecimal.class)) return (BigDecimal) value;
            try {
                return new BigDecimal(ToString(value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return BigDecimal.ZERO;
    }

    public static String ToBoolString(Object value){
        if(TypeConvert.ToBoolean(value)){
            return "是";
        }
        return "否";
    }

    public static String ToString(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(String.class)) return (String) value;
            try {
                return value.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    public static Boolean ToBoolean(Object value) {
        if (!StrUtil.isEmpty(value)) {
            if (value.getClass().equals(Boolean.class)) return (Boolean) value;
            if ("1".equals(value)) return true;
            if ("是".equals(value)) return true;
            try {
                return Boolean.valueOf(value.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    //endregion

    public static int CompareValue(Object o1,Object o2){
        if(o1.getClass().equals(Date.class)){
            Date d1=TypeConvert.ToDate(o1);
            Date d2=TypeConvert.ToDate(o2);
            return d1.compareTo(d2);
        }else if(o1.getClass().equals(String.class)){
            String s1 = TypeConvert.ToString(o1);
            String s2 = TypeConvert.ToString(o2);
            return s1.compareTo(s2);
        }else if(o1.getClass().equals(Integer.class)||o1.getClass().equals(int.class)){
            Integer i1=TypeConvert.ToInteger(o1);
            Integer i2 = TypeConvert.ToInteger(o2);
            return i1.compareTo(i2);
        }else if(o1.getClass().equals(Double.class)||o1.getClass().equals(double.class)){
            Double i1=TypeConvert.ToDouble(o1);
            Double i2 = TypeConvert.ToDouble(o2);
            return i1.compareTo(i2);
        }else if(o1.getClass().equals(Long.class)||o1.getClass().equals(long.class)){
            Long i1=TypeConvert.ToLong(o1);
            Long i2 = TypeConvert.ToLong(o2);
            return i1.compareTo(i2);
        }

        return 0;
    }


    // 将集合类型转为数组
    public static Object[] ToArrayByCollection(Collection list) {
        if (list == null) return null;
        return list.toArray();
    }

    // 将集合类型转为数组
    public static String[] ToArrayStringByCollection(Collection<String> list) {
        if (list == null) return null;
        String[] strArray = new String[list.size()];
        list.toArray(strArray);
        return strArray;
    }

    // 将集合类型枚举类型
    public static String[] ToArrayByEnum(Class<? extends Enum> type) {
        String[] a = {};
        List<String> ls = new ArrayList<>();
        if (type.isEnum()) {
            Enum[] o = type.getEnumConstants();
            for (Enum e : o) {
                ls.add(e.name());
            }
        }
        return ls.toArray(a);
    }

    public static String ToJsonByEnum(Class<? extends Enum> type) throws InvocationTargetException, IllegalAccessException {
        Method getValue = null;
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            if ("getValue".equals(method.getName())) {
                getValue = method;
                break;
            }
        }
        if (getValue != null) {
            Enum[] enumConstants = type.getEnumConstants();
            Map<String, String> map = new HashMap<>();
            for (int i = 0; i < enumConstants.length; i++) {
                Enum anEnum = enumConstants[i];
                String key = TypeConvert.ToString(getValue.invoke(anEnum));
                map.put(key, anEnum.name());
            }
            return TypeConvert.ToJson(map);
        } else {
            return TypeConvert.ToJson(ToArrayByEnum(type));
        }
    }

    public static String RequestToString(HttpServletRequest request) throws IOException {
        return ToStringByBufferedReader(request.getReader());
    }

    private static String ToStringByBufferedReader(BufferedReader br) {
        String inputLine;
        String str = "";
        try {
            while ((inputLine = br.readLine()) != null) {
                str += inputLine;
            }
            br.close();
        } catch (IOException e) {
            LogUtil.i("IOException: " + e);
        }
        return str;
    }

    public static <T extends Enum<T>> T ToEnumByString(Class<T> t, String name) {
        try {
            return Enum.valueOf(t, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
