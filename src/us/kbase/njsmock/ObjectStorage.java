package us.kbase.njsmock;

import java.util.List;
import java.util.Map;

import us.kbase.common.service.Tuple11;
import us.kbase.workspace.ListObjectsParams;
import us.kbase.workspace.ObjectData;
import us.kbase.workspace.ObjectIdentity;
import us.kbase.workspace.SaveObjectsParams;
import us.kbase.workspace.SubObjectIdentity;

public interface ObjectStorage {

	public List<ObjectData> getObjects(String authToken, List<ObjectIdentity> objectIds) throws Exception;
    
    public List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String,String>>> saveObjects(
    		String authToken, SaveObjectsParams params) throws Exception;

	public List<Tuple11<Long, String, String, String, Long, String, Long, String, String, Long, Map<String, String>>> listObjects(
			String authToken, ListObjectsParams params) throws Exception;

    public List<ObjectData> getObjectSubset(String authToken, List<SubObjectIdentity> objectIds) throws Exception;
    
    public String getUrl();
}
