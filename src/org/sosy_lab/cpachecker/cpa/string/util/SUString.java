/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.string.util;

public class SUString implements StringDomain<SUString> {

  private static final long serialVersionUID = 1L;
  private final String suffix;
  // private Integer number = 3;

  public SUString() {
    suffix = "";
  }

  public SUString(String str) {
    suffix = str;
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  public final static SUString EMPTY = new SUString();
  public final static bottomString<SUString> BOTTOM = new bottomString<>();
  public final static SUString TOP = new SUString();

  public String getSuffix() {
    return suffix;
  }

  public int getLenght() {
    return suffix.length();
  }

  public Character getFromSuffixByIndex(int i) {
    if (i > getLenght()) {
      return null;
    }
    return suffix.charAt(i);
  }

  public boolean isEmpty() {
    return suffix.isEmpty();
  }

  @Override
  public StringDomain<SUString> join(StringDomain<SUString> pOther) {
    if (pOther.isBottom()) {
      return BOTTOM;
    }

    SUString expOther = (SUString) pOther;
    int lenOfCommonSuf = 0;

    for (;
        lenOfCommonSuf < Math.min(getLenght(), expOther.getLenght())
            && getFromSuffixByIndex(getLenght() - 1 - lenOfCommonSuf)
                .equals(expOther.getFromSuffixByIndex(expOther.getLenght() - 1 - lenOfCommonSuf));
        lenOfCommonSuf++) {
    }

    return new SUString(suffix.substring(getLenght() - lenOfCommonSuf, getLenght()));
  }

  @Override
  public boolean isLessOrEqual(StringDomain<SUString> pOther) {

    if (pOther.isBottom()) {
      return false;
    }

    // SUString expOther = (SUString) pOther;

    return suffix.endsWith(((SUString) pOther).getSuffix());
    /*
     * if (getLenght() >= expOther.getLenght() && expOther.getSuffix()
     * .equals(getSuffix().substring(getLenght() - expOther.getLenght(), getLenght()))) { return
     * true; }
     *
     * return false;
     */
  }

  @Override
  public boolean equals(Object pObj) {

    if (!(pObj instanceof SUString)) {
      return false;
    }

    if ((((SUString) pObj)).isTop()) {
      return getSuffix().equals("");
    }

    return getSuffix().equals(((SUString) pObj).getSuffix());
  }

  @Override
  public String toString() {
    return suffix.toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /*
   * @Override public Integer getNumber() { return number; }
   */

}
