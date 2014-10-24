package us.kbase.njsmock;

public class RunAppBuilder extends DefaultTaskBuilder<App> {

	@Override
	public Class<App> getInputDataType() {
		return App.class;
	}
	
	@Override
	public String getOutRef(App inputData) {
		return null;
	}
	
	@Override
	public String getTaskDescription() {
		return "Narrative application runner";
	}
	
	@Override
	public void run(String token, App inputData, String jobId, String outRef)
			throws Exception {
		
	}
}
