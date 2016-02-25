
package us.kbase.narrativejobservice;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: Status</p>
 * 
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "reboot_mode",
    "stopping_mode",
    "running_tasks_total",
    "running_tasks_per_user",
    "tasks_in_queue",
    "config",
    "git_commit"
})
public class Status {

    @JsonProperty("reboot_mode")
    private java.lang.Long rebootMode;
    @JsonProperty("stopping_mode")
    private java.lang.Long stoppingMode;
    @JsonProperty("running_tasks_total")
    private java.lang.Long runningTasksTotal;
    @JsonProperty("running_tasks_per_user")
    private Map<String, Long> runningTasksPerUser;
    @JsonProperty("tasks_in_queue")
    private java.lang.Long tasksInQueue;
    @JsonProperty("config")
    private Map<String, String> config;
    @JsonProperty("git_commit")
    private java.lang.String gitCommit;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("reboot_mode")
    public java.lang.Long getRebootMode() {
        return rebootMode;
    }

    @JsonProperty("reboot_mode")
    public void setRebootMode(java.lang.Long rebootMode) {
        this.rebootMode = rebootMode;
    }

    public Status withRebootMode(java.lang.Long rebootMode) {
        this.rebootMode = rebootMode;
        return this;
    }

    @JsonProperty("stopping_mode")
    public java.lang.Long getStoppingMode() {
        return stoppingMode;
    }

    @JsonProperty("stopping_mode")
    public void setStoppingMode(java.lang.Long stoppingMode) {
        this.stoppingMode = stoppingMode;
    }

    public Status withStoppingMode(java.lang.Long stoppingMode) {
        this.stoppingMode = stoppingMode;
        return this;
    }

    @JsonProperty("running_tasks_total")
    public java.lang.Long getRunningTasksTotal() {
        return runningTasksTotal;
    }

    @JsonProperty("running_tasks_total")
    public void setRunningTasksTotal(java.lang.Long runningTasksTotal) {
        this.runningTasksTotal = runningTasksTotal;
    }

    public Status withRunningTasksTotal(java.lang.Long runningTasksTotal) {
        this.runningTasksTotal = runningTasksTotal;
        return this;
    }

    @JsonProperty("running_tasks_per_user")
    public Map<String, Long> getRunningTasksPerUser() {
        return runningTasksPerUser;
    }

    @JsonProperty("running_tasks_per_user")
    public void setRunningTasksPerUser(Map<String, Long> runningTasksPerUser) {
        this.runningTasksPerUser = runningTasksPerUser;
    }

    public Status withRunningTasksPerUser(Map<String, Long> runningTasksPerUser) {
        this.runningTasksPerUser = runningTasksPerUser;
        return this;
    }

    @JsonProperty("tasks_in_queue")
    public java.lang.Long getTasksInQueue() {
        return tasksInQueue;
    }

    @JsonProperty("tasks_in_queue")
    public void setTasksInQueue(java.lang.Long tasksInQueue) {
        this.tasksInQueue = tasksInQueue;
    }

    public Status withTasksInQueue(java.lang.Long tasksInQueue) {
        this.tasksInQueue = tasksInQueue;
        return this;
    }

    @JsonProperty("config")
    public Map<String, String> getConfig() {
        return config;
    }

    @JsonProperty("config")
    public void setConfig(Map<String, String> config) {
        this.config = config;
    }

    public Status withConfig(Map<String, String> config) {
        this.config = config;
        return this;
    }

    @JsonProperty("git_commit")
    public java.lang.String getGitCommit() {
        return gitCommit;
    }

    @JsonProperty("git_commit")
    public void setGitCommit(java.lang.String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public Status withGitCommit(java.lang.String gitCommit) {
        this.gitCommit = gitCommit;
        return this;
    }

    @JsonAnyGetter
    public Map<java.lang.String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(java.lang.String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public java.lang.String toString() {
        return ((((((((((((((((("Status"+" [rebootMode=")+ rebootMode)+", stoppingMode=")+ stoppingMode)+", runningTasksTotal=")+ runningTasksTotal)+", runningTasksPerUser=")+ runningTasksPerUser)+", tasksInQueue=")+ tasksInQueue)+", config=")+ config)+", gitCommit=")+ gitCommit)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
