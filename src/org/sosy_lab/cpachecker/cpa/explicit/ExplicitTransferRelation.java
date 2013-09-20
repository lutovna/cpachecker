/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.AArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.AExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.APointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.AUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IAExpression;
import org.sosy_lab.cpachecker.cfa.ast.IAInitializer;
import org.sosy_lab.cpachecker.cfa.ast.IARightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IAStatement;
import org.sosy_lab.cpachecker.cfa.ast.IAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.java.JBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JEnumConstantExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldAccess;
import org.sosy_lab.cpachecker.cfa.ast.java.JFieldDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.java.JSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JClassOrInterfaceType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.forwarding.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.cpa.pointer.Memory;
import org.sosy_lab.cpachecker.cpa.pointer.Pointer;
import org.sosy_lab.cpachecker.cpa.pointer.PointerState;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.ImmutableMap;

@Options(prefix="cpa.explicit")
public class ExplicitTransferRelation extends ForwardingTransferRelation<ExplicitState, ExplicitPrecision> {
  // set of functions that may not appear in the source code
  // the value of the map entry is the explanation for the user
  private static final Map<String, String> UNSUPPORTED_FUNCTIONS
      = ImmutableMap.of("pthread_create", "threads");

  @Option(description = "if there is an assumption like (x!=0), "
      + "this option sets unknown (uninitialized) variables to 1L, "
      + "when the true-branch is handled.")
  private boolean initAssumptionVars = false;

  private final Set<String> globalVariables = new HashSet<>();

  private final Set<String> javaNonStaticVariables = new HashSet<>();

  private String missingInformationLeftVariable = null;
  private Type missingInformationLeftVariableType = null;
  private String missingInformationLeftPointer  = null;
  private Type missingInformationLeftPointerType  = null;

  private IARightHandSide missingInformationRightExpression = null;

  /**
   * name for the special variable used as container for return values of functions
   */
  public static final String FUNCTION_RETURN_VAR = "___cpa_temp_result_var_";


  private JRightHandSide missingInformationRightJExpression = null;
  private String missingInformationLeftJVariable = null;

  private boolean missingFieldVariableObject;
  private Pair<String, Long> fieldNameAndInitialValue;

  private boolean missingScopedFieldName;
  private JIdExpression notScopedField;
  private Long notScopedFieldValue;

  private boolean missingAssumeInformation;

  private final MachineModel machineModel;
  private final LogManager logger;

  public ExplicitTransferRelation(Configuration config, LogManager pLogger, MachineModel pMachineModel) throws InvalidConfigurationException {
    config.inject(this);
    machineModel = pMachineModel;
    logger = pLogger;
  }

  @Override
  protected void postProcessing(ExplicitState successor) {
    if (successor != null){
      successor.addToDelta(state);
    }
  }

  @Override
  protected ExplicitState handleMultiEdge(final MultiEdge cfaEdge) throws CPATransferException {
    // we need to keep the old state,
    // because the analysis uses a 'delta' for the now state
    final ExplicitState backup = state;
    for (CFAEdge edge : cfaEdge) {
      state = handleSimpleEdge(edge);
    }
    final ExplicitState successor = state;
    state = backup;
    return successor;
  }

  @Override
  protected ExplicitState handleFunctionCallEdge(FunctionCallEdge callEdge,
      List<? extends IAExpression> arguments, List<? extends AParameterDeclaration> parameters,
      String calledFunctionName) throws UnrecognizedCCodeException {
    ExplicitState newElement = state.clone();

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      assert (parameters.size() == arguments.size());
    }

    // visitor for getting the values of the actual parameters in caller function context
    final ExplicitExpressionValueVisitor visitor = getVisitor();

    // get value of actual parameter in caller function context
    for (int i = 0; i < parameters.size(); i++) {
      Long value;
      IAExpression exp = arguments.get(i);

      if (exp instanceof JExpression) {
        value = ((JExpression) exp).accept(visitor);
      } else if (exp instanceof CExpression) {
        value = visitor.evaluate((CExpression) exp, (CType) parameters.get(i).getType());
      } else {
        throw new AssertionError("unknown expression: " + exp);
      }

      String formalParamName = getScopedVariableName(parameters.get(i).getName(), calledFunctionName);

      if (value == null) {
        newElement.forget(formalParamName);
      } else {
        newElement.assignConstant(formalParamName, value);
      }
    }

