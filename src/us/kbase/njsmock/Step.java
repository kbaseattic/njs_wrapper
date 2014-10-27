
package us.kbase.njsmock;

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
 * type - 'generic', 'python' or 'script'.
 * job_id_output_field - this field is used only in case this step is long running job and
 *     output of service method is structure with field having name coded in 
 *     'job_id_output_field' rather than just output string with job id.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "step_id",
    "type",
    "generic",
    "python",
    "script",
    "input_values",
    "is_long_running",
    "job_id_output_field"
})
public class Step {

    @JsonProperty("step_id")
    private String stepId;
    @JsonProperty("type")
    private String type;
    /**
     * <p>Original spec-file type: generic_service_method</p>
     * 
     * 
     */
    @JsonProperty("generic")
    private GenericServiceMethod generic;
    /**
     * <p>Original spec-file type: python_backend_method</p>
     * 
     * 
     */
    @JsonProperty("python")
    private PythonBackendMethod python;
    /**
     * <p>Original spec-file type: commandline_script_method</p>
     * 
     * 
     */
    @JsonProperty("script")
    private CommandlineScriptMethod script;
    @JsonProperty("input_values")
    private List<UObject> inputValues;
    @JsonProperty("is_long_running")
    private Long isLongRunning;
    @JsonProperty("job_id_output_field")
    private String jobIdOutputField;
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
     * <p>Original spec-file type: generic_service_method</p>
     * 
     * 
     */
    @JsonProperty("generic")
    public GenericServiceMethod getGeneric() {
        return generic;
    }

    /**
     * <p>Original spec-file type: generic_service_method</p>
     * 
     * 
     */
    @JsonProperty("generic")
    public void setGeneric(GenericServiceMethod generic) {
        this.generic = generic;
    }

    public Step withGeneric(GenericServiceMethod generic) {
        this.generic = generic;
        return this;
    }

    /**
     * <p>Original spec-file type: python_backend_method</p>
     * 
     * 
     */
    @JsonProperty("python")
    public PythonBackendMethod getPython() {
        return python;
    }

    /**
     * <p>Original spec-file type: python_backend_method</p>
     * 
     * 
     */
    @JsonProperty("python")
    public void setPython(PythonBackendMethod python) {
        this.python = python;
    }

    public Step withPython(PythonBackendMethod python) {
        this.python = python;
        return this;
    }

    /**
     * <p>Original spec-file type: commandline_script_method</p>
     * 
     * 
     */
    @JsonProperty("script")
    public CommandlineScriptMethod getScript() {
        return script;
    }

    /**
     * <p>Original spec-file type: commandline_script_method</p>
     * 
     * 
     */
    @JsonProperty("script")
    public void setScript(CommandlineScriptMethod script) {
        this.script = script;
    }

    public Step withScript(CommandlineScriptMethod script) {
        this.script = script;
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
        return ((((((((((((((((((("Step"+" [stepId=")+ stepId)+", type=")+ type)+", generic=")+ generic)+", python=")+ python)+", script=")+ script)+", inputValues=")+ inputValues)+", isLongRunning=")+ isLongRunning)+", jobIdOutputField=")+ jobIdOutputField)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
