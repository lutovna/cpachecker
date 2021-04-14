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

public interface StringDomain<T> extends Serializable, LatticeAbstractState<StringDomain<T>> {

  // Integer number = 0;

  boolean isBottom();

  boolean isTop();

  // Integer getNumber();

  @Override
  boolean equals(Object pObj);

  @Override
  StringDomain<T> join(StringDomain<T> pOther);

  @Override
  public boolean isLessOrEqual(StringDomain<T> pOther);

  @Override
  public String toString();

  public class bottomString<T> implements StringDomain<T> {

    private static final long serialVersionUID = 1L;

    public bottomString() {
    }

    @Override
    public boolean isBottom() {
      return true;
    }

    @Override
    public boolean isTop() {
      return false;
    }

    @Override
    public StringDomain<T> join(StringDomain<T> pOther) {
      return this;
    }

    @Override
    public boolean isLessOrEqual(StringDomain<T> pOther) {
      return true;
    }

    @Override
    public String toString() {
      return "BOTTOM";
    }
  }

  public class topString<T> implements StringDomain<T> {

    private static final long serialVersionUID = 1L;

    public topString() {
    }

    @Override
    public boolean isBottom() {
      return false;
    }

    @Override
    public boolean isTop() {
      return true;
    }

    @Override
    public StringDomain<T> join(StringDomain<T> pOther) {
      return pOther;
    }

    @Override
    public boolean isLessOrEqual(StringDomain<T> pOther) {
      return pOther.isTop();
    }

    @Override
    public String toString() {
      return "TOP";
    }

  }
}
