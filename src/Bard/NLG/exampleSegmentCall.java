package Bard.NLG;

import Bard.NLG.Segment.Analyser;
import Bard.NLG.Tools;

import java.nio.charset.Charset;

public class exampleSegmentCall {

    private static final boolean testJSON = false;
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final String PFX = "/home/matthieu/repos/BARD-NLG-2.git/jsonFiles/";
    public static void main(String[] args) {

        Tools.loggerOn();

        /*
        Analyser analyser = new Analyser();

        analyser.addTarget("Log");
        analyser.addEvidence("Quin");
        analyser.addEvidence("Em");
        analyser.addEvidence("Com");
        analyser.addEvidence("Sawyer");
        analyser.addSimple("Spider");

        analyser.addEdge("Log", "Quin");
        analyser.addEdge("Log", "Em");
        analyser.addEdge("Spider", "Quin");
        analyser.addEdge("Spider", "Em");
        analyser.addEdge("Spider", "Com");
        analyser.addEdge("Spider", "Sawyer");

        analyser.getSegments();
        */


        //BLACK SITE
        /*
        Analyser analyserBS = new Analyser();

        analyserBS.addTarget("SpyPlane");
        analyserBS.addEvidence("ForeignIntel");
        analyserBS.addEvidence("SpyPlaneExpert");
        analyserBS.addEvidence("TCR12Test");
        analyserBS.addSimple("Drone");
        analyserBS.addEvidence("DoDReport");
        analyserBS.addEvidence("DroneExpert");

        analyserBS.addEdge("SpyPlane", "ForeignIntel");
        analyserBS.addEdge("SpyPlane", "SpyPlaneExpert");
        analyserBS.addEdge("SpyPlane", "TCR12Test");
        analyserBS.addEdge("Drone", "DoDReport");
        analyserBS.addEdge("Drone", "DroneExpert");
        analyserBS.addEdge("Drone", "TCR12Test");

        analyserBS.getSegments();
        */




        // THREE NATIONS

        Analyser analyserTN = new Analyser();

        // --- Nodes

        analyserTN.addEvidence("residue");

        analyserTN.addSimple("Oclar L");
        analyserTN.addSimple("Trubia L");

        analyserTN.addTarget("Oclar H");
        analyserTN.addEvidence("Oclar E1");
        analyserTN.addEvidence("Oclar E2");

        analyserTN.addSimple("Trubia H");
        analyserTN.addEvidence("Trubia E1");
        analyserTN.addEvidence("Trubia E2");

        // --- Edges

        analyserTN.addEdge("Oclar H", "Oclar E1");
        analyserTN.addEdge("Oclar H", "Oclar E2");
        analyserTN.addEdge("Oclar H", "Oclar L");

        analyserTN.addEdge("Oclar L", "Trubia L");
        analyserTN.addEdge("Oclar L", "residue");
        analyserTN.addEdge("Trubia L", "residue");

        analyserTN.addEdge("Trubia H", "Trubia E1");
        analyserTN.addEdge("Trubia H", "Trubia E2");
        analyserTN.addEdge("Trubia H", "Trubia L");
        analyserTN.addEdge("Trubia E1", "Trubia E2");

        analyserTN.getSegments();




        /*
        // -------------------------------------------------------------------------------------------------------------
        if (testJSON) {
            List<RawSegment> l = analyser.getRawSegments();
            System.out.println("--- JAVA");
            System.out.println(l.toString());

            Set<String> SB = new HashSet<>();
            SB.add("toto");
            SB.add("tata");

            Set<String> DB = new HashSet<>();
            DB.add("blo");
            DB.add("bla");

            Segment s = new Segment(l.get(1), SB, 0.5, DB, 0.7);
            s.put("toto",
                    new NodeInfo("toto", "totoName", "totoState", 0.1, 0.8, true, false));

            System.out.println(s.toJSONString());

            Segment s2 = Segment.fromJSON(new JSONObject(s.toJSONString()));

            System.out.println("s.equals(s2): " + s.equals(s2));

            System.out.println(s2.toJSONString());

            System.out.println("\n\n");

            s.put("aaaaaaaaaaaaaaaaaaaaaaa",
                    new NodeInfo("toto", "totoName", "totoState", 0.1, 0.8, true, false));
            System.out.println(Segment.toStringJSON(java.util.Arrays.asList(s, s2)));

            List<Segment> sl = Segment.fromJSON(new JSONArray(Segment.toStringJSON(java.util.Arrays.asList(s, s2))));

            System.out.println(sl.get(0).equals(s));
        }
        // -------------------------------------------------------------------------------------------------------------
        */

        /*
        Analyser blackSite = new Analyser();

        blackSite.addTarget("SpyPlane");

        blackSite.addSimple("Drone");

        blackSite.addEvidence("SpyPlaneExpert");
        blackSite.addEvidence("ForeignIntelligence");
        blackSite.addEvidence("TCR12Test");
        blackSite.addEvidence("DoDReport");
        blackSite.addEvidence("DroneExpert");

        blackSite.addEdge("SpyPlane", "SpyPlaneExpert");
        blackSite.addEdge("SpyPlane", "ForeignIntelligence");
        blackSite.addEdge("SpyPlane", "TCR12Test");

        blackSite.addEdge("Drone", "TCR12Test");
        blackSite.addEdge("Drone", "DoDReport");
        blackSite.addEdge("Drone", "DroneExpert");

        List<RawSegment> blackSiteRS = blackSite.getRawSegments();
        List<Segment> blackSiteS = blackSiteRS.stream()
                .map(rs-> new Segment(rs, new HashSet<String>(), 0.2, new HashSet<String>(), 0.5 ))
                .collect(Collectors.toList());
        String content = Segment.toStringJSON(blackSiteS);
        System.out.println(content);
        */


        // -------------------------------------------------------------------------------------------------------------
        // Load a json file representing the Segment
        /*
        try {
            List<Segment> ls = Segment.fromJSON(new JSONArray(content));
            TextGenerator tg = new TextGenerator(ls);
            String res = tg.getText();
            System.out.println(res);
        }
        catch (Exception e){
            e.printStackTrace();
        }*/


        /*
        String[] pathArray = null;

        if (args.length != 0) {
            pathArray = args;
        } else {

            pathArray = Stream.of(
                    "Black_Site - Q1.json",
                    // "Black_Site - Q2.json",
                    // "Cyberattack - Q1.json",
                    // "Cyberattack - Q2.json",
                    // "Cyberattack - Q4.json",
                    // "Cyberattack - Q5.json",
                    // "Evidence Chain Model - Q1.json",
                    // "Evidence Chain Model - Q2.json",
                    // "Kernel_Error - Q1.json",
                    // "Kernel_Error - Q2.json",
                    // "Kernel_Error - Q3.json",
                    // "Kernel_Error - Q4.json",
                    // "Kernel_Error - Q5.json",
                    // "Kernel_Error - Q6.json",
                    // "Oil_Spillage - Q1.json",
                    // "Oil_Spillage - Q2.json",
                    // "Oil_Spillage - Q3.json",
                    // "Oil_Spillage - Q4.json",
                    // "Spy_messaging - Q1.json",
                    // "Spy_messaging - Q2.json",
                    // "Spy_messaging - Q3.json",
                    // "Spy_messaging - Q4.json",
                    // "Spy_messaging - Q5.json",
                    // "The_Spider - Q1.json",
                    // "The_Spider - Q2.json",
                    // "The_Spider - Q3.json",
                    // "Three Nations Problem.json",
                    ""
            ).filter(p->!p.equals("")).map(p -> PFX + p).toArray(String[]::new);
        }


        VPPhraseSpec test = NLGTools.nlgFactory.createVerbPhrase("be");
        NLGTools.setNumber(test, 5);
        String txt = NLGTools.getText(test).toLowerCase();
        String txt2 = txt.substring(0, txt.length()-1);
        System.err.println("---------\n" + txt2);



        for (String p : pathArray) {
            java.nio.file.Path file = Paths.get(p);
            try {
                byte[] fileArray = java.nio.file.Files.readAllBytes(file);
                String content = new String(fileArray, UTF8_CHARSET);
                System.out.println("\n--- --- --- FILE: " + file);
                System.out.println(content);
                List<Segment> ls = Segment.fromJSON(new JSONArray(content));
                TextGenerator tg = new TextGenerator(ls);
                String res = tg.getText();
                System.out.println(res);
            } catch (Exception e) {
                System.err.println("Error reading file " + file);
                e.printStackTrace();
            }
        }
*/

    }

}
