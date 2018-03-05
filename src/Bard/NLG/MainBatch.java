package Bard.NLG;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MainBatch {

	public static void main(String[] args) {

		//Tools.loggerOn();

		// Test arg number
		if(args.length < 1){
			Tools.logError("Provide the path to testing files");
		}

		// Get path from arg and test if it is a directory
		Path dirPath = Paths.get(args[0]);
		if(!Files.isDirectory(dirPath)){
			Tools.logError(dirPath + ": not a directory");
		}



		// Get output dir
		Path outDir = null;
		if(args.length == 2){
			outDir = Paths.get(args[1]);
			if(!Files.isDirectory(outDir)){
				Tools.logError(outDir + ": not a directory");
			}
		}



		// Will contains all the file to take care of
		List<Path> paths = Stream.of(

			
				"SingleNodeNet.json",
				 
				// IARPA problems
				 "Black_Site - Q1.json",
				 "Black_Site - Q2.json",
				 "Cyberattack - Q1.json",
				 "Cyberattack - Q2.json",
				 "Cyberattack - Q4.json",
				 "Cyberattack - Q5.json",
				 "Evidence Chain Model - Q1.json",
				 "Evidence Chain Model - Q2.json",
				 "Kernel_Error - Q1.json",
				 "Kernel_Error - Q2.json",
				 "Kernel_Error - Q3.json",
				 "Kernel_Error - Q4.json",
				 "Kernel_Error - Q5.json",
				 "Kernel_Error - Q6.json",
				 "Oil_Spillage - Q1.json",
				 "Oil_Spillage - Q2.json",
				 "Oil_Spillage - Q3.json",
				 "Oil_Spillage - Q4.json",
				 "Spy_messaging - Q1.json",
				 "Spy_messaging - Q2.json",
				 "Spy_messaging - Q3.json",
				 "Spy_messaging - Q4.json",
				 "Spy_messaging - Q5.json",
				 "The_Spider - Q1.json",
				 "The_Spider - Q2.json",
				 "The_Spider - Q3.json",
				 
				 // Training Problems
				 "Three Nations - Part 1.json",
				 "Three Nations - Part 2.json",
				 "Three Nations - Part 3.json",
				 "Three Nations - Part 4.json",
				 "Three Nations - Part 5.json",
				 "Three Nations Problem - complete BN.json",		// bug: SHOULD NOT HAPPEN: Could not find a cutting point in a 2 CE Loop
				 "Blue Green Taxi Problem.json",
				 "Buggy LISP.json",
				 "Drug Cheat 6 node V6.json",
				 "Lecturer's Life.json",
				 "Metastatic Cancer (BARD version).json",
				 "Pearl Burglary Alarm Problem.json",
				 
				 // errorNets found by Steve
				 "temp0-1.json",
				 "temp0-28.json",
				 "temp1-1.json",
				 "temp1-5.json",
				 "temp2-1.json",
				 "temp3-3.json",
				 "temp6-4.json",
				 "temp6-22.json",
				 "temp7-1.json",
				 "temp7-3.json",
				 "temp7-20.json",
				 "temp7-24.json",
				 "temp16-18.json",
				 "temp16-27.json",
				 "temp16-47.json",
				 
				 
				 // Other test problems 
				 "ChestClinic.json",		// bug: SHOULD NOT HAPPEN: Could not find a cutting point in a 2 CE
				 "Cyberattack2.json",
				 "MetaStaticCancer.json",
				 "DrugCheat.json",
				 "Hydraulic_advanced.json",
				 "Problem - Evidence Chain (UlrikeH) BN V1.0.json",
				 "Oil_Spillage.json - temp1.json",
				 "ChestClinic_noEvidence.json",		// bug: SHOULD NOT HAPPEN: Could not find a cutting point in a 2 CE
				 "The_Spider - [recursion error].json",
				 
				 // new NETs that Generated Error
				 "temp81-30.json",
				 "temp383-67.json",
				 "temp395-104.json",

				""
				).filter(p->!p.equals("")).map(dirPath::resolve).collect(Collectors.toList());

		// Create the base module & go through the files
		BaseModule bm = new BaseModule();

		for(Path p : paths){
			try {
				// Get the json object for the file
				String content = new String(Files.readAllBytes(p));
				JSONObject config = new JSONObject(content);
				//Calling the base module
				String result = bm.runNLG(config, p);
				if(outDir != null) {

					BufferedWriter bw = null;
					try {

						Path outPath = outDir.resolve(p.getFileName() + "_NLG_Explanation.html");
						File oFile = new File(outPath.toString());

						/* This logic will make sure that the file
						 * gets created if it is not present at the
						 * specified location*/
						if (!oFile.exists()) {
							oFile.createNewFile();
						}

						FileWriter fw = new FileWriter(oFile);
						bw = new BufferedWriter(fw);

						bw.write(result);
					} catch (IOException ioe) {
						ioe.printStackTrace();
					} finally {
						try {
							if (bw != null)
								bw.close();
						} catch (Exception ex) {
							System.out.println("Error in closing the BufferedWriter" + ex);
						}
					}
				}

			} catch(Exception e) {
				e.printStackTrace();
				Tools.logError(e.getMessage());
			}

		}
	}






}