    return newElement;
  }

  @Override
  protected ExplicitState handleReturnStatementEdge(AReturnStatementEdge returnEdge, IAExpression expression)
          throws UnrecognizedCCodeException {

    if (expression == null) {
      expression = CNumericTypes.ZERO; // this is the default in C
    }

    String assignedVar = getScopedVariableName(FUNCTION_RETURN_VAR, functionName);

    return handleAssignmentToVariable(assignedVar,
        returnEdge.getSuccessor().getEntryNode().getFunctionDefinition().getType().getReturnType(), // TODO easier way to get type?
        expression, getVisitor());
  }

  /**
   * Handles return from one function to another function.
   * @param functionReturnEdge return edge from a function to its call site
   * @return new abstract state
   */
  @Override
  protected ExplicitState handleFunctionReturnEdge(FunctionReturnEdge functionReturnEdge,
      FunctionSummaryEdge summaryEdge, AFunctionCall exprOnSummary, String callerFunctionName)
    throws UnrecognizedCodeException {

    ExplicitState newElement  = state.clone();

    // expression is an assignment operation, e.g. a = g(b);

    if (exprOnSummary instanceof AFunctionCallAssignmentStatement) {
      AFunctionCallAssignmentStatement assignExp = ((AFunctionCallAssignmentStatement)exprOnSummary);
      IAExpression op1 = assignExp.getLeftHandSide();

      // we expect left hand side of the expression to be a variable

      if ((op1 instanceof AIdExpression) || (op1 instanceof CFieldReference)) {
        String returnVarName = getScopedVariableName(FUNCTION_RETURN_VAR, functionName);

        String assignedVarName = getScopedVariableName(op1, callerFunctionName);

        if (!state.contains(returnVarName)) {
          newElement.forget(assignedVarName);
        } else if (op1 instanceof JIdExpression && ((JIdExpression) op1).getDeclaration() instanceof JFieldDeclaration && !((JFieldDeclaration) ((JIdExpression) op1).getDeclaration()).isStatic()) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
          notScopedFieldValue = state.getValueFor(returnVarName);
        } else {
          newElement.assignConstant(assignedVarName, state.getValueFor(returnVarName));
        }
      }

      // a* = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof APointerExpression) {
      }

      // a[x] = b(); TODO: for now, nothing is done here, but cloning the current element
      else if (op1 instanceof CArraySubscriptExpression) {
      }

      else {
        throw new UnrecognizedCodeException("on function return", summaryEdge, op1);
      }
    }

    newElement.dropFrame(functionName);
    return newElement;
  }


  @Override
  protected ExplicitState handleAssumption(AssumeEdge cfaEdge, IAExpression expression, boolean truthValue)
    throws UnrecognizedCCodeException {
    // convert an expression like [a + 753 != 951] to [a != 951 - 753]
    expression = optimizeAssumeForEvaluation(expression);

    // get the value of the expression (either true[1L], false[0L], or unknown[null])
    Long value = getExpressionValue(expression, CNumericTypes.INT); // TODO which type? TODO special java-type?

    // value is null, try to derive further information
    if (value == null) {

      ExplicitState element = state.clone();
      AssigningValueVisitor avv = new AssigningValueVisitor(element, truthValue);

      if (expression instanceof JExpression && ! (expression instanceof CExpression)) {

        ((JExpression) expression).accept(avv);

        if (avv.hasMissingFieldAccessInformation() || avv.hasMissingEnumComparisonInformation()) {
          assert missingInformationRightJExpression != null;
          missingAssumeInformation = true;
        }

      } else {
        ((CExpression)expression).accept(avv);
      }
      return element;

    } else if ((truthValue && value == 1L) || (!truthValue && value == 0L)) {
      // we do not know more than before, and the assumption is fulfilled,
      // so return the old state
      return state;

    } else {
      // assumption not fulfilled
      return null;
    }
  }


  @Override
  protected ExplicitState handleDeclarationEdge(ADeclarationEdge declarationEdge, IADeclaration declaration)
    throws UnrecognizedCCodeException {

    if (!(declaration instanceof AVariableDeclaration)
        || (declaration.getType() instanceof JType && !(declaration.getType() instanceof JSimpleType))) {
      // nothing interesting to see here, please move along
      return state;
    }

    ExplicitState newElement = state.clone();
    AVariableDeclaration decl = (AVariableDeclaration)declaration;

    // get the variable name in the declarator
    String varName = decl.getName();

    Long initialValue = null;

    // handle global variables
    if (decl.isGlobal()) {
      // if this is a global variable, add to the list of global variables
      globalVariables.add(varName);

      if (decl instanceof JFieldDeclaration && !((JFieldDeclaration)decl).isStatic()) {
        missingFieldVariableObject = true;
        javaNonStaticVariables.add(varName);
      }

      // global variables without initializer are set to 0 in C
      initialValue = 0L;
    }

    // get initial value
    IAInitializer init = decl.getInitializer();

    if (init instanceof AInitializerExpression) {
      IAExpression exp = ((AInitializerExpression)init).getExpression();
      initialValue = getExpressionValue(exp, decl.getType());
    }

    // assign initial value if necessary
      String scopedVarName = decl.isGlobal() ? varName : functionName + "::" + varName;

      boolean complexType = decl.getType() instanceof JClassOrInterfaceType || decl.getType() instanceof JArrayType;


    if (!complexType  && (missingInformationRightJExpression != null || initialValue != null)) {
      if (missingFieldVariableObject) {
        fieldNameAndInitialValue = Pair.of(varName, initialValue);
      } else if (missingInformationRightJExpression == null) {
        newElement.assignConstant(scopedVarName, initialValue);
      } else {
        missingInformationLeftJVariable = scopedVarName;
      }
    } else {

      // If variable not tracked, its Object is irrelevant
      missingFieldVariableObject = false;
      newElement.forget(scopedVarName);

    }

    return newElement;
  }


  @Override
  protected ExplicitState handleStatementEdge(AStatementEdge cfaEdge, IAStatement expression)
    throws UnrecognizedCodeException {

    if (expression instanceof CFunctionCall) {
      CExpression fn = ((CFunctionCall)expression).getFunctionCallExpression().getFunctionNameExpression();
      if (fn instanceof CIdExpression) {
        String func = ((CIdExpression)fn).getName();
        if (UNSUPPORTED_FUNCTIONS.containsKey(func)) {
          throw new UnsupportedCCodeException(UNSUPPORTED_FUNCTIONS.get(func), cfaEdge, fn);
        }
      }
    }

    // expression is a binary operation, e.g. a = b;

    if (expression instanceof IAssignment) {
      return handleAssignment((IAssignment)expression, cfaEdge);

    // external function call - do nothing
    } else if (expression instanceof AFunctionCallStatement) {

    // there is such a case
    } else if (expression instanceof AExpressionStatement) {

    } else {
      throw new UnrecognizedCodeException("Unknown statement", cfaEdge, expression);
    }

    return state;
  }


  private ExplicitState handleAssignment(IAssignment assignExpression, CFAEdge cfaEdge)
    throws UnrecognizedCodeException {
    IAExpression op1    = assignExpression.getLeftHandSide();
    IARightHandSide op2 = assignExpression.getRightHandSide();


    if (op1 instanceof AIdExpression) {
      // a = ...

        if (op1 instanceof JIdExpression && ((JIdExpression) op1).getDeclaration() instanceof JFieldDeclaration && !((JFieldDeclaration) ((JIdExpression) op1).getDeclaration()).isStatic()) {
          missingScopedFieldName = true;
          notScopedField = (JIdExpression) op1;
        }
        return handleAssignmentToVariable(op1, op1.getExpressionType(), op2, getVisitor());

    } else if (op1 instanceof APointerExpression) {
      // *a = ...

      op1 = ((APointerExpression)op1).getOperand();

      // Cil produces code like
      // *((int*)__cil_tmp5) = 1;
      // so remove cast
      if (op1 instanceof CCastExpression) {
        op1 = ((CCastExpression)op1).getOperand();
      }


      if (op1 instanceof AIdExpression) {
        missingInformationLeftPointer = ((AIdExpression)op1).getName();
        missingInformationLeftPointerType = ((AIdExpression)op1).getExpressionType();
        missingInformationRightExpression = op2;
      }
    }

    else if (op1 instanceof CFieldReference) {
      // a->b = ...
      return handleAssignmentToVariable(op1, op1.getExpressionType(), op2, getVisitor());
    }

    // TODO assignment to array cell
    else if (op1 instanceof CArraySubscriptExpression || op1 instanceof AArraySubscriptExpression) {
      // array cell
    } else {
      throw new UnrecognizedCodeException("left operand of assignment has to be a variable", cfaEdge, op1);
    }

    return state; // the default return-value is the old state
  }

  private ExplicitState handleAssignmentToVariable(
      IAExpression lParam, final Type lType, IARightHandSide exp, ExplicitExpressionValueVisitor visitor)
      throws UnrecognizedCCodeException {
    String assignedVar = getScopedVariableName(lParam, functionName);
    return handleAssignmentToVariable(assignedVar, lType, exp, visitor);
  }

  /** This method analyses the expression with the visitor and assigns the value to lParam.
   * The method returns a new state, that contains (a copy of) the old state and the new assignment. */
  private ExplicitState handleAssignmentToVariable(
      String assignedVar, final Type lType, IARightHandSide exp, ExplicitExpressionValueVisitor visitor)
      throws UnrecognizedCCodeException {

    Long value;
    if (exp instanceof JRightHandSide) {
       value = ((JRightHandSide) exp).accept(visitor);
    } else if (exp instanceof CRightHandSide) {
       value = visitor.evaluate((CRightHandSide) exp, (CType) lType);
    } else {
      throw new AssertionError("unknown righthandside-expression: " + exp);
    }

    if (visitor.hasMissingPointer()) {
      missingInformationRightExpression = exp;
      assert value == null;
    }

    // here we clone the state, because we get new information or must forget it.
    ExplicitState newElement = state.clone();

    if (visitor.hasMissingFieldAccessInformation() || visitor.hasMissingEnumComparisonInformation()) {
      // This may happen if an object of class is created which could not be parsed,
      // In  such a case, forget about it
      if (value != null) {
        newElement.forget(assignedVar);
        return newElement;
      } else {
        missingInformationRightJExpression = (JRightHandSide) exp;
        if (!missingScopedFieldName) {
          missingInformationLeftJVariable = assignedVar;
        }
      }
    }

    if (missingScopedFieldName) {
      notScopedFieldValue = value;
    } else {
      if (value == null) {
        // Don't erase it when there if it has yet to be evaluated
        if (missingInformationRightJExpression == null) {
          // TODO HasToBeErased Later
         newElement.forget(assignedVar);
        }
      } else {
        newElement.assignConstant(assignedVar, value);
      }

    }
    return newElement;
  }

  /**
   * Visitor that derives further information from an assume edge
   */
  private class AssigningValueVisitor extends ExplicitExpressionValueVisitor {

    private ExplicitState assignableState;
    protected boolean truthValue = false;

    public AssigningValueVisitor(ExplicitState assignableState, boolean truthValue) {
      super(state, functionName, machineModel, logger, edge);
      this.assignableState = assignableState;
      this.truthValue = truthValue;
    }

    private IAExpression unwrap(IAExpression expression) {
      // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

      if (expression instanceof AUnaryExpression) {
        AUnaryExpression exp = (AUnaryExpression)expression;
        if (exp.getOperator() == UnaryOperator.NOT) { // TODO why only C-UnaryOperator?
          expression = exp.getOperand();
          truthValue = !truthValue;

          expression = unwrap(expression);
        }
      }

      if (expression instanceof CCastExpression) {
        CCastExpression exp = (CCastExpression)expression;
        expression = exp.getOperand();

        expression = unwrap(expression);
      }

      return expression;
    }

    @Override
    public Long visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      BinaryOperator binaryOperator   = pE.getOperator();

      CExpression lVarInBinaryExp  = pE.getOperand1();

      lVarInBinaryExp = (CExpression) unwrap(lVarInBinaryExp);

      CExpression rVarInBinaryExp  = pE.getOperand2();

      Long leftValue                  = lVarInBinaryExp.accept(this);
      Long rightValue                 = rVarInBinaryExp.accept(this);

      if ((binaryOperator == BinaryOperator.EQUALS && truthValue) || (binaryOperator == BinaryOperator.NOT_EQUALS && !truthValue)) {
        if (leftValue == null &&  rightValue != null && isAssignable(lVarInBinaryExp)) {
          String leftVariableName = getScopedVariableName(lVarInBinaryExp, functionName);
          assignableState.assignConstant(leftVariableName, rightValue);
        }

        else if (rightValue == null && leftValue != null && isAssignable(rVarInBinaryExp)) {
          String rightVariableName = getScopedVariableName(rVarInBinaryExp, functionName);
          assignableState.assignConstant(rightVariableName, leftValue);
        }
      }

      if (initAssumptionVars) {
        // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
        // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
        if ((binaryOperator == BinaryOperator.NOT_EQUALS && truthValue)
            || (binaryOperator == BinaryOperator.EQUALS && !truthValue)) {
          if (leftValue == null && rightValue == 0L && isAssignable(lVarInBinaryExp)) {
            String leftVariableName = getScopedVariableName(lVarInBinaryExp, functionName);
            assignableState.assignConstant(leftVariableName, 1L);
          }

          else if (rightValue == null && leftValue == 0L && isAssignable(rVarInBinaryExp)) {
            String rightVariableName = getScopedVariableName(rVarInBinaryExp, functionName);
            assignableState.assignConstant(rightVariableName, 1L);
          }
        }
      }
      return super.visit(pE);
    }


    @Override
    public Long visit(JBinaryExpression pE) throws UnrecognizedCCodeException {
      JBinaryExpression.BinaryOperator binaryOperator   = pE.getOperator();

      IAExpression lVarInBinaryExp  = pE.getOperand1();

      lVarInBinaryExp = unwrap(lVarInBinaryExp);

      IAExpression rVarInBinaryExp  = pE.getOperand2();

      Long leftValue                  = ((JExpression) lVarInBinaryExp).accept(this);
      Long rightValue                 = ((JExpression) rVarInBinaryExp).accept(this);

      if ((binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && truthValue) || (binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && !truthValue)) {
        if (leftValue == null &&  rightValue != null && isAssignable(lVarInBinaryExp)) {

          String leftVariableName = getScopedVariableName(lVarInBinaryExp, functionName);
          assignableState.assignConstant(leftVariableName, rightValue);
        } else if (rightValue == null && leftValue != null && isAssignable(rVarInBinaryExp)) {
          String rightVariableName = getScopedVariableName(rVarInBinaryExp, functionName);
          assignableState.assignConstant(rightVariableName, leftValue);

        }
      }

      if (initAssumptionVars) {
        // x is unknown, a binaryOperation (x!=0), true-branch: set x=1L
        // x is unknown, a binaryOperation (x==0), false-branch: set x=1L
        if ((binaryOperator == JBinaryExpression.BinaryOperator.NOT_EQUALS && truthValue)
            || (binaryOperator == JBinaryExpression.BinaryOperator.EQUALS && !truthValue)) {
          if (leftValue == null && rightValue == 0L && isAssignable(lVarInBinaryExp)) {
            String leftVariableName = getScopedVariableName(lVarInBinaryExp, functionName);
            assignableState.assignConstant(leftVariableName, 1L);

          }

          else if (rightValue == null && leftValue == 0L && isAssignable(rVarInBinaryExp)) {
            String rightVariableName = getScopedVariableName(rVarInBinaryExp, functionName);
            assignableState.assignConstant(rightVariableName, 1L);
          }
        }
      }
      return super.visit(pE);
    }


    private boolean isAssignable(IAExpression expression) {

      if (expression instanceof CIdExpression || expression instanceof CFieldReference) {
        return true;
      }

      boolean result = false;

      if (expression instanceof JIdExpression) {

        JSimpleDeclaration decl = ((JIdExpression) expression).getDeclaration();

        if (decl == null) {
          result = false;
        } else if (decl instanceof JFieldDeclaration) {
          result = ((JFieldDeclaration) decl).isStatic();
        } else {
          result = true;
        }
      }

      return result;
    }
  }

  private class PointerExpressionValueVisitor extends ExplicitExpressionValueVisitor {
    private final PointerState pointerState;

    public PointerExpressionValueVisitor(PointerState pPointerState) {
      super(state, functionName, machineModel, logger, edge);
      pointerState = pPointerState;
    }

    @Override
    public Long visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {
        return super.visit(unaryExpression);
    }

  @Override
  public Long visit(CPointerExpression pointerExpression) throws UnrecognizedCCodeException {

    // Cil produces code like
    // __cil_tmp8 = *((int *)__cil_tmp7);
    // so remove cast
    CExpression operand = pointerExpression.getOperand();
    if (operand instanceof CCastExpression) {
      operand = ((CCastExpression)operand).getOperand();
    }

    if (operand instanceof CIdExpression) {
      String rightVar = derefPointerToVariable(pointerState, ((CIdExpression)operand).getName());
      if (rightVar != null) {
        rightVar = getScopedVariableName(rightVar, functionName);

        if (state.contains(rightVar)) {
          return state.getValueFor(rightVar);
        }
      }
    } else {
      throw new UnrecognizedCCodeException("Pointer dereference of something that is not a variable", edge, pointerExpression);
    }

    return null;
  }
}

  private class  FieldAccessExpressionValueVisitor extends ExplicitExpressionValueVisitor {
    private final RTTState jortState;

    public FieldAccessExpressionValueVisitor(RTTState pJortState) {
      super(state, functionName, machineModel, logger, edge);
      jortState = pJortState;
    }

    @Override
    public Long visit(JBinaryExpression binaryExpression) throws UnrecognizedCCodeException {

      if ((binaryExpression.getOperator() == JBinaryExpression.BinaryOperator.EQUALS
          || binaryExpression.getOperator() == JBinaryExpression.BinaryOperator.NOT_EQUALS)
          && (binaryExpression.getOperand1() instanceof JEnumConstantExpression
              ||  binaryExpression.getOperand2() instanceof JEnumConstantExpression)) {
        return handleEnumComparison(
            binaryExpression.getOperand1(),
            binaryExpression.getOperand2(), binaryExpression.getOperator());
      }

      return super.visit(binaryExpression);
    }

    private Long handleEnumComparison(JExpression operand1, JExpression operand2, JBinaryExpression.BinaryOperator operator) throws UnrecognizedCCodeException {

      String value1;
      String value2;

      if (operand1 instanceof JEnumConstantExpression) {
        value1 = ((JEnumConstantExpression) operand1).getConstantName();
      } else if (operand1 instanceof JIdExpression) {
        String scopedVarName = handleIdExpression((JIdExpression) operand1);

        if (jortState.contains(scopedVarName)) {
          String uniqueObject = jortState.getUniqueObjectFor(scopedVarName);

          if (jortState.getConstantsMap().containsValue(uniqueObject)) {
            value1 = jortState.getRunTimeClassOfUniqueObject(uniqueObject);
          } else {
            return null;
          }
        } else {
          return null;
        }
      } else {
        return null;
      }


      if (operand2 instanceof JEnumConstantExpression) {
        value2 = ((JEnumConstantExpression) operand2).getConstantName();
      } else if (operand1 instanceof JIdExpression) {
        String scopedVarName = handleIdExpression((JIdExpression) operand2);

        if (jortState.contains(scopedVarName)) {
          String uniqueObject = jortState.getUniqueObjectFor(scopedVarName);

          if (jortState.getConstantsMap().containsValue(uniqueObject)) {
            value2 = jortState.getRunTimeClassOfUniqueObject(uniqueObject);
          } else {
            return null;
          }
        } else {
          return null;
        }
      } else {
        return null;
      }

      boolean result = value1.equals(value2);

      switch (operator) {
      case EQUALS:   break;
      case NOT_EQUALS: result = !result;
      }

      return  result ? 1L : 0L;
    }

    private String handleIdExpression(JIdExpression expr) {

      JSimpleDeclaration decl = expr.getDeclaration();

      if (decl == null) {
        return null;
      }

      String objectScope = getObjectScope(jortState, functionName, expr);

      return getRTTScopedVariableName(decl, functionName, objectScope);

    }

    @Override
    public Long visit(JIdExpression idExp) throws UnrecognizedCCodeException {

      String varName = handleIdExpression(idExp);

      if (state.contains(varName)) {
        return state.getValueFor(varName);
      } else {
        return null;
      }
    }
  }


  private Long getExpressionValue(final IAExpression expression, final Type type)
      throws UnrecognizedCCodeException {

    final ExplicitExpressionValueVisitor evv = getVisitor();

    if (expression instanceof JRightHandSide) {
      final Long value = ((JRightHandSide) expression).accept(evv);
      if (evv.hasMissingFieldAccessInformation() || evv.hasMissingEnumComparisonInformation()) {
        missingInformationRightJExpression = (JRightHandSide) expression;
        return null;
      } else {
        return value;
      }

    } else if (expression instanceof CRightHandSide) {
      return evv.evaluate((CRightHandSide) expression, (CType) type);

    } else {
      throw new AssertionError("unhandled righthandside-expression: " + expression);
    }
  }

  public String getScopedVariableName(String variableName, String functionName) {

    if (globalVariables.contains(variableName)) {
      return variableName;
    }

    return functionName + "::" + variableName;
  }

  public String getScopedVariableName(IAExpression variableName, String functionName) {

    if (isGlobal(variableName)) {
      return variableName.toASTString();
    }

    return functionName + "::" + variableName.toASTString();
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState element, List<AbstractState> elements, CFAEdge cfaEdge, Precision precision)
    throws UnrecognizedCCodeException {
    assert element instanceof ExplicitState;

    super.setInfo(element, precision, cfaEdge);

    Collection<? extends AbstractState> retVal = null;

    for (AbstractState ae : elements) {
      if (ae instanceof PointerState) {
        retVal = strengthen((PointerState)ae);
        break;
      } else if (ae instanceof RTTState) {
        retVal =  strengthen((RTTState)ae);
        break;
      }
    }

    super.resetInfo();

    return retVal;
  }

  private Collection<? extends AbstractState> strengthen(RTTState rttState)
      throws UnrecognizedCCodeException {

    ExplicitState newElement = state.clone();

    if (missingFieldVariableObject) {
      newElement.assignConstant(getRTTScopedVariableName(
          fieldNameAndInitialValue.getFirst(),
          rttState.getKeywordThisUniqueObject()),
          fieldNameAndInitialValue.getSecond());

      missingFieldVariableObject = false;
      fieldNameAndInitialValue = null;
      return Collections.singleton(newElement);

    } else if (missingScopedFieldName) {

      newElement = handleNotScopedVariable(rttState, newElement);
      missingScopedFieldName = false;
      notScopedField = null;
      notScopedFieldValue = null;
      missingInformationRightJExpression = null;

      if (newElement != null) {
      return Collections.singleton(newElement);
      } else {
        return null;
      }
    } else if (missingAssumeInformation && missingInformationRightJExpression != null) {
      Long value = handleMissingInformationRightJExpression(rttState);

      missingAssumeInformation = false;
      missingInformationRightJExpression = null;

      if (value == null) {
        return null;
      } else if ((((AssumeEdge) edge).getTruthAssumption() && value == 1L)
          || (!((AssumeEdge) edge).getTruthAssumption() && value == 0L)) {
        return Collections.singleton(newElement);
      } else {
        return new HashSet<>();
      }
    } else if (missingInformationRightJExpression != null) {

      Long value = handleMissingInformationRightJExpression(rttState);

      if (value != null) {
        newElement.assignConstant(missingInformationLeftJVariable, value);
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        return Collections.singleton(newElement);
      } else {
        missingInformationRightJExpression = null;
        missingInformationLeftJVariable = null;
        if (missingInformationLeftJVariable != null) { // TODO why check this???
          newElement.forget(missingInformationLeftJVariable);
        }
        return Collections.singleton(newElement);
      }
    }
    return null;
  }

  private String getRTTScopedVariableName(String fieldName, String uniqueObject) {
    return  uniqueObject + "::"+ fieldName;
  }

  private Long handleMissingInformationRightJExpression(RTTState pJortState)
      throws UnrecognizedCCodeException {
    return missingInformationRightJExpression.accept(
        new FieldAccessExpressionValueVisitor(pJortState));
  }

  private ExplicitState handleNotScopedVariable(RTTState rttState, ExplicitState newElement) throws UnrecognizedCCodeException {

   String objectScope = getObjectScope(rttState, functionName, notScopedField);

   if (objectScope != null) {

     String scopedFieldName = getRTTScopedVariableName(notScopedField.getName(), objectScope);

     Long value = notScopedFieldValue;
     if (missingInformationRightJExpression != null) {
       value = handleMissingInformationRightJExpression(rttState);
     }

     if (value != null) {
       newElement.assignConstant(scopedFieldName, value);
       return newElement;
     } else {
       newElement.forget(scopedFieldName);
       return newElement;
     }
   } else {
     return null;
   }


  }

  private String getObjectScope(RTTState rttState, String methodName,
      JIdExpression notScopedField) {

    // Could not resolve var
    if (notScopedField.getDeclaration() == null) {
      return null;
    }

    if (notScopedField instanceof JFieldAccess) {

      JIdExpression qualifier = ((JFieldAccess) notScopedField).getReferencedVariable();

      String qualifierScope = getObjectScope(rttState, methodName, qualifier);

      String scopedFieldName =
          getRTTScopedVariableName(qualifier.getDeclaration(), methodName, qualifierScope);

      if (rttState.contains(scopedFieldName)) {
        return rttState.getUniqueObjectFor(scopedFieldName);
      } else {
        return null;
      }
    } else {
      if (rttState.contains(RTTState.KEYWORD_THIS)) {
        return rttState.getUniqueObjectFor(RTTState.KEYWORD_THIS);
      } else {
        return null;
      }
    }
  }

  private String getRTTScopedVariableName(
      JSimpleDeclaration decl,
      String methodName, String uniqueObject) {

    if (decl == null) { return ""; }

    if (decl instanceof JFieldDeclaration && ((JFieldDeclaration) decl).isStatic()) {
      return decl.getName();
    } else if (decl instanceof JFieldDeclaration) {
      return uniqueObject + "::" + decl.getName();
    } else {
      return methodName + "::" + decl.getName();
    }
  }

  private Collection<? extends AbstractState> strengthen(PointerState pointerElement)
    throws UnrecognizedCCodeException {
    try {
      if (missingInformationRightExpression != null) {
        ExplicitExpressionValueVisitor v = new PointerExpressionValueVisitor(pointerElement);

        if (missingInformationLeftVariable != null) { // TODO always null? there is no write-access.
          ExplicitState newElement = handleAssignmentToVariable(
              missingInformationLeftVariable, missingInformationLeftVariableType,
              missingInformationRightExpression, v);

          return Collections.singleton(newElement);
        } else if (missingInformationLeftPointer != null) {
          String leftVar = derefPointerToVariable(pointerElement, missingInformationLeftPointer);
          if (leftVar != null) {
            leftVar = getScopedVariableName(leftVar, functionName);
            ExplicitState newElement = handleAssignmentToVariable(leftVar,
                missingInformationLeftPointerType, missingInformationRightExpression, v);

            return Collections.singleton(newElement);
          }
        }
      }
      return null;
    }

    finally {
      missingInformationLeftVariable = null;
      missingInformationLeftVariableType = null;
      missingInformationLeftPointer = null;
      missingInformationLeftPointerType = null;
      missingInformationRightExpression = null;
    }
  }

  private String derefPointerToVariable(PointerState pointerElement, String pointer) {
    Pointer p = pointerElement.lookupPointer(pointer);
    if (p != null && p.getNumberOfTargets() == 1) {
      Memory.PointerTarget target = p.getFirstTarget();
      if (target instanceof Memory.Variable) {
        return ((Memory.Variable)target).getVarName();
      } else if (target instanceof Memory.StackArrayCell) {
        return ((Memory.StackArrayCell)target).getVarName();
      }
    }

    return null;
  }

  /**
   * This method converts an expression like [a + 753 != 951] to [a != 951 - 753], to be able to derive addition information easier with the current expression evaluation visitor.
   *
   * @param expression the expression to generalize
   * @return the generalized expression
   */

  private IAExpression optimizeAssumeForEvaluation(IAExpression expression) {
    if (expression instanceof CBinaryExpression) {
      CBinaryExpression binaryExpression = (CBinaryExpression)expression;

      BinaryOperator operator = binaryExpression.getOperator();
      CExpression leftOperand = binaryExpression.getOperand1();
      CExpression riteOperand = binaryExpression.getOperand2();


      if (operator == BinaryOperator.EQUALS || operator == BinaryOperator.NOT_EQUALS) {
        if (leftOperand instanceof CBinaryExpression && riteOperand instanceof CLiteralExpression) {
          CBinaryExpression expr = (CBinaryExpression)leftOperand;

          BinaryOperator operation = expr.getOperator();
          CExpression leftAddend = expr.getOperand1();
          CExpression riteAddend = expr.getOperand2();

          // [(a + 753) != 951] => [a != 951 + 753]

          if (riteAddend instanceof CLiteralExpression && (operation == BinaryOperator.PLUS || operation == BinaryOperator.MINUS)) {
            BinaryOperator newOperation = (operation == BinaryOperator.PLUS) ? BinaryOperator.MINUS : BinaryOperator.PLUS;

            CBinaryExpression sum = new CBinaryExpression(expr.getFileLocation(),
                                                                expr.getExpressionType(),
                                                                riteOperand,
                                                                riteAddend,
                                                                newOperation);

            CBinaryExpression assume = new CBinaryExpression(expression.getFileLocation(),
                                                                   binaryExpression.getExpressionType(),
                                                                   leftAddend,
                                                                   sum,
                                                                   operator);
            return assume;
          }
        }
      }
    } else if (expression instanceof JBinaryExpression) {
      JBinaryExpression binaryExpression = (JBinaryExpression)expression;

      JBinaryExpression.BinaryOperator operator = binaryExpression.getOperator();
      JExpression leftOperand = binaryExpression.getOperand1();
      JExpression riteOperand = binaryExpression.getOperand2();


      if (operator == JBinaryExpression.BinaryOperator.EQUALS || operator == JBinaryExpression.BinaryOperator.NOT_EQUALS) {
        if (leftOperand instanceof JBinaryExpression && riteOperand instanceof JLiteralExpression) {
          JBinaryExpression expr = (JBinaryExpression)leftOperand;

          JBinaryExpression.BinaryOperator operation = expr.getOperator();
          JExpression leftAddend = expr.getOperand1();
          JExpression riteAddend = expr.getOperand2();

          // [(a + 753) != 951] => [a != 951 + 753]

          if (riteAddend instanceof JLiteralExpression && (operation == JBinaryExpression.BinaryOperator.PLUS || operation == JBinaryExpression.BinaryOperator.MINUS)) {
            JBinaryExpression.BinaryOperator newOperation = (operation == JBinaryExpression.BinaryOperator.PLUS) ? JBinaryExpression.BinaryOperator.MINUS : JBinaryExpression.BinaryOperator.PLUS;

            JBinaryExpression sum = new JBinaryExpression(expr.getFileLocation(),
                                                                expr.getExpressionType(),
                                                                riteOperand,
                                                                riteAddend,
                                                                newOperation);

            JBinaryExpression assume = new JBinaryExpression(expression.getFileLocation(),
                                                                   binaryExpression.getExpressionType(),
                                                                   leftAddend,
                                                                   sum,
                                                                   operator);
            return assume;
          }
        }
      }
    }

    return expression;
  }

  /** returns an initialized, empty visitor */
  private ExplicitExpressionValueVisitor getVisitor() {
    return new ExplicitExpressionValueVisitor(state, functionName, machineModel, logger, edge);
  }
}
