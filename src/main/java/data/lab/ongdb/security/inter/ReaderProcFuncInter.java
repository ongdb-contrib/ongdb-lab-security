package data.lab.ongdb.security.inter;
/*
 *
 * Data Lab - graph database organization.
 *
 */

import data.lab.ongdb.security.result.MapResult;

import java.util.Map;
import java.util.stream.Stream;

/**
 * 数据读取
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.inter
 * @Description: TODO
 * @date 2022/2/22 9:25
 */
public interface ReaderProcFuncInter {

    /**
     * 通过分配给用户的QUERY ID执行查询语句
     * @param queryId:限制性只读用户可执行的查询ID【获取可执行查询ID：CALL olab.security.get() YIELD value RETURN value】
     * @param params:设置的参数
     * @return
     * @Description: TODO
     */
    Stream<MapResult> query(String queryId, Map<String,Object> params);

}

