package edu.buffalo.cse.apkfragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import soot.Body;
import soot.Hierarchy;
import soot.IntType;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.NullConstant;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.jimple.toolkits.callgraph.CHATransformer;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.scalar.SimpleLiveLocals;
import soot.toolkits.scalar.SmartLocalDefs;
import soot.util.Chain;
import soot.util.queue.QueueReader;

public class CgTransformer extends SceneTransformer {

	public static Set<SootMethod>
		reachableMethods_  = new HashSet<SootMethod>();
	public static List<SootMethod> entryPoints
						= new LinkedList<SootMethod>();
	
    private String apkLoc = null;
    private Scene scene = null;

    public CgTransformer(String al) {
        apkLoc = al;
    }
    
    @Override
    protected void internalTransform(String arg0, Map arg1) {
        removeAndroidAutoGenClasses();
        entryPoints = getEntryPoints();
        entryPoints.addAll(getDynamicEntryPoints(entryPoints));
        Scene.v().setEntryPoints(entryPoints);
        CHATransformer.v().transform();
        
        CallGraph cg = Scene.v().getCallGraph();
    }

    private List<SootMethod> getDynamicEntryPoints(List<SootMethod> initialEntryPoints) {
        ArrayList<SootMethod> returnList = new ArrayList<SootMethod>();
        LayoutFileParser layoutParser = new LayoutFileParser(apkLoc);
        Map<String, String> idToFile = layoutParser.getIdToFile();
        Map<String, Set<String>> functionsFromXmlFile = layoutParser.getFunctionsFromXmlFile();

        Scene.v().setEntryPoints(initialEntryPoints);
        CHATransformer.v().transform();

        ReachableMethods rm = Scene.v().getReachableMethods();
        QueueReader<MethodOrMethodContext> rmIt = rm.listener();

        while (rmIt.hasNext()) {
            SootMethod method = rmIt.next().method();
            if (!method.hasActiveBody())
                continue;

            Body body = method.getActiveBody();

            for (Iterator<Unit> unitIt = body.getUnits().iterator(); unitIt.hasNext();) {
                Stmt stmt = (Stmt) unitIt.next();

                if (!stmt.containsInvokeExpr())
                    continue;

                SootMethodRef methodRef = stmt.getInvokeExpr().getMethodRef();
                if (!methodRef.name().contains("setContentView"))
                    continue;
                if(stmt.getInvokeExpr().getArgCount() <= 0) continue;
                Value param = stmt.getInvokeExpr().getArg(0);
                if (!(param.getType() instanceof IntType))
                    continue;

                String fileName = null;
                try { 
                    int layoutIdInt = Integer.parseInt(stmt.getInvokeExpr().getArg(0).toString());
                
                    fileName = idToFile.get("0x" + Integer.toHexString(layoutIdInt));
                } catch (NumberFormatException e) {
                    // TODO: Right now we're only tracing back within the same method.
                    ExceptionalUnitGraph eug = new ExceptionalUnitGraph(body);
                    SmartLocalDefs localDefs = new SmartLocalDefs(eug, new SimpleLiveLocals(eug));

                    List<Unit> defs = localDefs.getDefsOfAt((Local) stmt.getUseBoxes().get(1).getValue(), stmt);
                    
//                    if (defs.size() != 1) {
//                        System.err.println("Warning: DynamicEntryPoint got more than one definition site!");
//                    }
        
                    Stmt defStmt = (Stmt) defs.get(defs.size() - 1);
                    Value rV = defStmt.getUseBoxes().get(0).getValue();
                    if (rV instanceof StaticFieldRef) {
                        fileName = ((StaticFieldRef) rV).getFieldRef().name();
                    } else {
                        // TODO: This requires backward flow analysis to trace back where it's coming from
                        //System.err.println("Warning: DynamicEntryPoint skips " + defStmt.toString());
                    }
                }

                if (!functionsFromXmlFile.containsKey(fileName))
                    // TODO: this means that we might be skipping some layout files
                    continue;

                for (Iterator<String> it = functionsFromXmlFile.get(fileName).iterator();
                        it.hasNext();) {
                    String signature = "<" + methodRef.declaringClass().getName() + ": void "
                            + it.next() + ">";

                    try {
                        returnList.add(Scene.v().getMethod(signature));
                    } catch (RuntimeException e) {
                        System.err.println("Warning: DynamicEntryPoint cannot find " + signature + " (signature is perhaps wrong)");
                    }
                }
            }
        }

        return returnList;
    }

    /*
     * retrieve all the entry points in the application
     */
    private List<SootMethod> getEntryPoints() {
        // TODO Auto-generated method stub
        List<SootMethod> entryPoints = new ArrayList<SootMethod>();
        Chain<SootClass> classes = Scene.v().getApplicationClasses();
        Map<String, Set<String>> epMap = new EntryPointsMapLoader().getEPMap();
        for (SootClass sc : classes) {
            List<SootClass> superTypes = getSuperTypes(sc);
            entryPoints.addAll(getEntryMethods(sc, superTypes, epMap));
        }

        return entryPoints;

    }

    /*
     * remove all the Anroid auto generated classes
     */
    private void removeAndroidAutoGenClasses() {
        // TODO Auto-generated method stub
        Set<SootClass> classesToRemove = new HashSet<SootClass>();
        for (SootClass clazz : Scene.v().getApplicationClasses()) {
            String name = clazz.getJavaStyleName();
            // BuildConfig.java
            if (name.equals("BuildConfig"))
                classesToRemove.add(clazz);
        }
        for (SootClass clazz : classesToRemove)
            Scene.v().removeClass(clazz);

    }

    /*
     * get all the entry methods in the application
     */
    private List<SootMethod> getEntryMethods(SootClass baseClass, List<SootClass> classes,
            Map<String, Set<String>> epMap) {

        List<SootMethod> entryMethods = new ArrayList<SootMethod>();
        for (SootClass c : classes) {
            // find which classes are in ep map
            String className = c.getName().replace('$', '.');

            if (epMap.containsKey(className)) {
                Set<String> methods = epMap.get(className);

                for (String method : methods) {
                    String signature = "<" + baseClass + method + ">";
                    try {
                        entryMethods.add(Scene.v().getMethod(signature));
                    } catch (Exception e) {
                    }
                }
            }
        }
        return entryMethods;
    }

    /*
     * get class's super classes
     */
    private List<SootClass> getSuperTypes(SootClass sc) {
        List<SootClass> superTypes = new ArrayList<SootClass>();
        while (sc.hasSuperclass()) {
            superTypes.add(sc);
            superTypes.addAll(sc.getInterfaces());
            sc = sc.getSuperclass();
        }
        return superTypes;
    }

}
