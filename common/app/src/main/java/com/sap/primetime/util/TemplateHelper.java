package com.sap.primetime.util;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sap.primetime.dao.entities.File;

public class TemplateHelper {
	private TemplateParam[] params;

	public TemplateHelper(String params) {
		if (params != null) {
			Gson gson = new Gson();
			this.params = gson.fromJson(params, TemplateParam[].class);
		}
	}

	public String replace(String text, String values) {
		if (params == null) {
			return text;
		}
		JsonObject obj = parseValues(values);

		for (TemplateParam p : params) {
			String value = "-missing value-";
			if (obj != null && obj.has(p.getKey())) {
				value = obj.get(p.getKey()).getAsString();
				if ("file".equalsIgnoreCase(p.getType())) {
					value = "/s/api/fileservice/files/" + value + "/content";
				}
			} else if ("file".equalsIgnoreCase(p.getType())) {
				value = "/ui/img/image_preview.png";
			}
			text = StringUtils.replace(text, "{{" + p.getKey() + "}}", value);
		}

		return text;
	}

	public boolean containsFile(File file, String values) {
		JsonObject obj = parseValues(values);
		if (obj == null) {
			return false;
		}

		for (TemplateParam p : params) {
			if ("file".equalsIgnoreCase(p.getType()) && obj.has(p.getKey())
					&& obj.get(p.getKey()).getAsLong() == file.getId()) {
				return true;
			}
		}

		return false;
	}

	private JsonObject parseValues(String values) {
		JsonObject obj = null;
		if (StringUtils.isNotEmpty(values)) {
			JsonParser parser = new JsonParser();
			JsonElement element = parser.parse(values);
			obj = element.getAsJsonObject();
		}
		return obj;
	}

}
