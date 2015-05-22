/*
 * Copyright (C) 2013 INRIA
 *
 * This software is governed by the CeCILL-C License under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/or redistribute the software under the terms of the CeCILL-C license as
 * circulated by CEA, CNRS and INRIA at http://www.cecill.info.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the CeCILL-C License for more details.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package fr.inria.lille.repair;


import com.martiansoftware.jsap.*;
import fr.inria.lille.commons.synthesis.smt.solver.SolverFactory;
import fr.inria.lille.repair.common.config.Config;
import fr.inria.lille.repair.common.synth.StatementType;
import fr.inria.lille.repair.infinitel.InfinitelLauncher;
import fr.inria.lille.repair.nopol.NoPolLauncher;
import fr.inria.lille.repair.ranking.Ranking;
import fr.inria.lille.repair.symbolic.SymbolicLauncher;
import xxl.java.library.FileLibrary;
import xxl.java.library.JavaLibrary;

import java.io.File;
import java.net.URL;
import java.util.Iterator;

public class Main {
	private static JSAP jsap = new JSAP();

	public static void main(String[] args) {
		try {
			initJSAP();
			if (!parseArguments(args)) {
				return;
			}

			File[] sourceFiles = new File[Config.INSTANCE.getProjectSourcePath().length];
			for (int i = 0; i < Config.INSTANCE.getProjectSourcePath().length; i++) {
				String path = Config.INSTANCE.getProjectSourcePath()[i];
				File sourceFile = FileLibrary.openFrom(path);
				sourceFiles[i] = sourceFile;
			}

			URL[] classpath = JavaLibrary.classpathFrom(Config.INSTANCE.getProjectClasspath());

			switch (Config.INSTANCE.getMode()) {
				case REPAIR:
					switch (Config.INSTANCE.getType()) {
						case LOOP:
							InfinitelLauncher.launch(sourceFiles, classpath, Config.INSTANCE.getProjectTests());
							break;
						default:
							NoPolLauncher.launch(sourceFiles, classpath, Config.INSTANCE.getType(), Config.INSTANCE.getProjectTests());
							break;
					}
					break;
				case RANKING:
					Ranking ranking = new Ranking(sourceFiles, classpath, Config.INSTANCE.getProjectTests());
					System.out.println(ranking.summary());
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			showUsage();
		}
	}

	private static void showUsage() {
		System.err.println();
		System.err.println("Usage: java -jar nopol.jar");
		System.err.println("                          " + jsap.getUsage());
		System.err.println();
		System.err.println(jsap.getHelp());
	}

	private static boolean parseArguments(String[] args) {
		JSAPResult config = jsap.parse(args);
		if (!config.success()) {
			System.err.println();
			for (Iterator<?> errs = config.getErrorMessageIterator(); errs.hasNext();) {
				System.err.println("Error: " + errs.next());
			}
			showUsage();
			return false;
		}

		Config.INSTANCE.setType(strToStatementType(config.getString("type")));
		Config.INSTANCE.setMode(strToMode(config.getString("mode")));
		Config.INSTANCE.setSynthesis(strToSynthesis(config.getString("synthesis")));
		Config.INSTANCE.setOracle(strToOracle(config.getString("oracle")));
		Config.INSTANCE.setSolver(strToSolver(config.getString("solver")));
		if(config.getString("solverPath") != null) {
			Config.INSTANCE.setSolverPath(config.getString("solverPath"));
			SolverFactory.setSolver(Config.INSTANCE.getSolver(), Config.INSTANCE.getSolverPath());
		}
		Config.INSTANCE.setProjectClasspath(config.getString("classpath"));
		Config.INSTANCE.setProjectSourcePath(config.getStringArray("source"));
		Config.INSTANCE.setProjectTests(config.getStringArray("test"));
		Config.INSTANCE.setComplianceLevel(config.getInt("complianceLevel", 7));
		return true;
	}

	private static Config.NopolSynthesis strToSynthesis(String str) {
		if (str.equals("smt")) {
			return Config.NopolSynthesis.SMT;
		} else if (str.equals("brutpol")) {
			return Config.NopolSynthesis.BRUTPOL;
		}
		throw new RuntimeException("Unknow Nopol oracle " + str);
	}

	private static Config.NopolOracle strToOracle(String str) {
		if (str.equals("angelic")) {
			return Config.NopolOracle.ANGELIC;
		} else if (str.equals("symbolic")) {
			return Config.NopolOracle.SYMBOLIC;
		}
		throw new RuntimeException("Unknow Nopol oracle " + str);
	}

	private static Config.NopolSolver strToSolver(String str) {
		if (str.equals("z3")) {
			return Config.NopolSolver.Z3;
		} else if (str.equals("cvc4")) {
			return Config.NopolSolver.CVC4;
		}
		throw new RuntimeException("Unknow Nopol solver " + str);
	}

	private static Config.NopolMode strToMode(String str) {
		if (str.equals("repair")) {
			return Config.NopolMode.REPAIR;
		} else if (str.equals("ranking")) {
			return Config.NopolMode.RANKING;
		}
		throw new RuntimeException("Unknow Nopol mode " + str);
	}

	private static StatementType strToStatementType(String str) {
		if (str.equals("loop")) {
			return StatementType.LOOP;
		} else if (str.equals("condition")) {
			return StatementType.CONDITIONAL;
		} else if (str.equals("precondition")) {
			return StatementType.PRECONDITION;
		} else if (str.equals("arithmetic")) {
			return StatementType.INTEGER_LITERAL;
		}
		return StatementType.NONE;
	}

	private static void initJSAP() throws JSAPException {
		FlaggedOption modeOpt = new FlaggedOption("mode");
		modeOpt.setRequired(false);
		modeOpt.setAllowMultipleDeclarations(false);
		modeOpt.setLongFlag("mode");
		modeOpt.setShortFlag('m');
		modeOpt.setUsageName("repair|ranking");
		modeOpt.setStringParser(JSAP.STRING_PARSER);
		modeOpt.setDefault("repair");
		modeOpt.setHelp("Define the mode of execution.");
		jsap.registerParameter(modeOpt);

		FlaggedOption typeOpt = new FlaggedOption("type");
		typeOpt.setRequired(false);
		typeOpt.setAllowMultipleDeclarations(false);
		typeOpt.setLongFlag("type");
		typeOpt.setShortFlag('e');
		typeOpt.setUsageName("loop|condition|precondition|arithmetic");
		typeOpt.setStringParser(JSAP.STRING_PARSER);
		typeOpt.setDefault("condition");
		typeOpt.setHelp("The type of statement to analyze (only used with repair mode).");
		jsap.registerParameter(typeOpt);

		FlaggedOption oracleOpt = new FlaggedOption("oracle");
		oracleOpt.setRequired(false);
		oracleOpt.setAllowMultipleDeclarations(false);
		oracleOpt.setLongFlag("oracle");
		oracleOpt.setShortFlag('o');
		oracleOpt.setUsageName("angelic|symbolic");
		oracleOpt.setStringParser(JSAP.STRING_PARSER);
		oracleOpt.setDefault("angelic");
		oracleOpt.setHelp("Define the oracle (only used with repair mode).");
		jsap.registerParameter(oracleOpt);

		FlaggedOption synthesisOpt = new FlaggedOption("synthesis");
		synthesisOpt.setRequired(false);
		synthesisOpt.setAllowMultipleDeclarations(false);
		synthesisOpt.setLongFlag("synthesis");
		synthesisOpt.setShortFlag('y');
		synthesisOpt.setUsageName("smt|brutpol");
		synthesisOpt.setStringParser(JSAP.STRING_PARSER);
		synthesisOpt.setDefault("smt");
		synthesisOpt.setHelp("Define the patch synthesis.");
		jsap.registerParameter(synthesisOpt);

		FlaggedOption solverOpt = new FlaggedOption("solver");
		solverOpt.setRequired(false);
		solverOpt.setAllowMultipleDeclarations(false);
		solverOpt.setLongFlag("solver");
		solverOpt.setShortFlag('l');
		solverOpt.setUsageName("z3|cvc4");
		solverOpt.setStringParser(JSAP.STRING_PARSER);
		solverOpt.setDefault("z3");
		solverOpt.setHelp("Define the solver (only used with smt synthesis).");
		jsap.registerParameter(solverOpt);

		FlaggedOption solverPathOpt = new FlaggedOption("solverPath");
		solverPathOpt.setRequired(false);
		solverPathOpt.setAllowMultipleDeclarations(false);
		solverPathOpt.setLongFlag("solver-path");
		solverPathOpt.setShortFlag('p');
		solverPathOpt.setStringParser(JSAP.STRING_PARSER);
		solverPathOpt.setHelp("Define the solver binary path (only used with smt synthesis).");
		jsap.registerParameter(solverPathOpt);

		FlaggedOption sourceOpt = new FlaggedOption("source");
		sourceOpt.setRequired(true);
		sourceOpt.setAllowMultipleDeclarations(false);
		sourceOpt.setLongFlag("source");
		sourceOpt.setShortFlag('s');
		sourceOpt.setStringParser(JSAP.STRING_PARSER);
		sourceOpt.setList(true);
		sourceOpt.setHelp("Define the path to the source code of the project.");
		jsap.registerParameter(sourceOpt);

		FlaggedOption classpathOpt = new FlaggedOption("classpath");
		classpathOpt.setRequired(true);
		classpathOpt.setAllowMultipleDeclarations(false);
		classpathOpt.setLongFlag("classpath");
		classpathOpt.setShortFlag('c');
		classpathOpt.setStringParser(JSAP.STRING_PARSER);
		classpathOpt.setHelp("Define the classpath of the project.");
		jsap.registerParameter(classpathOpt);

		FlaggedOption testOpt = new FlaggedOption("test");
		testOpt.setRequired(false);
		testOpt.setAllowMultipleDeclarations(false);
		testOpt.setLongFlag("test");
		testOpt.setShortFlag('t');
		testOpt.setList(true);
		testOpt.setStringParser(JSAP.STRING_PARSER);
		testOpt.setHelp("Define the tests of the project.");
		jsap.registerParameter(testOpt);

		FlaggedOption complianceLevelOpt = new FlaggedOption("complianceLevel");
		complianceLevelOpt.setRequired(false);
		complianceLevelOpt.setAllowMultipleDeclarations(false);
		complianceLevelOpt.setLongFlag("complianceLevel");
		complianceLevelOpt.setStringParser(JSAP.INTEGER_PARSER);
		complianceLevelOpt.setDefault("7");
		complianceLevelOpt.setHelp("The compliance level of the project.");
		jsap.registerParameter(complianceLevelOpt);
	}
}
