/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.sign;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;


public class SignTransferRelation extends ForwardingTransferRelation<SignState, SingletonPrecision> {

  @SuppressWarnings("unused")
  private LogManager logger;

  private Set<String> globalVariables = new HashSet<>();

  private Deque<Set<String>> stackVariables = new ArrayDeque<>();

  public final static String FUNC_RET_VAR = "__func_ret__";

  public SignTransferRelation(LogManager pLogger) {
    logger = pLogger;
    stackVariables.push(new HashSet<String>());
  }

  public String getScopedVariableName(IAExpression pVariableName) {
    return getScopedVariableName(pVariableName, functionName);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pState, List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge, Precision pPrecision) throws CPATransferException, InterruptedException {
    return null;
  }

  @Override
  protected SignState handleReturnStatementEdge(AReturnStatementEdge pCfaEdge, IAExpression pExpression)
      throws CPATransferException {
    if(pExpression == null) {
      pExpression = CNumericTypes.ZERO; // default in c
    }
    String assignedVar = getScopedVariableName(FUNC_RET_VAR, functionName);
    SignState result = handleAssignmentToVariable(state, assignedVar, pExpression);
    return result;
  }

  @Override
  protected SignState handleFunctionCallEdge(FunctionCallEdge pCfaEdge, List<? extends IAExpression> pArguments,
      List<? extends AParameterDeclaration> pParameters, String pCalledFunctionName) throws CPATransferException {
    if (!pCfaEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (pParameters.size() == pArguments.size());
    }
    SignState successor = state;
    stackVariables.push(new HashSet<String>()); // side-effect: allocate space for local function variables
    for(int i = 0; i < pParameters.size(); i++) {
      IAExpression exp = pArguments.get(i);
      if(!(exp instanceof CExpression)) {
        throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
      }
      String scopedVarIdent = getScopedVariableName(pParameters.get(i).getName(), pCalledFunctionName);
      stackVariables.getFirst().add(scopedVarIdent);
      successor = handleAssignmentToVariable(successor, scopedVarIdent, exp); // TODO performance
    }
    return successor;
  }

  @Override
  protected SignState handleFunctionReturnEdge(FunctionReturnEdge pCfaEdge, FunctionSummaryEdge pFnkCall,
      AFunctionCall pSummaryExpr, String pCallerFunctionName) throws CPATransferException {

    // x = fun();
    if (pSummaryExpr instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignStmt = (AFunctionCallAssignmentStatement) pSummaryExpr;
      IAExpression leftSide = assignStmt.getLeftHandSide();
      if (!(leftSide instanceof AIdExpression)) { throw new UnrecognizedCodeException("Unsupported code found",
          pCfaEdge); }
      String returnVarName = getScopedVariableName(FUNC_RET_VAR, functionName);
      String assignedVarName = getScopedVariableName(leftSide, pCallerFunctionName);

      SignState result = state
          .assignSignToVariable(assignedVarName, state.getSignMap().getSignForVariable(returnVarName))
          .removeSignAssumptionOfVariable(returnVarName);


      // Clear stack TODO move to handleFunctionReturnEdge otherwise these variables are not removed if no return statement exists
      Set<String> localFunctionVars = stackVariables.pop();
      for(String scopedVarIdent : localFunctionVars) {
        result = result.removeSignAssumptionOfVariable(scopedVarIdent); // TODO performance
      }

      return result;
    }

    // fun()
    if (pSummaryExpr instanceof AFunctionCallStatement) {
      return state.removeSignAssumptionOfVariable(getScopedVariableName(FUNC_RET_VAR, functionName));
    }

    throw new UnrecognizedCodeException("Unsupported code found", pCfaEdge);
  }

  private static class IdentifierValuePair {
    CIdExpression identifier;
    SIGN value;
    public IdentifierValuePair(CIdExpression pIdentifier, SIGN pValue) {
      super();
      identifier = pIdentifier;
      value = pValue;
    }
  }

