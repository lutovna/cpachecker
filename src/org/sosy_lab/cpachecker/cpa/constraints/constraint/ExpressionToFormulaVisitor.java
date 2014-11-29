/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.constraints.constraint;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.sosy_lab.cpachecker.cfa.ast.ACharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.AIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormula;
import org.sosy_lab.cpachecker.cpa.invariants.formula.InvariantsFormulaManager;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

import com.google.common.base.Optional;

/**
 * Class for transforming expressions to formulas.
 *
 * <p>For each transformation, a new object has to be created. Otherwise, the resulting formulas might not reflect the
 * programs possible concrete states.</p>
 *
 */
public class ExpressionToFormulaVisitor {

  private final String functionName;

  private boolean missingInformation = false;
  private Optional<ValueAnalysisState> valueState;

  public ExpressionToFormulaVisitor(String pFunctionName) {
    functionName = pFunctionName;
    valueState = Optional.absent();
  }

  public ExpressionToFormulaVisitor(String pFunctionName, ValueAnalysisState pValueState) {
    this(pFunctionName);
    valueState = Optional.of(pValueState);
  }

   /* Not needed if strengthening with ValueAnalysis works
  public void addAlias(String pIdentifier) {
    MemoryLocation memLoc = MemoryLocation.valueOf(functionName, pIdentifier, 0);

    Alias oldAlias = identifierAliasses.get(memLoc);
    Alias newAlias;

    if (oldAlias != null) {
      newAlias = Alias.createNextAlias(oldAlias);
    } else {
      newAlias = Alias.createAlias(memLoc.toString());
    }

    identifierAliasses.put(memLoc, newAlias);
  }

  protected Alias getNewAlias(String pIdentifier) {
    MemoryLocation memLoc = MemoryLocation.valueOf(functionName, pIdentifier, 0);

    return identifierAliasses.get(memLoc);
  }*/

  protected InvariantsFormula<Value> negate(InvariantsFormula<Value> pFormula) {
    checkNotNull(pFormula);
    final InvariantsFormulaManager factory = InvariantsFormulaManager.INSTANCE;

    return factory.multiply(getMinusOne(), pFormula);
  }

  private InvariantsFormula<Value> getMinusOne() {
    return InvariantsFormulaManager.INSTANCE.asConstant(createNumericValue(-1L));
  }

  protected Value createNumericValue(long pValue) {
    return new NumericValue(pValue);
  }

  protected Value createNumericValue(BigDecimal pValue) {
    return new NumericValue(pValue);
  }

  protected Value createNumericValue(BigInteger pValue) {
    return new NumericValue(pValue);
  }

  /**
   * Returns whether information was missing while transforming the last expression.
   *
   * <p>This method always resets after one call. So when calling this method after the creation of a formula,
   * it will only return <code>true</code> at the first call, if at all.</p>
   *
   * @return <code>true</code> if information was missing, <code>false</code> otherwise
   */
  public boolean hasMissingInformation() {
    return missingInformation;
  }

  public InvariantsFormula<Value> visit(AIdExpression pIastIdExpression) throws UnrecognizedCodeException {
    if (!valueState.isPresent()) {
      missingInformation = true;
      return null;
    }

    final ValueAnalysisState state = valueState.get();
    final String identifier = pIastIdExpression.getName();
    final MemoryLocation memLoc = MemoryLocation.valueOf(functionName, identifier, 0);

    final Value idValue = state.getValueFor(memLoc);

    if (idValue == null) {
      return null;
    }

    return InvariantsFormulaManager.INSTANCE.asConstant(idValue);
  }

  public InvariantsFormula<Value> visit(AIntegerLiteralExpression pIastIntegerLiteralExpression)
      throws UnrecognizedCodeException {
    BigInteger value = pIastIntegerLiteralExpression.getValue();

    return InvariantsFormulaManager.INSTANCE.asConstant(createNumericValue(value));
  }

  public InvariantsFormula<Value> visit(ACharLiteralExpression pIastCharLiteralExpression)
      throws UnrecognizedCodeException {
    long castValue = (long) pIastCharLiteralExpression.getCharacter();

    return InvariantsFormulaManager.INSTANCE.asConstant(createNumericValue(castValue));
  }

  public InvariantsFormula<Value> visit(AFloatLiteralExpression pIastFloatLiteralExpression)
      throws UnrecognizedCodeException {
    BigDecimal value = pIastFloatLiteralExpression.getValue();

    return InvariantsFormulaManager.INSTANCE.asConstant(createNumericValue(value));
  }

}
