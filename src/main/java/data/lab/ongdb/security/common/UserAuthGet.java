package data.lab.ongdb.security.common;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import com.alibaba.fastjson.JSONArray;
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
    public static JSONObject nodeLabelObject(JSONObject userAuth, String label) throws ParameterNotFoundException {
        if (Objects.isNull(label)) {
            throw new ParameterNotFoundException("Get user's label permission exception[label exception]!");
        }
        JSONObject obj = userAuth.getJSONArray("nodeLabels")
                .parallelStream()
                .filter(v -> {
                    JSONObject object = (JSONObject) v;
                    return label.equals(object.get("label"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject());
        if (!obj.isEmpty()) {
            return obj;
        } else {
            throw new ParameterNotFoundException("Get user's label permission exception!");
        }
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
    public static JSONObject relTypeObject(JSONObject userAuth, List<String> startLabel, String relationshipType, List<String> endLabel) throws ParameterNotFoundException {
        if (Objects.isNull(startLabel) || startLabel.isEmpty()) {
            throw new ParameterNotFoundException("Get user's relationshipType permission exception[start-labels exception]!");
        }
        if (Objects.isNull(endLabel) || endLabel.isEmpty()) {
            throw new ParameterNotFoundException("Get user's relationshipType permission exception[end-labels exception]!");
        }
        if (Objects.isNull(relationshipType)) {
            throw new ParameterNotFoundException("Get user's relationshipType permission exception[relationshipType exception]!");
        }
        JSONObject obj = userAuth.getJSONArray("relTypes")
                .parallelStream()
                .filter(v -> {
                    JSONObject object = (JSONObject) v;
                    return startLabel.contains(object.getString("start_label")) &&
                            relationshipType.equals(object.get("type")) &&
                            endLabel.contains(object.getString("end_label"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject());
        if (!obj.isEmpty()) {
            return obj;
        } else {
            throw new ParameterNotFoundException("Get user's relationshipType permission exception!");
        }
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
        if (!object.isEmpty()) {
            return getOperator(object.getString("operator"));
        } else {
            throw new ParameterNotFoundException("Get operator of label[" + label + "] exception!");
        }
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
                    if (!object.isEmpty()) {
                        return new HashMap<String, Operator>() {{
                            put(object.getString("field"), getOperator(object.getString("operator")));
                        }};
                    } else {
                        throw new ParameterNotFoundException("Get operator of properties exception!");
                    }
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
        if (Objects.nonNull(oper)) {
            Optional<Operator> operator = Arrays.asList(Operator.values())
                    .parallelStream()
                    .filter(v -> v.getOperate().equals(oper.toLowerCase()))
                    .findFirst();
            if (operator.isPresent()) {
                return operator.get();
            } else {
                throw new ParameterNotFoundException("Get operator permission exception!");
            }
        } else {
            throw new ParameterNotFoundException("Get operator permission exception!");
        }
    }

    /**
     * 从用户权限中获取，每个标签在某一个指定KEY上的权限
     *
     * @param userAuth:用户权限
     * @param labels:标签列表
     * @param key:属性KEY
     * @return
     * @Description: TODO
     */
    public static List<Operator> prosKeyOperator(JSONObject userAuth, List<String> labels, String key) {
        List<Operator> operatorList = new ArrayList<>();
        labels.parallelStream()
                .forEach(label -> {
                            Optional<Map<String, Operator>> operatorMap = prosOperator(userAuth, label).parallelStream()
                                    .filter(v -> v.containsKey(key))
                                    .findFirst();
                            if (operatorMap.isPresent()) {
                                operatorList.add(operatorMap.get().get(key));
                            } else {
                                throw new ParameterNotFoundException("Get operator of properties-key[" + key + "] exception!");
                            }
                        }
                );
        return operatorList;
    }

    /**
     * 从已经过滤的用户权限中获取，关系类型在KEYS上的权限
     *
     * @param object:标签列表
     * @param keys:属性KEYS
     * @return
     * @Description: TODO
     */
    public static List<Operator> prosKeyOperator(JSONObject object, Set<String> keys) {
        List<Map<String, Operator>> prosOperator = UserAuthGet.getProsOperator(object);
        if (Objects.nonNull(prosOperator) && Objects.nonNull(keys)) {
            return keys.parallelStream()
                    .map(v -> getOperator(prosOperator, v))
                    .collect(Collectors.toList());
        } else {
            throw new ParameterNotFoundException("Get operator of properties exception!");
        }
    }

    /**
     * 从列表中获取指定KEY的Operator
     *
     * @param prosOperator:属性值与Operator映射列表
     * @param key:属性键
     * @return
     * @Description: TODO
     */
    private static Operator getOperator(List<Map<String, Operator>> prosOperator, String key) {
        if (Objects.nonNull(key) && Objects.nonNull(prosOperator)) {
            Optional<Operator> operator = prosOperator
                    .parallelStream()
                    .filter(v -> v.containsKey(key)).map(v -> {
                                Optional<Operator> optionalOperator = v.values().parallelStream().findFirst();
                                if (optionalOperator.isPresent()) {
                                    return optionalOperator.get();
                                } else {
                                    throw new ParameterNotFoundException("Get operator permission exception!");
                                }
                            }
                    )
                    .findFirst();
            if (operator.isPresent()) {
                return operator.get();
            } else {
                throw new ParameterNotFoundException("Get operator permission exception!");
            }
        } else {
            throw new ParameterNotFoundException("Get operator permission exception!");
        }
    }

    /**
     * 获取关系类型的权限
     *
     * @param
     * @return
     * @Description: TODO
     */
    public static Operator typeOperator(JSONObject userAuth, List<String> startLabels, String mergeRelType, List<String> endLabels) {
        JSONObject object = relTypeObject(userAuth, startLabels, mergeRelType, endLabels);
        if (!object.isEmpty()) {
            return getOperator(object.getString("operator"));
        } else {
            throw new ParameterNotFoundException("Get operator of relationshipType[" + mergeRelType + "] exception!");
        }
    }

    /**
     * 从节点或者关系对象中获取属性操作权限的封装
     *
     * @param jsonObject:标签或者关系的对象
     * @return
     * @Description: TODO
     */
    public static List<Map<String, Operator>> getProsOperator(JSONObject jsonObject) {
        if (Objects.nonNull(jsonObject)) {
            return jsonObject
                    .getJSONArray("properties")
                    .parallelStream()
                    .map(v -> {
                        JSONObject object = (JSONObject) v;
                        if (!object.isEmpty()) {
                            return new HashMap<String, Operator>() {{
                                put(object.getString("field"), getOperator(object.getString("operator")));
                            }};
                        } else {
                            throw new ParameterNotFoundException("Get operator of properties exception!");
                        }
                    }).collect(Collectors.toList());
        } else {
            throw new ParameterNotFoundException("Get operator of properties exception!");
        }
    }

    /**
     * 判断用户在标签上是否有权限
     *
     * @param
     * @return
     * @Description: TODO
     */
    public static boolean isContainslabelOperator(JSONObject userAuth, String label) {
        JSONArray obj = userAuth.getJSONArray("nodeLabels");
        if (!obj.isEmpty()) {
            return obj.parallelStream()
                    .filter(v -> {
                        JSONObject object = (JSONObject) v;
                        return object.getString("label").equals(label);
                    })
                    .map(v -> {
                        JSONObject object = (JSONObject) v;
                        return object.getString("label");
                    })
                    .collect(Collectors.toList())
                    .contains(label);
        } else {
            return false;
        }
    }
}


