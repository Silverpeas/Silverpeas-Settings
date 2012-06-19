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
 * "http://www.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 */
package org.silverpeas.SilverpeasSettings;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.silverpeas.SilverpeasSettings.xml.XmlTransformer;
import org.silverpeas.SilverpeasSettings.xml.transform.XPathTransformer;
import org.silverpeas.applicationbuilder.AppBuilderException;
import org.silverpeas.applicationbuilder.XmlDocument;
import org.silverpeas.file.BackupFile;
import org.silverpeas.file.FileUtil;
import org.silverpeas.file.GestionVariables;
import org.silverpeas.file.ModifFile;
import org.silverpeas.file.ModifProperties;
import org.silverpeas.file.ModifText;
import org.silverpeas.file.ModifTextSilverpeas;
import org.silverpeas.file.ModifXMLSilverpeas;
import org.silverpeas.file.RegexpElementMotif;
import org.silverpeas.installedtree.DirectoryLocator;
import org.silverpeas.xml.XmlTreeHandler;
import org.silverpeas.xml.xpath.XPath;

public class SilverpeasSettings {

  static final String NEW_LINE = System.getProperty("line.separator");
  private static PrintWriter bufLog = null;
  private static XPath _xpathEngine = null;
  private static final String[] TAGS_TO_MERGE = { "global-vars", "fileset", "script" };
  private static List<File> xmlFiles;
  private static final String TOOL_VERSION = "SilverpeasSettings V5.0";
  public static final String DIR_SETTINGS = DirectoryLocator.getSilverpeasHome() +
      "/setup/settings";
  public static final String SILVERPEAS_SETTINGS = "SilverpeasSettings.xml";
  public static final String SILVERPEAS_CONFIG = "config.xml";
  public static final String DEPENDENCIES_TAG = "dependencies";
  public static final String SETTINGSFILE_TAG = "settingsfile";
  public static final String CONFIG_FILE_TAG = "configfile";
  public static final String TEXT_FILE_TAG = "textfile";
  public static final String COPY_FILE_TAG = "copyfile";
  public static final String COPY_DIR_TAG = "copydir";
  public static final String DELETE_TAG = "delete";
  public static final String XML_FILE_TAG = "xmlfile";
  public static final String PARAMETER_TAG = "parameter";
  public static final String VALUE_TAG = "value";
  private static final String SCRIPT_TAG = "script";
  public static final String FILE_NAME_ATTRIB = "name";
  public static final String XPATH_MODE_ATTRIB = "mode";
  public static final String PARAMETER_KEY_ATTRIB = "key";
  public static final String VALUE_LOCATION_ATTRIB = "location";
  public static final String RELATIVE_VALUE_ATTRIB = "relative-to";
  static final Map<String, Character> _modeMap = new HashMap<String, Character>(5);
  static final String[] scriptsRootPath =
      new String[] { DirectoryLocator.getSilverpeasHome() + "/bin/scripts/" };
  static GroovyScriptEngine scriptEngine = null;

  static {
    try {
      scriptEngine = new GroovyScriptEngine(scriptsRootPath);
    } catch (IOException ex) {
      Logger.getLogger(SilverpeasSettings.class.getName()).log(Level.SEVERE, null, ex);
    }

    _modeMap.put("select", Character.valueOf(XmlTreeHandler.MODE_SELECT));
    _modeMap.put("insert", Character.valueOf(XmlTreeHandler.MODE_INSERT));
    _modeMap.put("update", Character.valueOf(XmlTreeHandler.MODE_UPDATE));
    _modeMap.put("delete", Character.valueOf(XmlTreeHandler.MODE_DELETE));
    _modeMap.put("unique", Character.valueOf(XmlTreeHandler.MODE_UNIQUE));
  }

  public static char getXmlMode(String textualMode) {
    if (textualMode == null || textualMode.equals("")) {
      return XmlTreeHandler.MODE_UPDATE;
    }
    return _modeMap.get(textualMode.toLowerCase()).charValue();
  }

  public static String getXmlModeString(String textualMode) {
    if (textualMode == null || textualMode.equals("")) {
      return "default(update)";
    }
    return textualMode;
  }

  public static XPath getXPathEngine() {
    if (_xpathEngine == null) {
      _xpathEngine = new XPath();
    }
    return _xpathEngine;
  }

