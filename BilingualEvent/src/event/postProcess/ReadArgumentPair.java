package event.postProcess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;

public class ReadArgumentPair {
	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("java ~ [folder]");
			System.exit(1);
		}
		Util.part = args[0];
		HashMap<String, HashMap<String, EventMention>> eventMentionses = loadPairResults(Util.part);

	}

	public static void orderArguments(HashMap<String, HashMap<String, EventMention>> eventMentionses) {
		for (String fileID : eventMentionses.keySet()) {
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			for (String key : eventMentions.keySet()) {
				EventMention eventMention = eventMentions.get(key);
				Collections.sort(eventMention.eventMentionArguments);
			}
		}
	}

	public static HashMap<String, HashMap<String, EventMention>> loadPairResults(String folder) {
		ArrayList<String> argumentPairLines = Common
				.getLines("/users/yzcchen/workspace/Coling2012/src/data/Joint_argumentPairLines_testsvm" + folder);
		ArrayList<String> resultsLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/argumentPair"
				+ folder);

		HashMap<String, HashMap<String, EventMention>> eventMentionses = new HashMap<String, HashMap<String, EventMention>>();

		for (int i = 0; i < argumentPairLines.size(); i++) {
			String line = argumentPairLines.get(i);
			String tokens[] = line.split("\\s+");
			String fileID = tokens[0];

			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
				eventMentionses.put(fileID, eventMentions);
			}
			EventMention temp = new EventMention();
			temp.setAnchorStart(Integer.valueOf(tokens[1]));
			temp.setAnchorEnd(Integer.valueOf(tokens[2]));

			EventMention eventMention = eventMentions.get(temp.toString());
			if (eventMention == null) {
				eventMention = temp;
				eventMentions.put(temp.toString(), eventMention);
			}

			EventMentionArgument tempArg1 = new EventMentionArgument();
			tempArg1.setStart(Integer.valueOf(tokens[3]));
			tempArg1.setEnd(Integer.valueOf(tokens[4]));
			EventMentionArgument arg1 = eventMention.argumentHash.get(tempArg1.toString());
			if (arg1 == null) {
				arg1 = tempArg1;
				eventMention.addArgument(arg1);
			}

			EventMentionArgument tempArg2 = new EventMentionArgument();
			tempArg2.setStart(Integer.valueOf(tokens[5]));
			tempArg2.setEnd(Integer.valueOf(tokens[6]));
			EventMentionArgument arg2 = eventMention.argumentHash.get(tempArg2.toString());
			if (arg2 == null) {
				arg2 = tempArg2;
				eventMention.addArgument(arg2);
			}

			String label = resultsLines.get(i).split("\\s+")[0];
			if (label.equals("1")) {
				arg1.vote++;
			} else {
				arg2.vote++;
			}
		}
		return eventMentionses;
	}
}
