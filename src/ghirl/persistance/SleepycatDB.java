package ghirl.persistance;

import com.sleepycat.bind.serial.*;
import com.sleepycat.bind.tuple.*;
import com.sleepycat.je.*;

import ghirl.util.FilesystemUtil;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * Simpler interface to a sleepy cat database.
 *
 * This is designed to be subclassed to create persistant hashtable-like
 * structures.
 */

public class SleepycatDB
{
	private static final Logger log = Logger.getLogger(SleepycatDB.class);
	private Environment env;
	private DatabaseConfig dbConfig, dbDupConfig;
	private List<Database> openDBs = new ArrayList<Database>();
	private List<Cursor> openCursors = new ArrayList<Cursor>();

	/** Initialize a database "environment", which will hold multiple
	 * 'databases'.  Each database is a persistant hashtable that maps
	 * strings to strings, or strings to sets of strings.
	 *
	 * @param dbDirName directory to contain the database
	 * @param mode If 'w', will create (overwrite) the database in the specified directory.
	 */
	public void initDBs(String dbDirName,char mode) throws DatabaseException
	{
		EnvironmentConfig envConfig = new EnvironmentConfig();
		dbConfig = new DatabaseConfig();
		dbDupConfig = new DatabaseConfig();
		dbDupConfig.setSortedDuplicates(true);
		File envDir = new File(dbDirName);
		switch(mode) {
			case 'w':
				if (envDir.exists()) {
					// remove old copies of databases rooted here.  This is sortof
					// a hack -- if a different db was rooted here than
					// the one which is currently running, we won't remove the
					// right databases, and we'll still end up with old data
					// in the db.  There ought to be a better way to do this but
					// I don't know what it is.  Deleting the files at runtime
					// isn't enough.
					cleanEnvironment(envDir); 
				}
				createEnvironment(envDir,envConfig);
				break;
			case 'a':
				if (!envDir.exists()) createEnvironment(envDir,envConfig);
				break;
		}
		env = new Environment(envDir,envConfig);
	}
	
	private void createEnvironment(File envDir, EnvironmentConfig envConfig) {
		envDir.mkdir();
		if (!envDir.isDirectory() || !envDir.canWrite()) {
			throw new IllegalArgumentException("can't write to env directory "+envDir);
		}
		envConfig.setAllowCreate(true);
		// exclusiveCreate: will complain if a db is opened which already exists
		dbConfig.setExclusiveCreate(true); 
		dbConfig.setAllowCreate(true);
		dbDupConfig.setExclusiveCreate(true); 
		dbDupConfig.setAllowCreate(true);
	}
	
	protected void cleanEnvironment(File envDir) throws DatabaseException {
		log.warn("A previous environment existed at this location. Overwriting: Removing all database contents rooted at "+envDir.getPath());
		try {
			EnvironmentConfig tmpconfig = new EnvironmentConfig();
			tmpconfig.setAllowCreate(true);
			Environment tmpenv = new Environment(envDir, tmpconfig);
			for (String dbname : tmpenv.getDatabaseNames()) {
				log.info("Cleaning "+dbname);
				tmpenv.removeDatabase(null, dbname);
			}
		} catch (com.sleepycat.je.recovery.NoRootException e) {
			log.warn("Previous environment at this location was corrupted; check your cleanup procedure.  Wiping and restoring...",e);
		} 
		FilesystemUtil.rm_r(envDir);
	}
	protected List<String> getDatabaseNames() { return new ArrayList<String>(); }
	
	private boolean isclosed=false;
	public void finalize() {
		if (isclosed) return;
		try {
			this.closeDBs();
		} catch (DatabaseException ex) {
			log.error("Problem closing DB at finalization time",ex);
		}
	}

	/** Close all open databases.
	 * @throws DatabaseException 
	 */
	public void closeDBs() throws DatabaseException {
		if (isclosed) {
			log.info("Database is already closed.");
			return;
		}
		log.info("Closing SleepycatDB...");
		for(Cursor c : openCursors) {
			try {
				c.close();
			} catch (DatabaseException e) {
				log.error("BADNESS 10000 Couldn't close open cursor");
				throw new IllegalStateException("Database could not be closed properly");
			}
		}
		for (Database db : openDBs) {
			try { db.close(); } catch (DatabaseException e) { 
				try { log.error("Trouble closing database "+db.getDatabaseName(), e); } catch (DatabaseException f) {
					log.error("Trouble closing a database that is so hosed I can't even get the name", f);
				}
			}
		}
		env.close();
		isclosed = true;
	}

	/** Open a "DB", which will map keys to values.
	 */
	public Database openDB(String dbName) throws DatabaseException
	{
		Database db = env.openDatabase(null,dbName,dbConfig);
		openDBs.add(db);
		return db;
	}

	/** Open a "DB", which will map keys to sets of values.
	 */
	public Database openDupDB(String dbName) throws DatabaseException
	{
		Database db = env.openDatabase(null,dbName,dbDupConfig);
		openDBs.add(db);
		return db;
	}

	/** Write any buffers out to disk.
	 */
	public void sync() 
	{
		try {
			env.sync();
		} catch (DatabaseException ex) {
			System.err.println("error thrown trying to sync: "+ex);
		}
	}

	/** Get the set of entries associated with the key in the given database.
	 */
	protected Set getDB(Database db,String key) throws DatabaseException
	{
		return getDB(db,key,Integer.MAX_VALUE);
	}

