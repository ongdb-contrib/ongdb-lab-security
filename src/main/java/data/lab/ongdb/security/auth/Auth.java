package data.lab.ongdb.security.auth;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import data.lab.ongdb.security.common.Operator;
import data.lab.ongdb.security.common.Types;
import data.lab.ongdb.security.inter.AuthInter;
import data.lab.ongdb.security.model.User;
import data.lab.ongdb.security.result.ListResult;
import data.lab.ongdb.security.result.Output;
import data.lab.ongdb.security.role.Publisher;
import data.lab.ongdb.security.role.Reader;
import data.lab.ongdb.security.util.FileUtil;
import data.lab.ongdb.security.util.StringUtil;
import org.neo4j.cypher.ParameterNotFoundException;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.procedure.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.DBMS;
import static org.neo4j.procedure.Mode.READ;

/**
 * 自定义权限配置
 * 【生成配置文件位置：auth/auth.json】
 *
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security
 * @Description: TODO
 * @date 2022/2/21 16:37
 */
public class Auth implements AuthInter {

    private static final String PUBLISHER_AUTH_JSON = "publisher_auth.json";

    private static final String READER_AUTH_JSON = "reader_auth.json";

    @Context
    public SecurityContext securityContext;

    /**
     * 配置Publisher权限【合并权限列表】【admin】
     *
     * @param username   :用户名
     * @param nodeLabels :可操作的标签列表【可为空】
     * @param relTypes   :可操作的关系类型列表【可为空】 start_label type end_label
     * @return 返回本次配置信息
     * @Description: TODO
     */
    @Override
    @Admin
    @Procedure(name = "olab.security.setPublisher", mode = DBMS)
    @Description("CALL olab.security.setPublisher('publisher',['Person'],[{start_label:'Person',type:'ACTED_IN',end_label:'Movie'}]) YIELD username,currentRole,nodeLabels,relTypes - Configure Publisher.")
    public Stream<Publisher> setPublisher(@Name("username") String username,
                                          @Name("nodeLabels") List<Map<String, Object>> nodeLabels,
                                          @Name("relTypes") List<Map<String, Object>> relTypes) {
        check(nodeLabels, relTypes);
        if (Objects.nonNull(username)) {
            Publisher rawPublisher = FileUtil.readPublisherAuth(PUBLISHER_AUTH_JSON, username, FileUtil.ENCODING);
            Publisher newPublisher = new Publisher(username, "admin", nodeLabels, relTypes);
            if (rawPublisher != null) {
                newPublisher.merge(rawPublisher);
            }
            FileUtil.writePublisherAuth(PUBLISHER_AUTH_JSON, newPublisher);
            return Stream.of(newPublisher);
        }

        return Stream.of(new Publisher());
    }

    private void check(List<Map<String, Object>> nodeLabels, List<Map<String, Object>> relTypes) throws ParameterNotFoundException {
        if ((Objects.isNull(nodeLabels) || nodeLabels.isEmpty()) && (Objects.isNull(relTypes) || relTypes.isEmpty())) {
            throw new ParameterNotFoundException("neither nodeLabels nor relTypes [not exist] exist!");
        }
        nodeLabelsCheck(nodeLabels);
        relTypesCheck(relTypes);
    }

    private void relTypesCheck(List<Map<String, Object>> relTypes) throws ParameterNotFoundException {
        if (Objects.nonNull(relTypes)) {
            for (int i = 0; i < relTypes.size(); i++) {
                Map<String, Object> v = relTypes.get(i);

                for (String field : new String[]{"start_label", "type", "end_label"}) {
                    if (!v.containsKey(field)) {
                        throw new ParameterNotFoundException("relTypes:" + field + " not exists!" + "index:" + i);
                    } else if (!(v.get(field) instanceof String)) {
                        throw new ParameterNotFoundException("relTypes:" + field + " [Set String] type error!" + "index:" + i);
                    }
                }

                if (!v.containsKey("invalid_values")) {
                    throw new ParameterNotFoundException("relTypes:invalid_values not exists!" + "index:" + i);
                } else if (!(v.get("invalid_values") instanceof List)) {
                    throw new ParameterNotFoundException("relTypes:invalid_values [Set List] type error!" + "index:" + i);
                }

                if (!v.containsKey("properties")) {
                    throw new ParameterNotFoundException("relTypes:properties not exists!" + "index:" + i);
                } else if (!(v.get("properties") instanceof List)) {
                    throw new ParameterNotFoundException("relTypes:properties [Set List] type error!" + "index:" + i);
                } else {
                    JSONArray properties = JSONArray.parseArray(JSON.toJSONString(v.get("properties")));
                    for (int j = 0; j < properties.size(); j++) {
                        JSONObject object = (JSONObject) properties.get(j);
                        for (String field : new String[]{"field", "operator", "check"}) {
                            if (!object.containsKey(field)) {
                                throw new ParameterNotFoundException("relTypes.properties:" + field + " not exists!" + "index:" + i + ",properties_index:" + j);
                            } else if (!(object.get(field) instanceof String)) {
                                throw new ParameterNotFoundException("relTypes.properties:" + field + " [Set String] type error!" + "index:" + i + ",properties_index:" + j);
                            } else if ("operator".equals(field)) {
                                operatorValueCheck(object.getString("operator"), "relTypes", i, j);
                            }
                        }
                        if (!object.containsKey("invalid_values")) {
                            throw new ParameterNotFoundException("nodeLabels.properties:" + "invalid_values" + " not exists!" + "index:" + i + ",properties_index:" + j);
                        } else if (!(object.get("invalid_values") instanceof List)) {
                            throw new ParameterNotFoundException("nodeLabels.properties:" + "invalid_values" + " [Set List] type error!" + "index:" + i + ",properties_index:" + j);
                        }
                    }
                }
                if (!v.containsKey("operator")) {
                    throw new ParameterNotFoundException("relTypes:operator not exists!" + "index:" + i);
                } else if (!(v.get("operator") instanceof String)) {
                    throw new ParameterNotFoundException("relTypes:operator [Set List] type error!" + "index:" + i);
                } else {
                    operatorValueCheck(String.valueOf(v.get("operator")), "relTypes", i, -1);
                }
            }
        }
    }

