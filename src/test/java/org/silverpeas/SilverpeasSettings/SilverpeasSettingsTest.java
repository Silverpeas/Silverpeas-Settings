/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of the GPL, you may
 * redistribute this Program in connection with Free/Libre Open Source Software ("FLOSS")
 * applications as described in Silverpeas's FLOSS exception. You should have received a copy of the
 * text describing the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.SilverpeasSettings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.silverpeas.applicationbuilder.XmlDocument;
import org.silverpeas.file.GestionVariables;

import static java.io.File.separatorChar;
import static org.silverpeas.SilverpeasSettings.SilverpeasSettings.*;

/**
 *
 * @author ehugonnet
 */
public class SilverpeasSettingsTest {

  private static final String resourcesDir = System.getProperty("basedir") + separatorChar +
       "target" + separatorChar + "test-classes" + separatorChar;

  public SilverpeasSettingsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    System.setProperty("silverpeas.home", resourcesDir);
    System.setProperty("SILVERPEAS_HOME", resourcesDir);
    System.setProperty("JBOSS_HOME", resourcesDir);
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of loadGlobalVariables method, of class SilverpeasSettings.
   *
   * @throws Exception
   */
  @Test
  public void testLoadGlobalVariables() throws Exception {
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(
        resourcesDir + "expected" + separatorChar + "MergedSettings.xml"));
    // Get the root element
    Element root = doc.getRootElement();
    GestionVariables gv = SilverpeasSettings.loadGlobalVariables(new File(
        resourcesDir + "expected"), root);
    Assert.assertEquals("test@silverpeas.com", gv.getValue("ADMIN_EMAIL"));
    Assert.assertEquals("http://www.silverpeas.com", gv.getValue("URL_SERVER"));
    Assert.assertEquals("5432", gv.getValue("SQL_LISTEN_PORT_POSTGRES"));
    Assert.assertEquals("silver", gv.getValue("DB_PASSWD"));
  }

  @Test
  public void testLoadConfiguration() throws Exception {
    File dir = new File(resourcesDir + "xml");
    GestionVariables properties = SilverpeasSettings.loadConfiguration(dir);
    Assert.assertEquals("c:/toto", properties.getValue("SILVERPEAS_HOME"));
    Assert.assertEquals("http://localhost:8000", properties.getValue("URL_SERVER"));
    Assert.assertEquals("80", properties.getValue("JBOSS_LISTEN_PORT"));
    Assert.assertEquals("silveradmin@localhost", properties.getValue("EXPED_EMAIL"));
  }

  /**
   * Test of mergeConfigurationFiles method, of class SilverpeasSettings.
   *
   * @throws Exception
   */
  @Test
  public void testMergeConfigurationFiles() throws Exception {
    File dirXml = new File(resourcesDir + "xml");
    XmlDocument fileXml = new XmlDocument(dirXml, SilverpeasSettings.SILVERPEAS_SETTINGS);
    fileXml.load();
    SilverpeasSettings.mergeConfigurationFiles(fileXml, dirXml);
    fileXml.setOutputEncoding("UTF-8");
    XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());

    File tempDir = new File(System.getProperty("basedir") + separatorChar + "target" +
         separatorChar + "temp");
    tempDir.mkdirs();
    File mergedFile = new File(tempDir, "merge_result.xml");
    OutputStream out = new FileOutputStream(mergedFile);
    output.output(fileXml.getDocument(), out);
    out.close();
    @SuppressWarnings("unchecked")
    List<String> resultLines = FileUtils.readLines(mergedFile, "UTF-8");
    @SuppressWarnings("unchecked")
    List<String> expectedtLines = FileUtils.readLines(new File(resourcesDir + "expected" +
         separatorChar + "MergedSettings.xml"), "UTF-8");
    Assert.assertNotNull(resultLines);
    Assert.assertEquals(expectedtLines.size(), resultLines.size());
    for (int i = 0; i < resultLines.size(); i++) {
      Assert.assertEquals(expectedtLines.get(i), resultLines.get(i));
    }
  }

  @Test
  public void xmlFileTransformation() throws Exception {
    GestionVariables gestion = new GestionVariables(new Properties());
    gestion.addVariable("JBOSS_LISTEN_PORT", "9500");
    gestion.addVariable("SILVERPEAS_DATA_HOME_DEPENDANT", resourcesDir + "data");
    gestion.addVariable("SILVERPEAS_HOME_DEPENDANT", resourcesDir + "toto");
    String dir = resourcesDir + "transform" + separatorChar;
    Element element = new Element(XML_FILE_TAG);
    element.setAttribute(FILE_NAME_ATTRIB, "jbossweb-tomcat55.sar/server.xml");
    Collection<Element> values = new ArrayList<Element>(2);
    values.add(getValueElement("@URIEncoding", "UTF-8"));
    values.add(getValueElement("@port", "${JBOSS_LISTEN_PORT}"));
    element.addContent(getParameterElement(
        "//Service[@name='jboss.web']/Connector[@maxThreads='250']",
        "update", values));
    values = new ArrayList<Element>(2);
    values.add(getValueElement("@docBase", "${SILVERPEAS_DATA_HOME_DEPENDANT}/data/weblib"));
    values.add(getValueElement("@path", "/weblib"));
    element.addContent(getParameterElement("/Server/Service[@name='jboss.web']/Engine[@name='jboss.web']/" +
         "Host[@name='localhost']/Context[@path='/weblib']", "update", values));
    values = new ArrayList<Element>(2);
    values.add(getValueElement("@docBase", "${SILVERPEAS_DATA_HOME_DEPENDANT}/data/website"));
    values.add(getValueElement("@path", "/website"));
    element.addContent(getParameterElement("//Service[@name='jboss.web']/Engine[@name='jboss.web']" +
         "/Host[@name='localhost']/Context[@path='/website']", "update", values));
    values = new ArrayList<Element>(2);
    values.add(getValueElement("@docBase", "${SILVERPEAS_HOME_DEPENDANT}/help/fr"));
    values.add(getValueElement("@path", "/help_fr"));
    element.addContent(getParameterElement("/Server/Service[@name='jboss.web']/Engine[@name='jboss.web']/" +
         "Host[@name='localhost']/Context[@path='/help_fr']", "update", values));
    xmlfile(dir, element, gestion);
    @SuppressWarnings("unchecked")
    List<String> resultLines = FileUtils.readLines(new File(resourcesDir + "transform" +
         separatorChar + "jbossweb-tomcat55.sar" + separatorChar + "server.xml"), "UTF-8");
    @SuppressWarnings("unchecked")
    List<String> expectedtLines = FileUtils.readLines(new File(resourcesDir + "expected_transform" +
         separatorChar + "server.xml"), "UTF-8");
    Assert.assertNotNull(resultLines);
    Assert.assertEquals(expectedtLines.size(), resultLines.size());
    for (int i = 0; i < resultLines.size(); i++) {
      Assert.assertEquals(expectedtLines.get(i), resultLines.get(i));
    }
  }

  protected Element getValueElement(String location, String value) {
    Element element = new Element(VALUE_TAG);
    element.setAttribute(VALUE_LOCATION_ATTRIB, location);
    element.setText(value);
    return element;
  }

  protected Element getParameterElement(String key, String mode, Collection<Element> values) {
    Element element = new Element(PARAMETER_TAG);
    element.setAttribute(PARAMETER_KEY_ATTRIB, key);
    element.setAttribute(XPATH_MODE_ATTRIB, mode);
    element.setContent(values);
    return element;
  }
}
