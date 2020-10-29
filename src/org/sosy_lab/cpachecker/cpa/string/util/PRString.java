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

public class PRString implements StringDomain<PRString> {

  private static final long serialVersionUID = 1L;
  private String prefix;
  // private Integer number = 2;

  public PRString() {
    prefix = new String();
  }

  public PRString(String str) {
    prefix = new String(str);
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  public final static PRString EMPTY = new PRString();
  public final static bottomString<PRString> BOTTOM = new bottomString<>();
  public final static topString<PRString> TOP = new topString<>();

  public String getPrefix() {
    return prefix;
  }

  public Integer getLenght() {
    return prefix.length();
  }

  public void addToPrefix(Character c) {
    prefix = prefix.concat(String.valueOf(c));
  }

  public Character getFromPrefixByIndex(int i) {
    if (i > getLenght()) {
      return null;
    }
    return prefix.charAt(i);
  }

  public boolean isEmpty() {
    return prefix.isEmpty();
  }

  public void clean() {
    prefix = prefix.replaceAll(prefix, "");
  }

  @Override
  public StringDomain<PRString> join(StringDomain<PRString> pOther) {
    if (pOther.isBottom()) {
      return BOTTOM;
    }

    PRString newPRStr = new PRString();
    PRString expOther = (PRString) pOther;

    for (int i = 0;
        i < Math.min(getLenght(), expOther.getLenght())
            && getFromPrefixByIndex(i) == expOther.getFromPrefixByIndex(i);
        i++) {
      newPRStr.addToPrefix(getFromPrefixByIndex(i));
    }

    return newPRStr;
  }

  @Override
  public boolean isLessOrEqual(StringDomain<PRString> pOther) {

    if (pOther.isBottom()) {
      return false;
    }

    PRString expOther = (PRString) pOther;

    if (getLenght() > expOther.getLenght()
        && expOther.getPrefix() == getPrefix().substring(0, getLenght() - 1)) {
      return true;
    }

    return false;
  }

  @Override
  public boolean equals(Object pObj) {

    if (!(pObj instanceof PRString)) {
      return false;
    }
    return getPrefix() == ((PRString) pObj).getPrefix();
  }

  @Override
  public String toString() {
    return prefix.toString();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  /*
   * @Override public Integer getNumber() { return number; }
   */

}
