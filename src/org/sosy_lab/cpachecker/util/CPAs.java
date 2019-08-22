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
package org.sosy_lab.cpachecker.util;

import com.google.common.base.Throwables;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.Traverser;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.WrapperCPA;

/**
 * Helper functions to work with CPAs.
 */
public class CPAs {

  private CPAs() { }

  /**
   * Retrieve a specific CPA out of a structure of wrapper and composite CPAs.
   *
   * @param cpa The root of the tree of CPAs where to search.
   * @param cls The type to search for.
   * @return The found CPA, or null if none was found.
   */
  @Nullable
  public static <T extends ConfigurableProgramAnalysis> T retrieveCPA(
      ConfigurableProgramAnalysis cpa, Class<T> cls) {
    if (cls.isInstance(cpa)) {
      return cls.cast(cpa);
    } else if (cpa instanceof WrapperCPA) {
      return ((WrapperCPA)cpa).retrieveWrappedCpa(cls);
    } else {
      return null;
    }
  }

  /**
   * Retrieve an immutable set of specific CPAs out of a collection of various CPAs. If any of the
   * CPAs are wrapper or composite CPAs, these are also searched recursively for the specified
   * CPA-type. Duplicated found elements are stored only once in the returning set.
   *
   * @param pCpas A collection of CPAs.
   * @param cls The type to search for.
   * @return An immutable set containing all CPAs of the specified type.
   */
  public static <T extends ConfigurableProgramAnalysis> ImmutableSet<T> retrieveCPAsOfType(
      Iterable<ConfigurableProgramAnalysis> pCpas, Class<T> cls) {
    return ImmutableSet.copyOf(retrieveCPAsOfType0(pCpas, cls));
  }

  private static <T extends ConfigurableProgramAnalysis> Collection<T> retrieveCPAsOfType0(
      Iterable<ConfigurableProgramAnalysis> pCpas, Class<T> cls) {
    Set<T> set = new HashSet<>();
    for (ConfigurableProgramAnalysis cpa : pCpas) {
      if (cls.isInstance(cpa)) {
        set.add(cls.cast(cpa));
      }
      if (cpa instanceof WrapperCPA) {
        set.addAll(retrieveCPAsOfType0(((WrapperCPA) cpa).getWrappedCPAs(), cls));
      }
    }
    return set;
  }

  /**
   * Retrieve a specific CPA out of a structure of wrapper and composite CPAs.
   *
   * @param cpa The root of the tree of CPAs where to search.
   * @param cls The type to search for.
   * @param callee Used for the message of the exception if needed.
   * @return The found CPA, or InvalidConfigurationException if no matching CPA was found.
   */
  @NonNull
  public static <T extends ConfigurableProgramAnalysis, C> T retrieveCPAOrFail(
      ConfigurableProgramAnalysis cpa, Class<T> cls, Class<C> callee)
      throws InvalidConfigurationException {
    T result = CPAs.retrieveCPA(cpa, cls);
    if (result == null) {
      throw new InvalidConfigurationException(
          callee.getSimpleName() + " needs a " + cls.getSimpleName());
    }
    return result;
  }

  /**
   * Creates an iterable that enumerates all the CPAs contained in a single CPA, including the root
   * CPA itself. The tree of elements is traversed in pre-order.
   */
  public static FluentIterable<ConfigurableProgramAnalysis> asIterable(
      final ConfigurableProgramAnalysis pCpa) {
    return FluentIterable.from(
        Traverser.forTree(
                (ConfigurableProgramAnalysis cpa) ->
                    (cpa instanceof WrapperCPA)
                        ? ((WrapperCPA) cpa).getWrappedCPAs()
                        : ImmutableList.of())
            .depthFirstPreOrder(pCpa));
  }

  /**
   * Close all CPAs (including wrapped CPAs) if they support this.
   * @param cpa A CPA (possibly a WrapperCPA).
   */
  public static void closeCpaIfPossible(ConfigurableProgramAnalysis cpa, LogManager logger) {
    for (ConfigurableProgramAnalysis currentCpa : CPAs.asIterable(cpa)) {
      closeIfPossible(currentCpa, logger);
    }
  }

  /**
   * Call {@link AutoCloseable#close()} on an supplied object if it implements
   * {@link AutoCloseable}. Checked exceptions are logged but not re-thrown.
   * @param obj An object.
   */
  public static void closeIfPossible(Object obj, LogManager logger) {
    if (obj instanceof AutoCloseable) {
      try {
        ((AutoCloseable)obj).close();
      } catch (Exception e) {
        Throwables.throwIfUnchecked(e);
        logger.logUserException(Level.WARNING, e, "Failed to close " + obj.getClass().getSimpleName());
      }
    }
  }
}
