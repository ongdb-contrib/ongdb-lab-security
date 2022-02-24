package data.lab.ongdb.security.common;
/*
 *
 * Data Lab - graph database organization.
 *
 */

/**
 * 对数据编辑类权限做枚举
 *
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.auth
 * @Description: TODO
 * @date 2022/2/23 13:06
 */
public enum Operator {

    /**
     * 1、【禁止读取】
     * **/
    READER_FORBID("reader_forbid",1,"【禁止读取】"),

    /**
     * 1、【可以读取】
     * **/
    READER("reader",2,"【可以读取】"),

    /**
     * 1、对已有数据的编辑修改权限
     **/
    EDITOR("editor", 3,"【对已有数据的编辑修改权限】"),

    /**
     * 1、对已有数据的编辑修改权限
     * 2、拥有创建新数据权限
     **/
    PUBLISHER("publisher", 4,"【对已有数据的编辑修改权限】【拥有创建新数据权限】"),

    /**
     * 最高级别的操作权限
     * 1、对已有数据的编辑修改权限
     * 2、拥有创建新数据权限
     * 3、拥有删除数据权限【仅允许删除用户自己创建的数据】
     **/
    DELETER_RESTRICT("deleter_restrict", 5,"【对已有数据的编辑修改权限】【拥有创建新数据权限】【仅允许删除用户自己创建的数据】"),

    /**
     * 最高级别的操作权限
     * 1、对已有数据的编辑修改权限
     * 2、拥有创建新数据权限
     * 3、拥有删除数据权限【允许删除用户自己创建的数据】
     * 4、拥有删除数据权限【可以删除其它用户创建的数据】
     **/
    DELETER("deleter", 6,"对已有数据的编辑修改权限】【拥有创建新数据权限】【可以删除其它用户创建的数据】");

    /**
     * 操作类别
     **/
    public String operate;

    /**
     * 操作级别：数字越大权限越大
     **/
    public Number level;

    public String description;

    Operator(String operate, int level, String description) {
        this.operate = operate;
        this.level = level;
        this.description = description;
    }

    public String getOperate() {
        return operate;
    }

    public void setOperate(String operate) {
        this.operate = operate;
    }

    public Number getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}


