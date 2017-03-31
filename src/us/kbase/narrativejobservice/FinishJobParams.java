
package us.kbase.narrativejobservice;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Generated;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import us.kbase.common.service.UObject;


/**
 * <p>Original spec-file type: FinishJobParams</p>
 * <pre>
 * Either 'result', 'error' or 'is_canceled' field should be defined;
 * result - keeps exact copy of what original server method puts
 *     in result block of JSON RPC response;
 * error - keeps exact copy of what original server method puts
 *     in error block of JSON RPC response;
 * is_cancelled - Deprecated (field is kept for backward 
 *     compatibility), please use 'is_canceled' instead.
 * </pre>
 * 
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("com.googlecode.jsonschema2pojo")
@JsonPropertyOrder({
    "result",
    "error",
    "is_cancelled",
    "is_canceled"
})
public class FinishJobParams {

    @JsonProperty("result")
    private UObject result;
    /**
     * <p>Original spec-file type: JsonRpcError</p>
     * <pre>
     * Error block of JSON RPC response
     * </pre>
     * 
     */
    @JsonProperty("error")
    private JsonRpcError error;
    @JsonProperty("is_cancelled")
    private Long isCancelled;
    @JsonProperty("is_canceled")
    private Long isCanceled;
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("result")
    public UObject getResult() {
        return result;
    }

    @JsonProperty("result")
    public void setResult(UObject result) {
        this.result = result;
    }

    public FinishJobParams withResult(UObject result) {
        this.result = result;
        return this;
    }

    /**
     * <p>Original spec-file type: JsonRpcError</p>
     * <pre>
     * Error block of JSON RPC response
     * </pre>
     * 
     */
    @JsonProperty("error")
    public JsonRpcError getError() {
        return error;
    }

    /**
     * <p>Original spec-file type: JsonRpcError</p>
     * <pre>
     * Error block of JSON RPC response
     * </pre>
     * 
     */
    @JsonProperty("error")
    public void setError(JsonRpcError error) {
        this.error = error;
    }

    public FinishJobParams withError(JsonRpcError error) {
        this.error = error;
        return this;
    }

    @JsonProperty("is_cancelled")
    public Long getIsCancelled() {
        return isCancelled;
    }

    @JsonProperty("is_cancelled")
    public void setIsCancelled(Long isCancelled) {
        this.isCancelled = isCancelled;
    }

    public FinishJobParams withIsCancelled(Long isCancelled) {
        this.isCancelled = isCancelled;
        return this;
    }

    @JsonProperty("is_canceled")
    public Long getIsCanceled() {
        return isCanceled;
    }

    @JsonProperty("is_canceled")
    public void setIsCanceled(Long isCanceled) {
        this.isCanceled = isCanceled;
    }

    public FinishJobParams withIsCanceled(Long isCanceled) {
        this.isCanceled = isCanceled;
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
        return ((((((((((("FinishJobParams"+" [result=")+ result)+", error=")+ error)+", isCancelled=")+ isCancelled)+", isCanceled=")+ isCanceled)+", additionalProperties=")+ additionalProperties)+"]");
    }

}
