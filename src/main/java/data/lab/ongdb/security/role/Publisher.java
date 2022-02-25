package data.lab.ongdb.security.role;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import data.lab.ongdb.security.common.ParaWrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security
 * @Description: TODO
 * @date 2022/2/21 16:38
 */
public class Publisher {

    /**
     * 用户名
     **/
    public String username;

    /**
     * 用户角色
     **/
    public String currentRole;

    /**
     * 拥有操作权限的标签 label properties
     **/
    public List<Map<String, Object>> nodeLabels;

    /**
     * 拥有权限的关系 start_label type end_label properties
     **/
    public List<Map<String, Object>> relTypes;

    public Publisher() {
        init();
    }

    public Publisher(String username, String currentRole, List<Map<String, Object>> nodeLabels, List<Map<String, Object>> relTypes) {
        this.username = username;
        this.currentRole = currentRole;
        this.nodeLabels = nodeLabels;
        this.relTypes = relTypes;
        init();
    }

    private void init() {
        if (Objects.nonNull(this.nodeLabels)) {
            this.nodeLabels = this.nodeLabels.parallelStream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        if (Objects.nonNull(this.relTypes)) {
            this.relTypes = this.relTypes.parallelStream()
                    .filter(v -> Objects.nonNull(v) && !"".equals(v.toString()))
                    .collect(Collectors.toList());
        }
        if (Objects.isNull(this.nodeLabels)) {
            this.nodeLabels = new ArrayList<>();
        }
        if (Objects.isNull(this.relTypes)) {
            this.relTypes = new ArrayList<>();
        }
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

    public List<Map<String, Object>> getNodeLabels() {
        return nodeLabels;
    }

    public void setNodeLabels(List<Map<String, Object>> nodeLabels) {
        this.nodeLabels = nodeLabels;
    }

    public List<Map<String, Object>> getRelTypes() {
        return relTypes;
    }

    public void setRelTypes(List<Map<String, Object>> relTypes) {
        this.relTypes = relTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Publisher publisher = (Publisher) o;
        return Objects.equals(username, publisher.username) &&
                Objects.equals(currentRole, publisher.currentRole) &&
                Objects.equals(nodeLabels, publisher.nodeLabels) &&
                Objects.equals(relTypes, publisher.relTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, currentRole, nodeLabels, relTypes);
    }

    @Override
    public String toString() {
        return "Publisher{" +
                "username='" + username + '\'' +
                ", currentRole='" + currentRole + '\'' +
                ", nodeLabels=" + nodeLabels +
                ", relTypes=" + relTypes +
                '}';
    }

    /**
     * 合并细粒度权限对象
     *
     * @param publisher：被合并对象
     * @return
     * @Description: TODO
     */
    public void merge(Publisher publisher) {
        // 使用label字段排重
        this.nodeLabels.addAll(publisher.nodeLabels);
        this.nodeLabels = this.nodeLabels.stream()
                .filter(
                        ParaWrap.distinctById(v -> String.valueOf(v.get("label")))
                )
                .collect(Collectors.toList());

        // 使用start_label type end_label排重
        this.relTypes.addAll(publisher.relTypes);
        this.relTypes = this.relTypes.stream()
                .filter(
                        ParaWrap.distinctById(v ->
                        String.valueOf(v.get("start_label"))+
                        v.get("type")+
                        v.get("end_label"))
                )
                .collect(Collectors.toList());
    }

}




