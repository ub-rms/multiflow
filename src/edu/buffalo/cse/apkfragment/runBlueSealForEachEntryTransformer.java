/*
* this class is used to run BlueSeal on individual callgraph for every single entry point
 * 
 * this can only be called after generating sub-callgraph for each entry
 */

package edu.buffalo.cse.apkfragment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SceneTransformer;
import soot.SootMethod;
import edu.buffalo.cse.apkfragment.ClassFragmentation.BlueSealSingleRunForCallGraph;
import edu.buffalo.cse.apkfragment.callgraph.FragmentCallGraph;
import edu.buffalo.cse.apkfragment.struturalanalysis.FlowStructComplexity;
import edu.buffalo.cse.blueseal.BSG.Node;
import edu.buffalo.cse.blueseal.BSG.SinkNode;
import edu.buffalo.cse.blueseal.BSG.SourceNode;
import edu.buffalo.cse.blueseal.BSInfoFlowAnalysis.BSPostAnalysisExecutor;
import edu.buffalo.cse.blueseal.blueseal.Complexity.FlowComplexity;

public class runBlueSealForEachEntryTransformer extends SceneTransformer {
	
	public final static int PACKAGE = 0;
	public final static int CLASS = 1;
	public final static int METHOD = 2;
	public static	Map<SootMethod, Integer> ifLevel = new HashMap<SootMethod, Integer>();
	public static	Map<SootMethod, Integer> cyclomatic = new HashMap<SootMethod, Integer>();
	
	private void computenGrams(){
		//compute multi-flows
		Set<List<Node>> multiFlowPath = new HashSet<List<Node>>();
		for(Iterator<SourceNode> it = FlowComplexity.srcToMultiFlow.keySet().iterator(); it.hasNext();){
			multiFlowPath.addAll(FlowComplexity.srcToMultiFlow.get(it.next()));
		}

		for(Iterator<SinkNode> it = FlowComplexity.sinkToMultiFlow.keySet().iterator(); it.hasNext();){
			multiFlowPath.addAll(FlowComplexity.sinkToMultiFlow.get(it.next()));
		}
		
		FlowStructComplexity.computeFlowPlatformAPIs(multiFlowPath);
	}

	@Override
	protected void internalTransform(String arg0, Map<String, String> arg1) {
		BlueSealSingleRunForCallGraph singleRun = new BlueSealSingleRunForCallGraph(FragmentCallGraph.cg);
		BSPostAnalysisExecutor analyzer = singleRun.runBlueSeal();
		computenGrams();

	}

}
