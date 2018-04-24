package edu.usc.softarch.arcade.fieldaccess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.Local;
import soot.MethodSource;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.ArrayRef;
import soot.jimple.AssignStmt;
import soot.jimple.Constant;
import soot.jimple.DefinitionStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;
import soot.util.Chain;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import edu.usc.softarch.arcade.classgraphs.ClassGraphTransformer;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.datatypes.Proj;
import edu.usc.softarch.arcade.weka.AddTargetClassToArff;

/**
 * @author joshua
 *
 */
public class FieldAccessTransformer extends SceneTransformer {

	protected static int classFieldReadCount;
	protected static int classFieldWriteCount;
	protected static int classFieldInstanceInvokeCount;
	protected static boolean DEBUG = false;
	protected static boolean DEBUG_FIELDREADINVOKE = true;
	protected static Instances data;
	// private static ClassGraph clg = new ClassGraph();
	private static int classMethodSizeAggregator;

	@Override
	protected void internalTransform(final String phaseName, final Map options) {
		final String datasetsDir = "/Users/joshuaga/Documents/workspace/MyExtractors/datasets";
		// FileReader fr;

		final String arffFilePrefix = prepareWekaInstances(datasetsDir);

		Chain<SootClass> appClasses = Scene.v().getApplicationClasses();

		for (final SootClass c : appClasses) {
			System.out.println(c);
		}

		System.out.println("Number of application classes: " + appClasses.size());

		ClassGraphTransformer.constructClassGraph();
		final ArrayList<SootClass> cgNodes = ClassGraphTransformer.clg.getNodes();

		for (final SootClass c : cgNodes) {
			if (c.isConcrete()) {
				try {
					extractFromClass(c);
				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}

		appClasses = Scene.v().getApplicationClasses();
		for (final SootClass c : appClasses) {
			if (c.isAbstract() && !c.isInterface()) {
				try {
					extractFromClass(c);
				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1);

				}
			}
		}

		System.out.println("The dataset (changed?): ");
		System.out.println("=======================================");
		System.out.println(data);
		System.out.println();

		saveWekaInstances(datasetsDir, arffFilePrefix);
	}

	private void saveWekaInstances(final String datasetsDir, final String arffFilePrefix) {
		final ArffSaver saver = new ArffSaver();
		saver.setInstances(data);
		try {
			final String fullDirAndArffFilename = datasetsDir + "/" + arffFilePrefix + "/" + arffFilePrefix + "_withFieldAccessInfo.arff";
			saver.setFile(new File(fullDirAndArffFilename));
			saver.setDestination(new File(fullDirAndArffFilename)); // **not**
			// necessary
			// in 3.5.4 and
			// later
			saver.writeBatch();
			System.out.println("Wrote file: " + fullDirAndArffFilename);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private String prepareWekaInstances(final String datasetsDir) {
		FileReader fr;
		String arffFilePrefix = "";

		if (Config.proj.equals(Proj.LlamaChat)) {
			arffFilePrefix = Config.llamaChatStr;
		} else if (Config.proj.equals(Proj.FreeCS)) {
			arffFilePrefix = Config.freecsStr;
		} else if (Config.proj.equals(Proj.GujChat)) {
			arffFilePrefix = Config.gujChatStr;
		} else {
			System.err.println("Couldn't determine current project for FieldAccessTransformer");
			System.exit(1);
		}

		try {
			fr = new FileReader(datasetsDir + "/" + arffFilePrefix + "/" + arffFilePrefix + ".arff");
			data = new Instances(fr);
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		System.out.println("The dataset: ");
		System.out.println("=======================================");
		System.out.println(data);
		System.out.println();

		data.insertAttributeAt(new Attribute("fieldReadCount"), data.numAttributes());
		data.insertAttributeAt(new Attribute("fieldWriteCount"), data.numAttributes());
		data.insertAttributeAt(new Attribute("fieldInstanceInvokeCount"), data.numAttributes());
		data.insertAttributeAt(new Attribute("avgMethodSize"), data.numAttributes());
		data.insertAttributeAt(new Attribute("maxMethodSize"), data.numAttributes());
		return arffFilePrefix;
	}

	protected static void extractFromClass(final SootClass sClass) throws IOException {
		final Instance instance = AddTargetClassToArff.findMatchingInstance(data, sClass.toString());
		if (instance == null) {
			System.out.println("Cannot find instance for: " + sClass);
			System.exit(1);
		}

		System.out.println();
		System.out.println("Existing attribute values for " + sClass);
		System.out.println("=======================================");
		System.out.println(instance);
		System.out.println();

		final Chain<SootField> sClassFields = sClass.getFields();

		final Iterator<?> fIt = sClass.getFields().iterator();
		System.out.println("\n");
		System.out.println("Fields of " + sClass);
		System.out.println("=======================================");
		while (fIt.hasNext()) {
			final SootField f = (SootField) fIt.next();
			System.out.println(f);
		}

		final Iterator<?> methodIt = sClass.getMethods().iterator();
		classFieldReadCount = 0; // must reinitialize field read count
		classFieldWriteCount = 0; // must reinitialize field write count
		classFieldInstanceInvokeCount = 0;
		classMethodSizeAggregator = 0;
		int maxMethodSize = 0;
		final int depth = 0;
		while (methodIt.hasNext()) {
			final SootMethod m = (SootMethod) methodIt.next();
			System.out.println("=======================================");
			System.out.println("method: " + m.toString());

			Body b = null;
			if (m.isConcrete()) {
				b = m.retrieveActiveBody();
			} else {
				final MethodSource ms = m.getSource();
				ms.getBody(m, "cg");
			}

			if (b == null) {
				System.out.println("Got null active body for.");
				continue;
			}

			final UnitGraph graph = new ExceptionalUnitGraph(b);
			final SimpleLocalDefs localDefs = new SimpleLocalDefs(graph);
			final Iterator<?> graphIt = graph.iterator();
			int methodFieldReadCount = 0;
			int methodFieldWriteCount = 0;
			int methodInstanceInvokeExprCount = 0;
			while (graphIt.hasNext()) { // loop through all units of method's
				// graph
				final Unit u = (Unit) graphIt.next(); // grab the next unit from
				// the
				// graph
				System.out.println(u);

				final FieldStats fs = countFieldReads(depth, sClassFields, localDefs, u);
				methodFieldReadCount += fs.fieldReadCount;
				methodFieldWriteCount += countFieldWrites(sClassFields, localDefs, u);
				methodInstanceInvokeExprCount += fs.fieldInstanceInvokeCount;

			}
			System.out.println("Method's field read count: " + methodFieldReadCount);
			System.out.println("Method's field write count: " + methodFieldWriteCount);
			System.out.println("Method's field instance invoke count: " + methodInstanceInvokeExprCount);
			System.out.println("Method's size: " + graph.size());
			classFieldReadCount += methodFieldReadCount;
			classFieldWriteCount += methodFieldWriteCount;
			classFieldInstanceInvokeCount += methodInstanceInvokeExprCount;
			classMethodSizeAggregator += graph.size();
			if (graph.size() > maxMethodSize) {
				maxMethodSize = graph.size();
			}
		}
		final double classMethodAvgSize = classMethodSizeAggregator / sClass.getMethodCount();
		System.out.println("=======================================");
		System.out.println("Total number of field reads: " + classFieldReadCount);
		System.out.println("Total number of field writes: " + classFieldWriteCount);
		System.out.println("Total number of field instance invocations: " + classFieldInstanceInvokeCount);
		System.out.println("Average method size: " + classMethodAvgSize);
		System.out.println("Max method size: " + maxMethodSize);

		if (data.attribute("fieldReadCount") == null) {
			System.out.println("data.attribute(\"fieldReadCount\") == null");
		}

		instance.setValue(data.attribute("fieldReadCount"), classFieldReadCount);
		instance.setValue(data.attribute("fieldWriteCount"), classFieldWriteCount);
		instance.setValue(data.attribute("fieldInstanceInvokeCount"), classFieldInstanceInvokeCount);
		instance.setValue(data.attribute("avgMethodSize"), classMethodAvgSize);
		instance.setValue(data.attribute("maxMethodSize"), maxMethodSize);
	}

	protected static int countFieldWrites(final Chain<SootField> sClassFields, final SimpleLocalDefs localDefs, final Unit u) {
		int methodFieldWriteCount = 0;
		if (u instanceof DefinitionStmt) { // if the next unit is an if
			// statement
			if (DEBUG) {
				System.out.println("\tAbove line is a definition statement");
			}
			final Iterator<?> dIt = u.getDefBoxes().iterator();
			while (dIt.hasNext()) { // loop through all of the defs in the unit
				final ValueBox defBox = (ValueBox) dIt.next(); // grab a def box
				// from
				// the unit
				if (DEBUG) {
					System.out.println("\t\tdefBox's value and type: " + defBox.getValue() + "," + defBox.getValue().getType());
				}
				for (final SootField f : sClassFields) {
					if (DEBUG) {
						System.out.println("\t\t\t\tChecking against " + f);
					}
					if (f.toString().equals(defBox.getValue().toString())) {
						if (DEBUG) {
							System.out.println("\t\t\t\tDetected definition to the following field: " + defBox.getValue());
						}
						methodFieldWriteCount++;
					}
				}
			}
		}
		return methodFieldWriteCount;
	}

	protected static FieldStats countFieldReads(final int depth, final Chain<SootField> sClassFields, final SimpleLocalDefs localDefs, final Unit u) {
		int methodFieldReadCount = 0;
		int instanceInvokeFieldCount = 0;
		final FieldStats fs = new FieldStats();

		if (u instanceof Stmt) {
			final Stmt stmt = (Stmt) u;
			if (stmt.containsFieldRef()) {
				final FieldRef fr = stmt.getFieldRef();
				if (DEBUG_FIELDREADINVOKE) {
					System.out.println(tabs(1 + 1 * depth) + "FieldRef: " + fr + " at " + u);
				}
			}
			if (stmt.containsArrayRef()) {
				final ArrayRef ar = stmt.getArrayRef();
				if (DEBUG_FIELDREADINVOKE) {
					System.out.println(tabs(1 + 1 * depth) + "ArrayRef: " + ar + " at " + u);
				}
			}
		}
		if (DEBUG) {
			System.out.println(tabs(1 + 1 * depth) + "Above line is an if statement");
		}
		methodFieldReadCount = countFieldReadsInUnit(depth, sClassFields, localDefs, u, methodFieldReadCount);

		final List<ValueBox> useBoxes = u.getUseBoxes();
		for (final ValueBox vb : useBoxes) {
			/*
			 * if (checkFieldReadOnInvokeExpr(depth, sClassFields, localDefs, u,
			 * vb)) { methodFieldReadCount++; }
			 */
			if (vb.getValue() instanceof InstanceInvokeExpr) {
				final InstanceInvokeExpr expr = (InstanceInvokeExpr) vb.getValue(); // if
				// so
				// then
				// typecast
				// the
				// value
				// to
				// an
				// InstanceInvokeExpr
				if (isRecursiveFieldRead(depth, sClassFields, localDefs, u, expr.getBase())) {
					instanceInvokeFieldCount++;
				}
			}

		}

		fs.fieldReadCount = methodFieldReadCount;
		fs.fieldInstanceInvokeCount = instanceInvokeFieldCount;

		return fs;
	}

	// private static boolean checkFieldReadOnInvokeExpr(int depth,
	// Chain<SootField> sClassFields, SimpleLocalDefs localDefs, Unit u,
	// ValueBox vb) {
	// if (vb.getValue() instanceof InstanceInvokeExpr) { // check if the
	// // valuebox is an
	// // InstanceInvokeExpr
	// if (DEBUG_FIELDREADINVOKE) {
	// System.out.println(tabs(1 + 1 * depth) + "Found InvokeExpr");
	// }
	// InstanceInvokeExpr expr = (InstanceInvokeExpr) vb.getValue(); // if
	// // so
	// // then
	// // typecast
	// // the
	// // value
	// // to
	// // an
	// // InstanceInvokeExpr
	// if (DEBUG_FIELDREADINVOKE) {
	// System.out.println(tabs(1 + 1 * depth) + "InstanceInvokeExpr: "
	// + expr);
	// System.out.println(tabs(2 + 1 * depth) + "Base:"
	// + expr.getBase());
	// }
	// if (isRecursiveFieldRead(depth, sClassFields, localDefs, u,
	// expr.getBase())) { // check if the expr is an invocation of
	// // a field or takes a field as an
	// // argument
	// SootMethod m = expr.getMethod();
	// Chain<SootField> sClassFields2 = m.getDeclaringClass()
	// .getFields();
	//
	// Iterator fIt = m.getDeclaringClass().getFields().iterator();
	// if (DEBUG_FIELDREADINVOKE) {
	// System.out.println(tabs(3 + 1 * depth) + "\n");
	// System.out.println(tabs(3 + 1 * depth) + "Fields of "
	// + m.getDeclaringClass());
	// System.out.println(tabs(3 + 1 * depth)
	// + "=======================================");
	// }
	// while (fIt.hasNext()) {
	// SootField f = (SootField) fIt.next();
	// System.out.println(tabs(3 + 1 * depth) + f);
	// }
	//
	// if (m.isConcrete()) {
	// if (DEBUG_FIELDREADINVOKE) {
	// System.out.println(tabs(3 + 1 * depth)
	// + "=======================================");
	// System.out.println(tabs(3 + 1 * depth) + "method: "
	// + m.toString());
	// }
	// Body b = m.retrieveActiveBody();
	// UnitGraph graph = new ExceptionalUnitGraph(b);
	// SimpleLocalDefs localDefs2 = new SimpleLocalDefs(graph);
	// Iterator graphIt = graph.iterator();
	// int methodFieldReadCount = 0;
	// while (graphIt.hasNext()) { // loop through all units of
	// // method's graph
	// Unit u2 = (Unit) graphIt.next(); // grab the next unit
	// // from the graph
	// if (DEBUG_FIELDREADINVOKE) {
	// System.out.println(tabs(3 + 1 * depth) + u2);
	// }
	//
	// methodFieldReadCount += (countFieldReads(depth + 1,
	// sClassFields2, localDefs2, u2)).fieldReadCount;
	//
	// }
	// System.out.println(tabs(3 + 1 * depth)
	// + methodFieldReadCount);
	// }
	// return true;
	// } else {
	// return false;
	// }
	// } else {
	// return false;
	// }
	//
	// }

	private static boolean isRecursiveFieldRead(final int depth, final Chain<SootField> sClassFields, final SimpleLocalDefs localDefs, final Unit u, final Value base) {
		boolean foundSomeMatchingField = false;

		if (base instanceof Local) {
			if (DEBUG_FIELDREADINVOKE) {
				System.out.println(tabs(3 + 1 * depth) + base + " instanceof Local");
			}
			final List<Unit> baseDefs = localDefs.getDefsOfAt((Local) base, u);
			for (final Unit d : baseDefs) {
				if (d instanceof DefinitionStmt) {
					if (DEBUG_FIELDREADINVOKE) {
						System.out.println(tabs(3 + 1 * depth) + d + " instanceof DefinitionStmt");
					}
					final DefinitionStmt stmt = (DefinitionStmt) d;
					if (stmt.getRightOp() instanceof FieldRef) {
						if (DEBUG_FIELDREADINVOKE) {
							System.out.println(tabs(3 + 1 * depth) + stmt.getRightOp() + "instanceof FieldRef");
						}
						final FieldRef fr = (FieldRef) stmt.getRightOp();
						if (DEBUG_FIELDREADINVOKE) {
							System.out.println(tabs(3 + 1 * depth) + "fr.getField(): " + fr.getField());
						}
						for (final SootField f : sClassFields) {
							if (DEBUG_FIELDREADINVOKE) {
								System.out.println(tabs(3 + 1 * depth) + "Comparing Field: " + f + " to FieldRef's field: " + fr.getField());
							}
							if (fr.getField().toString().equals(f.toString())) {
								if (DEBUG_FIELDREADINVOKE) {
									System.out.println(tabs(4 + 1 * depth) + "Match on Field: " + f + " to FieldRef's field: " + fr.getField());
								}
								foundSomeMatchingField = true;
							}
						}
					} else {
						final Iterator<?> ubIter = stmt.getRightOp().getUseBoxes().iterator();
						if (DEBUG_FIELDREADINVOKE) {
							System.out.println(tabs(3 + 1 * depth) + "num of useBoxes in " + d + ": " + stmt.getRightOp().getUseBoxes().size());
						}
						if (ubIter.hasNext()) {
							final Object ubObj = ubIter.next();
							if (DEBUG_FIELDREADINVOKE) {
								System.out.println(tabs(4 + 1 * depth) + "ubObj: " + ubObj);
							}
							if (ubObj instanceof ValueBox) {
								if (DEBUG_FIELDREADINVOKE) {
									System.out.println(tabs(5 + 1 * depth) + ubObj + " instanceof Value");
								}
								final ValueBox ubValBox = (ValueBox) ubObj;
								final Value ubVal = ubValBox.getValue();
								if (ubVal instanceof FieldRef) {
									if (DEBUG_FIELDREADINVOKE) {
										System.out.println(tabs(5 + 1 * depth) + ubVal + " instanceof Local");
									}
									for (final SootField f : sClassFields) {
										if (DEBUG_FIELDREADINVOKE) {
											System.out.println(tabs(5 + 1 * depth) + "Comparing " + f + " to " + ubVal);
										}
										if (f.toString().equals(ubVal.toString())) {
											if (DEBUG_FIELDREADINVOKE) {
												System.out.println(tabs(6 * depth) + "Found matching field and ubVal");
											}
											foundSomeMatchingField = true;
										}
									}
								}
								/*
								 * else if (ubVal instanceof InstanceInvokeExpr)
								 * { InstanceInvokeExpr expr =
								 * (InstanceInvokeExpr)ubVal;
								 * isField(sClassFields
								 * ,localDefs,d,expr.getBase()); }
								 */
							}
						}
					}
				}
			}
		}
		/*
		 * if ( f.toString().equals(base.toString()) ) foundSomeMatchingField =
		 * true;
		 */

		return foundSomeMatchingField;

	}

	private static String tabs(final int numTabs) {
		String tabStr = "";
		if (numTabs < 0) {
			System.out.println("tabs() got a negative parameter");
			return "";
		}
		for (int i = 0; i < numTabs; i++) {
			tabStr += "\t";
		}

		return tabStr;

	}

	private static int countFieldReadsInUnit(final int depth, final Chain<SootField> sClassFields, final SimpleLocalDefs localDefs, final Unit u, int methodFieldReadCount) {
		final Iterator<?> uIt = u.getUseBoxes().iterator();
		while (uIt.hasNext()) { // loop through all of the uses in the unit
			final ValueBox useBox = (ValueBox) uIt.next(); // grab a use box
			// from the
			// unit
			if (DEBUG) {
				System.out.println(tabs(2 * depth) + "useBox's value and type: " + useBox.getValue() + "," + useBox.getValue().getType());
			}
			if (useBox.getValue() instanceof Local) { // if the usebox is a
				// local variable
				final Iterator<?> ldIt = localDefs.getDefsOfAt((Local) useBox.getValue(), u).iterator();
				while (ldIt.hasNext()) { // loop through all the definitions for
					// that use box
					final Unit d = (Unit) ldIt.next();

					final ArrayList<FieldDefPair> fdPairs = findFieldOriginalDefinitionSitePairs(depth, sClassFields, localDefs, d, methodFieldReadCount);

					methodFieldReadCount = fdPairs.size();

					// }
				}

			}
		}
		return methodFieldReadCount;
	}

	protected static ArrayList<FieldDefPair> findFieldOriginalDefinitionSitePairs(final int depth, final Chain<SootField> sClassFields, final SimpleLocalDefs localDefs, final Unit d,
			int methodFieldReadCount) {
		final ArrayList<FieldDefPair> origFieldDefPairs = new ArrayList<FieldDefPair>();
		if (d instanceof AssignStmt) { // if the definition site of the usebox
			// is an assignment statement
			final AssignStmt stmt = (AssignStmt) d; // grab the assignment
			// statement
			if (DEBUG) {
				System.out.println(tabs(4 + 1 * depth) + "stmt.getRightOp() " + stmt.getRightOp() + " has useBoxes of size " + stmt.getRightOp().getUseBoxes().size());
			}
			if (stmt.getRightOp().getUseBoxes().size() <= 1) {
				for (final SootField f : sClassFields) {
					if (DEBUG) {
						System.out.println(tabs(4 + 1 * depth) + "Checking against " + f);
					}
					if (f.toString().equals(stmt.getRightOp().toString())) {
						if (DEBUG) {
							System.out.println(tabs(4 + 1 * depth) + "Detected Assignment using the following field as the right op: " + stmt.getRightOp());
						}
						methodFieldReadCount++;
						origFieldDefPairs.add(new FieldDefPair(f, d));
					}
				}
			} else {
				if (DEBUG) {
					System.out.println(tabs(4 + 1 * depth) + "right op for " + stmt + ": " + stmt.getRightOp());
					System.out.println(tabs(4 + 1 * depth) + "Values used by " + stmt.getRightOp());
				}
				final Iterator<?> ubIt = stmt.getRightOp().getUseBoxes().iterator(); // grab
				// all
				// the
				// use
				// boxes
				// from
				// the
				// right
				// side
				// of
				// the
				// assignment
				while (ubIt.hasNext()) { // loop through all the use boxes from
					// the right side of the assignment
					final ValueBox box = (ValueBox) ubIt.next(); // grab a use
					// box
					// from the right
					// side of the
					// assignment
					if (DEBUG) {
						System.out.println(tabs(4 + 1 * depth) + box.getValue());
					}
					if (box.getValue() instanceof Constant) {
						continue;
					}
					final Iterator<?> ld2It = localDefs.getDefsOfAt((Local) box.getValue(), d).iterator(); // get
					// all
					// the
					// definitions
					// of the
					// previous
					// definition
					// site
					while (ld2It.hasNext()) {
						final Unit d2 = (Unit) ld2It.next();

						final ArrayList<FieldDefPair> fdPairs = findFieldOriginalDefinitionSitePairs(depth, sClassFields, localDefs, d2, methodFieldReadCount);

						methodFieldReadCount = fdPairs.size();
					}

				}
			}
		}
		return origFieldDefPairs;
	}

}