  public static String getRelativePath(final String base, final String path) {
    String result = path;
    String relBase = base;

    boolean baseUnixSep;
    int nbLevel;
    // removes drive
    if (relBase != null && relBase.length() >= 2 && relBase.charAt(1) == ':') {
      relBase = relBase.substring(2);
    }
    // detects file separator
    baseUnixSep = (relBase != null && relBase.indexOf('/') != -1);
    // removes starting file separator
    if (relBase != null && relBase.length() >= 1 &&
        relBase.charAt(0) == (baseUnixSep ? '/' : '\\')) {
      relBase = relBase.substring(1);
    }
    // removes ending file separator
    if (relBase != null && relBase.length() >= 1 &&
        relBase.endsWith(baseUnixSep ? "/" : "\\")) {
      relBase = relBase.substring(0, relBase.length() - 2);
    }
    // detects number of levels
    if (relBase == null || relBase.length() == 0) {
      nbLevel = 0;
    } else {
      StringTokenizer st = new StringTokenizer(relBase, baseUnixSep ? "/" : "\\");
      nbLevel = st.countTokens();
    }
    String resultBase = null;
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
    if (result != null && result.length() >= 1 &&
        result.charAt(0) != (baseUnixSep ? '/' : '\\')) {
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
      File fileLog = new File(DirectoryLocator.getLogHome() + "/SilverpeasSettings.log");
      bufLog = new PrintWriter(new BufferedWriter(new FileWriter(fileLog.getAbsolutePath(), true)));
      displayMessageln(NEW_LINE +
          "************************************************************************");
      displayMessageln("start settings of Silverpeas (" + new java.util.Date() +
          ").");
      if (args.length != 0) {
        throw new Exception("parameters forbidden");
      }
      File dirXml = new File(DIR_SETTINGS);
      XmlDocument fileXml = new XmlDocument(dirXml, SILVERPEAS_SETTINGS);
      fileXml.load();
      // merge tous les fichiers de configurations
      displayMessageln(NEW_LINE + "merged files with " + SILVERPEAS_SETTINGS + " :");
      mergeConfigurationFiles(fileXml, dirXml);
      Document doc = fileXml.getDocument();
      // Get the root element
      Element root = doc.getRootElement();
      GestionVariables gv = loadGlobalVariables(dirXml, root);

      // liste des chemins des fichiers
      displayMessageln(NEW_LINE + "modified files :");
      List<Element> scripts = root.getChildren("script");
      for (Element aScript : scripts) {
        executeScript(null, aScript, gv);
      }
      List<Element> listeFileSet = root.getChildren("fileset");
      for (Element eltFileSet : listeFileSet) {
        String dir = eltFileSet.getAttributeValue("root");
        List<Element> listeActions = eltFileSet.getChildren();
        for (Element action : listeActions) {
          try {
            if (CONFIG_FILE_TAG.equals(action.getName())) {
              configfile(dir, action, gv);
            } else if (TEXT_FILE_TAG.equals(action.getName())) {
              textfile(dir, action, gv);
            } else if (COPY_FILE_TAG.equals(action.getName())) {
              copyfile(dir, action, gv);
            } else if (XML_FILE_TAG.equals(action.getName())) {
              xmlfile(dir, action, gv);
            } else if (DELETE_TAG.equals(action.getName())) {
              deletefile(dir, action, gv);
            } else if (SCRIPT_TAG.equals(action.getName())) {
              executeScript(dir, action, gv);
            } else {
              displayMessageln("Unknown setting action : " + action.getName());
            }
          } catch (Exception e) {
            printError(e);
          }
        } // while actions
      } // while fileset
      displayMessageln(NEW_LINE + "Silverpeas has been successfuly configured (" + new Date() +
          ").");
      bufLog.close();
      System.out.println(
          NEW_LINE + "Silverpeas has been successfuly configured (" + new Date() + ").");
    } catch (Exception e) {
      printError(e);
      e.printStackTrace(System.err);
    }
  }

