package us.kbase.narrativejobservice;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import com.mongodb.MongoCredential;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;

import java.util.Arrays;

import com.mongodb.Block;

import com.mongodb.client.MongoCursor;

import static com.mongodb.client.model.Filters.*;

import com.mongodb.client.result.DeleteResult;

import static com.mongodb.client.model.Updates.*;

import com.mongodb.client.result.UpdateResult;

import static com.mongodb.client.model.Projections.*;

import java.util.ArrayList;
import java.util.List;


public class ReaperService {

    public void init() throws Exception {
        //http://mongodb.github.io/mongo-java-driver/3.4/driver/tutorials/authentication/
        String user = "bsadkhin";
        String password = "password";
        String db = "userjobstate";


        MongoCredential credential = MongoCredential.createCredential(user, db, password.toCharArray());
// Arrays.asList(credential)
        MongoClient mongoClient = new com.mongodb.MongoClient(new ServerAddress("localhost", 27017)
        );
        MongoDatabase database = mongoClient.getDatabase(db);
        MongoCollection<Document> collection = database.getCollection("jobstate");


        final List<String> idList = new ArrayList<>();

        collection.find(eq("complete", false)).projection(include("_id")).forEach(
                new Block<Document>() {
                    @Override
                    public void apply(final Document document) {

                        idList.add(document.get("_id").toString());

                    }
                });

        System.out.println("Incomplete IDS are");
        System.out.println(idList);
    }


//    private synchronized static MongoClient getMongoClient(final String hosts)
//            throws UnknownHostException, InvalidHostException {
////Only make one instance of MongoClient per JVM per mongo docs
//        final MongoClient client;
//        if (!HOSTS_TO_CLIENT.containsKey(hosts)) {
//            // Don't print to stderr
//            java.util.logging.Logger.getLogger("com.mongodb")
//                    .setLevel(Level.OFF);
//            @SuppressWarnings("deprecation") final MongoClientOptions opts = MongoClientOptions.builder()
//                    .autoConnectRetry(true).build();
//            try {
//                List<ServerAddress> addr = new ArrayList<ServerAddress>();
//                for (String s : hosts.split(","))
//                    addr.add(new ServerAddress(s));
//                client = new MongoClient(addr, opts);
//            } catch (NumberFormatException nfe) {
//                //throw a better exception if 10gen ever fixes this
//                throw new InvalidHostException(hosts
//                        + " is not a valid mongodb host");
//            }
//            HOSTS_TO_CLIENT.put(hosts, client);
//        } else {
//            client = HOSTS_TO_CLIENT.get(hosts);
//        }
//        return client;
//    }


}


