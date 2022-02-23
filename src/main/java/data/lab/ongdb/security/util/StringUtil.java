package data.lab.ongdb.security.util;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.util
 * @Description: TODO
 * @date 2022/2/22 17:54
 */
public class StringUtil {

    private static final Pattern PATTERN = Pattern.compile("\\[(.*?)]");

    /**
     * 从字符串中提取位于方括号中间的数据，提取一次即可
     *
     * @param string:字符串
     * @return
     * @Description: TODO
     */
    public static String btnSquareBktOnce(String string) {
        if (Objects.nonNull(string)) {
            Matcher matcher = PATTERN.matcher(string);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return "";
    }
}

