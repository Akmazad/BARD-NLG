package Bard.NLG;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.File;

//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;


public class MainBatch {
	public static String filename = "";
	public MainBatch() {
		// TODO Auto-generated constructor stub
	}

	public static JSONObject parseJSONFile(String filename) throws JSONException, IOException {
		String content = new String(Files.readAllBytes(Paths.get(filename)));
		return new JSONObject(content);
	}

	public static void main(String[] args) {
		//Object obj = parser.parse(new FileReader("f:\\test.json"));
		
		Tools.loggerOn();

		BaseModule bm = new BaseModule();
		String[] fileArr = new String[33];

		//Object obj = parser.parse(new FileReader("C:\\\\Users\\\\aazad\\\\Google Drive\\\\BARD-NLG Team [private]\\\\BN_NLG_Report_Generation\\\\ChestClinic.json"));
		//filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack2.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\ChestClinic.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\ChestClinic_noEvidence.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\MetaStaticCancer.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\DrugCheat.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\spyMessaging.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Hydraulic_advanced.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Problem - Evidence Chain (UlrikeH) BN V1.0.json";
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\AlongCameASpider.json";


		// Testing Problems for IARPA
		fileArr[0] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Black_Site - Q1.json";		// SB-DB O.K. -- NLG text: SB-DB analyses of Segment 2 are wrong
		fileArr[1] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Black_Site - Q2.json";			// SB should include TRC12Test (i.e. Radar Report) in Segment 2 check with Ingrid

		fileArr[2] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q1.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[3] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q2.json";				// SB-DB O.K. -- NLG text O.K.
		fileArr[4] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q4.json";		// SB-DB O.K.
		fileArr[5] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Cyberattack - Q5.json";		// SB-DB O.K.

		fileArr[6] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Evidence Chain Model - Q1.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[7] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Evidence Chain Model - Q2.json";			// SB-DB O.K. -- NLG text O.K.

		fileArr[8] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q1.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[9] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q2.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[10] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q3.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[11] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q4.json";		// SB-DB O.K.
		fileArr[12] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q5.json";		// SB-DB O.K.
		fileArr[13] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Kernel_Error - Q6.json";			// SB-DB O.K. -- NLG text O.K.

		fileArr[14] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q1.json";		// SB-DB O.K.
		fileArr[15] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q2.json";		// issues: 1) Segment 1 is not having "West Company"; 2) Segment 2, code is not producing any antiCause support for Target: NorthCompany = True, but in the Expected Document there is one; 3) SB-DB batches aren't correct for segment 2   		
		fileArr[16] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q3.json";		// SB-DB O.K. -- NLG TEXT O.K.
		fileArr[17] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Oil_Spillage - Q4.json";			// issues: 1) Segment 1 is not having "West Company"; 2) SB-DB batches aren't correct for segment 2

		fileArr[18] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider - Q1.json";		// SB-DB O.K. -- NLG TEXT O.K. --NLG is a bit enhanced looking
		fileArr[19] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider - Q2.json";		// SB-DB look different
		fileArr[20] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\The_Spider - Q3.json";		// SB-DB O.K.

		fileArr[21] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q1.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[22] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q2.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[23] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q3.json";			// SB-DB O.K. -- NLG text O.K.
		fileArr[24] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q4.json";		// SB-DB O.K. -- NLG TEXT O.K.
		fileArr[25] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Spy_messaging - Q5.json";		// SB-DB O.K. -- NLG TEXT needs modifications
		
		fileArr[26] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations Problem - complete BN.json";
		fileArr[27] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 1.json";
		fileArr[28] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 2.json";
		fileArr[29] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 3.json";
		fileArr[30] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 4.json";
		fileArr[31] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations - Part 5.json";
		fileArr[32] = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\ChestClinic - Copy.json";
		
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
		// filename = "C:\\Users\\aazad\\Google Drive\\BARD-NLG Team [private]\\BN_NLG_Report_Generation\\Three Nations Problem.json"; // error in new algo too: "SHOULD NOT HAPPEN: Common Effect, causal direction; There is a bug in our program, please contact us!"


		for(int i = 0; i < fileArr.length; i++) {
			try {
				filename = fileArr[i];
				JSONObject config = parseJSONFile(filename);
				try {
					//System.out.println(config.getString("netPath"));
					//System.out.println(bm.runNLG(config));
					System.out.println("fileProcessed: " + filename);
					bm.runNLG(config);
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

}

