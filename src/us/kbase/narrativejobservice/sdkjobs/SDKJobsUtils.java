package us.kbase.narrativejobservice.sdkjobs;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

public class SDKJobsUtils {

    /**
     * Attempt to get cgroup for given PID
     */
    public static String lookupParentCgroup(int pid) throws Exception {
        String cgroupsPath = String.format("/proc/%s/cgroup", pid);
        List<String> lines = FileUtils.readLines(new File(cgroupsPath));
        String parentCgroup = null;
        for (String line : lines) {
            if (line.contains("htcondor")) {
                String[] idLabelCgroup = line.split(":");
                if (idLabelCgroup.length != 3) {
                    throw new Exception("Invalid cgroups encountered");
                }
                else{
                    parentCgroup = idLabelCgroup[2];
                }
            }
        }
        if (parentCgroup == null) {
            throw new Exception("Couldn't extract valid cgroups from" + parentCgroup.toString());
        }
        return parentCgroup;
    }

}
