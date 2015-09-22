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
        return DockerClientBuilder.getInstance().build();
    }
}
