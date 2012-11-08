package net.ivoa.pdr.utils;

import java.util.List;

/**
 * @author Carlo Maria Zwolf
 * Observatoire de Paris
 * LERMA
 */


public class PDRMap {
	public String getKey() {
		return key;
	}
	public List<String> getValues() {
		return values;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public void setValues(List<String> values) {
		this.values = values;
	}
	public PDRMap(String key, List<String> values) {
		super();
		this.key = key;
		this.values = values;
	}
	private String key;
	private List<String> values;
}
