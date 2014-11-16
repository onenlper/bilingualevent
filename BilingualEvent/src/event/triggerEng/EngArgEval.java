package event.triggerEng;

import java.util.ArrayList;
import java.util.HashMap;

import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;
import event.postProcess.CrossValidation;

public class EngArgEval {
	public static void main(String args[]) {
		if (args.length < 3) {
			System.out
					.println("java ~ [folder]");
			System.exit(1);
		}
		Util.part = args[2];
		String model = args[1];
		System.out.println("Output final result:\n==========");
		ArrayList<String> files = Common.getLines("ACE_English_test" + args[2]);
		HashMap<String, HashMap<String, EventMention>> allMentions = null;
		allMentions = jointSVMLine();
		System.out.println("Overall performance:");
		CrossValidation.evaluate(model, allMentions, files, "eng");
		
		Util.outputResult(allMentions, "data/result"
				+ Util.part);
	}

	public static HashMap<String, HashMap<String, EventMention>> jointSVMLine() {
		HashMap<String, HashMap<String, EventMention>> eventMentionses = new HashMap<String, HashMap<String, EventMention>>();

		ArrayList<String> argumentLines = Common
				.getLines("data/engArgName_test" + Util.part);
		ArrayList<String> argPreds = Common
				.getLines("/users/yzcchen/tool/svm_multiclass/engArOutput_test" + Util.part);
		ArrayList<String> relateEMLines = Common
				.getLines("data/engTrofArgName_test" + Util.part);

		for (String line : relateEMLines) {
			String tokens[] = line.split("\\s+");

			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses
					.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionses.put(fileID, eventMentions);
			}

			int mentionStart = Integer.valueOf(tokens[1]);
			int mentionEnd = Integer.valueOf(tokens[2]);
			double confidence = Double.valueOf(tokens[3]);
			String mentionType = tokens[4];
			double typeConfidence = Double.valueOf(tokens[5]);
			String subType = tokens[6];
			double subTypeConfidence = Double.valueOf(tokens[7]);

			ArrayList<Double> typeConfidences = new ArrayList<Double>();
			for (int k = 9; k < tokens.length; k++) {
				typeConfidences.add(Double.valueOf(tokens[k]));
			}

			EventMention temp = new EventMention();
			temp.setAnchorStart(mentionStart);
			temp.setAnchorEnd(mentionEnd);
			temp.setType(mentionType);
			temp.setSubType(subType);
			temp.confidence = confidence;
			temp.typeConfidence = typeConfidence;
			temp.subTypeConfidence = subTypeConfidence;
			temp.inferFrom = tokens[8];
			temp.typeConfidences = typeConfidences;

			eventMentions.put(temp.toString(), temp);
		}

		for (int i = 0; i < argumentLines.size(); i++) {
			String argumentLine = argumentLines.get(i);
			String argumentRoleLine = argPreds.get(i);

			String tokens[] = argumentRoleLine.split("\\s+");

			String role = Util.roles.get(Integer.parseInt(tokens[0]) - 1);

			ArrayList<Double> roleConfidences = new ArrayList<Double>();

			for (int k = 1; k < tokens.length; k++) {
				roleConfidences.add(Double.valueOf(tokens[k]));
			}

			String roleResult = Util.roles.get(Integer.parseInt(tokens[0]) - 1);
			String roleConfidence = tokens[Integer.parseInt(tokens[0])];

			tokens = argumentLine.split("\\s");

			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses
					.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionses.put(fileID, eventMentions);
			}

			int mentionStart = Integer.valueOf(tokens[1]);
			int mentionEnd = Integer.valueOf(tokens[2]);

			EventMention temp = new EventMention();
			temp.setAnchorStart(mentionStart);
			temp.setAnchorEnd(mentionEnd);

			EventMention mention = eventMentions.get(temp.toString());

			EventMentionArgument argument = new EventMentionArgument();
			argument.setStart(Integer.valueOf(tokens[8]));
			argument.setEnd(Integer.valueOf(tokens[9]));
			argument.setRole(roleResult);
			argument.roleConfidence = Double.parseDouble(roleConfidence);
			argument.roleConfidences = roleConfidences;
			mention.eventMentionArguments.add(argument);
		}
		return eventMentionses;
	}

}
