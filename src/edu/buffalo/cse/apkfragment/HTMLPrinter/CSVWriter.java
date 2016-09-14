package edu.buffalo.cse.apkfragment.HTMLPrinter;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import edu.buffalo.cse.apkfragment.Constants;
import edu.buffalo.cse.apkfragment.struturalanalysis.FlowStructComplexity;
import edu.buffalo.cse.blueseal.BSG.Node;
import edu.buffalo.cse.blueseal.BSG.SinkNode;
import edu.buffalo.cse.blueseal.BSG.SourceNode;

public class CSVWriter {
	
	public static File OUTPUT_DIR;
	public static String APP_OUTPUT_PATH;

	public void write(){
		//create output directory
		createOutputDirForAPP();
		writeAPIAnalysisResult();
	}
	
	private void writeAPIAnalysisResult() {
		StringBuilder sb = new StringBuilder();
		for(Iterator it = FlowStructComplexity.flowAPIInfo.keySet().iterator();
				it.hasNext();){
			List<Node> key = (List<Node>) it.next();
			Set<List<String>> apis = FlowStructComplexity.flowAPIInfo.get(key);
			SourceNode src = (SourceNode) key.get(0);
			SinkNode sink = (SinkNode)key.get(key.size()-1);
			sb.append("Flow;");
			sb.append(src.getSrsAPI());
			sb.append(";");
			sb.append(sink.getSinkAPI());
			sb.append("\n");
			int count = 0;
			for(List<String> seq : apis){
				sb.append("sequence "+count+"\n");
				for(String sig : seq){
					sb.append("-"+sig+"\n");
				}
				count++;
			}
		}

		try{
			File file = new File(APP_OUTPUT_PATH+"/api.csv");
			FileUtils.writeStringToFile(file, sb.toString());
		}catch(Exception e){}
	}

	private void createOutputDirForAPP() {
		APP_OUTPUT_PATH = Constants.OUTPUT_DIR + "/" + Constants.apkName;
		OUTPUT_DIR = new File(APP_OUTPUT_PATH);
		try {
			if(OUTPUT_DIR.exists()){
				FileUtils.deleteDirectory(OUTPUT_DIR);
			}else{
				OUTPUT_DIR.mkdir();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
}
