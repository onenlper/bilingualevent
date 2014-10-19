package event.preProcess;

import java.util.ArrayList;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EventMention;
import util.ChineseUtil;
import util.Common;
import util.Util;

public class KnownTrigger {
	public static void main(String args[]) {
		if(args.length!=1) {
			System.out.println("java ~ [folder]");
			System.exit(1);
		}
		Util.part = args[0];
		System.out.println("Build errata table...\n===========");
		ArrayList<String> files = Common.getLines("ACE_Chinese_train"+Util.part);
		HashSet<String> errataTable1 = new HashSet<String>();
		HashSet<String> errataTable2 = new HashSet<String>();
//		HashSet<String> trigger = new HashSet<String>();
		for(String file : files) {
//			System.out.println(file);
			ACEChiDoc document = new ACEChiDoc(file);
			ArrayList<EventMention> mentions = document.goldEventMentions;
			for(EventMention mention : mentions) {
				int position[] = ChineseUtil.findParseFilePosition(mention.getAnchorStart(), mention.getAnchorEnd(), document);
				ArrayList<String> words = document.parseReults.get(position[0]).words;
				String word = words.get(position[1]);
				if(position[1]==position[3] && position[2]==0 && position[4]==word.length()-1) {
					
				} else if(position[1]==position[3]) {
//					System.out.println("Inconsistent: " + mention.getAnchor() + "&" + word);
					errataTable1.add(mention.getAnchor() + "_" + word);
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append(mention.getAnchor());
					for(int i=position[1];i<=position[3];i++) {
						sb.append("_").append(words.get(i));
					}
					errataTable2.add(sb.toString());
				} 
//				trigger.add(mention.getAnchor().replaceAll("\\s+", ""));
			}
		}
//		Common.outputHashSet(trigger, "chinese_trigger_known");
		Common.outputHashSet(errataTable1, "errata1"+Util.part);
		Common.outputHashSet(errataTable2, "errata2"+Util.part);
	}
}