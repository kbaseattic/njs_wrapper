package us.kbase.narrativejobservice.sdkjobs;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.WriteConcernException;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import us.kbase.auth.AuthConfig;
import us.kbase.auth.AuthException;
import us.kbase.auth.AuthToken;
import us.kbase.auth.ConfigurableAuthService;
import us.kbase.catalog.CatalogClient;
import us.kbase.catalog.ClientGroupConfig;
import us.kbase.catalog.ClientGroupFilter;
import us.kbase.catalog.LogExecStatsParams;
import us.kbase.catalog.ModuleVersion;
import us.kbase.catalog.SelectModuleVersion;
import us.kbase.common.executionengine.JobRunnerConstants;
import us.kbase.common.service.*;
import us.kbase.common.utils.AweUtils;
import us.kbase.common.utils.CondorUtils;
import us.kbase.common.utils.CountingOutputStream;
import us.kbase.narrativejobservice.CancelJobParams;
import us.kbase.narrativejobservice.CheckJobCanceledResult;
import us.kbase.narrativejobservice.CheckJobsParams;
import us.kbase.narrativejobservice.CheckJobsResults;
import us.kbase.narrativejobservice.FinishJobParams;
import us.kbase.narrativejobservice.GetJobLogsResults;
import us.kbase.narrativejobservice.JobState;
import us.kbase.narrativejobservice.JsonRpcError;
import us.kbase.narrativejobservice.LogLine;
import us.kbase.narrativejobservice.NarrativeJobServiceServer;
import us.kbase.narrativejobservice.RunJobParams;
import us.kbase.narrativejobservice.UpdateJobParams;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;
import us.kbase.narrativejobservice.db.ExecLog;
import us.kbase.narrativejobservice.db.ExecLogLine;
import us.kbase.narrativejobservice.db.ExecTask;
import us.kbase.narrativejobservice.db.SanitizeMongoObject;
import us.kbase.userandjobstate.*;
import us.kbase.workspace.GetObjectInfoNewParams;
import us.kbase.workspace.ObjectSpecification;
import us.kbase.workspace.WorkspaceClient;

public class SDKMethodRunner {

	private final static DateTimeFormatter DATE_FORMATTER =
			DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZoneUTC();

	public static final String APP_STATE_QUEUED = "queued";
	public static final String APP_STATE_STARTED = "in-progress";
	public static final String APP_STATE_DONE = "completed";
	public static final String APP_STATE_ERROR = "suspend";
	public static final String APP_STATE_CANCELLED = "cancelled";
	public static final String APP_STATE_CANCELED = "canceled";
	public static final String RELEASE = JobRunnerConstants.RELEASE;
	public static final Set<String> RELEASE_TAGS =
			JobRunnerConstants.RELEASE_TAGS;
	public static final int MAX_LOG_LINE_LENGTH = 1000;
	public static final String REQ_REL = "requested_release";
	private static final int MAX_IO_BYTE_SIZE = JobRunnerConstants.MAX_IO_BYTE_SIZE;

	private static AuthToken cachedCatalogAdminAuth = null;
	private static AuthToken cachedAweAdminAuth = null;

	private static ExecEngineMongoDb db = null;

	public static String requestClientGroups(Map<String, String> config, String srvMethod)
			throws UnauthorizedException, IOException, AuthException, JsonClientException {
		String aweClientGroups = null;
		String[] modMeth = srvMethod.split(Pattern.quote("."));
		if (modMeth.length == 2) {
			CatalogClient catCl = getCatalogClient(config, false);
			List<ClientGroupConfig> ret = catCl.listClientGroupConfigs(
					new ClientGroupFilter().withModuleName(modMeth[0]).withFunctionName(modMeth[1]));
			if (ret != null && ret.size() == 1) {
				ClientGroupConfig cgc = ret.get(0);
				List<String> groupList = cgc.getClientGroups();
				StringBuilder sb = new StringBuilder();
				for (String group : groupList) {
					if (sb.length() > 0)
						sb.append(",");
					sb.append(group);
				}
				aweClientGroups = sb.toString();
			}
		}
		return aweClientGroups;
	}

