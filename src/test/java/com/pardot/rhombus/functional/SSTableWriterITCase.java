package com.pardot.rhombus.functional;

import com.datastax.driver.core.utils.UUIDs;
import com.pardot.rhombus.ConnectionManager;
import com.pardot.rhombus.ObjectMapper;
import com.pardot.rhombus.cobject.CDefinition;
import com.pardot.rhombus.cobject.CIndex;
import com.pardot.rhombus.cobject.CKeyspaceDefinition;
import com.pardot.rhombus.cobject.CObjectCQLGenerator;
import com.pardot.rhombus.cobject.shardingstrategy.ShardingStrategyMonthly;
import com.pardot.rhombus.util.JsonUtil;
import org.apache.cassandra.io.util.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

import static org.junit.Assert.*;

public class SSTableWriterITCase extends RhombusFunctionalTest {

    private static Logger logger = LoggerFactory.getLogger(ObjectMapperUpdateITCase.class);


    @Test
    public void testInsertingAllNonNullValuesInSchema_simple() throws Exception {
        logger.debug("Starting testInsertingAllNonNullValuesInSchema");
        System.setProperty("cassandra.config", "cassandra-config/cassandra.yaml");

        //Build the connection manager
        ConnectionManager cm = getConnectionManager();

        //Build our keyspace definition object
        CKeyspaceDefinition keyspaceDefinition = JsonUtil.objectFromJsonResource(CKeyspaceDefinition.class, this.getClass().getClassLoader(), "TableWriterSimpleKeyspace.js");
        assertNotNull(keyspaceDefinition);
        String keyspaceName = keyspaceDefinition.getName();
        ShardingStrategyMonthly shardStrategy = new ShardingStrategyMonthly();

        // Make sure the SSTableOutput directory exists and is clear
        File keyspaceDir = new File(keyspaceName);
        if (keyspaceDir.exists()) {
            FileUtils.deleteRecursive(new File(keyspaceName));
        }
        assertTrue(new File(keyspaceName).mkdir());

        //Rebuild the keyspace and get the object mapper
        cm.buildKeyspace(keyspaceDefinition, true);
        logger.debug("Built keyspace: {}", keyspaceDefinition.getName());
        cm.setDefaultKeyspace(keyspaceDefinition);
        ObjectMapper om = cm.getObjectMapper();
        om.setLogCql(true);

        // This is the only static table definition this test keyspace has
        List<String> staticTableNames = Arrays.asList("simple");
        Map<String, CIndex> indexes = new HashMap<String, CIndex>();
        Map<String, String> indexTableToStaticTableName = new HashMap<String, String>();
        // Pull the definition and associated indexes for each static table
        for (String staticTableName : staticTableNames) {
            CDefinition definition = keyspaceDefinition.getDefinitions().get(staticTableName);
            List<CIndex> indexDefinitions = definition.getIndexesAsList();
            for (CIndex index : indexDefinitions) {
                String indexTableName = CObjectCQLGenerator.makeTableName(definition, index);
                indexes.put(indexTableName, index);
                indexTableToStaticTableName.put(indexTableName, staticTableName);
            }
        }

        //Insert our test data into the SSTable
        // For this test, all this data goes into the one table we have defined
        List<Map<String, Object>> values = JsonUtil.rhombusMapFromResource(this.getClass().getClassLoader(), "SSTableWriterSimpleTestData.js");
        // Tack on time based UUIDs because we don't really care what the UUID values are
        for (Map<String, Object> map : values) {
            map.put("id", UUIDs.startOf(Long.parseLong(map.get("created_at").toString(), 10)));
        }
        // Build the map to insert that we'll actually pass in
        Map<String, List<Map<String, Object>>> insert = new HashMap<String, List<Map<String, Object>>>();
        for (String staticTableName : staticTableNames) {
            insert.put(staticTableName, values);
        }
        // Add in shardId for index tables
        for (Map<String, Object> map : values) {
            map.put("shardid", shardStrategy.getShardKey(Long.parseLong(map.get("created_at").toString(), 10)));
        }
        for (String indexTableName : indexes.keySet()) {
            insert.put(indexTableName, values);
        }
        // Actually insert the data into the SSTableWriters
        om.insertIntoSSTable(insert, indexes, indexTableToStaticTableName);
        assertTrue(om.completeSSTableWrites());

        // Make a list of all table names so we can load them into Cassandra
        List<String> allTableNames = new ArrayList<String>(indexes.keySet());
        allTableNames.addAll(staticTableNames);
        for (String tableName : allTableNames) {
            String SSTablePath = keyspaceName + "/" + tableName;

            // Load the SSTables into Cassandra
            ProcessBuilder builder = new ProcessBuilder("sstableloader", "-d", "localhost", SSTablePath);
            builder.redirectErrorStream(true);
            Process p = builder.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            // TODO: sleep is the devil
            while (!r.readLine().contains("100%")) {
                Thread.sleep(100);
            }
        }

        String staticTableName = staticTableNames.get(0);
        for (Map<String, Object> expected : values) {
            Map<String, Object> actual = om.getByKey(staticTableName, expected.get("id"));
            actual.put("shardid", shardStrategy.getShardKey(Long.parseLong(actual.get("created_at").toString(), 10)));
            assertEquals(expected, actual);
        }

        // Clean up the SSTable directories after ourselves
        FileUtils.deleteRecursive(new File(keyspaceName));
    }
}