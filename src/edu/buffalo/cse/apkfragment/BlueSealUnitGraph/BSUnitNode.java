package edu.buffalo.cse.apkfragment.BlueSealUnitGraph;

import soot.SootMethod;
import soot.Unit;

public class BSUnitNode {
	SootMethod method;
	Unit unit;
	
	public BSUnitNode(SootMethod m, Unit u){
		method = m;
		unit = u;
	}
	
	public Unit getUnit(){
		return unit;
	}
	
	public SootMethod getMethod(){
		return method;
	}
	
	public int hashCode(){
		return unit.toString().hashCode()+method.getSignature().hashCode();
	}
	
	public boolean equals(Object o){
		if(!(o instanceof BSUnitNode))
			return false;

		BSUnitNode node = (BSUnitNode) o;
		return method.getSignature().equals(node.getMethod().getSignature())
				&& unit.toString().equals(node.getUnit().toString());
		
	}
}
