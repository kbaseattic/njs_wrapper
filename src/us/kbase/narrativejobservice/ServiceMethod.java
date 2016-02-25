
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
 * <p>Original spec-file type: service_method</p>
 * <pre>
 * service_url could be empty in case of docker image of service loaded from registry,
 * service_version - optional parameter defining version of service docker image.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "service_name",
    "method_name",
    "service_url",
    "service_version"
})
public class ServiceMethod {

    @JsonProperty("service_name")
    private String serviceName;
    @JsonProperty("method_name")
    private String methodName;
    @JsonProperty("service_url")
    private String serviceUrl;
    @JsonProperty("service_version")
    private String serviceVersion;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("service_name")
    public String getServiceName() {
        return serviceName;
    }

    @JsonProperty("service_name")
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public ServiceMethod withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    @JsonProperty("method_name")
    public String getMethodName() {
        return methodName;
    }

    @JsonProperty("method_name")
    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public ServiceMethod withMethodName(String methodName) {
        this.methodName = methodName;
        return this;
    }

    @JsonProperty("service_url")
    public String getServiceUrl() {
        return serviceUrl;
    }

    @JsonProperty("service_url")
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public ServiceMethod withServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        return this;
    }

    @JsonProperty("service_version")
    public String getServiceVersion() {
        return serviceVersion;
    }

    @JsonProperty("service_version")
    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public ServiceMethod withServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
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
        return ((((((((((("ServiceMethod"+" [serviceName=")+ serviceName)+", methodName=")+ methodName)+", serviceUrl=")+ serviceUrl)+", serviceVersion=")+ serviceVersion)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
