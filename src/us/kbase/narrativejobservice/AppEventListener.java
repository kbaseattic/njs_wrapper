package us.kbase.narrativejobservice;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class AppEventListener implements ServletContextListener {

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		try {
			NarrativeJobServiceServer.getTaskQueue();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		try {
			NarrativeJobServiceServer.getTaskQueue().stopAllThreads();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
