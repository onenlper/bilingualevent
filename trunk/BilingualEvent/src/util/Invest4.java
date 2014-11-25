package util;

import java.util.ArrayList;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;

public class Invest4 {

	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("");
		for(String line : lines) {
			ACEDoc doc = new ACEChiDoc(line);
			ArrayList<EventChain> ecs = doc.goldEventChains;
			
			for(EventChain ec : ecs) {
				
				for(int i=0;i<ec.getEventMentions().size();i++) {
					EventMention m2 = ec.getEventMentions().get(i);
					for(int j=i-1;j>=0;j--) {
						EventMention m1 = ec.getEventMentions().get(j);
						
						if(Util._commonBV_(m1, m2) ||
								m1.getAnchor().equals(m2.getAnchor())
								) {
							
						} else {
							System.out.println(m1.getAnchor());
							System.out.println(m1.getExtent());
							System.out.println(m2.getAnchor());
							System.out.println(m2.getExtent());
							System.out.println("===============================");
						}
						
						
					}
				}
				
			}
		}
	}
}
