package data.lab.ongdb.security.inter;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import data.lab.ongdb.security.common.Operator;
import data.lab.ongdb.security.model.User;
import data.lab.ongdb.security.result.ListResult;
import data.lab.ongdb.security.result.Output;
import data.lab.ongdb.security.role.Publisher;
import data.lab.ongdb.security.role.Reader;
import org.neo4j.procedure.Admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 权限配置
 *
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.inter
 * @Description: TODO
 * @date 2022/2/21 17:08
 */
public interface AuthInter {

    /**
     * 配置Publisher权限【合并权限列表】【admin】
     *
     * @param username:用户名
     * @param nodeLabels:可操作的标签列表【可为空】
     * @param relTypes:可操作的关系类型列表【可为空】 start_label type end_label
     * @return 返回本次配置信息
     * @Description: TODO
     */
    @Admin
    Stream<Publisher> setPublisher(String username, List<Map<String, Object>> nodeLabels, List<Map<String, Object>> relTypes);

    /**
     * 配置Reader权限【合并权限列表】【admin】【query_id不可重复】
     *
     * @param username:用户名
     * @param queries:可执行查询列表 query_id role
     * @return
     * @Description: TODO
     */
    @Admin
    Stream<Reader> setReader(String username, List<Map<String, String>> queries);

    /**
     * 查看指定用户的权限列表【admin】
     *
     * @param username:用户名
     * @return 返回指定用户的配置信息
     * @Description: TODO
     */
    @Admin
    Stream<Output> fetchUserAuth(String username);

    /**
     * 重置指定用户的权限列表【admin】
     *
     * @param username:用户名
     * @return 返回本次配置信息
     * @Description: TODO
     */
    @Admin
    Stream<User> clear(String username);

    /**
     * 获取所有的已配置权限列表【admin】
     *
     * @return 返回本次配置信息
     * @Description: TODO
     */
    @Admin
    Stream<ListResult> list();

    /**
     * 根据用户名称信息获取权限列表
     *
     * @return 返回用户的配置信息
     * @Description: TODO
     */
    Stream<Output> get();

    /**
     * 获取权限说明列表
     *
     * @return 权限限制的说明
     * @Description: TODO
     */
    Stream<Operator> getAuth();

    /**
     * 可设置值的类型
     *
     * @return 可设置值的类型
     * @Description: TODO
     */
    Stream<Output> getValueTypes();
}








