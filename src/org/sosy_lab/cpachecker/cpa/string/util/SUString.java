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
  private String suffix;

  public SUString() {
    suffix = new String();
  }

  public SUString(String str) {
    suffix = new String(str);
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
  public final static topString<SUString> TOP = new topString<>();

  protected String getSuffix() {
    return suffix;
  }

  protected Integer getLenght() {
    return suffix.length();
  }


  public void addToSuffix(Character c) {
    suffix = String.valueOf(c).concat(suffix);
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

  public void clean() {
    suffix = suffix.replaceAll(suffix, "");
  }

  @Override
  public StringDomain<SUString> join(StringDomain<SUString> pOther) {
    if (pOther.isBottom()) {
      return BOTTOM;
    }

    SUString newPRStr = new SUString();
    SUString expOther = (SUString) pOther;

    for (int i = Math.min(getLenght(), expOther.getLenght()) - 1; i >= 0; i++) {

      if (getFromSuffixByIndex(i) != expOther.getFromSuffixByIndex(i)) {
        break;
      }

      newPRStr.addToSuffix(getFromSuffixByIndex(i));
    }

    return newPRStr;
  }

  @Override
  public boolean isLessOrEqual(StringDomain<SUString> pOther) {

    if (pOther.isBottom()) {
      return false;
    }

    SUString expOther = (SUString) pOther;

    if (getLenght() > expOther.getLenght()
        && expOther.getSuffix() == getSuffix().substring(0, getLenght() - 1)) {
      return true;
    }

    return false;
  }

  @Override
  public boolean equals(Object pObj) {

    if (!(pObj instanceof SUString)) {
      return false;
    }

    return getSuffix() == ((SUString) pObj).getSuffix();
  }

  @Override
  public String toString() {
    return "(" + suffix.toString() + ")";
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

}
