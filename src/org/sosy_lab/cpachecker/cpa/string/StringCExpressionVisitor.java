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
import org.sosy_lab.cpachecker.cpa.string.util.PRString;
import org.sosy_lab.cpachecker.cpa.string.util.SUString;
import org.sosy_lab.cpachecker.cpa.string.util.StringState;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class StringCExpressionVisitor
    extends DefaultCExpressionVisitor<StringState, UnrecognizedCodeException>
    implements CRightHandSideVisitor<StringState, UnrecognizedCodeException> {

  // private final CFAEdge cfaEdge;
  private final Strings strings;
  private final BuiltinFunctions builtins;
  private CalledFunctions called;

  public StringCExpressionVisitor(
      // CFAEdge edge,
      Strings pStrings,
      BuiltinFunctions pBuiltins,
      CalledFunctions pCalled) {
    // cfaEdge = edge;
    strings = pStrings;
    builtins = pBuiltins;
    called = pCalled;
  }

  @Override
  protected StringState visitDefault(CExpression pExp) throws UnrecognizedCodeException {
    return StringState.BOTTOM;
  }

  @Override
  public StringState visit(CArraySubscriptExpression e) throws UnrecognizedCodeException {

    StringState newStringState = e.getArrayExpression().accept(this);
    newStringState.setPRDomain(PRString.TOP);
    newStringState.setSUDomain(SUString.TOP);

    return newStringState;
  }

  @Override
  public StringState visit(CCharLiteralExpression e) throws UnrecognizedCodeException {
    return new StringState(strings.getActivity(), Character.toString(e.getCharacter()));
  }

  @Override
  public StringState visit(CStringLiteralExpression e) throws UnrecognizedCodeException {
    return new StringState(strings.getActivity(), e.getContentString());
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
    return e.getOperand().accept(this);
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
      CFunctionCallExpression expression)
      throws UnrecognizedCodeException {
    switch (fName) {
      case "strtok":
        return builtins.evaluateSTRTOK(this, expression);
      case "strstr":
        return builtins.evaluateSTRSTR(this, expression);
      case "strpbrk":
        return builtins.evaluateSTRSTR(this, expression);
      case "strcpy":
        return builtins.evaluateSTRCPY(this, expression, "strcpy");
      case "strncpy":
        return builtins.evaluateSTRCPY(this, expression, "strncpy");
      case "memcpy":
        return builtins.evaluateSTRCPY(this, expression, "memcpy");
      case "memmove":
        return builtins.evaluateSTRCPY(this, expression, "memmove");
      case "strcat":
        return builtins.evaluateSTRCAT(this, expression, "strcat");
      case "strncat":
        return builtins.evaluateSTRCAT(this, expression, "strncat");
      default:
        return StringState.EMPTY;
    }
  }
}
