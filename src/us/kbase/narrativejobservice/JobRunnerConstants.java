package us.kbase.narrativejobservice;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;


public class JobRunnerConstants {

    public static final String JOB_CONFIG_FILE = "config.properties";

    //TODO consider an enum here
    public static final String DEV = "dev";
    public static final String BETA = "beta";
    public static final String RELEASE = "release";
    public static final Set<String> RELEASE_TAGS =
            Collections.unmodifiableSet(new LinkedHashSet<String>(
                    Arrays.asList(DEV, BETA, RELEASE)));
}
