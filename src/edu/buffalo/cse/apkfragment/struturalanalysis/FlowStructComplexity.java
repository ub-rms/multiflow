package edu.buffalo.cse.apkfragment.struturalanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.buffalo.cse.blueseal.BSFlow.SootMethodFilter;
import edu.buffalo.cse.blueseal.BSFlow.SourceSinkDetection;
import edu.buffalo.cse.blueseal.BSG.CVNode;
import edu.buffalo.cse.blueseal.BSG.Node;
import edu.buffalo.cse.blueseal.BSG.SinkNode;
import edu.buffalo.cse.blueseal.BSG.SourceNode;
import edu.buffalo.cse.blueseal.blueseal.Complexity.FlowComplexity;

import soot.Body;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IfStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.BriefUnitGraph;

public class FlowStructComplexity {
	
	public static Map<List<Node>, Set<List<String>>> flowAPIInfo = new HashMap<List<Node>, Set<List<String>>>();
	public static Map<SourceNode, Set<SootMethod>> branchedSinks = new HashMap<SourceNode, Set<SootMethod>>();
	
	private static Map<SootMethod, Set<List<String>>> methodToAPIs = new HashMap<SootMethod, Set<List<String>>>();
	
	public static void computeBranchedSinkFromSameSource(Set<List<Node>> set){
		Set<SourceNode> allSources = new HashSet<SourceNode>();
		for(List<Node> path : set){
			SourceNode src = (SourceNode) path.get(0);
			allSources.add(src);
		} 
		
		
		for(SourceNode src : allSources){
			if(FlowComplexity.srcToMultiSink.get(src).size() < 2)
				continue;
			
			Set<SinkNode> multiSinks = FlowComplexity.srcToMultiSink.get(src);
			Set<SootMethod> methods = getAllFlowMethodsFor(src, set);

			for(SootMethod method : methods){
				if(!SootMethodFilter.want(method))
					continue;

			}
		}
	}

	private static Set<SootMethod> getReachableSinks(
			SootMethod method, Set<SootMethod> seen) {
		Set<SootMethod> result = new HashSet<SootMethod>();
		seen.add(method);
		
		if(!method.hasActiveBody())
			return result;

		Body body = method.getActiveBody();
		PatchingChain<Unit> units = body.getUnits();

		for(Unit unit : units){
			Stmt stmt = (Stmt)unit;
			
			if(!stmt.containsInvokeExpr())
				continue;
			
			SootMethod invokeM = stmt.getInvokeExpr().getMethod();
			
			if(invokeM.getDeclaringClass().isApplicationClass()){
				if(!seen.contains(invokeM))
					result.addAll(getReachableSinks(invokeM, seen));
			}else{
				if(SourceSinkDetection.isSinkMethod(invokeM)){
					result.add(invokeM);
				}
			}
		}
		return result;
	}

	private static Set<SootMethod> getAllFlowMethodsFor(SourceNode src,
			Set<List<Node>> set) {
		Set<SootMethod> result = new HashSet<SootMethod>();

		for(List<Node> path : set){
			if(path.get(0).equals(src)){
				result.addAll(FlowComplexity.getFlowMethods(path));
			}
		}
		return result;
	}

	public static void computeFlowPlatformAPIs(Set<List<Node>> set){
		Set<String> seenFlow = new HashSet<String>();
		Flow:
		for(List<Node> path : set){
			if(path.size()==0) continue;

			LinkedList<Node> srcList = new LinkedList<Node>();
			LinkedList<Node> sinkList = new LinkedList<Node>();
			
//			if(path.get(0).getName().contains("getDeviceId")
//					|| path.get(0).getName().contains("getSubscriberId")){
//				
//			}else{
//				continue Flow;
//			}
			
			for(Node node : path){
				if(node instanceof SourceNode){
					srcList.addFirst(node);
				}else if(node instanceof SinkNode){
					sinkList.add(node);
				}else if(node instanceof CVNode){
					continue Flow;
				}
			}

			Set<SootMethod> seen = new HashSet<SootMethod>();
			SourceNode src = (SourceNode) srcList.get(0);
			SinkNode sink = (SinkNode) sinkList.get(0);
			Set<List<String>> srcPath = new HashSet<List<String>>();
			Set<List<String>> sinkPath = new HashSet<List<String>>();
			Set<List<String>> result = new HashSet<List<String>>();
			
			String methodString = src.getMethod().getSignature()+"->"+sink.getMethod().getSignature();
			if(seenFlow.contains(methodString)){
				flowAPIInfo.put(path, result);
				continue Flow;
			}else{
				seenFlow.add(methodString);
			}

			System.out.println(" path starts");
			if(src.getMethod() != null && !seen.contains(src.getMethod())){
				if(!methodToAPIs.containsKey(src.getMethod())){
					Set<List<String>> apis = APIAnalysis.computeAPIPaths(src.getMethod(), src.getStmt(), seen);
					methodToAPIs.put(src.getMethod(), apis);
				}
				srcPath.addAll(methodToAPIs.get(src.getMethod()));
			}

			if(sink.getMethod() != null && !seen.contains(sink.getMethod())
					&& !sink.getMethod().getSignature().equals(src.getMethod().getSignature())){
				Set<SootMethod> sinkSeen = new HashSet<SootMethod>();
				if(!methodToAPIs.containsKey(sink.getMethod())){
					Set<List<String>> apis = APIAnalysis.computeAPIPaths(sink.getMethod(), sink.getStmt(), sinkSeen);
					methodToAPIs.put(sink.getMethod(), apis);
				}
				sinkPath.addAll(methodToAPIs.get(sink.getMethod()));
			}
			
			if(sinkPath.size()!=0){
				for(List<String> srcP : srcPath){
					for(List<String> sinkP : sinkPath){
						List<String> newList = new LinkedList<String>();
						newList.addAll(srcP);
						newList.addAll(sinkP);
						result.add(newList);
					}
				}
			}else{
				result.addAll(srcPath);
			}

			flowAPIInfo.put(path, result);
			System.out.println(" path done");
		}
	}

	private static LinkedList<SootMethod> getSootMethodListFromPath(List<Node> path) {
		LinkedList<SootMethod> list = new LinkedList<SootMethod>();
		
		for(Node node : path){
			if(node instanceof CVNode) continue;
			
			if(node instanceof SourceNode){
				list.add(((SourceNode)node).getMethod());
			}
			
			if(node instanceof SinkNode){
				list.add(((SinkNode)node).getMethod());
			}
		}
		return list;
	}

}
