package event.argument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;
import event.postProcess.CrossValidation;

public class OnlyOneArg {
	public static void main(String args[]) {
		Util.part = args[0];
		buildOnlyOne();
		onlyOne();
	}
	
	public static HashSet<String> onlyOnes = new HashSet<String>();
	
	public static void buildOnlyOne() {
		ArrayList<String> files = Common.getLines("ACE_Chinese_train" + Util.part);
		HashSet<String> all = new HashSet<String>();
		HashSet<String> moreOne = new HashSet<String>();
		for (String file : files) {
			ACEChiDoc document = new ACEChiDoc(file);
			ArrayList<EventMention> mentions = document.goldEventMentions;
			for (EventMention mention : mentions) {
				ArrayList<EventMentionArgument> arguments = mention.eventMentionArguments;
				HashMap<String, Boolean> onlyMe = new HashMap<String, Boolean>();
				for(EventMentionArgument argument : arguments) {
					String role = argument.getRole();
					if(onlyMe.containsKey(role)) {
						onlyMe.put(role, false);
						moreOne.add(mention.type + "_" + role);
						System.out.println("Duplicate: " + mention.type + "_" + role);
					} else {
						onlyMe.put(role, true);
					}
					all.add(mention.type + "_" + role);
				}
			}
		}
		for(String key : all) {
			if(!moreOne.contains(key)) {
				onlyOnes.add(key);
			}
		}
	}
	
	public static void onlyOne() {
		HashMap<String, HashMap<String, EventMention>> mentionses = Util.readResult("joint_svm/result"+Util.part, "chi");
		for(String file : mentionses.keySet()) {
			HashMap<String, EventMention> mentions = mentionses.get(file);
			for(EventMention mention : mentions.values()) {
				ArrayList<EventMentionArgument> arguments = mention.eventMentionArguments;
				HashMap<String, Double> maxConfidence = new HashMap<String, Double>();
				for(int i=0;i<arguments.size();i++) {
					EventMentionArgument argument = arguments.get(i);
					String role = argument.role;
					if(role.equals("null")) {
						continue;
					}
					double confidence = argument.roleConfidences.get(Util.roles.indexOf(role));
					if(maxConfidence.containsKey(role)) {
						double old = maxConfidence.get(role);
						if(old<confidence) {
							maxConfidence.put(role, confidence);
						}
					} else {
						maxConfidence.put(role, confidence);
					}
				}
				for(int i=0;i<arguments.size();i++) {
					EventMentionArgument argument = arguments.get(i);
					String role = argument.role;
					if(role.equals("null")) {
						continue;
					}
					double confidence = argument.roleConfidences.get(Util.roles.indexOf(role));
					double max = maxConfidence.get(role);
					if(max!=confidence && onlyOnes.contains(mention.type + "_" + role)) {
						argument.role = "null";
					}
				}
			}
		}
		ArrayList<String> files = Common.getLines("ACE_Chinese_test"+Util.part);
		CrossValidation.evaluate("svm", mentionses, files, "chi");
	}
}
