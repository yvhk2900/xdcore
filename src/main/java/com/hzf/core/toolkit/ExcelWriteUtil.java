package com.hzf.core.toolkit;

import com.hzf.core.common.DataTable;
import jxl.Sheet;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.VerticalAlignment;
import jxl.write.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

//生成2003版本的excel
public class ExcelWriteUtil {
    public static final String CHARSET_NAME = "utf-8";

    public static void main(String[] args) {
        try {
            DataTable dt = new DataTable();
            dt.Data = new ArrayList<Map<String, Object>>();
            for (int j = 0; j < 13; j++) {
                Map<String, Object> map = new HashMap<String, Object>();
                for (int i = 0; i < 9; i++) {
                    String key = "tangbin" + i;
                    map.put(key, key);
                }
                dt.Data.add(map);
            }
            String filePath = "C:\\Users\\tangbin\\Desktop\\test.xls";
            writeXls(filePath, "测试数据", dt);
            DataTable dataTable = ExcelReadUtil.readExcel(filePath, 1);
            System.out.println(TypeConvert.ToJson(dataTable.Data));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // String filePath = "C:\\Users\\tangbin\\Desktop\\test.xls";
        // try {
        // WritableWorkbook wwb = Workbook.createWorkbook(new File(filePath));
        // WritableSheet ws = wwb.createSheet("Test Sheet 1", 0);
        // File file = new File("C:\\Users\\tangbin\\Desktop\\123.png");
        // WritableImage image = new WritableImage(0, 0, 1, 1, file);
        // ws.addImage(image);
        // wwb.write();
        // wwb.close();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
    }

    public static String exportPath(HttpServletRequest request) {
        String dateName = "";
        Calendar date = Calendar.getInstance();
        dateName += String.valueOf(date.get(Calendar.YEAR));
        dateName += String.format("%02d", (date.get(Calendar.MONTH) + 1));
        dateName += String.format("%02d", (date.get(Calendar.DATE)));
        dateName += String.format("%02d", (date.get(Calendar.HOUR_OF_DAY)));
        dateName += String.format("%02d", (date.get(Calendar.MINUTE)));
        dateName += String.format("%02d", (date.get(Calendar.SECOND)));
        dateName += String.format("%03d", (date.get(Calendar.MILLISECOND)));
        dateName += "_" + String.format("%05d", (new Random(10000)).nextInt());
        String filePath = request.getSession().getServletContext().getRealPath("") + "/export/xls/" + dateName + ".xls";
        return filePath;
    }

    public static boolean exportXls(HttpServletRequest request, HttpServletResponse response, String name, String title, DataTable dt) {
        return exportXls(request, response, name, title, dt, null);
    }

    public static boolean exportXls(HttpServletRequest request, HttpServletResponse response, String name, String title, DataTable dt, List<String> cols) {
        return exportXls(request, response, name, title, dt, cols, null);
    }

    public static boolean exportXls(HttpServletRequest request, HttpServletResponse response, String name, String title, DataTable dt, List<String> cols, WriteXlsHandler handler) {
        boolean ret = false;
        OutputStream output = null;
        FileInputStream fis = null;
        try {
            response.reset();
            response.setContentType("application/x-msdownload");
            response.setHeader("Content-Disposition", "attachment; filename="
                    + new String(name.getBytes(CHARSET_NAME), "ISO_8859_1") + ".xls");
            output = response.getOutputStream();
            writeXls(output, title, dt, cols, handler);
            output.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(fis);
            close(output);
        }
        return ret;
    }

    public static boolean writeXls(String filePath, String title, DataTable dt) {
        return writeXls(filePath, title, dt, null);
    }

    public static boolean writeXls(String filePath, String title, DataTable dt, List<String> cols) {
        boolean ret = false;
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            ret = writeXls(out, title, dt, cols, null);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(out);
        }
        return ret;
    }

