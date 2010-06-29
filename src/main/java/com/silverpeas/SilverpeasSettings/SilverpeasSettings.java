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

import com.silverpeas.applicationbuilder.AppBuilderException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jdom.Document;
import org.jdom.Element;

import com.silverpeas.file.BackupFile;
import com.silverpeas.file.FileUtil;
import com.silverpeas.file.GestionVariables;
import com.silverpeas.file.ModifProperties;
import com.silverpeas.file.ModifText;
import com.silverpeas.file.ModifTextSilverpeas;
import com.silverpeas.file.ModifXMLSilverpeas;
import com.silverpeas.applicationbuilder.XmlDocument;
import com.silverpeas.file.RegexpElementMotif;
import com.silverpeas.installedtree.DirectoryLocator;
import com.silverpeas.xml.XmlTreeHandler;
import com.silverpeas.xml.xpath.XPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import org.apache.commons.io.FileUtils;

public class SilverpeasSettings {

  static final String NEW_LINE = System.getProperty("line.separator");
  static File fileLog = null;
  static PrintWriter bufLog = null;
  static XPath _xpathEngine = null;
  private static final String[] TAGS_TO_MERGE = { "global-vars", "fileset" };
  private static List<File> xmlFiles;
  private static final String TOOL_VERSION = "SilverpeasSettings V5.0";
  protected static final String DIR_SETTINGS = DirectoryLocator.getSilverpeasHome()
      + "/setup/settings";
  protected static final String SILVERPEAS_SETTINGS = "SilverpeasSettings.xml";
  protected static final String SILVERPEAS_CONFIG = "config.xml";
  protected static final String DEPENDENCIES_TAG = "dependencies";
  protected static final String SETTINGSFILE_TAG = "settingsfile";
  static final String FILENAME_ATT = "name";
  protected static final String CONFIG_FILE_TAG = "configfile";
  protected static final String TEXT_FILE_TAG = "textfile";
  protected static final String COPY_FILE_TAG = "copyfile";
  protected static final String COPY_DIR_TAG = "copydir";
  protected static final String XML_FILE_TAG = "xmlfile";
  protected static final String XPATH_MODE_ATTRIB = "mode";
  static final Map _modeMap = new HashMap(5);
  static final String VALUE_LOCATION_ATTRIB = "location";
  static final String RELATIVE_VALUE_ATTRIB = "relative-to";

  static {
    _modeMap.put("select", Character.valueOf(XmlTreeHandler.MODE_SELECT));
    _modeMap.put("insert", Character.valueOf(XmlTreeHandler.MODE_INSERT));
    _modeMap.put("update", Character.valueOf(XmlTreeHandler.MODE_UPDATE));
    _modeMap.put("delete", Character.valueOf(XmlTreeHandler.MODE_DELETE));
    _modeMap.put("unique", Character.valueOf(XmlTreeHandler.MODE_UNIQUE));
  }

  protected static char getXmlMode(String textualMode) {
    if (textualMode == null || textualMode.equals("")) {
      return XmlTreeHandler.MODE_UPDATE;
    }
    return ((Character) _modeMap.get(textualMode.toLowerCase())).charValue();
  }

  protected static String getXmlModeString(String textualMode) {
    if (textualMode == null || textualMode.equals("")) {
      return "default(update)";
    }
    return textualMode;
  }

  protected static XPath getXPathEngine() {
    if (_xpathEngine == null) {
      _xpathEngine = new XPath();
    }
    return _xpathEngine;
  }

