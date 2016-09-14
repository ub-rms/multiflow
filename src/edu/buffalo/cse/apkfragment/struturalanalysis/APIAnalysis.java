package edu.buffalo.cse.apkfragment.struturalanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.apkfragment.BlueSealUnitGraph.BSUnitEdge;
import edu.buffalo.cse.apkfragment.BlueSealUnitGraph.BSUnitGraph;
import edu.buffalo.cse.apkfragment.BlueSealUnitGraph.BSUnitGraphBuilder;
import edu.buffalo.cse.apkfragment.BlueSealUnitGraph.BSUnitNode;
import edu.buffalo.cse.blueseal.BSCallgraph.BSCallGraphTransformer;
import edu.buffalo.cse.blueseal.BSFlow.SootMethodFilter;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.toolkits.graph.BriefUnitGraph;

public class APIAnalysis {
	
	public static Map<SootMethod, Set<List<Unit>>> methodToPaths = 
			new HashMap<SootMethod, Set<List<Unit>>>();
	public static Unit dummyHeadUnit = null;
	public static int count = 0;

	private static Map<Unit, Set<List<Unit>>> unitToPaths =
			new HashMap<Unit, Set<List<Unit>>>();
	private static Map<SootMethod, Set<List<Unit>>> methodToAPIs =
			new HashMap<SootMethod, Set<List<Unit>>>();
	private static Set<List<String>> seenPaths = new HashSet<List<String>>();
	
	public static Set<List<String>> computeAPIPaths(SootMethod method, Stmt src, Set<SootMethod> seen){
		seen.add(method);
		seenPaths.clear();
		if(!methodToAPIs.containsKey(method)){
			Set<List<Unit>> result = computeAPISequenceFor(method, seen);
			methodToAPIs.put(method, result);
		}

		Set<List<Unit>> result = methodToAPIs.get(method);
		Set<List<String>> sequences = new HashSet<List<String>>();
		for(List<Unit> list : result){
			List<String> newList = new LinkedList<String>();
			for(Unit unit : list){
				Stmt stmt = (Stmt)unit;
				if(!stmt.containsInvokeExpr())
					continue;
				
				newList.add(stmt.getInvokeExpr().getMethod().getSignature());
			}
			sequences.add(newList);
		}
		return sequences;
	}

	private static Set<List<Unit>> computeAPISequenceFor(List<Unit> path, Set<SootMethod> seen) {
		Set<List<Unit>> result = new HashSet<List<Unit>>();
		Set<List<Unit>> worklist = new HashSet<List<Unit>>();
		worklist.add(path);
		
		while(true){
			if(worklist.size()==0){
				break;
			}

			Set<List<Unit>> temp = new HashSet<List<Unit>>();
			Path:
			for(List<Unit> curList : worklist){
				boolean changed = false;
				List<Unit> prefix = new LinkedList<Unit>();
				
				current:
				for(int i = 0; i < curList.size(); i++){
					Stmt stmt = (Stmt)curList.get(i);
	
					if(!stmt.containsInvokeExpr())
						continue current;
					
					SootMethod invokeM = stmt.getInvokeExpr().getMethod();
					if(invokeM.getDeclaringClass().isApplicationClass()){
						if(seen.contains(invokeM)){
							//skip to remove cycles
							curList.remove(i);
							temp.add(curList);
							continue Path;
						}
						changed = true;
						
						if(!methodToAPIs.containsKey(invokeM)){
							Set<List<Unit>> seqs = computeAPISequenceFor(invokeM, seen);
							methodToAPIs.put(invokeM, seqs);
						}

						List<Unit> postfix = new LinkedList<Unit>();
						for(int j = i+1; j < curList.size(); j++){
							postfix.add(curList.get(j));
						}
						
						if(methodToAPIs.get(invokeM).isEmpty()){
							List<Unit> newList = new LinkedList<Unit>();
							newList.addAll(prefix);
							newList.addAll(postfix);
							temp.add(newList);
							break current;
						}else{
							for(List<Unit> seq : methodToAPIs.get(invokeM)){
								List<Unit> newList = new LinkedList<Unit>();
								newList.addAll(prefix);
								newList.addAll(seq);
								newList.addAll(postfix);
								temp.add(newList);
							}
							break current;
						}
					}else{
						prefix.add(stmt);
					}
				}
				
				if(!changed && curList.size() > 0){
					result.add(curList);
				}
			}
			
			worklist = temp;
		}
		return result;
	}
	
