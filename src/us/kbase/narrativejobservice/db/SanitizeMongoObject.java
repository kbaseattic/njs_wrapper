package us.kbase.narrativejobservice.db;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SanitizeMongoObject {

	//TODO SHOCK add tests for this junk
	
	public static void sanitize(final Map<String, Object> data) {
		alterKeys(data, new KeyModifier() {
			
			@Override
			public String modify(String key) {
				//slightly inefficient, but meh
				return key.replace("%", "%25").replace("$", "%24")
						.replace(".", "%2e");
			}
		});
	}
	
	public static void befoul(final Map<String, Object> data) {
		alterKeys(data, new KeyModifier() {
			
			@Override
			public String modify(String key) {
				//slightly inefficient, but meh
				return key.replace("%2e", ".").replace("%24", "$")
						.replace("%25", "%");
			}
		});
	}
	
	private static interface KeyModifier {
		public String modify(String key);
	}

	//rewrite w/o recursion?
	private static Object alterKeys(final Object o, final KeyModifier mod) {
		if (o instanceof String || o instanceof Number ||
				o instanceof Boolean || o == null) {
			return o;
		} else if (o instanceof List) {
			@SuppressWarnings("unchecked")
			final List<Object> l = (List<Object>)o;
			for (Object lo: l) {
				alterKeys(lo, mod);
			}
			return o;
		} else if (o instanceof Map) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> m = (Map<String, Object>)o;
			//save updated keys in separate map so we don't overwrite
			//keys before they're escaped
			final Map<String, Object> newm = new HashMap<String, Object>();
			final Iterator<Entry<String, Object>> iter = m.entrySet().iterator();
			while (iter.hasNext()) {
				final Entry<String, Object> e = iter.next();
				final Object value = alterKeys(e.getValue(), mod);
				final String newkey = mod.modify(e.getKey());
				iter.remove();
				newm.put(newkey, value);
			}
			m.putAll(newm);
			return o;
		} else {
			throw new IllegalStateException(
					"Unsupported class: " + o.getClass());
		}
	}
}
