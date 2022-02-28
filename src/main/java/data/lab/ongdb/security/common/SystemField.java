package data.lab.ongdb.security.common;
/*
 *
 * Data Lab - graph database organization.
 *
 */

/**
 * 保存数据的创建人信息
 *
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.common
 * @Description: TODO
 * @date 2022/2/23 14:20
 */
public enum SystemField {

    /*
    * To do list
    * */

    /**
     * 数组格式存储用户名称信息【表示是否对节点或关系拥有权限】
     * 存储数据的创建人
     * __system_users:[{label:'Person',user:'reader-1'},{label:'Person2',user:'reader-2'}]
     *
     **/
    __SYSTEM_USERS("__system_users"),

    /**
     * 数组格式存储用户名称信息【表示是否对属性拥有权限】
     * 存储数据的创建人
     * __system_field_users:[{field:'name',user:'reader-1'},{label:'code',user:'reader-2'}]
     **/
    __SYSTEM_FIELD_USERS("__system_field_users");

    private String field;

    SystemField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }
}