  static GestionVariables loadConfiguration(File dir) throws IOException, AppBuilderException {
    Properties defaultConfig = new Properties();
    defaultConfig.load(SilverpeasSettings.class.getClassLoader().getResourceAsStream(
        "default_config.properties"));
    GestionVariables configuration;
    Properties config = new Properties();
    File configFile = new File(dir, "config.properties");
    if (configFile.exists() && configFile.isFile()) {
      config.load(new FileInputStream(configFile));
      configuration = new GestionVariables(config, defaultConfig);
    } else {
      configuration = new GestionVariables(defaultConfig);
      configFile = new File(dir, SILVERPEAS_CONFIG);
      if (configFile.exists() && configFile.isFile()) {
        XmlDocument fileXml = new XmlDocument(dir, SILVERPEAS_CONFIG);
        fileXml.load();
        Document doc = fileXml.getDocument();
        Element root = doc.getRootElement();
        loadVariablesFromXml(configuration, root);
      }
    }
    return configuration;
  }

  // ---------------------------------------------------------------------
  /**
   * @param errMsg
   * @see
   */
  private static void configfile(String dir, Element eltConfigFile, GestionVariables gv) throws
      Exception {
    String dirFile = dir + eltConfigFile.getAttributeValue(FILE_NAME_ATTRIB);
    dirFile = gv.resolveAndEvalString(dirFile);
    String typeFile = FileUtil.getExtension(dirFile);
    displayMessageln(dirFile);
    // fichiers xml
    if ("xml".equalsIgnoreCase(typeFile)) {
      ModifXMLSilverpeas fic = new ModifXMLSilverpeas(dirFile);
      applyModifications(eltConfigFile, gv, fic);
    } // fichiers properties
    else if ("properties".equalsIgnoreCase(typeFile)) {
      ModifProperties fic = new ModifProperties(dirFile);
      applyModifications(eltConfigFile, gv, fic);
    } else {
      // fichiers fonctionnants avec le mode key '=' value
      ModifTextSilverpeas fic = new ModifTextSilverpeas(dirFile);
      applyModifications(eltConfigFile, gv, fic);
    }
  }

  private static void applyModifications(Element eltConfigFile, GestionVariables gv, ModifFile fic)
      throws IOException, Exception {
    @SuppressWarnings("unchecked")
    List<Element> listeParameter = eltConfigFile.getChildren(PARAMETER_TAG);
    for (Element eltParameter : listeParameter) {
      String key = eltParameter.getAttributeValue(PARAMETER_KEY_ATTRIB);
      String value = eltParameter.getTextTrim();
      value = gv.resolveAndEvalString(value);
      fic.addModification(key, value);
      displayMessageln("\tkey = " + key + "\t value = " + value);
    }
    fic.executeModification();
  }

