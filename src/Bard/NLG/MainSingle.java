package Bard.NLG;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;


public class MainSingle {
	public static String filename = "";
	
	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
        String content = new String(Files.readAllBytes(Paths.get(filename)));
        return new JSONObject(content);
    }
	public static void main(String[] args) {
		try {
			//Object obj = parser.parse(new FileReader("f:\\test.json"));

			BaseModule bm = new BaseModule();
			
			//Object obj = parser.parse(new FileReader("C:\\\\Users\\\\aazad\\\\Google Drive\\\\BARD-NLG Team [private]\\\\BN_NLG_Report_Generation\\\\ChestClinic.json"));
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack2.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\ChestClinic.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\ChestClinic - Copy.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\ChestClinic_noEvidence.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\MetaStaticCancer.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\DrugCheat.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\spyMessaging.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Hydraulic_advanced.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Problem - Evidence Chain (UlrikeH) BN V1.0.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\AlongCameASpider.json";
			
			
			// Testing Problems for IARPA
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Black_Site - Q1.json";		// SB-DB O.K. -- NLG text: SB-DB analyses of Segment 2 are wrong
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Black_Site - Q2.json";			// SB should include TRC12Test (i.e. Radar Report) in Segment 2 check with Ingrid
			
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q1.json";			// SB-DB O.K. -- NLG text O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q2.json";				// SB-DB O.K. -- NLG text O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q4.json";		// SB-DB O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q5.json";		// SB-DB O.K.
			
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Evidence Chain Model - Q1.json";			// SB-DB O.K. -- NLG text O.K.
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Evidence Chain Model - Q2.json";			// SB-DB O.K. -- NLG text O.K.
			
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q1.json";			// SB-DB O.K. -- NLG text O.K.
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q2.json";			// SB-DB O.K. -- NLG text O.K.
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q3.json";			// SB-DB O.K. -- NLG text O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q4.json";		// SB-DB O.K.
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q5.json";		// SB-DB O.K.
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q6.json";			// SB-DB O.K. -- NLG text O.K.
			
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q1.json";		// SB-DB O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q2.json";		// issues: 1) Segment 1 is not having "West Company"; 2) Segment 2, code is not producing any antiCause support for Target: NorthCompany = True, but in the Expected Document there is one; 3) SB-DB batches aren't correct for segment 2   		
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q3.json";		// SB-DB O.K. -- NLG TEXT O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q4.json";			// issues: 1) Segment 1 is not having "West Company"; 2) SB-DB batches aren't correct for segment 2
			
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider - Q1.json";		// SB-DB O.K. -- NLG TEXT O.K. --NLG is a bit enhanced looking
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider - Q2.json";		// SB-DB look different
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider - Q3.json";		// SB-DB O.K.
			
			filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q1.json";			// SB-DB O.K. -- NLG text O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q2.json";			// SB-DB O.K. -- NLG text O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q3.json";			// SB-DB O.K. -- NLG text O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q4.json";		// SB-DB O.K. -- NLG TEXT O.K.
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q5.json";		// SB-DB O.K. -- NLG TEXT needs modifications
			
			
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Black_Site.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Black_Site - Copy.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Evidence Chain Model.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage.json - temp1.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider - [recursion error].json"; // recursion error
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider.json";
			
			
			// Testing Problems for Training-BARD-Deliverables
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Blue Green Taxi Problem.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Buggy LISP.json"; // output has bug
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Drug Cheat 6 node V6.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Lecturer's Life.json";
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Metastatic Cancer (BARD version).json"; // Null pointer error: at BARD_NLG.BaseModule.set_findings_of_ConditionedNodes_3(BaseModule.java:2923) 
			// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Pearl Burglary Alarm Problem.json";

			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations Problem - complete BN.json"; // error in new algo too: "SHOULD NOT HAPPEN: Common Effect, causal direction; There is a bug in our program, please contact us!"
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 1.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 2.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 3.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 4.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 5.json";
			
			// Testing BNs with error 
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp0-1.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp0-28.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp1-1.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp1-5.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp2-1.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp3-3.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp6-4.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp6-22.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp7-1.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp7-3.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp7-20.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp7-24.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp16-18.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp16-27.json";
			//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\temp16-47.json";
			
		
			
			JSONObject config = parseJSONFile(filename);

            
			
			try {
				//System.err.println(config.getString("netPath"));
				//System.out.println(bm.runNLG(config));
				System.out.println(bm.runNLG(config));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}


		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}	
}
