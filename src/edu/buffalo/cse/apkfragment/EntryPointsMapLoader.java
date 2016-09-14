package edu.buffalo.cse.apkfragment;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EntryPointsMapLoader {

	public static Map<String, Set<String>> epMap_ = 
			new HashMap<String, Set<String>>();
	
	public EntryPointsMapLoader(){
		this("input/EntryPoints.txt");
	}

	public EntryPointsMapLoader(String epLoc) {
		// TODO Auto-generated constructor stub
		BufferedReader in;
		try {
			in = new BufferedReader(new FileReader(epLoc));
			String line;
		    String lastClass = ""; 
		    while ((line = in.readLine()) != null)
		    {   
		    	line = line.trim();
		        if (line.isEmpty()) 
		          continue;
		        if (line.startsWith("#")) //don's section of the txt file
		          break;
		    
		        if (line.startsWith(":")) //is a method
		        {   
		          if (lastClass.isEmpty())
		            System.err.println("error parsing EntryPoints file");
		          else 
		            epMap_.get(lastClass).add(line);
		        }   
		        else //is a class
		        {   
		          lastClass = line;
		          epMap_.put(lastClass, new HashSet<String>());
		    
		        }   
		    }   
		    in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Map<String, Set<String>> getEPMap(){
		return epMap_;
	}
}
