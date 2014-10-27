package us.kbase.njsmock;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppEventListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		try {
			NJSMockServer.getTaskQueue();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			NJSMockServer.getTaskQueue().stopAllThreads();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
