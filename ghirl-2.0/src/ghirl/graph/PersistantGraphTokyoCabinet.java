package ghirl.graph;

import ghirl.util.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import tokyocabinet.BDB;
import tokyocabinet.BDBCUR;

public class PersistantGraphTokyoCabinet extends PersistantGraph {
	private static final HashMap<Character,Integer> MODES = new HashMap<Character,Integer>();
	public static final char
		MODE_READ = 'r',
		MODE_WRITE= 'w',
		MODE_APPEND='a';
	private static final Logger logger = Logger.getLogger(PersistantGraphTokyoCabinet.class);
	static {
		MODES.put(MODE_READ,   BDB.OREADER);
		MODES.put(MODE_WRITE,  BDB.OWRITER | BDB.OTRUNC | BDB.OCREAT);
		MODES.put(MODE_APPEND, BDB.OWRITER | BDB.OCREAT);
	}
	protected BDB nodeNames;
	protected BDB nodeProps;
	protected BDB edgeDestinations;
	protected BDB nodeEdges;
	protected List<BDB> dbs;
	protected char mode;
	protected static final String
		NAME_NODENAMES = "_nodes",
		NAME_NODEPROPS = "_props",
		NAME_EDGEDESTS = "_dests",
		NAME_NODEEDGES = "_edges";
		
	
	public PersistantGraphTokyoCabinet(String dbName,char mode) throws IOException {
		this.mode= mode;
		String fqpath = Config.getProperty(Config.DBDIR)
		+ File.separator
		+ dbName
		+ File.separator;
		File dbdir = new File(fqpath);
		if (!dbdir.exists()) dbdir.mkdir();
		int imode = MODES.get(mode);
		dbs = new ArrayList<BDB>();
		nodeNames = initDB(fqpath+NAME_NODENAMES, imode); dbs.add(nodeNames);
		nodeProps = initDB(fqpath+NAME_NODEPROPS, imode); dbs.add(nodeProps);
		edgeDestinations = initDB(fqpath+NAME_EDGEDESTS, imode); dbs.add(edgeDestinations);
		nodeEdges = initDB(fqpath+NAME_NODEEDGES, imode); dbs.add(nodeEdges);
	}
	
	protected BDB initDB(String fqpath, int mode) throws IOException {
		BDB db;
		try {
			db = new BDB();
			logger.debug("Opening Tokyo Cabinet at "+fqpath+" in mode "+mode);
			if(db.open(fqpath,mode)) return db;
			int code = db.ecode();
			switch(code) {
			case BDB.ENOFILE:
				throw new FileNotFoundException("Couldn't open database at "+fqpath+": "+BDB.errmsg(db.ecode()));
			case BDB.ENOPERM:
				throw new IOException("Permission denied on "+fqpath);
			}
			throw new IllegalStateException("Couldn't open database at "+fqpath+": "+BDB.errmsg(db.ecode()));
		} catch (UnsatisfiedLinkError e) {
			throw new IllegalStateException("Tokyo Cabinet requires \"-Djava.library.path=usr/local/lib\""
					+"(or correct path to tokyocabinet.dylib) on command line or in VM arguments.",e);			
		}
	}
	
	@Override
	public void addEdge(String linkLabel, GraphId from, GraphId to) {
		boolean ed = edgeDestinations.putdup(makeKey(from,linkLabel), makeKey(to));
		boolean ne = nodeEdges.putdup(makeKey(from), linkLabel);
		if (!ed) logger.error("Problem adding edge "+linkLabel+" "+from.toString()+" "+to.toString()+": "
				+BDB.errmsg(edgeDestinations.ecode()));
		if (!ne) logger.error("Problem adding edge "+linkLabel+" "+from.toString()+" "+to.toString()+": "
				+BDB.errmsg(nodeEdges.ecode()));
	}

	@Override
	public void freeze() {
		if (!this.isFrozen && this.mode != MODE_READ) {
			for (BDB db: dbs) {
				if (!db.sync())
					logger.error("EXCEPTION NEEDED: problem syncing Tokyo Cabinet: "+BDB.errmsg(db.ecode()));
			}
		}
		super.freeze();
	}
	
	@Override
	public void close() {
		for (BDB db: dbs) {
			if (!db.close())
				logger.error("Problem closing database: "+BDB.errmsg(nodeNames.ecode()));
		}
	}

	@Override
	public boolean contains(GraphId id) {
		return (nodeNames.get(makeKey(id)) != null);
	}

	@Override
	public GraphId createNode(String flavor, String shortName) {
		GraphId node = new GraphId(flavor,shortName);
		nodeNames.putdup(makeKey(node),node.toString());
		return node;
	}

	@Override
	public Set<GraphId> followLink(GraphId from, String linkLabel) {
		List<String> el = (List<String>) edgeDestinations.getlist(makeKey(from,linkLabel));
		if (el == null) return Collections.EMPTY_SET;
		HashSet<GraphId> result = new HashSet<GraphId>();
		for (String node : el) {
			result.add(GraphId.fromString(node));
		}
		return result;
	}

	@Override
	public Set<String> getEdgeLabels(GraphId from) {
		List el = nodeEdges.getlist(makeKey(from));
		if (el != null) return new HashSet(el);
		return Collections.EMPTY_SET;
	}

	@Override
	public GraphId getNodeId(String flavor, String shortName) {
		String nodename = nodeNames.get(makeKey(flavor,shortName));
		if (nodename != null) return GraphId.fromString(nodename);
		return null;
	}

	@Override
	public Iterator<GraphId> getNodeIterator() {
		return new TokyoValueIterator(new BDBCUR(nodeNames));
	}

	@Override
	public String getProperty(GraphId id, String prop) {
		return nodeProps.get(makeKey(id,prop));
	}

	@Override
	public void setProperty(GraphId id, String prop, String val) {
		nodeProps.put(makeKey(id,prop), val);
	}
	
	protected class TokyoValueIterator implements Iterator<GraphId> {
		private BDBCUR cursor;
		public TokyoValueIterator(BDBCUR cursor) {
			this.cursor = cursor;
			this.cursor.first();
		}
		public void remove() { throw new UnsupportedOperationException("Can't remove from a Tokyo Cabinet iterator."); }
		public boolean hasNext() {
			return (cursor.key2() != null);
		}
		public GraphId next() {
			String value = cursor.val2();
			cursor.next();
			return GraphId.fromString(value);
		}
	}
}