package coref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;

public class EMUtil {
	
	public static double alpha = 1.0;
	public static boolean train = false;

	public static ArrayList<String> Tense = new ArrayList<String>(Arrays.asList("Future", "Past", "Present", "Unspecified", "Fake"));
	
	public static ArrayList<String> Polarity = new ArrayList<String>(Arrays.asList("Positive", "Negative", "Fake"));
	
	public static ArrayList<String> Modality = new ArrayList<String>(Arrays.asList("Asserted", "Other", "Fake"));
	
	public static ArrayList<String> Genericity = new ArrayList<String>(Arrays.asList("Specific", "Generic", "Fake"));
	
	public static ArrayList<String> EventSubType = new ArrayList<String>(Arrays.asList("Start-Position", "Elect", "Transfer-Ownership", "Extradite",
			"Declare-Bankruptcy", "Marry", "Demonstrate", "Start-Org", "End-Org", "Appeal", "Trial-Hearing", "Attack",
			"Sue", "Convict", "Meet", "Pardon", "Charge-Indict", "Divorce", "End-Position", "Nominate", "Fine",
			"Release-Parole", "Transfer-Money", "Phone-Write", "Merge-Org", "Die", "Arrest-Jail", "Be-Born", "Injure",
			"Transport", "Sentence", "Acquit", "Execute", "Fake"));
	
	public static ArrayList<EventMention> getEventMentionInOneS(ACEDoc doc, ArrayList<EventMention> allEvms, int sid) {
		ArrayList<EventMention> mentions = new ArrayList<EventMention>();
		for(EventMention em : allEvms) {
			if(doc.positionMap.get(em.getAnchorStart())[0]==sid) {
				mentions.add(em);
			}
		}
		return mentions;
	}

	public static HashSet<String> negativeSets = Common.readFile2Set("negativePairs");
	
	public static double getP_C(EventMention ant, EventMention m, ACEDoc doc) {
		// TODO Auto-generated method stub
		
		if(!ant.subType.equals(m.subType) && !ant.getAnchor().equals(m.getAnchor())) {
			return 0;
		}
		
		String tr1 = ant.getAnchor();
		String tr2 = m.getAnchor();
		
		String key = "";
		if(tr1.compareTo(tr2)>0) {
			key = tr1 + "#" + tr2;
		} else {
			key = tr2 + "#" + tr1;
		}
		if(negativeSets.contains(key)) {
//			System.out.println(key);
//			return 0;
		}
		
//		short dis = (short) (m.sequenceID - ant.sequenceID);
////		if (dis >= cap) {
////			dis = cap;
////		}
//		int ret = (short) Context.bins.length;
//		for(int i =0;i<Context.bins.length;i++) {
//			int bin = Context.bins[i];
//			if(dis<bin) {
//				ret = (short) i;
//				break;
//			}
//		}
//		return Context.probs[ret];
//		if(Context.inNegativeContext(ant, m, doc)==1) {
//			return 0;
//		}
		
		return 1;
	}

	public static HashMap<String, Integer> formChainMap(
			ArrayList<EventChain> goldChains) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for(int i=0;i<goldChains.size();i++) {
			EventChain ec = goldChains.get(i);
			for(EventMention em : ec.getEventMentions()) {
				map.put(em.toString(), i);
			}
		}
		return map;
	}
}
