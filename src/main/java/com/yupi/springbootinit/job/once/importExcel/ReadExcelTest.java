package com.yupi.springbootinit.job.once.importExcel;

import com.alibaba.excel.EasyExcel;

import java.util.List;


/**
 * @author xianyu
 *
 * 读取表格
 */
public class ReadExcelTest {
    public static void main(String[] args) {
        String fileName = "D:\\java\\project_java\\yupi_dog\\src\\main\\resources\\demo.xlsx";
        readByListener(fileName);
//        synchronousRead(fileName);
    }

    /**
     * 监听器读
     * @param fileName
     */
    public static void readByListener(String fileName) {
        //这里 需要指定用哪个class去读，然后读取第一个sheet 文件流会自动关闭
        EasyExcel.read(fileName, ExcelData.class, new ExcelDataListener()).sheet().doRead();
    }

    /**
     * 同步读【它会一次性读取所有数据。数据量大时，有等待时长，还可能内存溢出】
     * 同步读不建议有返回值，因为如果返回的数据量过大，很容易内存溢出（内存占满）
     *
     * ExcelData对象相当于表格中的一行数据【知道表头。指定class】
     * Map<String, Object>相当于表格中的一行数据【不知道表头。不指定class】
     */
    public static void synchronousRead(String fileName) {
        //这里 需要指定用哪个class去读，然后读取第一个sheet 同步读会自动finish
        List<ExcelData> list = EasyExcel.read(fileName).head(ExcelData.class).sheet().doReadSync();
        for (ExcelData data : list) {
            System.out.println(data);
        }
        // 这里 可以不指定class【不知道表头】，返回一个list，然后读取第一个sheet 同步读会自动finish
        //        List<Map<String, Object>> listMap = EasyExcel.read(fileName).sheet().doReadSync();
        //        for (Map<String, Object> data : listMap) {
        //            System.out.println(data);
        //        }
    }
}