  private BinaryOperator negateComparisonOperator(BinaryOperator pOp) {
    switch(pOp) {
    case LESS_THAN:
      return BinaryOperator.GREATER_EQUAL;
    case LESS_EQUAL:
      return BinaryOperator.GREATER_THAN;
    case GREATER_THAN:
      return BinaryOperator.LESS_EQUAL;
    case GREATER_EQUAL:
      return BinaryOperator.LESS_THAN;
    case EQUALS:
      return BinaryOperator.NOT_EQUALS;
    case NOT_EQUALS:
      return BinaryOperator.EQUALS;
     default:
       throw new IllegalArgumentException("Cannot negate given operator");
    }
  }

  private Optional<IdentifierValuePair> evaluateAssumption(CBinaryExpression pAssumeExp, boolean truthAssumption, CFAEdge pCFAEdge)  {
    Optional<CIdExpression> optStrongestIdent = getStrongestIdentifier(pAssumeExp, pCFAEdge);
    if(!optStrongestIdent.isPresent()) {
      return Optional.absent(); // No refinement possible, since no strongest identifier was found
    }
    CIdExpression strongestIdent = optStrongestIdent.get();
    CExpression refinementExpression = getRefinementExpression(strongestIdent, pAssumeExp);
    BinaryOperator resultOp = !truthAssumption ? negateComparisonOperator(pAssumeExp.getOperator()) : pAssumeExp.getOperator();
    SIGN resultSign;
    try {
      resultSign = refinementExpression.accept(new SignCExpressionVisitor(pCFAEdge, state, this));
    } catch (UnrecognizedCodeException e) {
      return Optional.absent();
    }
    Optional<IdentifierValuePair> result = evaluateAssumption(strongestIdent, resultOp, resultSign, pCFAEdge, isLeftOperand(strongestIdent, pAssumeExp));
    return result;
  }

  private boolean isLeftOperand(CExpression pExp, CBinaryExpression  pBinExp) {
    if(pExp == pBinExp.getOperand1()) {
      return true;
    } else if(pExp == pBinExp.getOperand2()) {
      return false;
    }
    throw new IllegalArgumentException("Argument pExp is not part of pBinExp");
  }