  protected static String getRelativePath(final String base, final String path) {
    String result = path;
    String relBase = base;
    String resultBase = null;
    boolean baseUnixSep;
    int nbLevel;

    // BASE (../.. etc)

    // removes drive
    if (relBase != null && relBase.length() >= 2 && relBase.charAt(1) == ':') {
      relBase = relBase.substring(2);
    }
    // detects file separator
    baseUnixSep = (relBase != null && relBase.indexOf('/') != -1);
    // removes starting file separator
    if (relBase != null && relBase.length() >= 1
        && relBase.charAt(0) == (baseUnixSep ? '/' : '\\')) {
      relBase = relBase.substring(1);
    }
    // removes ending file separator
    if (relBase != null && relBase.length() >= 1
        && relBase.endsWith(baseUnixSep ? "/" : "\\")) {
      relBase = relBase.substring(0, relBase.length() - 2);
    }
    // detects number of levels
    if (relBase == null || relBase.length() == 0) {
      nbLevel = 0;
    } else {
      StringTokenizer st = new StringTokenizer(relBase, baseUnixSep ? "/" : "\\");
      nbLevel = st.countTokens();
    }
    // creates the base (../.. etc)
    for (int i = 0; i < nbLevel; i++) {
      if (i == 0) {
        resultBase = "..";
      } else {
        resultBase += (baseUnixSep ? "/" : "\\") + "..";
      }
    }
    // removes drive
    if (result.length() >= 2 && result.charAt(1) == ':') {
      result = result.substring(2);
    }
    // detects file separator
    baseUnixSep = (result != null && result.indexOf('/') != -1);
    // adds starting file separator
    if (result != null && result.length() >= 1
        && result.charAt(0) != (baseUnixSep ? '/' : '\\')) {
      result = (baseUnixSep ? "/" : "\\") + result;
    }
    result = resultBase + result;
    return result;
  }

  /**
   * @param args
   * @see
   */
  public static void main(String[] args) {
    try {
      System.out.println("start settings of " + TOOL_VERSION + " (" + new Date() + ").");
      fileLog = new File(DirectoryLocator.getLogHome()
          + "/SilverpeasSettings.log");
      bufLog = new PrintWriter(new BufferedWriter(new FileWriter(fileLog.getAbsolutePath(), true)));
      displayMessageln(NEW_LINE
          + "************************************************************************");
      displayMessageln("start settings of Silverpeas (" + new java.util.Date()
          + ").");
      if (args.length != 0) {
        throw new Exception("parameters forbidden");
      }
      File dirXml = new File(DIR_SETTINGS);
      XmlDocument fileXml = new XmlDocument(dirXml, SILVERPEAS_CONFIG);
      fileXml.load();

      XmlDocument fXml = new XmlDocument(dirXml, SILVERPEAS_SETTINGS);
      fXml.load();
      fileXml.mergeWith(TAGS_TO_MERGE, fXml);

      // merge tous les fichiers de configurations
      displayMessageln(NEW_LINE + "merged files with " + SILVERPEAS_SETTINGS + " :");

      mergeConfigurationFiles(fileXml, dirXml);
      Document doc = fileXml.getDocument();
      // Get the root element
      Element root = doc.getRootElement();
      GestionVariables gv = loadGlobalVariables(root);
      // liste des chemins des fichiers
      displayMessageln(NEW_LINE + "modified files :");
      List listeFileSet = root.getChildren("fileset");
      Iterator iterFileSet = listeFileSet.iterator();
      while (iterFileSet.hasNext()) {
        Element eltFileSet = (Element) iterFileSet.next();
        String dir = eltFileSet.getAttributeValue("root");

        List listeActions = eltFileSet.getChildren();
        Iterator iterActions = listeActions.iterator();
        while (iterActions.hasNext()) {
          try {
            Element action = (Element) iterActions.next();
            if (action.getName().equals(CONFIG_FILE_TAG)) {
              configfile(dir, action, gv);
            } else if (action.getName().equals(TEXT_FILE_TAG)) {
              textfile(dir, action, gv);
            } else if (action.getName().equals(COPY_FILE_TAG)) {
              copyfile(dir, action, gv);
            } else if (action.getName().equals(XML_FILE_TAG)) {
              xmlfile(dir, action, gv);
            } else {
              displayMessageln("Unknown setting action : " + action.getName());
            }
          } catch (Exception e) {
            printError(e.toString());
          }
        } // while actions
      } // while fileset
      displayMessageln(NEW_LINE + "Silverpeas has been successfuly configured (" + new Date()
          + ").");
      bufLog.close();
      System.out.println(NEW_LINE + "Silverpeas has been successfuly configured (" + new Date()
          + ").");
    } catch (Exception e) {
      printError(e.toString());
      e.printStackTrace();
    }
  }

