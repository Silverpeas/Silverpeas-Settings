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
package org.silverpeas.SilverpeasSettings.xml.transform;

import org.silverpeas.file.StringUtil;
import java.util.StringTokenizer;

/**
 * @author ehugonnet
 */
public class Value {

  private String location;
  private char mode;
  private String value;

  /**
   * Get the value of value
   * @return the value of value
   */
  public String getValue() {
    return value;
  }

  public Value(String location, String relativePath, String value, char mode) {
    if (location != null) {
      this.location = location;
    } else {
      this.location = ".";
    }
    this.mode = mode;
    this.value = value;
    if (relativePath != null && !relativePath.equals("")) {
      this.value = getRelativePath(relativePath, value);
    }
  }

  /**
   * Get the value of mode
   * @return the value of mode
   */
  public char getMode() {
    return mode;
  }

  /**
   * Get the value of location
   * @return the value of location
   */
  public String getLocation() {
    return location;
  }

  private String getRelativePath(final String base, final String path) {
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
}
