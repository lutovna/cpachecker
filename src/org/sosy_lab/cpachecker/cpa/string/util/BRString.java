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

import java.util.ArrayList;

public class BRString implements StringDomain<BRString> {

  private static final long serialVersionUID = 1L;
  private ArrayList<StringDomain<Brick>> brickList;

  public BRString() {
    brickList = new ArrayList<>();
  }

  public BRString(String str) {
    brickList = new ArrayList<>();
    brickList.add(new Brick(str));
  }

  private BRString(ArrayList<StringDomain<Brick>> newBrickList) {
    brickList = newBrickList;
  }

  public final static BRString EMPTY = new BRString();
  public final static bottomString<BRString> BOTTOM = new bottomString<>();
  public final static topString<BRString> TOP = new topString<>();

  public boolean isEmpty() {
    return equals(BRString.EMPTY);
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  public void setBrickList(ArrayList<StringDomain<Brick>> newBrickList) {
    brickList.clear();
    brickList.addAll(newBrickList);
  }

  public ArrayList<StringDomain<Brick>> getBrickList() {
    return brickList;
  }

  public void normalize() {

  }

  protected ArrayList<StringDomain<Brick>>
      makeSameLenght(ArrayList<StringDomain<Brick>> brickList2) {
    if (brickList.size() >= brickList2.size()) {
      return brickList;
    }

    ArrayList<StringDomain<Brick>> newBrickList = new ArrayList<>();
    ArrayList<StringDomain<Brick>> brickList1 = copyOf().getBrickList();

    int emptyBricksAdded = 0;
    int i = 0;

    while (i < brickList2.size()) {
      if (emptyBricksAdded >= brickList2.size() - brickList.size()) {
        newBrickList.add(brickList1.get(0));
        brickList1.remove(0);
      } else if (brickList1.isEmpty() || !brickList1.get(0).equals(brickList2.get(i))) {
        newBrickList.add(new Brick());
        emptyBricksAdded++;
      } else {
        newBrickList.add(brickList1.get(0));
        brickList1.remove(0);
      }
    }

    return newBrickList;
  }

  public BRString copyOf() {
    return new BRString(brickList);
  }

  @Override
  public boolean equals(Object pObj) {

    if (!(pObj instanceof BRString)) {
      return false;
    }
    return brickList.equals(((BRString) pObj).getBrickList());
  }

  @Override
  public StringDomain<BRString> join(StringDomain<BRString> pOther) {
    // if (pOther == null) {
    // return null;
    // }

    if(pOther.isBottom()) {
      return BOTTOM;
    }

    ArrayList<StringDomain<Brick>> brickList1 =
        makeSameLenght(((BRString) pOther).getBrickList());
    ArrayList<StringDomain<Brick>> brickList2 = ((BRString) pOther).makeSameLenght(brickList);
    ArrayList<StringDomain<Brick>> newBrickList = new ArrayList<>();

    for (int i = 0; i < brickList1.size(); i++) {
      newBrickList.add(brickList1.get(i).join(brickList2.get(i)));
    }

    BRString newBRString = new BRString(newBrickList);
    newBRString.normalize();
    return newBRString;
  }

  @Override
  public boolean isLessOrEqual(StringDomain<BRString> pOther) {

    if (pOther.isBottom()) {
      return false;
    }
    if (pOther.isTop()) {
      return true;
    }

    ArrayList<StringDomain<Brick>> brickList1 = makeSameLenght(((BRString) pOther).getBrickList());
    ArrayList<StringDomain<Brick>> brickList2 = ((BRString) pOther).makeSameLenght(brickList);

    for (int i = 0; i < brickList1.size(); i++) {
      if (!brickList1.get(i).isLessOrEqual(brickList2.get(i))) {
        return false;
      }
    }

    return true;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
