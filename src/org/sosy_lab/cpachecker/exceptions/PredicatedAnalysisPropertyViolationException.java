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
package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

/**
 * Exception if states in predicated analysis are created which violate property to be checked
 */
public class PredicatedAnalysisPropertyViolationException extends CPAException{

  private static final long serialVersionUID = 6723698516455641373L;

  private final AbstractState failureElem;

  public PredicatedAnalysisPropertyViolationException(String pMsg, AbstractState failureCause) {
    super(pMsg);
    failureElem = failureCause;
  }

  public PredicatedAnalysisPropertyViolationException(String msg, Throwable cause, AbstractState failureCause) {
    super(msg, cause);
    failureElem = failureCause;
  }

  public AbstractState getFailureCause(){
    return failureElem;
  }

}