	private static Set<List<Unit>> computeAPISequenceFor(SootMethod method, Set<SootMethod> seenSet) {
		Set<List<Unit>> result = new HashSet<List<Unit>>();
		seenSet.add(method);
		
		if(!methodToPaths.containsKey(method)){
			Set<List<Unit>> paths = computeMethodPaths(method);
			methodToPaths.put(method, paths);
		}

		Set<List<Unit>> paths = methodToPaths.get(method);
		HashSet<String> seen = new HashSet<String>();

		for(List<Unit> path : paths){

			if(path.size() == 0)
				continue;

			Set<List<Unit>> pathRes = computeAPISequenceFor(path, seenSet);
			for(List<Unit> list : pathRes){
				if(list.size() == 0)
					continue;

				StringBuilder sb = new StringBuilder();
				for(Unit unit : list){
					sb.append(unit.toString());
				}

				if(!seen.contains(sb.toString())){
					result.add(list);
					seen.add(sb.toString());
				}
			}
		}
		return result;
	}
	public static Set<List<String>> computedFrameAPIs(SootMethod method, Stmt src, Set<SootMethod> seen){
		Set<List<String>> result = new HashSet<List<String>>();
		BSUnitGraph graph = new BSUnitGraph();
		graph = buildUnitGraphFor(method, src, seen, 0);
		Set<List<BSUnitNode>> paths = BSUnitGraphBuilder.computeGraphPaths(graph);
		
		for(List<BSUnitNode> path : paths){
			List<String> list = new LinkedList<String>();
			for(BSUnitNode node : path){
				if(node.getUnit() == dummyHeadUnit) continue;

				Stmt stmt = (Stmt) node.getUnit();

				if(!stmt.containsInvokeExpr()){
					System.out.println("Stmt does not contain invokeExpr, something goes wrong!");
					continue;
				}
				
				SootMethod invokeM = stmt.getInvokeExpr().getMethod();
				if(!invokeM.getDeclaringClass().isApplicationClass()){
					list.add(invokeM.getSignature());
				}
			}
			result.add(list);
		}
		return result;
	}
	
	private static BSUnitGraph buildUnitGraphFor(SootMethod method,
			Stmt src, Set<SootMethod> seen, int level) {
		BSUnitGraph result = new BSUnitGraph();
		SootMethod dummyMain = BSCallGraphTransformer.dummyMain;
		if(dummyHeadUnit == null){
			Local newLocal = Jimple.v().newLocal("$r0", RefType.v("java.lang.String"));
			dummyHeadUnit = Jimple.v().newAssignStmt(newLocal, StringConstant.v("dummyMain"));
		}

		BSUnitNode dummyHead = new BSUnitNode(dummyMain, dummyHeadUnit);

		if(!SootMethodFilter.want(method)
				||!method.hasActiveBody()
				|| seen.contains(method)
				){
			return result;
		}

		seen.add(method);
		Set<List<Unit>> paths = computeMethodPaths(method);
		for(List<Unit> path : paths){
			if(path.size() == 0){
				continue;
			}
			
			if(src!=null && !path.contains(src)){
				continue;
			}

			Unit previous = path.get(0);
			BSUnitNode preNode = new BSUnitNode(method, previous);
			result.addEdge(new BSUnitEdge(dummyHead, preNode));

			if(path.size() == 1){
				result.addNode(preNode);
			}else{
				for(int i = 1; i < path.size(); i++){
					Unit next = path.get(i); 
					BSUnitNode nextNode = new BSUnitNode(method, next);
					result.addEdge(new BSUnitEdge(preNode, nextNode));
					preNode = nextNode;
				}
			}
		}
		
		//replace method invoke
		replaceAppMethodInvoke(result, seen, level);
		return result;
	}

