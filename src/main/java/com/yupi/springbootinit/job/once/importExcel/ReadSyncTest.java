package com.yupi.springbootinit.job.once.importExcel;
import com.alibaba.excel.EasyExcel;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xianyu
 */
public class ReadSyncTest {
    public static void main(String[] args) {
        String fileName = "D:\\java\\project_java\\yupi_dog\\src\\main\\resources\\demo.xlsx";
        List<ExcelData> userlist = EasyExcel.read(fileName).head(ExcelData.class).sheet().doReadSync();
        System.out.println("用户的总数量=" + userlist.size());
        //假设重复的昵称有很多。debug查看哪些昵称重复
        Map<String, List<ExcelData>> listMap =
                userlist.stream()
                        //获取的外界数据需要处理、清洗。过滤掉那些昵称为空的数据
                        .filter(ExcelData -> StringUtils.isNotEmpty(ExcelData.getUserName()))
                        .collect(Collectors.groupingBy(ExcelData::getUserName));
        for (Map.Entry<String, List<ExcelData>> stringListEntry : listMap.entrySet()) {
            //>1,昵称有重复
            if (stringListEntry.getValue().size() > 1) {
                System.out.println("重复的用户昵称userName=" + stringListEntry.getKey());
            }
        }
        System.out.println("不重复的昵称数量=" + listMap.keySet().size());
//        System.out.println("不重复的昵称数量=" + listMap.size());
    }
}
