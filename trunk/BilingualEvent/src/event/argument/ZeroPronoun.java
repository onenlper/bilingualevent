package event.argument;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;
import event.postProcess.CrossValidation;

public class ZeroPronoun {
	public static void main(String args[]) {
		printZero();
//		changeZero(args);
	}
	
	public static void printZero() {
		ArrayList<String> files = Common.getLines("ACE_Chinese_train1");
		int z = 0;
		int all = 0;
		int p = 0;
		for (String file : files) {
			ACEChiDoc document = new ACEChiDoc(file);
			ArrayList<EventMention> mentions = document.goldEventMentions;
			all += mentions.size();
			for (EventMention mention : mentions) {
				boolean zero = Util.isZeroPronoun(mention, document);
				if (zero && mention.zeroSubjects.size() > 0) {
					ArrayList<EventMentionArgument> arguments = mention.eventMentionArguments;
					EntityMention subject = mention.zeroSubjects.get(0);
					boolean right = false;
					for (EventMentionArgument argument : arguments) {
						if (subject.end == argument.getEnd()) {
							p++;
							right = true;
							break;
						}
					}
					if (right) {
						StringBuilder sb = new StringBuilder();
						sb.append(mention.getAnchor()).append("#").append(subject.getExtent()).append(" ").append(file);
						System.out.println(sb.toString());
					}
					z++;
				}
			}
		}
		System.out.println((double) p / (double) z);
		System.out.println(p);
		System.out.println(z);
		System.out.println(all);
	}

	public static void changeZero(String args[]) {
		for (int i = 1; i <= 10; i++) {
			Util.part = Integer.toString(i);
			HashMap<String, HashMap<String, EventMention>> mentionses = Util.readResult(args[0] + "_" + args[1] +"/result" + Util.part, "chi");
			for (String file : mentionses.keySet()) {
				HashMap<String, EventMention> mentions = mentionses.get(file);
				ACEChiDoc document = new ACEChiDoc(file);
				for (EventMention mention : mentions.values()) {
					boolean zero = Util.isZeroPronoun(mention, document);
					if (zero && mention.zeroSubjects.size() > 0) {
						ArrayList<EventMentionArgument> arguments = mention.eventMentionArguments;
						EntityMention subject = mention.zeroSubjects.get(0);
						for (EventMentionArgument argument : arguments) {
							if (subject.end == argument.getEnd()) {
								if (argument.role.equals("null")) {
									ArrayList<Double> confidences = argument.roleConfidences;
									ArrayList<Double> copy = new ArrayList<Double>();
									for (double c : confidences) {
										copy.add(c);
									}
									Collections.sort(copy);
									Collections.reverse(copy);
									for (double c : copy) {
										int index = confidences.indexOf(c);
										if (!Util.roles.get(index).equals("null")) {
											argument.role = Util.roles.get(index);
											break;
										}
									}
								}
							}
						}
					}
				}
			}
			ArrayList<String> files = Common.getLines("ACE_Chinese_test" + Util.part);
			CrossValidation.evaluate("svm", mentionses, files, "chi");
			File f = new File(args[0] + "_" + args[1] + "Zero");
			if(!f.exists()) {
				f.mkdir();
			}
			Util.outputResult(mentionses, args[0] + "_" + args[1] + "Zero/result" + Util.part);
		}
	}
}
