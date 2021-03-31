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
package org.sosy_lab.cpachecker.cpa.string.util;

import java.io.Serializable;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;

public class StringState implements Serializable, LatticeAbstractState<StringState> {

  private static final long serialVersionUID = 1L;

  private StringDomain<CIString> ciDomain;
  private StringDomain<PRString> prDomain;
  private StringDomain<SUString> suDomain;

  // now activity is equal for all string states. May be that will change after adding percision
  private static Integer numberOfDomains = 3;

  private static Boolean[] activity = new Boolean[numberOfDomains];

  /*
   * // initializing actvity[] with all "true" { for (int i = 0; i < numberOfDomains; i++) {
   * activity[i] = true; } }
   */

  /*
   * public StringState() { ciDomain = new CIString(); prDomain = new PRString(); suDomain = new
   * SUString(); }
   */

  /*
   * public StringState(String str) { ciDomain = new CIString(str); prDomain = new PRString(str);
   * suDomain = new SUString(str); }
   */

  private StringState(
      StringDomain<CIString> newCIString,
      StringDomain<PRString> newPRString,
      StringDomain<SUString> newSUString) {
    ciDomain = newCIString;
    prDomain = newPRString;
    suDomain = newSUString;
  }

  //
  public final static StringState EMPTY =
      new StringState(new CIString(), new PRString(), new SUString());
  public final static StringState BOTTOM =
      new StringState(CIString.BOTTOM, PRString.BOTTOM, SUString.BOTTOM);
  public final static StringState TOP = new StringState(CIString.TOP, PRString.TOP, SUString.TOP);

  // if one of the domains is bottom, then other also should be
  private void checkBottom() {
    if (ciDomain.isBottom() || prDomain.isBottom() || suDomain.isBottom()) {
      ciDomain = CIString.BOTTOM;
      prDomain = PRString.BOTTOM;
      suDomain = SUString.BOTTOM;
    }
  }

  public StringState(Boolean[] newActivity) {
    ciDomain = new CIString();
    prDomain = new PRString();
    suDomain = new SUString();

    for (int i = 0; i < numberOfDomains; i++) {
      if (i < newActivity.length) {
        activity[i] = newActivity[i];
      } else {
        activity[i] = false;
      }
    }

    checkBottom();
  }

  public StringState(Boolean[] newActivity, String str) {
    ciDomain = new CIString(str);
    prDomain = new PRString(str);
    suDomain = new SUString(str);

    for (int i = 0; i < numberOfDomains; i++) {
      if (i < newActivity.length) {
        activity[i] = newActivity[i];
      } else {
        activity[i] = false;
      }
    }

    checkBottom();
  }

  private StringState(
      Boolean[] newActivity,
      StringDomain<CIString> newCIString,
      StringDomain<PRString> newPRString,
      StringDomain<SUString> newSUString) {
    ciDomain = newCIString;
    prDomain = newPRString;
    suDomain = newSUString;

    for (int i = 0; i < numberOfDomains; i++) {
      if (i < newActivity.length) {
        activity[i] = newActivity[i];
      } else {
        activity[i] = false;
      }
    }

    checkBottom();
  }

  public boolean isEmpty() {
    return equals(StringState.EMPTY);
  }

  public boolean isBottom() {
    return equals(StringState.BOTTOM);
  }

  public boolean isTop() {
    return equals(StringState.TOP);
  }

  public StringDomain<CIString> getCIDomain() {
    if (!activity[0]) {
      return CIString.TOP;
    }

    return ciDomain;
  }

  public StringDomain<PRString> getPRDomain() {
    if (!activity[1]) {
      return PRString.TOP;
    }

    return prDomain;
  }

  public StringDomain<SUString> getSUDomain() {
    if (!activity[2]) {
      return SUString.TOP;
    }

    return suDomain;
  }

  public void setCIDomain(StringDomain<CIString> newCIString) {
    if (!activity[0]) {
      ciDomain = CIString.TOP;
    }
    ciDomain = newCIString;

    checkBottom();
  }

  public void setPRDomain(StringDomain<PRString> newPRString) {
    if (!activity[1]) {
      prDomain = PRString.TOP;
    }
    prDomain = newPRString;

    checkBottom();
  }

  public void setSUDomain(StringDomain<SUString> newSUString) {
    if (!activity[2]) {
      suDomain = SUString.TOP;
    }
    suDomain = newSUString;

    checkBottom();
  }

  @Override
  public boolean equals(Object pObj) {

    if (!(pObj instanceof StringState)) {
      return false;
    }

    StringState other = (StringState) pObj;

    return (!activity[0] || ciDomain.equals(other.ciDomain))
        && (!activity[1] || prDomain.equals(other.prDomain))
        && (!activity[2] || suDomain.equals(other.suDomain));
  }

  @Override
  public StringState join(StringState pOther) {

    return new StringState(
        activity,
        ciDomain.join(pOther.ciDomain),
        prDomain.join(pOther.prDomain),
        suDomain.join(pOther.suDomain));
  }

  @Override
  public boolean isLessOrEqual(StringState pOther) {
    return (!activity[0] || ciDomain.isLessOrEqual(pOther.ciDomain))
        && (!activity[1] || prDomain.isLessOrEqual(pOther.prDomain))
        && (!activity[2] || suDomain.isLessOrEqual(pOther.suDomain));
  }

  public StringState copyOf() {
    return new StringState(activity, ciDomain, prDomain, suDomain);
  }

  @Override
  public String toString() {
    String ret = "(";

    if (activity[0]) {
      ret += ciDomain.toString() + ", ";
    }
    if (activity[1]) {
      ret += prDomain.toString() + ", ";
    }
    if (activity[2]) {
      ret += suDomain.toString();
    }

    return ret + ")";
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