	/** Get the set of entries associated with the key in the given database.
	 */
	protected Set getDB(Database db,String key,int maxNum) throws DatabaseException
	{
		try {
			Set accum = new HashSet();
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry valEntry = new DatabaseEntry();
			Cursor cursor = db.openCursor(null,null);
			OperationStatus status = cursor.getSearchKey(keyEntry,valEntry,LockMode.DEFAULT);
			int num = 0;
			while (status==OperationStatus.SUCCESS && num<maxNum) {
				accum.add( new String(valEntry.getData()) );
				status = cursor.getNextDup(keyEntry,valEntry,LockMode.DEFAULT);
				num++;
			}
			cursor.close();
			//System.out.println("get "+key+" -> "+accum); 
			return accum;
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException("unexpected error "+ex);
		}
	}

	/** Get the first entry associated with the key in the given database.
	 */
	protected String getFirstDB(Database db,String key) throws DatabaseException
	{
		try {
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry valEntry = new DatabaseEntry();
			Cursor cursor = db.openCursor(null,null);
			OperationStatus status = cursor.getSearchKey(keyEntry,valEntry,LockMode.DEFAULT);
			String val = (status!=OperationStatus.SUCCESS)?null:new String(valEntry.getData());
			cursor.close();
			return val;
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException("unexpected error "+ex);
		}
	}

	/** Get the first entry associated with the key in the given database.
	 */
	protected byte[] getFirstDB(Database db,byte[] key) throws DatabaseException
	{
		DatabaseEntry keyEntry = new DatabaseEntry(key);
		DatabaseEntry valEntry = new DatabaseEntry();
		Cursor cursor = db.openCursor(null,null);
		OperationStatus status = cursor.getSearchKey(keyEntry,valEntry,LockMode.DEFAULT);
		byte[] val = (status!=OperationStatus.SUCCESS)?null:valEntry.getData();
		cursor.close();
		return val;
	}


	/** Define a new value that is associated with the key in the given database.
	 */
	protected void putDB(Database db,String key,String val) 
	throws DatabaseException
	{
		try {
			//System.out.println("put: "+key+" -> "+val);
			//Transaction tx = env.beginTransaction(null,null);
			Transaction tx = null;
			DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
			DatabaseEntry valEntry = new DatabaseEntry(val.getBytes("UTF-8"));
			db.put(tx, keyEntry, valEntry);
			//tx.commit();
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException("unexpected error "+ex);
		}
	}

	/** Define a new value that is associated with the key in the given database.
	 */
	protected void putDB(Database db,byte[] key,byte[] val) throws DatabaseException
	{
		//System.out.println("put: "+key+" -> "+val);
		//Transaction tx = env.beginTransaction(null,null);
		Transaction tx = null;
		DatabaseEntry keyEntry = new DatabaseEntry(key);
		DatabaseEntry valEntry = new DatabaseEntry(val);
		db.put(tx, keyEntry, valEntry);
	}

	/** Remove all values associated with the key.
	 */
	protected void clearDB(Database db,String key) 
	throws DatabaseException,UnsupportedEncodingException
	{
		//System.out.println("delete: "+key+" -> ?");
		DatabaseEntry keyEntry = new DatabaseEntry(key.getBytes("UTF-8"));
		DatabaseEntry valEntry = new DatabaseEntry();
		//Transaction tx = env.beginTransaction(null,null);
		Transaction tx = null;
		Cursor cursor = db.openCursor(tx,null);
		OperationStatus status = cursor.getSearchKey(keyEntry,valEntry,LockMode.DEFAULT);
		while (status==OperationStatus.SUCCESS) {
			String val = new String(valEntry.getData());
			//System.out.println("deleted: "+key+" -> '"+val+"'");
			cursor.delete();
			status = cursor.getNextDup(keyEntry,valEntry,LockMode.DEFAULT);
		}
		cursor.close();
		//tx.commit();
	}
	
	private Cursor addCursor(Cursor c) { this.openCursors.add(c); return c; }
	private void removeCursor(Cursor c) { this.openCursors.remove(c); }

	/** An iterator over all keys in a database.
	 */
	public class KeyIteratorDB implements Iterator
	{
		private Cursor cursor;
		private OperationStatus status;
		private DatabaseEntry keyEntry, valEntry;

		public KeyIteratorDB( Database db ) throws DatabaseException
		{
			try {
				cursor = addCursor(db.openCursor(null,null));
				keyEntry = new DatabaseEntry("".getBytes("UTF-8"));
				valEntry = new DatabaseEntry();
				status = cursor.getSearchKeyRange(keyEntry,valEntry,LockMode.DEFAULT);
			} catch (UnsupportedEncodingException ex) {
				throw new IllegalStateException("unexpected error "+ex);		
			}
		} 
		public boolean hasNext()
		{
			return status==OperationStatus.SUCCESS;
		}
		public Object next()
		{
			String key = new String(keyEntry.getData());
			try {
				status = cursor.getNext(keyEntry,valEntry,LockMode.DEFAULT);
			} catch (DatabaseException ex) {
				throw new IllegalStateException("database error "+ex);
			}
			return key;
		}
		public void remove()
		{
			throw new UnsupportedOperationException("not implemented");
		}
		protected void finalize() throws Throwable
		{
			cursor.close();
			removeCursor(cursor);
		}
	}
}