	private static void replaceAppMethodInvoke(BSUnitGraph result, Set<SootMethod> seen, int level) {
		BSUnitGraph temp = new BSUnitGraph();
		temp.merge(result);

		for(BSUnitNode node : temp.getNodes()){
			Stmt stmt = (Stmt) node.getUnit();
			
			if(!stmt.containsInvokeExpr()) continue;

			SootMethod invokeM = stmt.getInvokeExpr().getMethod();
			if(invokeM.getDeclaringClass().isApplicationClass()
					&& invokeM.hasActiveBody()){
				BSUnitGraph graph = buildUnitGraphFor(invokeM, null, seen, level + 1);
				
				if(graph.getEdges().size() == 0){
					continue;
				}
				
				Set<BSUnitNode> heads = new HashSet<BSUnitNode>();
				for(BSUnitNode head : graph.getHeads()){
					heads.addAll(graph.getSuccsOf(head));
				}
				Set<BSUnitNode> tails = graph.getTails();
				Set<BSUnitNode> preds = result.getPredsOf(node);
				Set<BSUnitNode> succs = result.getSuccsOf(node);

				for(BSUnitNode pred : preds){
					for(BSUnitNode head : heads){
						result.addEdge(new BSUnitEdge(pred, head));
					}
				}

				for(BSUnitNode succ : succs){
					for(BSUnitNode tail : tails){
						result.addEdge(new BSUnitEdge(tail, succ));
					}
				}
				result.merge(graph);
				result.remove(node);
			}
		}

	}

	public static Set<List<Unit>> computeMethodPaths(SootMethod method){
		unitToPaths.clear();
		Set<List<Unit>> result = new HashSet<List<Unit>>();
		
		if(!methodToPaths.containsKey(method)){
			if(!SootMethodFilter.want(method)
					||!method.hasActiveBody()){
				return result;
			}
			
			Body body = method.getActiveBody();
			BriefUnitGraph bug = new BriefUnitGraph(body);

			for(Unit head : bug.getHeads()){
				Set<Unit> seen = new HashSet<Unit>();
				Set<List<Unit>> headRes = DepthFirstVisit(head, seen, bug);
				if(headRes.size()!=0){
					result.addAll(headRes);
				}
			}
			methodToPaths.put(method, result);
		}
		return methodToPaths.get(method);
	}
	
	private static Set<List<Unit>> DepthFirstVisit(Unit head,
			Set<Unit> seen, BriefUnitGraph bug) {
		boolean wanted = false;
		if(((Stmt)head).containsInvokeExpr()){
			SootMethod invokeM = ((Stmt)head).getInvokeExpr().getMethod();
			SootClass sc = invokeM.getDeclaringClass();

			if((sc.isApplicationClass()&&invokeM.hasActiveBody())
						|| sc.getName().startsWith("android.")
						|| sc.getName().startsWith("org.apache.http")){
				wanted = true;
			}
		}
		Set<List<Unit>> result = new HashSet<List<Unit>>();
		seen.add(head);

		if(bug.getTails().contains(head)){
			if(wanted){
					List<Unit> list = new LinkedList<Unit>();
					list.add(head);
					result.add(list);
			}
		}else{
			for(Unit succ : bug.getSuccsOf(head)){
				if(!seen.contains(succ)){
					Set<Unit> newSeen = new HashSet<Unit>();
					newSeen.addAll(seen);
				
					if(!unitToPaths.containsKey(succ)){
						Set<List<Unit>> sucRes = DepthFirstVisit(succ, newSeen, bug);
						unitToPaths.put(succ, sucRes);
					}
	
					if(wanted){
						for(List<Unit> list : unitToPaths.get(succ)){
							List<Unit> newList = new LinkedList<Unit>();
							newList.add(head);
							newList.addAll(list);
							result.add(newList);
						}
					}else{
						result.addAll(unitToPaths.get(succ));
					}
				}
			}

			if(wanted && result.size()==0){
				List<Unit> list = new LinkedList<Unit>();
				list.add(head);
				result.add(list);
			}
		}
		Set<List<Unit>> temp = new HashSet<List<Unit>>();
		Set<String> tempSeen = new HashSet<String>();
		for(List<Unit> path : result){
			if(!tempSeen.contains(path.toString())){
				temp.add(path);
				tempSeen.add(path.toString());
			}
		}
		result = temp;
		return result;
	}

}
