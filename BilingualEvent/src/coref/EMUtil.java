package coref;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEDoc;
import model.EventChain;
import model.EventMention;

public class EMUtil {
	
	public static double alpha = 1.0;
	public static boolean train = false;

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
