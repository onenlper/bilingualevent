package util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.EntityMention;
import model.EventChain;
import model.EventMention;
import model.EventMentionArgument;

public class Invest {

	static ACEChiDoc document;

	static HashMap<String, Integer> typeStats = new HashMap<String, Integer>();
	static HashMap<String, HashMap<String, Integer>> pairStats = new HashMap<String, HashMap<String, Integer>>();

	public static void addMap(String pair, HashMap<String, Integer> stats) {
		Integer i = stats.get(pair);
		if (i == null) {
			stats.put(pair, 1);
		} else {
			stats.put(pair, i.intValue() + 1);
		}
	}

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_Chinese_all");
		// HashMap<String, HashSet<String>> knownTrigger =
		// Common.readFile2Map6("chinese_trigger_known");
		int known = 0;
		int unkown = 0;
		int zeroArg = 0;
		int triggerNumber = 0;
		HashSet<String> labels = new HashSet<String>();
		HashSet<String> subTypes = new HashSet<String>();
		HashMap<String, HashSet<String>> roleMap = new HashMap<String, HashSet<String>>();

		HashMap<String, HashSet<String>> typesMap = new HashMap<String, HashSet<String>>();
		int G = 0;

		double overall = 0;
		double corefDiffRole = 0;
		double notCorefSameRole = 0;
		double noCorefArgument = 0;
		for (String file : files) {
			// System.out.println(G++);
			int a = file.lastIndexOf(File.separator);
			// System.out.println(file.substring(a + 1));
			System.out.println(file);
			document = new ACEChiDoc(file);
			// ArrayList<EntityMention> goldEntityMentions =
			// document.entityMentions;
			// ArrayList<EventMention> eventMentions =
			// document.goldEventMentions;
			// Collections.sort(eventMentions);
			// triggerNumber += eventMentions.size();

			// String text = document.content.replaceAll("\\s+",
			// "").replace("\n", "").replace("\r", "");
			// if (text.contains("西安") && text.contains("北京")) {
			// System.out.println(text);
			// System.out.println(file);
			// }
			HashSet<String> set = new HashSet<String>();
			ArrayList<EventChain> goldEventChains = document.goldEventChains;
			for (EventChain chain : goldEventChains) {
				ArrayList<EventMention> eventMentions = chain.getEventMentions();
				// if (eventMentions.size() <= 1) {
				// continue;
				// }
				// System.out.println("======================");
				for (EventMention e : eventMentions) {
					if (e.getEventMentionArguments().size() == 0) {
						zeroArg++;
						// System.out.println("X" + eventMention.getAnchor() +
						// ":" +
						// eventMention.type + "#" + eventMention.subType);
					}
					triggerNumber++;
					set.add(e.subType);
				}
			}

			ArrayList<EventMention> ems = document.goldEventMentions;
			Collections.sort(ems);
			for (int i = 0; i < ems.size(); i++) {
				EventMention em1 = ems.get(i);
				String preType = ems.get(i).subType;
				HashSet<String> nexts = new HashSet<String>();
				for (int j = i + 1; j < ems.size(); j++) {
					EventMention em2 = ems.get(j);
					if (em2.start - em1.start < 50) {
						nexts.add(em2.subType);
					}
				}
				addMap(preType, typeStats);
				HashMap<String, Integer> nextMap = pairStats.get(preType);
				if (nextMap == null) {
					nextMap = new HashMap<String, Integer>();
					pairStats.put(preType, nextMap);
				}
				for (String next : nexts) {
					addMap(next, nextMap);
				}
			}

			// for (EventMention e1 : eventMentions) {
			//					
			// boolean print = false;
			// HashSet<String> role = new HashSet<String>();
			// for(EventMentionArgument arg : e1.eventMentionArguments) {
			// if(role.contains(arg.role)) {
			// print = true;
			// break;
			// }
			// role.add(arg.role);
			// }
			// if(print) {
			// System.out.println(printIt(e1));
			// }
			//					
			// String trType = e1.subType;
			// HashSet<String> roles = roleMap.get(trType);
			// if(roles==null) {
			// roles = new HashSet<String>();
			// roleMap.put(trType, roles);
			// }
			// roles.addAll(role);
			//					
			// ArrayList<EventMentionArgument> args1 = e1.eventMentionArguments;
			// loop: for (EventMention e2 : eventMentions) {
			// overall++;
			// boolean corefDiffRole_ = false;
			// ArrayList<EventMentionArgument> args2 = e2.eventMentionArguments;
			//
			// HashSet<String> corefRoles = new HashSet<String>();
			// HashSet<String> notCorefRoles = new HashSet<String>();
			// for (EventMentionArgument arg1 : args1) {
			// Entity en1 = document.id2EntityMap.get(arg1.getRefID());
			// if (en1 == null) {
			// continue;
			// }
			// for (EventMentionArgument arg2 : args2) {
			// Entity en2 = document.id2EntityMap.get(arg2.getRefID());
			// if (en2 == null) {
			// continue;
			// }
			//
			// if (arg1.role.equals(arg2.role) && en1 != en2) {
			// notCorefRoles.add(arg1.role);
			// }
			//
			// if (arg1.role.equals(arg2.role) && en1 == en2) {
			// corefRoles.add(arg1.role);
			// }
			//
			// if (!arg1.role.equals(arg2.role) && en1 == en2) {
			// corefDiffRole_ = true;
			// }
			// }
			// }
			// notCorefRoles.removeAll(corefRoles);
			// if (notCorefRoles.size() != 0) {
			//
			// System.out.println(printIt(e1));
			// System.out.println( (e2));
			// System.out.println("-----------------");
			//							
			// notCorefSameRole++;
			// }
			//
			// if (corefRoles.size() == 0) {
			// noCorefArgument++;
			// }
			//
			// if (corefDiffRole_) {
			// corefDiffRole++;
			// }
			//
			// }

			// String t = eventMention.getType();
			// String st = eventMention.getSubType();
			// HashSet<String> stSet = typesMap.get(t);
			// if(stSet==null) {
			// stSet = new HashSet<String>();
			// typesMap.put(t, stSet);
			// }
			// stSet.add(st);
			// }
			// String trigger = eventMention.getAnchor().replace(" ",
			// "").replace("\n", "");
			// if (knownTrigger.containsKey(trigger)) {
			// known++;
			// System.out.println(eventMention.getAnchor() + ":" +
			// eventMention.type + "#" + eventMention.subType);
			// } else {
			// unkown++;
			// System.out.println("X" + eventMention.getAnchor() + ":" +
			// eventMention.type + "#"
			// + eventMention.subType);
			// }
			// if (eventMention.getEventMentionArguments().size() == 0) {
			// zeroArg++;
			// // System.out.println("X" + eventMention.getAnchor() + ":" +
			// // eventMention.type + "#" + eventMention.subType);
			// }
			// String triggerType = eventMention.getSubType();
			// subTypes.add(triggerType);
			// HashSet<String> relateRoles = roleMap.get(triggerType);
			// if (relateRoles == null) {
			// relateRoles = new HashSet<String>();
			// roleMap.put(triggerType, relateRoles);
			// }
			// for (EventMentionArgument argument :
			// eventMention.getEventMentionArguments()) {
			// relateRoles.add(argument.getRole());
			// labels.add(eventMention.subType + "_" + argument.getRole());
			// int entityID = getEntityID(argument, goldEntityMentions);
			// System.out.println(argument.getExtent() + "@" +
			// argument.getRole() + "###" + entityID);
			// }
			// }
		}
		// System.out.println("corefDiffRole: " + corefDiffRole / overall);
		// System.out.println("notCorefSameRole: " + notCorefSameRole /
		// overall);
		// System.out.println("noCorefArgument: " + noCorefArgument / overall);
		// System.out.println("Know:\t" + known);
		// System.out.println("Unknow:\t" + unkown);
		// System.out.println("Unknown Percent:\t" +
		// ((double)unkown/(double)(known+unkown)));
		// System.out.println("triggerNumber:\t" + triggerNumber);
		// System.out.println("zeroArg:\t" + zeroArg);
		//		
		// for(String type : Util.subTypes) {
		// if(type.equals("null")) {
		// labels.add(type + "_Na");
		// } else {
		// labels.add(type + "_null");
		// }
		// }
		//		
		// System.out.println(labels.size());
		//		
		//		
		//		
		// ArrayList<String> labelLines = new ArrayList<String>();
		// int k=1;
		// for(String label : labels) {
		// labelLines.add(label + " " + (k++));
		// }
		// Common.outputLines(labelLines, "jointLabel");
		//		
		// String template =
		// "triggerType(tr, $1) => argument(tr, +ar, role) ^ (role=Place)";
		//		
		// for(String key : roleMap.keySet()) {
		// StringBuilder sb = new StringBuilder();
		// sb.append("triggerType(tr, ").append(key).append(") => argument(tr, ar, Null) v ");
		// // System.out.println(key);
		// for(String role : roleMap.get(key)) {
		// // System.out.print(role + " ");
		// sb.append("argument(tr, ar, ").append(role).append(") v ");
		// }
		// String formular = sb.toString().trim();
		// formular = formular.substring(0, formular.length()-1).trim() + ".";
		// formular = "formulars.append(\'" + formular + "\')";
		// System.out.println(formular);
		// }
		// System.out.println(subTypes.size());
		//		
		//
		// double th = .1;
		// // while (true) {
		// int qualify = 0;
		// for (String key : pairStats.keySet()) {
		// HashMap<String, Integer> nextStat = pairStats.get(key);
		// double denom = (double) (typeStats.get(key));
		// // System.err.println("===============");
		// // System.err.println(key + ":\t" + denom);
		// // System.err.println("++++");
		// for (String next : nextStat.keySet()) {
		// double nom = (double) nextStat.get(next);
		// // System.err.println(key + "-" + next + ":\t" + nom / denom + "\t" +
		// nom);
		// double prob = nom / denom;
		// if (prob > th) {
		// System.out.println(key + ":" + next + "\t" + nom / denom );
		// qualify++;
		// }
		// }
		// }
		// System.out.println(th + ":\t" + qualify);
		// // th -= .01;
		// // if (th <= 0) {
		// // break;
		// // }
		// // }
		// for (String trTy : roleMap.keySet()) {
		// HashSet<String> roles = roleMap.get(trTy);
		// StringBuilder sb = new StringBuilder();
		//
		// // TriggerType(i,j,k,+t)=> ArgType(i,j,k,a,+r)
		//
		// sb.append("TriggerType(i,j,k,").append("T-" + trTy).append(") => ");
		//
		// ArrayList<String> arrs = new ArrayList<String>(roles);
		//
		// for (int i = 0; i < arrs.size(); i++) {
		// String role = arrs.get(i);
		// sb.append("ArgType(i,j,k,a,").append("A-" + role).append(") v ");
		// }
		// sb.append("ArgType(i,j,k,a,A-None).");
		// System.out.println(sb.toString());
		// }
		// System.out.println("TriggerType(i,j,k,T-None) => ArgType(i,j,k,a,A-None).");
		//
		// System.out.println("================================");
		System.out.println(zeroArg);
		System.out.println(triggerNumber);
	}

	private static StringBuilder printIt(EventMention eventMention) {
		StringBuilder sb = new StringBuilder();
		sb.append("Example: ").append(eventMention.getLdcScope()).append("\n");
		sb.append("Trigger: ").append(eventMention.getAnchor()).append("\n");
		sb.append("Type: ").append(eventMention.getSubType() + "#" + eventMention.getType()).append("\n");
		sb.append("Polarity: ").append(eventMention.polarity).append("\n");
		sb.append("Modality: ").append(eventMention.modality).append("\n");
		sb.append("Genericity: ").append(eventMention.genericity).append("\n");
		sb.append("Tense: ").append(eventMention.tense).append("\n");
		for (EventMentionArgument argument : eventMention.getEventMentionArguments()) {
			String refID = argument.getRefID();
			int eid = -1;
			if (document.id2EntityMap.containsKey(refID)) {
				eid = document.id2EntityMap.get(refID).entityIdx;
			}
			sb.append("Argument (").append(argument.getRole()).append(" " + eid).append("): ").append(
					argument.getExtent()).append("\n");
		}
		// sb.append("=============================").append("\n");
		return sb;
	}

	public static int getEntityID(EventMentionArgument argument, ArrayList<EntityMention> mentions) {
		for (EntityMention mention : mentions) {
			if (argument.getStart() == mention.start && argument.getEnd() == mention.end) {
				return mention.entityIndex;
			}
		}
		return -1;
	}

}
