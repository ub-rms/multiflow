/*
 * this class is to weight each method in a call graph
 */
package edu.buffalo.cse.apkfragment.weight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.itextpdf.text.pdf.hyphenation.TernaryTree.Iterator;

import edu.buffalo.cse.apkfragment.callgraph.FragmentCallGraph;
import edu.buffalo.cse.blueseal.BSFlow.SourceSinkDetection;
import soot.Body;
import soot.PatchingChain;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.CallGraph;

public class SimpleWeight {
	List<SootMethod> appMethods = new ArrayList<SootMethod>();
	List<SootMethod> srcMethods = new ArrayList<SootMethod>();
	List<SootMethod> sinkMethods = new ArrayList<SootMethod>();
	int totalWeights = 0;
	int totalNumOfMethods = 0;
	double sourceRatio = 0.0;
	double sinkRatio = 0.0;
	int totalNumberOfAppMethods = 0;
	int totalNumberOfLibraryMethods = 0;
	CallGraph cg;

	public SimpleWeight(CallGraph graph){
		cg = graph;
	}
	
	public int getTotalNumOfSourceMethods(){
		return this.srcMethods.size();
	}
	
	public int getTotalNumOfSinkMethods(){
		return this.sinkMethods.size();
	}
	
	public Set<String> getSourceAPISs(){
		Set<String> sourceAPISet = new HashSet<String>();
		for(SootMethod sm : srcMethods){
			String sig = sm.getSignature();
			sig = sig.replace("<", "");
			sig = sig.replace(">", "");
			sourceAPISet.add(sig);
		}
		return sourceAPISet;
	}
	
	public Set<String> getSinkAPISs(){
		Set<String> sinkAPISet = new HashSet<String>();
		for(SootMethod sm : sinkMethods){
			String sig = sm.getSignature();
			sig = sig.replace("<", "");
			sig = sig.replace(">", "");
			sinkAPISet.add(sig);
		}
		return sinkAPISet;
	}
	
	public void print(){
		System.out.println("total weights:"+totalWeights);
		System.out.println("total number of methods:"+totalNumOfMethods);
		System.out.println("source ratio:"+sourceRatio);
		System.out.println("sink ratio:"+sinkRatio);
	}
	
	public void calculate(){
		calculateWeights();
		calculateRatio();
	}
	
	public void calculateWeights(){
//		Set<SootMethod> reachable = new HashSet<SootMethod>();
		List<SootMethod> reachable = new ArrayList<SootMethod>();
		reachable.addAll(FragmentCallGraph.getReachableMethodsList(cg));

		totalNumOfMethods = reachable.size();
		
		for(SootMethod method : reachable){
			
			if(isApplicationMethod(method)){
				//totalWeights++;
				appMethods.add(method);
			}else if(isSourceMethod(method)){
				totalWeights+=10;
				srcMethods.add(method);
			}else if(isSinkMethod(method)){
				totalWeights+=5;
				sinkMethods.add(method);
			}else{
				
			}
		}
		
		totalNumberOfAppMethods = appMethods.size();
		totalNumberOfLibraryMethods = totalNumOfMethods - totalNumberOfAppMethods;
	}

	public void calculateRatio(){
		int totalMethodCount = srcMethods.size()+sinkMethods.size()+appMethods.size();
		sourceRatio = srcMethods.size()/(double)totalMethodCount;
		sinkRatio = sinkMethods.size()/(double)totalMethodCount;
	}
	
	/*
	 * for each application method invoke, weight 1
	 */
	public boolean isApplicationMethod(SootMethod method){
		if(method.getDeclaringClass().isApplicationClass()){
			return true;
		}
		
		return false;
	}
	
	/*
	 * for each source method invoke, weight 5
	 */
	public boolean isSourceMethod(SootMethod method){
		if(SourceSinkDetection.isSourceMethod(method)){
			return true;
		}
		
		return false;
	}
	
	/*
	 * for each sink method invoke, weight 10
	 */
	public boolean isSinkMethod(SootMethod method){
		if(SourceSinkDetection.isSinkMethod(method)){
			return true;
		}
		
		return false;
	}

	public int getTotalWeight() {
		
		return this.totalWeights;
	}

	public double getSrsRatio() {
		return this.sourceRatio;
	}

	public double getSinkRatio() {
		// TODO Auto-generated method stub
		return this.sinkRatio;
	}
	
	public int getTotalNumOfAppMethods(){
		return this.totalNumberOfAppMethods;
	}
	
	public int getTotalNumOfLibraryMethods(){
		return this.totalNumberOfLibraryMethods;
	}
}