    private void nodeLabelsCheck(List<Map<String, Object>> nodeLabels) throws ParameterNotFoundException {
        if (Objects.nonNull(nodeLabels)) {
            for (int i = 0; i < nodeLabels.size(); i++) {
                Map<String, Object> v = nodeLabels.get(i);
                if (!v.containsKey("label")) {
                    throw new ParameterNotFoundException("nodeLabels:label not exists!" + "index:" + i);
                } else if (!(v.get("label") instanceof String)) {
                    throw new ParameterNotFoundException("nodeLabels:label [Set String] type error!" + "index:" + i);
                }

                if (!v.containsKey("invalid_values")) {
                    throw new ParameterNotFoundException("nodeLabels:invalid_values not exists!" + "index:" + i);
                } else if (!(v.get("invalid_values") instanceof List)) {
                    throw new ParameterNotFoundException("nodeLabels:invalid_values [Set List] type error!" + "index:" + i);
                }

                if (!v.containsKey("properties")) {
                    throw new ParameterNotFoundException("nodeLabels:properties not exists!" + "index:" + i);
                } else if (!(v.get("properties") instanceof List)) {
                    throw new ParameterNotFoundException("nodeLabels:properties [Set List] type error!" + "index:" + i);
                } else {
                    JSONArray properties = JSONArray.parseArray(JSON.toJSONString(v.get("properties")));
                    for (int j = 0; j < properties.size(); j++) {
                        JSONObject object = (JSONObject) properties.get(j);
                        for (String field : new String[]{"field", "operator", "check"}) {
                            if (!object.containsKey(field)) {
                                throw new ParameterNotFoundException("nodeLabels.properties:" + field + " not exists!" + "index:" + i + ",properties_index:" + j);
                            } else if (!(object.get(field) instanceof String)) {
                                throw new ParameterNotFoundException("nodeLabels.properties:" + field + " [Set String] type error!" + "index:" + i + ",properties_index:" + j);
                            } else if ("operator".equals(field)) {
                                operatorValueCheck(object.getString("operator"), "nodeLabels", i, j);
                            }
                        }
                        if (!object.containsKey("invalid_values")) {
                            throw new ParameterNotFoundException("nodeLabels.properties:" + "invalid_values" + " not exists!" + "index:" + i + ",properties_index:" + j);
                        } else if (!(object.get("invalid_values") instanceof List)) {
                            throw new ParameterNotFoundException("nodeLabels.properties:" + "invalid_values" + " [Set List] type error!" + "index:" + i + ",properties_index:" + j);
                        }
                    }
                }
                if (!v.containsKey("operator")) {
                    throw new ParameterNotFoundException("nodeLabels:operator not exists!" + "index:" + i);
                } else if (!(v.get("operator") instanceof String)) {
                    throw new ParameterNotFoundException("nodeLabels:operator [Set List] type error!" + "index:" + i);
                } else {
                    operatorValueCheck(String.valueOf(v.get("operator")), "nodeLabels", i, -1);
                }
            }
        }
    }

    /**
     * 操作权限类别检查
     **/
    private void operatorValueCheck(String operator, String para, int i, int j) {
        List<String> list = Arrays.asList(Operator.values())
                .parallelStream()
                .map(Operator::getOperate)
                .collect(Collectors.toList());
        if (!list.contains(operator.toLowerCase())) {
            if (j > -1) {
                throw new ParameterNotFoundException(para + ":operator value error!" + "index:" + i);
            } else {
                throw new ParameterNotFoundException(para + ":operator value error!" + "index:" + i + ",properties_index:" + j);
            }
        }
    }

