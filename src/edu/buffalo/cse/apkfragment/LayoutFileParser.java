package edu.buffalo.cse.apkfragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LayoutFileParser {
    private File[] layoutFiles;
    private File[] valuesFile;
    private Map<String, String> idToFile = new HashMap<String, String>();
    private Map<String, Set<String>> functionsFromXmlFile = new HashMap<String, Set<String>>();
    
    private String apkLoc;

    public LayoutFileParser(String al) {
        apkLoc = al;
        parseLayoutXmls();
        createLayoutIdAndFileMap();
        getFunctionsFromLayout();
        try {
            Runtime.getRuntime().exec("rm -rf ./LayoutOutput ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public Map<String, String> getIdToFile() {
        return idToFile;
    }
    
    public Map<String, Set<String>> getFunctionsFromXmlFile() {
        return functionsFromXmlFile;
    }
    
    public void parseLayoutXmls() {
//        Debug.println("Laout parser", "IN GETLAYOUT FUNCTION");
        try {
            Runtime.getRuntime().exec("rm -rf ./LayoutOutput ");
            Process p = Runtime.getRuntime().exec(
                    Constants.apktool + " d " + apkLoc + " ./LayoutOutput ");
            int exitValue;
            try {
                exitValue = p.waitFor();

                if (exitValue == 0) {
                    File layoutFolder = new File("./LayoutOutput/res/layout/");
                    layoutFiles = layoutFolder.listFiles();
                    File valuesFolder = new File("./LayoutOutput/res/values/");
                    valuesFile = valuesFolder.listFiles();
                }
                // createLayoutIdAndFileMap();
                // getFunctionsFromLayout();
                // updateEntryPoints();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createLayoutIdAndFileMap() {
        // just looping through files in values directory until we get
        // public.xml file
    	if(valuesFile == null) return;
        int i;
        for (i = 0; i < valuesFile.length && !valuesFile[i].toString().contains("public"); i++)
            ;

        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(valuesFile[i].toString()));

            String line = null;
            while ((line = reader.readLine()) != null) {
                String fields[] = line.split("\"");
                if (fields.length > 5) {
                    if (fields[1].contains("layout")) {
                        idToFile.put(fields[5], fields[3]);
                    }
                }
            }

            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void updateEntryPoints() {
//        /*
//         * now we have two map LayoutIdToClassname and LayoutIdToFile LayoutId
//         * -> ClassName and LayoutId -> xmlFileName Iterator it =
//         * mp.entrySet().iterator(); while (it.hasNext()) { Map.Entry pairs =
//         * (Map.Entry)it.next(); System.out.println(pairs.getKey() + " = " +
//         * pairs.getValue()); it.remove(); // avoids a
//         * ConcurrentModificationException
//         */
//
//        try {
//            Process fileCopy = Runtime.getRuntime().exec(
//                    "cp input/EntryPoints.txt input/EntryPointsUpdated.txt");
//            fileCopy.waitFor();
//
//            PrintWriter fileOut = new PrintWriter(new BufferedWriter(new FileWriter(
//                    "input/EntryPointsUpdated.txt", true)));
//
//            Iterator IdtoFileIterator = idToFile.entrySet().iterator();
//
//            while (IdtoFileIterator.hasNext()) {/*
//                                                 * Looping through LayoutId and
//                                                 * files
//                                                 */
//                Map.Entry pairs = (Map.Entry) IdtoFileIterator.next();
//                String LayoutId = (String) pairs.getKey();
//                String xmlFile = (String) pairs.getValue();
//                // GlobalData.epMap_.put(lastClass, new HashSet<String>());
//                // GlobalData.epMap_.get(lastClass).add(line);'
//                String className = GlobalData.layoutIdToClassNameMap.get(LayoutId);
//
//                Debug.println("updateEntryPoints", "LayoutId " + LayoutId);
//                Debug.println("EntryPoint Map", "Class name in Classmap " + className
//                        + "with xml file " + xmlFile);
//
//                // GlobalData.epMap_.put(className,
//                // new HashSet<String>());
//
//                fileOut.println(className);
//                Set<String> functions = functionsFromXmlFile.get(xmlFile);
//                Iterator<String> iter = functions.iterator();
//                while (iter.hasNext()) {
//                    fileOut.println(": void " + iter.next());
//                }
//                /* adding set of function already extracted */
//                // GlobalData.epMap_.get(className).addAll(functions);
//
//            }
//
//            fileOut.close();
//        } catch (IOException e) {
//            // oh noes!
//        } catch (InterruptedException e) {
//            // oh noes!
//        }
//
//    }

    public void getFunctionsFromLayout() {
    	if(layoutFiles == null ) return;
        for (int i = 0; i < layoutFiles.length; i++) {
            try {
                BufferedReader reader = new BufferedReader(
                        new FileReader(layoutFiles[i].toString()));
                String line = null;

                String filePath = layoutFiles[i].toString();
                String subFields[] = filePath.split("/");
                /* ./LayoutOutput/res/layout/activity_group_messenger.xml */
                /* 4th field will have filename */
                //Debug.println("Get Functions from Layout ", "filename " + subFields[4]);
                String fullFileName[] = subFields[4].split("[/.]");
                /* 0 th filed will have the filename */
                String filename = fullFileName[0];
                //Debug.println("Get Functions from Layout ", "in XML file " + filename);
                
                while ((line = reader.readLine()) != null) {
                    String fields[] = line.split("\"");
                    if (fields.length > 12) {
                        if (fields[12].contains("android:onClick")) {
                            //Debug.println("XML parsing ", "Found onclick");
                            String function = fields[13] + "(android.view.View)";
                            //Debug.println("Layout parser ", "function name " + function);
                            if (!functionsFromXmlFile.containsKey(filename))
                                functionsFromXmlFile.put(filename, new HashSet<String>());
                            functionsFromXmlFile.get(filename).add(function);
                        }
                    }

                }
                
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
