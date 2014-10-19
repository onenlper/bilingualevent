package event.postProcess;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;

public class ArgumentInvest {
	public static void main(String args[]) {
		if(args.length!=2) {
			System.out.println("java ~ [pipe|svm] [maxent|svm]");
			System.exit(1);
		}
		HashMap<String, HashMap<String, EventMention>> pipelineResults = new HashMap<String, HashMap<String, EventMention>>();
		for (int i = 1; i <= 10; i++) {
			HashMap<String, HashMap<String, EventMention>> part = Util.readResult(args[0]+"_"+args[1] + "/result"
				 +Integer.toString(i), "chi");
			for (String key : part.keySet()) {
				pipelineResults.put(key, part.get(key));
			}
		}
		boolean svm = false;
		boolean maxent = false;
		if(args[1].equalsIgnoreCase("svm")) {
			svm = true;
		}
		if(args[1].equalsIgnoreCase("maxent")) {
			maxent = true;
		}
		
		ArrayList<String> files = Common.getLines("ACE_Chinese_all");

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
			ACEChiDoc document = new ACEChiDoc(file);
			ArrayList<EventMention> gEventMentions = document.goldEventMentions;

			ArrayList<EventMention> sEventMentions = new ArrayList<EventMention>();
			sEventMentions.addAll(eventMentions.values());

			gEM += gEventMentions.size();

			ArrayList<EventMentionArgument> sArguments = new ArrayList<EventMentionArgument>();
			ArrayList<EventMentionArgument> gArguments = new ArrayList<EventMentionArgument>();

			for (EventMention s : sEventMentions) {
				if ((maxent&&s.confidence <= 0.5) || (svm&&s.confidence<0)) {
					continue;
				}
				sEM++;
				sArg += s.eventMentionArguments.size();
				sArguments.addAll(s.getEventMentionArguments());
			}

			for (EventMention g : gEventMentions) {
				gArg += g.eventMentionArguments.size();
				gArguments.addAll(g.getEventMentionArguments());
			}

			for (EventMention s : sEventMentions) {
				// System.out.println(s.confidence);
				if ((maxent&&s.confidence <= 0.5) || (svm&&s.confidence<0)) {
					continue;
				}
				for (EventMention g : gEventMentions) {
					if (s.equals(g)) {
						hitEM++;
						if (s.type.equals(g.type)) {
							hitType++;
							ArrayList<EventMentionArgument> sArgs = s.eventMentionArguments;
							ArrayList<EventMentionArgument> gArgs = g.eventMentionArguments;
							for(int i=0;i<sArgs.size();i++) {
								EventMentionArgument sAr = sArgs.get(i);
								for(int j=0;j<gArgs.size();j++) {
									EventMentionArgument gAr = gArgs.get(j);
									if(sAr.equals(gAr)) {
										gArgs.remove(j);
										sArgs.remove(i);
										i--;
										break;
									}
								}
							}
							
							String content = document.content;
							System.out.println("Trigger: " + g.getAnchor() + " ## " + document.fileID);
							System.out.println("-------");
							System.out.println("Recall Error");
							System.out.println("+++");
							for(EventMentionArgument arg : gArgs) {
								int start = arg.getStart();
								int end = arg.getEnd();
								System.out.println(content.substring(start, end+1).replace("\n", "").replace(" ", "") + "#" + arg.getRole());
							}
							System.out.println("-------");
							System.out.println("Precision Error");
							System.out.println("+++");
							for(EventMentionArgument arg : sArgs) {
								int start = arg.getStart();
								int end = arg.getEnd();
								System.out.println(content.substring(start, end+1).replace("\n", "").replace(" ", "") + "#" + arg.getRole());
							}
							System.out.println("=====================");
						}
					}
				}
			}

			for (EventMentionArgument s : sArguments) {
				for (EventMentionArgument g : gArguments) {
					if (s.equals(g) && s.getEventMention().getType().equals(g.getEventMention().getType())
//							&& s.getEventMention().equals(g.getEventMention())
							) {
						hitArg++;
						if (s.getRole().equals(g.getRole())) {
							hitRole++;
						}
					}
				}
			}
		}

		System.out.println("===========\nTrigger Identification");
		double r = (double) hitEM / (double) gEM;
		double p = (double) hitEM / (double) sEM;
		double f = 2 * r * p / (r + p);
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
