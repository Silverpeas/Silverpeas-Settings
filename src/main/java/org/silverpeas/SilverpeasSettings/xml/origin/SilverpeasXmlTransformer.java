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
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "http://www.silverpeas.com/legal/licensing"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.SilverpeasSettings.xml.origin;

import java.io.File;
import java.util.List;

import org.jdom.Element;

import org.silverpeas.SilverpeasSettings.xml.XmlTransformer;
import org.silverpeas.applicationbuilder.XmlDocument;
import org.silverpeas.file.BackupFile;
import org.silverpeas.file.GestionVariables;
import org.silverpeas.xml.XmlTreeHandler;

import static org.silverpeas.SilverpeasSettings.SilverpeasSettings.*;

/**
 * This is the original transformer used by SilverpeasSettings before the 5.3 release of Silverpeas.
 * It is left here for backward compability.
 * @author ehugonnet
 */
public class SilverpeasXmlTransformer implements XmlTransformer {

  @SuppressWarnings("unchecked")
  @Override
  public void xmlfile(String dir, org.jdom.Element eltConfigFile, GestionVariables gv)
      throws Exception {
    String dirFile = dir + eltConfigFile.getAttributeValue(FILE_NAME_ATTRIB);
    // ici nouveaute t005 : evaluation dynamique
    dirFile = gv.resolveAndEvalString(dirFile);
    displayMessageln(dirFile);
    File dirFileFile = new File(dirFile);
    boolean backuped = false;

    XmlDocument xmlDoc = new XmlDocument(dirFileFile.getParentFile(), dirFileFile.getName());
    xmlDoc.load();
    getXPathEngine().setStartingElement(xmlDoc.getDocument().getRootElement());
    // liste des parametres a modifier
    List<Element> parameters = eltConfigFile.getChildren(PARAMETER_TAG);
    for (Element eltParameter : parameters) {
      String key = gv.resolveAndEvalString(eltParameter.getAttributeValue(PARAMETER_KEY_ATTRIB));
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
        List<Element> values = eltParameter.getChildren(VALUE_TAG);
        for (Element eltValue : values) {
          String location = eltValue.getAttributeValue(VALUE_LOCATION_ATTRIB);
          location = gv.resolveAndEvalString(location);
          String childMode = eltValue.getAttributeValue(XPATH_MODE_ATTRIB);
          String value = eltValue.getTextTrim();
          value = gv.resolveAndEvalString(value);
          String relativePath = eltValue.getAttributeValue(RELATIVE_VALUE_ATTRIB);
          if (relativePath != null && !relativePath.isEmpty()) {
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

}
