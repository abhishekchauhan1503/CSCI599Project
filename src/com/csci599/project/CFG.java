package com.csci599.project;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.ILOAD;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LocalVariableInstruction;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Select;

import com.sun.org.apache.bcel.internal.classfile.LocalVariable;

public class CFG {
	protected static final String header = "digraph control_flow_graph {\n\n\tnode [shape = rectangle]; entry exit;\n\tnode [shape = circle];\n\n";
	protected static final String footer = "\n}";
	protected InstructionList instList;

	public CFG(InstructionList instructions) {
		instList = instructions;
	}

	public CFG() {

	}

	public boolean checkIfNodeAlreadyExistsInDependencyList(
			DependencyInformation dependency,
			ArrayList<DependencyInformation> dependencyList) {

		for (DependencyInformation dep : dependencyList) {
			if (dep.dependencyNode.equals(dependency.dependencyNode)
					&& (dep.true_false == dependency.true_false)) {
				return true;
			}
		}
		return false;
	}

	public InstructionHandle findPreviousInstruction(CFG_Graph graph,
			InstructionHandle target) {

		for (int i = 0; i < graph.edges.size(); i++) {
			ArrayList<InstructionHandle> edgeToTest = graph.edges.get(i);
			if (edgeToTest.get(0) != null
					&& edgeToTest.get(0).getPosition() == target.getPosition()) {

				return graph.edges.get(i - 1).get(0);
			}
		}
		return null;
	}

	public boolean isReachableInCFG(CFG_Graph graph, Integer from, Integer to) {
		if (graph.reachabilityList.get(from) == null) {
			return false;
		}
		for (InstructionHandle handle : graph.reachabilityList.get(to)) {
			if (handle.getPosition() == from) {
				return true;
			}
		}
		return false;
	}

	public ArrayList<Nodes> getChildren(Nodes node,
			ArrayList<ArrayList<InstructionHandle>> edges) {
		InstructionHandle nodeName = node.nodeName;
		ArrayList<Nodes> children = new ArrayList<Nodes>();

		for (int i = 0; i < edges.size(); i++) {
			ArrayList<InstructionHandle> edge = edges.get(i);

			if (edge.get(0).equals(nodeName)) {
				Nodes newNode = new Nodes(edge.get(1));
				children.add(newNode);
			}

		}

		return children;
	}

	public ArrayList<Nodes> getParents(Nodes node,
			ArrayList<ArrayList<InstructionHandle>> edges) {
		InstructionHandle nodeName = node.nodeName;
		ArrayList<Nodes> parents = new ArrayList<Nodes>();

		for (int i = 0; i < edges.size(); i++) {
			ArrayList<InstructionHandle> edge = edges.get(i);

			if (edge.get(1) != null && edge.get(1).equals(nodeName)) {
				Nodes newNode = new Nodes(edge.get(0));
				parents.add(newNode);
			}

		}

		return parents;
	}

	public void getPath(SortedMap<Integer, Nodes> nodesMap, int to) {
		int current = to;
		System.out.println("Parents: " + nodesMap.get(current).parents.size());
		ArrayList<InstructionHandle> dependencyList = new ArrayList<InstructionHandle>();
		while (current != 0) {
			InstructionHandle currentInst = nodesMap.get(current).parents
					.get(0).nodeName;

			current = nodesMap.get(current).parents.get(0).nodeName
					.getPosition();
			if (currentInst.getInstruction() instanceof BranchInstruction) {
				dependencyList.add(currentInst);
			}
		}
		// System.out.println(to + " depends on " + dependencyList.size()
		// + " other instructions");
		// for (InstructionHandle handle : dependencyList) {
		// System.out.println(handle);
		// }
	}

