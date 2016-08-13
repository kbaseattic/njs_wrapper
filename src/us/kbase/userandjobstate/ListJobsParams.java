
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
 * <p>Original spec-file type: ListJobsParams</p>
 * <pre>
 * Input parameters for the list_jobs2 method.
 * Optional parameters:
 * list<service_name> services - the services from which to list jobs.
 *         Omit to list jobs from all services.
 * job_filter filter - the filter to apply to the set of jobs.
 * auth_strategy authstrat - return jobs with the specified
 *         authorization strategy. If this parameter is omitted, jobs
 *         with the default strategy will be returned.
 * list<auth_params> authparams - only return jobs with one of the
 *         specified authorization parameters. An authorization strategy must
 *         be provided if authparams is specified. In most cases, at least one
 *         authorization parameter must be supplied and there is an upper
 *         limit to the number of paramters allowed. In the case of the
 *         kbaseworkspace strategy, these limits are 1 and 10, respectively.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "services",
    "filter",
    "authstrat",
    "authparams"
})
public class ListJobsParams {

    @JsonProperty("services")
    private List<String> services;
    @JsonProperty("filter")
    private java.lang.String filter;
    @JsonProperty("authstrat")
    private java.lang.String authstrat;
    @JsonProperty("authparams")
    private List<String> authparams;
    private Map<java.lang.String, Object> additionalProperties = new HashMap<java.lang.String, Object>();

    @JsonProperty("services")
    public List<String> getServices() {
        return services;
    }

    @JsonProperty("services")
    public void setServices(List<String> services) {
        this.services = services;
    }

    public ListJobsParams withServices(List<String> services) {
        this.services = services;
        return this;
    }

    @JsonProperty("filter")
    public java.lang.String getFilter() {
        return filter;
    }

    @JsonProperty("filter")
    public void setFilter(java.lang.String filter) {
        this.filter = filter;
    }

    public ListJobsParams withFilter(java.lang.String filter) {
        this.filter = filter;
        return this;
    }

    @JsonProperty("authstrat")
    public java.lang.String getAuthstrat() {
        return authstrat;
    }

    @JsonProperty("authstrat")
    public void setAuthstrat(java.lang.String authstrat) {
        this.authstrat = authstrat;
    }

    public ListJobsParams withAuthstrat(java.lang.String authstrat) {
        this.authstrat = authstrat;
        return this;
    }

    @JsonProperty("authparams")
    public List<String> getAuthparams() {
        return authparams;
    }

    @JsonProperty("authparams")
    public void setAuthparams(List<String> authparams) {
        this.authparams = authparams;
    }

    public ListJobsParams withAuthparams(List<String> authparams) {
        this.authparams = authparams;
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
        return ((((((((((("ListJobsParams"+" [services=")+ services)+", filter=")+ filter)+", authstrat=")+ authstrat)+", authparams=")+ authparams)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