  // ---------------------------------------------------------------------
  private static void textfile(String dir, Element eltTextFile,
      GestionVariables gv) throws Exception {
    String dirFile = dir + eltTextFile.getAttributeValue(FILE_NAME_ATTRIB);
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
    @SuppressWarnings("unchecked")
    List<Element> listeParameter = eltTextFile.getChildren(PARAMETER_TAG);
    for (Element eltParameter : listeParameter) {
      String key = eltParameter.getAttributeValue(PARAMETER_KEY_ATTRIB);
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

  protected static void xmlfile(String dir, Element eltConfigFile,
      GestionVariables gv) throws Exception {
    getXmlTransformer().xmlfile(dir, eltConfigFile, gv);
  }

  protected static XmlTransformer getXmlTransformer() {
    return new XPathTransformer();
  }

  // ---------------------------------------------------------------------
  protected static void copyfile(String dir, Element eltTextFile, GestionVariables gv)
      throws Exception {
    String dirFile = dir + eltTextFile.getAttributeValue(FILE_NAME_ATTRIB);
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

  public static void printError(Exception ex) {
    StringWriter buffer = new StringWriter(2000);
    try {
      if (bufLog != null) {
        ex.printStackTrace(new PrintWriter(buffer));
        displayMessageln(NEW_LINE + buffer.toString());
        bufLog.close();
      }
      System.out.println(NEW_LINE + buffer.toString() + NEW_LINE);
    } finally {
      IOUtils.closeQuietly(buffer);
    }
  }

  public static void printError(String errMsg) {
    if (bufLog != null) {
      displayMessageln(NEW_LINE + errMsg);
      bufLog.close();
    }
    System.out.println(NEW_LINE + errMsg + NEW_LINE);
  }

  public static void displayMessageln(String msg) {
    displayMessage(msg + NEW_LINE);
  }

  public static void displayMessage(String msg) {
    if (bufLog != null) {
      bufLog.print(msg);
      System.out.print(".");
    } else {
      System.out.print(msg);
    }
  }

  protected static boolean checkDependencies(List<File> listeFileXml, XmlDocument fXml) {
    Element root = fXml.getDocument().getRootElement(); // Get the root element
    @SuppressWarnings("unchecked")
    List<Element> listeDependencies = root.getChildren(DEPENDENCIES_TAG);
    if (listeDependencies != null && !listeDependencies.isEmpty()) {
      for (Element eltDependencies : listeDependencies) {
        @SuppressWarnings("unchecked")
        List<Element> listeDependencyFiles = eltDependencies.getChildren(SETTINGSFILE_TAG);
        for (Element eltDependencyFile : listeDependencyFiles) {
          String name = eltDependencyFile.getAttributeValue(FILE_NAME_ATTRIB);
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

  /**
   * Load variables defined in an XML file.
   * @param configDir
   * @param root
   * @return
   * @throws IOException
   * @throws AppBuilderException
   */
  public static GestionVariables loadGlobalVariables(File configDir, Element root)
      throws IOException, AppBuilderException {
    GestionVariables gv = loadConfiguration(configDir);
    displayMessageln(NEW_LINE + "var :");
    loadVariablesFromXml(gv, root);
    return gv;
  }

  public static void loadVariablesFromXml(GestionVariables gv, Element root) throws IOException {
    List<Element> listeGlobalVars = root.getChildren("global-vars");
    for (Element eltGlobalVar : listeGlobalVars) {
      List<Element> listeVars = eltGlobalVar.getChildren("var");
      for (Element eltVar : listeVars) {
        String name = eltVar.getAttributeValue(FILE_NAME_ATTRIB);
        String value = gv.resolveAndEvalString(eltVar.getAttributeValue(VALUE_TAG));
        String relativePath = eltVar.getAttributeValue(RELATIVE_VALUE_ATTRIB);
        if (relativePath != null && !relativePath.equals("")) {
          relativePath = gv.resolveAndEvalString(relativePath);
          value = getRelativePath(relativePath, value);
        }
        gv.addVariable(name, value);
        displayMessageln("nom : " + name + "\t value : " + value);
      }
    }
  }

  @SuppressWarnings("unchecked")
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
      displayMessageln("Is File = " + f.isFile() + " - Extension: " + FileUtil.getExtension(f) +
          " - Nom =" + f.getName());
      if (!(SILVERPEAS_SETTINGS.equalsIgnoreCase(f.getName()) || SILVERPEAS_CONFIG
          .equalsIgnoreCase(f.
          getName()))) {
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

  protected static void deletefile(String dir, Element eltTextFile, GestionVariables gv)
      throws Exception {
    String dirFile = dir + eltTextFile.getAttributeValue(FILE_NAME_ATTRIB);
    dirFile = gv.resolveAndEvalString(dirFile);
    File sourceFile = new File(dirFile);
    if (FileUtils.deleteQuietly(sourceFile)) {
      displayMessageln(dirFile + System.getProperty("line.separator") + "\tdeleted");
    } else {
      displayMessageln(dirFile + System.getProperty("line.separator") + "\tdeletion failed!");
    }
  }

  protected static void executeScript(String dir, Element scriptElt, GestionVariables gv) throws
      Exception {
    String script = gv.resolveAndEvalString(scriptElt.getAttributeValue(FILE_NAME_ATTRIB));
    if (scriptEngine != null) {
      Binding withVariables = bindToVariables(gv);
      if (dir != null && !dir.trim().isEmpty()) {
        withVariables.setVariable("filesetRoot", gv.resolveAndEvalString(dir));
      }
      scriptEngine.run(script, withVariables);
    } else {
      displayMessageln("The Groovy Script Engine is not set: cannot run script '" + script);
    }
  }

  private static Binding bindToVariables(GestionVariables gv) throws Exception {
    Binding binding = new Binding();
    Enumeration<String> variables = gv.getVariableNames();
    while (variables.hasMoreElements()) {
      String variable = variables.nextElement();
      binding.setVariable(variable, gv.getValue(variable));
    }
    binding.setVariable("gestionVariables", gv);
    return binding;
  }
}