	public static String runJob(RunJobParams params, AuthToken authPart,
								String appJobId, Map<String, String> config, String aweClientGroups) throws Exception {
		//perform sanity checks before creating job
		checkWSObjects(authPart, config, params.getSourceWsObjects());
		//need to update the params before transforming to a Map
		checkModuleAndUpdateRunJobParams(params, config);
		@SuppressWarnings("unchecked") final Map<String, Object> jobInput =
				UObject.transformObjectToObject(params, Map.class);
		checkObjectLength(jobInput, MAX_IO_BYTE_SIZE, "Input", null);

		String kbaseEndpoint = config.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT);
		if (kbaseEndpoint == null) {
			String wsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
			if (!wsUrl.endsWith("/ws"))
				throw new IllegalStateException("Parameter " +
						NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT +
						" is not defined in configuration");
			kbaseEndpoint = wsUrl.replace("/ws", "");
		}
		final UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
		final CreateJobParams cjp = new CreateJobParams()
				.withMeta(params.getMeta());
		if (params.getWsid() != null) {
			cjp.withAuthstrat("kbaseworkspace")
					.withAuthparam("" + params.getWsid());
		}
		String ujsJobId = ujsClient.createJob2(cjp);
		// final String ujsJobId = ujsClient.createJob2(cjp);
		String selfExternalUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL);
		if (selfExternalUrl == null)
			selfExternalUrl = kbaseEndpoint + "/njs_wrapper";
		if (aweClientGroups == null || aweClientGroups.isEmpty())
			aweClientGroups = config.get(NarrativeJobServiceServer.CFG_PROP_DEFAULT_AWE_CLIENT_GROUPS);
		if (aweClientGroups == null || aweClientGroups.equals("*"))
			aweClientGroups = "";

		// Config switch to switch to calling new Condor Utils method submitToCondor
		if (config.get(NarrativeJobServiceServer.CFG_PROP_CONDOR_MODE).equals("1")) {
			System.out.println("UJS JOB ID FOR SUBMITTED JOB IS:" + ujsJobId);
			HashMap<String, String> optClassAds = new HashMap<String, String>();
			String[] modNameFuncName = params.getMethod().split(Pattern.quote("."));
			optClassAds.put("kb_parent_job_id", params.getParentJobId());
			optClassAds.put("kb_module_name", modNameFuncName[0]);
			optClassAds.put("kb_function_name", modNameFuncName[0]);
			optClassAds.put("kb_app_id", params.getAppId());

			if (params.getWsid() != null) {
				optClassAds.put("kb_wsid", "" + params.getWsid());
			}

			String baseDir = String.format("%s/%s/", config.get(NarrativeJobServiceServer.CFG_PROP_CONDOR_JOB_DATA_DIR), authPart.getUserName());
			String newExternalURL = config.get(NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL);
			String parentJobId = params.getParentJobId();
			String schedulerType = "condor";
			try {
				String condorId = CondorUtils.submitToCondorCLI(ujsJobId, authPart, aweClientGroups, newExternalURL, baseDir, optClassAds, getCatalogAdminAuth(config));
				saveTask(ujsJobId, condorId, jobInput, appJobId, schedulerType, parentJobId, config);

				//Start job in UJS to become available in ujs.list_jobs2(ws)
				try {
					ujsClient.startJob(ujsJobId, authPart.getToken(),
							APP_STATE_QUEUED,
							APP_STATE_QUEUED, new InitProgress().withPtype("none"),
							null);
				} catch (ServerException se) {
					// ignore and continue if the job was just started
				}

			} catch (Exception e) {
				throw new IllegalStateException("Couldn't submit condor job: " + e);
			}

		} else {
			String aweJobId = AweUtils.runTask(getAweServerURL(config), "ExecutionEngine", params.getMethod(), ujsJobId + " " + selfExternalUrl, NarrativeJobServiceServer.AWE_CLIENT_SCRIPT_NAME, authPart, aweClientGroups, getCatalogAdminAuth(config));
			if (appJobId != null && appJobId.isEmpty()) appJobId = ujsJobId;
			addAweTaskDescription(ujsJobId, aweJobId, jobInput, appJobId, config);
			//CALL THIS AND LOOK INSIDE OF THE DOCUMENT
		}
		return ujsJobId;
	}

	private static AuthToken getCatalogAdminAuth(Map<String, String> config)
			throws IOException, AuthException {
		if (cachedCatalogAdminAuth == null) {
			String adminUser = config.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_USER);
			if (adminUser != null && adminUser.trim().isEmpty()) {
				adminUser = null;
			}
			String adminPwd = config.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_PWD);
			String adminToken = config.get(NarrativeJobServiceServer.CFG_PROP_CATALOG_ADMIN_TOKEN);
			if (adminToken != null && adminToken.trim().isEmpty()) {
				adminToken = null;
			}
			if (adminToken == null && adminUser == null) {
				throw new IllegalStateException("Catalog admin credentials are not defined in " +
						"configuration");
			}
			if (adminToken == null) {
				cachedCatalogAdminAuth = getAuth(config).login(adminUser,
						adminPwd == null ? "" : adminPwd).getToken();
			} else {
				cachedCatalogAdminAuth = getAuth(config).validateToken(adminToken);
			}
		}
		return cachedCatalogAdminAuth;
	}

	private static void checkModuleAndUpdateRunJobParams(
			final RunJobParams params,
			final Map<String, String> config)
			throws IOException, JsonClientException,
			AuthException {
		final String[] modMeth = params.getMethod().split("\\.");
		if (modMeth.length != 2) {
			throw new IllegalStateException("Illegal method name: " +
					params.getMethod());
		}
		final String moduleName = modMeth[0];

		CatalogClient catClient = getCatalogClient(config, false);
		final String servVer;
		if (params.getServiceVer() == null ||
				params.getServiceVer().isEmpty()) {
			servVer = RELEASE;
		} else {
			servVer = params.getServiceVer();
		}
		final ModuleVersion mv;
		try {
			mv = catClient.getModuleVersion(new SelectModuleVersion()
					.withModuleName(moduleName)
					.withVersion(servVer));
		} catch (ServerException se) {
			throw new IllegalArgumentException(String.format(
					"Error looking up module %s with version %s: %s",
					moduleName, servVer, se.getLocalizedMessage()));
		}
		params.setServiceVer(mv.getGitCommitHash());
		params.setAdditionalProperties(REQ_REL,
				RELEASE_TAGS.contains(servVer) ? servVer : null);
	}

	private static void checkWSObjects(
			final AuthToken token,
			final Map<String, String> config,
			final List<String> objrefs)
			throws UnauthorizedException, IOException, JsonClientException {
		if (objrefs == null || objrefs.isEmpty()) {
			return;
		}
		final String wsUrlstr = config.get(
				NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
		if (wsUrlstr == null || wsUrlstr.isEmpty())
			throw new IllegalStateException("Parameter " +
					NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL +
					" is not defined in configuration");
		final URL wsURL;
		try {
			wsURL = new URL(wsUrlstr);
		} catch (MalformedURLException mue) {
			throw new IllegalStateException("Config parameter " +
					NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL +
					" is invalid: " + wsUrlstr);
		}
		final WorkspaceClient wscli = new WorkspaceClient(wsURL, token);
		final List<ObjectSpecification> ois =
				new LinkedList<ObjectSpecification>();
		for (final String obj : objrefs) {
			ois.add(new ObjectSpecification().withRef(obj));
		}
		final List<Tuple11<Long, String, String, String, Long, String, Long,
				String, String, Long, Map<String, String>>> objinfo;
		try {
			objinfo = wscli.getObjectInfoNew(new GetObjectInfoNewParams()
					.withObjects(ois).withIgnoreErrors(1L));
		} catch (ServerException se) {
			if (se.getLocalizedMessage().indexOf(
					"Error on ObjectIdentity") != -1) {
				throw new ServerException(se.getLocalizedMessage().replace(
						"ObjectIdentity", "workspace reference"),
						se.getCode(), se.getName(), se.getData());
			} else {
				throw se;
			}
		}
		final Set<String> inaccessible = new LinkedHashSet<String>();
		for (int i = 0; i < objinfo.size(); i++) {
			if (objinfo.get(i) == null) {
				inaccessible.add(objrefs.get(i));
			}
		}
		if (!inaccessible.isEmpty()) {
			throw new IllegalArgumentException(String.format(
					"The workspace objects %s either don't exist or were " +
							"inaccessible to the user %s.",
					inaccessible, token.getUserName()));
		}
	}

	public static RunJobParams getJobInputParams(String ujsJobId, AuthToken auth,
												 Map<String, String> config, Map<String, String> resultConfig) throws Exception {
		UserAndJobStateClient ujsClient = getUjsClient(auth, config);
		ujsClient.getJobStatus(ujsJobId);
		final RunJobParams input = getJobInput(ujsJobId, config);
		if (resultConfig != null) {
			String[] propsToSend = {
					NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL,
					NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL,
					NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_SCRATCH,
					NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL,
					NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_DOCKER_URI,
					NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL,
					NarrativeJobServiceServer.CFG_PROP_REF_DATA_BASE,
					NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_CALLBACK_NETWORKS,
					NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL,
					NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL_V2,
					NarrativeJobServiceServer.CFG_PROP_TIME_BEFORE_EXPIRATION,
					NarrativeJobServiceServer.CFG_PROP_JOB_TIMEOUT_MINUTES,
					NarrativeJobServiceServer.CFG_PROP_DOCKER_JOB_TIMEOUT_SECONDS,
			};
			for (String key : propsToSend) {
				String value = config.get(key);
				if (value != null)
					resultConfig.put(key, value);
			}
			String kbaseEndpoint = config.get(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT);
			if (kbaseEndpoint == null) {
				String wsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_WORKSPACE_SRV_URL);
				if (!wsUrl.endsWith("/ws"))
					throw new IllegalStateException("Parameter " +
							NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT +
							" is not defined in configuration");
				kbaseEndpoint = wsUrl.replace("/ws", "");
			}
			resultConfig.put(NarrativeJobServiceServer.CFG_PROP_KBASE_ENDPOINT, kbaseEndpoint);
			propagateConfigUrl(config, NarrativeJobServiceServer.CFG_PROP_SELF_EXTERNAL_URL,
					resultConfig, kbaseEndpoint, "njs_wrapper");
			propagateConfigUrl(config, NarrativeJobServiceServer.CFG_PROP_SHOCK_URL,
					resultConfig, kbaseEndpoint, "shock-api");
			propagateConfigUrl(config, NarrativeJobServiceServer.CFG_PROP_HANDLE_SRV_URL,
					resultConfig, kbaseEndpoint, "handle_service");
			propagateConfigUrl(config, NarrativeJobServiceServer.CFG_PROP_SRV_WIZ_URL,
					resultConfig, kbaseEndpoint, "service_wizard");
			resultConfig.put(JobRunnerConstants.CFG_PROP_EE_SERVER_VERSION,
					NarrativeJobServiceServer.VERSION);
			String authNotSecure = config.get(
					NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM);
			resultConfig.put(
					NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM,
					String.valueOf("true".equals(authNotSecure)));
		}
		return input;
	}

	private static void propagateConfigUrl(Map<String, String> config, String paramName,
										   Map<String, String> resultConfig, String endpointUrl, String optionalSuffix) {
		String paramUrl = config.get(paramName);
		if (paramUrl == null)
			paramUrl = endpointUrl + "/" + optionalSuffix;
		resultConfig.put(paramName, paramUrl);
	}

	public static List<String> updateJob(UpdateJobParams params, AuthToken auth,
										 Map<String, String> config) throws Exception {
		String ujsJobId = params.getJobId();
		UserAndJobStateClient ujsClient = getUjsClient(auth, config);
		String jobOwner = ujsClient.getJobOwner(ujsJobId);
		if (auth == null || !jobOwner.equals(auth.getUserName()))
			throw new IllegalStateException("Only owner of the job can update it");
		Tuple7<String, String, String, Long, String, Long, Long> jobStatus =
				ujsClient.getJobStatus(ujsJobId);
		if (params.getIsStarted() == null || params.getIsStarted() != 1L)
			throw new IllegalStateException("Method is currently supported only for " +
					"switching jobs into stated state");
		List<String> ret = new ArrayList<String>();
        final RunJobParams input = getJobInput(ujsJobId, config);
        final String jobstage = jobStatus.getE2();
        final String status = jobStatus.getE3();
        if ("started".equals(jobstage)) {

// This message appears in the logs twice for some reason.. Removing it
// TODO Remove this section or find out why logs happen twice
//
//
//            String message = String.format(
//                    "Updating job status for %s from '%s'  to 'in-progress' (%s)",
//                    ujsJobId, status, new Date().getTime() );
//            ret.add(message);
//            List<LogLine> lines = new ArrayList<>();
//            lines.add(new LogLine().withLine(message).withIsError(0L));
//            addJobLogs(ujsJobId, lines, auth, config);
//
//
//			String tomorrow = DATE_FORMATTER.print(new DateTime().plusDays(1));

            ujsClient.updateJob(ujsJobId, auth.getToken(), "in-progress", null);
            //is this the fix???
			updateTaskExecTime(ujsJobId, config, false);
            return ret;


        }
        return ret;

//        try {
//            ujsClient.startJob(ujsJobId, auth.getToken(), "in-progress",
//                    "Execution engine job for " + input.getMethod(),
//                    new InitProgress().withPtype("none"), null);
//
//        } catch (ServerException se) {
//            throw new IllegalStateException(String.format(
//                    "Job %s couldn't be started and is in state " +
//                            "%s. Server stacktrace:\n%s",
//                    ujsJobId, jobstage, se.getData()), se);
//        }
//
//        return ret;
    }

	public static void finishJob(
			final String ujsJobId,
			final FinishJobParams params,
			final AuthToken auth,
			final ErrorLogger log,
			final Map<String, String> config)
			throws Exception {
		if (params.getIsCanceled() == null && params.getIsCancelled() != null) {
			params.setIsCanceled(params.getIsCancelled());
		}
		final UserAndJobStateClient ujsClient = getUjsClient(auth, config);
		final Tuple7<String, String, String, Long, String, Long,
				Long> jobStatus = ujsClient.getJobStatus(ujsJobId);

		if (jobStatus.getE6() != null && jobStatus.getE6() == 1L) {
			// Job was already done
			final List<LogLine> lines = new ArrayList<LogLine>();
			lines.add(new LogLine().withLine(
					"Attempt to finish already completed job")
					.withIsError(1L));
			addJobLogs(ujsJobId, lines, auth, config);
			return;
		}
		@SuppressWarnings("unchecked") final Map<String, Object> jobOutput =
				UObject.transformObjectToObject(params, Map.class);
		//should never trigger since the local method runner limits uploads to
		//15k
		checkObjectLength(jobOutput, MAX_IO_BYTE_SIZE, "Output", ujsJobId);
		SanitizeMongoObject.sanitize(jobOutput);
		// Updating UJS job state
		if (params.getIsCanceled() != null &&
				params.getIsCanceled() == 1L) {
			// will throw an error here if user doesn't have rights to cancel
			ujsClient.cancelJob(ujsJobId, "canceled by user");
			getDb(config).addExecTaskResult(ujsJobId, jobOutput);

			try{
				updateTaskExecTime(ujsJobId, config, true);
			}
			catch (NullPointerException e){
			}
			return;
		}
		final String jobOwner = ujsClient.getJobOwner(ujsJobId);
		if (auth == null || !jobOwner.equals(auth.getUserName())) {
			throw new IllegalStateException(
					"Only the owner of a job can complete it");
		}
		getDb(config).addExecTaskResult(ujsJobId, jobOutput);
		updateTaskExecTime(ujsJobId, config, true);
		if (jobStatus.getE2().equals("created")) {
			// job hasn't started yet. Need to put it in started state to
			// complete it
			try {
				ujsClient.startJob(ujsJobId, auth.getToken(),
						"starting job so that it can be finished",
						"as state", new InitProgress().withPtype("none"),
						null);
			} catch (ServerException se) {
				// ignore and continue if the job was just started
			}
		}
		if (params.getError() != null) {
			String status = params.getError().getMessage();
			if (status == null)
				status = "Unknown error";
			if (status.length() > 200)
				status = status.substring(0, 197) + "...";
			ujsClient.completeJob(ujsJobId, auth.getToken(), status,
					params.getError().getError(), null);
		} else {
			ujsClient.completeJob(ujsJobId, auth.getToken(), "done", null,
					new Results());
		}
		// let's make a call to catalog sending execution stats
		try {
			final AppInfo info = getAppInfo(ujsJobId, config);
			final RunJobParams input = getJobInput(ujsJobId, config);
			String[] parts = input.getMethod().split(Pattern.quote("."));
			String funcModuleName = parts.length > 1 ? parts[0] : null;
			String funcName = parts.length > 1 ? parts[1] : parts[0];
			String gitCommitHash = input.getServiceVer();
			Long[] execTimes = getTaskExecTimes(ujsJobId, config);

			long creationTime = -1L;
			long execStartTime = -1L;
			long finishTime = -1L;


			if (execTimes != null) {
				creationTime = execTimes[0];
				execStartTime = execTimes[1];
				finishTime = execTimes[2];
			}


			boolean isError = params.getError() != null;
			String errorMessage = null;
			try {
				sendExecStatsToCatalog(auth.getUserName(), info.uiModuleName,
						info.methodSpecId, funcModuleName, funcName,
						gitCommitHash, creationTime, execStartTime, finishTime,
						isError, ujsJobId, config);
			} catch (ServerException ex) {
				errorMessage = ex.getData();
				if (errorMessage == null)
					errorMessage = ex.getMessage();
				if (errorMessage == null)
					errorMessage = "Unknown server error";
			} catch (Exception ex) {
				errorMessage = ex.getMessage();
				if (errorMessage == null)
					errorMessage = "Unknown error";
			}
			if (errorMessage != null) {
				String message = "Error sending execution stats to catalog (" +
						auth.getUserName() + ", " + info.uiModuleName + ", " + info.methodSpecId +
						", " + funcModuleName + ", " + funcName + ", " + gitCommitHash + ", " +
						creationTime + ", " + execStartTime + ", " + finishTime + ", " + isError +
						"): " + errorMessage;
				System.err.println(message);
				if (log != null)
					log.logErr(message);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			if (log != null)
				log.logErr(ex);
		}
	}

	private static void sendExecStatsToCatalog(String userId, String uiModuleName,
											   String methodSpecId, String funcModuleName, String funcName, String gitCommitHash,
											   long creationTime, long execStartTime, long finishTime, boolean isError,
											   String jobId, Map<String, String> config) throws Exception {
		CatalogClient catCl = getCatalogClient(config, true);
		catCl.logExecStats(new LogExecStatsParams().withUserId(userId)
				.withAppModuleName(uiModuleName).withAppId(methodSpecId)
				.withFuncModuleName(funcModuleName).withFuncName(funcName)
				.withGitCommitHash(gitCommitHash).withCreationTime(creationTime / 1000.0)
				.withExecStartTime(execStartTime / 1000.0).withFinishTime(finishTime / 1000.0)
				.withIsError(isError ? 1L : 0L).withJobId(jobId));
	}

	public static int addJobLogs(String ujsJobId, List<LogLine> lines,
								 AuthToken authPart, Map<String, String> config) throws Exception {
		UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
		ujsClient.getJobStatus(ujsJobId);
		ExecEngineMongoDb db = getDb(config);
		ExecLog dbLog = db.getExecLog(ujsJobId);



		if (dbLog == null) {
			dbLog = new ExecLog();
			dbLog.setUjsJobId(ujsJobId);
			dbLog.setOriginalLineCount(0);
			dbLog.setStoredLineCount(0);
			dbLog.setLines(new ArrayList<ExecLogLine>());
			db.insertExecLog(dbLog);
		}
		if (dbLog.getOriginalLineCount() > dbLog.getStoredLineCount()) {
			// Error with out of space happened previously. So we just update line count.
			db.updateExecLogOriginalLineCount(ujsJobId, dbLog.getOriginalLineCount() + lines.size());
			return dbLog.getStoredLineCount();
		}
		int linePos = dbLog.getOriginalLineCount();
		try {
			int partSize = 1000;
			int partCount = (lines.size() + partSize - 1) / partSize;
			for (int i = 0; i < partCount; i++) {
				int newLineCount = Math.min((i + 1) * partSize, lines.size());
				List<ExecLogLine> dbLines = new ArrayList<ExecLogLine>();
				for (int j = i * partSize; j < newLineCount; j++) {
					LogLine line = lines.get(j);
					String text = line.getLine();
					if (text.length() > MAX_LOG_LINE_LENGTH)
						text = text.substring(0, MAX_LOG_LINE_LENGTH - 3) + "...";
					ExecLogLine dbLine = new ExecLogLine();
					dbLine.setLinePos(linePos);
					dbLine.setLine(text);
					dbLine.setIsError((long) line.getIsError() == 1L);
					dbLines.add(dbLine);
					linePos++;
				}
				db.updateExecLogLines(ujsJobId, linePos, dbLines);
			}
			return linePos;
		} catch (WriteConcernException ex) {
			ex.getCode();
			db.updateExecLogOriginalLineCount(ujsJobId, dbLog.getOriginalLineCount() + lines.size());
			return dbLog.getStoredLineCount();
		}
	}

	public static GetJobLogsResults getJobLogs(String ujsJobId, Long skipLines,
											   AuthToken authPart, Set<String> admins, Map<String, String> config) throws Exception {
		boolean isAdmin = admins != null && admins.contains(authPart.getUserName());
		if (!isAdmin) {
			// If it's not admin then let's check if there is permission in UJS
			UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
			ujsClient.getJobStatus(ujsJobId);
		}
		ExecEngineMongoDb db = getDb(config);
		ExecLog dbLog = db.getExecLog(ujsJobId);
		List<LogLine> lines;
		if (dbLog == null || (skipLines != null && dbLog.getStoredLineCount() <= skipLines)) {
			lines = Collections.<LogLine>emptyList();
		} else {
			lines = new ArrayList<LogLine>();
			int from = skipLines == null ? 0 : (int) (long) skipLines;
			int count = dbLog.getStoredLineCount() - from;
			for (ExecLogLine dbLine : db.getExecLogLines(ujsJobId, from, count)) {
				lines.add(new LogLine().withLine(dbLine.getLine())
						.withIsError(dbLine.getIsError() ? 1L : 0L));
			}
		}
		return new GetJobLogsResults().withLines(lines)
				.withLastLineNumber((long) lines.size() + (skipLines == null ? 0L : skipLines));
	}

	private static ConfigurableAuthService getAuth(Map<String, String> config)
			throws IOException, AuthException {
		String authUrl = config.get(NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_URL);
		String authAllowInsecure = config.get(NarrativeJobServiceServer.CFG_PROP_AUTH_SERVICE_ALLOW_INSECURE_URL_PARAM);
		try {
			final AuthConfig c = new AuthConfig().withKBaseAuthServerURL(new URL(authUrl));
			if ("true".equals(authAllowInsecure)) {
				c.withAllowInsecureURLs(true);
			}
			return new ConfigurableAuthService(c);
		} catch (URISyntaxException ex) {
			throw new AuthException(ex.getMessage(), ex);
		}
	}

	private static AuthToken getAweAdminAuth(Map<String, String> config)
			throws IOException, AuthException {
		if (cachedAweAdminAuth == null) {
			String aweAdminUser = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_READONLY_ADMIN_USER);
			if (aweAdminUser != null && aweAdminUser.trim().isEmpty()) {
				aweAdminUser = null;
			}
			String aweAdminPwd = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_READONLY_ADMIN_PWD);
			String aweAdminToken = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_READONLY_ADMIN_TOKEN);
			if (aweAdminToken != null && aweAdminToken.trim().isEmpty()) {
				aweAdminToken = null;
			}
			if (aweAdminToken == null && aweAdminUser == null) {
				throw new IllegalStateException("AWE admin creadentials are not defined in configuration");
			}
			// Use the config token if provided, otherwise generate one
			// userid/password may be deprecated in the future
			if (aweAdminToken == null) {
				cachedAweAdminAuth = getAuth(config).login(aweAdminUser,
						aweAdminPwd == null ? "" : aweAdminPwd).getToken();
			} else {
				cachedAweAdminAuth = getAuth(config).validateToken(aweAdminToken);
			}
		}
		return cachedAweAdminAuth;
	}

	public static CheckJobCanceledResult checkJobCanceled(
			final CancelJobParams params,
			final AuthToken authPart,
			final Map<String, String> config) throws Exception {
		final String ujsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
		if (params == null) {
			throw new NullPointerException("No parameters supplied to method");
		}
		final String jobId = params.getJobId();
		if (jobId == null || jobId.trim().isEmpty()) {
			throw new IllegalArgumentException("No job id supplied");
		}
		final UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
		final Tuple7<String, String, String, Long, String, Long, Long> jobStatus =
				ujsClient.getJobStatus(jobId);
		return new CheckJobCanceledResult().withJobId(jobId).withUjsUrl(ujsUrl)
				// null if job not started yet
				.withFinished(jobStatus.getE6() == null ? 0 : jobStatus.getE6())
				.withCanceled(APP_STATE_CANCELED.equals(jobStatus.getE2()) ? 1L : 0L);
	}

	public static String getJobState(String ujsJobId) throws Exception {
		/**
		 * Get job state from a condor status based on
		 * http://pages.cs.wisc.edu/~adesmet/status.html
		 * @param jobID ujsJobId to get job state for
		 * @return Return an appropriate status constant based on condor status code
		 */
		String jobState = CondorUtils.getJobState(ujsJobId);
		int retries = 10;
		if (jobState == null) {
			while (retries > 0 && jobState == null) {
				retries--;
				jobState = CondorUtils.getJobState(ujsJobId);
			}
		}
		if (jobState == null) {
			return "unavailable";
		}
		switch (jobState) {
			case "0":
				return APP_STATE_QUEUED; //Maybe need to return a new state here?
			case "1":
				return APP_STATE_QUEUED; //Maybe need to return a new state here?
			case "2":
				return APP_STATE_STARTED;
			case "3":
				return APP_STATE_CANCELED;
			case "4":
				return APP_STATE_DONE;
			case "5":
				return APP_STATE_QUEUED; //Maybe need to return a new state here?
			default:
				return APP_STATE_ERROR;
		}
	}

	@SuppressWarnings("unchecked")
	public static JobState checkJob(String jobId, AuthToken authPart,
									Map<String, String> config) throws Exception {
		if (config.get(NarrativeJobServiceServer.CFG_PROP_CONDOR_MODE).equals("1")) {
			return checkJobCondor(jobId, authPart, config);
		} else {
			return checkJobAwe(jobId, authPart, config);
		}
	}

	@SuppressWarnings("unchecked")
	public static List<JobState> listStatusByWorkspace(String workspace, AuthToken authPart,
													   Map<String, String> config) throws Exception {

		List<JobState> js = new ArrayList<>();
		UserAndJobStateClient ujsClient = getUjsClient(authPart, config);

		List<String> authParams = new ArrayList<>();
		authParams.add(workspace);

		for (Tuple13<String, Tuple2<String, String>, String, String, String,
				Tuple3<String, String, String>, Tuple3<Long, Long, String>,
				Long, Long, Tuple2<String, String>, Map<String, String>,
				String, Results> j : ujsClient.listJobs2(new ListJobsParams().withAuthstrat("kbaseworkspace").withAuthparams(authParams))) {
					JobState njsJobState = checkJobCondor(j, authPart, config);
					js.add(njsJobState);
		}
		return js;
	}




	@SuppressWarnings("unchecked")
	public static JobState checkJobAwe(String jobId, AuthToken authPart,
									   Map<String, String> config) throws Exception {
		String ujsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
		JobState returnVal = new JobState().withJobId(jobId).withUjsUrl(ujsUrl);
		String aweJobId = getAweTaskAweJobId(jobId, config);
		returnVal.getAdditionalProperties().put("awe_job_id", aweJobId);
		UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
		Tuple7<String, String, String, Long, String, Long, Long> jobStatus =
				ujsClient.getJobStatus(jobId);
		returnVal.setStatus(new UObject(jobStatus));
		boolean complete = jobStatus.getE6() != null && jobStatus.getE6() == 1L;
		FinishJobParams params = null;
		if (complete) {
			params = getJobOutput(jobId, authPart, config);
		}
		if (params == null) {
			// We should consult AWE for case the job was killed or gone with no reason.
			AuthToken aweAdminToken = getAweAdminAuth(config);
			Map<String, Object> aweData = null;
			String aweState = null;
			String aweServerUrl = getAweServerURL(config);
			try {
				Map<String, Object> aweJob = AweUtils.getAweJobDescr(aweServerUrl, aweJobId,
						aweAdminToken);
				aweData = (Map<String, Object>) aweJob.get("data");
				if (aweData != null) {
					aweState = (String) aweData.get("state");
				}
			} catch (Exception ex) {
				throw new IllegalStateException("Error checking AWE job (id=" + aweJobId + ") " +
						"for ujs-id=" + jobId + " (" + ex.getMessage() + ")", ex);
			}
			if (aweState == null) {
				final String aweDataStr = new ObjectMapper().writeValueAsString(aweData);
				throw new IllegalStateException("Error checking AWE job (id=" + aweJobId + ") " +
						"for ujs-id=" + jobId + " - state is null. AWE returned:\n " + aweDataStr);
			}
			if ((!aweState.equals("init")) && (!aweState.equals("queued")) &&
					(!aweState.equals("in-progress"))) {
				// Let's double-check, what if UJS job was marked as complete while we checked AWE?
				jobStatus = ujsClient.getJobStatus(jobId);
				complete = jobStatus.getE6() != null && jobStatus.getE6() == 1L;
				if (complete) { // Yes, we are switching to "complete" scenario
					returnVal.setStatus(new UObject(jobStatus));
					params = getJobOutput(jobId, authPart, config);
				} else {
					if (aweState.equals("suspend")) {
						throw new IllegalStateException("FATAL error in AWE job (" + aweState +
								" for id=" + aweJobId + ")" + (jobStatus.getE2().equals("created") ?
								" whereas job script wasn't started at all" : ""));
					}
					throw new IllegalStateException(String.format(
							"Unexpected AWE job state: %s. Job id: %s. Awe job id: %s.",
							aweState, jobId, aweJobId));
				}
			}
			if (!complete) {
				returnVal.getAdditionalProperties().put("awe_job_state", aweState);
				returnVal.setFinished(0L);
				String stage = jobStatus.getE2();
				if (stage != null && stage.equals("started")) {
					returnVal.setJobState(APP_STATE_STARTED);
				} else {
					returnVal.setJobState(APP_STATE_QUEUED);
					try {
						Map<String, Object> aweResp = AweUtils.getAweJobPosition(aweServerUrl,
								aweJobId, aweAdminToken);
						Map<String, Object> posData = (Map<String, Object>) aweResp.get("data");
						if (posData != null && posData.containsKey("position"))
							returnVal.setPosition(UObject.transformObjectToObject(posData.get("position"), Long.class));
					} catch (Exception ignore) {
					}
				}
			}
		}
		if (complete) {
			boolean isCanceled = params.getIsCanceled() == null ? false :
					(params.getIsCanceled() == 1L);
			returnVal.setFinished(1L);
			returnVal.setCanceled(isCanceled ? 1L : 0L);
			// Next line is here for backward compatibility:
			returnVal.setCancelled(isCanceled ? 1L : 0L);
			returnVal.setResult(params.getResult());
			returnVal.setError(params.getError());
			if (params.getError() != null) {
				returnVal.setJobState(APP_STATE_ERROR);
			} else if (isCanceled) {
				returnVal.setJobState(APP_STATE_CANCELLED);
			} else {
				returnVal.setJobState(APP_STATE_DONE);
			}
		}
		Long[] execTimes = getTaskExecTimes(jobId, config);
		if (execTimes != null) {
			if (execTimes[0] != null)
				returnVal.withCreationTime(execTimes[0]);
			if (execTimes[1] != null)
				returnVal.withExecStartTime(execTimes[1]);
			if (execTimes[2] != null)
				returnVal.withFinishTime(execTimes[2]);
		}
		return returnVal;
	}






