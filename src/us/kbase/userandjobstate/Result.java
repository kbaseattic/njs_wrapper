
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
 * <p>Original spec-file type: Result</p>
 * <pre>
 * A place where the results of a job may be found.
 * All fields except description are required.
 * string server_type - the type of server storing the results. Typically
 *         either "Shock" or "Workspace". No more than 100 characters.
 * string url - the url of the server. No more than 1000 characters.
 * string id - the id of the result in the server. Typically either a
 *         workspace id or a shock node. No more than 1000 characters.
 * string description - a free text description of the result.
 *          No more than 1000 characters.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "server_type",
    "url",
    "id",
    "description"
})
public class Result {

    @JsonProperty("server_type")
    private String serverType;
    @JsonProperty("url")
    private String url;
    @JsonProperty("id")
    private String id;
    @JsonProperty("description")
    private String description;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("server_type")
    public String getServerType() {
        return serverType;
    }

    @JsonProperty("server_type")
    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public Result withServerType(String serverType) {
        this.serverType = serverType;
        return this;
    }

    @JsonProperty("url")
    public String getUrl() {
        return url;
    }

    @JsonProperty("url")
    public void setUrl(String url) {
        this.url = url;
    }

    public Result withUrl(String url) {
        this.url = url;
        return this;
    }

    @JsonProperty("id")
    public String getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(String id) {
        this.id = id;
    }

    public Result withId(String id) {
        this.id = id;
        return this;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("description")
    public void setDescription(String description) {
        this.description = description;
    }

    public Result withDescription(String description) {
        this.description = description;
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
        return ((((((((((("Result"+" [serverType=")+ serverType)+", url=")+ url)+", id=")+ id)+", description=")+ description)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
