package data.lab.ongdb.security.util;

import org.junit.Test;

/*
 *
 * Data Lab - graph database organization.
 *
 */

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.util
 * @Description: TODO
 * @date 2022/2/22 17:59
 */
public class StringUtilTest {

    @Test
    public void btnSquareBktOnce() {
        System.out.println(StringUtil.btnSquareBktOnce("roles [reader_proc] overridden by READ"));
        System.out.println(StringUtil.btnSquareBktOnce(null));
        System.out.println(StringUtil.btnSquareBktOnce(""));
    }
}


