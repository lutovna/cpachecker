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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;


public class CalledFunctions {


  private Map<String, Integer> functions; // how many time we called each function
  private Deque<String> callingStack; // stack of function calls
  private String predessorFunctionName; // from cfaEdge
  private boolean predFun = false; // sometimes we need function from predessor

  public CalledFunctions() {
    functions = new HashMap<>();
    callingStack = new ArrayDeque<>();
    predessorFunctionName = "";
  }

  public void addFunctionSafe(String funcName) {

    if (callingStack.isEmpty()) {
      addFunction(predessorFunctionName);
    }

    addFunction(funcName);
  }

  private void addFunction(String funcName) {
    int calls = 0;

    if (functions.containsKey(funcName)) {
      calls = functions.get(funcName) + 1;
    }
    functions.put(funcName, calls);
    callingStack.push(funcName);
  }

  private void removeFunction(String funcName) {
    int calls = functions.get(funcName) - 1;
    callingStack.pop();

    if (calls == -1) {
      functions.remove(funcName);
    } else {
      functions.put(funcName, calls);
    }
  }

  public void popFunction() {
    if (!callingStack.isEmpty()) {
      removeFunction(callingStack.peek());
    }
  }

  public void setInPredFunTrue() {
    predFun = true;
  }

  public void setInPredFunFalse() {
    predFun = false;
  }

  public void setPredessorFunctionName(CFAEdge cfaEdge) {
    predessorFunctionName = cfaEdge.getPredecessor().getFunctionName();
  }

  /** @return function_name[number_of_calls] **/
  public String getQualifiedFunctionName() {

    if (callingStack.isEmpty() || (predFun && !functions.containsKey(predessorFunctionName))) {
      // TODO: rewrite this!
      addFunction(predessorFunctionName);
      return getQualifiedFunctionName();
    }

    if (predFun) {
      return predessorFunctionName + "[" + functions.get(predessorFunctionName) + "]";
    }
    return callingStack.peek() + "[" + functions.get(callingStack.peek()) + "]";
  }

  /** @return function_name[number_of_calls]::variable_name **/
  public String getQualifiedVariableName(String varName) {
    return getQualifiedFunctionName() + "::" + varName;
  }

  /**
   * @return if decl isn't Global function_name[number_of_calls]::variable_name; else qualifiedName
   **/
  public String getQualifiedVariableNameFromDeclaration(CSimpleDeclaration decl) {

    if (decl instanceof CVariableDeclaration && ((CVariableDeclaration) decl).isGlobal()) {
      return decl.getQualifiedName();
    }

    return getQualifiedVariableName(decl.getName());
  }
}
