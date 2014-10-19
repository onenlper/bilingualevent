package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EventMention;

public class Test {
	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("ACE_Chinese_all");
		HashMap<String, HashSet<String>> possibleTypes = new HashMap<String, HashSet<String>>();
		
		HashSet<String> anchors = new HashSet<String>();
		
		for(String line : lines) {
			ACEChiDoc document = new ACEChiDoc(line);
			ArrayList<EventMention> triggers = document.goldEventMentions;
			for(EventMention mention : triggers) {
				String trigger = mention.getAnchor();
				String type = mention.type;
				if(trigger.equals("病情")) {
					System.out.println(line);
				}
				anchors.add(trigger.replace("\n", "").replace("\r", "").replace(" ", ""));
				HashSet<String> set = possibleTypes.get(trigger);
				if(set==null) {
					set = new HashSet<String>();
					possibleTypes.put(trigger, set);
				}
				set.add(type);
			}
		}
		ArrayList<String> outs = new ArrayList<String>();
		for(String key : possibleTypes.keySet()) {
			HashSet<String> set = possibleTypes.get(key);
			StringBuilder sb = new StringBuilder();
			sb.append(key).append(":");
			for(String s : set) {
				sb.append(s).append(" ");
			}
			outs.add(sb.toString());
		}
		Common.outputLines(outs, "triggerType");
		
		Common.outputHashSet(anchors, "allAnchors");
	}
}
