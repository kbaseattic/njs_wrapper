package us.kbase.narrativejobservice.db;

import static us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_MONGO_HOSTS;
import static us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_MONGO_DBNAME;
import static us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_MONGO_USER;
import static us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_MONGO_PWD;
import static us.kbase.narrativejobservice.NarrativeJobServiceServer.CFG_PROP_SHOCK_URL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoException;
import com.mongodb.WriteConcernException;

import us.kbase.auth.AuthService;
import us.kbase.auth.AuthToken;
import us.kbase.auth.TokenExpiredException;
import us.kbase.common.mongo.exceptions.InvalidHostException;
import us.kbase.common.mongo.exceptions.MongoAuthException;
import us.kbase.common.service.UObject;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.shock.client.BasicShockClient;
import us.kbase.shock.client.ShockNodeId;
import us.kbase.shock.client.exceptions.InvalidShockUrlException;
import us.kbase.shock.client.exceptions.ShockHttpException;
import us.kbase.shock.client.exceptions.ShockNoFileException;

/* to munge script output into a list of shock ids:

In [1]: f = open('shocktomongoEE')

In [2]: for l in f:
    if 'shock id:' in l:
        _, id = l.split('shock id: ')
        print id.strip()

 */

public class MigrateShockDataToMongo {
	
	final private static int MAX_SIZE = 10000000;
	final private static boolean DONT_PULL_SHOCK_NODES = false;

	public static void main(String[] args) throws Exception{
		// arg 0 is path to config file
		// arg 1 is username for pulling shock nodes
		// arg 2 is pwd for pulling shock nodes
		// arg 3 is optional and if it exists will run script in test mode
		System.setProperty(
				NarrativeJobServiceServer.SYS_PROP_KB_DEPLOYMENT_CONFIG,
				args[0]);
		
		convert(AuthService.login(args[1], args[2]).getToken(),
				args.length > 3);
	}

	public static void convert(AuthToken token, boolean dryrun)
			throws UnknownHostException, InvalidHostException, IOException,
			MongoAuthException, InterruptedException,
			TokenExpiredException, InvalidShockUrlException,
			ShockHttpException {
		if (dryrun) {
			log("Dryrun mode, no changes will be made");
		} else {
			log("Making changes to database");
		}
		log("Querying shock with user " + token.getUserName());
		
		final Map<String, String> config = NarrativeJobServiceServer.config();
		DBCollection col = getJobsDB(config)
				.getCollection(ExecEngineMongoDb.COL_EXEC_TASKS);
		log("Mongo connection: " + col.getDB().getMongo().getConnectPoint() +
				" db: " + col.getDB());
		log("Objects to convert: " + col.count());
		final URL shockURL = new URL(config.get(CFG_PROP_SHOCK_URL));
		log("Shock URL: " + shockURL);
		final BasicShockClient bsc = new BasicShockClient(shockURL, token);
		final Set<String> seenIDs = new HashSet<String>();
		int count = 1;
		for (DBObject j: col.find()) {
			final String jobId = (String) j.get("ujs_job_id");
			if (seenIDs.contains(jobId)) {
				log("Skipping " + jobId + ", already processed");
				count++;
				continue;
			}
			seenIDs.add(jobId);
			log(String.format("#%s: jobid: %s", count, jobId));
			if (j.get("job_input") != null && j.get("job_output") != null) {
				log("    Skipping " + jobId + ", already has job_input and job_output records");
				count++;
				continue;
			}
			
			final String shockIn = (String) j.get("input_shock_id");
			final String shockOut = (String) j.get("output_shock_id");
			final DBObject update = new BasicDBObject();
			log("    input shock id: " + shockIn);
			log("    output shock id: " + shockOut);
			if (!DONT_PULL_SHOCK_NODES) {
				if (!getShockData(bsc, shockIn, update, "input")) {
					log("    Retrieving input data failed unexpectedly. Skipping record. " +
							"Please repair the node and try again.");
					count++;
					continue;
				}
				if (!getShockData(bsc, shockOut, update, "output")) {
					log("    Retrieving output data failed unexpectedly. Skipping record. " +
							"Please repair the node and try again.");
					count++;
					continue;
				}
				if (!dryrun) {
					final DBObject q = new BasicDBObject("ujs_job_id", jobId);
					try {
						col.update(q, new BasicDBObject("$set", update));
					} catch (WriteConcernException e) {
						log("Failed mongo write with update\n" + update);
						throw e;
					}
				}
			}
			count++;
		}
	}

