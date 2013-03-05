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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.NestedTimer;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager.RegionCreator;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment.AllSatResult;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Joiner;

@Options(prefix = "cpa.predicate")
public class PredicateAbstractionManager {

  static class Stats {

    public int numCallsAbstraction = 0;
    public int numSymbolicAbstractions = 0;
    public int numSatCheckAbstractions = 0;
    public int numCallsAbstractionCached = 0;
    public final NestedTimer abstractionEnumTime = new NestedTimer(); // outer: solver time, inner: bdd time
    public final Timer abstractionSolveTime = new Timer(); // only the time for solving, not for model enumeration

    public long allSatCount = 0;
    public int maxAllSatCount = 0;

    public int numPathFormulaCoverageChecks = 0;
    public int numEqualPathFormulae = 0;
    public int numSyntacticEntailedPathFormulae = 0;
    public int numSemanticEntailedPathFormulae = 0;
  }

  final Stats stats;

  private final LogManager logger;
  private final FormulaManagerView fmgr;
  private final AbstractionManager amgr;
  private final PathFormulaManager pfmgr;
  private final Solver solver;

  @Option(name = "abstraction.cartesian",
      description = "whether to use Boolean (false) or Cartesian (true) abstraction")
  private boolean cartesianAbstraction = false;

  @Option(name = "abstraction.dumpHardQueries",
      description = "dump the abstraction formulas if they took to long")
  private boolean dumpHardAbstractions = false;

  @Option(name = "abs.useCache", description = "use caching of abstractions")
  private boolean useCache = true;

  private boolean warnedOfCartesianAbstraction = false;

  private final Map<Pair<BooleanFormula, Collection<AbstractionPredicate>>, AbstractionFormula> abstractionCache;

  // Cache for satisfiability queries: if formula is contained, it is unsat
  private final Set<BooleanFormula> unsatisfiabilityCache;

  //cache for cartesian abstraction queries. For each predicate, the values
  // are -1: predicate is false, 0: predicate is don't care,
  // 1: predicate is true
  private final Map<Pair<BooleanFormula, AbstractionPredicate>, Byte> cartesianAbstractionCache;

  private final BooleanFormulaManagerView bfmgr;

  public PredicateAbstractionManager(
      AbstractionManager pAmgr,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      Solver pSolver,
      Configuration config,
      LogManager pLogger) throws InvalidConfigurationException {

    config.inject(this, PredicateAbstractionManager.class);

    stats = new Stats();
    logger = pLogger;
    fmgr = pFmgr;
    bfmgr = fmgr.getBooleanFormulaManager();
    amgr = pAmgr;
    pfmgr = pPfmgr;
    solver = pSolver;

    if (useCache) {
      abstractionCache = new HashMap<>();
      unsatisfiabilityCache = new HashSet<>();
    } else {
      abstractionCache = null;
      unsatisfiabilityCache = null;
    }
    if (useCache && cartesianAbstraction) {
      cartesianAbstractionCache = new HashMap<>();
    } else {
      cartesianAbstractionCache = null;
    }
  }

