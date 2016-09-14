package edu.buffalo.cse.apkfragment.BlueSealUnitGraph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;
import edu.buffalo.cse.blueseal.BSFlow.SootMethodFilter;

public class BSUnitGraphBuilder {
	public static Map<BSUnitNode, Set<List<BSUnitNode>>> nodeToPaths = 
			new HashMap<BSUnitNode, Set<List<BSUnitNode>>>();
	
	public static Set<List<BSUnitNode>> computeGraphPaths(BSUnitGraph graph){
		Set<List<BSUnitNode>> result = new HashSet<List<BSUnitNode>>();
		for(BSUnitNode head : graph.getHeads()){
			Set<List<BSUnitNode>> headRes = computePathFromHead(head, graph);
			result.addAll(headRes);
		}
		return result;
	}
	
	private static Set<List<BSUnitNode>> computePathFromHead(BSUnitNode head,
			BSUnitGraph graph) {
		Set<List<BSUnitNode>> result = new HashSet<List<BSUnitNode>>();
		Set<BSUnitNode> seen = new HashSet<BSUnitNode>();
		result.addAll(DepthFirstVisit(head, seen, graph));
		return result;
	}

	private static Set<List<BSUnitNode>> DepthFirstVisit(
			BSUnitNode head, Set<BSUnitNode> seen,
			BSUnitGraph graph) {
		Set<List<BSUnitNode>> result = new HashSet<List<BSUnitNode>>();
		seen.add(head);
		
		if(graph.getSuccsOf(head).size() == 0){
			List<BSUnitNode> list = new LinkedList<BSUnitNode>();
			list.add(head);
			result.add(list);
		}else{
			for(BSUnitNode succ : graph.getSuccsOf(head)){
				if(!seen.contains(succ)){
					if(!nodeToPaths.containsKey(succ)){
						List<BSUnitNode> newCurList = new LinkedList<BSUnitNode>();
						Set<BSUnitNode> newSeen = new HashSet<BSUnitNode>();
						newSeen.addAll(seen);
						Set<List<BSUnitNode>> sucRes = DepthFirstVisit(succ, newSeen, graph);
						nodeToPaths.put(succ, sucRes);
					}
					
					for(List<BSUnitNode> list : nodeToPaths.get(succ)){
						List<BSUnitNode> newList = new LinkedList<BSUnitNode>();
						newList.add(head);
						newList.addAll(list);
						result.add(newList);
					}
				}
			}
		}
		return result;
	}
	
	public static BSUnitGraph buildGraphFor(SootMethod method){
		BSUnitGraph result = new BSUnitGraph();

		if(!SootMethodFilter.want(method)
				|| !method.hasActiveBody())
			return result;

		Body body = method.getActiveBody();
		BriefUnitGraph bug = new BriefUnitGraph(body);
		PatchingChain<Unit> units = body.getUnits();
		
		for(Unit unit : units){
			BSUnitNode node = new BSUnitNode(method, unit);
			List<Unit> succs = bug.getSuccsOf(unit);
			
			for(Unit succ : succs){
				BSUnitNode sucNode = new BSUnitNode(method, succ);
				result.addEdge(new BSUnitEdge(node, sucNode));
			}
		}
		return result;
	}

	public static BSUnitGraph buildUnitGraphFor(SootMethod method, Set<SootMethod> seen){
		BSUnitGraph result = new BSUnitGraph();

		if(!SootMethodFilter.want(method)
				|| !method.hasActiveBody() || seen.contains(method))
			return result;

		seen.add(method);
		Body body = method.getActiveBody();
		BriefUnitGraph bug = new BriefUnitGraph(body);
		PatchingChain<Unit> units = body.getUnits();
		
		for(Unit unit : units){
			BSUnitNode node = new BSUnitNode(method, unit);
			List<Unit> succs = bug.getSuccsOf(unit);
			
			for(Unit succ : succs){
				BSUnitNode sucNode = new BSUnitNode(method, succ);
				result.addEdge(new BSUnitEdge(node, sucNode));
			}
		}
		
		//replace invokes
		Set<BSUnitNode> removeSet = new HashSet<BSUnitNode>();
		Set<BSUnitEdge> newEdges = new HashSet<BSUnitEdge>();
		for(BSUnitNode node : result.getNodes()){
			Stmt stmt = (Stmt) node.getUnit();
			
			if(!stmt.containsInvokeExpr()) continue;

			SootMethod invokeM = stmt.getInvokeExpr().getMethod();
			if(invokeM.getDeclaringClass().isApplicationClass()
					&& invokeM.hasActiveBody()){
				BSUnitGraph graph = buildUnitGraphFor(invokeM, seen);
				Set<BSUnitNode> heads = graph.getHeads();
				Set<BSUnitNode> tails = graph.getTails();
				Set<BSUnitNode> preds = result.getPredsOf(node);
				Set<BSUnitNode> succs = result.getSuccsOf(node);

				for(BSUnitNode pred : preds){
					for(BSUnitNode head : heads){
						newEdges.add(new BSUnitEdge(pred, head));
					}
				}

				for(BSUnitNode succ : succs){
					for(BSUnitNode tail : tails){
						newEdges.add(new BSUnitEdge(tail, succ));
					}
				}
				
				removeSet.add(node);
			}
		}

		result.addEdges(newEdges);
		result.removeNodes(removeSet);
		
		return result;
	}

}
