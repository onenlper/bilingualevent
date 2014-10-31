package coref;

import java.util.ArrayList;

import model.ACEDoc;
import model.EventMention;

public class EMUtil {
	
	public static double alpha = 1;
	public static boolean train = false;

	public static ArrayList<EventMention> getEventMentionInOneS(ACEDoc doc, int sid) {
		ArrayList<EventMention> mentions = new ArrayList<EventMention>();
		for(EventMention em : doc.eventMentions) {
			if(doc.positionMap.get(em.getAnchorStart())[0]==sid) {
				mentions.add(em);
			}
		}
		return mentions;
	}

	public static double getP_C(EventMention ant, EventMention m, ACEDoc doc) {
		// TODO Auto-generated method stub
		return 0;
	}
}
