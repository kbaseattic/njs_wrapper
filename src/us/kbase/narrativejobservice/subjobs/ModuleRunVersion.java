package us.kbase.narrativejobservice.subjobs;

import java.net.URL;
import java.util.Set;

import us.kbase.narrativejobservice.AweClientDockerJobScript;


/** The version of a KBase SDK method run during an NJS Execution Engine job.
 * @author gaprice@lbl.gov
 *
 */
public class ModuleRunVersion {

    final private Set<String> RELEASE_TAGS =
            AweClientDockerJobScript.RELEASE_TAGS;

    //TODO unit tests

    final private URL gitURL;
    final private String module;
    final private String method;
    final private String gitHash;
    final private String version;
    final private String release;

    /** Create a version specification for a method to be run.
     * @param gitURL the Git URL of the module
     * @param module the name of the module
     * @param method the name of the method
     * @param gitHash the Git hash of the module commit
     * @param version the version of the module
     * @param release the release tag for the module
     */
    public ModuleRunVersion(
            final URL gitURL,
            final String module,
            final String method,
            final String gitHash,
            final String version,
            final String release) {
        if (gitURL == null) {
            throw new IllegalArgumentException("Git URL may not be null");
        }
        if (release != null && !RELEASE_TAGS.contains(release)) {
            throw new IllegalArgumentException(
                    "Invalid release value: " + release);
        }
        notNullOrEmpty(module);
        notNullOrEmpty(method);
        notNullOrEmpty(gitHash);
        notNullOrEmpty(version);
        this.gitURL = gitURL;
        this.module = module;
        this.method = method;
        this.gitHash = gitHash;
        this.version = version;
        this.release = release;
    }
    
    private void notNullOrEmpty(final String s) {
        if (s == null || s.isEmpty()) {
            throw new IllegalArgumentException(
                    "No arguments may be null or the empty string");
        }
    }

    /**
     * @return the gitURL
     */
    public URL getGitURL() {
        return gitURL;
    }

    /**
     * @return the gitHash
     */
    public String getGitHash() {
        return gitHash;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the release
     */
    public String getRelease() {
        return release;
    }

    /**
     * @return the module
     */
    public String getModule() {
        return module;
    }

    /**
     * @return the method
     */
    public String getMethod() {
        return method;
    }

    /** Returns the version concatenated with the release as version-release.
     * If no release tag is provided, returns the version.
     * @return the version with release tag.
     */
    public String getVersionAndRelease() {
        if (release != null) {
            return version + "-" + release;
        }
        return version;
    }
    
    /** Returns the full method name, including the module.
     * @return the full method name, including the module.
     */
    public String getModuleDotMethod() {
        return module + "." + method;
    }
    
}
