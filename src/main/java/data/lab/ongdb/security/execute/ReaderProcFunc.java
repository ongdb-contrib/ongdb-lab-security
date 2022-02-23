package data.lab.ongdb.security.execute;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import data.lab.ongdb.security.inter.ReaderProcFuncInter;
import data.lab.ongdb.security.result.MapResult;
import data.lab.ongdb.security.util.FileUtil;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.internal.kernel.api.security.SecurityContext;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.neo4j.procedure.Mode.READ;

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.execute
 * @Description: TODO
 * @date 2022/2/22 9:28
 */
public class ReaderProcFunc implements ReaderProcFuncInter {

    private static final String READER_AUTH_JSON = "reader_auth.json";

    @Context
    public GraphDatabaseService db;

    @Context
    public SecurityContext securityContext;

    /**
     * 通过分配给用户的QUERY ID执行查询语句
     * @param queryId :限制性只读用户可执行的查询ID【获取可执行查询ID：CALL olab.security.get() YIELD value RETURN value】
     * @return
     * @Description: TODO
     */
    @Override
    @Procedure(name = "olab.security.reader", mode = READ)
    @Description("CALL olab.security.reader({query-id}) YIELD value")
    public Stream<MapResult> query(@Name("queryId") String queryId) {

        String user = securityContext.subject().username();

        /*
         * 通过用户名和查询ID获取配置的查询
         * */

        if (Objects.nonNull(queryId) && !"".equals(queryId)) {
            String query = fetchQuery(user, queryId);

            if (Objects.nonNull(query) && !"".equals(query)) {
                Map<String, Object> params = Collections.emptyMap();
                return db.execute(withParamMapping(query, params.keySet()), params).stream().map(MapResult::new);
            }
        }
        return Stream.of(MapResult.empty());
    }

    public static String withParamMapping(String fragment, Collection<String> keys) {
        if (keys.isEmpty()) {
            return fragment;
        }
        String declaration = " WITH " + keys.stream().map(s -> format(" {`%s`} as `%s` ", s, s)).collect(Collectors.joining(", "));
        return declaration + fragment;
    }

    private String fetchQuery(String user, String queryId) {
        JSONObject jsonObject = FileUtil.readAuthList(READER_AUTH_JSON)
                .parallelStream()
                .filter(v -> {
                    JSONObject object = (JSONObject) v;
                    return user.equals(object.get("username"));
                })
                .map(v -> (JSONObject) v)
                .findFirst()
                .orElse(new JSONObject());
        JSONArray queries = jsonObject.getJSONArray("queries");
        if (Objects.nonNull(queries) && !queries.isEmpty()) {
            return queries
                    .parallelStream()
                    .filter(v -> {
                        JSONObject object = (JSONObject) v;
                        return queryId.equals(object.get("query_id"));
                    })
                    .map(v -> ((JSONObject) v).getString("query"))
                    .findFirst()
                    .orElse("");
        }
        return null;
    }

}



