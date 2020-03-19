/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.string;

import java.io.Serializable;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.string.util.CIString;
import org.sosy_lab.cpachecker.cpa.string.util.explicitCIString;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class StringState
    implements Serializable, LatticeAbstractState<StringState> {
  private static final long serialVersionUID = 1L;

  private final PersistentMap<String, CIString> stringDomains;

  public StringState() {
    stringDomains = PathCopyingPersistentTreeMap.of();
  }

  public StringState(PersistentMap<String, CIString> ciDomains) {
    this.stringDomains = ciDomains;
  }

  public CIString getCIString(String stringName) {
    return stringDomains.getOrDefault(stringName, explicitCIString.EMPTY);
  }

  public boolean contains(String stringName) {
    return stringDomains.containsKey(stringName);
  }

  public StringState addCIString(String stringName, CIString ciString) {

    if (ciString.isBottom()) {
      return removeCIString(stringName);
    }
    if (!stringDomains.containsKey(stringName)) {
      return new StringState(stringDomains.putAndCopy(stringName, ciString));
    }
    if (!stringDomains.get(stringName).equals(ciString)) {
      CIString str = (stringDomains.get(stringName)).join(ciString);
      return new StringState(stringDomains.putAndCopy(stringName, str));
    }
    return this;
  }

  public StringState removeAndAddCIString(String stringName, CIString ciString) {

    StringState newStState = removeCIString(stringName);

    if (!ciString.isBottom()) {
      return new StringState(newStState.stringDomains.putAndCopy(stringName, ciString));
    }

    return newStState;
  }

  public StringState removeCIString(String stringName) {
    if (stringDomains.containsKey(stringName)) {
      return new StringState(stringDomains.removeAndCopy(stringName));
    }
    return this;
  }

  // this function need for deleting domains of local variables
  public StringState deleteByPrefix(String prefix) {
    StringState newState = this;

    for (String stringName : stringDomains.keySet()) {
      if (stringName.startsWith(prefix)) {
        newState = newState.removeCIString(stringName);
      }
    }

    return newState;
  }
  // Join two sets (name, CIString). If exist name from this.keySet() and from pOther.keySet() join
  // their CIStrings
  @Override
  public StringState join(StringState pOther) throws CPAException, InterruptedException {

    boolean changed = false;
    PersistentMap<String, CIString> newCIDomains = PathCopyingPersistentTreeMap.of();

    for (String stringName : pOther.stringDomains.keySet()) {
      CIString otherCIString = pOther.getCIString(stringName);
      if (stringDomains.containsKey(stringName)) {
        newCIDomains =
            newCIDomains.putAndCopy(stringName, otherCIString.join(this.getCIString(stringName)));
        changed = true;
      } else {
        newCIDomains = newCIDomains.putAndCopy(stringName, otherCIString);
      }
    }

    for (String stringName : stringDomains.keySet()) {
      if (!pOther.stringDomains.containsKey(stringName)) {
        newCIDomains = newCIDomains.putAndCopy(stringName, this.getCIString(stringName));
        changed = true;
      }
    }

    if (changed) {
      return new StringState(newCIDomains);
    }
    return pOther;
  }

  // return true, if for any our name: (name, CISrting) exist (name, otherCIString) from pOther AND
  // CIString isLessOrEqual otherString

  @Override
  public boolean isLessOrEqual(StringState pOther) throws CPAException, InterruptedException {
    // TODO: is it correct?
    for (String stringName : stringDomains.keySet()) {
      if (!pOther.stringDomains.containsKey(stringName)) {
        return false;
      } else {
        if (!(this.getCIString(stringName)).isLessOrEqual(pOther.getCIString(stringName))) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    String str = new String();
    for (String stringName : stringDomains.keySet()) {
      str = str + "(" + stringName + " = " + getCIString(stringName).toString() + "), ";
    }
    return str;
  }
}