  private Optional<IdentifierValuePair> evaluateAssumption(CIdExpression pIdExp, BinaryOperator pOp, SIGN pResultSign, CFAEdge pCFAEdge, boolean pIdentIsLeft) {
    boolean equalZero = false;
    switch(pOp) {
    case GREATER_EQUAL:
      equalZero = pResultSign == SIGN.ZERO;
      //$FALL-THROUGH$
    case GREATER_THAN:
      if(pIdentIsLeft) {
        if(SIGN.PLUS0.covers(pResultSign)) { // x > (0)+
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.PLUS0 : SIGN.PLUS));
        }
      } else {
        if(SIGN.MINUS0.covers(pResultSign)) { // (0)- > x
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.MINUS0 : SIGN.MINUS));
        }
      }
      break;
    case LESS_EQUAL:
      equalZero = pResultSign == SIGN.ZERO;
      //$FALL-THROUGH$
    case LESS_THAN:
      if(pIdentIsLeft) { // x < (0)-
        if(SIGN.MINUS0.covers(pResultSign)) {
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.MINUS0 : SIGN.MINUS));
        }
      } else {
        if(SIGN.PLUS0.covers(pResultSign)) { // (0)+ < x
          return Optional.of(new IdentifierValuePair(pIdExp, equalZero ? SIGN.PLUS0 : SIGN.PLUS));
        }
      }
      break;
    case EQUALS:
      return Optional.of(new IdentifierValuePair(pIdExp, pResultSign));
    }
    return Optional.absent();
  }

  private CExpression getRefinementExpression(CIdExpression pStrongestIdent, CBinaryExpression pBinExp) {
    if(pStrongestIdent == pBinExp.getOperand1()) {
      return pBinExp.getOperand2();
    } else if(pStrongestIdent == pBinExp.getOperand2()) {
      return pBinExp.getOperand1();
    }
    throw new IllegalArgumentException("Strongest identifier is not part of binary expression");
  }

  private List<CIdExpression> filterIdentifier(CBinaryExpression pAssumeExp) {
    List<CIdExpression> result = new ArrayList<>();
    if((pAssumeExp.getOperand1() instanceof CIdExpression)) {
      result.add((CIdExpression)pAssumeExp.getOperand1());
    }
    if((pAssumeExp.getOperand2() instanceof CIdExpression)) {
      result.add((CIdExpression)pAssumeExp.getOperand2());
    }
    return result;
  }

  private Optional<CIdExpression> getStrongestIdentifier(CBinaryExpression pAssumeExp, CFAEdge pCFAEdge) {
    List<CIdExpression> result = filterIdentifier(pAssumeExp);
    if(result.isEmpty()) {
      return Optional.absent();
    }
    if(result.size() == 1) {
      return Optional.of(result.get(0));
    }
    try {
      SIGN leftResultSign = result.get(0).accept(new SignCExpressionVisitor(pCFAEdge, state, this));
      SIGN rightResultSign = result.get(1).accept(new SignCExpressionVisitor(pCFAEdge, state, this));
      if(leftResultSign.covers(rightResultSign)) {
        return Optional.of(result.get(0));
      } else {
        return Optional.of(result.get(1));
      }
    } catch(UnrecognizedCodeException ex) {
      return Optional.absent();
    }
  }

  @Override
  protected SignState handleAssumption(CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException {
    // Analyse only expressions of the form x op y
    if(!(pExpression instanceof CBinaryExpression)) {
      return state;
    }
    Optional<IdentifierValuePair> result = evaluateAssumption((CBinaryExpression)pExpression, pTruthAssumption, pCfaEdge);
    if(result.isPresent()) {
      return state.assignSignToVariable(getScopedVariableName(result.get().identifier), result.get().value);
    }
    return state;
  }

  @Override
  protected SignState handleDeclarationEdge(ADeclarationEdge pCfaEdge, IADeclaration pDecl) throws CPATransferException {
    if(!(pDecl instanceof AVariableDeclaration)) {
      return state;
    }
    AVariableDeclaration decl = (AVariableDeclaration)pDecl;
    String scopedId;
    if(decl.isGlobal()) {
      scopedId = decl.getName();
      globalVariables.add(decl.getName());
    } else {
      scopedId = getScopedVariableName(decl.getName(), functionName);
      stackVariables.getFirst().add(scopedId);
    }
    IAInitializer init = decl.getInitializer();
    if(init instanceof AInitializerExpression) {
      return handleAssignmentToVariable(state, scopedId, ((AInitializerExpression)init).getExpression());
    }
    // since it is C, we assume it may have any value here
    return state.assignSignToVariable(scopedId, SIGN.ALL);
  }

  @Override
  protected SignState handleStatementEdge(AStatementEdge pCfaEdge, IAStatement pStatement) throws CPATransferException {
    // expression is a binary expressionm e.g. a = b.
    if(pStatement instanceof IAssignment) {
      return handleAssignment((IAssignment)pStatement);
    }

    // only expression expr; does not change state
    if(pStatement instanceof AExpressionStatement){
      return state;
    }

    throw new UnrecognizedCodeException("only assignments are supported at this time", edge);
  }

  private SignState handleAssignment(IAssignment pAssignExpr)
      throws CPATransferException {
    IAExpression left = pAssignExpr.getLeftHandSide();
    // a = ...
    if(left instanceof AIdExpression) {
      String scopedId = getScopedVariableName(left, functionName);
      return handleAssignmentToVariable(state, scopedId, pAssignExpr.getRightHandSide());
    }
    throw new UnrecognizedCodeException("left operand has to be an id expression", edge);
  }

  private SignState handleAssignmentToVariable(SignState pState, String pVarIdent, IARightHandSide pRightExpr)
      throws CPATransferException {
    if(pRightExpr instanceof CRightHandSide) {
      CRightHandSide right = (CRightHandSide)pRightExpr;
      SIGN result = right.accept(new SignCExpressionVisitor(edge, pState, this));
      return pState.assignSignToVariable(pVarIdent, result);
    }
    throw new UnrecognizedCodeException("unhandled righthandside expression", edge);
  }

  private String getScopedVariableName(IAExpression pVariableName, String pCalledFunctionName) {
    if (isGlobal(pVariableName)) {
      return pVariableName.toASTString();
    }
    return pCalledFunctionName + "::" + pVariableName.toASTString();
  }

  private String getScopedVariableName(String pVariableName, String pCallFunctionName) {
     if(globalVariables.contains(pVariableName)) {
       return pVariableName;
     }
     return pCallFunctionName + "::" + pVariableName;
  }

}
