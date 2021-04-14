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

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.DelegateAbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

@Options(prefix = "cpa.string")

public class StringCPA extends AbstractCPA {

  @Option(secure = true, name = "stringActivity", description = "active domains")
  private String stringActivity = "111";

  /*
   * @Option( secure = true, name = "merge", toUppercase = true, values = {"SEP", "JOIN"},
   * description = "which merge operator to use for SignCPA") private String mergeType = "SEP";
   *
   * @Option( secure = true, name = "stop", toUppercase = true, values = {"SEP", "JOIN"},
   * description = "which stop operator to use for SignCPA") private String stopType = "JOIN";
   */

  /*
   * protected StringCPA(AbstractDomain pDomain, TransferRelation pTransfer) { super(pDomain,
   * pTransfer); }
   */

  private final static int numberOfDomains = 3;

  protected StringCPA(Configuration config) throws InvalidConfigurationException {
    super("SEP", "JOIN", DelegateAbstractDomain.<Strings>getInstance(), null);
    config.inject(this);
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition)
      throws InterruptedException {
    return new Strings(activityToBoolean(stringActivity));
    // return new Strings();
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new StringTransferRelation(activityToBoolean(stringActivity));
    // return new StringTransferRelation();
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(StringCPA.class);
  }

  @Override
  public MergeOperator getMergeOperator() {
    return buildMergeOperator("SEP");
  }

  @Override
  public StopOperator getStopOperator() {
    return buildStopOperator("JOIN");
  }

  private boolean[] activityToBoolean(String str) {
    char[] charActivity = str.toCharArray();
    boolean[] activity = new boolean[numberOfDomains];

    int i = 0;
    for (; i < charActivity.length && i < numberOfDomains; i++) {
      activity[i] = (charActivity[i] == '1');
    }
    if (i < numberOfDomains - 1) {
      for (; i < numberOfDomains; i++) {
        activity[i] = false;
      }
    }

    return activity;
  }
}