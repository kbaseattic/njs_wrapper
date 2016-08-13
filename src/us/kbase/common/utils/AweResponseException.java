package us.kbase.common.utils;

public class AweResponseException extends Exception {
    private static final long serialVersionUID = -1L;

    private int responseCode;
    private String httpReasonPhrase;
    private String responseError;
    
    public AweResponseException(String message, int responseCode, 
            String httpReasonPhrase, String responseError) {
        super(message);
        this.responseCode = responseCode;
        this.httpReasonPhrase = httpReasonPhrase;
        this.responseError = responseError;
    }
    
    public int getResponseCode() {
        return responseCode;
    }
    
    public String getHttpReasonPhrase() {
        return httpReasonPhrase;
    }
    
    public String getResponseError() {
        return responseError;
    }
}
