package us.kbase.narrativejobservice.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;

import us.kbase.auth.AuthConfig;
import us.kbase.auth.AuthToken;
import us.kbase.auth.ConfigurableAuthService;
import us.kbase.common.test.TestException;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;

public class TesterUtils {
    
    public static File DEFAULT_CONFIG = new File("test.cfg");

    public static File prepareWorkDir(File tempDir, String testName)
            throws IOException {
        tempDir = tempDir.getCanonicalFile();
        if (!tempDir.exists())
            tempDir.mkdirs();
        for (File dir : tempDir.listFiles()) {
            if (dir.isDirectory() && dir.getName().startsWith(
                    "test_" + testName + "_"))
                try {
                    deleteRecursively(dir);
                } catch (Exception e) {
                    System.out.println("Can not delete directory [" +
                            dir.getName() + "]: " + e.getMessage());
                }
        }
        File workDir = new File(tempDir, "test_" + testName + "_" +
                System.currentTimeMillis());
        if (!workDir.exists())
            workDir.mkdir();
        return workDir;
    }
    
    public static void deleteRecursively(File fileOrDir) {
        if (fileOrDir.isDirectory() &&
                !Files.isSymbolicLink(fileOrDir.toPath()))
            for (File f : fileOrDir.listFiles()) 
                deleteRecursively(f);
        fileOrDir.delete();
    }

    public static Properties props()
            throws FileNotFoundException, IOException {
        return props(DEFAULT_CONFIG);
    }
    public static Properties props(File configFile)
            throws FileNotFoundException, IOException {
        Properties props = new Properties();
        InputStream is = new FileInputStream(configFile);
        props.load(is);
        is.close();
        return props;
    }
    
    public static void destroyDB(final DB db) {
        for (String name: db.getCollectionNames()) {
            if (!name.startsWith("system.")) {
                // dropping collection also drops indexes
                db.getCollection(name).remove(new BasicDBObject());
            }
        }
    }
    
    public static String getMongoExePath(Properties props)
            throws IOException, InterruptedException {
        String exep = props.getProperty("test-mongod-exe");
        if (exep == null || exep.isEmpty()) {
            System.out.println("getting mongod from path");
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("which mongod");
            if (proc.waitFor() > 0) {
                throw new TestException(
                        "No mongod executable specified in config file and " +
                        "couldn't find mongod in path: which mongod " +
                        "returned exit code " + proc.exitValue());
            }
            System.out.println("done getting mongod from path");
            StringWriter sw = new StringWriter();
            IOUtils.copy(proc.getInputStream(), sw, StandardCharsets.UTF_8);
            exep = sw.toString().trim();
        }
        return exep;
    }
    
    //TODO should use test.cfg for this, esp since deploy.cfg is checked in
    public static Map<String, String> loadConfig() 
            throws IOException, InvalidFileFormatException {
        Ini ini = new Ini(new File("deploy.cfg"));
        Map<String, String> origConfig = ini.get(
                NarrativeJobServiceServer.SERVICE_DEPLOYMENT_NAME);
        Properties props = props();
        for (String key : new ArrayList<String>(origConfig.keySet())) {
            if (props.containsKey(key)) {
                String value = props.getProperty(key).trim();
                origConfig.put(key, value);
            }
        }
        return origConfig;
    }
    
    public static AuthToken token(Properties props) throws Exception {
        String authUrl = loadConfig().get("auth-service-url");
        String authInsecure = loadConfig().get("auth-service-url-allow-insecure");
        ConfigurableAuthService auth = new ConfigurableAuthService(
                new AuthConfig().withKBaseAuthServerURL(new URL(authUrl))
                .withAllowInsecureURLs("true".equals(authInsecure)));
        String token = props.getProperty("token");
        if (token != null) {
            return auth.validateToken(token);
        }
        return auth.login(get(props, "user"), get(props, "password")).getToken();
    }

    public static String get(Properties props, String propName) {
        String ret = props.getProperty(propName);
        if (ret == null)
            throw new IllegalStateException("Property is not defined: " + propName);
        return ret;
    }
}
