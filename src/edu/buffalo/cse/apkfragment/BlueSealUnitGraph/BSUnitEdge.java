package edu.buffalo.cse.apkfragment.BlueSealUnitGraph;

public class BSUnitEdge {
	private BSUnitNode src;
	private BSUnitNode tgt;
	
	public BSUnitEdge(BSUnitNode s, BSUnitNode t){
		src = s;
		tgt = t;
	}
	
	public BSUnitNode getSrc(){
		return src;
	}

	public BSUnitNode getTgt(){
		return tgt;
	}
	
	public int hashCode(){
		return src.hashCode() + tgt.hashCode();
	}
	
	public boolean equals(Object o){
		if(!(o instanceof BSUnitEdge)) return false;
		
		BSUnitEdge edge = (BSUnitEdge)o;
		
		return src.equals(edge.getSrc())&&tgt.equals(edge.getTgt());
	}

	public void print() {
		System.out.println(src.getUnit().toString()+"->"+tgt.getUnit().toString());
	}

}
