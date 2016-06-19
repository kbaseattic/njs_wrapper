
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
 * <p>Original spec-file type: CreateJobParams</p>
 * <pre>
 * Parameters for the create_job2 method.
 * Optional parameters:
 * auth_strategy authstrat - the authorization strategy to use for the
 *         job. Omit to use the standard UJS authorization. If an
 *         authorization strategy is supplied, in most cases an authparam must
 *         be supplied as well.
 * auth_param - a parameter for the authorization strategy.
 * usermeta meta - metadata for the job.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "authstrat",
    "authparam",
    "meta"
})
public class CreateJobParams {

    @JsonProperty("authstrat")
    private java.lang.String authstrat;
    @JsonProperty("authparam")
    private java.lang.String authparam;
    @JsonProperty("meta")
    private Map<String, String> meta;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("authstrat")
    public java.lang.String getAuthstrat() {
        return authstrat;
    }

    @JsonProperty("authstrat")
    public void setAuthstrat(java.lang.String authstrat) {
        this.authstrat = authstrat;
    }

    public CreateJobParams withAuthstrat(java.lang.String authstrat) {
        this.authstrat = authstrat;
        return this;
    }

    @JsonProperty("authparam")
    public java.lang.String getAuthparam() {
        return authparam;
    }

    @JsonProperty("authparam")
    public void setAuthparam(java.lang.String authparam) {
        this.authparam = authparam;
    }

    public CreateJobParams withAuthparam(java.lang.String authparam) {
        this.authparam = authparam;
        return this;
    }

    @JsonProperty("meta")
    public Map<String, String> getMeta() {
        return meta;
    }

    @JsonProperty("meta")
    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    public CreateJobParams withMeta(Map<String, String> meta) {
        this.meta = meta;
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
        return ((((((((("CreateJobParams"+" [authstrat=")+ authstrat)+", authparam=")+ authparam)+", meta=")+ meta)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
