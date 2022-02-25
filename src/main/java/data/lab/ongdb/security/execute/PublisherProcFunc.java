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
    @Description("CALL olab.security.publisher.merge.node({label},{merge_field},{merge_value},{[other_pros]},{[other_labels]}) YIELD node RETURN node")
    public Stream<MapResult> mergeNode(@Name("label") String label, @Name("mergeField") String mergeField, @Name("mergeValue") Object mergeValue, @Name("otherPros") Map<String, Object> otherPros) {

        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);

        // 对属性值进行检查：为用户设置权限时，对于值的类型也设置了`check`操作，在这里检查用户输入的值类型是否满足管理员限定的要求
        // 对属性值的也进行判断：设置权限时可设置`invalid_values`参数，表示对值进行验证，如果属性值包含在这个列表中则提示属性错误，通常使用在限制用户输入错误的值或限制用户不能设置特定的属性值
        mergeNodeParaCheck(userAuth, label, mergeField, mergeValue, otherPros);

        // 主要对用户权限进行检查
        mergeNodeAuthCheck(userAuth, label, mergeField, mergeValue, otherPros);

        String query = "MERGE (n:" + label + " {" + mergeField + ":$mergeValue}) SET n+=$otherPros RETURN ID(n) AS id";
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("mergeValue", mergeValue);
            put("otherPros", Objects.nonNull(otherPros) ? otherPros : Collections.emptyMap());
        }};
        return db.execute(ParaWrap.withParamMapping(query, params.keySet()), params).stream().map(MapResult::new);
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
            throw new ParameterNotFoundException("label permission not obtained!");
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
            ifNotCanBuildNewProsThenError(userAuth, label, prosKey);

        } else if (labelOperator.equals(Operator.EDITOR) || labelOperator.equals(Operator.PUBLISHER) ||
                labelOperator.equals(Operator.DELETER_RESTRICT) || labelOperator.equals(Operator.DELETER)) {
            // 可以新增或者删除标签
            // 任一属性不可编辑即报错
            ifAnyProsNotCanEditorThenError(userAuth, label, prosKey);

            // 是否需要新建属性，如果不存在新建权限则报错
            ifNotCanBuildNewProsThenError(userAuth, label, prosKey);
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
        deleteNodeParaCheck(userAuth, nodeId);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void deleteNodeParaCheck(JSONObject userAuth, Long nodeId) throws ParameterNotFoundException {
        // 无参数有效性检查

    }

    /**
     * 分用户权限检查
     */
    @Override
    public void deleteNodeAuthCheck(JSONObject userAuth, Long nodeId) throws ParameterNotFoundException {

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
        mergeRelationshipParaCheck(userAuth, startId, endId, mergeRelType, relPros);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void mergeRelationshipParaCheck(JSONObject userAuth, Long startId, Long endId, String mergeRelType, Map<String, Object> relPros) throws ParameterNotFoundException {

        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
        JSONObject relTypeObj = UserAuthGet.relTypeObject(userAuth, "Person", "ACTED_IN", "Movie");

        // 标签有效性检查
        checkLabelOrType(mergeRelType, relTypeObj.getJSONArray("invalid_values"));

        checkFieldValue(relTypeObj.getJSONArray("properties"), new HashMap<String, Object>() {{
            putAll(relPros);
        }});

    }

    /**
     * 分用户权限检查
     */
    @Override
    public void mergeRelationshipAuthCheck(JSONObject userAuth, Long startId, Long endId, String mergeRelType, Map<String, Object> relPros) throws ParameterNotFoundException {

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
        deleteRelationshipParaCheck(userAuth, relId);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void deleteRelationshipParaCheck(JSONObject userAuth, Long relId) throws ParameterNotFoundException {
        // 无参数有效性检查
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void deleteRelationshipAuthCheck(JSONObject userAuth, Long relId) throws ParameterNotFoundException {

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
    @Description("CALL olab.security.publisher.update.node({node_id},{field_name},{field_value}) YIELD node RETURN node")
    public Stream<MapResult> updateNode(@Name("nodeId") Long nodeId, @Name("fieldName") String fieldName, @Name("fieldValue") Object fieldValue) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);
        updateNodeParaCheck(userAuth, nodeId, fieldName, fieldValue);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void updateNodeParaCheck(JSONObject userAuth, Long nodeId, String fieldName, Object fieldValue) throws ParameterNotFoundException {
        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
//        JSONObject nodeLabelObj = UserAuthGet.nodeLabelObject(userAuth, label);
//
//        checkFieldValue(nodeLabelObj.getJSONArray("properties"), new HashMap<String, Object>() {{
//            putAll(otherPros);
//            put(mergeField, mergeValue);
//        }});

    }

    /**
     * 分用户权限检查
     */
    @Override
    public void updateNodeAuthCheck(JSONObject userAuth, Long nodeId, String fieldName, Object fieldValue) throws ParameterNotFoundException {

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
        updateRelationshipParaCheck(userAuth, relId, fieldName, fieldValue);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void updateRelationshipParaCheck(JSONObject userAuth, Long relId, String fieldName, Object fieldValue) throws ParameterNotFoundException {
        // 字段有效性检查
        checkField(fieldName);

        // 值有效性检查
        checkValue(fieldValue);
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void updateRelationshipAuthCheck(JSONObject userAuth, Long relId, String fieldName, Object fieldValue) throws ParameterNotFoundException {

    }

    /**
     * 删除节点的属性键
     *
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.publisher.remove.node.key", mode = WRITE)
    @Description("CALL olab.security.publisher.remove.node.key({node_id},{field_name}) YIELD node RETURN node")
    public Stream<MapResult> removeNodeKey(@Name("nodeId") Long nodeId, @Name("fieldName") String fieldName) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);
        removeNodeKeyParaCheck(userAuth, nodeId, fieldName);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void removeNodeKeyParaCheck(JSONObject userAuth, Long nodeId, String fieldName) throws ParameterNotFoundException {
        // 字段有效性检查
        checkField(fieldName);

    }

    /**
     * 分用户权限检查
     */
    @Override
    public void removeNodeKeyAuthCheck(JSONObject userAuth, Long nodeId, String fieldName) throws ParameterNotFoundException {

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
    @Description("CALL olab.security.publisher.remove.node.label({node_id},{label}) YIELD node RETURN node")
    public Stream<MapResult> removeNodeLabel(@Name("nodeId") Long nodeId, @Name("label") String label) {
        String user = securityContext.subject().username();
        JSONObject userAuth = UserAuthGet.auth(user, PUBLISHER_AUTH_JSON);
        removeNodeLabelParaCheck(userAuth, nodeId, label);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void removeNodeLabelParaCheck(JSONObject userAuth, Long nodeId, String label) throws ParameterNotFoundException {
        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
        JSONObject relTypeObj = UserAuthGet.relTypeObject(userAuth, "Person", "ACTED_IN", "Movie");

        // 标签有效性检查
//        checkLabelOrType(mergeRelType,relTypeObj.getJSONArray("invalid_values"));
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void removeNodeLabelAuthCheck(JSONObject userAuth, Long nodeId, String label) throws ParameterNotFoundException {

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
        removeRelationshipKeyParaCheck(userAuth, relId, fieldName);
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void removeRelationshipKeyParaCheck(JSONObject userAuth, Long relId, String fieldName) throws ParameterNotFoundException {
        // 字段有效性检查
        checkField(fieldName);
    }

    /**
     * 分用户权限检查
     */
    @Override
    public void removeRelationshipKeyAuthCheck(JSONObject userAuth, Long relId, String fieldName) throws ParameterNotFoundException {

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
    @Description("CALL olab.security.publisher.add.node.label({node_id},{label}) YIELD node RETURN node")
    public Stream<MapResult> addNodeLabel(@Name("nodeId") Long nodeId, @Name("label") String label) {
        return Stream.of(MapResult.empty());
    }

    /**
     * 分用户检查入参
     **/
    @Override
    public void addNodeLabelParaCheck(JSONObject userAuth, Long nodeId, String label) throws ParameterNotFoundException {
        // 属性的KEY、VALUE检查【VALUE值的标准配置类型检查】
        JSONObject nodeLabelObj = UserAuthGet.nodeLabelObject(userAuth, label);

        // 标签有效性检查
        checkLabelOrType(label, nodeLabelObj.getJSONArray("invalid_values"));

    }

    /**
     * 分用户权限检查
     */
    @Override
    public void addNodeLabelAuthCheck(JSONObject userAuth, Long nodeId, String label) throws ParameterNotFoundException {

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
        if (!Types.of(value).toString().equals(valueCheckType)) {
            throw new ParameterNotFoundException("the value is of the wrong type or the field does not have permission!");
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
            throw new ParameterNotFoundException("value validation failed!");
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
                .orElse(new JSONObject()).getJSONArray("invalid_values");
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
        if (Objects.isNull(invalidValues) ||invalidValues.contains(labelOrType)) {
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
    private void ifNotCanBuildNewProsThenError(JSONObject userAuth, String label,  List<String> prosKey) throws ParameterNotFoundException {
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
        return db.execute(ParaWrap.withParamMapping("CALL db.propertyKeys() YIELD propertyKey WHERE propertyKey=$value RETURN value", params.keySet()), params)
                .stream()
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
    private void ifAnyProsNotCanEditorThenError(JSONObject userAuth, String label,  List<String> keys) throws ParameterNotFoundException {
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
        boolean bool = db.execute(ParaWrap.withParamMapping("CALL db.labels() YIELD label WHERE label=$value RETURN value", params.keySet()), params)
                .stream()
                .findFirst()
                .isPresent();
        if (!bool) {
            throw new ParameterNotFoundException("label permission denied[No label editing permission]!");
        }
    }

}