  // ---------------------------------------------------------------------
  /**
   * @param errMsg
   * @see
   */
  private static void configfile(String dir, Element eltConfigFile,
      GestionVariables gv) throws Exception {
    String dirFile = dir + eltConfigFile.getAttributeValue("name");
    dirFile = gv.resolveAndEvalString(dirFile);
    String typeFile = FileUtil.getExtension(dirFile);
    displayMessageln(dirFile);

    // fichiers xml
    if ("xml".equalsIgnoreCase(typeFile)) {
      ModifXMLSilverpeas fic = new ModifXMLSilverpeas(dirFile);
      // liste des parametres a modifier
      List listeParameter = eltConfigFile.getChildren("parameter");
      Iterator iterParameter = listeParameter.iterator();
      while (iterParameter.hasNext()) {
        Element eltParameter = (Element) iterParameter.next();
        String key = eltParameter.getAttributeValue("key");
        String value = eltParameter.getTextTrim();
        value = gv.resolveAndEvalString(value);
        fic.addModification(key, value);
        displayMessageln("\tkey = " + key + "\t value = " + value);
      }
      fic.executeModification();
    } // fichiers properties
    else if ("properties".equalsIgnoreCase(typeFile)) {
      ModifProperties fic = new ModifProperties(dirFile);

      // liste des parametres a modifier
      List listeParameter = eltConfigFile.getChildren("parameter");
      Iterator iterParameter = listeParameter.iterator();
      while (iterParameter.hasNext()) {
        Element eltParameter = (Element) iterParameter.next();
        String key = eltParameter.getAttributeValue("key");
        String value = eltParameter.getTextTrim();
        value = gv.resolveAndEvalString(value);
        fic.addModification(key, value);
        displayMessageln("\tkey = " + key + "\t value = " + value);
      }
      fic.executeModification();
    } else {
      // fichiers fonctionnants avec le mode key '=' value
      ModifTextSilverpeas fic = new ModifTextSilverpeas(dirFile);

      // liste des parametres a modifier
      List listeParameter = eltConfigFile.getChildren("parameter");
      Iterator iterParameter = listeParameter.iterator();
      while (iterParameter.hasNext()) {
        Element eltParameter = (Element) iterParameter.next();
        String key = eltParameter.getAttributeValue("key");
        String value = eltParameter.getTextTrim();
        value = gv.resolveAndEvalString(value);
        fic.addModification(key, value);
        displayMessageln("\tkey = " + key + "\t value = " + value);
      }
      fic.executeModification();
    }
  }

  // ---------------------------------------------------------------------
  private static void textfile(String dir, Element eltTextFile,
      GestionVariables gv) throws Exception {
    String dirFile = dir + eltTextFile.getAttributeValue("name");
    dirFile = gv.resolveString(dirFile);
    dirFile = gv.resolveAndEvalString(dirFile);
    File modifFile = new File(dirFile);
    if (modifFile.exists()) {
      BackupFile bf = new BackupFile(modifFile);
      bf.makeBackup();
    }
    displayMessageln(dirFile);
    ModifText fic = new ModifText(dirFile);
    // liste des parametres a modifier
    List listeParameter = eltTextFile.getChildren("parameter");
    Iterator iterParameter = listeParameter.iterator();
    while (iterParameter.hasNext()) {
      Element eltParameter = (Element) iterParameter.next();
      String key = eltParameter.getAttributeValue("key");
      String option = eltParameter.getAttributeValue("use-regex");
      String value = eltParameter.getTextTrim();
      value = gv.resolveAndEvalString(value);
      if ("true".equalsIgnoreCase(option)) {
        displayMessageln("\tregex = " + key + "\t value = " + value);
        RegexpElementMotif emv = new RegexpElementMotif(key);
        emv.setRemplacement(value);
        fic.addModification(emv);
      } else {
        fic.addModification(key, value);
        displayMessageln("\tkey = " + key + "\t value = " + value);
      }
    }
    fic.executeModification();
  }

