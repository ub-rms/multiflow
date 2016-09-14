package edu.buffalo.cse.apkfragment.ClassFragmentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.buffalo.cse.apkfragment.callgraph.FragmentCallGraph;
import edu.buffalo.cse.blueseal.BSFlow.BSInterproceduralAnalysis;
import edu.buffalo.cse.blueseal.BSFlow.SootMethodFilter;
import edu.buffalo.cse.blueseal.BSInfoFlowAnalysis.BSPostAnalysisExecutor;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;

public class BlueSealSingleRunForCallGraph {
	public CallGraph cg;
	public static BSInterproceduralAnalysis inter_;
	
  public BlueSealSingleRunForCallGraph(CallGraph callgraph){
    cg = callgraph;
  }
	
	public void run(){
		BSPostAnalysisExecutor executor = runBlueSeal();
		List<String> flows = executor.getFinalBSG().generateFlowStrings();
		Set<String> fps = executor.getFPs();
		//remove log sinks and recalculate the flows and FPs
		List<String> flowsWithoutLog = new ArrayList<String>();
		for(String flow : flows){
			if(flow.contains("android.util.Log")
					|| flow.contains("android.content.Intent"))
				continue;
			
			flowsWithoutLog.add(flow);
		}
	}
	
	
	/*
	 *  method to run BlueSeal on a single sub-callgraph
	 */
	public BSPostAnalysisExecutor runBlueSeal(){
		//we need a reachable methods from the callgraph which will be used later
		Set<SootMethod> reachableMethods = FragmentCallGraph.getReachableMethods(cg);
		 //do the analysis
		inter_ = new BSInterproceduralAnalysis(cg, new SootMethodFilter(null), 
				reachableMethods.iterator(), false);
		/*
		 * after analysis, we should post-process the results
		 * what we want is to see which fragment contains blueseal flow
		 */
		BSPostAnalysisExecutor analyzer = new BSPostAnalysisExecutor(inter_);
		analyzer.execute(reachableMethods);
		
		return analyzer;
	}
	
}
