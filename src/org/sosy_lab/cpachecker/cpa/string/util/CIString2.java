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

import java.util.Set;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class CIString2 implements StringDomain<CIString2> {

  private static final long serialVersionUID = 1L;
  private PersistentSet<Character> certainly;
  private PersistentSet<Character> maybe;

  public CIString2() {
    certainly = PersistentSet.of();
    maybe = PersistentSet.of();
  }

  public CIString2(String str) {

    certainly = PersistentSet.of();
    maybe = PersistentSet.of();

    char[] charArray = str.toCharArray();

    for (int i = 0; i < charArray.length; i++) {
      certainly = certainly.addAndCopy(new Character(charArray[i]));
      maybe = maybe.addAndCopy(new Character(charArray[i]));
    }
  }

  private CIString2(PersistentSet<Character> pCertainly, PersistentSet<Character> pMaybe) {
    certainly = pCertainly;
    maybe = pMaybe;
  }

  public CIString2 copyOf() {
    return new CIString2(certainly, maybe);
  }

  public final static CIString2 EMPTY = new CIString2();
  public final static bottomString<CIString2> BOTTOM = new bottomString<>();
  public final static topString<CIString2> TOP = new topString<>();

  public boolean isEmpty() {
    return equals(CIString2.EMPTY);
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public boolean equals(Object pObj) {

    if (!(pObj instanceof CIString2)) {
      return false;
    }

    CIString2 other = (CIString2) pObj;

    return certainly.equals(other.getCertainly()) && maybe.equals(other.getMaybe());
  }

  public void setCertainly(Set<Character> set) {
    certainly = certainly.removeAllAndCopy();
    certainly = certainly.addAllAndCopy(set);
  }

  public void clearCertainly() {
    certainly = certainly.removeAllAndCopy();
  }

  public void setMaybe(Set<Character> set) {
    maybe = maybe.removeAllAndCopy();
    maybe = maybe.addAllAndCopy(set);
  }

  public void addToSertainly(Set<Character> set) {
    certainly = certainly.addAllAndCopy(set);
  }

  public void addToMaybe(Set<Character> set) {
    maybe = maybe.addAllAndCopy(set);
  }

  public PersistentSet<Character> getCertainly() {
    return certainly;
  }

  public PersistentSet<Character> getMaybe() {
    return maybe;
  }

  @Override
  public StringDomain<CIString2> join(StringDomain<CIString2> pOther) {
    // if (pOther == null) {
    // return null;
    // }

    if(pOther.isBottom()) {
      return BOTTOM;
    }

    CIString2 str = new CIString2();
    CIString2 expOther = (CIString2) pOther;

    str.setCertainly(
        SetUtil.generalizedIntersect(this.getCertainly().asSet(), expOther.getCertainly().asSet()));
    str.setMaybe(SetUtil.generalizedUnion(this.getMaybe().asSet(), expOther.getMaybe().asSet()));

    return str;
  }

  @Override
  public boolean isLessOrEqual(StringDomain<CIString2> pOther) {

    if (pOther.isBottom()) {
      return false;
    }

    if(isEmpty()) {
      return true;
    }

    CIString2 expOther = (CIString2) pOther;

    return this.getCertainly().containsAll(expOther.getCertainly().asSet())
        && expOther.getMaybe().containsAll(this.getMaybe().asSet());

  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public String toString() {
    return "(" + certainly.toString() + ", " + maybe.toString() + ")";
  }

}
