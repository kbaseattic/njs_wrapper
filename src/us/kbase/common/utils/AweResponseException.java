package us.kbase.common.utils;

public class AweResponseException extends Exception {
    private static final long serialVersionUID = -1L;

    private int responseCode;
    private String httpReasonPhrase;
    private Object errorObject;
    
    public AweResponseException(String message, int responseCode, 
            String httpReasonPhrase, Object errorObject) {
        super(message);
        this.responseCode = responseCode;
        this.httpReasonPhrase = httpReasonPhrase;
        this.errorObject = errorObject;
    }
    
    public int getResponseCode() {
        return responseCode;
    }
    
    public String getHttpReasonPhrase() {
        return httpReasonPhrase;
    }
    
    public Object getErrorObject() {
        return errorObject;
    }
}
