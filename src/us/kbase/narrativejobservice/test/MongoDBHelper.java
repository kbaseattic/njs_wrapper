package us.kbase.narrativejobservice.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.kbase.common.utils.ProcessHelper;

public class MongoDBHelper {
    private final File tempDir;
    private final String testName;
    
    private File workDir = null;
    private File mongoDir = null;
    private int mongoPort = -1;
    private boolean deleteWorkFolderOnShutdown = false;

    public MongoDBHelper(String testName, File tempDir) {
        this.testName = testName;
        this.tempDir = tempDir;
    }

    public File getWorkDir() {
        return workDir;
    }
    
    public int getMongoPort() {
        return mongoPort;
    }
    
    public boolean isDeleteWorkFolderOnShutdown() {
        return deleteWorkFolderOnShutdown;
    }
    
    public void setDeleteWorkFolderOnShutdown(boolean deleteWorkFolderOnShutdown) {
        this.deleteWorkFolderOnShutdown = deleteWorkFolderOnShutdown;
    }
    
    public void startup(String mongoExePath) throws Exception {
        workDir = prepareWorkDir(testName);
        mongoDir = new File(workDir, "mongo");
        mongoPort = startupMongo(mongoExePath, mongoDir);
    }
    
    public void shutdown() throws Exception {
        killPid(mongoDir);
        if (deleteWorkFolderOnShutdown && workDir.exists())
            deleteRecursively(workDir);
    }
    
    private static int startupMongo(String mongodExePath, File dir) throws Exception {
        if (mongodExePath == null)
            mongodExePath = "mongod";
        if (!dir.exists())
            dir.mkdirs();
        File dataDir = new File(dir, "data");
        dataDir.mkdir();
        File logFile = new File(dir, "mongodb.log");
        int port = findFreePort();
        File configFile = new File(dir, "mongod.conf");
        writeLines(Arrays.asList(
                "dbpath=" + dataDir.getAbsolutePath(),
                "logpath=" + logFile.getAbsolutePath(),
                "logappend=true",
                "port=" + port,
                "bind_ip=127.0.0.1"
                ), configFile);
        File scriptFile = new File(dir, "start_mongo.sh");
        writeLines(Arrays.asList(
                "#!/bin/bash",
                "cd " + dir.getAbsolutePath(),
                mongodExePath + " --config " + configFile.getAbsolutePath() + " >out.txt 2>err.txt & pid=$!",
                "echo $pid > pid.txt"
                ), scriptFile);
        ProcessHelper.cmd("bash", scriptFile.getCanonicalPath()).exec(dir);
        boolean ready = false;
        int waitSec = 120;
        for (int n = 0; n < waitSec; n++) {
            Thread.sleep(1000);
            if (logFile.exists()) {
                if (grep(lines(logFile), "waiting for connections on port " + port).size() > 0) {
                    ready = true;
                    break;
                }
            }
        }
        if (!ready) {
            if (logFile.exists())
                for (String l : lines(logFile))
                    System.err.println("MongoDB log: " + l);
            throw new IllegalStateException("MongoDB couldn't startup in " + waitSec + " seconds");
        }
        System.out.println(dir.getName() + " was started up");
        return port;
    }

    private static void killPid(File dir) {
        if (dir == null)
            return;
        try {
            File pidFile = new File(dir, "pid.txt");
            if (pidFile.exists()) {
                String pid = lines(pidFile).get(0).trim();
                ProcessHelper.cmd("kill", pid).exec(dir);
                System.out.println(dir.getName() + " was stopped");
            }
        } catch (Exception ignore) {}
    }
    
    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {}
        throw new IllegalStateException("Can not find available port in system");
    }

    private File prepareWorkDir(String testName) throws IOException {
        if (!tempDir.exists())
            tempDir.mkdir();
        for (File dir : tempDir.listFiles()) {
            if (dir.isDirectory() && dir.getName().startsWith("test_" + testName + "_"))
                try {
                    deleteRecursively(dir);
                } catch (Exception e) {
                    System.out.println("Can not delete directory [" + dir.getName() + "]: " + e.getMessage());
                }
        }
        File workDir = new File(tempDir, "test_" + testName + "_" + System.currentTimeMillis());
        if (!workDir.exists())
            workDir.mkdir();
        return workDir;
    }

    private static void deleteRecursively(File fileOrDir) {
        if (fileOrDir.isDirectory() && !Files.isSymbolicLink(fileOrDir.toPath()))
            for (File f : fileOrDir.listFiles()) 
                deleteRecursively(f);
        fileOrDir.delete();
    }
    
    private static List<String> lines(File f) throws IOException {
        return lines(new FileInputStream(f));
    }

    private static List<String> lines(InputStream is) throws IOException {
        return lines(new InputStreamReader(is));
    }
    
    private static List<String> lines(Reader r) throws IOException {
        List<String> ret = new ArrayList<String>();
        BufferedReader in = new BufferedReader(r);
        String line;
        while ((line = in.readLine()) != null) 
            ret.add(line);
        in.close();
        return ret;
    }
    
    private static void writeLines(List<String> lines, File targetFile) throws IOException {
        PrintWriter pw = new PrintWriter(targetFile);
        for (String l : lines)
            pw.println(l);
        pw.close();
    }

    public static List<String> grep(List<String> lines, String substring) {
        List<String> ret = new ArrayList<String>();
        for (String l : lines)
            if (l.contains(substring))
                ret.add(l);
        return ret;
    }
}
