package data.lab.ongdb.security.common;
/*
 *
 * Data Lab - graph database organization.
 *
 */

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.common
 * @Description: TODO
 * @date 2022/3/2 9:19
 */
public enum Constraint {

    /*
     * 存在限制
     * */
    EXISTS("exists");

    public String value;

    Constraint(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

