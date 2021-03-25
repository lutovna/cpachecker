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
import org.sosy_lab.cpachecker.cpa.string.util.StringState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class Strings
    implements Serializable, LatticeAbstractState<Strings> {
  private static final long serialVersionUID = 1L;

  private final PersistentMap<String, StringState> stringStates;
  private static Integer numberOfDomains = 3;

  private static Boolean[] activity = new Boolean[numberOfDomains];

  public Strings() {
    stringStates = PathCopyingPersistentTreeMap.of();
  }

  public Strings(Boolean[] newActivity) {
    stringStates = PathCopyingPersistentTreeMap.of();

    for (int i = 0; i < numberOfDomains; i++) {
      if (i < newActivity.length) {
        activity[i] = newActivity[i];
      } else {
        activity[i] = false;
      }
    }
  }

  public Strings(PersistentMap<String, StringState> stringStates) {
    this.stringStates = stringStates;
  }

  public StringState getStringState(String stringName) {
    return stringStates.getOrDefault(stringName, StringState.EMPTY);
  }

  public boolean contains(String stringName) {
    return stringStates.containsKey(stringName);
  }

  public Strings addStringState(String stringName, StringState stringState) {

    if (stringState.isBottom()) {
      return removeStringState(stringName);
    }
    if (!stringStates.containsKey(stringName)) {
      return new Strings(stringStates.putAndCopy(stringName, stringState));
    }
    if (!stringStates.get(stringName).equals(stringState)) {
      StringState str = (stringStates.get(stringName)).join(stringState);
      return new Strings(stringStates.putAndCopy(stringName, str));
    }
    return this;
  }

  public Strings removeAndAddStringState(String stringName, StringState stringState) {

    Strings newStringState = removeStringState(stringName);

    if (!stringState.isBottom()) {
      return new Strings(newStringState.stringStates.putAndCopy(stringName, stringState));
    }

    return newStringState;
  }

  public Strings removeStringState(String stringName) {
    if (stringStates.containsKey(stringName)) {
      return new Strings(stringStates.removeAndCopy(stringName));
    }
    return this;
  }

  // this function need for deleting domains of local variables
  public Strings deleteByPrefix(String prefix) {
    Strings newState = this;

    for (String stringName : stringStates.keySet()) {
      if (stringName.startsWith(prefix)) {
        newState = newState.removeStringState(stringName);
      }
    }

    return newState;
  }

  public Boolean[] getActivity() {
    return activity;
  }

  public Boolean getActivityByNumber(Integer i) {
    return activity[i];
  }

  // Join two sets (name, StringState). If exist name from this.keySet() and from pOther.keySet()
  // join
  // their StringState
  @Override
  public Strings join(Strings pOther) throws CPAException, InterruptedException {

    boolean changed = false;
    PersistentMap<String, StringState> newStrings = PathCopyingPersistentTreeMap.of();

    for (String stringName : pOther.stringStates.keySet()) {
      StringState otherString = pOther.getStringState(stringName);
      if (stringStates.containsKey(stringName)) {
        newStrings =
            newStrings.putAndCopy(stringName, otherString.join(this.getStringState(stringName)));
        changed = true;
      } else {
        newStrings = newStrings.putAndCopy(stringName, otherString);
      }
    }

    for (String stringName : stringStates.keySet()) {
      if (!pOther.stringStates.containsKey(stringName)) {
        newStrings = newStrings.putAndCopy(stringName, this.getStringState(stringName));
        changed = true;
      }
    }

    if (changed) {
      return new Strings(newStrings);
    }
    return pOther;
  }

  // return true, if for any our name: (name, StringState) exist (name, otherStringState) from
  // pOther AND
  // StringState isLessOrEqual otherStringState

  @Override
  public boolean isLessOrEqual(Strings pOther) throws CPAException, InterruptedException {
    // TODO: is it correct?
    for (String stringName : stringStates.keySet()) {
      if (!pOther.stringStates.containsKey(stringName)) {
        return false;
      } else {
        if (!(this.getStringState(stringName)).isLessOrEqual(pOther.getStringState(stringName))) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    String str = new String();
    for (String stringName : stringStates.keySet()) {
      str = str + "(" + stringName + " = " + getStringState(stringName).toString() + "), ";
    }
    return str;
  }
}