    /**
     * 配置Reader权限【合并权限列表】【admin】【query_id不可重复】
     *
     * @param username :用户名
     * @param queries  :可执行查询列表 query_id query
     * @return
     * @Description: TODO
     */
    @Override
    @Admin
    @Procedure(name = "olab.security.setReader", mode = DBMS)
    @Description("CALL olab.security.setReader('reader',[{query_id:'query001',query:'MATCH (n) RETURN n.name AS name LIMIT 1'}]) YIELD username,currentRole,queries - Configure Reader.")
    public Stream<Reader> setReader(@Name("username") String username, @Name("queries") List<Map<String, String>> queries) {
        check(queries);
        if (Objects.nonNull(username)) {
            Reader rawReader = FileUtil.readReaderAuth(READER_AUTH_JSON, username, FileUtil.ENCODING);
            Reader newReader = new Reader(username, "admin", queries);
            if (rawReader != null) {
                newReader.merge(rawReader);
            }
            FileUtil.writeReaderAuth(READER_AUTH_JSON, newReader);
            return Stream.of(newReader);
        }
        return Stream.of(new Reader());
    }

    /**
     * 查看指定用户的权限列表【admin】
     *
     * @param username :用户名
     * @return 返回指定用户的配置信息
     * @Description: TODO
     */
    @Override
    @Admin
    @Procedure(name = "olab.security.fetchUserAuth", mode = DBMS)
    @Description("CALL olab.security.fetchUserAuth('reader-1') YIELD value RETURN value")
    public Stream<Output> fetchUserAuth(@Name("username") String username) {
        return Stream.of(
                FileUtil.readAuthList(READER_AUTH_JSON, PUBLISHER_AUTH_JSON)
                .parallelStream()
                .filter(v -> {
                    JSONObject object = (JSONObject) v;
                    return object.getString("username").equals(username);
                })
                .map(Output::new)
                .findFirst()
                .orElse(new Output())
        );
    }

    private void check(List<Map<String, String>> queries) {
        if (Objects.isNull(queries) || queries.isEmpty()) {
            throw new ParameterNotFoundException("queries not exist!");
        }
        for (int i = 0; i < queries.size(); i++) {
            Map<String, String> v = queries.get(i);
            for (String field : new String[]{"query_id", "query"}) {
                if (!v.containsKey(field)) {
                    throw new ParameterNotFoundException("queries:" + field + " not exists!" + "index:" + i);
                } else if ((v.get(field) == null) || ("".equals(v.get(field)))) {
                    throw new ParameterNotFoundException("queries:" + field + " [Set String] type error!" + "index:" + i);
                }
            }
        }
    }

    /**
     * 重置指定用户的权限列表【admin】
     *
     * @param username :用户名
     * @return 返回本次配置信息
     * @Description: TODO
     */
    @Override
    @Admin
    @Procedure(name = "olab.security.clear", mode = DBMS)
    @Description("CALL olab.security.clear('reader') YIELD username,currentRole")
    public Stream<User> clear(@Name("username") String username) {
        if (Objects.nonNull(username)) {
            FileUtil.clearAuth(PUBLISHER_AUTH_JSON, username, FileUtil.ENCODING);
            FileUtil.clearAuth(READER_AUTH_JSON, username, FileUtil.ENCODING);
        }
        return Stream.of(new User(username, "admin"));
    }

    /**
     * 获取所有的已配置权限列表【admin】
     *
     * @return 返回本次配置信息
     * @Description: TODO
     */
    @Override
    @Admin
    @Procedure(name = "olab.security.list", mode = DBMS)
    @Description("CALL olab.security.list() YIELD value")
    public Stream<ListResult> list() {
        return Stream.of(new ListResult(FileUtil.readAuthList(READER_AUTH_JSON, PUBLISHER_AUTH_JSON)));
    }

    /**
     * 根据用户名称信息获取权限列表
     *
     * @return 返回用户的配置信息
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.get", mode = READ)
    @Description("CALL olab.security.get() YIELD value")
    public Stream<Output> get() {
        String user = securityContext.subject().username();
        List<Object> value = FileUtil.readAuthList(READER_AUTH_JSON, PUBLISHER_AUTH_JSON);
        return Stream.of(
                new Output(
                        value.parallelStream()
                                .filter(v -> {
                                    JSONObject object = (JSONObject) v;
                                    return user.equals(object.get("username"));
                                })
                                .map(v -> {
                                    JSONObject object = (JSONObject) v;
                                    object.put("currentRole", StringUtil.btnSquareBktOnce(securityContext.mode().name()));
                                    return object;
                                })
                                .findFirst()
                                .orElse(new JSONObject())
                )
        );
    }

    /**
     * 获取权限说明列表
     *
     * @return 权限限制的说明
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.getAuth", mode = READ)
    @Description("CALL olab.security.getAuth() YIELD operate,level,description")
    public Stream<Operator> getAuth() {
        List<Operator> list = Arrays.asList(Operator.values());
        return list.stream();
    }

    /**
     * 可设置值的类型
     *
     * @return 可设置值的类型
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.getValueTypes", mode = READ)
    @Description("CALL olab.security.getValueTypes() YIELD value")
    public Stream<Output> getValueTypes() {
        return Arrays.asList(Types.values())
                .parallelStream()
                .map(v -> new Output(v.toString()))
                .collect(Collectors.toList())
                .stream();
    }
}


