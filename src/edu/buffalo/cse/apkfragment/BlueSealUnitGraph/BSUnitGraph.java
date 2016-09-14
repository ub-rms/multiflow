package edu.buffalo.cse.apkfragment.BlueSealUnitGraph;

import java.util.HashSet;
import java.util.Set;

import soot.SootMethod;
import soot.jimple.Stmt;

public class BSUnitGraph {
	private Set<BSUnitNode> nodes = new HashSet<BSUnitNode>();
	private Set<BSUnitEdge> edges = new HashSet<BSUnitEdge>();
	
	public BSUnitGraph(){}
	
	public Set<BSUnitNode> getNodes(){
		return nodes;
	}
	
	public Set<BSUnitEdge> getEdges(){
		return edges;
	}
	
	public void addEdge(BSUnitEdge edge){
		nodes.add(edge.getSrc());
		nodes.add(edge.getTgt());
		edges.add(edge);
	}
	
	public Set<BSUnitNode> getHeads(){
		Set<BSUnitNode> heads = new HashSet<BSUnitNode>();
		
		for(BSUnitNode node : nodes){
			boolean edgeinto = false;
			
			for(BSUnitEdge edge : edges){
				if(edge.getTgt().equals(node)){
					edgeinto = true;
					break;
				}
			}
			
			if(!edgeinto){
				heads.add(node);
			}
			
		}
		return heads;
	}
	
	public Set<BSUnitNode> getTails(){
		Set<BSUnitNode> tails = new HashSet<BSUnitNode>();
		
		for(BSUnitNode node : nodes){
			boolean edgeout = false;
			
			for(BSUnitEdge edge : edges){
				if(edge.getSrc().equals(node)){
					edgeout = true;
					break;
				}
			}
			
			if(!edgeout){
				tails.add(node);
			}
			
		}
		return tails;
	}
	
	public void add(BSUnitGraph graph) {
		edges.addAll(graph.getEdges());
	}
	
	public Set<BSUnitNode> getSuccsOf(BSUnitNode node) {
		Set<BSUnitNode> result = new HashSet<BSUnitNode>();

		for(BSUnitEdge edge : edges){
			if(edge.getSrc().equals(node)){
				result.add(edge.getTgt());
			}
		}
		return result;
	}
	
	public Set<BSUnitNode> getPredsOf(BSUnitNode node) {
		Set<BSUnitNode> result = new HashSet<BSUnitNode>();

		for(BSUnitEdge edge : edges){
			if(edge.getTgt().equals(node)){
				result.add(edge.getSrc());
			}
		}
		return result;
	}
	
	public int hashCode(){
		return nodes.hashCode()+edges.hashCode();
	}
	
	public boolean equals(Object o){
		if(!(o instanceof BSUnitGraph))
			return false;
		
		BSUnitGraph g = (BSUnitGraph) o;
		return nodes.equals(g.getNodes())
				&& edges.equals(g.getEdges());
	}

	public void remove(BSUnitNode node) {
		Set<BSUnitEdge> removeSet = new HashSet<BSUnitEdge>();
		for(BSUnitEdge edge : edges){
			if(edge.getSrc().equals(node)||
					edge.getTgt().equals(node)){
				removeSet.add(edge);
			}
		}
		edges.removeAll(removeSet);
		nodes.remove(node);
	}

	public void connect(BSUnitGraph graph) {
		Set<BSUnitNode> tails = getTails();
		Set<BSUnitNode> heads = graph.getHeads();
		
		for(BSUnitNode tail : tails){
			for(BSUnitNode head : heads){
				edges.add(new BSUnitEdge(tail, head));
			}
		}
		
		edges.addAll(graph.getEdges());
	}

	public void merge(BSUnitGraph graph) {
		nodes.addAll(graph.getNodes());
		edges.addAll(graph.getEdges());
	}

	public void addEdges(Set<BSUnitEdge> newEdges) {
		for(BSUnitEdge edge : newEdges){
			addEdge(edge);
		}
	}

	public void removeNodes(Set<BSUnitNode> removeSet) {
		for(BSUnitNode node : removeSet){
			remove(node);
		}
	}

	public void addNode(BSUnitNode node) {
		nodes.add(node);
	}

	public BSUnitNode getNode(Stmt stmt, SootMethod method) {
		BSUnitNode result = null;
		for(BSUnitNode node : nodes){
			if(node.getUnit().equals(stmt)
					&& node.getMethod().equals(method)){
				result = node;
			}
		}
		return result;
	}

	public void remove(BSUnitEdge bsUnitEdge) {
		edges.remove(bsUnitEdge);
	}

}
