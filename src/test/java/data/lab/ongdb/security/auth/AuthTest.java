package data.lab.ongdb.security.auth;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;
import org.neo4j.harness.junit.Neo4jRule;

/*
 *
 * Data Lab - graph database organization.
 *
 */

/**
 * @author Yc-Ma
 * @PACKAGE_NAME: data.lab.ongdb.security.auth
 * @Description: TODO
 * @date 2022/2/22 10:48
 */
public class AuthTest {

    @Rule
    public Neo4jRule NEO4J_PROC = new Neo4jRule().withProcedure(Auth.class);

    private GraphDatabaseService DB_PROC;

    @Before
    public void setUp() throws Exception {
        DB_PROC = NEO4J_PROC.getGraphDatabaseService();
    }

    @Test
    public void setPublisher() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.setPublisher('publisher-1',[{label:'Person',properties:[{field:'name',operator:'DELETER_RESTRICT',check:'STRING'}],operator:'EDITOR'}],[{start_label:'Person',type:'ACTED_IN',end_label:'Movie',operator:'DELETER_RESTRICT',properties:[{field:'date',operator:'PUBLISHER',check:'LONG'}]}]) YIELD username,currentRole,nodeLabels,relTypes RETURN username,currentRole,nodeLabels,relTypes");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void setReader() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.setReader('reader',[{query_id:'query001',query:'MATCH (n) RETURN n.name AS name LIMIT 1'}]) YIELD username,currentRole,queries RETURN username,currentRole,queries");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void clear() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.clear('reader') YIELD username,currentRole RETURN username,currentRole");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void list() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.list() YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void get() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.get() YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void getAuth() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.getAuth() YIELD operate,level,description RETURN operate,level,description");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void getValueTypes() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.getValueTypes() YIELD value RETURN value");
//            while (res.hasNext()){
//                System.out.println(res.next().get("value"));
//            }
        }
    }
}




