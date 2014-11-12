package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventChain;
import model.EventMention;
import util.Common;

public class EventCorefTrain {

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_English_train0");
		EventCorefFea fea = new EventCorefFea(true, "corefFea");
		ArrayList<String> trainLines = new ArrayList<String>();
		for(String file : files) {
			ACEDoc doc = new ACEEngDoc(file);
			ArrayList<EventMention> ems = doc.goldEventMentions;
			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();
			Collections.sort(ems);
			
			for(int i=0;i<ems.size();i++) {
				EventMention ana = ems.get(i);
				
				int corefID = -1;
				for(int j=activeChains.size()-1;j>=0;j--) {
					EventChain ec = activeChains.get(j);
					EventMention lem = ec.getEventMentions().get(ec.getEventMentions().size()-1);
					
					boolean coref = doc.eventCorefMap.get(ana.toName())==doc.eventCorefMap.get(lem.toName());
					if(coref) {
						corefID = j;
					}
					fea.configure(ec, ana, doc, ems);
					
					String feaStr = fea.getSVMFormatString();
					if(coref) {
						trainLines.add("+1 " + feaStr);	
					} else {
						trainLines.add("-1 " + feaStr);
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
		}
		fea.freeze();
		Common.outputLines(trainLines, "eventTrain");
	}
}
