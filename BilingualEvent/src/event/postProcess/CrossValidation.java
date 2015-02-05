package event.postProcess;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEChiDoc;
import model.ACEDoc;
import model.ACEEngDoc;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;

public class CrossValidation {
	public static void main(String args[]) {
		if (args.length < 2) {
			System.out.println("java ~ [pipe|joint] [maxent|svm]");
			System.exit(1);
		}
		if (args.length == 2) {
			HashMap<String, HashMap<String, EventMention>> pipelineResults = new HashMap<String, HashMap<String, EventMention>>();
			for (int i = 1; i <= 10; i++) {
				HashMap<String, HashMap<String, EventMention>> part = Util.readResult(args[0] + "_" + args[1]
						+ "/result" + Integer.toString(i), "chi");
				for (String key : part.keySet()) {
					pipelineResults.put(key, part.get(key));
				}
			}
			ArrayList<String> lines = Common.getLines("ACE_Chinese_all");
			if (args[0].equalsIgnoreCase("full")) {
				args[1] = "svm";
			}
			evaluate(args[1], pipelineResults, lines, "chi");
		} else if (args.length == 3) {
			HashMap<String, HashMap<String, EventMention>> pipelineResults = new HashMap<String, HashMap<String, EventMention>>();
			HashMap<String, HashMap<String, EventMention>> part = Util.readResult(args[0] + "_" + args[1] + "/result"
					+ args[2], "chi");
			for (String key : part.keySet()) {
				pipelineResults.put(key, part.get(key));
			}
			ArrayList<String> lines = Common.getLines("ACE_Chinese_test" + args[2]);
			if (args[0].equalsIgnoreCase("full")) {
				args[1] = "svm";
			}
			evaluate(args[1], pipelineResults, lines, "chi");
		}
	}

	public static double mlnTh = 0;

