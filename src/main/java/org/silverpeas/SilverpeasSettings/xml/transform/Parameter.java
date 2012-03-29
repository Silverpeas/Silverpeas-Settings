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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ehugonnet
 */
public class Parameter {

  private String key;
  private char mode;
  private List<Value> values;

  public Parameter(String key, char mode) {
    this.key = key;
    this.mode = mode;
    this.values = new ArrayList<Value>();
  }

  /**
   * Get the value of values
   * @return the value of values
   */
  public List<Value> getValues() {
    return Collections.unmodifiableList(values);
  }

  /**
   * Add a list of Value
   * @param values new values to be added.
   */
  public void addValues(List<Value> values) {
    this.values.addAll(values);
  }

  /**
   * Add a new Value
   * @param value new value to be added.
   */
  public void addValue(Value value) {
    this.values.add(value);
  }

  /**
   * Get the value of mode
   * @return the value of mode
   */
  public char getMode() {
    return mode;
  }

  /**
   * Get the value of key
   * @return the value of key
   */
  public String getKey() {
    return key;
  }

}
