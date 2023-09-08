package com.yupi.springbootinit.job.once.importExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

//重要的点 ExcelDataListener 不能被spring管理

/**
 * 简单的读的监听器
 * @author xianyu
 */
@Slf4j
public class ExcelDataListener implements ReadListener<ExcelData> {
    /**
     * 每一条数据解析后,会来调用
     *
     * @param data
     * @param context
     */
    @Override
    public void invoke(ExcelData data, AnalysisContext context) {
        System.out.println(data);
    }

    /**
     * 所有数据解析完成了后，会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        System.out.println("已解析完成");
    }

    /**
     * 加上存储数据库
     */
    private void saveData() {
    }
}