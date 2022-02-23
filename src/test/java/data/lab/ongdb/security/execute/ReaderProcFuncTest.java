package data.lab.ongdb.security.execute;

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
 * @PACKAGE_NAME: data.lab.ongdb.security.execute
 * @Description: TODO
 * @date 2022/2/22 18:33
 */
public class ReaderProcFuncTest {

    @Rule
    public Neo4jRule NEO4J_PROC = new Neo4jRule().withProcedure(ReaderProcFunc.class);

    private GraphDatabaseService DB_PROC;

    @Before
    public void setUp() throws Exception {
        DB_PROC = NEO4J_PROC.getGraphDatabaseService();
    }

    @Test
    public void query() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.reader('query001') YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }
}

