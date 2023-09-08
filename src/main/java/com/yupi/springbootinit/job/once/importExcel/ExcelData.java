package com.yupi.springbootinit.job.once.importExcel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 待读取的用户表对象
 *
 * @author xianyu
 */
@Data
public class ExcelData {

    /**读对象的方式：确定表头，建立对象，和表头形成映射关系
     * ExcelProperty("标题")要和表格的表头字段保持一致，才可以读取出来
     */
    @ExcelProperty("标题")
    private String title;

    @ExcelProperty("日期时间")
    private Date dateTime;

    @ExcelProperty("数字标题")
    private Double doubleTitle;

    @ExcelProperty("昵称")
    private String userName;

    @ExcelProperty("星球编号")
    private String planetCode;
}
