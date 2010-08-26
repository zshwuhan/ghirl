package ghirl.test.verify;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import ghirl.graph.Closable;
import ghirl.graph.CompactGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphFactory;
import ghirl.graph.GraphId;
import ghirl.graph.MutableGraph;
import ghirl.graph.PersistantCompactTokyoGraph;
import ghirl.graph.SparseCompactGraph;
import ghirl.graph.TextGraph;
import ghirl.test.GoldStandard;
import ghirl.util.Config;
import ghirl.util.Distribution;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import tokyocabinet.Util;
import static org.junit.Assert.*;

public class TestPersistantCompactTokyoGraph {
	protected static String DBNAME = "testPersistantSparseCompactGraph";
	protected static String TESTROOT = "tests";
	protected static String COMPACTSRC = "tests/TestCompactTextGraph";
	
	protected static Graph graph;
	@BeforeClass
	public static void setUp() throws Exception {
		ghirl.util.Config.setProperty(Config.DBDIR,TESTROOT);
		graph = load();
	}

	@AfterClass
	public static void shutDown() {
		if (graph != null) ((Closable)graph).close();
	}
	
	@Test
	public void testContains() throws Exception {
		assertTrue("Contains $william",graph.contains(GraphId.fromString("$william")));
	}
	
	@Test
	public void testQD() {
		Distribution d = graph.asQueryDistribution("william");
		assertTrue("Should have query distribution for william",d != null && d.size() > 0);
	}
	
	@Test
	public void testNodeIterator() {
		for (Iterator<GraphId> it=graph.getNodeIterator(); it.hasNext();) {
			GraphId node = it.next();
			assertTrue("Must contain claimed node "+node.toString(),graph.contains(node));
		}
	}
	
	@Test
	public void testEdgeLabels() {
		Set<String> s = (Set<String>) graph.getEdgeLabels(GraphId.fromString("$william"));
		for(String link : s)  {
			System.out.println(link);
			assertNotNull(link);
		}
	}
	
	@Test
	public void testDistributionStorage() {
		GraphId g = GraphId.fromString("$william");
		Set<String> s = graph.getEdgeLabels(g);
		
		Distribution d = graph.walk1(g);
	}
	
	
	public static Graph load() throws Exception {
		Logger.getRootLogger().setLevel(Level.INFO);
		TextGraph graph = (TextGraph) GraphFactory.makeGraph("-bshgraph","tests/compact-persistant-sparse-loader.bsh");
		
		((MutableGraph)graph).freeze();
		((TextGraph)graph).close();
		
		File link, node, walk, size;
		link = new File(COMPACTSRC+File.separator+"graphLink.pct");
		node = new File(COMPACTSRC+File.separator+"graphNode.pct");
		walk = new File(COMPACTSRC+File.separator+"graphRow.pct");
		size = new File(COMPACTSRC+File.separator+"graphSize.pct");
		
		PersistantCompactTokyoGraph cgraph = new PersistantCompactTokyoGraph(DBNAME,'w');
		cgraph.load(size, link, node, walk);
		graph = new TextGraph(DBNAME, cgraph);
		return graph;
	}	
	/**
	 * Test method for {@link ghirl.graph.CompactGraph#load(java.io.File, java.io.File, java.io.File, java.io.File)}.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void testLoad() throws Exception {
		GoldStandard gold = new GoldStandard(GoldStandard.GRAPH);
		gold.assertMatchesGoldStandard(gold.queryGraph(graph));
	}
}