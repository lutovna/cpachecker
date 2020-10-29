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

  // Integer level = 5;

  private static final long serialVersionUID = 1L;

  private StringDomain<CIString> ciDomain;
  private StringDomain<PRString> prDomain;
  private StringDomain<SUString> suDomain;

  StringState() {
    ciDomain = new CIString();
    prDomain = new PRString();
    suDomain = new SUString();
  }

  public StringState(String str) {
    ciDomain = new CIString(str);
    prDomain = new PRString(str);
    suDomain = new SUString(str);
  }

  private StringState(
      StringDomain<CIString> newCIString,
      StringDomain<PRString> newPRString,
      StringDomain<SUString> newSUString) {
    ciDomain = newCIString;
    prDomain = newPRString;
    suDomain = newSUString;
  }

  public final static StringState EMPTY = new StringState();
  public final static StringState BOTTOM =
      new StringState(CIString.BOTTOM, PRString.BOTTOM, SUString.BOTTOM);
  public final static StringState TOP = new StringState(CIString.TOP, PRString.TOP, SUString.TOP);

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
    return ciDomain;
  }

  public StringDomain<PRString> getPRDomain() {
    return prDomain;
  }

  public StringDomain<SUString> getSUDomain() {
    return suDomain;
  }

  public void setCIDomain(StringDomain<CIString> newCIString) {
    ciDomain = newCIString;
  }

  public void setPRDomain(StringDomain<PRString> newPRString) {
    prDomain = newPRString;
  }

  public void setSUDomain(StringDomain<SUString> newSUString) {
    suDomain = newSUString;
  }

  @Override
  public boolean equals(Object pObj) {

    if (!(pObj instanceof StringState)) {
      return false;
    }

    StringState other = (StringState) pObj;

    return ciDomain.equals(other.ciDomain)
        && prDomain.equals(other.prDomain)
        && suDomain.equals(other.suDomain);
  }

  @Override
  public StringState join(StringState pOther) {

    return new StringState(
        (ciDomain.join(pOther.ciDomain)),
        (prDomain.join(pOther.prDomain)),
        (suDomain.join(pOther.suDomain)));

    /*
     * StringState state = new StringState();
     *
     * state.setCIString((CIString) (ciString.join(pOther.ciString))); state.setPRString((PRString)
     * (prString.join(pOther.prString))); state.setSUString((SUString)
     * (suString.join(pOther.suString)));
     *
     * return state
     */
  }

  @Override
  public boolean isLessOrEqual(StringState pOther) {
    return ciDomain.isLessOrEqual(pOther.ciDomain)
        && prDomain.isLessOrEqual(pOther.prDomain)
        && suDomain.isLessOrEqual(pOther.suDomain);
  }

  public StringState copyOf() {
    return new StringState(ciDomain, prDomain, suDomain);
  }

  @Override
  public String toString() {
    return "("
        + ciDomain.toString()
        + ", "
        + prDomain.toString()
        + ", "
        + suDomain.toString()
        + ")";
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
