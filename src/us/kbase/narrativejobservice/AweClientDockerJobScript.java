package us.kbase.narrativejobservice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.UObject;
import us.kbase.common.utils.TextUtils;
import us.kbase.common.utils.UTCDateFormat;
import us.kbase.shock.client.BasicShockClient;
import us.kbase.shock.client.ShockNodeId;
import us.kbase.userandjobstate.InitProgress;
import us.kbase.userandjobstate.Results;
import us.kbase.userandjobstate.UserAndJobStateClient;

@SuppressWarnings("unchecked")
public class AweClientDockerJobScript {
    
    @SuppressWarnings("rawtypes")
    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.err.println("Usage: <program> <job_id> <input_shock_id> <output_shock_id> <configuration_json_hex_string>");
            for (int i = 0; i < args.length; i++)
                System.err.println("\tArgument[" + i + "]: " + args[i]);
            System.exit(1);
        }
        String jobId = args[0];
        String inputShockId = args[1];
        String outputShockId = args[2];
        String token = System.getenv("KB_AUTH_TOKEN");
        if (token == null || token.isEmpty())
            token = System.getProperty("KB_AUTH_TOKEN");  // For tests
        UserAndJobStateClient ujsClient = null;
        Map<String, String> config = UObject.getMapper().readValue(
                TextUtils.hexToString(args[3]), Map.class);
        try {
            ujsClient = getUjsClient(config, token);
            if (token == null || token.isEmpty())
                throw new IllegalStateException("Token is not defined");
            BasicShockClient shockClient = getShockClient(config, token);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            shockClient.getFile(new ShockNodeId(inputShockId), baos);
            baos.close();
            RunJobParams job = UObject.getMapper().readValue(baos.toByteArray(), RunJobParams.class);
            ujsClient.startJob(jobId, token, "running", "AWE job for " + job.getMethod(), 
                    new InitProgress().withPtype("none"), null);
            File jobDir = getJobDir(config, jobId);
            String moduleName = job.getMethod().split("\\.")[0];
            RpcContext context = job.getRpcContext();
            if (context == null)
                context = new RpcContext().withRunId("");
            if (context.getCallStack() == null)
                context.setCallStack(new ArrayList<MethodCall>());
            context.getCallStack().add(new MethodCall().withJobId(jobId).withMethod(job.getMethod())
                    .withTime(new UTCDateFormat().formatDate(new Date())));
            Map<String, Object> rpc = new LinkedHashMap<String, Object>();
            rpc.put("version", "1.1");
            rpc.put("method", job.getMethod());
            rpc.put("params", job.getParams());
            rpc.put("context", job.getRpcContext());
            File inputFile = new File(jobDir, "input.json");
            UObject.getMapper().writeValue(inputFile, rpc);
            File outputFile = new File(jobDir, "output.json");
            ujsClient.updateJob(jobId, token, "running", null);
            String imageName = decamelize(moduleName);
            String imageVersion = job.getServiceVer();
            if (imageVersion == null || imageVersion.isEmpty())
                imageVersion = "latest";
            new DockerRunner(getDockerRegistryURL(config)).run(imageName, imageVersion, moduleName, 
                    inputFile, token, new StringBuilder(), outputFile, false);
            FinishJobParams result = UObject.getMapper().readValue(outputFile, FinishJobParams.class);
            Object data = result.getResult().asClassInstance(Object.class);
            if (data instanceof List) {
                Object dataItem = ((List)data).get(0);
                if (dataItem instanceof Map) {
                    Map<String, Object> dataItemMap = (Map)dataItem;
                    if (dataItemMap.containsKey("token"))
                        dataItemMap.remove("token");
                    result.setResult(new UObject(data));
                    UObject.getMapper().writeValue(outputFile, result);
                }
            }
            InputStream is = new FileInputStream(outputFile);
            // save result into outputShockId;
            updateShockNode(getShockURL(config), token, outputShockId, is, "output.json", "json");
            ujsClient.completeJob(jobId, token, "done", null, new Results());
        } catch (Exception ex) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            pw.close();
            String stacktrace = sw.toString();
            try {
                FinishJobParams result = new FinishJobParams().withError(new JsonRpcError().withCode(-1L)
                        .withName("JSONRPCError").withMessage("Job service side error: " + ex.getMessage())
                        .withError(stacktrace));
                ByteArrayInputStream bais = new ByteArrayInputStream(UObject.getMapper().writeValueAsBytes(result));
                updateShockNode(getShockURL(config), token, outputShockId, bais, "output.json", "json");
            } catch (Exception ignore) {}
            if (ujsClient != null) {
                String status = "Error: " + ex.getMessage();
                if (status.length() > 200)
                    status = status.substring(0, 197) + "...";
                ujsClient.completeJob(jobId, token, status, stacktrace, null);
            }
        }
    }

    public static String decamelize(final String s) {
        final Matcher m = Pattern.compile("([A-Z])").matcher(s.substring(1));
        return (s.substring(0, 1) + m.replaceAll("_$1")).toLowerCase();
    }
    
    private static String updateShockNode(String shockUrl, String token, String shockNodeId, 
            InputStream file, final String filename, final String format) throws Exception {
        String nodeurl = shockUrl;
        if (!nodeurl.endsWith("/"))
            nodeurl += "/";
        nodeurl += "node/" + shockNodeId;
        final HttpPut htp = new HttpPut(nodeurl);
            final MultipartEntityBuilder mpeb = MultipartEntityBuilder.create();
            if (file != null) {
                mpeb.addBinaryBody("upload", file, ContentType.DEFAULT_BINARY,
                        filename);
            }
            if (format != null) {
                mpeb.addTextBody("format", format);
            }
            htp.setEntity(mpeb.build());
        PoolingHttpClientConnectionManager cm =
                new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1000);
        cm.setDefaultMaxPerRoute(1000);
        CloseableHttpClient client = HttpClients.custom().setConnectionManager(cm).build();
        htp.setHeader("Authorization", "OAuth " + token);
        final CloseableHttpResponse response = client.execute(htp);
        try {
            String resp = EntityUtils.toString(response.getEntity());
            Map<String, String> node = (Map<String, String>)UObject.getMapper()
                    .readValue(resp, Map.class).get("data");
            return node.get("id");
        } finally {
            response.close();
            file.close();
        }
    }
    
    private static File getJobDir(Map<String, String> config, String jobId) {
        String rootDirPath = config.get(NarrativeJobServiceServer.CFG_PROP_AWE_CLIENT_SCRATCH);
        File rootDir = new File(rootDirPath == null ? "." : rootDirPath);
        if (!rootDir.exists())
            rootDir.mkdirs();
        File ret = new File(rootDir, "job_" + jobId);
        if (!ret.exists())
            ret.mkdir();
        return ret;
    }

    private static UserAndJobStateClient getUjsClient(Map<String, String> config, 
            String token) throws Exception {
        String ujsUrl = config.get(NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL);
        if (ujsUrl == null)
            throw new IllegalStateException("Parameter '" + 
                    NarrativeJobServiceServer.CFG_PROP_JOBSTATUS_SRV_URL + "' is not defined in configuration");
        UserAndJobStateClient ret = new UserAndJobStateClient(new URL(ujsUrl), new AuthToken(token));
        ret.setIsInsecureHttpConnectionAllowed(true);
        return ret;
    }

    private static BasicShockClient getShockClient(Map<String, String> config, 
            String token) throws Exception {
        String shockUrl = getShockURL(config);
        BasicShockClient ret = new BasicShockClient(new URL(shockUrl), new AuthToken(token));
        return ret;
    }

    private static String getShockURL(Map<String, String> config) {
        String shockUrl = config.get(NarrativeJobServiceServer.CFG_PROP_SHOCK_URL);
        if (shockUrl == null)
            throw new IllegalStateException("Parameter '" + NarrativeJobServiceServer.CFG_PROP_SHOCK_URL +
                    "' is not defined in configuration");
        return shockUrl;
    }

    private static String getDockerRegistryURL(Map<String, String> config) {
        String drUrl = config.get(NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL);
        if (drUrl == null)
            throw new IllegalStateException("Parameter '" + NarrativeJobServiceServer.CFG_PROP_DOCKER_REGISTRY_URL +
                    "' is not defined in configuration");
        return drUrl;
    }
}
