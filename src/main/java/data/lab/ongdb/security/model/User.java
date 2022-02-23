package data.lab.ongdb.security.model;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import java.util.Objects;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.model
 * @Description: TODO
 * @date 2022/2/22 11:44
 */
public class User {

    /**
     * 用户名
     * **/
    public String username;

    /**
     * 用户角色
     * **/
    public String currentRole;

    public User(String username, String currentRole) {
        this.username = username;
        this.currentRole = currentRole;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public void setCurrentRole(String currentRole) {
        this.currentRole = currentRole;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(username, user.username) &&
                Objects.equals(currentRole, user.currentRole);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, currentRole);
    }

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", currentRole='" + currentRole + '\'' +
                '}';
    }
}

