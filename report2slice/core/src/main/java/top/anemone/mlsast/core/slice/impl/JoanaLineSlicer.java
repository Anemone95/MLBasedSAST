package top.anemone.mlsast.core.slice.impl;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGNode.Kind;
import edu.kit.joana.ifc.sdg.graph.slicer.Slicer;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import org.jgrapht.traverse.BreadthFirstIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.anemone.mlsast.core.exception.NotFoundException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class JoanaLineSlicer {

	private static final Logger LOGGER = LoggerFactory.getLogger(JoanaLineSlicer.class);
	private SDG sdg;
	private Slicer slicer;

	private Map<Line, Set<SDGNode>> line2nodes;
	private SortedSet<Line> lines;
	public ArrayList<SDGNode> toAbstract;

	public JoanaLineSlicer(final SDG sdg) {
		this(sdg, "Summary", true);
		toAbstract = new ArrayList<SDGNode>();
	}

	public JoanaLineSlicer(final SDG sdg, final String slicerKind, final boolean isBackward) {
		this.sdg = sdg;
		this.slicer = new SummarySlicerBackward(sdg);
		toAbstract = new ArrayList<SDGNode>();
	}

	public SortedSet<Line> getLines() {
		return Collections.unmodifiableSortedSet(lines);
	}

	public HashSet<SDGNode> getNodesAtLine(Line line) throws NotFoundException {
		HashSet<SDGNode> nodes = new HashSet<SDGNode>();
		HashSet<SDGNode> successorNodes=new HashSet<>();
		int dist = 987654321;
		final BreadthFirstIterator<SDGNode, SDGEdge> it = new BreadthFirstIterator<SDGNode, SDGEdge>(sdg);
		while (it.hasNext()) {
			final SDGNode node = it.next();
			// TODO node func name==sink func
//			if (node.getSource().equals(line.filename) && node.getKind().equals(Kind.CALL)) {
			if (node.getSource().equals(line.filename)) {
				//System.out.println(node.getId() + ":" + node.getSource() + ":" + node.getSr() + ":" + node.getLabel());
				if (node.getSr() == line.line) {//  && !isAbstractNode(node) && !isAbstractNode(node)) {
					nodes.add(node);
				}
				int currDist=node.getSr()-line.line; // 碰到string append可能会错位，但是应该只能错一位
				if (currDist>=0 && currDist<dist){
					successorNodes.clear();
					successorNodes.add(node);
					dist=currDist;
				} else if (currDist==dist){
					successorNodes.add(node);
				}
			}
		}
		if (nodes.isEmpty()) {
		    LOGGER.warn("No code at line: "+line.line+", alter to successor nodes");
			if (!successorNodes.isEmpty()){
			    nodes=successorNodes;
			} else {
				throw new NotFoundException("No node " + line);
			}
		}
		return nodes;
	}

	public Collection<SDGNode> slice(Line line) {
		Set<Line> mylines = new HashSet<Line>();
		mylines.add(line);
		return slice(mylines);
	}

	public Collection<SDGNode> slice(Set<Line> line) {
		if (sdg == null) { throw new IllegalStateException("Run readIn first to load sdg from file."); }
		HashSet<SDGNode> crit = new HashSet<>();
		for (Line l : line) {
			Set<SDGNode> nodes = line2nodes.get(l);
			if (nodes != null) {
				crit.addAll(nodes);
			}
		}
		return slice(crit);
	}

	/**
     * 先调用 SummarySlicerBackward 做切片，再剪枝
	 * @param crit sink点集合
	 * @return
	 */
	public Collection<SDGNode> slice(HashSet<SDGNode> crit) {
		Collection<SDGNode> result = slicer.slice(crit);
		List<SDGNode> toRemove = new ArrayList<SDGNode>();
		boolean verbose = false;
		for (SDGNode n : result) {
			if (isRemoveNode(n)) {
				//System.out.println(n.getId() + "\t" + n.getLabel() + "\t" + n.getType() + "\t" + n.getKind() + "\t"
				//	+ n.getOperation() + "\t" + n.getSr() + "\t" + n.getSource() + "\t" + n.getBytecodeIndex() + "\t"
				//	+ n.getClassLoader());
				toRemove.add(n);
			} else if (isAbstractNode(n)) {
				toAbstract.add(n);
			} else if (verbose) {
				System.out.println(n.getId() + "\t" + n.getLabel() + "\t" + n.getType() + "\t" + n.getKind() + "\t"
						+ n.getOperation() + "\t" + n.getSr() + "\t" + n.getSource() + "\t" + n.getBytecodeIndex());
			}
		}
		result.removeAll(toRemove);
		return result;
	}

	public HashMap<String, String> computeVariableNameMap(String fileName, Collection<SDGNode> slice)
			throws IOException {
		String codeStr = String.join("\n", Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8));
		CompilationUnit cu = JavaParser.parse(codeStr);

		HashMap<String, String> variableNameMap = new HashMap<String, String>();

		//LHS of assignment operations
		cu.getNodesByType(AssignExpr.class).stream().forEach(c -> {
			SDGNode latestNode = getLatestNode(slice, c.getBegin().get().line, fileName);
			if (latestNode != null) {
				String[] assignmentPair = latestNode.getLabel().split(" = ");
				if (!variableNameMap.containsKey(assignmentPair[0])) {
					variableNameMap.put(assignmentPair[0], c.getTarget().toString());
				}
			}
		});

		// Collect all VariableDeclarators
		List<VariableDeclarator> varDec = cu.getNodesByType(VariableDeclarator.class);
		for (VariableDeclarator vd : varDec) {
			List<SDGNode> nodes = getRelevantNodes(slice, vd.getBegin().get().line, fileName);
			for (SDGNode n : nodes) {
				String[] assignmentPair = n.getLabel().split(" = ");
				if (!variableNameMap.containsKey(assignmentPair[0])) {
					if ((n.getKind().equals(Kind.CALL) && typeComperator(n, vd))
							|| (n.getKind().equals(Kind.EXPRESSION) && n.getType().equals("Ljava/lang/Object"))) {
						variableNameMap.put(assignmentPair[0], vd.getIdentifierAsString());
					} else if (n.getType().equals("Ljava/lang/StringBuilder")) {
						variableNameMap.put(assignmentPair[0], "sb");
					}
				}
			}
		}
		return variableNameMap;
	}

	private boolean typeComperator(SDGNode n, VariableDeclarator variableDeclarator) {
		String string = variableDeclarator.getType().toString().replaceAll("<.*>|\\[\\]", "");
		String replace = n.getType().replace("/", ".");
		replace = replace.substring(replace.indexOf("L") + 1);
		return replace.endsWith(string)
				|| (string.contains(".") && replace.startsWith(string.substring(0, string.lastIndexOf(".") + 1))
						&& replace.endsWith(string.substring(string.lastIndexOf(".") + 1)));
	}

	public Set<Line> getLinesForNodes(Collection<SDGNode> nodes) {
		TreeSet<Line> lines = new TreeSet<Line>();

		for (SDGNode node : nodes) {
			if (isRemoveNode(node)) {
				lines.add(new Line(node.getSource(), node.getSr()));
			}
		}

		return lines;
	}

	public SDGNode getLatestNode(Collection<SDGNode> nodes, int line, String fileName) {
		SDGNode rslt = null;
		for (SDGNode n : nodes) {
			if (fileName.endsWith(n.getSource()) && line == n.getSr() && (rslt == null || rslt.getId() < n.getId())) {
				rslt = n;
			}
		}
		return rslt;
	}

	public List<SDGNode> getRelevantNodes(Collection<SDGNode> nodes, int line, String fileName) {
		List<SDGNode> rslt = new ArrayList<SDGNode>();
		for (SDGNode n : nodes) {
			if (n.getSource().equals(fileName) && line == n.getSr() && !n.getKind().equals(Kind.PREDICATE)) {
				rslt.add(n);
			}
		}
		return rslt;
	}

	public static class LineInSlice implements Comparable<LineInSlice> {
		public int count;
		public final Line line;

		public LineInSlice(final Line line) {
			this.line = line;
		}

		public int hashCode() {
			return line.hashCode() * 4177;
		}

		public boolean equals(Object o) {
			if (this == o) { return true; }

			if (o instanceof LineInSlice) {
				LineInSlice lis = (LineInSlice) o;
				return line.equals(lis.line);
			}

			return false;
		}

		public int compareTo(LineInSlice other) {
			int diff = count - other.count;

			if (diff != 0) { return diff; }

			return line.compareTo(other.line);
		}

		public String toString() {
			return count + "\t" + line;
		}

	}

	/**
	 * 若该节点满足一下条件任意一条，则被删去：
     * 	源代码为空
	 *  Java Rumtime节点
	 *  Node.getSr()获取代码在源代码的那行，若<0则被删去
     *  isExcluded()在黑名单上的类被删去（不是很理解，之前不是已经exclude了一批类了么？）
	 *  Exception的Label
	 * 	node.getBytecodeIndex() 不大懂
	 * 	node.getOperation() != SDGNode.Operation.ASSIGN Expr表达式被删除
	 *
	 * @param node
	 * @return
	 */
	public static boolean isRemoveNode(final SDGNode node) {
		return node.getSource() == null || (node.getClassLoader() != null && node.getClassLoader().equals("Primordial"))
				|| node.getSr() < 0 || isExcluded(node.getSource()) || node.getLabel().contains("_exception_")
				|| node.getLabel().contains("fake")
				|| (node.getBytecodeIndex() < -2 && node.getOperation() != SDGNode.Operation.ASSIGN);
	}

	private static boolean isExcluded(final String str) {
		return str.contains("java/lang") || str.contains("java/io") || str.contains("java/util")
				|| str.contains("com/ibm/wala") || str.contains("sun/reflect") || str.contains("java/security")
				|| str.contains("sun/") || str.contains("javax/servlet");
	}

	public boolean isAbstractNode(SDGNode sdgNode) {
		return sdgNode.getKind().equals(Kind.FORMAL_IN) || sdgNode.getKind().equals(Kind.FORMAL_OUT)
				|| sdgNode.getKind().equals(Kind.ACTUAL_IN) || sdgNode.getKind().equals(Kind.ACTUAL_OUT)
				|| sdgNode.getLabel().equals("many2many") || sdgNode.getLabel().contains("UNIQ(")
				|| sdgNode.getLabel().contains("<init>") || sdgNode.getLabel().equals("immutable");
	}

	public static String getClassName(String file) {
		String result;

		int indexOfSlash = file.lastIndexOf('/');
		if (indexOfSlash > 0) {
			result = file.substring(indexOfSlash + 1);
		} else {
			result = file;
		}
		result = result.substring(0, result.lastIndexOf('.'));
		return result;
	}

	public static class Line implements Comparable<Line> {
		public final String filename;
		public final int line;

		public Line(String filename, int line) {
			this.filename = filename;
			this.line = line;
		}

		public String toString() {
			return filename + ":" + line;
		}

		public String getRealLine(String base) throws IOException {
			String real = "<not found>";

			BufferedReader bIn = new BufferedReader(new FileReader(base + filename));
			for (int i = 1; i < line && bIn.ready(); i++) {
				bIn.readLine();
			}

			if (bIn.ready()) {
				real = bIn.readLine();
			}
			bIn.close();
			return real;
		}

		public boolean equals(Object obj) {
			if (obj instanceof Line) {
				Line l = (Line) obj;
				return filename.equals(l.filename) && line == l.line;
			}
			return false;
		}

		public int hashCode() {
			return filename.hashCode() * line;
		}

		public int compareTo(Line l) {
			int cmp = filename.compareTo(l.filename);
			if (cmp == 0) {
				cmp = line - l.line;
			}
			return cmp;
		}
	}
}