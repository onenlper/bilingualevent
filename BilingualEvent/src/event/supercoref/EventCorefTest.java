package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventChain;
import model.EventMention;

import util.Common;

public class EventCorefTest {
	
	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_English_train0");
		EventCorefFea fea = new EventCorefFea(false, "corefFea");
		double thres = .5;
		for(String file : files) {
			ACEDoc doc = new ACEEngDoc(file);
			ArrayList<EventMention> ems = doc.goldEventMentions;
			
			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();
			Collections.sort(ems);
			
			for(int i=0;i<ems.size();i++) {
				EventMention ana = ems.get(i);
				
				int corefID = -1;
				double maxVal = 0;
				for(int j=activeChains.size()-1;j>=0;j--) {
					EventChain ec = activeChains.get(j);
					
					fea.configure(ec, ana, doc, ems);
					String feaStr = fea.getSVMFormatString();
					double val = test(feaStr);
					if(val>thres && val>maxVal) {
						maxVal = val;
						corefID = j;
					}
				}
				
				if(corefID==-1) {
					EventChain ec = new EventChain();
					ec.addEventMention(ana);
					activeChains.add(ec);
				} else {
					activeChains.get(corefID).addEventMention(ana);
				}
			}
			
//			process activeChains
			
		}
	}
	
	public static double test(String str) {
		return 0;
	}
}
