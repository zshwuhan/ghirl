/**
 * 
 */
package ghirl.test.tdd;

import static org.junit.Assert.*;

import java.io.File;

import ghirl.graph.Closable;
import ghirl.graph.MutableGraph;
import ghirl.graph.PersistantGraph;
import ghirl.graph.PersistantGraphHexastore;
import ghirl.test.verify.TestTextGraph;
import ghirl.util.Config;
import ghirl.util.FilesystemUtil;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author katie
 *
 */
public class TestPersistantGraphHexastore extends TestTextGraph { 
	private static final Logger logger = Logger.getLogger(TestPersistantGraphHexastore.class);
	protected static String DBDIR = "tests";
	protected static String DBNAME = "testPersistantGraphHexastore";
	protected static String CLEANUPDIR = "tests/testPersistantGraphHexastore";
	
	public static File testhome;
	@BeforeClass
	public static void setAllUp() {
		logger.info("*********** SETTING UP...");
		setUpLogger();
		Config.setProperty("ghirl.dbDir", DBDIR);
		testhome = new File(CLEANUPDIR);
		if (testhome.exists()) {
			FilesystemUtil.rm_r(testhome);
			fail("Test home wasn't cleaned up -- run again.");
		}
		testhome.mkdir();
	}
	@AfterClass
	public static void tearAllDown() {
		logger.info("*********** TEARING DOWN...");
		FilesystemUtil.rm_r(testhome);
	}
	

	@Test @Override
	public void writeableTest() {
		logger.info("*************************** STARTING W-MODE TEST");
		graph = new PersistantGraphHexastore(DBNAME,'w');
		loadGraphGhirl((MutableGraph) graph);
		((MutableGraph)graph).freeze();
		((Closable)graph).close();
		graph = new PersistantGraphHexastore(DBNAME,'r');
	}
	

	@Test @Override
	public void appendableTest() {
		logger.info("*************************** STARTING A-MODE TEST");
		graph = new PersistantGraphHexastore(DBNAME,'a');
		((Closable)graph).close();
		graph = new PersistantGraphHexastore(DBNAME,'r');
	}
	
	@Test @Override
	public void readableTest() {
		logger.info("*************************** STARTING R-MODE TEST");
		graph = new PersistantGraphHexastore(DBNAME,'r');
	}
	
	@Test @Override
	public void overwriteTest() {
		logger.info("*************************** STARTING OVERWRITE TEST");
		graph = new PersistantGraphHexastore(DBNAME,'w');
		loadGraphGhirl((MutableGraph) graph);
		((MutableGraph)graph).freeze();
		((Closable)graph).close();
		graph = new PersistantGraphHexastore(DBNAME,'r');
	}
	

	@Test @Override @Ignore
	public void verifyContents() {
		logger.info("*************************** VERIFYING GRAPH CONTENTS");
		graph = new PersistantGraphHexastore(DBNAME,'w');
		// check that it starts empty
		assertEquals("Overwritten graph should be empty:",0,getIteratorNodecount(graph));
		((MutableGraph)graph).melt();
		loadGraphGhirl((MutableGraph) graph);
		((MutableGraph)graph).freeze();
		
		assertEquals("Nodes written:", 3, getIteratorNodecount(graph));
		assertEquals("Nodes written:", 3, getOrderedNodecount(graph));
		assertEquals("Edges labels used:", 2, graph.getOrderedEdgeLabels().length);
		
		((Closable)graph).close();
		graph = new PersistantGraphHexastore(DBNAME,'r');
	}
	
	@Test @Override @Ignore
	public void verifyContentsInDetail() {}
}