  private static void xmlfile(String dir, Element eltConfigFile,
      GestionVariables gv) throws Exception {
    String dirFile = dir + eltConfigFile.getAttributeValue("name");
    // ici nouveaut� t005 : evaluation dynamique
    // dirFile = gv.resolveString( dirFile );
    dirFile = gv.resolveAndEvalString(dirFile);
    displayMessageln(dirFile);
    File dirFileFile = new File(dirFile);
    boolean backuped = false;

    XmlDocument xmlDoc = new XmlDocument(dirFileFile.getParentFile(),
        dirFileFile.getName());
    xmlDoc.load();
    getXPathEngine().setStartingElement(xmlDoc.getDocument().getRootElement());

    // liste des parametres a modifier
    List listeParameter = eltConfigFile.getChildren("parameter");
    Iterator iterParameter = listeParameter.iterator();
    while (iterParameter.hasNext()) {
      Element eltParameter = (Element) iterParameter.next();
      String key = eltParameter.getAttributeValue("key");
      key = gv.resolveAndEvalString(key);
      String mode = eltParameter.getAttributeValue(XPATH_MODE_ATTRIB);
      displayMessageln("\t" + key + " (mode:" + getXmlModeString(mode) + ")");
      getXPathEngine().setXPath(key);
      // backup handling
      getXPathEngine().setMode(XmlTreeHandler.MODE_SELECT);
      getXPathEngine().parse();
      if (!backuped && !getXPathEngine().exists().booleanValue()) {
        BackupFile bf = new BackupFile(dirFileFile);
        bf.makeBackup();
        backuped = true;
      }
      // action
      getXPathEngine().setMode(getXmlMode(mode));
      getXPathEngine().parse();
      if (eltParameter.getChildren() != null
          && !eltParameter.getChildren().isEmpty()) {
        getXPathEngine().setNodeAsStart();
        List listeValue = eltParameter.getChildren("value");
        Iterator iterValue = listeValue.iterator();
        while (iterValue.hasNext()) {
          Element eltValue = (Element) iterValue.next();
          String location = eltValue.getAttributeValue(VALUE_LOCATION_ATTRIB);
          location = gv.resolveAndEvalString(location);
          String childMode = eltValue.getAttributeValue(XPATH_MODE_ATTRIB);
          String value = eltValue.getTextTrim();
          value = gv.resolveAndEvalString(value);
          String relativePath = eltValue.getAttributeValue(RELATIVE_VALUE_ATTRIB);
          if (relativePath != null && !relativePath.equals("")) {
            relativePath = gv.resolveAndEvalString(relativePath);
            value = getRelativePath(relativePath, value);
          }
          displayMessage("\t\tlocation:" + location + "\tvalue:" + value);
          getXPathEngine().setXPath(location);
          // backup handling
          getXPathEngine().setMode(XmlTreeHandler.MODE_SELECT);
          getXPathEngine().parse();
          if (!backuped
              && (!getXPathEngine().exists().booleanValue() || !getXPathEngine().getValue().equals(
              value))) {
            BackupFile bf = new BackupFile(dirFileFile);
            bf.makeBackup();
            backuped = true;
          }
          // action
          if (childMode != null && !childMode.equals("")) {
            displayMessageln("\tmode:" + getXmlModeString(childMode));
            getXPathEngine().setMode(getXmlMode(childMode));
          } else {
            displayMessageln("\tmode:inherited(" + getXmlModeString(mode) + ")");
            getXPathEngine().setMode(getXmlMode(mode));
          }
          getXPathEngine().parse();
          getXPathEngine().setValue(value);
        }
      } else {
        String value = eltParameter.getTextTrim();
        value = gv.resolveAndEvalString(value);
        String relativePath = eltParameter.getAttributeValue(RELATIVE_VALUE_ATTRIB);
        if (relativePath != null && !relativePath.equals("")) {
          relativePath = gv.resolveAndEvalString(relativePath);
          value = getRelativePath(relativePath, value);
        }
        displayMessageln("\tvalue:" + value);
        // backup handling
        if (!backuped && !getXPathEngine().getValue().equals(value)) {
          BackupFile bf = new BackupFile(dirFileFile);
          bf.makeBackup();
          backuped = true;
        }
        // action
        getXPathEngine().setValue(value);
      }
    }
    xmlDoc.save();
  }

