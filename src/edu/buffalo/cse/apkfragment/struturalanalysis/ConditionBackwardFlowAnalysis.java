package edu.buffalo.cse.apkfragment.struturalanalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Local;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.IdentityStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.ParameterRef;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.scalar.ArraySparseSet;
import soot.toolkits.scalar.SimpleLocalDefs;
import edu.buffalo.cse.apkfragment.ClassFragmentation.BlueSealSingleRunForCallGraph;
import edu.buffalo.cse.apkfragment.callgraph.FragmentCallGraph;
import edu.buffalo.cse.blueseal.BSFlow.BSInterproceduralAnalysis;
import edu.buffalo.cse.blueseal.BSFlow.SootMethodFilter;

public class ConditionBackwardFlowAnalysis {
	
	public static Set<Unit> analyseCondition(Stmt ifStmt, SootMethod method){
		Set<Unit> result = new HashSet<Unit>();
		
		if(!SootMethodFilter.want(method)) return result;

		Set<SootMethod> seen = new HashSet<SootMethod>();
		result.addAll(analyseCondition(ifStmt, method, seen));
		return result;
	}

	private static Set<Unit> analyseCondition(Stmt ifStmt,
			SootMethod method, Set<SootMethod> seen) {
		Set<Unit> result = new HashSet<Unit>();
		seen.add(method);

		if(!SootMethodFilter.want(method)||
				!method.hasActiveBody())
			return result;

		BSInterproceduralAnalysis inter = BlueSealSingleRunForCallGraph.inter_;
		Map<Unit, ArraySparseSet> sum = inter.getMethodSummary().get(method);
		ArraySparseSet flowInto = sum.get(ifStmt);

		for(Iterator it = flowInto.iterator(); it.hasNext();){
			Stmt stmt = (Stmt)it.next();
			
			if(stmt.containsInvokeExpr()&&
					stmt.getInvokeExpr().getMethod().getDeclaringClass().isApplicationClass()){
				if(!seen.contains(stmt.getInvokeExpr().getMethod())){
					result.addAll(getAllUnitsToRet(stmt.getInvokeExpr().getMethod(), seen));
				}
			}else if(isParameter(stmt)){
				Value para = ((IdentityStmt)stmt).getRightOp();
				int index = ((ParameterRef)para).getIndex();
				//here, get all possible callees
				Set<Edge> edges = new HashSet<Edge>();
				
				for(Iterator test = FragmentCallGraph.cg.iterator(); test.hasNext(); ){
					Edge e = (Edge) test.next();
					if(e.getTgt().method().getSignature().equals(method.getSignature())){
						edges.add(e);
					}
				}
				for(Edge edge : edges){
					SootMethod caller = edge.getSrc().method();
					result.addAll(getAllUnitsToPara(caller, method, index, seen));
				}
			}else{
				result.add(stmt);
			}
		}
		
		return result;
	}
	
	private static Set<Unit> analyseStmt(Stmt stmt, SootMethod method, Set<SootMethod> seen){
		Set<Unit> result = new HashSet<Unit>();

		if(!SootMethodFilter.want(method)||
				!method.hasActiveBody())
			return result;

		seen.add(method);
		
		if(stmt.containsInvokeExpr()&&
				stmt.getInvokeExpr().getMethod().getDeclaringClass().isApplicationClass()){
			if(!seen.contains(stmt.getInvokeExpr().getMethod()))
				result.addAll(getAllUnitsToRet(stmt.getInvokeExpr().getMethod(), seen));
		}else if(isParameter(stmt)){
			Value para = ((IdentityStmt)stmt).getRightOp();
			int ind = ((ParameterRef)para).getIndex();
			//here, get all possible callees
			Iterator<Edge> edges = FragmentCallGraph.cg.edgesInto(method);
			for(Iterator<Edge> eit = edges; eit.hasNext();){
				Edge edge = eit.next();
				SootMethod call = edge.getSrc().method();
				if(!seen.contains(call))
					result.addAll(getAllUnitsToPara(call, method, ind, seen));
			}
		}else{
			result.add(stmt);
		}
		return result;
	}

	private static Set<Unit> getAllUnitsToPara(
			SootMethod caller, SootMethod callee, int index, Set<SootMethod> seen) {
		Set<Unit> result = new HashSet<Unit>();
		seen.add(caller);

		if(!SootMethodFilter.want(caller)||
				!caller.hasActiveBody())
			return result;
		
		Body body = caller.getActiveBody();
		BriefUnitGraph bug = new BriefUnitGraph(body);
		SimpleLocalDefs sld = new SimpleLocalDefs(bug);
		PatchingChain<Unit> units = caller.getActiveBody().getUnits();

		for(Unit unit : units){
			Stmt stmt = (Stmt)unit;

			if(!stmt.containsInvokeExpr()) continue;

			InvokeExpr expr = stmt.getInvokeExpr();
			SootMethod invokeM = expr.getMethod();
			if(invokeM.equals(callee)){
				Value param = expr.getArg(index);
				
				if(!(param instanceof Local)) continue;

				List<Unit> defs = sld.getDefsOfAt((Local) param, unit);
				for(Unit def : defs){
					result.addAll(analyseStmt((Stmt) def, caller, seen));
				}
			}
		}
		return result;
	}

	private static boolean isParameter(Stmt stmt) {
		if(stmt instanceof IdentityStmt){
			Value rightOp = ((IdentityStmt)stmt).getRightOp();
			if(rightOp instanceof ParameterRef){
				return true;
			}
		}
		return false;
	}

	private static Set<Unit> getAllUnitsToRet(SootMethod method, Set<SootMethod> seen) {
		seen.add(method);
		Set<Unit> result = new HashSet<Unit>();

		if(!SootMethodFilter.want(method)||
				!method.hasActiveBody())
			return result;

		BSInterproceduralAnalysis inter = BlueSealSingleRunForCallGraph.inter_;
		Map<Unit, ArraySparseSet> sum = inter.getMethodSummary().get(method);
		PatchingChain<Unit> units = method.getActiveBody().getUnits();

		for(Unit unit : units){
			if(unit instanceof ReturnStmt){
				ArraySparseSet flowInto = sum.get(unit);
				for(Iterator it = flowInto.iterator(); it.hasNext();){
					Stmt stmt = (Stmt)it.next();
					result.addAll(analyseStmt(stmt, method, seen));
				}
			}
		}
		return result;
	}

}