  /**
   * Abstract post operation.
   */
  public AbstractionFormula buildAbstraction(
      AbstractionFormula abstractionFormula, PathFormula pathFormula,
      Collection<AbstractionPredicate> predicates) {

    stats.numCallsAbstraction++;

    if (predicates.isEmpty()) {
      logger.log(Level.FINEST, "Abstraction", stats.numCallsAbstraction, "with empty precision is true");
      stats.numSymbolicAbstractions++;
      return makeTrueAbstractionFormula(pathFormula);
    }

    logger.log(Level.FINEST, "Computing abstraction", stats.numCallsAbstraction, "with", predicates.size(), "predicates");
    logger.log(Level.ALL, "Old abstraction:", abstractionFormula);
    logger.log(Level.ALL, "Path formula:", pathFormula);
    logger.log(Level.ALL, "Predicates:", predicates);

    BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(pathFormula.getFormula());
    BooleanFormula f = bfmgr.and(absFormula, symbFormula);

    // caching
    Pair<BooleanFormula, Collection<AbstractionPredicate>> absKey = null;
    if (useCache) {
      absKey = Pair.of(f, predicates);
      AbstractionFormula result = abstractionCache.get(absKey);

      if (result != null) {
        // create new abstraction object to have a unique abstraction id

        // instantiate the formula with the current indices
        BooleanFormula stateFormula = result.asFormula();
        BooleanFormula instantiatedFormula = fmgr.instantiate(stateFormula, pathFormula.getSsa());

        result = new AbstractionFormula(fmgr, result.asRegion(), stateFormula, instantiatedFormula, pathFormula);
        logger.log(Level.FINEST, "Abstraction", stats.numCallsAbstraction, "was cached");
        logger.log(Level.ALL, "Abstraction result is", result);
        stats.numCallsAbstractionCached++;
        return result;
      }

      boolean unsatisfiable = unsatisfiabilityCache.contains(symbFormula)
                            || unsatisfiabilityCache.contains(f);
      if (unsatisfiable) {
        // block is infeasible
        logger.log(Level.FINEST, "Block feasibility of abstraction", stats.numCallsAbstraction, "was cached and is false.");
        stats.numCallsAbstractionCached++;
        return new AbstractionFormula(fmgr, amgr.getRegionCreator().makeFalse(),
            bfmgr.makeBoolean(false), bfmgr.makeBoolean(false), pathFormula);
      }
    }

    Region abs;
    if (cartesianAbstraction) {
      abs = buildCartesianAbstraction(f, pathFormula.getSsa(), predicates);
    } else {
      abs = buildBooleanAbstraction(f, pathFormula.getSsa(), predicates);
    }

    AbstractionFormula result = makeAbstractionFormula(abs, pathFormula.getSsa(), pathFormula);

    if (useCache) {
      abstractionCache.put(absKey, result);

      if (result.isFalse()) {
        unsatisfiabilityCache.add(f);
      }
    }

    long abstractionTime = stats.abstractionSolveTime.getLengthOfLastInterval()
        + stats.abstractionEnumTime.getLengthOfLastOuterInterval();
    logger.log(Level.FINEST, "Computing abstraction took", abstractionTime, "ms");
    logger.log(Level.ALL, "Abstraction result is", result);

    if (dumpHardAbstractions && abstractionTime > 10000) {
      // we want to dump "hard" problems...
      File dumpFile;

      dumpFile = fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "input", 0);
      fmgr.dumpFormulaToFile(f, dumpFile);

      dumpFile = fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "predicates", 0);
      try (Writer w = Files.openOutputFile(dumpFile.toPath())) {
        Joiner.on('\n').appendTo(w, predicates);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Failed to wrote predicates to file");
      }

