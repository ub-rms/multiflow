/*
 * this class is based on other more detailed leveled weighting scheme
 * 
 * group the simple weighting scheme by class classifers
 */
package edu.buffalo.cse.apkfragment.weight;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.SootClass;
import soot.SootMethod;

public class ClassLevelWeighting {
	
	Map<SootClass, ClassWeightingInfo> map;
	
	public ClassLevelWeighting(){
		map = new HashMap<SootClass, ClassWeightingInfo>();
	}
	
	public void calculate(Map entryToWeighting){
		for(Iterator it = entryToWeighting.keySet().iterator(); it.hasNext();){
			SootMethod entry = (SootMethod) it.next();
			
			//ToDo: currently we only have one weighting scheme, simpleWeighting
			//if we implement more, we need to change this
			SimpleWeight sw = (SimpleWeight) entryToWeighting.get(entry);
			SootClass sootClass = entry.getDeclaringClass();
			ClassWeightingInfo info;
			
			if(!map.containsKey(sootClass)){
				info = new ClassWeightingInfo();
				map.put(sootClass, info);
			}
			
			info = map.get(sootClass);
			info.addWeight(sw.totalWeights);
			info.addNewAppMethods(sw.appMethods);
			info.addNewSourceMethods(sw.srcMethods);
			info.addNewSinkMethods(sw.sinkMethods);
			info.calculate();
		}
	}
	
	public void print() {
		for(Iterator it = map.keySet().iterator(); it.hasNext();){
			SootClass sootClass = (SootClass) it.next();
			ClassWeightingInfo info = map.get(sootClass);
			System.out.println("for soot class:" +
					sootClass.getName());
			info.print();
		}
	}
	
	private class ClassWeightingInfo{
		public int weight;
		public double sourceRatio;
		public double sinkRatio;
		public List<SootMethod> appMethods;
		public List<SootMethod> sourceMethods;
		public List<SootMethod> sinkMethods;
		
		public ClassWeightingInfo(){
			weight = 0;
			sourceRatio = 0.0;
			sinkRatio = 0.0;
			appMethods = new ArrayList<SootMethod>();
			sourceMethods = new ArrayList<SootMethod>();
			sinkMethods = new ArrayList<SootMethod>();
		}
		
		public void print() {
			System.out.println("total weights:"+weight);
			System.out.println("source ratio:"+sourceRatio);
			System.out.println("sink ratio:"+sinkRatio);
			
		}

		public void addWeight(int newWeight){
			this.weight+=newWeight;
		}
		
		public void addNewAppMethods(List<SootMethod> newSet){
			this.appMethods.addAll(newSet);
		}
		
		public void addNewSourceMethods(List<SootMethod> newSet){
			this.sourceMethods.addAll(newSet);
		}
		
		public void addNewSinkMethods(List<SootMethod> newSet){
			this.sinkMethods.addAll(newSet);
		}
		
		public void calculate(){
			int totalNumber = appMethods.size() + sourceMethods.size() + sinkMethods.size();
			sourceRatio = sourceMethods.size()/(double)totalNumber;
			sinkRatio = sinkMethods.size()/(double)totalNumber;
		}
	}


}
