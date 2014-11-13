package coref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import model.ACEDoc;
import model.EventChain;
import model.EventMention;

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

	public static double getP_C(EventMention ant, EventMention m, ACEDoc doc) {
		// TODO Auto-generated method stub
		if(!ant.tense.equals(m.tense)) {
			return 0;
		}
		if(!ant.genericity.equals(m.genericity)) {
			return 0;
		}
		if(!ant.modality.equals(m.modality)) {
			return 0;
		}
		if(!ant.polarity.equals(m.polarity)) {
			return 0;
		}
		if(!ant.subType.equals(m.subType)) {
			return 0;
		}
		return 1;
	}

	public static HashMap<String, Integer> formChainMap(
			ArrayList<EventChain> goldChains) {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for(int i=0;i<goldChains.size();i++) {
			EventChain ec = goldChains.get(i);
			for(EventMention em : ec.getEventMentions()) {
				map.put(ec.toString(), i);
			}
		}
		return map;
	}
}
