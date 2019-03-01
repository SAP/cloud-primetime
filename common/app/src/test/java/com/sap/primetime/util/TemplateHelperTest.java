package com.sap.primetime.util;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonSyntaxException;
import com.sap.primetime.util.TemplateHelper;

public class TemplateHelperTest {
	private String params = "[{\"key\": \"key1\",\"name\": \"Name1\",\"type\": \"file\"},{\"key\": \"key2\",\"name\": \"Key2\", \"type\": \"text\"},{\"key\": \"key3\",\"name\": \"Key3\",\"type\": \"text\"}]";
	private String values = "{key2:\"value2\", key3:\"value3\"}";
	private TemplateHelper helper;

	@Before
	public void setup() throws Exception {
		helper = new TemplateHelper(params);
	}

	@Test
	public void testSimpleReplace() throws Exception {
		assertEquals("<div>value2</div>value3", helper.replace("<div>{{key2}}</div>{{key3}}", values));
	}

	@Test
	public void testMultiReplace() throws Exception {
		assertEquals("<div>value2</div>value2value3", helper.replace("<div>{{key2}}</div>{{key2}}{{key3}}", values));
	}

	@Test
	public void testImage() throws Exception {
		String testValues = "{key1:42, key2:\"value2\", key3:\"value3\"}";
		assertEquals("<div>/s/api/fileservice/files/42/content</div>value3",
				helper.replace("<div>{{key1}}</div>{{key3}}", testValues));
	}

	@Test
	public void testImagePlaceholder() throws Exception {
		assertEquals("<div>/ui/img/image_preview.png</div>value3",
				helper.replace("<div>{{key1}}</div>{{key3}}", values));
	}

	@Test
	public void testMissingParameters() throws Exception {
		helper = new TemplateHelper(null);
		assertEquals("<div>{{key2}}</div>{{key3}}", helper.replace("<div>{{key2}}</div>{{key3}}", values));

		helper = new TemplateHelper("");
		assertEquals("<div>{{key2}}</div>{{key3}}", helper.replace("<div>{{key2}}</div>{{key3}}", values));
	}

	@Test(expected = JsonSyntaxException.class)
	public void testCrappyParameters() throws Exception {
		helper = new TemplateHelper("some_crap");
	}

	@Test
	public void testMissingValues() throws Exception {
		assertEquals("<div>-missing value-</div>-missing value-", helper.replace("<div>{{key2}}</div>{{key3}}", null));
		assertEquals("<div>-missing value-</div>-missing value-", helper.replace("<div>{{key2}}</div>{{key3}}", ""));
		assertEquals("<div>/ui/img/image_preview.png</div>-missing value-",
				helper.replace("<div>{{key1}}</div>{{key3}}", null));
	}

	@Test
	public void testMissingText() throws Exception {
		assertEquals(null, helper.replace(null, values));
		assertEquals("", helper.replace("", values));
	}

	@Test(expected = IllegalStateException.class)
	public void testCrappyValues() throws Exception {
		String testValues = "crappy_values";
		assertEquals("<div>value2</div>value3", helper.replace("<div>{{key2}}</div>{{key3}}", testValues));
	}

}
