/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.overflow;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysisWithBAM;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.util.ArithmeticOverflowAssumptionBuilder;

/** CPA for detecting overflows in C programs. */
public class OverflowCPA extends AbstractCPA implements ConfigurableProgramAnalysisWithBAM {

  private final CBinaryExpressionBuilder expressionBuilder;
  private final ArithmeticOverflowAssumptionBuilder noOverflowAssumptionBuilder;

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(OverflowCPA.class);
  }

  private OverflowCPA(CFA pCfa, LogManager pLogger, Configuration pConfiguration)
      throws InvalidConfigurationException {
    super("sep", "sep", null);
    expressionBuilder = new CBinaryExpressionBuilder(pCfa.getMachineModel(), pLogger);
    noOverflowAssumptionBuilder =
        new ArithmeticOverflowAssumptionBuilder(pCfa, pLogger, pConfiguration);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new OverflowTransferRelation(noOverflowAssumptionBuilder, expressionBuilder);
  }


  @Override
  public AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return new OverflowState(ImmutableSet.of(), false);
  }
}
