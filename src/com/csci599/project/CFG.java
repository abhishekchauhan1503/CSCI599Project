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
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.RETURN;
import org.apache.bcel.generic.Select;

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
					&& (dep.if_else == dependency.if_else)) {
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

	public boolean isReachableInCFG(CFG_Graph graph, Integer from, Integer to){
		if(graph.reachabilityList.get(from) == null){
			return false;
		}
		for(InstructionHandle handle : graph.reachabilityList.get(to)){
			if(handle.getPosition() == from){
				return true;
			}
		}
		return false;
	}
	
	public ArrayList<DependencyInformation> getConditionDependencyForLineNumber(
			CFG_Graph graph, String methodName, int lineNumber) {
		ArrayList<DependencyInformation> dependencyList = new ArrayList<DependencyInformation>();
		int edge_count = 0;
		for (ArrayList<InstructionHandle> edge : graph.edges) {
			edge_count++;
			if (edge.get(0).getPosition() > lineNumber) {
				System.out.println("Crossed the required node...breaking");
				break;
			}

			DependencyInformation dependency = new DependencyInformation();
			if (edge.get(0).getInstruction() instanceof BranchInstruction && isReachableInCFG(graph, edge.get(0).getPosition(), lineNumber)) {
				BranchInstruction instruction = (BranchInstruction) edge.get(0)
						.getInstruction();
				InstructionHandle target = instruction.getTarget();
				int position = target.getPosition();
				if (position > lineNumber) {
					// TODO
					// Instruction at lineNumber depends on if part of
					// "instruction"
					dependency.dependencyNode = edge.get(0);
					dependency.if_else = true;
				} else {
					// Check if previous instruction of target is GOTO (End of
					// if-else
					// condition check. If previous instruction is GOTO and
					// target of GOTO>lineNumber, the line depends on else part
					// of the if condition)
					// System.out.println("Position < LineNumber");

					InstructionHandle nodeToCheck = findPreviousInstruction(
							graph, target);

						if (nodeToCheck != null) {
						if (nodeToCheck.getInstruction() instanceof GOTO) {
							GOTO gotoInstruction = (GOTO) nodeToCheck
									.getInstruction();
							if (gotoInstruction.getTarget().getPosition() > lineNumber) {
								// Instruction at lineNumber depends else
								// part
								// of "instruction"
								dependency.dependencyNode = edge.get(0);
								dependency.if_else = false;
							}
						}
					}

				}
			}
			if (dependency.dependencyNode != null
					&& !(checkIfNodeAlreadyExistsInDependencyList(dependency,
							dependencyList))
					&& !(dependency.dependencyNode.getInstruction() instanceof GOTO)) {
				dependencyList.add(dependency);

			}
		}

		return dependencyList;
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
			cfg_graph.reachabilityList = generateReachabilityInformation(
					cfg_graph.edges, cfg_graph.nodes);
			cfg_graphList.add(cfg_graph);
			cfg_graph.reachabilityList = generateReachabilityInformation(cfg_graph.edges, cfg_graph.nodes);
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
								if (!checkIfNodeIsAlreadyInSearchQueue(
										searchQueue, edge.get(0).getPosition())) {
									if (!checkIfNodeHasBeenVisited(
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

			reachList.put(objReachability.reachability.get(0).getPosition(), objReachability.reachability);
			
			reachabilityList.add(objReachability);
			nodesList.remove(0);

		}

		return reachList;
	}

	
	//Need to FIX LATER
	public ArrayList<Reachability> generateCanReachInformation(
			ArrayList<ArrayList<InstructionHandle>> edges,
			ArrayList<InstructionHandle> nodes) {
		ArrayList<Reachability> reachabilityList = new ArrayList<Reachability>();
		ArrayList<InstructionHandle> visitedNodes = new ArrayList<InstructionHandle>();

		ArrayList<InstructionHandle> nodesList = (ArrayList<InstructionHandle>) nodes
				.clone();
		// System.out.println("Total Nodes: " + nodesList.size());
		while (!nodesList.isEmpty()) {
			visitedNodes = new ArrayList<InstructionHandle>();
			ArrayList<InstructionHandle> searchQueue = new ArrayList<InstructionHandle>();
			searchQueue.add(nodesList.get(0));

			Reachability objReachability = new Reachability();
			// System.out.println("Examining: "+nodesList.get(0).getPosition());
			while (!searchQueue.isEmpty()) {
				// System.out.println("Examining: "
				// + searchQueue.get(0).getPosition());
				for (int j = 0; j < edges.size(); j++) {
					ArrayList<InstructionHandle> edge = edges.get(j);
					if (edge.size() > 1) {
						if (edge.get(1) != null) {
							if (edge.get(0).getPosition() == searchQueue.get(0)
									.getPosition()) {
								if (!checkIfNodeIsAlreadyInSearchQueue(
										searchQueue, edge.get(1).getPosition())) {
									if (!checkIfNodeHasBeenVisited(
											visitedNodes, edge.get(1))) {
										searchQueue.add(edge.get(1));
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

			reachabilityList.add(objReachability);
			nodesList.remove(0);

		}

		System.out.println("Prepared Reachability, total nodes = "
				+ reachabilityList.size());
		for (int i = 0; i < reachabilityList.size(); i++) {
			Reachability objReachability = reachabilityList.get(i);
			ArrayList<InstructionHandle> row = objReachability.reachability; // System.out.println();
			if (row.size() > 0) {
				// System.out.println("Size of row: " + row.size());
				System.out.print("\nNode " + row.get(0).getPosition()
						+ " can reach: ");
				for (int j = 0; j < row.size(); j++) {
					System.out.print(row.get(j).getPosition() + " , ");
				} //
					// System.out.println();
			}
		}
		return reachabilityList;
	}

	public boolean checkIfNodeHasBeenVisited(
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

	public boolean checkIfNodeIsAlreadyInSearchQueue(
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
