package event.postProcess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;

public class JointEvaluate {

	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("java ~ [folder]");
			System.exit(1);
		}
		Util.part = args[0];
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + Util.part);
		HashMap<String, HashMap<String, EventMention>> allMentions = jointSVMLine("");
		Util.outputResult(allMentions, "full_joint/result" + Util.part);
		System.out.println("Overall performance:");
		CrossValidation.evaluate("svm", allMentions, files, "chi");
	}

	public static HashMap<String, HashMap<String, EventMention>> jointSVMLine(String control) {
		HashMap<String, HashMap<String, EventMention>> eventMentionses = new HashMap<String, HashMap<String, EventMention>>();
		HashMap<String, String> labels = Common.readFile2Map2("jointLabel");

		HashMap<String, String> oppLabels = new HashMap<String, String>();
		for (String label : labels.keySet()) {
			oppLabels.put(labels.get(label), label);
		}

		ArrayList<String> pairLines = Common.getLines("data/Joint_Lines_" + control + "testsvm" + Util.part);
		ArrayList<String> pairResultLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/Joint_"
				+ control + Util.part);
		HashMap<String, ACEChiDoc> documentSet = new HashMap<String, ACEChiDoc>();
		for (int i = 0; i < pairLines.size(); i++) {
			String pairLine = pairLines.get(i);
			String pairResultLine = pairResultLines.get(i);

			String tokens[] = pairResultLine.split("\\s+");

			String label = oppLabels.get(tokens[0]);
//			System.out.println(label);
			ArrayList<Double> confidences = new ArrayList<Double>();
			
			for(int k=1;k<tokens.length;k++) {
				confidences.add(Double.valueOf(tokens[k]));
			}
			
			String triggerType = label.split("_")[0];
			String argRole = label.split("_")[1];			
			tokens = pairLine.split("\\s");
			String fileID = tokens[0];
			ACEChiDoc document = documentSet.get(fileID);
			if(document==null) {
				document = new ACEChiDoc(fileID);
				documentSet.put(fileID, document);
			}

			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionses.put(fileID, eventMentions);
			}

			int mentionStart = Integer.valueOf(tokens[1]);
			int mentionEnd = Integer.valueOf(tokens[2]);

			EventMention temp = new EventMention();
			temp.setAnchorStart(mentionStart);
			temp.setAnchorEnd(mentionEnd);
			
			if(!eventMentions.containsKey(temp.toString())) {
				eventMentions.put(temp.toString(), temp);
			}
		
			EventMention mention = eventMentions.get(temp.toString());
			mention.increaseType(triggerType);
			
//			if (!argRole.equalsIgnoreCase("null")) {
				EventMentionArgument argument = new EventMentionArgument();
				argument.setStart(Integer.valueOf(tokens[8]));
				argument.setEnd(Integer.valueOf(tokens[9]));
				argument.setRole(argRole);
				argument.jointLabel = label;
				mention.eventMentionArguments.add(argument);
				
				argument.fullJointConfidences = confidences;
				
//			}
		}
		int count = 0;
		
		//trigger type inference 
		for(String fileID : eventMentionses.keySet()) {
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			for(String key : eventMentions.keySet()) {
				eventMentions.get(key).assignTypeFromTypeHash();
				if(!eventMentions.get(key).subType.equals("null")) {
					count += eventMentions.get(key).eventMentionArguments.size(); 
				}
			}
		}
		//argument role back inference
		for(String fileID : eventMentionses.keySet()) {
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			for(String key : eventMentions.keySet()) {
				EventMention eventMention = eventMentions.get(key);
				String triggerType = eventMention.getSubType();
				if(!triggerType.equalsIgnoreCase("null")) {
					for(int i=0;i<eventMention.eventMentionArguments.size();i++) {
						EventMentionArgument argument = eventMention.eventMentionArguments.get(i);
						if(argument.changeType) {
							if(argument.jointLabel.startsWith("null_")) {
								System.out.println(argument.jointLabel);
							}
							ArrayList<Double> confidences = argument.fullJointConfidences;
							ArrayList<Integer> orders = getOrderIndex(confidences);
							for(int order : orders) {
								String label = oppLabels.get(Integer.toString(order));
								if(label.startsWith(triggerType+"_")) {
									String roleCandidate = label.split("_")[1];
									argument.role = roleCandidate;
									break;
								}
							}
						}
						if(argument.role.equalsIgnoreCase("null")) {
							eventMention.eventMentionArguments.remove(i);
							i--;
						}
					}
				}
			}
		}
		return eventMentionses;
	}
	
	public static ArrayList<Integer> getOrderIndex(ArrayList<Double> confidences) {
		ArrayList<Integer> orders = new ArrayList<Integer>();
		
		ArrayList<Double> copy = new ArrayList<Double>();
		for(double confidence : confidences) {
			copy.add(confidence);
		}
		
		Collections.sort(copy);
		Collections.reverse(copy);
		
		for(double conf : copy) {
			orders.add(confidences.indexOf(conf)+1);
		}
		
		return orders;
	}
}
