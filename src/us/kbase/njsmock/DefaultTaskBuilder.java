package us.kbase.njsmock;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.Tuple11;
import us.kbase.common.taskqueue.TaskQueueConfig;
import us.kbase.common.taskqueue.TaskRunner;
import us.kbase.workspace.ListObjectsParams;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectIdentity;
import us.kbase.workspace.SaveObjectsParams;
import us.kbase.workspace.SubObjectIdentity;
import us.kbase.workspace.WorkspaceClient;

public abstract class DefaultTaskBuilder<T> implements TaskRunner<T> {
	protected File tempDir;
	//protected File dataDir;
	protected ObjectStorage storage;

	@Override
	public void init(TaskQueueConfig queueCfg, Map<String, String> configParams) {
		init(getDirParam(configParams, NJSMockServer.CFG_PROP_SCRATCH), //getDirParam(configParams, "data.dir"),
				createDefaultObjectStorage(queueCfg.getWsUrl()));
	}

	public static ObjectStorage createDefaultObjectStorage(final WorkspaceClient client) {
		return new ObjectStorage() {
			
			@Override
			public List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> saveObjects(
					String authToken, SaveObjectsParams params) throws Exception {
				return client.saveObjects(params);
			}
			
			@Override
			public List<ObjectData> getObjects(String authToken,
					List<ObjectIdentity> objectIds) throws Exception {
				return client.getObjects(objectIds);
			}
			
			@Override
			public List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> listObjects(
					String authToken, ListObjectsParams params) throws Exception {
				return client.listObjects(params);
			}
			
			@Override
		    public List<ObjectData> getObjectSubset(String authToken, List<SubObjectIdentity> objectIds) throws Exception {
				return client.getObjectSubset(objectIds);
		    }
			
			@Override
			public String getUrl() {
				return client.getURL().toString();
			}
		};
	}
	
	public static ObjectStorage createDefaultObjectStorage(final String wsUrl) {
		return new ObjectStorage() {
			private WorkspaceClient client = null;
			
			@Override
			public List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> saveObjects(
					String authToken, SaveObjectsParams params) throws Exception {
				return client(authToken).saveObjects(params);
			}
			
			@Override
			public List<ObjectData> getObjects(String authToken,
					List<ObjectIdentity> objectIds) throws Exception {
				return client(authToken).getObjects(objectIds);
			}
			
			@Override
			public List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> listObjects(
					String authToken, ListObjectsParams params) throws Exception {
				return client(authToken).listObjects(params);
			}
			
			@Override
		    public List<ObjectData> getObjectSubset(String authToken, List<SubObjectIdentity> objectIds) throws Exception {
				return client(authToken).getObjectSubset(objectIds);
		    }
			
			@Override
			public String getUrl() {
				return wsUrl;
			}
			
			private WorkspaceClient client(String authToken) throws Exception {
				if (client != null)
					return client;
				WorkspaceClient client = new WorkspaceClient(new URL(wsUrl), new AuthToken(authToken));
				client.setAuthAllowedForHttp(true);
				return client;
			}
		};
	}

	public DefaultTaskBuilder<T> init(File tempDir, /*File dataDir,*/ ObjectStorage ws) {
		this.tempDir = tempDir;
		if (!tempDir.exists())
			tempDir.mkdir();
		/*this.dataDir = dataDir;
		if (!dataDir.exists())
			throw new IllegalStateException("Directory " + dataDir + " doesn't exist");*/
		this.storage = ws;
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
	
	/*protected File getDataDir() {
		return dataDir;
	}*/
	
	protected ObjectStorage getStorage() {
		return storage;
	}
	
	/*protected File getBinDir() {
		return new File(dataDir, "bin");
	}*/
	
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