//
//	public static List<JobState> checkWorkspaceJobs(String workspaceId, AuthToken authPart,
//                                          Map<String, String> config) throws Exception{
//
//        List<JobState> states = new ArrayList<>();
//        UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
//
//
//		ListJobsParams p = new ListJobsParams().withAuthstrat("kbaseworkspace").withAuthparams();
//		p.
//        ujsClient.listJobs2()
//
//		nar_jobs = clients.get('user_and_job_state').list_jobs2({
//				'authstrat': 'kbaseworkspace',
//				'authparams': [str(ws_id)]
//            })
//
//
//    }

	@SuppressWarnings("unchecked")
	public static JobState checkJobCondor(
		Tuple13<String, Tuple2<String, String>, String, String, String, Tuple3<String, String, String>, Tuple3<Long, Long, String>, Long, Long, Tuple2<String, String>, Map<String, String>, String, Results> jobInfo,
		AuthToken authPart,
		Map<String, String> config) throws Exception {
		// That jobInfo Tuple is a monster. Whoa.
		String jobId = jobInfo.getE1();
		String ujsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
		JobState returnVal = new JobState().withJobId(jobId).withUjsUrl(ujsUrl);
		String[] subJobs = getDb(config).getSubJobIds(jobId);
		returnVal.getAdditionalProperties().put("sub_jobs", subJobs);

		boolean complete = jobInfo.getE8() != null && jobInfo.getE8() == 1L;
		FinishJobParams params = null;
		if (complete) {
			params = getJobOutput(jobId, authPart, config);

			boolean isCanceled = params.getIsCanceled() == null ? false :
					(params.getIsCanceled() == 1L);


			returnVal.setFinished(1L);
			returnVal.setCanceled(isCanceled ? 1L : 0L);
			// Next line is here for backward compatibility:
			returnVal.setCancelled(isCanceled ? 1L : 0L);
			returnVal.setResult(params.getResult());
			returnVal.setError(params.getError());
			if (params.getError() != null) {
				returnVal.setJobState(APP_STATE_ERROR);
			} else if (isCanceled) {
				returnVal.setJobState(APP_STATE_CANCELLED);
			} else {
				returnVal.setJobState(APP_STATE_DONE);
			}
		} else {
			returnVal.setFinished(0L);

			// (A string that describes the stage of processing of the job.
			// One of 'created', 'started', 'completed', 'canceled' or 'error'.),
			String currentStage = jobInfo.getE4();

			//parameter "status" of original type "job_status"
			// (A job status string supplied by the reporting service. No more than 200 characters.)
			String currentStatus = jobInfo.getE5();

			if (currentStatus.equals("running") || currentStatus.equals("in-progress")) {
				returnVal.setJobState("in-progress");
				returnVal.getAdditionalProperties().put("awe_job_state", "in-progress");
				returnVal.getAdditionalProperties().put("job_state", "in-progress");
			}
			else if (currentStage.equals(APP_STATE_CANCELED) || currentStage.equals(APP_STATE_CANCELLED)) {
				returnVal.setFinished(1L);
				returnVal.setCanceled(1L);
				returnVal.setJobState(APP_STATE_CANCELED);
				returnVal.getAdditionalProperties().put("awe_job_state", APP_STATE_CANCELED);
				returnVal.getAdditionalProperties().put("job_state", APP_STATE_CANCELED);
			}
			else {
				returnVal.setJobState(APP_STATE_QUEUED);
				returnVal.getAdditionalProperties().put("awe_job_state", APP_STATE_QUEUED);
				returnVal.getAdditionalProperties().put("job_state", APP_STATE_QUEUED);
			}

		}

		Tuple7<String, String, String, Long, String, Long, Long> jobStatus = new Tuple7<String, String, String, Long, String, Long, Long>();
		jobStatus.setE1(jobInfo.getE6().getE2());
		jobStatus.setE2(jobInfo.getE4());
		jobStatus.setE3(jobInfo.getE5());
		jobStatus.setE4(jobInfo.getE7().getE1());
		jobStatus.setE5(jobInfo.getE6().getE3());
		jobStatus.setE6(jobInfo.getE8());
		jobStatus.setE7(jobInfo.getE9());
		returnVal.setStatus(new UObject(jobStatus));

		Long[] execTimes = getTaskExecTimes(jobId, config);
		if (execTimes != null) {
			if (execTimes[0] != null)
				returnVal.withCreationTime(execTimes[0]);
			if (execTimes[1] != null)
				returnVal.withExecStartTime(execTimes[1]);
			if (execTimes[2] != null)
				returnVal.withFinishTime(execTimes[2]);
		}
		return returnVal;
	}

	@SuppressWarnings("unchecked")
	public static JobState checkJobCondor(String jobId, AuthToken authPart,
										  Map<String, String> config) throws Exception {
		String ujsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
		JobState returnVal = new JobState().withJobId(jobId).withUjsUrl(ujsUrl);
		UserAndJobStateClient ujsClient = getUjsClient(authPart, config);
		Tuple7<String, String, String, Long, String, Long, Long> jobStatus =
				ujsClient.getJobStatus(jobId);


//		(1) parameter "last_update" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)),
//		(2) parameter "stage" of original type "job_stage" (A string that describes the stage of processing of the job. One of 'created', 'started', 'completed', 'canceled' or 'error'.),
//		(3) parameter "status" of original type "job_status" (A job status string supplied by the reporting service. No more than 200 characters.),
//		(4) parameter "progress" of original type "total_progress" (The total progress of a job.),
//		(5) parameter "est_complete" of original type "timestamp" (A time in the format YYYY-MM-DDThh:mm:ssZ, where Z is the difference in time to UTC in the format +/-HHMM, eg: 2012-12-17T23:24:06-0500 (EST time) 2013-04-03T08:56:32+0000 (UTC time)),
//		(6) parameter "complete" of original type "boolean" (A boolean. 0 = false, other = true.),
//		(7) parameter "error" of original type "boolean" (A boolean. 0 = false, other = true.)

		String[] subJobs = getDb(config).getSubJobIds(jobId);
		returnVal.getAdditionalProperties().put("sub_jobs", subJobs);


		boolean complete = jobStatus.getE6() != null && jobStatus.getE6() == 1L;
		FinishJobParams params = null;
		if (complete) {
			params = getJobOutput(jobId, authPart, config);

			boolean isCanceled = params.getIsCanceled() == null ? false :
					(params.getIsCanceled() == 1L);


			returnVal.setFinished(1L);
			returnVal.setCanceled(isCanceled ? 1L : 0L);
			// Next line is here for backward compatibility:
			returnVal.setCancelled(isCanceled ? 1L : 0L);
			returnVal.setResult(params.getResult());
			returnVal.setError(params.getError());
			if (params.getError() != null) {
				returnVal.setJobState(APP_STATE_ERROR);
			} else if (isCanceled) {
				returnVal.setJobState(APP_STATE_CANCELLED);
			} else {
				returnVal.setJobState(APP_STATE_DONE);
			}
		} else {
			returnVal.setFinished(0L);

			// (A string that describes the stage of processing of the job.
			// One of 'created', 'started', 'completed', 'canceled' or 'error'.),
			String currentStage = jobStatus.getE2();

			//parameter "status" of original type "job_status"
			// (A job status string supplied by the reporting service. No more than 200 characters.)
			String currentStatus = jobStatus.getE3();

			if (currentStatus.equals("running") || currentStatus.equals("in-progress")) {
				returnVal.setJobState("in-progress");
				returnVal.getAdditionalProperties().put("awe_job_state", "in-progress");
				returnVal.getAdditionalProperties().put("job_state", "in-progress");
			}
			else if (currentStage.equals(APP_STATE_CANCELED) || currentStage.equals(APP_STATE_CANCELLED)) {
				returnVal.setFinished(1L);
				returnVal.setCanceled(1L);
				returnVal.setJobState(APP_STATE_CANCELED);
				returnVal.getAdditionalProperties().put("awe_job_state", APP_STATE_CANCELED);
				returnVal.getAdditionalProperties().put("job_state", APP_STATE_CANCELED);
			}
			else {
				returnVal.setJobState(APP_STATE_QUEUED);
				returnVal.getAdditionalProperties().put("awe_job_state", APP_STATE_QUEUED);
				returnVal.getAdditionalProperties().put("job_state", APP_STATE_QUEUED);
			}

		}

		returnVal.setStatus(new UObject(jobStatus));

		Long[] execTimes = getTaskExecTimes(jobId, config);
		if (execTimes != null) {
			if (execTimes[0] != null)
				returnVal.withCreationTime(execTimes[0]);
			if (execTimes[1] != null)
				returnVal.withExecStartTime(execTimes[1]);
			if (execTimes[2] != null)
				returnVal.withFinishTime(execTimes[2]);
		}
		return returnVal;
	}



	public static CheckJobsResults checkJobs(CheckJobsParams params, AuthToken auth,
	        Map<String, String> config) throws Exception {
	    if (params.getJobIds() == null) {
	        throw new IllegalStateException("Input parameters should include 'job_ids' property");
	    }
	    boolean withJobParams = params.getWithJobParams() != null &&
	            params.getWithJobParams() == 1L;
	    Map<String, RunJobParams> jobParams = new LinkedHashMap<String, RunJobParams>();
	    Map<String, JsonRpcError> checkError = new LinkedHashMap<String, JsonRpcError>();
	    CheckJobsResults ret = new CheckJobsResults().withJobStates(
	            new LinkedHashMap<String, JobState>()).withCheckError(checkError);
	    for (String jobId : params.getJobIds()) {
	        try {
	            ret.getJobStates().put(jobId, checkJob(jobId, auth, config));
	            if (withJobParams) {
	                jobParams.put(jobId, getJobInputParams(jobId, auth, config, null));
	            }
	        } catch (Exception ex) {
	            JsonRpcError error = null;
	            if (ex instanceof ServerException) {
	                ServerException se = (ServerException)ex;
	                error = new JsonRpcError().withCode((long)se.getCode()).withName(se.getName())
	                        .withMessage(se.getMessage()).withError(se.getData());
	            } else {
	                StringWriter sw = new StringWriter();
	                PrintWriter pw = new PrintWriter(sw);
	                ex.printStackTrace(pw);
	                pw.close();
	                error = new JsonRpcError().withCode(-32603L)
	                        .withName(ex.getClass().getSimpleName()).withMessage(ex.getMessage())
	                        .withError(sw.toString());
	            }
	            checkError.put(jobId, error);
	        }
	    }
	    if (withJobParams) {
	        ret.withJobParams(jobParams);
	    }
	    return ret;
	}

	public static void cancelJob(CancelJobParams params, AuthToken auth,
	        Map<String, String> config) throws Exception {
	    FinishJobParams finishParams = new FinishJobParams().withIsCanceled(1L);
	    // Next line is here for backward compatibility:
	    finishParams.setIsCancelled(1L);
	    finishJob(params.getJobId(), finishParams, auth, null, config);
	    CondorUtils.condorRemoveJobRangeAsync(params.getJobId());
	}

	private static UserAndJobStateClient getUjsClient(AuthToken auth,
			Map<String, String> config) throws Exception {
		String jobSrvUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
		if (jobSrvUrl == null)
			throw new IllegalStateException("Parameter '" +
					NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL +
					"' is not defined in configuration");
		UserAndJobStateClient ret = new UserAndJobStateClient(new URL(jobSrvUrl), auth);
		ret.setIsInsecureHttpConnectionAllowed(true);
		ret.setAllSSLCertificatesTrusted(true);
		return ret;
	}

	private static String getAweServerURL(Map<String, String> config) throws Exception {
		String aweUrl = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_SRV_URL);
		if (aweUrl == null)
			throw new IllegalStateException("Parameter '" +
					NarrativeJobServiceServer.CFG_PROP_AWE_SRV_URL +
					"' is not defined in configuration");
		return aweUrl;
	}

	public static CatalogClient getCatalogClient(Map<String, String> config,
			boolean asAdmin)
			        throws UnauthorizedException, IOException,
			        AuthException {
		final String catalogUrl = getRequiredConfigParam(config,
				NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL);
		final CatalogClient ret;
		final URL catURL;
		try {
			catURL = new URL(catalogUrl);
		} catch (MalformedURLException mue) {
			throw new IllegalStateException("Config parameter " +
					NarrativeJobServiceServer.CFG_PROP_CATALOG_SRV_URL +
					" is invalid: " + catalogUrl);
		}
		if (asAdmin) {
			ret = new CatalogClient(catURL, getCatalogAdminAuth(config));
		} else {
			ret = new CatalogClient(catURL);
		}
		ret.setIsInsecureHttpConnectionAllowed(true);
		ret.setAllSSLCertificatesTrusted(true);
		return ret;
	}

	private static String getRequiredConfigParam(Map<String, String> config, String param) {
		String ret = config.get(param);
		if (ret == null)
			throw new IllegalStateException("Parameter '" + param +
					"' is not defined in configuration");
		return ret;
	}

	public static ExecEngineMongoDb getDb(Map<String, String> config) throws Exception {
		if (db == null)
			db = NarrativeJobServiceServer.getMongoDb(config);
		return db;
	}

	private static void addAweTaskDescription(
			final String ujsJobId,
			final String aweJobId,
			final Map<String, Object> jobInput,
			final String appJobId,
			final Map<String, String> config) throws Exception {
		SanitizeMongoObject.sanitize(jobInput);
		ExecEngineMongoDb db = getDb(config);
		ExecTask dbTask = new ExecTask();
		dbTask.setUjsJobId(ujsJobId);
		dbTask.setAweJobId(aweJobId);
		dbTask.setJobInput(jobInput);
		dbTask.setCreationTime(System.currentTimeMillis());
		dbTask.setAppJobId(appJobId);
		db.insertExecTask(dbTask);
	}


	/**
	 * Saves state in mongodb to allow the job to communicate its status
	 * @param ujsJobId (UJS ID)
	 * @param jobId (Scheduler ID, such as condor job range)
	 * @param jobInput (Runjob Params)
	 * @param appJobId
	 * @param schedulerType (Scheduler Type, such as Condor or Awe)
	 * @param parentJobId (ID of Parent Job)
	 * @param config (Configuration File)
	 * @throws Exception
	 */
	private static void saveTask(
			final String ujsJobId,
			final String jobId,
			final Map<String, Object> jobInput,
			final String appJobId,
			final String schedulerType,
			final String parentJobId,
			final Map<String, String> config) throws Exception {

		SanitizeMongoObject.sanitize(jobInput);
		ExecEngineMongoDb db = getDb(config);
		ExecTask dbTask = new ExecTask();
		dbTask.setUjsJobId(ujsJobId);
		dbTask.setJobInput(jobInput);
		dbTask.setCreationTime(System.currentTimeMillis());
		dbTask.setAppJobId(appJobId);
		dbTask.setSchdulerType(schedulerType);
		dbTask.setTaskId(jobId);
		dbTask.setParentJobId(parentJobId);
		db.insertExecTask(dbTask);
	}

	private static ExecTask getTaskDescription(String ujsJobId, Map<String, String> config) throws Exception {
		ExecEngineMongoDb db = getDb(config);
		ExecTask dbTask = db.getExecTask(ujsJobId);
		if (dbTask == null)
			throw new IllegalStateException("AWE task wasn't found in DB for jobid=" + ujsJobId);
		return dbTask;
	}

	private static String getAweTaskAweJobId(String ujsJobId, Map<String, String> config) throws Exception {
		return getTaskDescription(ujsJobId, config).getAweJobId();
	}

	/**
	 * Update run time, finish time, and queue time.
	 * */
	private static void updateTaskExecTime(String ujsJobId, Map<String, String> config, boolean finishTime) throws Exception {
		ExecEngineMongoDb db = getDb(config);
		ExecTask dbTask = db.getExecTask(ujsJobId);

		Long finishTimeMs = dbTask.getFinishTime();

		//Not Null = already finished
		if(finishTimeMs != null)
			db.updateExecTaskTime(ujsJobId, finishTime, finishTimeMs);
		else //Not finished, updating
			db.updateExecTaskTime(ujsJobId, finishTime, System.currentTimeMillis());

		//Refresh the task in order to update the Queue Time
		dbTask = db.getExecTask(ujsJobId);
		Long execTimeMS = dbTask.getExecStartTime();
		Long queueTime = dbTask.getQueueTime();
		if(queueTime == null && execTimeMS != null){
			Long creationTimeMS = dbTask.getCreationTime();
			Long queueTimeMS = execTimeMS - creationTimeMS;
			db.updateQueueTaskTime(ujsJobId, queueTimeMS);
		}
	}

	/**
	 * Update run finish time
	 * */
	private static void updateTaskExecTimeCancel(String ujsJobId, Map<String, String> config) throws Exception {
		ExecEngineMongoDb db = getDb(config);
		db.updateExecTaskTime(ujsJobId, true, System.currentTimeMillis());
	}

	private static Long[] getTaskExecTimes(String ujsJobId, Map<String, String> config) throws Exception {
		ExecEngineMongoDb db = getDb(config);
		ExecTask dbTask = db.getExecTask(ujsJobId);
		if (dbTask == null)
			return null;
		return new Long[] {dbTask.getCreationTime(), dbTask.getExecStartTime(), dbTask.getFinishTime()};
	}


	private static void checkObjectLength(
			final Map<String, Object> o,
			final int max,
			final String type,
			final String jobId) {
		final CountingOutputStream cos = new CountingOutputStream();
		try {
			//writes in UTF8
			new ObjectMapper().writeValue(cos, o);
		} catch (IOException ioe) {
			throw new RuntimeException("something's broken", ioe);
		} finally {
			try {
				cos.close();
			} catch (IOException ioe) {
				throw new RuntimeException("something's broken", ioe);
			}
		}
		if (cos.getSize() > max) {
			throw new IllegalArgumentException(String.format(
					"%s parameters%s are above %sB maximum: %s",
					type, jobId != null ? " for job ID " + jobId : "", max,
					cos.getSize()));
		}
	}

	private static RunJobParams getJobInput(
			final String ujsJobId,
			final Map<String, String> config)
			throws Exception {
	    return getJobInput(getTaskDescription(ujsJobId, config));
	}

    private static RunJobParams getJobInput(
            final ExecTask task)
            throws Exception {
		if (task.getJobInput() != null) {
			SanitizeMongoObject.befoul(task.getJobInput());
			return UObject.transformObjectToObject(task.getJobInput(),
					RunJobParams.class);
		}
		throw new IllegalStateException("According to the database, the " +
				"impossible occurred and a job was started without parameters");
	}

	private static FinishJobParams getJobOutput(
			final String ujsJobId,
			final AuthToken token,
			final Map<String, String> config) throws Exception {
		final ExecTask task = getTaskDescription(ujsJobId, config);
		if (task.getJobOutput() != null) {
			SanitizeMongoObject.befoul(task.getJobOutput());
			FinishJobParams ret = UObject.transformObjectToObject(task.getJobOutput(),
					FinishJobParams.class);
			if (ret.getIsCanceled() == null && ret.getIsCancelled() != null) {
			    ret.setIsCanceled(ret.getIsCancelled());
			}
			return ret;
		}
		return null;
	}

	private static AppInfo getAppInfo(
			final String ujsJobId,
			final Map<String, String> config)
			throws Exception {
        ExecTask task = getTaskDescription(ujsJobId, config);
        RunJobParams params = getJobInput(task);
        String methodSpecId = params.getAppId();
		String uiModuleName = null;
		if (methodSpecId != null) {
			String[] parts = methodSpecId.split("/");
			if (parts.length > 1) {
				uiModuleName = parts[0];
				methodSpecId = parts[1];
			}
		}
		return new AppInfo(uiModuleName, methodSpecId);
	}

	private static class AppInfo {
	    public final String uiModuleName;
	    public final String methodSpecId;

	    private AppInfo(String uiModuleName, String methodSpecId) {
	        this.uiModuleName = uiModuleName;
	        this.methodSpecId = methodSpecId;
	    }
	}
}
