package com.yupi.springbootinit.utils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;


/**
 *算法工具类测试
 */
public class AlgorithmUtilsTest {
    @Test
    void test() {
        String str1 = "鱼皮";
        String str2 = "鱼皮是鱼";
        String str3 = "鱼皮";
        String str4 = "鱼";
        String str5 = "咸鱼大王";
        //1
        int distance1 = AlgorithmUtils.minDistance(str1, str2);
        System.out.println(distance1);
        //0
        int distance2 = AlgorithmUtils.minDistance(str1, str3);
        System.out.println(distance2);
        //1
        int distance3 = AlgorithmUtils.minDistance(str1, str4);
        System.out.println(distance3);
        //2
        int distance4 = AlgorithmUtils.minDistance(str1, str5);
        System.out.println(distance4);
    }

    //数组Arrays的asList方法将多个元素变成一个list列表
    @Test
    void testCompareTags() {
        List<String> tagList1 = Arrays.asList("java","大一","男");
        List<String> tagList2 = Arrays.asList("java","大二","男");
        List<String> tagList3 = Arrays.asList("c++","大二","女");
        //0
        int distance2 = AlgorithmUtils.minDistance(tagList1, tagList2);
        System.out.println(distance2);
        //3
        int distance3 = AlgorithmUtils.minDistance(tagList1, tagList3);
        System.out.println(distance3);
    }

}