	public ArrayList<InstructionHandle> getDependencyInformation(
			CFG_Graph graph, int lineNumber) {
		ArrayList<InstructionHandle> reachabilityList = graph.reachabilityList
				.get(lineNumber);
		ArrayList<InstructionHandle> reachabilityByBranchStatements = new ArrayList<InstructionHandle>();
		ArrayList<InstructionHandle> alwaysExecutedBranches = new ArrayList<InstructionHandle>();
		ArrayList<InstructionHandle> conditionsToSatisfy = new ArrayList<InstructionHandle>();
		if (reachabilityList == null) {
			return null;
		}
		for (int i = 0; i < reachabilityList.size(); i++) {
			if (reachabilityList.get(i).getInstruction() instanceof BranchInstruction
					&& !((reachabilityList.get(i).getInstruction()) instanceof GOTO)) {
				reachabilityByBranchStatements.add(reachabilityList.get(i));
			}
		}

		for (InstructionHandle handle : reachabilityByBranchStatements) {
			// System.out.println("Checking " + handle.getPosition());
			if (checkTargetOnEveryPath(graph.edges, 0, handle.getPosition())) {
				alwaysExecutedBranches.add(handle);
			}
		}

		System.out.println("Following branches are always executed: ");
		for (InstructionHandle handle : alwaysExecutedBranches) {
			System.out.println(handle);
		}

		if (alwaysExecutedBranches.size() > 1) {
			int minDiff = 1000;
			InstructionHandle closestInstruction = alwaysExecutedBranches
					.get(0);
			for (InstructionHandle handle : alwaysExecutedBranches) {
				if (lineNumber - handle.getPosition() < minDiff) {
					minDiff = lineNumber - handle.getPosition();
					closestInstruction = handle;
				}
			}

			ArrayList<InstructionHandle> reachabilityByBranchStatementsForClosestBranch = new ArrayList<InstructionHandle>();
			ArrayList<InstructionHandle> reachabilityListForClosestBranch = graph.reachabilityList
					.get(closestInstruction.getPosition());

			for (int i = 0; i < reachabilityListForClosestBranch.size(); i++) {
				if (reachabilityListForClosestBranch.get(i).getInstruction() instanceof BranchInstruction
						&& !((reachabilityListForClosestBranch.get(i)
								.getInstruction()) instanceof GOTO)) {
					reachabilityByBranchStatementsForClosestBranch
							.add(reachabilityListForClosestBranch.get(i));
				}
			}

			reachabilityByBranchStatements
					.removeAll(reachabilityByBranchStatementsForClosestBranch);
			reachabilityByBranchStatements.add(closestInstruction);

			conditionsToSatisfy.addAll(reachabilityByBranchStatements);
		} else if (alwaysExecutedBranches.size() <= 1) {

			for (InstructionHandle branch : reachabilityByBranchStatements) {
				if (!checkTargetOnEveryPath(graph.edges, branch.getPosition(),
						lineNumber)) {
					conditionsToSatisfy.add(branch);
					// System.out.println(branch.getPosition()
					// + " does not always reach " + lineNumber);
				} else {
					// System.out.println(branch.getPosition()
					// + " will always reach " + lineNumber);
				}
			}

			ArrayList<InstructionHandle> independentConditions = new ArrayList<InstructionHandle>();
			for (int i = reachabilityByBranchStatements.size() - 1; i > 0; i--) {
				if (checkTargetOnEveryPath(graph.edges,
						reachabilityByBranchStatements.get(i).getPosition(),
						reachabilityByBranchStatements.get(i - 1).getPosition())) {
					independentConditions.add(reachabilityByBranchStatements
							.get(i));
					// System.out.println(reachabilityByBranchStatements.get(i)
					// .getPosition()
					// + " will always reach "
					// + reachabilityByBranchStatements.get(i - 1)
					// .getPosition());

				} else {
					// System.out.println(reachabilityByBranchStatements.get(i)
					// .getPosition()
					// + " does not always reach "
					// + reachabilityByBranchStatements.get(i - 1)
					// .getPosition());
					// Ignore branch
				}
			}
			System.out.println(independentConditions.size()
					+ " independent conditions out of "
					+ reachabilityByBranchStatements.size());
			conditionsToSatisfy.removeAll(independentConditions);

		}
		return conditionsToSatisfy;
	}

