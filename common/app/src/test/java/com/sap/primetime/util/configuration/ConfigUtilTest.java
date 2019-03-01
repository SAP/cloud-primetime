package com.sap.primetime.util.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

import com.sap.primetime.dao.ConfigDAO;
import com.sap.primetime.dao.entities.Config;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.util.configuration.ConfigUtil.Source;

public class ConfigUtilTest extends PrimeTimeTest {
	@Before
	public void setup() throws Exception {
		prepareTest();
	}

	@Test
	public void testFallbacks() throws Exception {
		List<Source> oldFallbacks = ConfigUtil.getFallbackChain();

		ConfigUtil.setFallbacks(Arrays.asList(ConfigUtil.Source.FILE));

		assertEquals(1, ConfigUtil.getFallbackChain().size());
		assertEquals(ConfigUtil.Source.FILE, ConfigUtil.getFallbackChain().get(0));

		// put a value into the database
		String randGroup = RandomStringUtils.randomAlphabetic(10);
		assertNull(ConfigUtil.getProperty(randGroup, "prop1"));

		ConfigDAO.saveNew(new Config(randGroup, "prop1", "value1"));
		assertNull(ConfigUtil.getProperty(randGroup, "prop1"));

		// add database fallback, value must be available now
		ConfigUtil.setFallbacks(Arrays.asList(ConfigUtil.Source.FILE, ConfigUtil.Source.DB));
		assertEquals("value1", ConfigUtil.getProperty(randGroup, "prop1"));

		ConfigUtil.setFallbacks(oldFallbacks);
	}

	@Test
	public void testRefreshConfig() throws Exception {
		String randGroup = RandomStringUtils.randomAlphabetic(10);
		Config config = new Config(randGroup, "prop1", "value1");
		ConfigDAO.saveNew(config);
		assertEquals("value1", ConfigUtil.getProperty(randGroup, "prop1"));

		// update value
		config.setParamValue("value2");
		config = ConfigDAO.save(config);

		assertEquals("value1", ConfigUtil.getProperty(randGroup, "prop1"));

		ConfigUtil.reload();
		assertEquals("value2", ConfigUtil.getProperty(randGroup, "prop1"));
	}

}