    public static boolean writeXls(OutputStream filePath, String title, DataTable dt, List<String> cols, WriteXlsHandler handler) {
        boolean ret = false;
        if (dt != null) {
            if (cols == null) {
                cols = new ArrayList<String>();
                if (dt != null && dt.DataSchema != null && dt.DataSchema.size() > 0) {
                    for (String key : dt.DataSchema.keySet()) {
                        cols.add(key);
                    }
                } else {
                    if (dt != null && dt.Data.size() > 0) {
                        Map<String, Object> map = dt.Data.get(0);
                        ArrayList<String> _cols = new ArrayList<String>();
                        _cols.add("id");
                        _cols.add("Parentid");
                        _cols.add("children");
                        _cols.add("CreateTime");
                        _cols.add("UpdateTime");
                        for (String key : map.keySet()) {
                            boolean exists = false;
                            for (String _col : _cols) {
                                if (key.equalsIgnoreCase(_col)) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) {
                                cols.add(key);
                            }
                        }
                    }
                }
            }
            WritableWorkbook workbook = null;
            try {
                workbook = Workbook.createWorkbook(filePath);
                String _title = "数据";
                if (StrUtil.isNotEmpty(title)) {
                    _title = title;
                }
                WritableSheet sheet = workbook.createSheet(_title, 0);// 设置sheet名字
                // 设置宽度
                for (int i = 0; i < cols.size(); i++) {
                    sheet.setColumnView(i, 20);// 定义第1列，及其宽度
                }
                sheet.setRowView(0, 700);// 行高度
                sheet.setRowView(1, 700);// 行高度
                sheet.getSettings().setRightMargin(0.5D);// 离右边0.5d

                // 设置文档样式
                WritableFont NormalFont = new WritableFont(WritableFont.ARIAL,
                        12);
                WritableFont BoldFont = new WritableFont(WritableFont.ARIAL,
                        16, WritableFont.BOLD);// 字体：arial 大小：16 粗体
                WritableFont tableFont = new WritableFont(WritableFont.ARIAL,
                        14, WritableFont.NO_BOLD);// 字体：arial 大小：14 不是粗体
                WritableFont baodanFont = new WritableFont(WritableFont.ARIAL,
                        12, WritableFont.BOLD);// 字体：arial 大小：12 粗体

                WritableCellFormat wcf_title = new WritableCellFormat(BoldFont);
                wcf_title.setBorder(Border.ALL, BorderLineStyle.THIN);// 边框为空，细线条
                wcf_title.setVerticalAlignment(VerticalAlignment.CENTRE);// 垂直居中
                wcf_title.setAlignment(Alignment.CENTRE);// 居中
                wcf_title.setWrap(true);

                WritableCellFormat wcf_tabletitle = new WritableCellFormat(
                        tableFont);
                wcf_tabletitle.setBorder(Border.NONE, BorderLineStyle.THIN);
                wcf_tabletitle.setVerticalAlignment(VerticalAlignment.CENTRE);
                wcf_tabletitle.setAlignment(Alignment.CENTRE);
                wcf_tabletitle.setWrap(true);

                WritableCellFormat wcf_left = new WritableCellFormat(NormalFont);
                wcf_left.setBorder(Border.ALL, BorderLineStyle.THIN);
                wcf_left.setVerticalAlignment(VerticalAlignment.CENTRE);
                wcf_left.setAlignment(Alignment.LEFT);
                wcf_left.setWrap(true);

                WritableCellFormat wcf_center = new WritableCellFormat(
                        NormalFont);
                wcf_center.setBorder(Border.ALL, BorderLineStyle.THIN);
                wcf_center.setVerticalAlignment(VerticalAlignment.CENTRE);
                wcf_center.setAlignment(Alignment.CENTRE);
                wcf_center.setWrap(true);

                WritableCellFormat wcf_titlecenter = new WritableCellFormat(
                        BoldFont);
                wcf_titlecenter.setBorder(Border.ALL, BorderLineStyle.THIN);
                wcf_titlecenter.setVerticalAlignment(VerticalAlignment.CENTRE);
                wcf_titlecenter.setAlignment(Alignment.CENTRE);
                wcf_titlecenter.setWrap(true);

                WritableCellFormat wcf_right = new WritableCellFormat(
                        NormalFont);
                wcf_right.setBorder(Border.ALL, BorderLineStyle.THIN);
                wcf_right.setVerticalAlignment(VerticalAlignment.CENTRE);
                wcf_right.setAlignment(Alignment.RIGHT);
                wcf_right.setWrap(false);

                WritableCellFormat wcf_merge = new WritableCellFormat(
                        NormalFont);
                wcf_merge.setVerticalAlignment(VerticalAlignment.TOP);// 垂直靠上
                wcf_merge.setAlignment(Alignment.LEFT);
                wcf_merge.setWrap(true);

                WritableCellFormat wcf_table = new WritableCellFormat(
                        NormalFont);
                wcf_table.setBorder(Border.ALL, BorderLineStyle.THIN);
                wcf_table.setVerticalAlignment(VerticalAlignment.CENTRE);
                wcf_table.setAlignment(Alignment.CENTRE);
                wcf_table.setBackground(Colour.GRAY_25);// 背景颜色
                wcf_table.setWrap(true);

                int startRowNum = 0;
                if (StrUtil.isNotEmpty(title)) {
                    if (cols.size() > 0) {
                        sheet.mergeCells(0, 0, cols.size() - 1, 0);// 合并单元格 "先列再行"
                    } else {
                        sheet.mergeCells(0, 0, 10, 0);// 合并单元格 "先列再行"
                    }
                    sheet.addCell(new Label(0, 0, title, wcf_titlecenter));
                    startRowNum++;
                }

                for (int i = 0; i < cols.size(); i++) {
                    sheet.addCell(new Label(i, startRowNum, cols.get(i), wcf_title));// 第1列标题
                }
                startRowNum++;
                String value = null;
                for (int row = 0, len = dt.Data.size(); row < len; row++) {
                    int _row = row + startRowNum;// 加上标题两行
                    Map<String, Object> map = dt.Data.get(row);
                    sheet.setRowView(_row, 600);
                    for (int col = 0; col < cols.size(); col++) {
                        String key = cols.get(col);
                        if (handler == null
                                || !handler.handerImage(key, map, sheet, col,
                                _row)) {
                            if (map.get(key) == null) {
                                value = "";
                            } else {
                                value = map.get(key).toString();
                            }
                            sheet.addCell(new Label(col, _row, value,
                                    wcf_center));
                        }
                    }
                }
                if (handler != null) {
                    handler.sheetComplete(sheet);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (workbook != null) {
                    try {
                        workbook.write();
                        ret = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        workbook.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return ret;
    }

    public static boolean writeAllXls(String filePath, ArrayList<String> titles, ArrayList<DataTable> dts, ArrayList<String> cols) {
        boolean ret = false;
        File file = new File(filePath);
        file.getParentFile().mkdirs();
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            ret = writeAllXls(out, titles, dts, cols, null);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            close(out);
        }
        return ret;
    }

    public static boolean writeAllXls(OutputStream filePath, ArrayList<String> titles, ArrayList<DataTable> dts, ArrayList<String> cols, WriteXlsHandler handler) {
        boolean ret = false;
        if (dts != null && dts.size() == titles.size()) {
            WritableWorkbook workbook = null;
            try {
                workbook = Workbook.createWorkbook(filePath);
                int w = 0;
                for (DataTable dt : dts) {//循环创建多个Sheet
                    WritableSheet sheet = workbook.createSheet(titles.get(w), w);// 设置sheet名字
                    // 设置宽度
                    for (int i = 0; i < cols.size(); i++) {
                        sheet.setColumnView(i, 20);// 定义第1列，及其宽度
                    }
                    sheet.setRowView(0, 700);// 行高度
                    sheet.setRowView(1, 700);// 行高度
                    sheet.getSettings().setRightMargin(0.5D);// 离右边0.5d

                    // 设置文档样式
                    WritableFont NormalFont = new WritableFont(WritableFont.ARIAL,
                            12);
                    WritableFont BoldFont = new WritableFont(WritableFont.ARIAL,
                            16, WritableFont.BOLD);// 字体：arial 大小：16 粗体
                    WritableFont tableFont = new WritableFont(WritableFont.ARIAL,
                            14, WritableFont.NO_BOLD);// 字体：arial 大小：14 不是粗体
                    WritableFont baodanFont = new WritableFont(WritableFont.ARIAL,
                            12, WritableFont.BOLD);// 字体：arial 大小：12 粗体

                    WritableCellFormat wcf_title = new WritableCellFormat(BoldFont);
                    wcf_title.setBorder(Border.ALL, BorderLineStyle.THIN);// 边框为空，细线条
                    wcf_title.setVerticalAlignment(VerticalAlignment.CENTRE);// 垂直居中
                    wcf_title.setAlignment(Alignment.CENTRE);// 居中
                    wcf_title.setWrap(true);

                    WritableCellFormat wcf_tabletitle = new WritableCellFormat(
                            tableFont);
                    wcf_tabletitle.setBorder(Border.NONE, BorderLineStyle.THIN);
                    wcf_tabletitle.setVerticalAlignment(VerticalAlignment.CENTRE);
                    wcf_tabletitle.setAlignment(Alignment.CENTRE);
                    wcf_tabletitle.setWrap(true);

                    WritableCellFormat wcf_left = new WritableCellFormat(NormalFont);
                    wcf_left.setBorder(Border.ALL, BorderLineStyle.THIN);
                    wcf_left.setVerticalAlignment(VerticalAlignment.CENTRE);
                    wcf_left.setAlignment(Alignment.LEFT);
                    wcf_left.setWrap(true);

                    WritableCellFormat wcf_center = new WritableCellFormat(
                            NormalFont);
                    wcf_center.setBorder(Border.ALL, BorderLineStyle.THIN);
                    wcf_center.setVerticalAlignment(VerticalAlignment.CENTRE);
                    wcf_center.setAlignment(Alignment.CENTRE);
                    wcf_center.setWrap(true);

                    WritableCellFormat wcf_titlecenter = new WritableCellFormat(
                            BoldFont);
                    wcf_titlecenter.setBorder(Border.ALL, BorderLineStyle.THIN);
                    wcf_titlecenter.setVerticalAlignment(VerticalAlignment.CENTRE);
                    wcf_titlecenter.setAlignment(Alignment.CENTRE);
                    wcf_titlecenter.setWrap(true);

                    WritableCellFormat wcf_right = new WritableCellFormat(
                            NormalFont);
                    wcf_right.setBorder(Border.ALL, BorderLineStyle.THIN);
                    wcf_right.setVerticalAlignment(VerticalAlignment.CENTRE);
                    wcf_right.setAlignment(Alignment.RIGHT);
                    wcf_right.setWrap(false);

                    WritableCellFormat wcf_merge = new WritableCellFormat(
                            NormalFont);
                    wcf_merge.setVerticalAlignment(VerticalAlignment.TOP);// 垂直靠上
                    wcf_merge.setAlignment(Alignment.LEFT);
                    wcf_merge.setWrap(true);

                    WritableCellFormat wcf_table = new WritableCellFormat(
                            NormalFont);
                    wcf_table.setBorder(Border.ALL, BorderLineStyle.THIN);
                    wcf_table.setVerticalAlignment(VerticalAlignment.CENTRE);
                    wcf_table.setAlignment(Alignment.CENTRE);
                    wcf_table.setBackground(Colour.GRAY_25);// 背景颜色
                    wcf_table.setWrap(true);

                    if (cols.size() > 0) {
                        sheet.mergeCells(0, 0, cols.size() - 1, 0);// 合并单元格 "先列再行"
                    } else {
                        sheet.mergeCells(0, 0, 10, 0);// 合并单元格 "先列再行"
                    }
                    sheet.addCell(new Label(0, 0, titles.get(w), wcf_titlecenter));
                    for (int i = 0; i < cols.size(); i++) {
                        sheet.addCell(new Label(i, 1, cols.get(i), wcf_title));// 第1列标题
                    }
                    String value = null;
                    for (int row = 0, len = dt.Data.size(); row < len; row++) {
                        int _row = row + 2;// 加上标题两行
                        Map<String, Object> map = dt.Data.get(row);
                        sheet.setRowView(_row, 600);
                        for (int col = 0; col < cols.size(); col++) {
                            String key = cols.get(col);
                            if (handler == null
                                    || !handler.handerImage(key, map, sheet, col,
                                    _row)) {
                                if (map.get(key) == null) {
                                    value = "";
                                } else {
                                    value = map.get(key).toString();
                                }
                                sheet.addCell(new Label(col, _row, value,
                                        wcf_center));
                            }
                        }
                    }
                    if (handler != null) {
                        handler.sheetComplete(sheet);
                    }
                    w++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (workbook != null) {
                    try {
                        workbook.write();
                        ret = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        workbook.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return ret;
    }

    /**
     * 合并单元格
     *
     * @param sheet
     * @param type  :row,col
     */
    public static void mergeCells(WritableSheet sheet, String type) throws Exception {
        mergeCells(sheet, type, 0, 0);
    }

    /**
     * 合并单元格
     *
     * @param sheet
     * @param type  :row,col
     * @param index
     * @param start
     */
    public static void mergeCells(WritableSheet sheet, String type, int index, int start) throws Exception {
        String lastStr = null;
        int lastIndex = -1;
        if (type.equals("row")) {// 合并第index行，相同的字符
            if (index < sheet.getRows()) {
                for (int i = start, len = sheet.getColumns(); i < len; i++) {
                    String text = sheet.getCell(i, index).getContents().trim();
                    if (!text.equals(lastStr)) {
                        if (lastIndex != -1) {
                            // for (int d = 1; d < i - lastIndex; d++) {
                            // sheet.getCell(d + lastIndex, index);
                            // }
                            // sheet.getCell(i, index);//当前格子
                            sheet.mergeCells(lastIndex, index, i - 1, index);// 合并单元格"先列再行"
                        }
                        lastStr = text;
                        lastIndex = i;
                    }
                }
                if (lastIndex != -1) {
                    int j = sheet.getColumns();
                    // for (int d = 1; d < j - lastIndex; d++) {
                    // sheet.getCell(d + lastIndex, index);
                    // }
                    // sheet.getCell(j, index);//当前格子
                    sheet.mergeCells(lastIndex, index, j - 1, index);// 合并单元格"先列再行"
                }
            }
        } else {
            if (index < sheet.getColumns()) {
                for (int i = start, len = sheet.getRows(); i < len; i++) {
                    String text = sheet.getCell(index, i).getContents().trim();
                    if (!text.equals(lastStr)) {
                        if (lastIndex != -1) {
                            // for (int d = 1; d < i - lastIndex; d++) {
                            // // sheet.getCell(d + lastIndex, index);
                            // sheet.addCell(new Label(d + lastIndex, index,
                            // null));
                            // }
                            // sheet.getCell(i, index);//当前格子
                            sheet.mergeCells(index, lastIndex, index, i - 1);// 合并单元格"先列再行"
                        }
                        lastStr = text;
                        lastIndex = i;
                    }
                }
                if (lastIndex != -1) {
                    int i = sheet.getRows();
                    // for (int d = 1; d < i - lastIndex; d++) {
                    // // sheet.getCell(d + lastIndex, index);
                    // sheet.addCell(new Label(d + lastIndex, index, null));
                    // }
                    // sheet.getCell(i, index);// 当前格子
                    sheet.mergeCells(index, lastIndex, index, i - 1);// 合并单元格"先列再行"
                }
            }
        }
    }

    /**
     * 导入excel
     *
     * @param file
     * @param handler
     * @return
     */
    public static boolean inportXls(File file, InportXlsHandler handler) {
        boolean isOk = false;
        Workbook book = null;
        try {
            book = Workbook.getWorkbook(file);// 读取xls文档
            if (book != null && book.getSheets().length > 0) {
                Sheet sheet = book.getSheet(0);// 第1个
                int rows = sheet.getRows();
                int columns = sheet.getColumns();
                if (rows > 2) {// 行数大于两行
                    HashMap<Integer, String> keys = new HashMap<Integer, String>();
                    String key = null, value = null;
                    for (int j = 0; j < columns; j++) {
                        key = sheet.getCell(j, 1).getContents().trim();
                        keys.put(j, key);
                    }
                    HashMap<String, String> data = null;
                    for (int r = 2; r < rows; r++) {// 第3行开始
                        data = new HashMap<String, String>();
                        for (int j = 0; j < columns; j++) {
                            value = sheet.getCell(j, r).getContents().trim();
                            data.put(keys.get(j), value);
                        }
                        handler.handerRow(r, data);
                    }
                }
                isOk = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (book != null) {
                book.close();
            }
        }
        return isOk;
    }

    public interface InportXlsHandler {
        public void handerRow(int row, HashMap<String, String> data);
    }

    public interface WriteXlsHandler {
        /**
         * 用于处理图片,或者格式化
         */
        public boolean handerImage(String key, Map<String, Object> map, WritableSheet sheet, int col, int row);

        public void sheetComplete(WritableSheet sheet);
        // //处理图片
        // GRJC干扰监测 obj = new GRJC干扰监测();
        // obj.setid((String) map.get("ID"));
        // obj.set任务id((String) map.get("任务ID"));
        // File _file = new File(obj.getUploadFileDirPath(request)
        // + "/" + value);
        // if (_file.exists()) {
        // WritableImage image = new WritableImage(col, _row,
        // 1, 1, _file);
        // sheet.addImage(image);
        // }
    }

    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
