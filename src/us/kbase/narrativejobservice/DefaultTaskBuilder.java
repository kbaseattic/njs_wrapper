package us.kbase.narrativejobservice;

import java.io.File;
import java.util.Map;

import us.kbase.common.taskqueue2.TaskQueueConfig;
import us.kbase.common.taskqueue2.TaskRunner;

public abstract class DefaultTaskBuilder<T> implements TaskRunner<T> {
	protected File tempDir;
	//protected File dataDir;
	protected String ujsUrl;
	protected String njsUrl;
	protected Map<String, String> config;

	@Override
	public void init(TaskQueueConfig queueCfg, Map<String, String> configParams) {
		init(getDirParam(configParams, NarrativeJobServiceServer.CFG_PROP_SCRATCH), 
				//getDirParam(configParams, "data.dir"),
				configParams.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL),
				configParams.get(NarrativeJobServiceServer.CFG_PROP_NJS_SRV_URL),
				configParams);
	}

	public DefaultTaskBuilder<T> init(File tempDir, String ujsUrl, String njsUrl,
	        Map<String, String> allConfigParams) {
		this.tempDir = tempDir;
		if (!tempDir.exists())
			tempDir.mkdir();
		this.ujsUrl = ujsUrl;
		this.njsUrl = njsUrl;
		this.config = allConfigParams;
		return this;
	}

	public static File getDirParam(Map<String, String> configParams, String param) {
		String tempDirPath = configParams.get(param);
		if (tempDirPath == null)
			throw new IllegalStateException("Parameter " + param + " is not defined in configuration");
		return new File(tempDirPath);
	}

	protected File getTempDir() {
		return tempDir;
	}
	
	protected String getUjsUrl() {
		return ujsUrl;
	}
	
	protected String getNjsUrl() {
		return njsUrl;
	}
	
	protected String getOsSuffix() {
		String osName = System.getProperty("os.name").toLowerCase();
		String suffix;
		if (osName.contains("linux")) {
			suffix = "linux";
		} else if (osName.contains("mac os x")) {
			suffix = "macosx";
		} else {
			throw new IllegalStateException("Unsupported OS type: " + osName);
		}
		return suffix;
	}
}
