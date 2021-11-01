package com.hzf.core.toolkit;

import com.hzf.core.common.AjaxResult;
import com.hzf.core.common.DataTable;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ExcelReadUtil {
    public static final String OFFICE_EXCEL_2003_POSTFIX = "xls";
    public static final String OFFICE_EXCEL_2010_POSTFIX = "xlsx";

    public static void main(String[] args) {
        try {
            DataTable dataTable = ExcelReadUtil.readExcel("C:\\Users\\tangbin\\Desktop\\test.xlsm", 1);
            System.out.println(TypeConvert.ToJson(dataTable.Data));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static abstract class ReadExcelCallback {
        public List<CellRangeAddress> mergedRegions;
        public int maxColumn = 0;

        public CellRangeAddress getCellRangeAddress(Cell cell) {
            CellRangeAddress cellRangeAddress = null;
            if (cell != null) {
                for (CellRangeAddress ca : mergedRegions) {// 获得合并单元格的起始行, 结束行, 起始列, 结束列
                    int firstCol = ca.getFirstColumn();
                    int lastCol = ca.getLastColumn();
                    int firstRow = ca.getFirstRow();
                    int lastRow = ca.getLastRow();
                    if (cell.getColumnIndex() <= lastCol && cell.getColumnIndex() >= firstCol) {
                        if (cell.getRowIndex() <= lastRow && cell.getRowIndex() >= firstRow) {
                            cellRangeAddress = ca;
                            break;
                        }
                    }
                }
            }
            return cellRangeAddress;
        }

        public void validateRow() {
        }

        public abstract void readCell(int sheet, String sheetname, int row, int col, String value, Cell Cell, boolean isOver);


        public AjaxResult getAjaxResult() {
            return null;
        }
    }

    public static DataTable readExcel(String filepath, int titlerow) throws Exception {
        String 行号 = "行号";
        DataTable table = new DataTable();
        table.DataColumns.add(table.CreateColumnMap(行号));
        ExcelReadUtil.ReadExcelCallback callback = new ExcelReadUtil.ReadExcelCallback() {
            int _row = -1;
            Map<String, Object> m = new HashMap<>();
            Map<Integer, String> mColumn = new HashMap<>();

            @Override
            public void validateRow() {
                if (m != null) {
                    boolean isAllNull = true;
                    for (String key : m.keySet()) {
                        if (!行号.equals(key)) {
                            if (StrUtil.isNotEmpty(TypeConvert.ToString(m.get(key)).trim())) {
                                isAllNull = false;
                                break;
                            }
                        }
                    }
                    if (isAllNull) {
                        table.Data.remove(m);//清除空数据
                    }
                }
            }

            @Override
            public void readCell(int sheet, String sheetname, int row, int col, String value, Cell Cell, boolean isOver) {
                if (StrUtil.isNotEmpty(value)) {
                    value = value.trim();
                }
                if (row < titlerow) {
                    return;
                } else if (row == titlerow) {
                    table.DataColumns.add(table.CreateColumnMap(value));
                    mColumn.put(col, value);
                } else {
                    if (this._row != row) {
                        this._row = row;
                        validateRow();
                        m = new HashMap<>();
                        m.put(行号, (row + 1));
                        table.Data.add(m);
                    }
                    if (StrUtil.isNotEmpty(mColumn.get(col))) {   //必须要有标题
                        m.put(mColumn.get(col), value);
                    }
                }
            }
        };
        ExcelReadUtil.readExcel(filepath, callback);
        callback.validateRow();
        return table;
    }

    public static void readExcel(String path, ReadExcelCallback callback) throws IOException {
        if (!StrUtil.isEmpty(path)) {
            String postfix = getPostfix(path);
            if (!StrUtil.isEmpty(postfix)) {
                FileInputStream input = new FileInputStream(path);
                readExcel(input, postfix, callback);
                input.close();
            }
        }
    }

    public static void readExcel(InputStream input, String postfix, ReadExcelCallback callback) throws IOException {
        if (!StrUtil.isEmpty(postfix)) {
            LogUtil.i("Processing..." + input);
            if (OFFICE_EXCEL_2003_POSTFIX.equals(postfix)) {
                readXlsxAndXls(callback, input, false);
            } else {
                readXlsxAndXls(callback, input, true);
            }
        } else {
            LogUtil.i(input + " : Not the Excel file!");
        }
    }

    private static void readXlsxAndXls(ReadExcelCallback callback, InputStream is, boolean isXlsx) throws IOException {
        if (isXlsx) {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook(is);
            readXlsx(xssfWorkbook, callback);
        } else {
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook(is);
            readXls(hssfWorkbook, callback);
        }
        is.close();
    }

    //Excel 2010
    private static void readXlsx(XSSFWorkbook xssfWorkbook, ReadExcelCallback callback) throws IOException {
        String value = null;
        for (int numSheet = 0, len = xssfWorkbook.getNumberOfSheets(); numSheet < len; numSheet++) {
            XSSFSheet xssfSheet = xssfWorkbook.getSheetAt(numSheet);
            if (xssfSheet != null) {
                callback.mergedRegions = xssfSheet.getMergedRegions();
                for (int rowNum = 0, rows = xssfSheet.getLastRowNum(); rowNum <= rows; rowNum++) {
                    XSSFRow xssfRow = xssfSheet.getRow(rowNum);
                    if (xssfRow != null) {
                        if (callback.maxColumn < xssfRow.getLastCellNum()) {
                            callback.maxColumn = xssfRow.getLastCellNum();
                        }
                        for (int colNum = 0, cols = xssfRow.getLastCellNum(); colNum <= cols; colNum++) {
                            XSSFCell cell = xssfRow.getCell(colNum);
                            if (cell != null) {
                                CellRangeAddress cellRangeAddress = callback.getCellRangeAddress(cell);
                                if (cellRangeAddress != null) {
                                    int row = cellRangeAddress.getFirstRow();
                                    int col = cellRangeAddress.getFirstColumn();
                                    value = getValue(xssfSheet.getRow(row).getCell(col));
                                } else {
                                    value = getValue(cell);
                                }
                            } else {
                                value = null;
                            }
                            callback.readCell(numSheet, xssfSheet.getSheetName(), rowNum, colNum, value, cell, colNum == cols);
                        }
                    }
                }
            }
        }
        xssfWorkbook.close();
    }

    //2003-2007
    private static void readXls(HSSFWorkbook hssfWorkbook, ReadExcelCallback callback) throws IOException {
        String value = null;
        for (int numSheet = 0; numSheet < hssfWorkbook.getNumberOfSheets(); numSheet++) {
            HSSFSheet hssfSheet = hssfWorkbook.getSheetAt(numSheet);
            if (hssfSheet != null) {
                callback.mergedRegions = hssfSheet.getMergedRegions();
                for (int rowNum = 0, rows = hssfSheet.getLastRowNum(); rowNum <= rows; rowNum++) {
                    HSSFRow hssfRow = hssfSheet.getRow(rowNum);
                    if (hssfRow != null) {
                        if (callback.maxColumn < hssfRow.getLastCellNum()) {
                            callback.maxColumn = hssfRow.getLastCellNum();
                        }
                        for (int colNum = 0, cols = hssfRow.getLastCellNum(); colNum <= cols; colNum++) {
                            HSSFCell cell = hssfRow.getCell(colNum);
                            if (cell != null) {
                                CellRangeAddress cellRangeAddress = callback.getCellRangeAddress(cell);
                                if (cellRangeAddress != null) {
                                    int row = cellRangeAddress.getFirstRow();
                                    int col = cellRangeAddress.getFirstColumn();
                                    value = getValue(hssfSheet.getRow(row).getCell(col));
                                } else {
                                    value = getValue(cell);
                                }
                            } else {
                                value = null;
                            }
                            callback.readCell(numSheet, hssfSheet.getSheetName(), rowNum, colNum, value, cell, colNum == cols);
                        }
                    }
                }
            }
        }
        hssfWorkbook.close();
    }

    private static String getValue(XSSFCell xssfRow) {
        if (xssfRow == null)
            return "";
        if (xssfRow.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(xssfRow.getBooleanCellValue());
        } else if (xssfRow.getCellType() == CellType.NUMERIC) {
            return String.valueOf(xssfRow.getNumericCellValue());
        } else {
            try {
                return String.valueOf(xssfRow.getStringCellValue());
            } catch (Exception e1) {
                try {
                    return String.valueOf(xssfRow.getNumericCellValue());
                } catch (Exception e2) {
                    return String.valueOf(xssfRow.getBooleanCellValue());
                }
            }
        }
    }

    public static String getValue(HSSFCell cell) {
        DecimalFormat df = new DecimalFormat("#");
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case NUMERIC:
                if (HSSFDateUtil.isCellDateFormatted(cell)) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    return sdf.format(HSSFDateUtil.getJavaDate(cell.getNumericCellValue())).toString();
                }
                return df.format(cell.getNumericCellValue());
            case STRING:
                return cell.getStringCellValue();
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            case BOOLEAN:
                return cell.getBooleanCellValue() + "";
            case ERROR:
                return cell.getErrorCellValue() + "";
        }
        return "";
    }

    public static String getValue(Cell cell) {
        String value = null;
        if (cell == null) {
            if (cell.getClass() == HSSFCell.class) {
                return getValue((HSSFCell) cell);
            } else if (cell.getClass() == XSSFCell.class) {
                return getValue((XSSFCell) cell);
            }
        }
        return value;
    }

    public static String getPostfix(String path) {
        String ret = null;
        if (!StrUtil.isEmpty(path)) {
            int index = path.lastIndexOf(".");
            int len = path.length();
            if (index > -1 && index < len - 1) {
                ret = path.substring(index + 1, len);
            }
        }
        return ret;
    }
}
