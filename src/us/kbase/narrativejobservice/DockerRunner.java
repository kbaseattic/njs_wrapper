package us.kbase.narrativejobservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class DockerRunner {
    private final String dockerRegistry;
    
    public DockerRunner(String dockerRegistry) {
        this.dockerRegistry = dockerRegistry;
    }
    
    public File run(String imageName, String version, String moduleName, 
            File inputData, String token, StringBuilder log, File outputFile, 
            boolean removeImage) throws Exception {
        if (!inputData.getName().equals("input.json"))
            throw new IllegalStateException("Input file has wrong name: " + 
                    inputData.getName() + "(it should be named input.json)");
        File workDir = inputData.getCanonicalFile().getParentFile();
        File tokenFile = new File(workDir, "token");
        DockerClient cl = createDockerClient();
        String fullImgName = checkImagePulled(cl, imageName, version);
        String cntName = null;
        try {
            FileWriter fw = new FileWriter(tokenFile);
            fw.write(token);
            fw.close();
            if (outputFile.exists())
                outputFile.delete();
            long suffix = System.currentTimeMillis();
            while (true) {
                cntName = imageName + "_" + suffix;
                if (findContainerByNameOrIdPrefix(cl, cntName) == null)
                    break;
                suffix++;
            }
            CreateContainerResponse resp = cl.createContainerCmd(fullImgName)
                    .withName(cntName).withBinds(new Bind(workDir.getAbsolutePath(), 
                            new Volume("/kb/deployment/services/" + moduleName + 
                                    "/work"))).exec();
            String cntId = resp.getId();
            cl.startContainerCmd(cntId).exec();
            cl.waitContainerCmd(cntId).exec();
            InspectContainerResponse resp2 = cl.inspectContainerCmd(cntId).exec();
            if (resp2.getState().isRunning()) {
                try {
                    Container cnt = findContainerByNameOrIdPrefix(cl, cntName);
                    cl.stopContainerCmd(cnt.getId()).exec();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                throw new IllegalStateException("Container was still running");
            }
            int exitCode = resp2.getState().getExitCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    cl.logContainerCmd(cntId).withStdOut(true).exec()));
            while (true) {
                String l = br.readLine();
                if (l == null)
                    break;
                log.append(l).append("\n");
            }
            br.close();
            StringBuilder err = new StringBuilder();
            br = new BufferedReader(new InputStreamReader(
                    cl.logContainerCmd(cntId).withStdErr(true).exec()));
            while (true) {
                String l = br.readLine();
                if (l == null)
                    break;
                log.append(l).append("\n");
                err.append(l).append("\n");
            }
            br.close();
            if (outputFile.exists()) {
                return outputFile;
            } else {
                String msg = "Output file is not found, exit code is " + exitCode;
                if (err.length() > 0)
                    msg += ", errors: " + err;
                throw new IllegalStateException(msg);
            }
        } finally {
            if (cntName != null) {
                Container cnt = findContainerByNameOrIdPrefix(cl, cntName);
                if (cnt != null) {
                    try {
                        cl.removeContainerCmd(cnt.getId()).withRemoveVolumes(true).exec();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            if (removeImage) {
                try {
                    Image img = findImageId(cl, fullImgName);
                    cl.removeImageCmd(img.getId()).exec();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (tokenFile.exists())
                try {
                    tokenFile.delete();
                } catch (Exception ignore) {}
        }
    }
    
    private String checkImagePulled(DockerClient cl, String imageName, String version)
            throws Exception {
        String requestedTag = dockerRegistry + "/" + imageName + ":" + version;
        if (findImageId(cl, requestedTag) == null) {
            BufferedReader br = new BufferedReader(new InputStreamReader(cl.pullImageCmd(requestedTag).exec()));
            try {
                while (true) {
                    String l = br.readLine();
                    if (l == null)
                        break;
                    //System.out.println(l);
                }
            } finally {
                br.close();
            }
        }
        if (findImageId(cl, requestedTag) == null) {
            throw new IllegalStateException("Image was not found: " + requestedTag);
        }
        return requestedTag;
    }

    private Image findImageId(DockerClient cl, String imageTagOrIdPrefix) {
        return findImageId(cl, imageTagOrIdPrefix, false);
    }
    
    private Image findImageId(DockerClient cl, String imageTagOrIdPrefix, boolean all) {
        for (Image image: cl.listImagesCmd().withShowAll(all).exec()) {
            if (image.getId().startsWith(imageTagOrIdPrefix))
                return image;
            for (String tag : image.getRepoTags())
                if (tag.equals(imageTagOrIdPrefix))
                    return image;
        }
        return null;
    }
    
    private Container findContainerByNameOrIdPrefix(DockerClient cl, String nameOrIdPrefix) {
        for (Container cnt : cl.listContainersCmd().withShowAll(true).exec()) {
            if (cnt.getId().startsWith(nameOrIdPrefix))
                return cnt;
            for (String name : cnt.getNames())
                if (name.equals(nameOrIdPrefix) || name.equals("/" + nameOrIdPrefix))
                    return cnt;
        }
        return null;
    }
    
    private DockerClient createDockerClient() throws Exception {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "ERROR");
        Logger log = LoggerFactory.getLogger("com.github.dockerjava");
        if (log instanceof ch.qos.logback.classic.Logger) {
            ch.qos.logback.classic.Logger log2 = (ch.qos.logback.classic.Logger)log;
            log2.setLevel(Level.ERROR);
        }
        DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder()
                .withUri("unix:///var/run/docker.sock")
                .build();
        return DockerClientBuilder.getInstance(config).build();
    }
}
/*
javax.ws.rs.ProcessingException: org.apache.http.conn.UnsupportedSchemeException: https protocol is not supported
        at org.glassfish.jersey.apache.connector.ApacheConnector.apply(ApacheConnector.java:513)
        at org.glassfish.jersey.client.ClientRuntime.invoke(ClientRuntime.java:246)
        at org.glassfish.jersey.client.JerseyInvocation$3.call(JerseyInvocation.java:705)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:315)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:297)
        at org.glassfish.jersey.internal.Errors.process(Errors.java:228)
        at org.glassfish.jersey.process.internal.RequestScope.runInScope(RequestScope.java:424)
        at org.glassfish.jersey.client.JerseyInvocation.invoke(JerseyInvocation.java:701)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.method(JerseyInvocation.java:417)
        at org.glassfish.jersey.client.JerseyInvocation$Builder.get(JerseyInvocation.java:313)
        at com.github.dockerjava.jaxrs.ListImagesCmdExec.execute(ListImagesCmdExec.java:39)
        at com.github.dockerjava.jaxrs.ListImagesCmdExec.execute(ListImagesCmdExec.java:17)
        at com.github.dockerjava.jaxrs.AbstrDockerCmdExec.exec(AbstrDockerCmdExec.java:57)
        at com.github.dockerjava.core.command.AbstrDockerCmd.exec(AbstrDockerCmd.java:29)
        at us.kbase.narrativejobservice.DockerRunner.findImageId(DockerRunner.java:152)
        at us.kbase.narrativejobservice.DockerRunner.findImageId(DockerRunner.java:148)
        at us.kbase.narrativejobservice.DockerRunner.checkImagePulled(DockerRunner.java:128)
        at us.kbase.narrativejobservice.DockerRunner.run(DockerRunner.java:38)
        at us.kbase.narrativejobservice.AweClientDockerJobScript.main(AweClientDockerJobScript.java:91)
Caused by: org.apache.http.conn.UnsupportedSchemeException: https protocol is not supported
        at org.apache.http.impl.conn.HttpClientConnectionOperator.connect(HttpClientConnectionOperator.java:99)
        at org.apache.http.impl.conn.PoolingHttpClientConnectionManager.connect(PoolingHttpClientConnectionManager.java:314)
        at org.apache.http.impl.execchain.MainClientExec.establishRoute(MainClientExec.java:357)
        at org.apache.http.impl.execchain.MainClientExec.execute(MainClientExec.java:218)
        at org.apache.http.impl.execchain.ProtocolExec.execute(ProtocolExec.java:194)
        at org.apache.http.impl.execchain.RetryExec.execute(RetryExec.java:85)
        at org.apache.http.impl.execchain.RedirectExec.execute(RedirectExec.java:108)
        at org.apache.http.impl.client.InternalHttpClient.doExecute(InternalHttpClient.java:186)
        at org.apache.http.impl.client.CloseableHttpClient.execute(CloseableHttpClient.java:72)
        at org.glassfish.jersey.apache.connector.ApacheConnector.apply(ApacheConnector.java:465)
        ... 18 more
*/
