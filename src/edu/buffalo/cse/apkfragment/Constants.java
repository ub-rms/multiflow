package edu.buffalo.cse.apkfragment;

public class Constants {

	public final static boolean debugOn = false;
	public final static String TOOLS = "./tools/";
	
	
	public final static String DEX2JAR = TOOLS + "dex2jar-0.0.9.9/dex2jar.sh";
	public final static String apktool = TOOLS + "apktool";
	public final static String ANDROID_JARS = "android-jars";
	public static String apkName;
	public static String OUTPUT_DIR="AFOUTPUT";
	public static String ERROR_DIR="ERROR/";
	
	//the following setting should be changed to the local path
	public static String aapt = "/home/fengshen/Documents/android/android-sdk-linux/build-tools/23.0.0_rc1/bin/aapt";
	
	public static void setAAPTpath(String path)
	{
		aapt = path;
	}
}
