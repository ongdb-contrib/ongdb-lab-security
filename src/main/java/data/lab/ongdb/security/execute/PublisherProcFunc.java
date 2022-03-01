package data.lab.ongdb.security.execute;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import data.lab.ongdb.security.common.Operator;
import data.lab.ongdb.security.common.ParaWrap;
import data.lab.ongdb.security.common.Types;
import data.lab.ongdb.security.common.UserAuthGet;
import data.lab.ongdb.security.inter.PublisherProcFuncInter;
import data.lab.ongdb.security.result.MapResult;
import org.neo4j.cypher.ParameterNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.WRITE;

/**
 * 数据修改
 *
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.execute
 * @Description: TODO
 * @date 2022/2/22 9:28
 */
public class PublisherProcFunc implements PublisherProcFuncInter {

    private static final String PUBLISHER_AUTH_JSON = "publisher_auth.json";

    @Context
    public GraphDatabaseService db;

    @Context
    public SecurityContext securityContext;

    /**
     * 合并节点
     *
     * @param label      :标签名称
     * @param mergeField :合并时使用的字段名
     * @param mergeValue :合并时合并字段设置的值
     * @param otherPros  :其它需要设置的节点属性信息
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.merge.node", mode = WRITE)
    @Description("CALL olab.security.publisher.merge.node({label},{merge_field},{merge_value},{[other_pros]},{[other_labels]}) YIELD value RETURN value")
    public Stream<MapResult> mergeNode(@Name("label") String label, @Name("mergeField") String mergeField, @Name("mergeValue") Object mergeValue, @Name("otherPros") Map<String, Object> otherPros) {

        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);

        // 对属性值进行检查：为用户设置权限时，对于值的类型也设置了`check`操作，在这里检查用户输入的值类型是否满足管理员限定的要求
        // 对属性值的也进行判断：设置权限时可设置`invalid_values`参数，表示对值进行验证，如果属性值包含在这个列表中则提示属性错误，通常使用在限制用户输入错误的值或限制用户不能设置特定的属性值
        mergeNodeParaCheck(userAuth, label, mergeField, mergeValue, otherPros);

        // 主要对用户权限进行检查
        mergeNodeAuthCheck(userAuth, label, mergeField, mergeValue, otherPros);

        String query = "MERGE (n:" + label + " {" + mergeField + ":$mergeValue}) SET n+=$otherPros RETURN ID(n) AS node_id";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("mergeValue", mergeValue);
            put("otherPros", Objects.nonNull(otherPros) ? otherPros : Collections.emptyMap());
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void mergeNodeParaCheck(JSONObject userAuth, String label, String mergeField, Object mergeValue, Map<String, Object> otherPros) throws ParameterNotFoundException {

        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
        JSONObject nodeLabelObj = UserAuthGet.nodeLabelObject(userAuth, label);

        // 标签有效性检查
        checkLabelOrType(label, nodeLabelObj.getJSONArray("invalid_values"));

        if (nodeLabelObj.isEmpty()) {
            throw new ParameterNotFoundException("label permission not obtained[" + label + "]!");
        }

        checkFieldValue(nodeLabelObj.getJSONArray("properties"), new HashMap<String, Object>() {{
            putAll(Objects.nonNull(otherPros) ? otherPros : Collections.emptyMap());
            put(mergeField, mergeValue);
        }});
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void mergeNodeAuthCheck(JSONObject userAuth, String label, String mergeField, Object mergeValue, Map<String, Object> otherPros) throws ParameterNotFoundException {

        // 获取标签的权限
        Operator labelOperator = UserAuthGet.labelOperator(userAuth, label);
        Set<String> prosSet = Objects.isNull(otherPros) ? Collections.emptySet() : otherPros.keySet();
        List<String> prosKey = new ArrayList<String>() {{
            addAll(prosSet);
            add(mergeField);
        }};

        if (labelOperator.equals(Operator.READER_FORBID) || labelOperator.equals(Operator.READER)) {
            // 不能读取标签 即 不能修改标签
            // 需要编辑标签则报错【即对标签进行新增或者删除】
            ifNotExistsLabelThenError(label);

            // 任一属性不可编辑即报错
            ifAnyProsNotCanEditorThenError(userAuth, label, prosKey);

            // 是否需要新建属性，如果不存在新建权限则报错
            ifNotCanBuildNewProsThenError(userAuth, label);

        } else if (labelOperator.equals(Operator.EDITOR) || labelOperator.equals(Operator.PUBLISHER) ||
                labelOperator.equals(Operator.DELETER_RESTRICT) || labelOperator.equals(Operator.DELETER)) {
            // 可以新增或者删除标签
            // 任一属性不可编辑即报错
            ifAnyProsNotCanEditorThenError(userAuth, label, prosKey);

            // 是否需要新建属性，如果不存在新建权限则报错
            ifNotCanBuildNewProsThenError(userAuth, label);
        }
    }

    /**
     * 删除节点
     *
     * @param nodeId :节点ID
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.delete.node", mode = WRITE)
    @Description("CALL olab.security.publisher.delete.node({node_id}) YIELD value RETURN value")
    public Stream<MapResult> deleteNode(@Name("nodeId") Long nodeId) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);

        // 检查用户是否有删除节点的权限：用户可以删除节点时即拥有对应节点的全部标签删除权限、全部属性删除权限
        deleteNodeAuthCheck(userAuth, nodeId);

        String query = "MATCH (n) WHERE ID(n)=$id DELETE n RETURN 'delete node " + nodeId + " succeeded!' AS message";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", nodeId);
        }};
        return execute(query, params);
    }

    /**
     * 分用户权限检查
     **/
    @Override
    public void deleteNodeAuthCheck(JSONObject userAuth, Long nodeId) throws ParameterNotFoundException {
        // 检查用户是否有删除节点的权限：用户可以删除节点时即拥有对应节点的全部标签删除权限、全部属性删除权限
        // 获取节点信息
        String query = "MATCH (n) WHERE ID(n)=$id RETURN LABELS(n) AS labels,PROPERTIES(n) AS pros";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", nodeId);
        }};
        Optional<MapResult> result = execute(query, params).findFirst();
        if (result.isPresent()) {
            MapResult mapResult = result.get();
            Map<String, Object> value = mapResult.value;
            List<String> labels = (List<String>) value.get("labels");
            Map<String, Object> pros = (Map<String, Object>) value.get("pros");
            if (!labels.isEmpty()) {
                // 检查标签权限
                labels.forEach(label -> {
                    Operator operator = UserAuthGet.labelOperator(userAuth, label);
                    if (operator.getLevel().intValue() < 5) {
                        throw new ParameterNotFoundException("label permission denied[No label deletion permission][" + operator.getOperate().toUpperCase() + "]!");
                    }
                });
            } else {
                throw new ParameterNotFoundException("label permission denied[No label deletion permission][label is null]!");
            }
            if (!pros.isEmpty()) {
                // 检查属性权限
                pros.keySet().forEach(key -> {
                    // 获取标签下指定属性KEY的所有操作权限
                    List<Operator> operators = UserAuthGet.prosKeyOperator(userAuth, labels, key);
                    // 遍历操作权限，任一不满足删除权限则报错
                    operators.forEach(operator -> {
                        if (operator.getLevel().intValue() < 5) {
                            throw new ParameterNotFoundException("properties-key permission denied[No properties-key deletion permission][" + operator.getOperate().toUpperCase() + "]!");
                        }
                    });
                });
            }
        } else {
            throw new ParameterNotFoundException("permissions cannot be judged!");
        }
    }

    /**
     * 合并关系
     *
     * @param startId      :from节点ID
     * @param endId        :to节点ID
     * @param mergeRelType :合并的关系类型
     * @param relPros      :需要给关系设置的属性信息
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.merge.relationship", mode = WRITE)
    @Description("CALL olab.security.publisher.merge.relationship({start_id},{end_id},{merge_rel_type},{rel_pros}) YIELD path RETURN path")
    public Stream<MapResult> mergeRelationship(@Name("startId") Long startId, @Name("endId") Long endId, @Name("mergeRelType") String mergeRelType, @Name("relPros") Map<String, Object> relPros) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);

        // 参数检查
        mergeRelationshipParaCheck(userAuth, startId, endId, mergeRelType, relPros);
        // 权限检查
        mergeRelationshipAuthCheck(userAuth, startId, endId, mergeRelType, relPros);

        String query = "MATCH (from),(to) WHERE ID(from)=$fromId AND ID(to)=$toId WITH from,to MERGE p=(from)-[r:" + mergeRelType + "]-(to) SET r+=$relPros RETURN ID(r) AS rel_id";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("fromId", startId);
            put("toId", endId);
            put("relPros", Objects.nonNull(relPros) ? relPros : Collections.emptyMap());
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void mergeRelationshipParaCheck(JSONObject userAuth, Long startId, Long endId, String mergeRelType, Map<String, Object> relPros) throws ParameterNotFoundException {

        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
        JSONObject relTypeObj = UserAuthGet.relTypeObject(userAuth, labels(userAuth, startId), mergeRelType, labels(userAuth, endId));

        // 标签有效性检查
        checkLabelOrType(mergeRelType, relTypeObj.getJSONArray("invalid_values"));

        if (relTypeObj.isEmpty()) {
            throw new ParameterNotFoundException("relationship type permission not obtained!");
        }

        checkFieldValue(relTypeObj.getJSONArray("properties"), new HashMap<String, Object>() {{
            putAll(Objects.nonNull(relPros) ? relPros : Collections.emptyMap());
        }});
    }

    /**
     * 获取用户有权限的标签
     **/
    private List<String> labels(JSONObject userAuth, Long startId) {
        String query = "MATCH (n) WHERE ID(n)=$id RETURN LABELS(n) AS labels";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", startId);
        }};
        Optional<MapResult> result = execute(query, params).findFirst();
        if (result.isPresent()) {
            MapResult mapResult = result.get();
            Map<String, Object> value = mapResult.value;
            List<String> labels = (List<String>) value.get("labels");
            if (!labels.isEmpty()) {
                return labels.parallelStream()
                        .filter(label -> {
                            boolean bool = UserAuthGet.isContainslabelOperator(userAuth, label);
                            if (bool) {
                                return bool;
                            } else {
                                throw new ParameterNotFoundException("node has " + labels.size() + " labels but this label `" + label + "` has no permission!");
                            }
                        })
                        .collect(Collectors.toList());
            } else {
                throw new ParameterNotFoundException("label permission denied[No label deletion permission][label is null]!");
            }
        } else {
            throw new ParameterNotFoundException("label permission denied[No label deletion permission][label is null]!");
        }
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void mergeRelationshipAuthCheck(JSONObject userAuth, Long startId, Long endId, String mergeRelType, Map<String, Object> relPros) throws ParameterNotFoundException {
        // 获取关系类型的权限
        List<String> startLabels = labels(userAuth, startId);
        List<String> endLabels = labels(userAuth, endId);
        Operator typeOperator = UserAuthGet.typeOperator(userAuth, startLabels, mergeRelType, endLabels);
        Set<String> prosSet = Objects.isNull(relPros) ? Collections.emptySet() : relPros.keySet();
        List<String> prosKey = new ArrayList<String>() {{
            addAll(prosSet);
        }};

        // 过滤出来的关系的权限
        JSONObject object = UserAuthGet.relTypeObject(userAuth, startLabels, mergeRelType, endLabels);
        if (typeOperator.equals(Operator.READER_FORBID) || typeOperator.equals(Operator.READER)) {
            // 不能读取关系 即 不能修改关系
            // 需要编辑关系则报错【即对关系进行新增或者删除】
            ifNotExistsTypeThenError(mergeRelType);

            // 任一属性不可编辑即报错
            ifAnyProsNotCanEditorThenError(object, prosKey);

            // 是否需要新建属性，如果不存在新建权限则报错
            ifNotCanBuildNewProsThenError(object);

        } else if (typeOperator.equals(Operator.EDITOR) || typeOperator.equals(Operator.PUBLISHER) ||
                typeOperator.equals(Operator.DELETER_RESTRICT) || typeOperator.equals(Operator.DELETER)) {
            // 可以新增或者删除关系
            // 任一属性不可编辑即报错
            ifAnyProsNotCanEditorThenError(object, prosKey);

            // 是否需要新建属性，如果不存在新建权限则报错
            ifNotCanBuildNewProsThenError(object);
        }
    }

    /**
     * 删除关系
     *
     * @param relId :关系ID
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.delete.relationship", mode = WRITE)
    @Description("CALL olab.security.publisher.delete.relationship({rel_id}) YIELD value RETURN value")
    public Stream<MapResult> deleteRelationship(@Name("relId") Long relId) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);

        // 检查用户是否有删除节点的权限：用户可以删除节点时即拥有对应节点的全部标签删除权限、全部属性删除权限
        deleteRelationshipAuthCheck(userAuth, relId);

        String query = "MATCH ()-[r]->() WHERE ID(r)=$id DELETE r  RETURN 'delete relationship " + relId + " succeeded!' AS message";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", relId);
        }};
        return execute(query, params);
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void deleteRelationshipAuthCheck(JSONObject userAuth, Long relId) throws ParameterNotFoundException {
        // 检查用户是否有删除关系的权限：用户可以删除关系时即拥有对应关系类型删除权限、必须拥有全部属性删除权限
        // 获取关系信息
        Optional<MapResult> result = typeInfo(relId);
        if (result.isPresent()) {
            MapResult mapResult = result.get();
            Map<String, Object> value = mapResult.value;
            List<String> startLabels = (List<String>) value.get("start_labels");
            String type = (String) value.get("type");
            List<String> endLabels = (List<String>) value.get("end_labels");
            Map<String, Object> pros = (Map<String, Object>) value.get("pros");
            // 过滤出来的关系的权限
            JSONObject object = UserAuthGet.relTypeObject(userAuth, startLabels, type, endLabels);
            if (!object.isEmpty()) {
                // 检查关系权限
                Operator operator = Operator.from(object.getString("operator"));
                if (operator.getLevel().intValue() < 5) {
                    throw new ParameterNotFoundException("relationshipType permission denied[No relationshipType deletion permission][" + operator.getOperate().toUpperCase() + "]!");
                }
                prosAuthLevelJudge(object, pros.keySet(), 5, "deletion");
            } else {
                throw new ParameterNotFoundException("relationshipType permission denied[No relationshipType deletion permission][relationshipType is null]!");
            }
        } else {
            throw new ParameterNotFoundException("permissions cannot be judged!");
        }
    }

    /**
     * 修改节点的属性值
     *
     * @param nodeId     :节点ID
     * @param fieldName  :字段名
     * @param fieldValue :字段值
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.update.node", mode = WRITE)
    @Description("CALL olab.security.publisher.update.node({node_id},{field_name},{field_value}) YIELD value RETURN value")
    public Stream<MapResult> updateNode(@Name("nodeId") Long nodeId, @Name("fieldName") String fieldName, @Name("fieldValue") Object fieldValue) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);
        // 参数检查
        updateNodeParaCheck(userAuth, nodeId, fieldName, fieldValue);
        // 权限检查
        updateNodeAuthCheck(userAuth, nodeId, fieldName, fieldValue);

        String query = "MATCH (n) WHERE ID(n)=$id SET n+=$pros RETURN ID(n) AS node_id";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", nodeId);
            put("pros", new HashMap<String, Object>() {{
                put(fieldName, fieldValue);
            }});
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void updateNodeParaCheck(JSONObject userAuth, Long nodeId, String fieldName, Object fieldValue) throws ParameterNotFoundException {
        List<String> labels = labels(userAuth, nodeId);
        for (String label : labels) {
            // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
            JSONObject nodeLabelObj = UserAuthGet.nodeLabelObject(userAuth, label);

            // 标签有效性检查
            checkLabelOrType(label, nodeLabelObj.getJSONArray("invalid_values"));

            if (nodeLabelObj.isEmpty()) {
                throw new ParameterNotFoundException("label permission not obtained[" + label + "]!");
            }

            checkFieldValue(nodeLabelObj.getJSONArray("properties"), new HashMap<String, Object>() {{
                putAll(Collections.emptyMap());
                put(fieldName, fieldValue);
            }});
        }
    }

    /**
     * 分用户权限检查
     * 需要在每个标签上都有该权限才行
     */
    @Override
    public void updateNodeAuthCheck(JSONObject userAuth, Long nodeId, String fieldName, Object fieldValue) throws ParameterNotFoundException {
        // 判断在字段上是否存在更新权限
        List<String> labels = labels(userAuth, nodeId);
        // 获取标签下指定属性KEY的所有操作权限
        List<Operator> operators = UserAuthGet.prosKeyOperator(userAuth, labels, fieldName);
        // 遍历操作权限，任一不满足删除权限则报错
        operators.forEach(operator -> {
            if (operator.getLevel().intValue() < 3) {
                throw new ParameterNotFoundException("properties-key permission denied[No properties-key update permission][" + operator.getOperate().toUpperCase() + "]!");
            }
        });
    }

    /**
     * 修改关系的属性值
     *
     * @param relId      :关系ID
     * @param fieldName  :字段名
     * @param fieldValue :字段值
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.update.relationship", mode = WRITE)
    @Description("CALL olab.security.publisher.update.relationship({rel_id},{field_name},{field_value}) YIELD path RETURN path")
    public Stream<MapResult> updateRelationship(@Name("relId") Long relId, @Name("fieldName") String fieldName, @Name("fieldValue") Object fieldValue) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);
        // 参数检查
        updateRelationshipParaCheck(userAuth, relId, fieldName, fieldValue);
        // 权限检查
        updateRelationshipAuthCheck(userAuth, relId, fieldName, fieldValue);

        String query = "MATCH ()-[r]->() WHERE ID(r)=$id SET r+=$pros RETURN ID(r) AS rel_id";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", relId);
            put("pros", new HashMap<String, Object>() {{
                put(fieldName, fieldValue);
            }});
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void updateRelationshipParaCheck(JSONObject userAuth, Long relId, String fieldName, Object fieldValue) throws ParameterNotFoundException {
        // 对字段入参进行检查
        Optional<MapResult> result = typeInfo(relId);
        if (result.isPresent()) {
            MapResult mapResult = result.get();
            Map<String, Object> value = mapResult.value;
            List<String> startLabels = (List<String>) value.get("start_labels");
            String type = (String) value.get("type");
            List<String> endLabels = (List<String>) value.get("end_labels");
            // 过滤出来的关系的权限
            JSONObject object = UserAuthGet.relTypeObject(userAuth, startLabels, type, endLabels);
            if (!object.isEmpty()) {
                checkFieldValue(object.getJSONArray("properties"), new HashMap<String, Object>() {{
                    putAll(Collections.emptyMap());
                    put(fieldName, fieldValue);
                }});
            } else {
                throw new ParameterNotFoundException("relationshipType permission denied[No relationshipType deletion permission][relationshipType is null]!");
            }
        } else {
            throw new ParameterNotFoundException("permissions cannot be judged!");
        }
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void updateRelationshipAuthCheck(JSONObject userAuth, Long relId, String fieldName, Object fieldValue) throws ParameterNotFoundException {
        // 检查用户是否有删除关系的权限：用户可以删除关系时即拥有对应关系类型删除权限、必须拥有全部属性删除权限
        // 获取关系信息
        Optional<MapResult> result = typeInfo(relId);
        if (result.isPresent()) {
            MapResult mapResult = result.get();
            Map<String, Object> value = mapResult.value;
            List<String> startLabels = (List<String>) value.get("start_labels");
            String type = (String) value.get("type");
            List<String> endLabels = (List<String>) value.get("end_labels");
            Map<String, Object> pros = (Map<String, Object>) value.get("pros");
            // 过滤出来的关系的权限
            JSONObject object = UserAuthGet.relTypeObject(userAuth, startLabels, type, endLabels);
            if (!object.isEmpty()) {
                // 检查关系权限
                Operator operator = Operator.from(object.getString("operator"));
                if (operator.getLevel().intValue() < 3) {
                    throw new ParameterNotFoundException("relationshipType permission denied[No relationshipType deletion permission][" + operator.getOperate().toUpperCase() + "]!");
                }
            } else {
                throw new ParameterNotFoundException("relationshipType permission denied[No relationshipType deletion permission][relationshipType is null]!");
            }
            if (!pros.isEmpty()) {
                // 检查属性权限
                // 获取标签下指定属性KEY的所有操作权限
                List<Operator> operators = UserAuthGet.prosKeyOperator(object, pros.keySet());
                // 遍历操作权限，任一不满足删除权限则报错
                operators.forEach(operator -> {
                    if (operator.getLevel().intValue() < 3) {
                        throw new ParameterNotFoundException("properties-key permission denied[No properties-key deletion permission][" + operator.getOperate().toUpperCase() + "]!");
                    }
                });
            }
        } else {
            throw new ParameterNotFoundException("permissions cannot be judged!");
        }
    }

    /**
     * 删除节点的属性键
     *
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.remove.node.key", mode = WRITE)
    @Description("CALL olab.security.publisher.remove.node.key({node_id},{field_name}) YIELD value RETURN value")
    public Stream<MapResult> removeNodeKey(@Name("nodeId") Long nodeId, @Name("fieldName") String fieldName) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);

        // 参数检查
        removeNodeKeyParaCheck(userAuth, nodeId, fieldName);
        // 权限检查
        removeNodeKeyAuthCheck(userAuth, nodeId, fieldName);

        String query = "MATCH (n) WHERE ID(n)=$id REMOVE n." + fieldName + " RETURN 'delete node " + nodeId + " properties-key succeeded!' AS message";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", nodeId);
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void removeNodeKeyParaCheck(JSONObject userAuth, Long nodeId, String fieldName) throws ParameterNotFoundException {
        List<String> labels = labels(userAuth, nodeId);
        for (String label : labels) {
            // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
            JSONObject nodeLabelObj = UserAuthGet.nodeLabelObject(userAuth, label);

            if (nodeLabelObj.isEmpty()) {
                throw new ParameterNotFoundException("label permission not obtained[" + label + "]!");
            }
            checkField(fieldName);
        }
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void removeNodeKeyAuthCheck(JSONObject userAuth, Long nodeId, String fieldName) throws ParameterNotFoundException {
        // 判断在字段上是否存在更新权限
        List<String> labels = labels(userAuth, nodeId);
        // 获取标签下指定属性KEY的所有操作权限
        List<Operator> operators = UserAuthGet.prosKeyOperator(userAuth, labels, fieldName);
        // 遍历操作权限，任一不满足删除权限则报错
        operators.forEach(operator -> {
            if (operator.getLevel().intValue() < 5) {
                throw new ParameterNotFoundException("properties-key permission denied[No properties-key deletion permission][" + operator.getOperate().toUpperCase() + "]!");
            }
        });
    }

    /**
     * 删除节点的某个标签
     *
     * @param nodeId
     * @param label
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.remove.node.label", mode = WRITE)
    @Description("CALL olab.security.publisher.remove.node.label({node_id},{label}) YIELD value RETURN value")
    public Stream<MapResult> removeNodeLabel(@Name("nodeId") Long nodeId, @Name("label") String label) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);

        // 参数检查
        removeNodeLabelParaCheck(userAuth, label);
        // 权限检查
        removeNodeLabelAuthCheck(userAuth, label);

        String query = "MATCH (n) WHERE ID(n)=$id REMOVE n:" + label + " RETURN 'delete node " + nodeId + " label `" + label + "` succeeded!' AS message";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", nodeId);
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void removeNodeLabelParaCheck(JSONObject userAuth, String label) throws ParameterNotFoundException {
        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
        JSONObject nodeLabelObj = UserAuthGet.nodeLabelObject(userAuth, label);

        if (nodeLabelObj.isEmpty()) {
            throw new ParameterNotFoundException("label permission not obtained[" + label + "]!");
        }

        // 标签有效性检查
        checkLabelOrType(label, nodeLabelObj.getJSONArray("invalid_values"));
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void removeNodeLabelAuthCheck(JSONObject userAuth, String label) throws ParameterNotFoundException {
        // 获取标签操作权限
        Operator operator = UserAuthGet.labelOperator(userAuth, label);
        // 遍历操作权限，任一不满足删除权限则报错
        if (operator.getLevel().intValue() < 3) {
            throw new ParameterNotFoundException("label permission denied[No label deletion permission][" + operator.getOperate().toUpperCase() + "]!");
        }
    }

    /**
     * 删除关系的属性键
     *
     * @param relId
     * @param fieldName
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.remove.relationship.key", mode = WRITE)
    @Description("CALL olab.security.publisher.remove.relationship.key({rel_id},{field_name}) YIELD path RETURN path")
    public Stream<MapResult> removeRelationshipKey(@Name("relId") Long relId, @Name("fieldName") String fieldName) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);
        // 参数检查
        removeRelationshipKeyParaCheck(fieldName);
        // 权限检查
        removeRelationshipKeyAuthCheck(userAuth, relId, fieldName);
        String query = "MATCH ()-[r]->() WHERE ID(r)=$id REMOVE r." + fieldName + " RETURN 'delete relationship " + relId + " properties-key succeeded!' AS message";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", relId);
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void removeRelationshipKeyParaCheck(String fieldName) throws ParameterNotFoundException {
        // 标签有效性检查
        checkField(fieldName);
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void removeRelationshipKeyAuthCheck(JSONObject userAuth, Long relId, String fieldName) throws ParameterNotFoundException {
        JSONObject object = typeInfoAuth(userAuth, relId);
        if (!object.isEmpty()) {
            // 判断指定的属性键是否满足要求的操作权限级别
            prosAuthLevelJudge(object, new HashSet<String>() {{
                add(fieldName);
            }}, 5, "deletion");
        } else {
            throw new ParameterNotFoundException("relationshipType permission denied[No relationshipType deletion permission][relationshipType is null]!");
        }
    }

    /**
     * 从用户权限中获取属性KEY权限列表，并通过权限级别判断指定的属性KEY是否满足指定权限
     *
     * @param object:用户权限
     * @param keys:属性键列表
     * @param level:小于该权限级别则报警提示
     * @param operateType:操作类别
     * @return
     * @Description: TODO
     */
    private void prosAuthLevelJudge(JSONObject object, Set<String> keys, int level, String operateType) {
        if (Objects.nonNull(keys) && !keys.isEmpty()) {
            // 检查属性权限
            // 获取标签下指定属性KEY的所有操作权限
            List<Operator> operators = UserAuthGet.prosKeyOperator(object, keys);
            // 遍历操作权限，任一不满足删除权限则报错
            operators.forEach(operator -> {
                if (operator.getLevel().intValue() < level) {
                    throw new ParameterNotFoundException("properties-key permission denied[No properties-key " + operateType + " permission][" + operator.getOperate().toUpperCase() + "]!");
                }
            });
        }
    }

    /**
     * 增加节点的标签
     *
     * @param nodeId :节点ID
     * @param label  :标签
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.add.node.label", mode = WRITE)
    @Description("CALL olab.security.publisher.add.node.label({node_id},{label}) YIELD value RETURN value")
    public Stream<MapResult> addNodeLabel(@Name("nodeId") Long nodeId, @Name("label") String label) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);
        // 参数检查
        addNodeLabelParaCheck(userAuth, label);
        // 权限检查
        addNodeLabelAuthCheck(userAuth, label);

        String query = "MATCH (n) WHERE ID(n)=$id SET n:" + label + " RETURN 'add node " + nodeId + " label `" + label + "` succeeded!' AS message";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", nodeId);
        }};
        return execute(query, params);
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void addNodeLabelParaCheck(JSONObject userAuth,  String label) throws ParameterNotFoundException {
        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
        JSONObject nodeLabelObj = UserAuthGet.nodeLabelObject(userAuth, label);

        if (nodeLabelObj.isEmpty()) {
            throw new ParameterNotFoundException("label permission not obtained[" + label + "]!");
        }

        // 标签有效性检查
        checkLabelOrType(label, nodeLabelObj.getJSONArray("invalid_values"));
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void addNodeLabelAuthCheck(JSONObject userAuth, String label) throws ParameterNotFoundException {
        // 获取标签操作权限
        Operator operator = UserAuthGet.labelOperator(userAuth, label);
        // 遍历操作权限，任一不满足删除权限则报错
        if (operator.getLevel().intValue() < 3) {
            throw new ParameterNotFoundException("label permission denied[No label deletion permission][" + operator.getOperate().toUpperCase() + "]!");
        }
    }

    /**
     * 执行QUERY
     **/
    private Stream<MapResult> execute(String query, Map<String, Object> params) {
        return db.execute(ParaWrap.withParamMapping(query, params.keySet()), params).stream().map(MapResult::new);
    }

    /**
     * @param properties:属性列表 field check operator
     * @param map:用户传入属性map
     * @return
     * @Description: TODO
     */
    private void checkFieldValue(JSONArray properties, HashMap<String, Object> map) {
        for (String key : map.keySet()) {
            Object value = map.get(key);
            checkField(key);
            checkValue(value, getValueCheckType(properties, key));
            checkValueValidity(value, getValueCheckValidity(properties, key));
        }
    }

    /**
     * @param value:属性值
     * @param valueCheckType:权限配置的值的指定类型
     * @return
     * @Description: TODO
     */
    private void checkValue(Object value, String valueCheckType) {
        String valueType = Objects.nonNull(value) ? Types.of(value).toString().toUpperCase() : "Can't set null value!";
        String checkType = Objects.nonNull(valueCheckType) ? valueCheckType.toUpperCase() : "Field can not get a permission type!";
        if (!valueType.equals(checkType)) {
            throw new ParameterNotFoundException("the value is of the wrong type or the field does not have permission[NEED:" + checkType + ",BUT OFFER:" + valueType + "]!");
        }
    }

    /**
     * @param value:属性值
     * @param valueCheckValidity:权限配置的值的指定类型
     * @return
     * @Description: TODO
     */
    private void checkValueValidity(Object value, JSONArray valueCheckValidity) {
        if (Objects.nonNull(valueCheckValidity) && valueCheckValidity.contains(value)) {
            throw new ParameterNotFoundException("value validation failed[invalid_values:"+valueCheckValidity.toJSONString()+"]!");
        }
    }

    /**
     * 获取属性键对应值的检查类型
     **/
    private String getValueCheckType(JSONArray properties, String key) {
        return properties.parallelStream()
                .filter(v -> {
                    JSONObject jsonObject = (JSONObject) v;
                    return key.equals(jsonObject.get("field"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject()).getString("check");
    }

    /**
     * 获取属性键对应值的有效性检查
     **/
    private JSONArray getValueCheckValidity(JSONArray properties, String key) {
        return properties.parallelStream()
                .filter(v -> {
                    JSONObject jsonObject = (JSONObject) v;
                    return key.equals(jsonObject.get("field"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject()).getJSONArray("invalid_values")
                .parallelStream()
                .map(v -> {
                    if (v instanceof Integer) {
                        return Long.valueOf(String.valueOf(v));
                    } else {
                        return v;
                    }
                })
                .collect(Collectors.toCollection(JSONArray::new));
    }

    /**
     * 值有效性检查
     **/
    private void checkValue(Collection<Object> value) {
        value.forEach(
                this::checkValue
        );
    }

    /**
     * 值有效性检查
     **/
    private void checkValue(Object value) {
        if (!Arrays.asList(Types.values()).contains(Types.of(value))) {
            throw new ParameterNotFoundException("value type error!");
        }
    }

    /**
     * 字段有效性检查
     **/
    private void checkField(Set<String> set) {
        set.forEach(
                this::checkField
        );
    }

    /**
     * 字段有效性检查
     **/
    private void checkField(String field) {
        if (Objects.isNull(field) || "".equals(field)) {
            throw new ParameterNotFoundException("field error!");
        }
    }

    /**
     * 标签或关系类型有效性检查
     **/
    private void checkLabelOrType(String labelOrType, JSONArray invalidValues) throws ParameterNotFoundException {
        if (Objects.isNull(labelOrType) || "".equals(labelOrType)) {
            throw new ParameterNotFoundException("the label is of the wrong type or the label does not have permission!");
        }
        if (Objects.isNull(invalidValues) || invalidValues.contains(labelOrType)) {
            throw new ParameterNotFoundException("the label is of the wrong type or the label does not have permission!");
        }
    }

    /**
     * 是否需要新建属性，如果不存在新建权限则报错
     *
     * @param
     * @return
     * @Description: TODO
     */
    private void ifNotCanBuildNewProsThenError(JSONObject userAuth, String label) throws ParameterNotFoundException {
        List<Map<String, Operator>> prosOperator = UserAuthGet.prosOperator(userAuth, label);
        prosOperator.forEach(v -> {
            // 需要新建
            if (!needBuildNewPros(v)) {
                // 有新建权限嘛，没有就报错
                if (!hasBuildNewProsAuth(prosOperator, v)) {
                    throw new ParameterNotFoundException("properties permission denied[New properties permission does not exist]!");
                }
            }
        });
    }

    /**
     * 属性是否有新建权限
     **/
    private boolean hasBuildNewProsAuth(List<Map<String, Operator>> prosOperator, Map<String, Operator> v) {
        Optional<Map<String, Operator>> operator = prosOperator.parallelStream()
                .filter(f -> f.keySet().equals(v.keySet()))
                .findFirst();

        if (operator.isPresent()) {
            return operator.get().values().iterator().next().getLevel().intValue() > 3;
        } else {
            throw new ParameterNotFoundException("properties permission denied[New properties permission does not exist]!");
        }
    }

    /**
     * 判断属性需不需要新建
     **/
    private boolean needBuildNewPros(Map<String, Operator> v) {
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("value", v.keySet().iterator().next());
        }};
        return execute(ParaWrap.withParamMapping("CALL db.propertyKeys() YIELD propertyKey WHERE propertyKey=$value RETURN value", params.keySet()), params)
                .findFirst()
                .isPresent();
    }

    /**
     * 任一属性不可编辑即报错
     *
     * @param userAuth：用户的权限
     * @param label:标签名
     * @param keys:属性键
     * @return
     * @pa
     * @Description: TODO
     */
    private void ifAnyProsNotCanEditorThenError(JSONObject userAuth, String label, List<String> keys) throws ParameterNotFoundException {
        List<Map<String, Operator>> prosOperator = UserAuthGet.prosOperator(userAuth, label);
        // 过滤出不可编辑属性KEYS
        boolean bool = keys.parallelStream()
                .anyMatch(v -> !ifHasEditorAuth(prosOperator, v));
        if (bool) {
            throw new ParameterNotFoundException("properties permission denied[Properties are not editable]!");
        }
    }

    /**
     * 属性键是否有可编辑权限【True表示可以编辑】
     *
     * @param prosOperator:用户的属性权限
     * @param key:属性KEY
     * @return
     * @Description: TODO
     */
    private boolean ifHasEditorAuth(List<Map<String, Operator>> prosOperator, String key) {
        Optional<Operator> operator = prosOperator.parallelStream()
                .filter(v -> v.containsKey(key))
                .map(v -> v.get(key))
                .findFirst();
        if (operator.isPresent()) {
            return operator.get().getLevel().intValue() > 2;
        } else {
            throw new ParameterNotFoundException("properties permission denied[Properties are not editable]!");
        }
    }

    /**
     * 需要编辑标签则报错【即对标签进行新增或者删除】
     *
     * @param label：标签名
     * @return
     * @Description: TODO
     */
    private void ifNotExistsLabelThenError(String label) throws ParameterNotFoundException {
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("value", label);
        }};
        boolean bool = execute(ParaWrap.withParamMapping("CALL db.labels() YIELD label WHERE label=$value RETURN label", params.keySet()), params)
                .findFirst()
                .isPresent();
        if (!bool) {
            throw new ParameterNotFoundException("label permission denied[No label editing permission]!");
        }
    }

    /**
     * 需要编辑关系则报错【即对关系进行新增或者删除】
     *
     * @param mergeRelType：关系类型
     * @return
     * @Description: TODO
     */
    private void ifNotExistsTypeThenError(String mergeRelType) {
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("value", mergeRelType);
        }};
        boolean bool = execute(ParaWrap.withParamMapping("CALL db.relationshipTypes() YIELD relationshipType WHERE relationshipType=$value RETURN relationshipType", params.keySet()), params)
                .findFirst()
                .isPresent();
        if (!bool) {
            throw new ParameterNotFoundException("relationshipType permission denied[No relationshipType editing permission]!");
        }
    }

    /**
     * 任一属性不可编辑即报错
     *
     * @param object：关系权限对象
     * @param keys:属性键
     * @return
     * @pa
     * @Description: TODO
     */
    private void ifAnyProsNotCanEditorThenError(JSONObject object, List<String> keys) throws ParameterNotFoundException {
        List<Map<String, Operator>> prosOperator = UserAuthGet.getProsOperator(object);
        // 过滤出不可编辑属性KEYS
        boolean bool = keys.parallelStream()
                .anyMatch(v -> !ifHasEditorAuth(prosOperator, v));
        if (bool) {
            throw new ParameterNotFoundException("properties permission denied[Properties are not editable]!");
        }
    }

    /**
     * 是否需要新建属性，如果不存在新建权限则报错
     *
     * @param object：关系权限对象
     * @return
     * @Description: TODO
     */
    private void ifNotCanBuildNewProsThenError(JSONObject object) throws ParameterNotFoundException {
        List<Map<String, Operator>> prosOperator = UserAuthGet.getProsOperator(object);
        prosOperator.forEach(v -> {
            // 需要新建
            if (!needBuildNewPros(v)) {
                // 有新建权限嘛，没有就报错
                if (!hasBuildNewProsAuth(prosOperator, v)) {
                    throw new ParameterNotFoundException("properties permission denied[New properties permission does not exist]!");
                }
            }
        });
    }

    /**
     * 通过关系ID获取关系的基本信息
     *
     * @param
     * @return
     * @Description: TODO
     */
    private Optional<MapResult> typeInfo(Long relId) {
        String query = "MATCH ()-[r]->() WHERE ID(r)=$id RETURN LABELS(STARTNODE(r)) AS start_labels,TYPE(r) AS type,LABELS(ENDNODE(r)) AS end_labels,PROPERTIES(r) AS pros";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("id", relId);
        }};
        return execute(query, params).findFirst();
    }

    /**
     * @param relId:关系ID
     * @param userAuth:用户权限
     * @return
     * @Description: TODO
     */
    private JSONObject typeInfoAuth(JSONObject userAuth, Long relId) {
        Optional<MapResult> result = typeInfo(relId);
        if (result.isPresent()) {
            MapResult mapResult = result.get();
            Map<String, Object> value = mapResult.value;
            List<String> startLabels = (List<String>) value.get("start_labels");
            String type = (String) value.get("type");
            List<String> endLabels = (List<String>) value.get("end_labels");
            // 过滤出来的关系的权限
            return UserAuthGet.relTypeObject(userAuth, startLabels, type, endLabels);
        } else {
            throw new ParameterNotFoundException("permissions cannot be judged!");
        }
    }
}

