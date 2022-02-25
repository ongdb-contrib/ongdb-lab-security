package data.lab.ongdb.security.execute;

import org.junit.After;
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
 * @date 2022/2/24 10:36
 */
public class PublisherProcFuncTest {

    @Rule
    public Neo4jRule NEO4J_PROC = new Neo4jRule().withProcedure(PublisherProcFunc.class);

    private GraphDatabaseService DB_PROC;

    @Before
    public void setUp() throws Exception {
        DB_PROC = NEO4J_PROC.getGraphDatabaseService();
    }

    @After
    public void tearDown() throws Exception {
        DB_PROC.shutdown();
    }

    @Test
    public void mergeNode() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.merge.node('Person','name','002',{}) YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void deleteNode() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.delete.node(12) YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void mergeRelationship() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.merge.relationship(3,5,'DIRECTED',{date:'2012'}) YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void deleteRelationship() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.delete.relationship(13) YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void updateNode() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.update.node(23,'code','003') YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void updateRelationship() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.update.relationship(45,'date','2022') YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void removeNodeKey() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.remove.node.key(23,'code') YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void removeNodeLabel() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.remove.node.label(67,'Actor') YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void removeRelationshipKey() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.remove.relationship.key(34,'name') YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }

    @Test
    public void addNodeLabel() {
        try (Transaction tx = DB_PROC.beginTx()) {
            Result res = DB_PROC.execute("CALL olab.security.publisher.add.node.label(1,'Person') YIELD value RETURN value");
            System.out.println(res.resultAsString());
        }
    }
}

