
package us.kbase.catalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


/**
 * <p>Original spec-file type: GetExecAggrStatsParams</p>
 * <pre>
 * full_app_ids - list of fully qualified app IDs (including module_name prefix followed by
 *     slash in case of dynamically registered repo).
 * full_func_names - list of fully qualified names of KIDL-spec functions (including
 *     module_name prefix followed by slash in case of dynamically registered repo).
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "full_app_ids",
    "full_func_names"
})
public class GetExecAggrStatsParams {

    @JsonProperty("full_app_ids")
    private List<String> fullAppIds;
    @JsonProperty("full_func_names")
    private List<String> fullFuncNames;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("full_app_ids")
    public List<String> getFullAppIds() {
        return fullAppIds;
    }

    @JsonProperty("full_app_ids")
    public void setFullAppIds(List<String> fullAppIds) {
        this.fullAppIds = fullAppIds;
    }

    public GetExecAggrStatsParams withFullAppIds(List<String> fullAppIds) {
        this.fullAppIds = fullAppIds;
        return this;
    }

    @JsonProperty("full_func_names")
    public List<String> getFullFuncNames() {
        return fullFuncNames;
    }

    @JsonProperty("full_func_names")
    public void setFullFuncNames(List<String> fullFuncNames) {
        this.fullFuncNames = fullFuncNames;
    }

    public GetExecAggrStatsParams withFullFuncNames(List<String> fullFuncNames) {
        this.fullFuncNames = fullFuncNames;
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
        return ((((((("GetExecAggrStatsParams"+" [fullAppIds=")+ fullAppIds)+", fullFuncNames=")+ fullFuncNames)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
