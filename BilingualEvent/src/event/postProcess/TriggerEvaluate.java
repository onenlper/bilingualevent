package event.postProcess;

import java.util.ArrayList;

import model.ACEChiDoc;
import model.EventMention;
import util.Common;
import util.Util;
import event.preProcess.ChineseTriggerIndent;

public class TriggerEvaluate {
	public static void main(String args[]) {
//		if(args.length!=1) {
//			System.out.println("java ~ [Chinese|English]");
//		}
		ChineseTriggerIndent triggerIndent = null;
//		if(args[0].equalsIgnoreCase("chinese")) {
			triggerIndent = new ChineseTriggerIndent();
//		}
		double goldTrigger = 0;
		double systemTrigger = 0;
		double hitTrigger = 0;
		
		for(int h=1;h<=10;h++) {
			Util.part = Integer.toString(h);
			triggerIndent.loadErrata();
			double goldTrigger2 = 0;
			double systemTrigger2 = 0;
			double hitTrigger2 = 0;
			ArrayList<String> fileList = Common.getLines("ACE_Chinese_test" + Util.part);
			ArrayList<EventMention> allRecallError = new ArrayList<EventMention>();
			ArrayList<EventMention> allPrecisionError = new ArrayList<EventMention>();
			for(String file : fileList) {
				System.out.println(file);
				ACEChiDoc document = new ACEChiDoc(file);
				ArrayList<EventMention> goldEMs = document.goldEventMentions;
				ArrayList<EventMention> systemEMs = triggerIndent.extractTrigger(file);
				goldTrigger += goldEMs.size();
				systemTrigger += systemEMs.size();
				for(int j=0;j<goldEMs.size();j++) {
					EventMention goldEM = goldEMs.get(j);
					for(int k=0;k<systemEMs.size();k++) {
						EventMention systemEM = systemEMs.get(k);
						if(goldEM.getAnchorStart()==systemEM.getAnchorStart() &&
								goldEM.getAnchorEnd()==systemEM.getAnchorEnd()) {
							hitTrigger++;
							systemEMs.remove(k);
							goldEMs.remove(j);
							j--;
							break;
						}
					}
				}
				System.out.println("Recall Error: "); 
				for(EventMention em : goldEMs) {
					em.document = document;
	//				System.out.println(em.getAnchor() + " " + em.getAnchorStart() + " " + em.getAnchorEnd());
				}
				System.out.println("Precision Error");
				for(EventMention em : systemEMs) {
					em.document = document;
	//				System.out.println(em.getAnchor() + " " + em.getAnchorStart() + " " + em.getAnchorEnd());
				}
				allRecallError.addAll(goldEMs);
				allPrecisionError.addAll(systemEMs);
			}
			goldTrigger += goldTrigger2;
			systemTrigger += systemTrigger2;
			hitTrigger += hitTrigger2;
		}
//		System.out.println("Recall Error: "); 
//		for(EventMention em : allRecallError) {
//			System.out.println(em.getAnchor());
//		}
//		System.out.println("============================");
//		System.out.println("Precision Error: ");
//		for(EventMention em : allPrecisionError) {
//			System.out.println(em.getAnchor() + "@" + em.document.fileID);
//		}
//		System.out.println("============================" + allPrecisionError.size());
		double precision = hitTrigger/systemTrigger;
		double recall = hitTrigger/goldTrigger;
		double fscore = 2*precision*recall/(precision+recall);
		System.out.println("System:\t" + systemTrigger);
		System.out.println("Gold:\t" + goldTrigger);
		System.out.println("Hit:\t" + hitTrigger);
		System.out.println("R:\t" + recall);
		System.out.println("P:\t" + precision);
		System.out.println("F:\t" + fscore);
	}
}
