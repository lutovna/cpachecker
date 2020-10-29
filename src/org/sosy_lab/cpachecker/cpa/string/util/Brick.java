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


public class Brick implements StringDomain<Brick> {

  private static final long serialVersionUID = 1L;
  private PersistentSet<String> stringSet;
  private int min;
  private int max;

  public Brick() {
    stringSet = PersistentSet.of();
    min = 0;
    max = 0;
  }

  public Brick(String str) {
    stringSet = PersistentSet.of();
    stringSet.addAndCopy(str);
    min = 1;
    max = 1;
  }

  public void setStringSet(Set<String> newStringSet) {
    stringSet = stringSet.removeAllAndCopy();
    stringSet = stringSet.addAllAndCopy(newStringSet);
  }

  public void setMin(int newMin) {
    min = newMin;
  }

  public void setMax(int newMax) {
    max = newMax;
  }

  public PersistentSet<String> getStringSet() {
    return stringSet;
  }

  public int getMin() {
    return min;
  }

  public int getMax() {
    return max;
  }

  public final static Brick EMPTY = new Brick();
  public final static bottomString<Brick> BOTTOM = new bottomString<>();
  public final static topString<Brick> TOP = new topString<>();

  public boolean isEmpty() {
    return equals(Brick.EMPTY);
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

    if (!(pObj instanceof Brick)) {
      return false;
    }

    Brick other = (Brick) pObj;

    return (min == other.getMin())
        && (max == other.getMax())
        && stringSet.equals(other.getStringSet());
  }

  @Override
  public StringDomain<Brick> join(StringDomain<Brick> pOther) {
    if (pOther.isBottom()) {
      return BOTTOM;
    }

    if (pOther.isTop()) {
      return this;
    }

    Brick other = (Brick) pOther;
    Brick newBrick = new Brick();

    newBrick.setMin(Math.min(min, other.getMin()));
    newBrick.setMax(Math.max(max, other.getMax()));
    newBrick
        .setStringSet(SetUtil.generalizedUnion(stringSet.asSet(), other.getStringSet().asSet()));

    return newBrick;
  }

  @Override
  public boolean isLessOrEqual(StringDomain<Brick> pOther) {

    if (pOther.isBottom()) {
      return false;
    }

    if(isEmpty()) {
      return true;
    }

    Brick other = (Brick) pOther;

    return min >= other.getMin()
        && max <= other.getMax()
        && other.getStringSet().containsAll(stringSet.asSet());
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
