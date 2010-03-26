/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.silverpeas.SilverpeasSettings;

import java.util.List;
import org.jdom.Document;
import java.io.OutputStream;
import org.apache.commons.io.FileUtils;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import com.silverpeas.applicationbuilder.XmlDocument;
import com.silverpeas.file.GestionVariables;
import java.io.File;
import java.io.FileOutputStream;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

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
    assertEquals("test@silverpeas.com", gv.getValue("ADMIN_EMAIL"));
    assertEquals("http://www.silverpeas.com", gv.getValue("URL_SERVER"));
    assertEquals("5432", gv.getValue("SQL_LISTEN_PORT_POSTGRES"));
    assertEquals("silver", gv.getValue("DB_PASSWD"));
  }

  /**
   * Test of mergeConfigurationFiles method, of class SilverpeasSettings.
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
    assertNotNull(resultLines);
    assertEquals(expectedtLines.size(), resultLines.size());
    for(int i = 0; i < resultLines.size(); i++) {
      assertEquals(expectedtLines.get(i), resultLines.get(i));
    }
  }
}
