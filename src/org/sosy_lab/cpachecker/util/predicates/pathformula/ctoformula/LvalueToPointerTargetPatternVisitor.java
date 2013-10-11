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
package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.Variable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget.PointerTargetPattern;


public class LvalueToPointerTargetPatternVisitor
extends DefaultCExpressionVisitor<PointerTargetPattern, UnrecognizedCCodeException> {

  public LvalueToPointerTargetPatternVisitor(final CToFormulaWithUFConverter conv,
                                             final CFAEdge cfaEdge,
                                             final PointerTargetSetBuilder pts) {
    this.conv = conv;
    this.cfaEdge = cfaEdge;
    this.pts = pts;
  }

  @Override
  protected PointerTargetPattern visitDefault(CExpression e) throws UnrecognizedCCodeException {
    throw new UnrecognizedCCodeException("Illegal expression in lhs", cfaEdge, e);
  }

  @Override
  public PointerTargetPattern visit(final CArraySubscriptExpression e) throws UnrecognizedCCodeException {
    final CExpression arrayExpression = e.getArrayExpression();
    final PointerTargetPattern result = arrayExpression.accept(this);
    if (result != null) {
      CType containerType = PointerTargetSet.simplifyType(arrayExpression.getExpressionType());
      if (containerType instanceof CArrayType || containerType instanceof CPointerType) {
        final CType elementType;
        if (containerType instanceof CPointerType) {
          elementType = ((CPointerType) containerType).getType();
          containerType = new CArrayType(containerType.isConst(),
                                         containerType.isVolatile(),
                                         elementType,
                                         null);
        } else {
          elementType = ((CArrayType) containerType).getType();
        }
        result.shift(containerType);
        final Integer index = e.getSubscriptExpression().accept(pts.getEvaluatingVisitor());
        if (index != null) {
          result.setProperOffset(index * pts.getSize(elementType));
        }
        return result;
      } else {
        throw new UnrecognizedCCodeException("Array expression has incompatible type", cfaEdge, e);
      }
    } else {
      throw new UnrecognizedCCodeException("Subscripting pure variable", cfaEdge, e);
    }
  }

  @Override
  public PointerTargetPattern visit(final CBinaryExpression e) throws UnrecognizedCCodeException {
    final CExpression operand1 = e.getOperand1();
    final CExpression operand2 = e.getOperand2();

    switch (e.getOperator()) {
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case DIVIDE:
    case EQUALS:
    case GREATER_EQUAL:
    case GREATER_THAN:
    case LESS_EQUAL:
    case LESS_THAN:
    case MODULO:
    case MULTIPLY:
    case NOT_EQUALS:
    case SHIFT_LEFT:
    case SHIFT_RIGHT:
      throw new UnrecognizedCCodeException("Illegal binary operator", cfaEdge, e);

    case MINUS: {
      final PointerTargetPattern result = operand1.accept(this);
      if (result != null) {
        final Integer offset = operand2.accept(pts.getEvaluatingVisitor());
        final Integer oldOffset = result.getProperOffset();
        if (offset != null && oldOffset != null && offset < oldOffset) {
          result.setProperOffset(oldOffset - offset);
        } else {
          result.retainBase();
        }
        return result;
      } else {
        throw new UnrecognizedCCodeException("Address arithmetic with pure variable", cfaEdge, e);
      }
    }

    case PLUS:{
      PointerTargetPattern result = operand1.accept(this);
      final Integer offset;
      if (result == null) {
        result = operand2.accept(this);
        offset = operand1.accept(pts.getEvaluatingVisitor());
      } else {
        offset = operand2.accept(pts.getEvaluatingVisitor());
      }
      if (result != null) {
        final Integer remaining = result.getRemainingOffset(pts);
        if (offset != null && remaining != null && offset < remaining) {
          assert result.getProperOffset() != null : "Unexpected nondet proper offset";
          result.setProperOffset(result.getProperOffset() + offset);
        } else {
          result.retainBase();
        }
        return result;
      } else {
        throw new UnrecognizedCCodeException("Pointer addition", cfaEdge, e);
      }
    }

    default:
      throw new UnrecognizedCCodeException("Unhandled binary operator", cfaEdge, e);
    }
  }

  @Override
  public PointerTargetPattern visit(final CCastExpression e) throws UnrecognizedCCodeException {
    return e.getOperand().accept(this);
  }

  @Override
  public PointerTargetPattern visit(final CFieldReference e) throws UnrecognizedCCodeException {
    assert !e.isPointerDereference() : "CFA should be transformed to eliminate ->s";
    final CExpression ownerExpression = e.getFieldOwner();
    final PointerTargetPattern result = ownerExpression.accept(this);
    if (result != null) {
      final CType containerType = PointerTargetSet.simplifyType(ownerExpression.getExpressionType());
      if (!(containerType instanceof CCompositeType) ||
          ((CCompositeType) containerType).getKind() == ComplexTypeKind.ENUM) {
        result.shift(containerType, pts.getOffset((CCompositeType) containerType, e.getFieldName()));
        return result;
      } else {
        throw new UnrecognizedCCodeException("Field owner expression has incompatible type", cfaEdge, e);
      }
    } else {
      return null;
    }
  }

  @Override
  public PointerTargetPattern visit(final CIdExpression e) throws UnrecognizedCCodeException {
    final Variable variable = conv.scopedIfNecessary(e, null, null);
    if (!pts.isBase(variable.getName())) {
      return null;
    } else {
      return new PointerTargetPattern(e.getName());
    }
  }

  @Override
  public PointerTargetPattern visit(final CUnaryExpression e) throws UnrecognizedCCodeException {
    switch (e.getOperator()) {
    case AMPER:
    case MINUS:
    case NOT:
    case SIZEOF:
    case TILDE:
      throw new UnrecognizedCCodeException("Illegal binary operator", cfaEdge, e);
    case PLUS:
      return e.getOperand().accept(this);
    case STAR:
      final CType type = PointerTargetSet.simplifyType(e.getExpressionType());
      final PointerTargetPattern result = e.getOperand().accept(this);
      if (type instanceof CPointerType) {
        result.clear();
        return result;
      } else if (type instanceof CArrayType) {
        result.shift(type, 0);
        return result;
      } else {
        throw new UnrecognizedCCodeException("Dereferencing non-pointer expression", cfaEdge, e);
      }

      default:
        throw new UnrecognizedCCodeException("Unhandled unary operator", cfaEdge, e);
    }
  }

  private final CToFormulaWithUFConverter conv;
  private final PointerTargetSetBuilder pts;
  private final CFAEdge cfaEdge;
}
