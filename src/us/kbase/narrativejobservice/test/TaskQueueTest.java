package us.kbase.narrativejobservice.test;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isNull;

import java.io.File;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.easymock.EasyMockSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import us.kbase.common.taskqueue2.JobStatuses;
import us.kbase.common.taskqueue2.RestartChecker;
import us.kbase.common.taskqueue2.Task;
import us.kbase.common.taskqueue2.TaskQueue;
import us.kbase.common.taskqueue2.TaskQueueConfig;
import us.kbase.common.taskqueue2.TaskRunner;
import us.kbase.narrativejobservice.db.ExecEngineMongoDb;

public class TaskQueueTest extends EasyMockSupport {
	private static MongoDBHelper dbh = null;
	private static ExecEngineMongoDb db = null;
	
	@BeforeClass
	public static void makeTempDir() throws Exception {
        dbh = new MongoDBHelper("test_task_queue", new File("temp_files"));
        dbh.startup(null);
        db = new ExecEngineMongoDb("localhost:" + dbh.getMongoPort(), "exec_engine", null, null, null);
	}
	
	@AfterClass
	public static void dropTempDir() throws Exception {
	    try {
	        dbh.shutdown();
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	}
	
	private static void delete(File fileOrDir) {
		if (fileOrDir.isDirectory()) {
			for (File sub : fileOrDir.listFiles())
				delete(sub);
		}
		fileOrDir.delete();
	}
	
	@Test
	public void testGood() throws Exception {
		String token = "goodSecret";
		String jobId = "jobGood";
		JobStatuses jbst = createStrictMock(JobStatuses.class);
		expect(jbst.createAndStartJob(eq(token), eq("queued"), anyObject(String.class), 
				anyObject(String.class), isNull(String.class))).andReturn(jobId);
		jbst.updateJob(eq(jobId), eq(token), eq("running"), isNull(String.class));
		jbst.completeJob(eq(jobId), eq(token), eq("done"), isNull(String.class), 
				anyObject(String.class), anyObject(String.class));
		final TaskQueue[] tq = {null};
		final boolean[] complete = {false};
		expectLastCall().andDelegateTo(new JobStatuses() {
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
			}
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				return null;
			}
			@Override
		    public void completeJob(String job, String token, String status, String error, String wsUrl, 
		    		String outRef) throws Exception {
				complete[0] = true;
		    }
		});
		replayAll();
		final boolean[] isOk = {false};
		tq[0] = new TaskQueue(new TaskQueueConfig(1, null, jbst, null, -1, null),
				new RestartChecker() {
					@Override
					public boolean isInRestartMode() {
						return false;
					}
				}, db,
				new TestTaskRunner() {
					@Override
					public void run(String token, TestTask inputData,
							String jobId, String outRef) throws Exception {
						isOk[0] = true;
					}
				});
		Assert.assertEquals(jobId, tq[0].addTask(new TestTask("something-saved"), token));
		while (tq[0].getTask(jobId) != null) {
			Thread.sleep(100);
		}
		tq[0].stopAllThreads();
		verifyAll();
		Assert.assertTrue(complete[0]);
		Assert.assertTrue(isOk[0]);
		checkForEmptyDbQueue();
	}

	@Test
	public void testUserTaskLimit() throws Exception {
		String token = "limitSecret";
		final String job1Id = "jobGood1";
		final String job2Id = "jobGood2";
		final int[] jobNum = {0}; 
		final Set<String> complete = new TreeSet<String>();
		JobStatuses jbst = new JobStatuses() {
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
			}
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				int num = jobNum[0];
				jobNum[0]++;
				return num == 0 ? job1Id : job2Id;
			}
			@Override
		    public void completeJob(String job, String token, String status, String error, String wsUrl, 
		    		String outRef) throws Exception {
				complete.add(job);
		    }
		};
		replayAll();
		final Set<String> isOk = new TreeSet<String>();
		TaskQueue tq = new TaskQueue(new TaskQueueConfig(2, null, jbst, null, 1, null),
				new RestartChecker() {
					@Override
					public boolean isInRestartMode() {
						return false;
					}
				}, db,
				new TestTaskRunner() {
					@Override
					public void run(String token, TestTask inputData,
							String jobId, String outRef) throws Exception {
						if (jobId.equals(job2Id)) {
							if (!(complete.contains(job1Id) && isOk.contains(job1Id)))
								throw new IllegalStateException("Job [" + jobId + "] was started before job [" + job1Id + "] was finished");
						}
						Thread.sleep(1000);
						isOk.add(jobId);
					}
				}) {
			@Override
			protected String getUserForTask(Task task) {
				return task.getAuthToken();
			}
		};
		Assert.assertEquals(job1Id, tq.addTask(new TestTask("task1"), token));
		Assert.assertEquals(job2Id, tq.addTask(new TestTask("task2"), token));
		while (tq.getTask(job1Id) != null) {
			Thread.sleep(100);
		}
		while (tq.getTask(job2Id) != null) {
			Thread.sleep(100);
		}
		tq.stopAllThreads();
		verifyAll();
		Assert.assertTrue(complete.contains(job1Id));
		Assert.assertTrue(isOk.contains(job1Id));
		Assert.assertTrue(complete.contains(job2Id));
		Assert.assertTrue(isOk.contains(job2Id));
		checkForEmptyDbQueue();
	}

	@Test
	public void testBad() throws Exception {
		String token = "badSecret";
		String jobId = "jobBad";
		final String errorMsg = "Super error!";
		JobStatuses jbst = createStrictMock(JobStatuses.class);
		expect(jbst.createAndStartJob(eq(token), eq("queued"), anyObject(String.class), 
				anyObject(String.class), isNull(String.class))).andReturn(jobId);
		jbst.updateJob(eq(jobId), eq(token), eq("running"), isNull(String.class));
		jbst.completeJob(eq(jobId), eq(token), eq("Error: " + errorMsg), anyObject(String.class), 
				anyObject(String.class), anyObject(String.class));
		final TaskQueue[] tq = {null};
		final boolean[] complete = {false};
		expectLastCall().andDelegateTo(new JobStatuses() {
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
			}
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				return null;
			}
			@Override
		    public void completeJob(String job, String token, String status, String error, String wsUrl, 
		    		String outRef) throws Exception {
				complete[0] = true;
		    }
		});
		replayAll();
		tq[0] = new TaskQueue(new TaskQueueConfig(1, null, jbst, null, 0, null), 
				new RestartChecker() {
					@Override
					public boolean isInRestartMode() {
						return false;
					}
				}, db,
				new TestTaskRunner() {
					@Override
					public void run(String token, TestTask inputData, String jobId, String outRef) throws Exception {
						throw new IllegalStateException(errorMsg);
					}
				});
		Assert.assertEquals(jobId, tq[0].addTask(new TestTask("something-saved"), token));
		while (tq[0].getTask(jobId) != null) {
			Thread.sleep(100);
		}
		tq[0].stopAllThreads();
		verifyAll();
		Assert.assertTrue(complete[0]);
		checkForEmptyDbQueue();
	}

	private void checkForEmptyDbQueue() throws Exception {
		Assert.assertEquals(0, db.getQueuedTasks().size());
	}

	@Test
	public void testReboot() throws Exception {
		String token = "rebootSecret";
		final String jobId = "jobReboot";
		JobStatuses jbst = new JobStatuses() {
			@Override
			public void updateJob(String job, String token, String status,
					String estComplete) throws Exception {
				throw new IllegalStateException();
			}
			@Override
			public String createAndStartJob(String token, String status, String desc,
					String initProgressPtype, String estComplete) throws Exception {
				return jobId;
			}
			@Override
		    public void completeJob(String job, String token, String status, String error, String wsUrl, 
		    		String outRef) throws Exception {
				throw new IllegalStateException();
		    }
		};
		final TaskQueue tq = new TaskQueue(new TaskQueueConfig(1, null, jbst, null, 0, null), 
				new RestartChecker() {
					@Override
					public boolean isInRestartMode() {
						return true;
					}
				}, db,
				new TestTaskRunner() {
					@Override
					public void run(String token, TestTask inputData, String jobId, String outRef) throws Exception {
						throw new IllegalStateException("Task is running");
					}
				});
		tq.addTask(new TestTask("something-saved"), token);
		Thread.sleep(1000);
		Assert.assertEquals(1, tq.getQueuedTasks());
		Assert.assertEquals(0, tq.getRunningTasksPerUser().size());
		tq.stopAllThreads();
		tq.deleteTaskFromDb(jobId);
		checkForEmptyDbQueue();
	}

	public static class TestTask {
		private String innerParam;
		
		public TestTask() {
		}
		
		public TestTask(String param) {
			this.innerParam = param;
		}
		
		public String getInnerParam() {
			return innerParam;
		}
		
		public void setInnerParam(String innerParam) {
			this.innerParam = innerParam;
		}
	}
	
	public abstract static class TestTaskRunner implements TaskRunner<TestTask> {
		@Override
		public Class<TestTask> getInputDataType() {
			return TestTask.class;
		}
		
		@Override
		public String getOutRef(TestTask inputData) {
			return null;
		}
		
		@Override
		public String getTaskDescription() {
			return "Nothing personal, just test";
		}
		
		@Override
		public void init(TaskQueueConfig mainCfg, Map<String, String> configParams) {
		}
	}
}
