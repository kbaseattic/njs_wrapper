
package us.kbase.userandjobstate;

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
 * <p>Original spec-file type: Results</p>
 * <pre>
 * A pointer to job results. All arguments are optional. Applications
 * should use the default shock and workspace urls if omitted.
 * list<string> shocknodes - the shocknode(s) where the results can be
 *         found.
 * string shockurl - the url of the shock service where the data was
 *         saved.
 * list<string> workspaceids - the workspace ids where the results can be
 *         found.
 * string workspaceurl - the url of the workspace service where the data
 *         was saved.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "shocknodes",
    "shockurl",
    "workspaceids",
    "workspaceurl"
})
public class Results {

    @JsonProperty("shocknodes")
    private List<String> shocknodes;
    @JsonProperty("shockurl")
    private java.lang.String shockurl;
    @JsonProperty("workspaceids")
    private List<String> workspaceids;
    @JsonProperty("workspaceurl")
    private java.lang.String workspaceurl;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("shocknodes")
    public List<String> getShocknodes() {
        return shocknodes;
    }

    @JsonProperty("shocknodes")
    public void setShocknodes(List<String> shocknodes) {
        this.shocknodes = shocknodes;
    }

    public Results withShocknodes(List<String> shocknodes) {
        this.shocknodes = shocknodes;
        return this;
    }

    @JsonProperty("shockurl")
    public java.lang.String getShockurl() {
        return shockurl;
    }

    @JsonProperty("shockurl")
    public void setShockurl(java.lang.String shockurl) {
        this.shockurl = shockurl;
    }

    public Results withShockurl(java.lang.String shockurl) {
        this.shockurl = shockurl;
        return this;
    }

    @JsonProperty("workspaceids")
    public List<String> getWorkspaceids() {
        return workspaceids;
    }

    @JsonProperty("workspaceids")
    public void setWorkspaceids(List<String> workspaceids) {
        this.workspaceids = workspaceids;
    }

    public Results withWorkspaceids(List<String> workspaceids) {
        this.workspaceids = workspaceids;
        return this;
    }

    @JsonProperty("workspaceurl")
    public java.lang.String getWorkspaceurl() {
        return workspaceurl;
    }

    @JsonProperty("workspaceurl")
    public void setWorkspaceurl(java.lang.String workspaceurl) {
        this.workspaceurl = workspaceurl;
    }

    public Results withWorkspaceurl(java.lang.String workspaceurl) {
        this.workspaceurl = workspaceurl;
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
        return ((((((((((("Results"+" [shocknodes=")+ shocknodes)+", shockurl=")+ shockurl)+", workspaceids=")+ workspaceids)+", workspaceurl=")+ workspaceurl)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