	private static boolean getShockData(
			final BasicShockClient bsc,
			final String shockNodeId,
			final DBObject update,
			final String type)
			throws IOException, ShockHttpException, TokenExpiredException,
			JsonParseException, JsonMappingException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			bsc.getFile(new ShockNodeId(shockNodeId), baos);
		} catch (ShockNoFileException e) {
			log("    Skipping " + type + " data, shock node is empty: " +
					shockNodeId);
			return true;
		} catch (ShockHttpException e) {
			log(String.format("    Unexpected error for node %s:\n%s",
					shockNodeId, e.getMessage()));
			return false;
		}
		if (baos.size() > MAX_SIZE) {
			log("    Skipping " + type + " data, too many bytes: "
					+ baos.size() + ": shock node: " + shockNodeId);
		} else {
			@SuppressWarnings("unchecked")
			final Map<String, Object> data = UObject.getMapper()
					.readValue(baos.toByteArray(), Map.class);
			SanitizeMongoObject.sanitize(data);
			update.put("job_" + type, data);
		}
		return true;
	}
	
	private static void log(String l) {
		System.out.println(String.format("%.3f %s",
				(System.currentTimeMillis() / 1000.0), l));
	}

	private static DB getJobsDB(Map<String, String> config)
			throws UnknownHostException, InvalidHostException,
			IOException, MongoAuthException, InterruptedException {
		final String hosts = config.get(CFG_PROP_MONGO_HOSTS);
		if (hosts == null)
			throw new IllegalStateException("Parameter " +
					CFG_PROP_MONGO_HOSTS + " is not defined in configuration");
		final String dbname = config.get(CFG_PROP_MONGO_DBNAME);
		if (dbname == null)
			throw new IllegalStateException("Parameter " +
					CFG_PROP_MONGO_DBNAME + " is not defined in configuration");
		final String user = config.get(CFG_PROP_MONGO_USER);
		final String pwd = config.get(CFG_PROP_MONGO_PWD);
		if (user == null || user.isEmpty()) {
			throw new IllegalArgumentException(
					"mongo user is not defined in the config");
		}
		if (pwd == null || pwd.isEmpty()) {
			throw new IllegalArgumentException(
					"mongo pwd is not defined in the config");
		}
		return getDB(hosts, dbname, user, pwd);
	}

	private synchronized static MongoClient getMongoClient(final String hosts)
			throws UnknownHostException, InvalidHostException {
		//Only make one instance of MongoClient per JVM per mongo docs
		final MongoClient client;
		// Don't print to stderr
		java.util.logging.Logger.getLogger("com.mongodb")
			.setLevel(Level.OFF);
		final MongoClientOptions opts = MongoClientOptions.builder().build();
		try {
//			List<ServerAddress> addr = new ArrayList<ServerAddress>();
//			for (String s: hosts.split(","))
//				addr.add(new ServerAddress(s));
			client = new MongoClient(hosts, opts);
		} catch (NumberFormatException nfe) {
			//throw a better exception if 10gen ever fixes this
			throw new InvalidHostException(hosts
					+ " is not a valid mongodb host");
		}
		return client;
	}

	@SuppressWarnings("deprecation")
	private static DB getDB(final String hosts, final String database,
			final String user, final String pwd)
					throws UnknownHostException, InvalidHostException, IOException,
					MongoAuthException, InterruptedException {
		if (database == null || database.isEmpty()) {
			throw new IllegalArgumentException(
					"database may not be null or the empty string");
		}
		final DB db = getMongoClient(hosts).getDB(database);
		db.authenticate(user, pwd.toCharArray());
		try {
			db.getCollectionNames();
		} catch (MongoException me) {
			throw new MongoAuthException("Not authorized for database "
					+ database, me);
		}
		return db;
	}

}