      dumpFile = fmgr.formatFormulaOutputFile("abstraction", stats.numCallsAbstraction, "result", 0);
      fmgr.dumpFormulaToFile(result.asInstantiatedFormula(), dumpFile);
    }

    return result;
  }

  private Region buildCartesianAbstraction(final BooleanFormula f, final SSAMap ssa,
      Collection<AbstractionPredicate> predicates) {
    final RegionCreator rmgr = amgr.getRegionCreator();

    try (ProverEnvironment thmProver = solver.newProverEnvironment()) {
      thmProver.push(f);

      stats.abstractionSolveTime.start();
      boolean feasibility = !thmProver.isUnsat();
      stats.abstractionSolveTime.stop();

      if (!feasibility) {
        // abstract post leads to false, we can return immediately
        return rmgr.makeFalse();
      }

      if (!warnedOfCartesianAbstraction && !fmgr.isPurelyConjunctive(f)) {
        logger
            .log(
                Level.WARNING,
                "Using cartesian abstraction when formulas contain disjunctions may be imprecise. This might lead to failing refinements.");
        warnedOfCartesianAbstraction = true;
      }

      stats.abstractionEnumTime.startOuter();
      try {
        Region absbdd = rmgr.makeTrue();

        // check whether each of the predicate is implied in the next state...

        for (AbstractionPredicate p : predicates) {
          Pair<BooleanFormula, AbstractionPredicate> cacheKey = Pair.of(f, p);
          if (useCache && cartesianAbstractionCache.containsKey(cacheKey)) {
            byte predVal = cartesianAbstractionCache.get(cacheKey);

            stats.abstractionEnumTime.getInnerTimer().start();
            Region v = p.getAbstractVariable();
            if (predVal == -1) { // pred is false
              v = rmgr.makeNot(v);
              absbdd = rmgr.makeAnd(absbdd, v);
            } else if (predVal == 1) { // pred is true
              absbdd = rmgr.makeAnd(absbdd, v);
            } else {
              assert predVal == 0 : "predicate value is neither false, true, nor unknown";
            }
            stats.abstractionEnumTime.getInnerTimer().stop();

          } else {
            logger.log(Level.ALL, "DEBUG_1",
                "CHECKING VALUE OF PREDICATE: ", p.getSymbolicAtom());

            // instantiate the definition of the predicate
            BooleanFormula predTrue = fmgr.instantiate(p.getSymbolicAtom(), ssa);
            BooleanFormula predFalse = bfmgr.not(predTrue);

            // check whether this predicate has a truth value in the next
            // state
            byte predVal = 0; // pred is neither true nor false

            thmProver.push(predFalse);
            boolean isTrue = thmProver.isUnsat();
            thmProver.pop();

            if (isTrue) {
              stats.abstractionEnumTime.getInnerTimer().start();
              Region v = p.getAbstractVariable();
              absbdd = rmgr.makeAnd(absbdd, v);
              stats.abstractionEnumTime.getInnerTimer().stop();

              predVal = 1;
            } else {
              // check whether it's false...
              thmProver.push(predTrue);
              boolean isFalse = thmProver.isUnsat();
              thmProver.pop();

              if (isFalse) {
                stats.abstractionEnumTime.getInnerTimer().start();
                Region v = p.getAbstractVariable();
                v = rmgr.makeNot(v);
                absbdd = rmgr.makeAnd(absbdd, v);
                stats.abstractionEnumTime.getInnerTimer().stop();

                predVal = -1;
              }
            }

            if (useCache) {
              cartesianAbstractionCache.put(cacheKey, predVal);
            }
          }
        }

        return absbdd;

      } finally {
        thmProver.pop();
        stats.abstractionEnumTime.stopOuter();
      }
    }
  }

  private BooleanFormula buildFormula(BooleanFormula symbFormula) {

    if (fmgr.useBitwiseAxioms()) {
      BooleanFormula bitwiseAxioms = fmgr.getBitwiseAxioms(symbFormula);
      if (!bfmgr.isTrue(bitwiseAxioms)) {
        symbFormula = bfmgr.and(symbFormula, bitwiseAxioms);

        logger.log(Level.ALL, "DEBUG_3", "ADDED BITWISE AXIOMS:", bitwiseAxioms);
      }
    }

    return symbFormula;
  }

  private Region buildBooleanAbstraction(BooleanFormula f, SSAMap ssa,
      Collection<AbstractionPredicate> predicates) {

    // first, create the new formula corresponding to
    // (symbFormula & edges from e to succ)
    // TODO - at the moment, we assume that all the edges connecting e and
    // succ have no statement or assertion attached (i.e. they are just
    // return edges or gotos). This might need to change in the future!!
    // (So, for now we don't need to to anything...)

    // build the definition of the predicates, and instantiate them
    // also collect all predicate variables so that the solver knows for which
    // variables we want to have the satisfying assignments
    BooleanFormula predDef = bfmgr.makeBoolean(true);
    List<BooleanFormula> predVars = new ArrayList<>(predicates.size());

    for (AbstractionPredicate p : predicates) {
      // get propositional variable and definition of predicate
      BooleanFormula var = p.getSymbolicVariable();
      BooleanFormula def = p.getSymbolicAtom();
      if (bfmgr.isFalse(def)) {
        continue;
      }
      def = fmgr.instantiate(def, ssa);

      // build the formula (var <-> def) and add it to the list of definitions
      BooleanFormula equiv = bfmgr.equivalence(var, def);
      predDef = bfmgr.and(predDef, equiv);

      predVars.add(var);
    }

    // the formula is (abstractionFormula & pathFormula & predDef)
    BooleanFormula fm = bfmgr.and(f, predDef);
    Region result;

    if (predVars.isEmpty()) {
      stats.numSatCheckAbstractions++;

      stats.abstractionSolveTime.start();
      boolean satResult = !solver.isUnsat(fm);
      stats.abstractionSolveTime.stop();

      RegionCreator rmgr = amgr.getRegionCreator();

      result = (satResult) ? rmgr.makeTrue() : rmgr.makeFalse();

    } else {
      logger.log(Level.ALL, "COMPUTING ALL-SMT ON FORMULA: ", fm);
      AllSatResult allSatResult;
      try (ProverEnvironment thmProver = solver.newProverEnvironment()) {
        thmProver.push(fm);
        allSatResult = thmProver.allSat(predVars, amgr.getRegionCreator(),
            stats.abstractionSolveTime, stats.abstractionEnumTime);
      }
      result = allSatResult.getResult();

      // update statistics
      int numModels = allSatResult.getCount();
      if (numModels < Integer.MAX_VALUE) {
        stats.maxAllSatCount = Math.max(numModels, stats.maxAllSatCount);
        stats.allSatCount += numModels;
      }
    }

    return result;
  }

  /**
   * Checks if a1 => a2
   */
  public boolean checkCoverage(AbstractionFormula a1, AbstractionFormula a2) {
    return amgr.entails(a1.asRegion(), a2.asRegion());
  }

  /**
   * Checks if (a1 & p1) => a2
   */
  public boolean checkCoverage(AbstractionFormula a1, PathFormula p1, AbstractionFormula a2) {
    BooleanFormula absFormula = a1.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(p1.getFormula());
    BooleanFormula a = bfmgr.and(absFormula, symbFormula);

    // get formula of a2 with the indices of p1
    BooleanFormula b = fmgr.instantiate(a2.asFormula(), p1.getSsa());

    return solver.implies(a, b);
  }

  /**
   * Checks whether a1.getFormula() => a2.getFormula() and whether the a1.getSsa()(v) <= a2.getSsa()(v) for all v
   */
  public boolean checkCoverage(PathFormula a1, PathFormula a2, PathFormulaManager pfmgr) {
    stats.numPathFormulaCoverageChecks++;

    //handle common special case more efficiently
    if (a1.equals(a2)) {
      stats.numEqualPathFormulae++;
      return true;
    }

    //check ssa maps
    SSAMap map1 = a1.getSsa();
    SSAMap map2 = a2.getSsa();
    for (String var : map1.allVariables()) {
      if (map2.getIndex(var) < map1.getIndex(var)) { return false; }
    }

    //merge path formulae
    PathFormula mergedPathFormulae = pfmgr.makeOr(a1, a2);

    //quick syntactic check
    Formula arg = fmgr.getUnsafeFormulaManager().getArg(fmgr.extractFromView(mergedPathFormulae.getFormula()), 0);
    BooleanFormula leftFormula = fmgr.wrapInView(fmgr.getUnsafeFormulaManager().typeFormula(FormulaType.BooleanType, arg));
    // BooleanFormula leftFormula = getArguments(mergedPathFormulae.getFormula())[0];
    BooleanFormula rightFormula = a2.getFormula();
    if (fmgr.checkSyntacticEntails(leftFormula, rightFormula)) {
      stats.numSyntacticEntailedPathFormulae++;
      return true;
    }


    //check formulae
    if (!solver.implies(mergedPathFormulae.getFormula(), a2.getFormula())) { return false; }
    stats.numSemanticEntailedPathFormulae++;

    return true;
  }

  /**
   * Checks if an abstraction formula and a pathFormula are unsatisfiable.
   * @param pAbstractionFormula the abstraction formula
   * @param pPathFormula the path formula
   * @return unsat(pAbstractionFormula & pPathFormula)
   */
  public boolean unsat(AbstractionFormula abstractionFormula, PathFormula pathFormula) {
    BooleanFormula absFormula = abstractionFormula.asInstantiatedFormula();
    BooleanFormula symbFormula = buildFormula(pathFormula.getFormula());
    BooleanFormula f = bfmgr.and(absFormula, symbFormula);
    logger.log(Level.ALL, "Checking satisfiability of formula", f);

    return solver.isUnsat(f);
  }

  public AbstractionFormula makeTrueAbstractionFormula(PathFormula pPreviousBlockFormula) {
    if (pPreviousBlockFormula == null) {
      pPreviousBlockFormula = pfmgr.makeEmptyPathFormula();
    }

    return new AbstractionFormula(fmgr, amgr.getRegionCreator().makeTrue(), bfmgr.makeBoolean(true), bfmgr.makeBoolean(true),
        pPreviousBlockFormula);
  }

  /**
   * Conjuncts two abstractions.
   * Both need to have the same block formula.
   */
  public AbstractionFormula makeAnd(AbstractionFormula a1, AbstractionFormula a2) {
    checkArgument(a1.getBlockFormula().equals(a2.getBlockFormula()));

    Region region = amgr.getRegionCreator().makeAnd(a1.asRegion(), a2.asRegion());
    BooleanFormula formula = fmgr.makeAnd(a1.asFormula(), a2.asFormula());
    BooleanFormula instantiatedFormula = fmgr.makeAnd(a1.asInstantiatedFormula(), a2.asInstantiatedFormula());

    return new AbstractionFormula(fmgr, region, formula, instantiatedFormula, a1.getBlockFormula());
  }

  private AbstractionFormula makeAbstractionFormula(Region abs, SSAMap ssaMap, PathFormula blockFormula) {
    BooleanFormula symbolicAbs = amgr.toConcrete(abs);
    BooleanFormula instantiatedSymbolicAbs = fmgr.instantiate(symbolicAbs, ssaMap);

    return new AbstractionFormula(fmgr, abs, symbolicAbs, instantiatedSymbolicAbs, blockFormula);
  }

  /**
   * Remove a set of predicates from an abstraction.
   * @param oldAbstraction The abstraction to start from.
   * @param removePredicates The predicate to remove.
   * @param ssaMap The SSAMap to use for instantiating the new abstraction.
   * @return A new abstraction similar to the old one without the predicates.
   */
  public AbstractionFormula reduce(AbstractionFormula oldAbstraction,
      Collection<AbstractionPredicate> removePredicates, SSAMap ssaMap) {
    RegionCreator rmgr = amgr.getRegionCreator();

    Region newRegion = oldAbstraction.asRegion();
    for (AbstractionPredicate predicate : removePredicates) {
      newRegion = rmgr.makeExists(newRegion, predicate.getAbstractVariable());
    }

    return makeAbstractionFormula(newRegion, ssaMap, oldAbstraction.getBlockFormula());
  }

  /**
   * Extend an abstraction by a set of predicates.
   * @param reducedAbstraction The abstraction to extend.
   * @param sourceAbstraction The abstraction where to take the predicates from.
   * @param relevantPredicates The predicates to add.
   * @param newSSA The SSAMap to use for instantiating the new abstraction.
   * @return A new abstraction similar to the old one with some more predicates.
   */
  public AbstractionFormula expand(AbstractionFormula reducedAbstraction, AbstractionFormula sourceAbstraction,
      Collection<AbstractionPredicate> relevantPredicates, SSAMap newSSA) {
    return expand(reducedAbstraction.asRegion(), sourceAbstraction.asRegion(), relevantPredicates, newSSA,
        reducedAbstraction.getBlockFormula());
  }

  /**
   * Extend an abstraction by a set of predicates.
   * @param reducedAbstraction The abstraction to extend.
   * @param sourceAbstraction The abstraction where to take the predicates from.
   * @param relevantPredicates The predicates to add.
   * @param newSSA The SSAMap to use for instantiating the new abstraction.
   * @param blockFormula block formula of reduced abstraction state
   * @return A new abstraction similar to the old one with some more predicates.
   */
  public AbstractionFormula expand(Region reducedAbstraction, Region sourceAbstraction,
      Collection<AbstractionPredicate> relevantPredicates, SSAMap newSSA, PathFormula blockFormula) {
    RegionCreator rmgr = amgr.getRegionCreator();

    for (AbstractionPredicate predicate : relevantPredicates) {
      sourceAbstraction = rmgr.makeExists(sourceAbstraction,
          predicate.getAbstractVariable());
    }

    Region expandedRegion = rmgr.makeAnd(reducedAbstraction, sourceAbstraction);

    return makeAbstractionFormula(expandedRegion, newSSA, blockFormula);
  }

  public Collection<AbstractionPredicate> extractPredicates(BooleanFormula pFormula) {
    Collection<BooleanFormula> atoms = fmgr.extractAtoms(pFormula);

    List<AbstractionPredicate> preds = new ArrayList<>(atoms.size());

    for (BooleanFormula atom : atoms) {
      preds.add(amgr.makePredicate(atom));
    }

    return preds;
  }

  // delegate methods

  public Collection<AbstractionPredicate> extractPredicates(Region pRegion) {
    return amgr.extractPredicates(pRegion);
  }

  public Region buildRegionFromFormula(BooleanFormula pF) {
    return amgr.buildRegionFromFormula(pF);
  }

}
