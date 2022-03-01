package data.lab.ongdb.security.inter;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import com.alibaba.fastjson.JSONObject;
import data.lab.ongdb.security.result.MapResult;
import org.neo4j.cypher.ParameterNotFoundException;

import java.util.Map;
import java.util.stream.Stream;

/**
 * 数据修改
 *
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.inter
 * @Description: TODO
 * @date 2022/2/21 17:29
 */
public interface PublisherProcFuncInter {

    /*
      核心步骤：
      1、定义入口
      2、定义入参检查接口
      3、定义权限检查接口
      **/

    /**
     * 合并节点
     *
     * @param label:标签名称
     * @param mergeField:合并时使用的字段名
     * @param mergeValue:合并时合并字段设置的值
     * @param otherPros:其它需要设置的节点属性信息
     * @return
     * @Description: TODO
     */
    Stream<MapResult> mergeNode(String label, String mergeField, Object mergeValue, Map<String, Object> otherPros);

    /**
     * 分用户检查入参
     **/
    void mergeNodeParaCheck(JSONObject userAuth, String label, String mergeField, Object mergeValue, Map<String, Object> otherPros) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void mergeNodeAuthCheck(JSONObject userAuth, String label, String mergeField, Object mergeValue, Map<String, Object> otherPros) throws ParameterNotFoundException;

    /**
     * 删除节点
     *
     * @param nodeId:节点ID
     * @return
     * @Description: TODO
     */
    Stream<MapResult> deleteNode(Long nodeId);

    /**
     * 分用户权限检查
     **/
    void deleteNodeAuthCheck(JSONObject userAuth, Long nodeId) throws ParameterNotFoundException;

    /**
     * 合并关系
     *
     * @param startId:from节点ID
     * @param endId:to节点ID
     * @param mergeRelType:合并的关系类型
     * @param relPros:需要给关系设置的属性信息
     * @return
     * @Description: TODO
     */
    Stream<MapResult> mergeRelationship(Long startId, Long endId, String mergeRelType, Map<String, Object> relPros);

    /**
     * 分用户检查入参
     **/
    void mergeRelationshipParaCheck(JSONObject userAuth, Long startId, Long endId, String mergeRelType, Map<String, Object> relPros) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void mergeRelationshipAuthCheck(JSONObject userAuth, Long startId, Long endId, String mergeRelType, Map<String, Object> relPros) throws ParameterNotFoundException;

    /**
     * 删除关系
     *
     * @param relId:关系ID
     * @return
     * @Description: TODO
     */
    Stream<MapResult> deleteRelationship(Long relId);

    /**
     * 分用户权限检查
     **/
    void deleteRelationshipAuthCheck(JSONObject userAuth, Long relId) throws ParameterNotFoundException;

    /**
     * 修改节点的属性值
     *
     * @param nodeId:节点ID
     * @param fieldName:字段名
     * @param fieldValue:字段值
     * @return
     * @Description: TODO
     */
    Stream<MapResult> updateNode(Long nodeId, String fieldName, Object fieldValue);

    /**
     * 分用户检查入参
     **/
    void updateNodeParaCheck(JSONObject userAuth, Long nodeId, String fieldName, Object fieldValue) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void updateNodeAuthCheck(JSONObject userAuth, Long nodeId, String fieldName, Object fieldValue) throws ParameterNotFoundException;

    /**
     * 修改关系的属性值
     *
     * @param relId:关系ID
     * @param fieldName:字段名
     * @param fieldValue:字段值
     * @return
     * @Description: TODO
     */
    Stream<MapResult> updateRelationship(Long relId, String fieldName, Object fieldValue);

    /**
     * 分用户检查入参
     **/
    void updateRelationshipParaCheck(JSONObject userAuth, Long relId, String fieldName, Object fieldValue) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void updateRelationshipAuthCheck(JSONObject userAuth, Long relId, String fieldName, Object fieldValue) throws ParameterNotFoundException;

    /**
     * 删除节点的属性键
     *
     * @param
     * @return
     * @Description: TODO
     */
    Stream<MapResult> removeNodeKey(Long nodeId, String fieldName);

    /**
     * 分用户检查入参
     **/
    void removeNodeKeyParaCheck(JSONObject userAuth, Long nodeId, String fieldName) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void removeNodeKeyAuthCheck(JSONObject userAuth, Long nodeId, String fieldName) throws ParameterNotFoundException;

    /**
     * 删除节点的某个标签
     *
     * @param
     * @return
     * @Description: TODO
     */
    Stream<MapResult> removeNodeLabel(Long nodeId, String label);

    /**
     * 分用户检查入参
     **/
    void removeNodeLabelParaCheck(JSONObject userAuth, String label) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void removeNodeLabelAuthCheck(JSONObject userAuth, String label) throws ParameterNotFoundException;

    /**
     * 删除关系的属性键
     *
     * @param
     * @return
     * @Description: TODO
     */
    Stream<MapResult> removeRelationshipKey(Long relId, String fieldName);

    /**
     * 分用户检查入参
     **/
    void removeRelationshipKeyParaCheck(String fieldName) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void removeRelationshipKeyAuthCheck(JSONObject userAuth, Long relId, String fieldName) throws ParameterNotFoundException;

    /**
     * 增加节点的标签
     *
     * @param nodeId:节点ID
     * @param label:标签
     * @return
     * @Description: TODO
     */
    Stream<MapResult> addNodeLabel(Long nodeId, String label);

    /**
     * 分用户检查入参
     **/
    void addNodeLabelParaCheck(JSONObject userAuth, String label) throws ParameterNotFoundException;

    /**
     * 分用户权限检查
     **/
    void addNodeLabelAuthCheck(JSONObject userAuth, String label) throws ParameterNotFoundException;

}


