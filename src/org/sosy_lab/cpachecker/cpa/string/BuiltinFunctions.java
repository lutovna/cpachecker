/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.string;

import com.google.common.collect.Sets;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;
import org.sosy_lab.cpachecker.cpa.string.util.CIString;
import org.sosy_lab.cpachecker.cpa.string.util.PRString;
import org.sosy_lab.cpachecker.cpa.string.util.SUString;
import org.sosy_lab.cpachecker.cpa.string.util.StringDomain;
import org.sosy_lab.cpachecker.cpa.string.util.StringState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class BuiltinFunctions {

  private final Set<String> BFUNC =
      Sets.newHashSet(
          "strcpy",
          "strncpy",
          "strcat",
          "strncat",
          "memcpy",
          "memmove",
          "strtok",
          "strstr",
          "strpbrk");

  private boolean STRTOK_NEW;
  private StringState prevString;

  private static final int numberOfDomains = 3;
  private static boolean[] activity = new boolean[numberOfDomains];

  public final boolean isABuiltin(String fName) {
    return BFUNC.contains(fName);
  }

  BuiltinFunctions(boolean[] newActivity) {
    STRTOK_NEW = true;
    prevString = StringState.BOTTOM;
    activity = newActivity;
  }

  public StringState
      evaluateSTRCPY(
          StringCExpressionVisitor visitor,
          CFunctionCallExpression expression,
          String fName)
          throws UnrecognizedCodeException {
    CExpression s2 = expression.getParameterExpressions().get(1);

    StringState strState = s2.accept(visitor);

    if (!strState.isBottom() || !fName.equals("strcpy")) {

      StringState newStringState = StringState.TOP.copyOf();

      if (activity[0]) {

        if (!strState.getCIDomain().isBottom() && !strState.getCIDomain().isTop()) {

          CIString newCIStr = (CIString) strState.getCIDomain();
          newCIStr.clearCertainly();
          newStringState.setCIDomain(newCIStr);

        } else {
          newStringState.setCIDomain(strState.getCIDomain());
        }
      }

        // TODO: after adding lenght make this more accurate (PR)
        return newStringState;
      }

    return StringState.EMPTY;
  }

  public StringState evaluateSTRCAT(
      StringCExpressionVisitor visitor,
      CFunctionCallExpression expression,
      String fName)
      throws UnrecognizedCodeException {

    CExpression s1 = expression.getParameterExpressions().get(0);
    CExpression s2 = expression.getParameterExpressions().get(1);

    StringState strState1 = s1.accept(visitor);
    StringState strState2 = s2.accept(visitor);

    if (!strState1.isBottom() && !strState2.isBottom()) {
      return concat(strState1, strState2, fName);
    }
    return StringState.EMPTY;
  }

  public StringState
      evaluateSTRTOK(StringCExpressionVisitor visitor, CFunctionCallExpression expression)
          throws UnrecognizedCodeException {

    // char *strtok(char *string, const char *delim)

    CExpression s1 = expression.getParameterExpressions().get(0);
    CExpression s2 = expression.getParameterExpressions().get(1);


    StringState strState1 = s1.accept(visitor);
    StringState strState2 = s2.accept(visitor);

    if (strState1.isBottom() || strState2.isBottom()) {
      // if string = NULL
      if (!isNEW()) {
        return getPrevCIString();
      }
      return StringState.BOTTOM;

    } else {

      if (strState1.isEmpty()) {
        // if string is empty we return NULL
        return StringState.BOTTOM;
      }

      setNEWFalse();

      // TODO: make it more accurate
      strState1.setPRDomain(PRString.TOP);
      strState1.setSUDomain(SUString.TOP);
      // Exists one symbol from delim in string?
      if (activity[0]) {
        Boolean isInters =
            !SetUtil
                .generalizedIntersect(
                  ((CIString) strState1.getCIDomain()).getMaybe().asSet(),
                  ((CIString) strState2.getCIDomain()).getMaybe().asSet())
              .isEmpty();

        if (isInters) {
          // now we can't say which symbols are certainly in string
          ((CIString) strState1.getCIDomain()).clearCertainly();
          setPrevStringState(strState1);
          return strState1;
        } else {
          // return NULL
          setNEWTrue();
          return StringState.BOTTOM;
        }
      }

      return strState1;
    }
  }

  public StringState
      evaluateSTRSTR(StringCExpressionVisitor visitor, CFunctionCallExpression expression)
          throws UnrecognizedCodeException {

    // char *strstr(const char *str1, const char *str2)
    CExpression s1 = expression.getParameterExpressions().get(0);
    CExpression s2 = expression.getParameterExpressions().get(1);


      StringState strState1 = s1.accept(visitor);
      StringState strState2 = s2.accept(visitor);

      if (strState1.isBottom() || strState2.isBottom()) {
        // ERROR
        // TODO: write it
        return StringState.BOTTOM;
      }

      if (strState1.isLessOrEqual(strState2)) {
        // if str2 is found in str1
        if (strState2.isEmpty()) {
          return strState1;
        }
        // we know only that str2 is in certainly
        return strState1.join(strState2);

      } else {
        // if the str2 is not found in str1 return NULL
        return StringState.BOTTOM;
      }
  }

  public void setNEWTrue() {
    STRTOK_NEW = true;
  }

  public void setNEWFalse() {
    STRTOK_NEW = false;
  }

  public boolean isNEW() {
    return STRTOK_NEW;
  }

  public void setPrevStringState(StringState string) {
    prevString = string;
  }

  public StringState getPrevCIString() {
    return prevString;
  }

  private StringState concat(StringState strState1, StringState strState2, String fName) {

    if (strState2.isTop() || strState1.isTop()) {
      return StringState.TOP;
    }

    StringState newStringState = strState1.copyOf();

    if (activity[0]) {

      StringDomain<CIString> ciStr = newStringState.getCIDomain();
      StringDomain<CIString> ciStr2 = strState2.getCIDomain();

      if (!ciStr.isBottom() && !ciStr2.isBottom() && !ciStr.isTop() && !ciStr2.isTop()) {
        if (fName.equals("strcat")) {
          ((CIString) ciStr).addToSertainly(((CIString) ciStr2).getCertainly().asSet());
        }
        ((CIString) ciStr).addToMaybe(((CIString) ciStr2).getMaybe().asSet());
      } else if (ciStr.isBottom() || ciStr2.isBottom()) {
        newStringState.setCIDomain(CIString.BOTTOM);
      } else if (ciStr.isTop() || ciStr2.isTop()) {
        newStringState.setCIDomain(CIString.TOP);
      }
    }

    if (activity[1]) {
      if (strState2.getPRDomain().isBottom()) {
        newStringState.setPRDomain(PRString.BOTTOM);
      }
    }

    if (activity[2]) {
      if (strState1.getSUDomain().isBottom()) {
        newStringState.setSUDomain(SUString.BOTTOM);
      } else if (fName.equals("strcat")) {
        newStringState.setSUDomain(strState2.getSUDomain());
      } else if (fName.equals("strncat")) {
        // TODO: make that more accurate after adding lenght
        newStringState.setSUDomain(SUString.EMPTY);
      }
    }

    return newStringState;
  }
}
