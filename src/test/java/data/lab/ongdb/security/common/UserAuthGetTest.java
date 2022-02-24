package data.lab.ongdb.security.common;

import org.junit.Test;

/*
 *
 * Data Lab - graph database organization.
 *
 */

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.common
 * @Description: TODO
 * @date 2022/2/24 17:25
 */
public class UserAuthGetTest {

    @Test
    public void labelOperator() {
        UserAuthGet.labelOperator(UserAuthGet.auth("publisher-1","publisher_auth.json"),"Person");
    }
}
