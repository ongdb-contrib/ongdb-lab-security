package data.lab.ongdb.security.inter;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import data.lab.ongdb.security.result.MapResult;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.inter
 * @Description: TODO
 * @date 2022/2/21 17:29
 */
public interface PublisherProcFuncInter {

    /**
     * 合并节点
     *
     * @param label:标签名称
     * @param mergeField:合并时使用的字段名
     * @param mergeValue:合并时合并字段设置的值
     * @param otherPros:其它需要设置的节点属性信息
     * @param otherLabels:其它需要设置的标签信息
     * @return
     * @Description: TODO
     */
    Stream<Node> mergeNode(String label, String mergeField, String mergeValue, Map<String, Object> otherPros, List<String> otherLabels);

    /**
     * 删除节点
     *
     * @param nodeId:节点ID
     * @return
     * @Description: TODO
     */
    Stream<MapResult> deleteNode(Long nodeId);

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
    Stream<Path> mergeRelationship(Long startId, Long endId, String mergeRelType, Map<String, Object> relPros);

    /**
     * 删除关系
     *
     * @param relId:关系ID
     * @return
     * @Description: TODO
     */
    Stream<MapResult> deleteRelationship(Long relId);

    /**
     * 修改节点的属性值
     *
     * @param nodeId:节点ID
     * @param fieldName:字段名
     * @param fieldValue:字段值
     * @return
     * @Description: TODO
     */
    Stream<Node> updateNode(Long nodeId, String fieldName, Object fieldValue);

    /**
     * 修改关系的属性值
     *
     * @param relId:关系ID
     * @param fieldName:字段名
     * @param fieldValue:字段值
     * @return
     * @Description: TODO
     */
    Stream<Path> updateRelationship(Long relId, String fieldName, Object fieldValue);

    /**
     * 删除节点的属性键
     *
     * @param
     * @return
     * @Description: TODO
     */
    Stream<Node> removeNodeKey(Long nodeId, String fieldName);

    /**
     * 删除节点的某个标签
     *
     * @param
     * @return
     * @Description: TODO
     */
    Stream<Node> removeNodeLabel(Long nodeId, String label);

    /**
     * 删除关系的属性键
     * @param
     * @return
     * @Description: TODO
     */
    Stream<Path> removeRelationshipKey(Long relId, String fieldName);
}


