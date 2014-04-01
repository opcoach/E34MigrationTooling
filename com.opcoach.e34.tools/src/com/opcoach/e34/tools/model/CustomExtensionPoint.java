package com.opcoach.e34.tools.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomExtensionPoint {
	private String uniqueId = null;
	private Map<String, CustomSchema> schemas = new HashMap<String, CustomSchema>();

	public CustomExtensionPoint(String id) {
		this.uniqueId = id;
	}

	public String getUniqueId() {
		return uniqueId;
	}

	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}

	public CustomSchema getSchema(String schemaId) {
		if (schemas.containsKey(schemaId) == false) {
			CustomSchema schema = new CustomSchema(uniqueId , schemaId);
			schemas.put(schemaId, schema);
		}
		return schemas.get(schemaId);
	}

	public boolean containSchema(String schemaId) {
		return schemas.containsKey(schemaId);
	}

	public Collection<CustomSchema> getSchemas() {
		return schemas.values();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((uniqueId == null) ? 0 : uniqueId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CustomExtensionPoint other = (CustomExtensionPoint) obj;
		if (uniqueId == null) {
			if (other.uniqueId != null)
				return false;
		} else if (!uniqueId.equals(other.uniqueId))
			return false;
		return true;
	}
}