	public static void evaluate(String model, HashMap<String, HashMap<String, EventMention>> pipelineResults,
			ArrayList<String> files, String lang) {
		boolean svm = false;
		boolean maxent = false;
		boolean mln = false;
		if (model.startsWith("svm")) {
			svm = true;
		}
		if (model.equalsIgnoreCase("maxent")) {
			maxent = true;
		}
		if (model.equalsIgnoreCase("mln")) {
			mln = true;
		}
		int sEM = 0;
		int gEM = 0;
		int hitEM = 0;
		int hitType = 0;
		int sArg = 0;
		int gArg = 0;
		int hitArg = 0;
		int hitRole = 0;

		for (String file : files) {
			HashMap<String, EventMention> eventMentions = pipelineResults.get(file);
			if (eventMentions == null) {
				eventMentions = new HashMap<String, EventMention>();
			}
			ACEDoc document;
			if(lang.equals("chi")) {
				document = new ACEChiDoc(file);
			} else {
				document = new ACEEngDoc(file);
			}
			ArrayList<EventMention> gEventMentions = document.goldEventMentions;

			ArrayList<EventMention> sEventMentions = new ArrayList<EventMention>();
			sEventMentions.addAll(eventMentions.values());

			gEM += gEventMentions.size();

			for (EventMention s : sEventMentions) {
				if ((s.subType.equalsIgnoreCase("None"))
						|| s.subType.equalsIgnoreCase("null")
						|| (maxent && s.confidence <= 0.5)
						|| (svm && (s.confidence < 0 || s.subType.equalsIgnoreCase("null") || s.subType
								.equalsIgnoreCase("none")))) {
					// System.out.println(s.confidence);
					// System.out.println(s.type);
					// System.exit(1);
					continue;
				}
				sEM++;

				for (EventMentionArgument argument : s.eventMentionArguments) {
					if (!argument.role.equalsIgnoreCase("null") && !argument.role.equalsIgnoreCase("none")) {
						sArg++;
					}
				}
			}

			for (EventMention g : gEventMentions) {
				gArg += g.eventMentionArguments.size();
			}

			for (EventMention s : sEventMentions) {
				// System.out.println(s.confidence);
				if ((s.subType.equalsIgnoreCase("None"))
						|| s.subType.equalsIgnoreCase("null")
						|| (maxent && s.confidence <= 0.5)
						|| (svm && (s.confidence < 0 || s.subType.equalsIgnoreCase("null") || s.subType
								.equalsIgnoreCase("none")))) {
					continue;
				}
				// System.out.println(s.confidence);
				for (EventMention g : gEventMentions) {
					if (s.equals(g)) {
						hitEM++;
						if (s.subType.equals(g.subType)) {
							hitType++;
						} else {
							// System.out.println(g.getAnchor() + "#" +
							// g.document.fileID + "@" + s.type + ":" + g.type);
							// System.out.println(hitType);
							// System.out.println(s.subType + " # " +
							// g.subType);
						}
					}
				}
			}

			for (EventMention s : sEventMentions) {
				// System.out.println(s.confidence);
				if (s.subType.equalsIgnoreCase("None")
						|| s.subType.equalsIgnoreCase("null")
						|| (maxent && s.confidence <= 0.5)
						|| (svm && (s.confidence < 0 || s.subType.equalsIgnoreCase("null") || s.subType
								.equalsIgnoreCase("none")))) {
					continue;
				}
				for (EventMention g : gEventMentions) {
					if (!s.equals(g)) {
						continue;
					}
					if (!s.getSubType().equals(g.getSubType())) {
						continue;
					}
					// System.out.println("============");
					// System.out.println(g.getEventMentionArguments());
					// System.out.println(s.getEventMentionArguments());
					for (EventMentionArgument sA : s.getEventMentionArguments()) {
						if (sA.role.equalsIgnoreCase("null") || sA.role.equalsIgnoreCase("none")) {
							continue;
						}
						for (EventMentionArgument gA : g.getEventMentionArguments()) {
							if ( (sA.equals(gA) || false 
									|| (gA.getEntityMention().headStart==sA.getStart() && gA.getEntityMention().headEnd==sA.getEnd())
									)
									&& s.getSubType().equals(g.getSubType())) {
								if (sA.getRole().equals(gA.getRole())) {
									hitRole++;
									break;
								} else {
									// System.out.println(g.getAnchor() + "#" +
									// gA.getExtent() + "@" + gA.role + ":" +
									// sA.role +" "+g.document.fileID);
								}
							}
						}
					}

					for (EventMentionArgument sA : s.getEventMentionArguments()) {
						if (sA.role.equalsIgnoreCase("null") || sA.role.equalsIgnoreCase("none")) {
							continue;
						}
						for (EventMentionArgument gA : g.getEventMentionArguments()) {
							if ( (sA.equals(gA) || false 
									|| (gA.getEntityMention().headStart==sA.getStart() && gA.getEntityMention().headEnd==sA.getEnd())
									)
									&& s.getSubType().equals(g.getSubType())) {
								hitArg++;
								break;
							}
						}
					}
				}
			}
		}

		System.out.println("===========\nTrigger Identification");
		double r = (double) hitEM / (double) gEM;
		double p = (double) hitEM / (double) sEM;
		double f = 2 * r * p / (r + p);
		System.out.println("Gold EM:" + gEM);
		System.out.println("Sysem EM:" + sEM);
		System.out.println("Hit EM:" + hitEM);
		System.out.println("R:\t" + r * 100);
		System.out.println("P:\t" + p * 100);
		System.out.println("F:\t" + f * 100);
		System.out.println("===========\nTrigger Labeling");
		r = (double) hitType / (double) gEM;
		p = (double) hitType / (double) sEM;
		f = 2 * r * p / (r + p);
		System.out.println("R:\t" + r * 100);
		System.out.println("P:\t" + p * 100);
		System.out.println("F:\t" + f * 100);
		System.out.println("===========\nArgument Identification");
		r = (double) hitArg / (double) gArg;
		p = (double) hitArg / (double) sArg;
		f = 2 * r * p / (r + p);
		System.out.println("Gold Arg:" + gArg);
		System.out.println("Sysem Arg:" + sArg);
		System.out.println("Hit Arg:" + hitArg);
		System.out.println("R:\t" + r * 100);
		System.out.println("P:\t" + p * 100);
		System.out.println("F:\t" + f * 100);
		System.out.println("===========\nArgument Labeling");
		r = (double) hitRole / (double) gArg;
		p = (double) hitRole / (double) sArg;
		f = 2 * r * p / (r + p);
		System.out.println("R:\t" + r * 100);
		System.out.println("P:\t" + p * 100);
		System.out.println("F:\t" + f * 100);
		System.out.println("===========");
	}
}
