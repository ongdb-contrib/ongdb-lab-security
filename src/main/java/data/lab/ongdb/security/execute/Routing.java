package data.lab.ongdb.security.execute;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import data.lab.ongdb.security.common.ParaWrap;
import data.lab.ongdb.security.result.MapResult;
import data.lab.ongdb.security.util.FileUtil;
import org.neo4j.cypher.ParameterNotFoundException;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.neo4j.procedure.Mode.WRITE;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.execute
 * @Description: TODO
 * @date 2022/3/2 15:02
 */
public class Routing {

    private static final String PUBLISHER_AUTH_JSON = "security_routing.access";

    @Context
    public GraphDatabaseService db;

    /**
     * 合并节点
     *
     * @param username   :标签名称
     * @param password   :合并时使用的字段名
     * @param writeQuery :合并时合并字段设置的值
     * @return
     * @Description: TODO
     */
    @Procedure(name = "routing", mode = WRITE)
    @Description("集群情况下使用：CALL routing({username},{password},{write_query}) YIELD value RETURN value")
    public Stream<MapResult> mergeNode(@Name("username") String username, @Name("password") String password, @Name("writeQuery") String writeQuery) {

        String coreUrl = FileUtil.readFile(FileUtil.PATH_DIR + File.separator + PUBLISHER_AUTH_JSON, FileUtil.ENCODING);
        String prefix = "bolt+routing";
        if (Objects.isNull(coreUrl) || "".equals(coreUrl)) {
            coreUrl = "localhost:7687";
            prefix = "bolt";
        }else {
            coreUrl = coreUrl.replace("\n","");
        }

        check("username", username);
        check("password", password);
        check("writeQuery", writeQuery);
        check("coreUrl", coreUrl);

        /*
         * 读取集群路由地址
         * */

        String query = "WITH $write_query AS query,$username AS user,$password AS pwd,$coreUrl AS url CALL apoc.bolt.execute(\"" + prefix + "://\"+user+\":\"+pwd+\"@\"+url+\"\",query, {}) YIELD row RETURN row";
        String finalCoreUrl = coreUrl;
        Map<String, Object> params = new HashMap<String, Object>() {{
            put("username", username);
            put("password", password);
            put("write_query", writeQuery);
            put("coreUrl", finalCoreUrl);
        }};
        return execute(query, params);
    }

    private void check(String key, String value) {
        if (Objects.isNull(value) || "".equals(value)) {
            throw new ParameterNotFoundException("parameter exception[" + key + "]!");
        }
    }

    /**
     * 执行QUERY
     **/
    private Stream<MapResult> execute(String query, Map<String, Object> params) {
        return db.execute(ParaWrap.withParamMapping(query, params.keySet()), params).stream().map(MapResult::new);
    }
}