	public VariableValues getVariablesForCondition(InstructionHandle condition,
			LocalVariableTable table, ArrayList<Nodes> nodes) {
		org.apache.bcel.classfile.LocalVariable[] localVariables = table
				.getLocalVariableTable();
		InstructionHandle conditionInstruction1 = null;
		InstructionHandle conditionInstruction2 = null;
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).nodeName.getPosition() == condition.getPosition()) {
				conditionInstruction1 = nodes.get(i - 1).nodeName;
				conditionInstruction2 = nodes.get(i - 2).nodeName;
				break;
			}
		}
		/*
		 * if(conditionInstruction1 !=null && conditionInstruction2 != null){
		 * System.out.println(condition);
		 * System.out.println(conditionInstruction1);
		 * System.out.println(conditionInstruction2); }
		 */
		Object value = null;
		if (conditionInstruction1.getInstruction() instanceof BIPUSH) {
			value = ((BIPUSH) conditionInstruction1.getInstruction())
					.getValue();

		} else if (conditionInstruction1.getInstruction() instanceof ICONST) {
			value = ((ICONST) conditionInstruction1.getInstruction())
					.getValue();

		} else {
			// check for variable in comparison condition
			if (conditionInstruction1.getInstruction() instanceof LocalVariableInstruction) {
				int index = ((LocalVariableInstruction) conditionInstruction1
						.getInstruction()).getIndex();
				// System.out.println(conditionInstruction2
				// +" defines variable at index: "+index);
				for (org.apache.bcel.classfile.LocalVariable var : localVariables) {
					if (var.getIndex() == index) {
						// System.out.println("Variable Name: "+var.getName());
						value = var.getName();
						break;
					}
				}
			}
		}
		// System.out.println("Value : "+value);

		String variableName = "";
		if (conditionInstruction2.getInstruction() instanceof LocalVariableInstruction) {
			int index = ((LocalVariableInstruction) conditionInstruction2
					.getInstruction()).getIndex();
			// System.out.println(conditionInstruction2
			// +" defines variable at index: "+index);
			for (org.apache.bcel.classfile.LocalVariable var : localVariables) {
				if (var.getIndex() == index) {
					// System.out.println("Variable Name: "+var.getName());
					variableName = var.getName();
					break;
				}
			}
		}
		// System.out.println(variableName+" has the value: "+value);
		VariableValues varVal = new VariableValues();
		varVal.variableName = variableName;
		varVal.value = value;

		return varVal;
	}

	public ArrayList<DependencyInformation> dependencyAdapter(
			ArrayList<InstructionHandle> conditionsToSatisfy, int lineNumber,
			LocalVariableTable table, ArrayList<Nodes> nodes) {
		ArrayList<DependencyInformation> dependencyList = new ArrayList<DependencyInformation>();
		for (InstructionHandle dep : conditionsToSatisfy) {

			IfInstruction ifInstruction = (IfInstruction) dep.getInstruction();
			DependencyInformation dependency = new DependencyInformation();
			dependency.dependencyNode = dep;
			if (dep.getPosition() > lineNumber) { // If branch from a loop
				if (ifInstruction.getTarget().getPosition() < lineNumber) {
					dependency.true_false = true;
				} else {
					dependency.true_false = false;
				}
			} else { // Regular if branch
				if (ifInstruction.getTarget().getPosition() > lineNumber) {
					dependency.true_false = true;
				} else {
					dependency.true_false = false;
				}
			}
			VariableValues varVal = getVariablesForCondition(dep, table, nodes);
			dependency.varVal = varVal;
			dependencyList.add(dependency);

		}
		return dependencyList;
	}

	public boolean checkTargetOnEveryPath(
			ArrayList<ArrayList<InstructionHandle>> edges, int from, int target) {
		// System.out.println("New Run");
		// System.out.println("Starting at: " + from);
		ArrayList<Integer> searchQueue = new ArrayList<Integer>();
		ArrayList<Integer> visitedQueue = new ArrayList<Integer>();
		// System.out.println("Target = " + target);
		boolean isAlwaysFound = true;
		searchQueue.add(from);

		boolean found = false;
		boolean neverFound = true;
		int pathsCount = 0;
		boolean exitFound = false;
		// System.out.println();
		while (!searchQueue.isEmpty()) {
			// System.out.println("Current Start: " + searchQueue.get(0));
			// System.out.print(searchQueue.get(0) + " , ");

			// System.out.println("Search Queue Size: " + searchQueue.size());
			if (searchQueue.get(0) == target) {
				pathsCount++;

				found = true;
				neverFound = false;

				if (exitFound) {
					return false;
				} else {
					found = false;
					exitFound = false;
				}
				searchQueue.remove(0);

				if (searchQueue.isEmpty()) {
					// System.out
					// .println("Search Queue Empty. All possible nodes traversed.");
					return true;
				}
				// System.out.println("Target found at: " + searchQueue.get(0));

			} else if (searchQueue.get(0) == -1) {
				exitFound = true;

				if (!found) {
					// System.out
					// .println("End node found before target at position: "
					// + searchQueue.get(0));
					isAlwaysFound = false;// System.exit(0);

					return false;
				} else {
					// System.out
					// .println("End node found After target at position: "
					// + searchQueue.get(0));
					// isAlwaysFound = false;// System.exit(0);
					// return false;

					// System.out.println("Target Found: "+((InvokeInstruction)(searchQueue.get(0).getInstruction())).getMethodName(cp));

					searchQueue.remove(0);
					// System.out.println("Target found.");
					// System.out.println("Starting new path from "
					// + searchQueue.get(0));
					found = false;
					if (searchQueue.isEmpty()) {
						// System.out
						// .println("Search Queue Empty. All possible nodes traversed.");
						break;
					}
				}
				pathsCount++;

				break;

			}

			ArrayList<Integer> children = new ArrayList<Integer>();
			for (int i = 0; i < edges.size(); i++) {
				ArrayList<InstructionHandle> edge = edges.get(i);

				if (edge.get(0).getPosition() == searchQueue.get(0)) {
					if (edge.get(1) != null) {
						if (!checkIfNodeHasBeenVisited(visitedQueue,
								edge.get(1))
								&& (!checkIfNodeIsAlreadyInSearchQueue(
										searchQueue, edge.get(1).getPosition()))) {

							children.add(edge.get(1).getPosition());
						}
					} else {
						children.add(-1);
					}
				}

			}

			visitedQueue.add(searchQueue.get(0));
			searchQueue.remove(0);
			searchQueue.addAll(0, children);
			// System.out.println("Added " + children.size() + " children");

			// System.out.println("Search Queue Size after iteration: "
			// + searchQueue.size());

		}
		if (neverFound) {
			// System.out.println("Target never found for this run");
		}
		// System.out.println("Total paths: " + pathsCount);
		// System.out.println();
		return isAlwaysFound;
	}

	public ArrayList<CFG_Graph> cfgMaker(String dir, String inputClassFilename)
			throws IOException {

		SortedMap<Integer, InstructionHandle> g_statements = new TreeMap<Integer, InstructionHandle>();
		String path = dir + inputClassFilename + ".class";
		// System.out.println("Parsing " + path + ".");

		JavaClass cls = null;
		try {
			cls = (new ClassParser(path)).parse();
		} catch (IOException e) {
			// e.printStackTrace(debug);
			System.out.println("Error while parsing " + inputClassFilename
					+ ".");
			System.exit(1);
		}

		ClassGen cg = new ClassGen(cls);
		ConstantPoolGen cp = cg.getConstantPool();

		// Search for main method.
		// System.out.println("Searching for entry method:");
		Method mainMethod = null;
		ArrayList<CFG_Graph> cfg_graphList = new ArrayList<CFG_Graph>();

		for (Method m : cls.getMethods()) {
			g_statements.clear();
			// if ("_jspService".equals(m.getName())) {
			mainMethod = m;

			// break;
			// }
			// }
			if (mainMethod == null) {
				System.out.println("No entry method found in "
						+ inputClassFilename + ".");
				break;// System.exit(1);
			}

			// Create CFG.
			// System.out.println("Creating CFG object.");

			// System.out.println("CODE: \n\n");

			InstructionList instList = new InstructionList(mainMethod.getCode()
					.getCode());

			int[] instructionPositions = instList.getInstructionPositions();
			for (int i = 0; i < instList.size(); i++) {

				InstructionHandle handle = instList
						.findHandle(instructionPositions[i]);
				Integer lineNum = handle.getPosition();

				g_statements.put(lineNum, handle);

			}
			// System.exit(0);
			// Map<InstructionHandle, InstructionHandle> internalCFG = new
			// TreeMap<InstructionHandle, InstructionHandle>();
			FileWriter fwr = new FileWriter(dir + "/Dotty/"
					+ inputClassFilename + "-" + mainMethod.getName()
					+ ".dotty", false);
			fwr.write(header);
			// ArrayList<InstructionHandle> edge = new
			// ArrayList<InstructionHandle>();
			ArrayList<ArrayList<InstructionHandle>> graph = new ArrayList<ArrayList<InstructionHandle>>();
			ArrayList<InstructionHandle> nodes = new ArrayList<InstructionHandle>();
			// System.out.println("entry -> 0;");
			fwr.write("\n" + "entry -> 0;");

			// System.out.println("Method "+mainMethod.getName());

			for (SortedMap.Entry<Integer, InstructionHandle> entry : g_statements
					.entrySet()) {
				nodes.add(entry.getValue());
				// System.out.println("Added "+nodes.get(nodes.size()-1));
				if (entry.getValue().getNext() != null) {
					nodes.add(entry.getValue().getNext());
				}
				if (entry.getValue().getNext() != null
						&& !(entry.getValue().getInstruction() instanceof RETURN)) {
					if (entry.getValue().getInstruction() instanceof BranchInstruction) {
						BranchInstruction branchInstruction = (BranchInstruction) entry
								.getValue().getInstruction();

						// System.out.println(entry.getValue().getPosition() +
						// " -> " +
						// ifInstruction.getTarget().getPosition()+";");
						fwr.write("\n" + entry.getValue().getPosition()
								+ " -> "
								+ branchInstruction.getTarget().getPosition()
								+ ";");
						graph.add(new ArrayList<InstructionHandle>(Arrays
								.asList(entry.getValue(),
										branchInstruction.getTarget())));
						// internalCFG.put(entry.getValue(),
						// ifInstruction.getTarget());
						if (branchInstruction instanceof IfInstruction) {
							// System.out.println(entry.getValue().getPosition()
							// +
							// " -> " + entry.getValue().getNext().getPosition()
							// +
							// ";");
							fwr.write("\n" + entry.getValue().getPosition()
									+ " -> "
									+ entry.getValue().getNext().getPosition()
									+ ";");
							// internalCFG.put(entry.getValue(),
							// entry.getValue().getNext());

							graph.add(new ArrayList<InstructionHandle>(Arrays
									.asList(entry.getValue(), entry.getValue()
											.getNext())));
						} else if (branchInstruction instanceof Select) {

							Select selectInst = (Select) branchInstruction;
							InstructionHandle[] instHandleList = selectInst
									.getTargets();

							for (int i = 0; i < instHandleList.length; i++) {
								fwr.write("\n" + entry.getValue().getPosition()
										+ " -> "
										+ instHandleList[i].getPosition() + ";");
								// internalCFG.put(entry.getValue(),
								// entry.getValue().getNext());

								graph.add(new ArrayList<InstructionHandle>(
										Arrays.asList(entry.getValue(),
												instHandleList[i])));
							}

						}
					} else {

						fwr.write("\n" + entry.getValue().getPosition()
								+ " -> "
								+ entry.getValue().getNext().getPosition()
								+ ";");

						graph.add(new ArrayList<InstructionHandle>(Arrays
								.asList(entry.getValue(), entry.getValue()
										.getNext())));

					}
				} else {

					fwr.write("\n" + entry.getValue().getPosition()
							+ " -> exit;");

					graph.add(new ArrayList<InstructionHandle>(Arrays.asList(
							entry.getValue(), null)));

				}
			}
			fwr.write(footer);

			fwr.close();

			// remove duplicate nodes
			// System.out.println("Size before removing duplicates: " +
			// nodes.size());
			HashSet<InstructionHandle> hs = new HashSet<InstructionHandle>();
			hs.addAll(nodes);
			nodes.clear();
			nodes.addAll(hs);

			CFG_Graph cfg_graph = new CFG_Graph();
			cfg_graph.edges = graph;

			SortedMap<Integer, ArrayList<Nodes>> edgesMap = new TreeMap<Integer, ArrayList<Nodes>>();

			nodes = sortNodeList(nodes);
			ArrayList<Nodes> nodesList = new ArrayList<Nodes>();
			SortedMap<Integer, Nodes> nodesMap = new TreeMap<Integer, Nodes>();
			for (InstructionHandle node : nodes) {
				Nodes newNode = new Nodes(node);
				ArrayList<Nodes> children = getChildren(newNode, graph);
				ArrayList<Nodes> parents = getParents(newNode, graph);
				newNode.parents = parents;
				newNode.children = children;
				nodesList.add(newNode);
				edgesMap.put(node.getPosition(), children);
				nodesMap.put(node.getPosition(), newNode);
			}

			cfg_graph.nodesMap = nodesMap;
			cfg_graph.nodes = nodesList;
			cfg_graph.edgesMap = edgesMap;
			cfg_graph.servletName = inputClassFilename;
			cfg_graph.method = mainMethod;

			SortedMap<Integer, Integer> byteCode_to_sourceCode_mapping = new TreeMap<Integer, Integer>();
			LineNumberTable table = mainMethod.getCode().getLineNumberTable();

			cfg_graph.lineNumberTable = table;
			cfg_graph.localVariableTable = mainMethod.getCode()
					.getLocalVariableTable();
			for (InstructionHandle node : nodes) {
				// System.out.println("table Size: " + table.getLength());
				// System.out.println(table.getSourceLine(2));

				// System.out.println("Position " + node
				// + " corresponds to line number "
				// + table.getSourceLine(7));
				try {
					byteCode_to_sourceCode_mapping.put(node.getPosition(),
							table.getSourceLine(node.getPosition()));
				} catch (Exception ex) {

				}
			}
			cfg_graph.byteCode_to_sourceCode_mapping = byteCode_to_sourceCode_mapping;

			for (int i = 0; i < cfg_graph.edges.size(); i++) {
				if (cfg_graph.edges.get(i).get(1) != null) {
					if (cfg_graph.edges.get(i).get(0).getPosition() > cfg_graph.edges
							.get(i).get(0).getPosition()) {
						cfg_graph.edges.remove(i);
						System.out.println("Edge Removed");
					}
				}
			}
			cfg_graph.reachabilityList = generateReachabilityInformation(
					cfg_graph.edges, cfg_graph.nodes);
			cfg_graphList.add(cfg_graph);
			cfg_graph.reachabilityList = generateReachabilityInformation(
					cfg_graph.edges, cfg_graph.nodes);
			cfg_graph.constantPool = cp;
		}
		return cfg_graphList;

	}

	public SortedMap<Integer, ArrayList<InstructionHandle>> generateReachabilityInformation(
			ArrayList<ArrayList<InstructionHandle>> edges,
			ArrayList<Nodes> nodes) {
		ArrayList<Reachability> reachabilityList = new ArrayList<Reachability>();
		ArrayList<InstructionHandle> visitedNodes = new ArrayList<InstructionHandle>();
		SortedMap<Integer, ArrayList<InstructionHandle>> reachList = new TreeMap<Integer, ArrayList<InstructionHandle>>();

		ArrayList<Nodes> nodesList = (ArrayList<Nodes>) nodes.clone();
		// System.out.println("Total Nodes: " + nodesList.size());
		while (!nodesList.isEmpty()) {
			visitedNodes = new ArrayList<InstructionHandle>();
			ArrayList<InstructionHandle> searchQueue = new ArrayList<InstructionHandle>();
			searchQueue.add(nodesList.get(0).nodeName);

			Reachability objReachability = new Reachability();
			// System.out.println("Examining: "+nodesList.get(0).getPosition());
			while (!searchQueue.isEmpty()) {
				// System.out.println("Examining: "
				// + searchQueue.get(0).getPosition());
				for (int j = 0; j < edges.size(); j++) {
					ArrayList<InstructionHandle> edge = edges.get(j);
					if (edge.size() > 1) {
						if (edge.get(1) != null) {
							if (edge.get(1).getPosition() == searchQueue.get(0)
									.getPosition()) {
								if (!checkIfNodeIsAlreadyInSearchQueue2(
										searchQueue, edge.get(0).getPosition())) {
									if (!checkIfNodeHasBeenVisited2(
											visitedNodes, edge.get(0))) {
										searchQueue.add(edge.get(0));
									}
								}
							}
						}
					}
				}
				objReachability.reachability.add(searchQueue.get(0));
				visitedNodes.add(searchQueue.get(0));
				searchQueue.remove(0);
			}

			reachList.put(objReachability.reachability.get(0).getPosition(),
					objReachability.reachability);

			reachabilityList.add(objReachability);
			nodesList.remove(0);

		}

		return reachList;
	}

	public boolean checkIfNodeHasBeenVisited2(
			ArrayList<InstructionHandle> visitedNodes,
			InstructionHandle instructionHandle) {
		for (int i = 0; i < visitedNodes.size(); i++) {

			if (visitedNodes.get(i).getPosition() == instructionHandle
					.getPosition()) {
				// System.out.println(visitedNodes.get(i).getPosition()+" has already been visited");
				return true;
			}
		}
		return false;
	}

	public boolean checkIfNodeIsAlreadyInSearchQueue2(
			ArrayList<InstructionHandle> searchQueue, int position) {
		for (int i = 0; i < searchQueue.size(); i++) {
			// System.out.println("POSITION Matching: "+position+" with "+searchQueue.get(0)
			// .getPosition());
			if (searchQueue.get(i).getPosition() == position) {
				return true;
			}
		}
		return false;
	}

	public boolean checkIfNodeHasBeenVisited(ArrayList<Integer> visitedNodes,
			InstructionHandle instructionHandle) {
		for (int i = 0; i < visitedNodes.size(); i++) {

			if (visitedNodes.get(i) == instructionHandle.getPosition()) {
				// System.out.println(visitedNodes.get(i).getPosition()+" has already been visited");
				return true;
			}
		}
		return false;
	}

	public boolean checkIfNodeIsAlreadyInSearchQueue(
			ArrayList<Integer> searchQueue, int position) {
		for (int i = 0; i < searchQueue.size(); i++) {
			// System.out.println("POSITION Matching: "+position+" with "+searchQueue.get(0)
			// .getPosition());
			if (searchQueue.get(i) == position) {
				return true;
			}
		}
		return false;
	}

	public static ArrayList<String> getArrayListFromCode(String line) {
		ArrayList<String> statements = new ArrayList<String>();
		String temp = "";
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '\n') {
				if (temp.equalsIgnoreCase("Attribute(s) = ")) {
					break;
				}
				statements.add(temp);
				temp = "";
			} else {
				temp += line.charAt(i);
			}
		}
		return statements;
	}

	public ArrayList<InstructionHandle> sortNodeList(
			ArrayList<InstructionHandle> nodes) {

		for (int i = 0; i < nodes.size(); i++) {
			for (int j = 1; j < (nodes.size()); j++) {
				if (nodes.get(j - 1).getPosition() >= nodes.get(j)
						.getPosition()) {

					InstructionHandle temp = nodes.get(j - 1);
					nodes.remove(j - 1);
					nodes.add(j, temp);
				}

			}
		}

		return nodes;

	}

	public void traverseCFG(ArrayList<ArrayList<InstructionHandle>> internalCFG) {
		for (int i = 0; i < internalCFG.size(); i++) {
			ArrayList<InstructionHandle> edge = internalCFG.get(i);
			System.out.println(edge.get(0) + " ----> " + edge.get(1));
		}
	}
}
