package data.lab.ongdb.security.common;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import com.alibaba.fastjson.JSONObject;
import data.lab.ongdb.security.util.FileUtil;
import org.neo4j.cypher.ParameterNotFoundException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.common
 * @Description: TODO
 * @date 2022/2/24 10:03
 */
public class UserAuthGet {

    /**
     * 获取用户权限
     *
     * @param user:用户名
     * @param filename:权限文件的文件名
     * @return
     * @Description: TODO
     */
    public static JSONObject auth(String user, String filename) throws ParameterNotFoundException {
        JSONObject re = FileUtil.readAuthList(filename)
                .parallelStream()
                .filter(v -> {
                    JSONObject object = (JSONObject) v;
                    return user.equals(object.get("username"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject());
        if (re.isEmpty()) {
            throw new ParameterNotFoundException("Get user permission exception!");
        }
        return re;
    }

    /**
     * 获取给用户指定的标签权限
     *
     * @param userAuth:过滤出的用户权限
     * @param label:标签
     * @return
     * @Description: TODO
     */
    public static JSONObject nodeLabelObject(JSONObject userAuth, String label) {
        return userAuth.getJSONArray("nodeLabels")
                .parallelStream()
                .filter(v -> {
                    JSONObject object = (JSONObject) v;
                    return label.equals(object.get("label"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject());
    }

    /**
     * 获取给用户指定的关系权限
     *
     * @param userAuth:过滤出的用户权限
     * @param startLabel:from节点的标签
     * @param relationshipType:关系类型
     * @param endLabel:to节点的标签
     * @return
     * @Description: TODO
     */
    public static JSONObject relTypeObject(JSONObject userAuth, String startLabel, String relationshipType, String endLabel) {
        return userAuth.getJSONArray("relTypes")
                .parallelStream()
                .filter(v -> {
                    JSONObject object = (JSONObject) v;
                    return startLabel.equals(object.get("start_label")) &&
                            relationshipType.equals(object.get("type")) &&
                            endLabel.equals(object.get("end_label"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject());
    }

    /**
     * 获取给用户指定的标签权限操作类型
     *
     * @param userAuth:过滤出的用户权限
     * @param label:标签
     * @return
     * @Description: TODO
     */
    public static Operator labelOperator(JSONObject userAuth, String label) throws ParameterNotFoundException {
        JSONObject object = nodeLabelObject(userAuth, label);
        return getOperator(object.getString("operator"));
    }

    /**
     * 获取给用户指定的属性权限操作类型
     *
     * @param userAuth:过滤出的用户权限
     * @param label:标签
     * @return
     * @Description: TODO
     */
    public static List<Map<String, Operator>> prosOperator(JSONObject userAuth, String label) throws ParameterNotFoundException {
        return nodeLabelObject(userAuth, label)
                .getJSONArray("properties")
                .parallelStream()
                .map(v -> {
                    JSONObject object = (JSONObject) v;
                    return new HashMap<String, Operator>() {{
                        put(object.getString("field"), getOperator(object.getString("operator")));
                    }};
                }).collect(Collectors.toList());

    }

    /**
     * 通过操作类型获取`Operator`枚举的操作算子
     *
     * @param oper:操作类型
     * @return
     * @Description: TODO
     */
    private static Operator getOperator(String oper) {
        Optional<Operator> operator = Arrays.asList(Operator.values())
                .parallelStream()
                .filter(v -> v.getOperate().equals(oper.toLowerCase()))
                .findFirst();
        if (operator.isPresent()) {
            return operator.get();
        } else {
            throw new ParameterNotFoundException("Get the operator type of the label permission specified for the user!");
        }
    }

}

