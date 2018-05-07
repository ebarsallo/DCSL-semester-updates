package Ape;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Ape {

	static BufferedWriter outcmd_orig; // = new BufferedWriter(new FileWriter("ape_commands.sh"));;
	static BufferedWriter outcmd_semi;
	static BufferedWriter outcmd_rand;
	
	static HashSet<String> touch_X = new HashSet<String>(100);
	static HashSet<String> touch_Y = new HashSet<String>(100);
	static HashSet<String> flip = new HashSet<String>(16);
	static HashSet<String> trackball_X = new HashSet<String>(100);
	static HashSet<String> trackball_Y = new HashSet<String>(100);
	static HashSet<String> rotation = new HashSet<String>(100);
	static HashSet<String> permissions = new HashSet<String>(100);
	static HashSet<String> packages = new HashSet<String>(100);
	static HashSet<String> actions = new HashSet<String>(100);
	static HashSet<String> categories = new HashSet<String>(16);
	static HashSet<String> launchflags = new HashSet<String>(100);
	
	static ArrayList<String> touch_X_arr;
	static ArrayList<String> touch_Y_arr;
	static ArrayList<String> flip_arr;
	static ArrayList<String> trackball_X_arr;
	static ArrayList<String> trackball_Y_arr;
	static ArrayList<String> rotation_arr;
	static ArrayList<String> permissions_arr;
	static ArrayList<String> packages_arr;
	static ArrayList<String> actions_arr;
	static ArrayList<String> categories_arr;
	static ArrayList<String> launchflags_arr;
	
	static int count = 0;

	// Modify this variables with the corresponding path or path/to/file
	static String path = "./logs";
	static String targetInput = "./events/input.txt"

	public static void main(String[] args) {
		try {

			String filename = path + targetInput;

			// Set filename with the arg if exists, otherwise use the filename hardcoded			
			if (args.length > 0 ) {
				filename = args[0];
			} 

			File file = new File(filename);
			BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
			
			outcmd_orig = new BufferedWriter(new FileWriter("ape_commands_orig.sh",false));
			outcmd_semi = new BufferedWriter(new FileWriter("ape_commands_semivalid.sh",false));
			outcmd_rand = new BufferedWriter(new FileWriter("ape_commands_random.sh",false));
			
			
			//StringBuffer stringBuffer = new StringBuffer();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				parseLine(line, "ORIG", outcmd_orig);
				//stringBuffer.append(line);
				//stringBuffer.append("\n");
			}
			//This is important
			buildArrayLists();
			bufferedReader.close();
			System.out.println("After first pass of file: "+filename);
			
			bufferedReader = new BufferedReader(new FileReader(file));
			count = 0;
			while ((line = bufferedReader.readLine()) != null) {
				parseLine(line, "SEMI", outcmd_semi);
				//stringBuffer.append(line);
				//stringBuffer.append("\n");
			}
			bufferedReader.close();
			System.out.println("After second pass of file: "+filename);
			
			bufferedReader = new BufferedReader(new FileReader(file));
			count = 0;
			while ((line = bufferedReader.readLine()) != null) {
				parseLine(line, "RAND", outcmd_rand);
				//stringBuffer.append(line);
				//stringBuffer.append("\n");
			}
			bufferedReader.close();
			System.out.println("After third pass of file: "+filename);
						
			System.out.println("Done reading file: "+filename);
			outcmd_orig.flush();
			outcmd_semi.flush();
			outcmd_rand.flush();
			outcmd_orig.close();
			outcmd_semi.close();
			outcmd_rand.close();
			//System.out.println(stringBuffer.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void parseLine(String line, String OutMode, BufferedWriter outcmd)
	{
		if ((count+1)%20 == 0){
			//go back to home screen every 100 events
			String tmp = count + ": shell am start -a android.intent.action.MAIN -c android.intent.category.HOME";
			try {
				outcmd.write(tmp+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				System.out.println("Error writing to file\n" + e.getMessage());
				e.printStackTrace();
			}
			count++;
		}
		//System.out.println(line); 
		if(line.startsWith(":Sending Touch")){
			//System.out.println(line); 
			
			String touchpattern = ":Sending Touch \\((\\w+)\\): \\d+:\\((\\d+\\.\\d+),(\\d+\\.\\d+)\\)";
			Pattern tp = Pattern.compile(touchpattern);
			Matcher m = tp.matcher(line);
			
			if(m.matches())
			{
				//String tmp = count + ": " + m.group(1) + " " + m.group(2) + " " + m.group(3);
				String tmp = "";
				if(OutMode.matches("ORIG")){
					touch_X.add(m.group(2));
					touch_Y.add(m.group(3));
					tmp = count + ": shell input tap " + m.group(2) + " " + m.group(3);
				}
				else if(OutMode.matches("SEMI")){
					tmp = count + ": shell input tap " + getSemivalid_X() + " " + getSemivalid_Y();
				}
				else if(OutMode.matches("RAND")){
					tmp = count + ": shell input tap " + getRandomCoord() + " " + getRandomCoord();
				}
				//System.out.println("Parsed " + tmp);
				try {
					outcmd.write(tmp+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error writing to file\n" + e.getMessage());
					e.printStackTrace();
				}
				
				count++;
			}
		}
		else if(line.startsWith(":Sending Flip")){
			//System.out.println(line); 
			
			String flippattern = ":Sending Flip (\\w+)=(\\w+)";
			Pattern fp = Pattern.compile(flippattern);
			Matcher m = fp.matcher(line);
			
			if(m.matches())
			{
				String tmp = "";
				if(OutMode.matches("ORIG")){
					flip.add(m.group(2));
					//tmp = count + ": " + m.group(1) + " " + m.group(2);
					tmp = count + ": shell input keyevent 111";
				}
				else if(OutMode.matches("SEMI")){
					//tmp = count + ": " + m.group(1) + " " + getSemivalidFlip();
					tmp = count + ": shell input keyevent 111";
				}
				if(OutMode.matches("RAND")){
					//tmp = count + ": " + m.group(1) + " " + getRandomFlip();
					tmp = count + ": shell input keyevent " + getRandomKeyevent();
				}
				//System.out.println("Parsed " + tmp);
				try {
					outcmd.write(tmp+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error writing to file\n" + e.getMessage());
					e.printStackTrace();
				}
				
				count++;
			}
		}
		else if(line.startsWith(":Sending Trackball")){
			//System.out.println(line); 
			
			String touchpattern = ":Sending Trackball \\((\\w+)\\): \\d+:\\((\\-?\\d+\\.\\d+),(\\-?\\d+\\.\\d+)\\)";
			Pattern tp = Pattern.compile(touchpattern);
			Matcher m = tp.matcher(line);
			
			if(m.matches())
			{
				//String tmp = count + ": " + m.group(1) + " " + m.group(2) + " " + m.group(3);
				String tmp = "";
				if(OutMode.matches("ORIG")){
					trackball_X.add(m.group(2));
					trackball_Y.add(m.group(3));
					tmp = count + ": shell input trackball roll " + m.group(2) + " " + m.group(3);
				}
				else if(OutMode.matches("SEMI")){
					tmp = count + ": shell input trackball roll " + getSemivalidTrackball_X() + " " + getSemivalidTrackball_Y();
				}
				else if(OutMode.matches("RAND")){
					tmp = count + ": shell input trackball roll " + getRandomTrackball() + " " + getRandomTrackball();
				}
				//System.out.println("Parsed " + tmp);
				try {
					outcmd.write(tmp+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error writing to file\n" + e.getMessage());
					e.printStackTrace();
				}
				
				count++;
			}
			
		}
		else if(line.startsWith(":Sending rotation")){
			//System.out.println(line); 
			
			String touchpattern = ":Sending rotation degree=(\\-?\\d+), persist=(\\w+)";
			Pattern tp = Pattern.compile(touchpattern);
			Matcher m = tp.matcher(line);
			
			if(m.matches())
			{
				//String tmp = count + ": " + m.group(1) + " " + m.group(2);
				String tmp = "";
				if(OutMode.matches("ORIG")){
					rotation.add(m.group(1));
					tmp = count + ": shell settings put system accelerometer_rotation " + m.group(1);
				}
				else if(OutMode.matches("SEMI")){
					tmp = count + ": shell settings put system accelerometer_rotation " + getSemivalidRotation();
				}
				else if(OutMode.matches("RAND")){
					tmp = count + ": shell settings put system accelerometer_rotation " + getRandomRotation();
				}
				//System.out.println("Parsed " + tmp);
				try {
					outcmd.write(tmp+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error writing to file\n" + e.getMessage());
					e.printStackTrace();
				}
				
				count++;
			}
			
		}
		else if(line.startsWith(":Permission")){
			//System.out.println(line); 
			
			String touchpattern = ":Permission (\\w+) ([\\w\\.]+) to package ([\\w\\.]+)";
			Pattern tp = Pattern.compile(touchpattern);
			Matcher m = tp.matcher(line);
			
			if(m.matches())
			{
				//String tmp = count + ": " + m.group(1) + " " + m.group(2) + " " + m.group(3);
				String tmp = "";
				if(OutMode.matches("ORIG")){
					permissions.add(m.group(2));
					packages.add(m.group(3));
					tmp = count + ": shell pm " + m.group(1) + " " + m.group(3) + " " + m.group(2);
				}
				else if(OutMode.matches("SEMI")){
					tmp = count + ": shell pm " + m.group(1) + " " + getSemivalidPackage() + " " + getSemivalidPerm();
				}
				else if(OutMode.matches("RAND")){
					tmp = count + ": shell pm " + m.group(1) + " " + getRandomPackage() + " " + getRandomPerm();
					//tmp = count + ": shell pm " + m.group(1) + " " + m.group(3) + " " + getRandomPerm();
				}
				//System.out.println("Parsed " + tmp);
				try {
					outcmd.write(tmp+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error writing to file\n" + e.getMessage());
					e.printStackTrace();
				}
				
				count++;
			}
			
		}
		else if(line.startsWith(":Switch:")){
			//System.out.println(line); 
			
			String touchpattern = ":Switch: (.*)";
			Pattern tp = Pattern.compile(touchpattern);
			Matcher m = tp.matcher(line);
			
			if(m.matches())
			{
				String tmp = count + ": " + StrToIntentCmd(m.group(1), OutMode);
				//System.out.println("Parsed " + tmp);
				try {
					outcmd.write(tmp+"\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.out.println("Error writing to file\n" + e.getMessage());
					e.printStackTrace();
				}
				
				count++;
			}
			
		}
		else {
			System.out.println("#nomatch: "+line);
		}
	}
	
	public static String StrToIntentCmd(String line, String OutMode){
		String icmd = "shell am start ";
		
		if(line.startsWith("#Intent") && line.endsWith("end")){
			String args[] = line.split(";");
			if(args.length < 2){
				System.out.println("Intent too short: Probably a bad intent.");
				return "";
			}
			for(int i=0; i < args.length; i++){
				if(args[i].startsWith("action")){
					//icmd += "-a " + args[i].split("=")[1] + " ";
					icmd += getAction(args[i],OutMode);
				}
				else if(args[i].startsWith("category")){
					//icmd += "-c " + args[i].split("=")[1] + " ";
					icmd += getCategory(args[i],OutMode);
				}
				else if(args[i].startsWith("launchFlags")){
					//icmd += "-f " + args[i].split("=")[1] + " ";
					icmd += getLaunchFlags(args[i],OutMode);
				}
				else if(args[i].startsWith("component")){
					icmd += " " + args[i].split("=")[1] + " ";
				}
				else{
					System.out.println("Unrecognized args: "+args[i]);
				}
			}
		}
		else {
			System.out.println("Bad Intent. Cannot parse: "+line);
			return "";
		}
		
		return icmd;
	}
	
	public static String getSemivalid_X(){
		return fetchSomeElement(touch_X_arr);
	}
	
	public static String getSemivalid_Y(){
		return fetchSomeElement(touch_Y_arr);
	}
	
	public static String getRandomCoord(){
		return genRandomDouble(10000);
	}
	
	public static String getSemivalidFlip(){
		return fetchSomeElement(flip_arr);
	}
	
	public static String getRandomFlip(){
		return genRandomString(ThreadLocalRandom.current().nextInt(0,16));
	}
	
	public static String getRandomKeyevent(){
		return Integer.toString(ThreadLocalRandom.current().nextInt());
	}
	
	public static String getSemivalidTrackball_X(){
		return fetchSomeElement(trackball_X_arr);
	}
	
	public static String getSemivalidTrackball_Y(){
		return fetchSomeElement(trackball_Y_arr);
	}
	
	public static String getRandomTrackball(){
		return genRandomDouble(10000);
	}
	
	public static String getSemivalidRotation(){
		return fetchSomeElement(rotation_arr);
	}
	
	public static String getRandomRotation(){
		return genRandomDouble(10000);
	}
	
	public static String getSemivalidPerm(){
		return fetchSomeElement(permissions_arr);
	}
	
	public static String getRandomPerm(){
		return genRandomString(ThreadLocalRandom.current().nextInt(0,128));
	}
	
	public static String getSemivalidPackage(){
		return fetchSomeElement(packages_arr);
	}
	
	public static String getRandomPackage(){
		return genRandomString(ThreadLocalRandom.current().nextInt(0,128));
	}
	
	public static String getAction(String ll, String OutMode){
		String t = "";
		if(OutMode.matches("ORIG")){
			actions.add(ll.split("=")[1]);
			t = "-a " + ll.split("=")[1] + " ";
		}
		else if(OutMode.matches("SEMI")){
			t = "-a " + getSemivalidAction() + " ";
		}
		else if(OutMode.matches("RAND")){
			String a = getRandomAction();
			if (! a.matches("''"))
				t = "-a " + a + " ";
		}
		return t;
	}
	
	public static String getSemivalidAction(){
		return fetchSomeElement(actions_arr);
	}
	
	public static String getRandomAction(){
		return genRandomString(ThreadLocalRandom.current().nextInt(0,64));
	}
	
	public static String getCategory(String ll, String OutMode){
		String t = "";
		if(OutMode.matches("ORIG")){
			categories.add(ll.split("=")[1]);
			t = "-c " + ll.split("=")[1] + " ";
		}
		else if(OutMode.matches("SEMI")){
			t = "-c " + getSemivalidCategory() + " ";
		}
		else if(OutMode.matches("RAND")){
			String c = getRandomCategory();
			if (! c.matches("''"))
				t = "-c " + c + " ";
		}
		return t;
	}
	
	public static String getSemivalidCategory(){
		return fetchSomeElement(categories_arr);
	}
	
	public static String getRandomCategory(){
		return genRandomString(ThreadLocalRandom.current().nextInt(0,64));
	}
	
	public static String getLaunchFlags(String ll, String OutMode){
		String t = "";
		if(OutMode.matches("ORIG")){
			launchflags.add(ll.split("=")[1]);
			t = "-f " + ll.split("=")[1] + " ";
		}
		else if(OutMode.matches("SEMI")){
			t = "-f " + getSemivalidLaunchFlags() + " ";
		}
		else if(OutMode.matches("RAND")){
			t = "-f " + getRandomLaunchFlags() + " ";
		}
		return t;
	}
	
	public static String getSemivalidLaunchFlags(){
		return fetchSomeElement(launchflags_arr);
	}
	
	public static String getRandomLaunchFlags(){
		return "0x" + genRandomDigits(8);
	}
	
	public static String fetchSomeElement(ArrayList<String> arr){
		int i = ThreadLocalRandom.current().nextInt(0,arr.size());
		return arr.get(i);
	}
	
	public static void buildArrayLists(){
		touch_X_arr = new ArrayList<String>(touch_X);
		touch_Y_arr = new ArrayList<String>(touch_Y);
		flip_arr = new ArrayList<String>(flip);
		trackball_X_arr = new ArrayList<String>(trackball_X);
		trackball_Y_arr = new ArrayList<String>(trackball_Y);
		rotation_arr = new ArrayList<String>(rotation);
		permissions_arr = new ArrayList<String>(permissions);
		packages_arr = new ArrayList<String>(packages);
		actions_arr = new ArrayList<String>(actions);
		categories_arr = new ArrayList<String>(categories);
		launchflags_arr = new ArrayList<String>(launchflags);
	}
	
	public static String genRandomString(int len){
		StringBuilder t = new StringBuilder("'");
		for(int i=0; i<len; i++){
			char c = (char)(ThreadLocalRandom.current().nextInt(32,128));
			if ((c < 47) || (c > 57 && c < 65) || (c > 90 && c < 97) || (c > 122)  ){
				t.append('\\');
			}
			if(c == '\'')
				c = '"';
			t.append(c);
		}
		t.append('\'');
		return t.toString();
	}
	
	public static String genRandomDigits(int len){
		StringBuilder t = new StringBuilder("");
		for(int i=0; i<len; i++){
			t.append(ThreadLocalRandom.current().nextInt(0,10));
		}
		return t.toString();
	}
	
	public static String genRandomDouble(int range){
		int sign = 1;
		if(ThreadLocalRandom.current().nextDouble() < 0.10)
			sign = -1;
		Double c = sign * ThreadLocalRandom.current().nextDouble() * range;
		return c.toString();
	}
}