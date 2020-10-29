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

import com.google.common.base.Optional;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.util.SetUtil;
import org.sosy_lab.cpachecker.cpa.string.util.CIString;
import org.sosy_lab.cpachecker.cpa.string.util.PRString;
import org.sosy_lab.cpachecker.cpa.string.util.SUString;
import org.sosy_lab.cpachecker.cpa.string.util.StringDomain;
import org.sosy_lab.cpachecker.cpa.string.util.StringState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class StringTransferRelation
    extends ForwardingTransferRelation<Strings, Strings, SingletonPrecision> {

  private final BuiltinFunctions builtins = new BuiltinFunctions();
  private CalledFunctions called = new CalledFunctions(); // function calls stack
  public StringTransferRelation() {
  }

  @Override
  protected Strings handleBlankEdge(BlankEdge cfaEdge) {
    return state;
  }

  @Override
  protected Strings handleReturnStatementEdge(CReturnStatementEdge cfaEdge) {

    called.setPredessorFunctionName(cfaEdge);
    Strings newState = state;
    if (cfaEdge.asAssignment().isPresent()) {
      CAssignment ass = cfaEdge.asAssignment().get();
      StringCExpressionVisitor visitor =
          new StringCExpressionVisitor(cfaEdge, newState, builtins, called);
      try {
        newState =
            removeAndAddStringState(
                newState,
                ass.getLeftHandSide(),
                ((CExpression) ass.getRightHandSide()).accept(visitor));
        return newState;

      } catch (UnrecognizedCodeException e) {
        e.printStackTrace();
      }
    }

    return state;
  }

  @Override
  protected @Nullable Strings
      handleAssumption(CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
          throws CPATransferException {

    called.setPredessorFunctionName(cfaEdge);
    Strings newState = state;
    if (!(expression instanceof CBinaryExpression)) {
      return null;
    }

    BinaryOperator operator = ((CBinaryExpression)expression).getOperator();
    CExpression operand1 = ((CBinaryExpression)expression).getOperand1();
    CExpression operand2 = ((CBinaryExpression)expression).getOperand2();

    if (!truthAssumption) {
      operator = operator.getOppositLogicalOperator();
    }

    if (operator.getOperator() != "==" && operator.getOperator() != "!=") {
      return state;
    }

    StringCExpressionVisitor visitor =
        new StringCExpressionVisitor(cfaEdge, newState, builtins, called);
    StringState strState1 = operand1.accept(visitor);
    StringState strState2 = operand2.accept(visitor);

    // if we don't check this cpa_string will break on (x == 1) for example
    if (!strState1.isBottom() && !strState2.isBottom()) {

      boolean equal = canBeEqual(strState1, strState2);

      switch (operator) {
        case EQUALS: {
          if (!equal) {
            return null;
          }
        }
          break;
        case NOT_EQUALS: {
          if (equal) {
            return null;
          }
        }
          break;
        default:
          break;
      }
    }
    return newState;
  }



  @Override
  protected Strings
      handleDeclarationEdge(CDeclarationEdge cfaEdge, CDeclaration declaration)
          throws UnrecognizedCodeException {

    called.setPredessorFunctionName(cfaEdge);
    Strings newState = state;

    if (!(declaration instanceof CVariableDeclaration)) {
      return newState;
    }
    CVariableDeclaration decl = (CVariableDeclaration) declaration;

    // we need initilizer to be from CInitializerExpression to take expression from it
    if ((decl.getInitializer() instanceof CInitializerExpression)) {

    CInitializerExpression init = (CInitializerExpression) decl.getInitializer();
    CExpression exp = init.getExpression();

    newState =
          newState.removeAndAddStringState(
              called.getQualifiedVariableNameFromDeclaration(decl),
              evaluateStringState(newState, exp, cfaEdge));
    }

    return newState;

  }

  @Override
  protected Strings handleStatementEdge(CStatementEdge cfaEdge, CStatement statement)
      throws UnrecognizedCodeException {

    called.setPredessorFunctionName(cfaEdge);
    if (statement instanceof CAssignment) {
      return handleAssignment((CAssignment) statement, cfaEdge);
    } else if (statement instanceof CFunctionCall) {

      CFunctionCall fCall = (CFunctionCall) statement;
      CFunctionCallExpression fCallExp = fCall.getFunctionCallExpression();
      CExpression fNameExpression = fCallExp.getFunctionNameExpression();

      if (fNameExpression instanceof CIdExpression) {
        String funcName = ((CIdExpression) fNameExpression).getName();

        if (builtins.isABuiltin(funcName)) {
          return handleBuiltinFunctionCall(cfaEdge, fCallExp, funcName);
        }
      }
    }
    return state;
  }

  protected Strings
      handleBuiltinFunctionCall(
          CStatementEdge cfaEdge,
          CFunctionCallExpression fCallExp,
          String calledFunctionName) {
    switch (calledFunctionName) {
      case "strcpy":
        return evaluateSTRCPY("strcpy", cfaEdge, fCallExp);
      case "strncpy":
        return evaluateSTRCPY("strncpy", cfaEdge, fCallExp);
      case "strcat":
        return evaluateSTRCAT("strcat", cfaEdge, fCallExp);
      case "strncat":
        return evaluateSTRCAT("strncat", cfaEdge, fCallExp);
      case "memcpy":
        return evaluateSTRCPY("memcpy", cfaEdge, fCallExp);
      case "memmove":
        return evaluateSTRCPY("memmove", cfaEdge, fCallExp);
      default:
        return state;
    }
  }

  private Strings handleAssignment(CAssignment assignExpression, CStatementEdge cfaEdge)
      throws UnrecognizedCodeException {

    Strings newState = state;

    CExpression op1 = assignExpression.getLeftHandSide();
    CRightHandSide op2 = assignExpression.getRightHandSide();

    return removeAndAddStringState(newState, op1, evaluateStringState(newState, op2, cfaEdge));

  }

  @Override
  protected Strings handleFunctionCallEdge(
      CFunctionCallEdge cfaEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> parameters,
      String calledFunctionName)
      throws UnrecognizedCodeException {

    called.setPredessorFunctionName(cfaEdge);
    Strings newState = state;
    called.addFunction(calledFunctionName);

    for (int i = 0; i < parameters.size(); i++) {

      // arguments have names with function from predessor
      called.setInPredFunTrue();
      StringState stringState = evaluateStringState(newState, arguments.get(i), cfaEdge);
      // parametrs have names with function from peek of stack (= from successor)
      called.setInPredFunFalse();
      String formalParameterName =
          called.getQualifiedVariableNameFromDeclaration(parameters.get(i));
      newState = newState.removeAndAddStringState(formalParameterName, stringState);

    }

    return newState;
  }

  @Override
  protected Strings handleFunctionReturnEdge(
      CFunctionReturnEdge cfaEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws CPATransferException {

    called.setPredessorFunctionName(cfaEdge);
    Strings newState = state;
    Optional<CVariableDeclaration> retVar = fnkCall.getFunctionEntry().getReturnVariable();


    if (retVar.isPresent()) {
      String retVarName = called.getQualifiedVariableNameFromDeclaration(retVar.get());
      newState = newState.removeStringState(retVarName);

      if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcExp = (CFunctionCallAssignmentStatement) summaryExpr;

        if (state.contains(retVarName)) {
          newState =
              removeAndAddStringState(
                  newState,
                funcExp.getLeftHandSide(),
                  state.getStringState(retVarName));
        }
      }

    } // else if (summaryExpr instanceof CFunctionCallStatement) {
      // TODO: what should we do here?
    // } else {
    // throw new UnrecognizedCodeException("on function return", cfaEdge, summaryExpr);
    // }

    return deleteLocalVariablesAndPopFunction(newState);
  }

  private Strings
      evaluateSTRCPY(String fName, CStatementEdge cfaEdge, CFunctionCallExpression expression) {
    Strings newState = state;

    CExpression s1 = expression.getParameterExpressions().get(0);
    CExpression s2 = expression.getParameterExpressions().get(1);

    StringCExpressionVisitor visitor =
        new StringCExpressionVisitor(cfaEdge, newState, builtins, called);
    try {
      StringState strState = s2.accept(visitor);

      if (!strState.isBottom() || !fName.equals("strcpy")) {

        StringState newStringState = StringState.EMPTY.copyOf();

        if (!strState.getCIDomain().isBottom() && !strState.getCIDomain().isTop()) {
          CIString newCIStr = (CIString) strState.getCIDomain();
          newCIStr.clearCertainly();
          newStringState.setCIDomain(newCIStr);
        } else {
          newStringState.setCIDomain(strState.getCIDomain());
        }

        return removeAndAddStringState(newState, s1, newStringState);

      } else {
        return removeAndAddStringState(newState, s1, strState);
      }
    } catch (UnrecognizedCodeException e) {
      e.printStackTrace();
    }
    return newState;
  }

  private Strings
      evaluateSTRCAT(String fName, CStatementEdge cfaEdge, CFunctionCallExpression expression) {
    Strings newState = state;

    CExpression s1 = expression.getParameterExpressions().get(0);
    CExpression s2 = expression.getParameterExpressions().get(1);

    /*
     * if (fName.equals("strncat")) { CExpression s3 = expression.getParameterExpressions().get(2);
     * }
     */

    StringCExpressionVisitor visitor =
        new StringCExpressionVisitor(cfaEdge, newState, builtins, called);
    try {
      StringState strState1 = s1.accept(visitor);
      StringState strState2 = s2.accept(visitor);

      if (!strState1.isBottom() && !strState2.isBottom()) {
        return removeAndAddStringState(newState, s1, concat(strState1, strState2, fName));
      }
    } catch (UnrecognizedCodeException e) {
      e.printStackTrace();
    }
    return newState;
  }

  private Strings
      removeAndAddStringState(Strings newState, CExpression expression, StringState stringState) {

    if (expression instanceof CArraySubscriptExpression) {
      expression = ((CArraySubscriptExpression) expression).getArrayExpression();
      newState = addStringState(newState, expression, stringState); // add without remove bc we
                                                                        // don't know
                                                              // which part of the string changed
    } else if (expression instanceof CIdExpression) {
      newState =
          newState.removeAndAddStringState(
              called.getQualifiedVariableNameFromDeclaration(
                  ((CIdExpression) expression).getDeclaration()),
              stringState);
    } else if (expression instanceof CFieldReference) {
      newState =
          newState.removeAndAddStringState(
              called.getQualifiedVariableName(expression.toASTString()),
              stringState);
    }

    return newState;
  }

  private Strings
      addStringState(Strings newState, CExpression expression, StringState stringState) {

    if (expression instanceof CArraySubscriptExpression) {
      expression = ((CArraySubscriptExpression) expression).getArrayExpression();
    }
    if (expression instanceof CIdExpression) {

      newState =
          newState.addStringState(
              called.getQualifiedVariableNameFromDeclaration(
                  ((CIdExpression) expression).getDeclaration()),
              stringState);
    } else if (expression instanceof CFieldReference) {
      newState =
          newState.addStringState(
              called.getQualifiedVariableName(expression.toASTString()),
              stringState);
    }

    return newState;
  }

  // return new domain(expression)
  private StringState
      evaluateStringState(Strings strings, CRightHandSide expression, CFAEdge cfaEdge)
          throws UnrecognizedCodeException {
    return expression
        .accept(new StringCExpressionVisitor(cfaEdge, strings, builtins, called));
  }

  // this function delete domains of function local variables and pop function from stack
  private Strings deleteLocalVariablesAndPopFunction(Strings pState) {
    Strings newState = pState;
    newState = newState.deleteByPrefix(called.getQualifiedFunctionName() + "::");
    called.popFunction();
    return newState;
  }

  private boolean canBeEqual(StringState strState1, StringState strState2) {

    StringDomain<CIString> ciStr1 = strState1.getCIDomain();
    StringDomain<CIString> ciStr2 = strState2.getCIDomain();

    if (!ciStr1.isBottom() && !ciStr2.isBottom() && !ciStr1.isTop() && !ciStr2.isTop()) {
      Set<Character> set = new HashSet<>();

      set =
          SetUtil.generalizedIntersect(
              ((CIString) ciStr1).getMaybe().asSet(),
              ((CIString) ciStr2).getMaybe().asSet());

      if (set.isEmpty()) {
        return false;
      }
    }

    StringDomain<PRString> prStr1 = strState1.getPRDomain();
    StringDomain<PRString> prStr2 = strState2.getPRDomain();

    if (!prStr1.isBottom() && !prStr2.isBottom() && !prStr1.isTop() && !prStr2.isTop()) {
      PRString prString1 = (PRString) prStr1;
      PRString prString2 = (PRString) prStr2;

      if (prString1.getLenght() >= prString2.getLenght()) {
        if (!prString1.getPrefix().startsWith(prString2.getPrefix())) {
          return false;
        }
      } else {
        if (!prString2.getPrefix().startsWith(prString1.getPrefix())) {
          return false;
        }
      }

    }

    StringDomain<SUString> suStr1 = strState1.getSUDomain();
    StringDomain<SUString> suStr2 = strState2.getSUDomain();

    if (!suStr1.isBottom() && !suStr2.isBottom() && !suStr1.isTop() && !suStr2.isTop()) {
      SUString suString1 = (SUString) suStr1;
      SUString suString2 = (SUString) suStr2;

      if (suString1.getLenght() >= suString2.getLenght()) {
        if (!suString1.getSuffix().endsWith(suString2.getSuffix())) {
          return false;
        }
      } else {
        if (!suString2.getSuffix().endsWith(suString1.getSuffix())) {
          return false;
        }
      }

    }

    return true;
  }

  private StringState concat(StringState strState1, StringState strState2, String fName) {

    if (strState2.isTop() || strState1.isTop()) {
      return StringState.TOP;
    }

    StringState newStringState = strState1.copyOf();

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

    if (strState2.getPRDomain().isBottom()) {
      newStringState.setPRDomain(PRString.BOTTOM);
    }

    if (strState1.getSUDomain().isBottom()) {
      newStringState.setSUDomain(SUString.BOTTOM);
    } else if (fName.equals("strcat")) {
      newStringState.setSUDomain(strState2.getSUDomain());
    } else if (fName.equals("strncat")) {
      newStringState.setSUDomain(SUString.EMPTY);
    }

    return newStringState;
  }

}
