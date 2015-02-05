package util;

import java.util.ArrayList;
import java.util.Collections;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;

public class Test2 {
	
	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_Chinese_train" + args[0]);
		double all = 0;
		double coref = 0;
		for(int i=0;i<files.size();i++) {
			String file = files.get(i);
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = i;
			ArrayList<EventChain> eventChains = doc.goldEventChains;
			ArrayList<EventMention> evs = doc.goldEventMentions;
			
			for(EventMention e : evs) {
				Util.identBVs(e, doc);
			}
			
			Collections.sort(evs);
			for(int j=0;j<evs.size();j++) {
				EventMention ev2 = evs.get(j);
				for(int k=j-1;k>=0;k--) {
					EventMention ev1 = evs.get(k);
					if(
							Util._commonBV_(ev2, ev1)  
						&& ev1.getType().equals(ev2.getType()) && !ev1.getAnchor().equals(ev2.getAnchor())) {
						all += 1;
						if(ev1.getEventChain()==ev2.getEventChain()) {
							coref += 1;
						}
					}
				}
			}
/**			
			for(EventChain chain : eventChains) {
				evs = chain.getEventMentions();
				Collections.sort(evs);
				for(int j=1;j<evs.size();j++) {
					EventMention ev2 = evs.get(j);
					boolean find = false;
					for(int k=j-1;k>=0;k--) {
						EventMention ev1 = evs.get(k);
						if(
								Util._commonBV_(ev2, ev1) || 
								ev1.getAnchor().equals(ev2.getAnchor())) {
							find = true;
							break;
						}
					}
					all += 1;
					if(find) {
						coref += 1;
					} else {
						System.out.println(ev2.getAnchor() + "#" + evs.get(j-1).getAnchor());
					}
				}
			}
**/			
		}

		System.out.println(coref/all);
	}
}
