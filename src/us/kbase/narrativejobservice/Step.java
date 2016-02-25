
package us.kbase.narrativejobservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import us.kbase.common.service.UObject;


/**
 * <p>Original spec-file type: step</p>
 * <pre>
 * type - 'service' or 'script'.
 * job_id_output_field - this field is used only in case this step is long running job and
 *     output of service method is structure with field having name coded in
 *     'job_id_output_field' rather than just output string with job id.
 * method_spec_id - high level id of UI method used for logging of execution time 
 *     statistics.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "step_id",
    "type",
    "service",
    "script",
    "parameters",
    "input_values",
    "is_long_running",
    "job_id_output_field",
    "method_spec_id"
})
public class Step {

    @JsonProperty("step_id")
    private String stepId;
    @JsonProperty("type")
    private String type;
    /**
     * <p>Original spec-file type: service_method</p>
     * <pre>
     * service_url could be empty in case of docker image of service loaded from registry,
     * service_version - optional parameter defining version of service docker image.
     * </pre>
     * 
     */
    @JsonProperty("service")
    private ServiceMethod service;
    /**
     * <p>Original spec-file type: script_method</p>
     * 
     * 
     */
    @JsonProperty("script")
    private ScriptMethod script;
    @JsonProperty("parameters")
    private List<StepParameter> parameters;
    @JsonProperty("input_values")
    private List<UObject> inputValues;
    @JsonProperty("is_long_running")
    private Long isLongRunning;
    @JsonProperty("job_id_output_field")
    private String jobIdOutputField;
    @JsonProperty("method_spec_id")
    private String methodSpecId;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("step_id")
    public String getStepId() {
        return stepId;
    }

    @JsonProperty("step_id")
    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public Step withStepId(String stepId) {
        this.stepId = stepId;
        return this;
    }

    @JsonProperty("type")
    public String getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(String type) {
        this.type = type;
    }

    public Step withType(String type) {
        this.type = type;
        return this;
    }

    /**
     * <p>Original spec-file type: service_method</p>
     * <pre>
     * service_url could be empty in case of docker image of service loaded from registry,
     * service_version - optional parameter defining version of service docker image.
     * </pre>
     * 
     */
    @JsonProperty("service")
    public ServiceMethod getService() {
        return service;
    }

    /**
     * <p>Original spec-file type: service_method</p>
     * <pre>
     * service_url could be empty in case of docker image of service loaded from registry,
     * service_version - optional parameter defining version of service docker image.
     * </pre>
     * 
     */
    @JsonProperty("service")
    public void setService(ServiceMethod service) {
        this.service = service;
    }

    public Step withService(ServiceMethod service) {
        this.service = service;
        return this;
    }

    /**
     * <p>Original spec-file type: script_method</p>
     * 
     * 
     */
    @JsonProperty("script")
    public ScriptMethod getScript() {
        return script;
    }

    /**
     * <p>Original spec-file type: script_method</p>
     * 
     * 
     */
    @JsonProperty("script")
    public void setScript(ScriptMethod script) {
        this.script = script;
    }

    public Step withScript(ScriptMethod script) {
        this.script = script;
        return this;
    }

    @JsonProperty("parameters")
    public List<StepParameter> getParameters() {
        return parameters;
    }

    @JsonProperty("parameters")
    public void setParameters(List<StepParameter> parameters) {
        this.parameters = parameters;
    }

    public Step withParameters(List<StepParameter> parameters) {
        this.parameters = parameters;
        return this;
    }

    @JsonProperty("input_values")
    public List<UObject> getInputValues() {
        return inputValues;
    }

    @JsonProperty("input_values")
    public void setInputValues(List<UObject> inputValues) {
        this.inputValues = inputValues;
    }

    public Step withInputValues(List<UObject> inputValues) {
        this.inputValues = inputValues;
        return this;
    }

    @JsonProperty("is_long_running")
    public Long getIsLongRunning() {
        return isLongRunning;
    }

    @JsonProperty("is_long_running")
    public void setIsLongRunning(Long isLongRunning) {
        this.isLongRunning = isLongRunning;
    }

    public Step withIsLongRunning(Long isLongRunning) {
        this.isLongRunning = isLongRunning;
        return this;
    }

    @JsonProperty("job_id_output_field")
    public String getJobIdOutputField() {
        return jobIdOutputField;
    }

    @JsonProperty("job_id_output_field")
    public void setJobIdOutputField(String jobIdOutputField) {
        this.jobIdOutputField = jobIdOutputField;
    }

    public Step withJobIdOutputField(String jobIdOutputField) {
        this.jobIdOutputField = jobIdOutputField;
        return this;
    }

    @JsonProperty("method_spec_id")
    public String getMethodSpecId() {
        return methodSpecId;
    }

    @JsonProperty("method_spec_id")
    public void setMethodSpecId(String methodSpecId) {
        this.methodSpecId = methodSpecId;
    }

    public Step withMethodSpecId(String methodSpecId) {
        this.methodSpecId = methodSpecId;
        return this;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperties(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

    @Override
    public String toString() {
        return ((((((((((((((((((((("Step"+" [stepId=")+ stepId)+", type=")+ type)+", service=")+ service)+", script=")+ script)+", parameters=")+ parameters)+", inputValues=")+ inputValues)+", isLongRunning=")+ isLongRunning)+", jobIdOutputField=")+ jobIdOutputField)+", methodSpecId=")+ methodSpecId)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
