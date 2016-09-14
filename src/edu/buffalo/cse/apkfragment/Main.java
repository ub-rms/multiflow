package edu.buffalo.cse.apkfragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import soot.Pack;
import soot.PackManager;
import soot.Scene;
import soot.SceneTransformer;
import soot.Transform;
import soot.options.Options;
import edu.buffalo.cse.apkfragment.HTMLPrinter.CSVWriter;
import edu.buffalo.cse.apkfragment.callgraph.FragmentCallGraph;
import edu.buffalo.cse.blueseal.BSFlow.SourceSink;


public class Main {
	private static long runtime = 0;
	private static String apkName = "APKNAME";

	private static String apkpath="default";
	public static String OUTPUT_DIR = "AFOUTPUT";
	
	public static long getRunTime(){
		return runtime;
	}
	
	public static String getApkName(){
		return apkName;
	}

	public static void main(String[] args){
		
		Date start = new Date();
		
		//check apk path info input
		if(args.length == 0){
			System.out.println("Missing apk file path!");
			System.exit(1);
		}

		apkpath = args[0];
		System.out.println("analyzing apk:"+apkpath);
		
		int index = apkpath.lastIndexOf("/");
		
		if(index!=-1)
			apkName = apkpath.substring(index+1);
		Constants.apkName = apkName;
		
		//since this will involve sources and sinks, we need to load all sources and sinks first
		SourceSink.extractSootSourceSink();
		ApkFragmentOptions.setSparkOn();
		initializeSoot();
		
		SceneTransformer cgTransformer = new FragmentCallGraph(apkpath);
		Pack wjtp = PackManager.v().getPack("wjtp");
		wjtp.add(new Transform("wjtp.mycgtran", cgTransformer));
		wjtp.add(new Transform("wjtp.apkTransformer", new runBlueSealForEachEntryTransformer()));
		wjtp.apply();

    writeResults();
    Date finish = new Date();
    runtime = finish.getTime() - start.getTime();
    System.out.println("apkfragment running time: "+runtime/1000 + " second");

	}
	
	private static void writeResults() {
		//check if we already have a output directory, otherwise, create one
		File output_dir = new File(OUTPUT_DIR);
		if(!output_dir.exists()){
			output_dir.mkdir();
		}

		CSVWriter writer = new CSVWriter();
    writer.write();
	}

	private static void initializeSoot() {
		soot.G.reset();
		List<String> exList = new ArrayList<String>();
		exList.add("android.support.*");
		exList.add("android.annotation.*");
		exList.add("java.*");
		
		List<String> dir = new ArrayList<String>();
		dir.add(apkpath);
		
		soot.options.Options.v().set_whole_program(true);
		Options.v().set_allow_phantom_refs(true);
		Options.v().set_android_jars(Constants.ANDROID_JARS);
		Options.v().set_src_prec(Options.src_prec_apk);
		Options.v().set_exclude(exList);
		Options.v().set_process_dir(dir);
		Options.v().set_no_bodies_for_excluded(true);
		Options.v().set_output_format(Options.output_format_none);
		

		Scene.v().loadNecessaryClasses();
		
	}
	
}
