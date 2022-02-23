package data.lab.ongdb.security.role;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security
 * @Description: TODO
 * @date 2022/2/21 16:38
 */
public class Reader {

    /**
     * 用户名
     * **/
    public String username;

    /**
     * 用户角色
     * **/
    public String currentRole;

    /**
     * 拥有权限的可执行查询列表 query_id query
     * **/
    public List<Map<String,String>> queries;

    public Reader() {
        init();
    }

    public Reader(String username, String currentRole, List<Map<String, String>> queries) {
        this.username = username;
        this.currentRole = currentRole;
        this.queries = queries;
        init();
    }

    private void init() {
        if (Objects.nonNull(this.queries)) {
            this.queries = this.queries.parallelStream()
                    .filter(v -> Objects.nonNull(v) && !"".equals(v.toString()))
                    .collect(Collectors.toList());
        }
        if (Objects.isNull(this.queries)) {
            this.queries = new ArrayList<>();
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

    public List<Map<String, String>> getQueries() {
        return queries;
    }

    public void setQueries(List<Map<String, String>> queries) {
        this.queries = queries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Reader reader = (Reader) o;
        return Objects.equals(username, reader.username) &&
                Objects.equals(currentRole, reader.currentRole) &&
                Objects.equals(queries, reader.queries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, currentRole, queries);
    }

    @Override
    public String toString() {
        return "Reader{" +
                "username='" + username + '\'' +
                ", currentRole='" + currentRole + '\'' +
                ", queries=" + queries +
                '}';
    }

    public void merge(Reader reader) {
        this.queries.addAll(reader.queries);
        this.queries = this.queries.parallelStream()
                .distinct()
                .collect(Collectors.toList());
    }
}