  // ---------------------------------------------------------------------
  private static void copyfile(String dir, Element eltTextFile,
      GestionVariables gv) throws Exception {
    String dirFile = dir + eltTextFile.getAttributeValue("name");
    dirFile = gv.resolveAndEvalString(dirFile);
    File sourceFile = new File(dirFile);
    String destFile = eltTextFile.getTextTrim();
    destFile = gv.resolveAndEvalString(destFile);
    File destFileFile = new File(destFile);
    if (!destFileFile.isAbsolute()) {
      destFile = dir + destFile;
      destFile = gv.resolveAndEvalString(destFile);
      destFileFile = new File(destFile);
    }
    if (sourceFile.isDirectory()) {
      if (destFileFile.exists()) {
        FileUtil.deleteFiles(destFile);
        destFileFile.mkdir();
      }
      FileUtil.copyDir(sourceFile, destFileFile);
    } else {
      if (destFileFile.exists()) {
        BackupFile bf = new BackupFile(destFileFile);
        bf.makeBackup();
      }
      FileUtil.copyFile(sourceFile, destFileFile);
    }
    displayMessageln(dirFile + System.getProperty("line.separator") + "\tcopied to " + destFile);
  }

  private static void printError(String errMsg) {
    if (bufLog != null) {
      displayMessageln(NEW_LINE + errMsg);
      bufLog.close();
    }
    System.out.println(NEW_LINE + errMsg + NEW_LINE);
  }

  private static void displayMessageln(String msg) {
    displayMessage(msg + NEW_LINE);
  }

  private static void displayMessage(String msg) {
    if (bufLog != null) {
      bufLog.print(msg);
      System.out.print(".");
    } else {
      System.out.print(msg);
    }
  }

  private static boolean checkDependencies(List<File> listeFileXml, XmlDocument fXml) {
    Element root = fXml.getDocument().getRootElement(); // Get the root element
    List<Element> listeDependencies = root.getChildren(DEPENDENCIES_TAG);
    if (listeDependencies != null && !listeDependencies.isEmpty()) {
      for (Element eltDependencies : listeDependencies) {
        List<Element> listeDependencyFiles = eltDependencies.getChildren(SETTINGSFILE_TAG);
        for (Element eltDependencyFile : listeDependencyFiles) {
          String name = eltDependencyFile.getAttributeValue(FILENAME_ATT);
          boolean found = false;
          for (int i = 0; i < listeFileXml.size() && !found; i++) {
            File f = xmlFiles.get(i);
            if (f.getName().equals(name)) {
              found = true;
            }
          }
          if (found == false) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public static GestionVariables loadGlobalVariables(Element root) throws Exception {
    GestionVariables gv = new GestionVariables();
    // liste des var globales
    displayMessageln(NEW_LINE + "var :");
    List<Element> listeGlobalVars = root.getChildren("global-vars");
    for (Element eltGlobalVar : listeGlobalVars) {
      List<Element> listeVars = eltGlobalVar.getChildren("var");
      for (Element eltVar : listeVars) {
        String name = eltVar.getAttributeValue("name");
        String value = gv.resolveAndEvalString(eltVar.getAttributeValue("value"));
        String relativePath = eltVar.getAttributeValue(RELATIVE_VALUE_ATTRIB);
        if (relativePath != null && !relativePath.equals("")) {
          relativePath = gv.resolveAndEvalString(relativePath);
          value = getRelativePath(relativePath, value);
        }
        gv.addVariable(name, value);
        displayMessageln("nom : " + name + "\t value : " + value);
      }
    }
    return gv;
  }

  public static void mergeConfigurationFiles(XmlDocument fileXml, File dirXml) throws IOException,
      AppBuilderException {
    // Tri par ordre alphabetique
    xmlFiles =
        new ArrayList<File>((Collection<File>) FileUtils.listFiles(dirXml, new String[] { "xml" },
        false));
    Collections.sort(xmlFiles);
    for (File xmlFile : xmlFiles) {
      displayMessageln(xmlFile.toString());
    }
    for (File f : xmlFiles) {
      displayMessageln("Is File = " + f.isFile() + " - Extension: " + FileUtil.getExtension(f)
          + " - Nom =" + f.getName());
      if (!(SILVERPEAS_SETTINGS.equalsIgnoreCase(f.getName()) || SILVERPEAS_CONFIG
          .equalsIgnoreCase(f.getName()))) {
        displayMessageln(f.getName());
        XmlDocument fXml = new XmlDocument(dirXml, f.getName());
        fXml.load();
        boolean dependenciesOK = checkDependencies(xmlFiles, fXml);
        // prise en compte uniquement si dependences OK
        if (dependenciesOK) {
          fileXml.mergeWith(TAGS_TO_MERGE, fXml);
        } else {
          displayMessageln("Ignore " + f.getName() + " file because dependencies are not resolved.");
        }
      }
    }
  }
}
