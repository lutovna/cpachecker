/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cpa.symbpredabsCPA;

import java.util.List;

import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.JoinOperator;
import cpa.common.interfaces.PartialOrder;
import cpa.symbpredabs.AbstractFormulaManager;
import exceptions.CPAException;


/**
 * Abstract domain for Symbolic lazy abstraction with summaries.
 *
 * @author Erkan
 */
public class SymbPredAbsAbstractDomain implements AbstractDomain {

  private SymbPredAbsCPA cpa;

  public SymbPredAbsAbstractDomain(SymbPredAbsCPA cpa) {
    this.cpa = cpa;
  }

  private final static class SymbPredAbsBottomElement extends SymbPredAbsAbstractElement {
    @Override
    public String toString() {
      return "<BOTTOM>";
    }
  }
  private final static class SymbPredAbsTopElement extends SymbPredAbsAbstractElement {
    @Override
    public String toString() {
      return "<TOP>";
    }
  }

  private final static class SymbPredAbsJoinOperator implements JoinOperator {
    public AbstractElement join(AbstractElement element1,
                                AbstractElement element2) throws CPAException {
      return top;
    }
  }

  private final class SymbPredAbsPartialOrder implements PartialOrder {
    public boolean satisfiesPartialOrder(AbstractElement element1,
                                         AbstractElement element2) throws CPAException {
      SymbPredAbsAbstractElement e1 = (SymbPredAbsAbstractElement)element1;
      SymbPredAbsAbstractElement e2 = (SymbPredAbsAbstractElement)element2;

      if (e1 == bottom) {
        return true;
      } else if (e2 == top) {
        return true;
      } else if (e2 == bottom) {
        // we should not put this in the reached set
        assert(false);
        return false;
      } else if (e1 == top) {
        return false;
      }

      assert(e1.getAbstraction() != null);
      assert(e2.getAbstraction() != null);

      // if not an abstraction location
      if(!e1.isAbstractionNode()){
        // if abstraction paths are same
        if(e1.getAbstractionPathList().equals(e2.getAbstractionPathList())){

          List<Integer> succList = e1.getPfParents();
          List<Integer> reachedList = e2.getPfParents();

          assert(succList.size() == 1);
          // and if pfParents of e1 is included in e2's pfParents
          // we don't need to add again, return true
          // this is useful after merging two elements which are not
          // abstraction locations and stop not to add the same
          // element again
          return reachedList.containsAll(succList);
        }
        return false;
      }
      // if abstraction location
      else{
        AbstractFormulaManager amgr = cpa.getAbstractFormulaManager();
        // if e1's predicate abstraction entails e2's pred. abst.
        return amgr.entails(e1.getAbstraction(), e2.getAbstraction());
      }
    }
  }

  private final static SymbPredAbsBottomElement bottom = new SymbPredAbsBottomElement();
  private final static SymbPredAbsTopElement top = new SymbPredAbsTopElement();
  private final static JoinOperator join = new SymbPredAbsJoinOperator();
  private final PartialOrder partial = new SymbPredAbsPartialOrder();

  public AbstractElement getBottomElement() {
    return bottom;
  }

  public JoinOperator getJoinOperator() {
    return join;
  }

  public PartialOrder getPartialOrder() {
    return partial;
  }

  public AbstractElement getTopElement() {
    return top;
  }

  public SymbPredAbsCPA getCPA() {
    return cpa;
  }
}
