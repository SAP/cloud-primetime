package com.sap.primetime.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import javax.ws.rs.WebApplicationException;

import org.junit.Before;
import org.junit.Test;

import com.github.zafarkhaja.semver.Version;
import com.sap.primetime.testutil.PrimeTimeTest;
import com.sap.primetime.util.configuration.ConfigSection;
import com.sap.primetime.util.configuration.ConfigSectionParam;
import com.sap.primetime.util.configuration.ConfigUtil;
import com.sap.primetime.util.configuration.ConfigUtil.Source;

public class SystemServiceTest extends PrimeTimeTest {
	private SystemService systemService;

	@Before
	public void setup() throws Exception {
		prepareTest();

		systemService = spy(new SystemService());
		systemService.request = requestMock;
	}

	@Test
	public void testGetSystemInfo() throws Exception {
		assertNotNull(systemService.getSytemInfo().getCurrentTime());
	}

	@Test(expected = WebApplicationException.class)
	public void testGetConfigAsNonAdmin() throws Exception {
		systemService.getConfigSections();
	}

	@Test
	public void testGetConfiguration() throws Exception {
		// simulate user with admin priviledges
		doReturn(true).when(systemService).isAdmin();

		assertTrue(systemService.getConfigSections().size() > 0);
		assertTrue(systemService.getConfigSections().get(0).getParameters().size() > 0);
		assertNotNull(systemService.getConfigSections().get(0).getParameters().get(0).getCurrentValue());
	}

	@Test
	public void testUpdateConfiguration() throws Exception {
		// simulate user with admin priviledges
		doReturn(true).when(systemService).isAdmin();

		assertEquals(Source.FILE, systemService.getConfigSections().get(0).getParameters().get(0).getSource());
		assertEquals(Source.FILE, systemService.getConfigSections().get(0).getParameters().get(0).getResetSource());

		ConfigSection section = systemService.getConfigSections().get(0);
		String oldVal = section.getParameters().get(0).getCurrentValue();
		String newVal = oldVal + "new";
		section.getParameters().get(0).setCurrentValue(newVal);
		systemService.updateConfigSection(section, section.getKey());
		assertEquals(newVal, systemService.getConfigSections().get(0).getParameters().get(0).getCurrentValue());
		assertEquals(oldVal, systemService.getConfigSections().get(0).getParameters().get(0).getResetValue());
		assertEquals(Source.DB, systemService.getConfigSections().get(0).getParameters().get(0).getSource());
		assertEquals(Source.FILE, systemService.getConfigSections().get(0).getParameters().get(0).getResetSource());

		// empty should convert to null for more streamlined handling of config values
		newVal = "";
		section.getParameters().get(0).setCurrentValue(newVal);
		systemService.updateConfigSection(section, section.getKey());
		assertNull(systemService.getConfigSections().get(0).getParameters().get(0).getCurrentValue());
		assertEquals(Source.DB, systemService.getConfigSections().get(0).getParameters().get(0).getSource());
		assertEquals(Source.FILE, systemService.getConfigSections().get(0).getParameters().get(0).getResetSource());

		// removing the setting should show the reset values again
		systemService.deleteDBConfigKey(section.getKey(), section.getParameters().get(0).getKey());
		assertEquals(oldVal, systemService.getConfigSections().get(0).getParameters().get(0).getCurrentValue());
		assertEquals(oldVal, systemService.getConfigSections().get(0).getParameters().get(0).getResetValue());
		assertEquals(Source.FILE, systemService.getConfigSections().get(0).getParameters().get(0).getSource());
		assertEquals(Source.FILE, systemService.getConfigSections().get(0).getParameters().get(0).getResetSource());
	}

	@Test
	public void testPropertyNotInitiallyInFile() throws Exception {
		// simulate user with admin priviledges
		doReturn(true).when(systemService).isAdmin();

		// add new so not existing parameter
		ConfigUtil.getConfigSections().get(0).addParameter(new ConfigSectionParam("test", "value"));

		ConfigSection section = systemService.getConfigSections().get(0);
		int paramIdx = section.getParameters().size() - 1;
		assertEquals(null, section.getParameters().get(paramIdx).getSource());

		section.getParameters().get(paramIdx).setCurrentValue("somevalue");
		systemService.updateConfigSection(section, section.getKey());
		assertEquals(Source.DB, systemService.getConfigSections().get(0).getParameters().get(paramIdx).getSource());

		// should survive reload
		systemService.reloadConfiguration();
		assertEquals(Source.DB, systemService.getConfigSections().get(0).getParameters().get(paramIdx).getSource());

		// setting to empty should return null DB value
		section.getParameters().get(paramIdx).setCurrentValue("");
		systemService.updateConfigSection(section, section.getKey());
		assertEquals(null, systemService.getConfigSections().get(0).getParameters().get(paramIdx).getCurrentValue());
		assertEquals(Source.DB, systemService.getConfigSections().get(0).getParameters().get(paramIdx).getSource());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalSemVer() throws Exception {
		Version.valueOf(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIllegalSemVer2() throws Exception {
		Version.valueOf("");
	}

	@Test
	public void testSemVer() throws Exception {
		Version v1 = Version.valueOf("1.1.2");
		Version v2 = Version.valueOf("1.1.3");

		assertFalse(v1.greaterThan(v2));
		assertFalse(v1.greaterThanOrEqualTo(v2));
		assertTrue(v1.lessThan(v2));
		assertTrue(v1.lessThanOrEqualTo(v2));
	}
}
