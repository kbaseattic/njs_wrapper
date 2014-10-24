
package us.kbase.userandjobstate;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: InitProgress</p>
 * <pre>
 * Initialization information for progress tracking. Currently 3 choices:
 * progress_type ptype - one of 'none', 'percent', or 'task'
 * max_progress max- required only for task based tracking. The 
 *         total number of tasks until the job is complete.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "ptype",
    "max"
})
public class InitProgress {

    @JsonProperty("ptype")
    private String ptype;
    @JsonProperty("max")
    private Long max;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("ptype")
    public String getPtype() {
        return ptype;
    }

    @JsonProperty("ptype")
    public void setPtype(String ptype) {
        this.ptype = ptype;
    }

    public InitProgress withPtype(String ptype) {
        this.ptype = ptype;
        return this;
    }

    @JsonProperty("max")
    public Long getMax() {
        return max;
    }

    @JsonProperty("max")
    public void setMax(Long max) {
        this.max = max;
    }

    public InitProgress withMax(Long max) {
        this.max = max;
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
        return ((((((("InitProgress"+" [ptype=")+ ptype)+", max=")+ max)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
