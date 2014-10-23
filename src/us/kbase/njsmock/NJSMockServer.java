package us.kbase.njsmock;

import us.kbase.auth.AuthToken;
import us.kbase.common.service.JsonServerMethod;
import us.kbase.common.service.JsonServerServlet;

//BEGIN_HEADER
//END_HEADER

/**
 * <p>Original spec-file module name: NJSMock</p>
 * <pre>
 * </pre>
 */
public class NJSMockServer extends JsonServerServlet {
    private static final long serialVersionUID = 1L;

    //BEGIN_CLASS_HEADER
    //END_CLASS_HEADER

    public NJSMockServer() throws Exception {
        super("NJSMock");
        //BEGIN_CONSTRUCTOR
        //END_CONSTRUCTOR
    }

    /**
     * <p>Original spec-file function name: run_app</p>
     * <pre>
     * </pre>
     * @param   app   instance of type {@link us.kbase.njsmock.App App} (original type "app")
     * @return   instance of type {@link us.kbase.njsmock.AppJobs AppJobs} (original type "app_jobs")
     */
    @JsonServerMethod(rpc = "NJSMock.run_app")
    public AppJobs runApp(App app, AuthToken authPart) throws Exception {
        AppJobs returnVal = null;
        //BEGIN run_app
        //END run_app
        return returnVal;
    }

    /**
     * <p>Original spec-file function name: check_app_state</p>
     * <pre>
     * </pre>
     * @param   appRunId   instance of String
     * @return   instance of type {@link us.kbase.njsmock.AppJobs AppJobs} (original type "app_jobs")
     */
    @JsonServerMethod(rpc = "NJSMock.check_app_state")
    public AppJobs checkAppState(String appRunId, AuthToken authPart) throws Exception {
        AppJobs returnVal = null;
        //BEGIN check_app_state
        //END check_app_state
        return returnVal;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <program> <server_port>");
            return;
        }
        new NJSMockServer().startupServer(Integer.parseInt(args[0]));
    }
}
