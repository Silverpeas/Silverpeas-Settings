/**
 * Copyright (C) 2000 - 2009 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have recieved a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://repository.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.silverpeas.SilverpeasSettings;

import com.silverpeas.applicationbuilder.XmlDocument;
import com.silverpeas.file.GestionVariables;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
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

/**
 *
 * @author ehugonnet
 */
public class SilverpeasSettingsTest {

  public SilverpeasSettingsTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
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
   * @throws Exception 
   */
  @Test
  public void testLoadGlobalVariables() throws Exception {
    SAXBuilder builder = new SAXBuilder();
    Document doc = builder.build(new File(System.getProperty("basedir")
            + File.separatorChar + "target" + File.separatorChar + "test-classes"
            + File.separatorChar + "expected" + File.separatorChar + "MergedSettings.xml"));
    // Get the root element
    Element root = doc.getRootElement();
    GestionVariables gv = SilverpeasSettings.loadGlobalVariables(root);
    Assert.assertEquals("test@silverpeas.com", gv.getValue("ADMIN_EMAIL"));
    Assert.assertEquals("http://www.silverpeas.com", gv.getValue("URL_SERVER"));
    Assert.assertEquals("5432", gv.getValue("SQL_LISTEN_PORT_POSTGRES"));
    Assert.assertEquals("silver", gv.getValue("DB_PASSWD"));
  }

  /**
   * Test of mergeConfigurationFiles method, of class SilverpeasSettings.
   * @throws Exception 
   */
  @Test
  public void testMergeConfigurationFiles() throws Exception {
    File dirXml = new File(System.getProperty("basedir") + File.separatorChar + "target"
            + File.separatorChar + "test-classes" + File.separatorChar + "xml");
    XmlDocument fileXml = new XmlDocument(dirXml, SilverpeasSettings.SILVERPEAS_SETTINGS);
    fileXml.load();
    SilverpeasSettings.mergeConfigurationFiles(fileXml, dirXml);
    fileXml.setOutputEncoding("UTF-8");
    XMLOutputter output = new XMLOutputter(Format.getPrettyFormat());

    File tempDir = new File(System.getProperty("basedir") + File.separatorChar + "target"
            + File.separatorChar + "temp");
    tempDir.mkdirs();
    File mergedFile = new File(tempDir, "merge_result.xml");
    OutputStream out = new FileOutputStream(mergedFile);
    output.output(fileXml.getDocument(), out);
    out.close();
    List<String> resultLines = FileUtils.readLines(mergedFile, "UTF-8");
    List<String> expectedtLines = FileUtils.readLines(new File(System.getProperty("basedir")
            + File.separatorChar + "target" + File.separatorChar + "test-classes"
            + File.separatorChar + "expected" + File.separatorChar + "MergedSettings.xml"), "UTF-8");
    Assert.assertNotNull(resultLines);
    Assert.assertEquals(expectedtLines.size(), resultLines.size());
    for(int i = 0; i < resultLines.size(); i++) {
      Assert.assertEquals(expectedtLines.get(i), resultLines.get(i));
    }
  }
}
