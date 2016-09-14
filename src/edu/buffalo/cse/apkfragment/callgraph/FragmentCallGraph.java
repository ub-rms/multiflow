/*
 * this class defines the callgraph that will be used in apkFragment
 * 
 * in order to deploy the complete callgraph, we re-use the one from BlueSeal project
 */

package edu.buffalo.cse.apkfragment.callgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.PatchingChain;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.util.queue.QueueReader;
import edu.buffalo.cse.apkfragment.ApkFragmentOptions;
import edu.buffalo.cse.blueseal.BSCallgraph.BSCallGraphTransformer;

public class FragmentCallGraph extends BSCallGraphTransformer {
	
	public static Map<SootMethod, CallGraph> entryToCallgraph;
	public static Map<SootClass, Set<Edge>> classToCallGraph;
	public static Map<String, Set<Edge>> packToCallGraph;
	
	public FragmentCallGraph(String apk){
		super(apk);
		entryToCallgraph = new HashMap<SootMethod, CallGraph>();
		classToCallGraph = new HashMap<SootClass, Set<Edge>>();
		packToCallGraph = new HashMap<String, Set<Edge>>();
		
		if(ApkFragmentOptions.Spark){
			super.setSparkOn();
		}
	}
	
	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1){
		super.internalTransform(arg0, arg1);
	}
	

	
	public static Set<SootMethod> getReachableMethods(CallGraph cg) {
		Set<SootMethod> set = new HashSet<SootMethod>();
		
		if(cg == null){
			System.out.println(" callgraph is null");
			return set;
		}
		QueueReader<Edge> reader = cg.listener();
		
		while(reader.hasNext()){
			Edge e = reader.next();
			SootMethod srcM = e.getSrc().method();
			SootMethod tgtM = e.getTgt().method();
			set.add(srcM);
			set.add(tgtM);
		} 
		
		return set;
	}
	
	
	/*
	 * this will get a set of reachable methods of given callgraph 
	 * 
	 */
	public static List<SootMethod> getReachableMethodsList(CallGraph cg) {
		Set<SootMethod> set = new HashSet<SootMethod>();
		
		if(cg == null){
			System.out.println(" callgraph is null");
			return new ArrayList<SootMethod>();
		}
		QueueReader<Edge> reader = cg.listener();
		
		while(reader.hasNext()){
			Edge e = reader.next();
			SootMethod srcM = e.getSrc().method();
			SootMethod tgtM = e.getTgt().method();
			set.add(srcM);
			set.add(tgtM);
		} 
		
		return updateReachable(set);
	}
	
	private static List<SootMethod> updateReachable(Set<SootMethod> reachable) {
		List<SootMethod> updatedReachable = new ArrayList<SootMethod>();
//		updatedReachable.addAll(reachable);
		// this is to update the set of reachable methods
		// by adding all library method invokes
		for(Iterator mit = reachable.iterator(); mit.hasNext();){
			SootMethod method = (SootMethod) mit.next();
			if(method.getDeclaringClass().isApplicationClass()){
				
				if(!method.hasActiveBody()) continue;
				
				Body body = method.getActiveBody();
				PatchingChain<Unit> units = body.getUnits();
				
				for(Iterator<Unit> it = units.iterator(); it.hasNext();){
					Stmt stmt = (Stmt)it.next();
					
					if(stmt.containsInvokeExpr()){
						InvokeExpr invokeExpr = stmt.getInvokeExpr();
						SootMethod invokeM = invokeExpr.getMethod();
						updatedReachable.add(invokeM);
					}
				}
			}
		}
		return updatedReachable;
	}
}