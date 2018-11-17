package com.wpc.system.factory;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组合字符串生产者
 *
 * @author 王鹏程
 * @date 2017-04-27 16:42
 */
public class MutiStrFactory {

    /**
     * 每个条目之间的分隔符
     */
    public static final String ITEM_SPLIT = ";";

    /**
     * 属性之间的分隔符
     */
    public static final String ATTR_SPLIT = ":";

    /**
     * 拼接字符串的id
     */
    public static final String MUTI_STR_ID = "ID";

    /**
     * 拼接字符串的CODE
     */
    public static final String MUTI_STR_CODE = "CODE";

    /**
     * 拼接字符串的NAME
     */
    public static final String MUTI_STR_NAME = "NAME";

    /**
     * 拼接字符串的NUM
     */
    public static final String MUTI_STR_NUM = "NUM";

    /**
     * 解析一个组合字符串(例如:  "1:启用;2:禁用;3:冻结"  这样的字符串)
     *
     * @author 王鹏程
     * @Date 2017/4/27 16:44
     */
    public static List<Map<String, String>> parseKeyValue(String mutiString) {
        if (StringUtils.isEmpty(mutiString)) {
            return new ArrayList<>();
        } else {
            ArrayList<Map<String, String>> results = new ArrayList<>();
            String[] items = StringUtils.split(StringUtils.removeStart(mutiString, ITEM_SPLIT), ITEM_SPLIT);
            for (String item : items) {
                String[] attrs = item.split(ATTR_SPLIT);
                HashMap<String, String> itemMap = new HashMap<>();
                itemMap.put(MUTI_STR_CODE, attrs[0]);
                itemMap.put(MUTI_STR_NAME, attrs[1]);
                itemMap.put(MUTI_STR_NUM, attrs[2]);
                results.add(itemMap);
            }
            return results;
        }
    }
}
