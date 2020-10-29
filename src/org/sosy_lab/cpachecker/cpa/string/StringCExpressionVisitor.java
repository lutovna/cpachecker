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
package org.sosy_lab.cpachecker.cpa.string;

import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;
import org.sosy_lab.cpachecker.cpa.string.util.CIString;
import org.sosy_lab.cpachecker.cpa.string.util.PRString;
import org.sosy_lab.cpachecker.cpa.string.util.SUString;
import org.sosy_lab.cpachecker.cpa.string.util.StringState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class StringCExpressionVisitor
    extends DefaultCExpressionVisitor<StringState, UnrecognizedCodeException>
    implements CRightHandSideVisitor<StringState, UnrecognizedCodeException> {

  private final CFAEdge cfaEdge;
  private final Strings strings;
  private final BuiltinFunctions builtins;
  private CalledFunctions called;

  public StringCExpressionVisitor(
      CFAEdge edge,
      Strings pStrings,
      BuiltinFunctions pBuiltins,
      CalledFunctions pCalled) {
    cfaEdge = edge;
    strings = pStrings;
    builtins = pBuiltins;
    called = pCalled;
  }

  @Override
  protected StringState visitDefault(CExpression pExp) {
    return StringState.BOTTOM;
  }

  @Override
  public StringState visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {
    return (e.getArrayExpression()).accept(this);
  }

  @Override
  public StringState visit(CCharLiteralExpression e) throws UnrecognizedCodeException {
    return new StringState(Character.toString(e.getCharacter()));
  }

  @Override
  public StringState visit(CStringLiteralExpression e) throws UnrecognizedCodeException {
    return new StringState(e.getContentString());
  }

  @Override
  public StringState visit(CBinaryExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CCastExpression e) throws UnrecognizedCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public StringState visit(CComplexCastExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CFieldReference e) throws UnrecognizedCodeException {
    return strings.getStringState(called.getQualifiedVariableName(e.toASTString()));
  }

  @Override
  public StringState visit(CIdExpression e) throws UnrecognizedCodeException {
    return strings.getStringState(called.getQualifiedVariableNameFromDeclaration(e.getDeclaration()));
  }

  @Override
  public StringState visit(CImaginaryLiteralExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CFloatLiteralExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CIntegerLiteralExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CTypeIdExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CUnaryExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CPointerExpression e) throws UnrecognizedCodeException {
    return (e.getOperand()).accept(this);
  }

  @Override
  public StringState visit(CAddressOfLabelExpression e) throws UnrecognizedCodeException {
    return visitDefault(e);
  }

  @Override
  public StringState visit(CFunctionCallExpression fCallExpression)
      throws UnrecognizedCodeException {

    CExpression fNameExpression = fCallExpression.getFunctionNameExpression();

    if (fNameExpression instanceof CIdExpression) {
      String funcName = ((CIdExpression) fNameExpression).getName();

      if (builtins.isABuiltin(funcName)) {
        return evaluateFunctionExpression(funcName, fCallExpression);
      }
    }
    return StringState.BOTTOM;
  }

  private StringState evaluateFunctionExpression(
      String fName,
      CFunctionCallExpression expression) {
    switch (fName) {
      case "strtok":
        return evaluateSTRTOK(expression);
      case "strstr":
        return evaluateSTRSTR(expression);
      case "strpbrk":
        return evaluateSTRSTR(expression);
      default:
        return StringState.EMPTY;
    }
  }

  private StringState evaluateSTRTOK(CFunctionCallExpression expression) {

    // char *strtok(char *string, const char *delim)

    CExpression s1 = expression.getParameterExpressions().get(0);
    CExpression s2 = expression.getParameterExpressions().get(1);

    try {

      StringState strState1 = s1.accept(this);
      StringState strState2 = s2.accept(this);

      if (strState1.isBottom() || strState2.isBottom()) {
        // if string = NULL
        if (!builtins.isNEW()) {
          return builtins.getPrevCIString();
        }
        return StringState.BOTTOM;

      } else {
        // if string != NULL
        // explicitCIString exCIStr1 = (explicitCIString) strState1;
        // explicitCIString exCIStr2 = (explicitCIString) strState2;

        if (strState1.isEmpty()) {
          // if string is empty we return NULL
          return StringState.BOTTOM;
        }

        builtins.setNEWFalse();

        strState1.setPRDomain(PRString.EMPTY);
        strState1.setSUDomain(SUString.EMPTY);
        // Exists one symbol from delim in string?
        Boolean isInters =
            !SetUtil
                .generalizedIntersect(
                    ((CIString) strState1.getCIDomain()).getMaybe().asSet(),
                    ((CIString) strState2.getCIDomain()).getMaybe().asSet())
                .isEmpty();

        if (isInters) {
          // now we can't say which symbols are certainly in string
          ((CIString) strState1.getCIDomain()).clearCertainly();
          builtins.setPrevStringState(strState1);
          return strState1;
        } else {
          // return NULL
          builtins.setNEWTrue();
          return StringState.BOTTOM;
        }
      }

    } catch (UnrecognizedCodeException e) {
      e.printStackTrace();
    }
    return StringState.EMPTY;
  }

  private StringState evaluateSTRSTR(CFunctionCallExpression expression) {

    // char *strstr(const char *str1, const char *str2)
    CExpression s1 = expression.getParameterExpressions().get(0);
    CExpression s2 = expression.getParameterExpressions().get(1);

    try {

      StringState strState1 = s1.accept(this);
      StringState strState2 = s2.accept(this);

      if (strState1.isBottom() || strState2.isBottom()) {
        // ERROR
        // TODO: write it
        return StringState.BOTTOM;
      }

      // explicitCIString exCIStr1 = (explicitCIString) strState1;
      // explicitCIString exCIStr2 = (explicitCIString) strState2;

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

    } catch (UnrecognizedCodeException e) {
      e.printStackTrace();
    }
    return StringState.EMPTY;
  }
}
