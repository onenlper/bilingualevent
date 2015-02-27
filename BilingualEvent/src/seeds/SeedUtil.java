package seeds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import model.ParseResult;
import model.SemanticRole;
import model.stanford.StanfordResult;
import model.stanford.StanfordXMLReader;
import seeds.Seed.Argument;
import seeds.Seed.EventType;
import seeds.Seed.Sent;
import util.Common;
import util.Util;
import entity.semantic.ACECommon;
import entity.semantic.SemanticTrainMultiSeed;
import event.argument.JointArgumentSeed;
import event.trigger.JointTriggerIndentSeed;

public class SeedUtil {
	
	public static String getContent() {
		String content = Common.getLine("sents");
		return content;
	}
	
	public static ArrayList<ParseResult> getParseResults() {
		StanfordResult sr = StanfordXMLReader.read("sents.xml");
		ArrayList<ParseResult> parseResults = ACECommon
				.standford2ParseResult(sr, SeedUtil.getContent());
		return parseResults;
	}
	
	public static HashMap<String, HashSet<String>> loadRoleToSemConstraints() {
		
		ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
		
		String content = getContent();
		ArrayList<String> lines = Common.getLines("seeds.txt");
		EventType eventType = null;
		Sent sent = null;
		Argument arg = null;
		EventMention event = null;
		ArrayList<EventType> eventTypes = new ArrayList<EventType>();
		int sid = 0;
		
		ArrayList<String> sents = new ArrayList<String>();
		
		int pointer = 0;
		
		for(int i=0;i<lines.size();i++) {
			String line = lines.get(i).trim();
			if(line.isEmpty()) {
				continue;
			}
			if(line.startsWith("T")) {
				eventType = new EventType(line.substring(2));
				eventTypes.add(eventType);
				if(!lines.get(i+1).startsWith("S")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
				if(!Util.subTypes.contains(line.substring(2))) {
					Common.bangErrorPOS(line);
				}
			}
			if(line.startsWith("S")) {
				sent = new Sent(line.substring(2));
				eventType.sents.add(sent);
				sid += 1;

				if(sents.size()!=0) {
					pointer += sents.get(sents.size()-1).length() + 1;
				}
				
				sents.add(line.substring(2).trim());
				
				if(!lines.get(i+1).startsWith("E")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
			}
			if(line.startsWith("E")) {
				sent.trigger = line.substring(2);
				
				if(!sent.s.contains(sent.trigger)) {
					System.out.println(sent.trigger);
					Common.bangErrorPOS(lines.get(i-1));
				}
				
				if(!lines.get(i+1).startsWith("A")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
				
				event = new EventMention();
				event.setAnchor(sent.trigger);
				event.subType = eventType.subtype;
				
				event.setAnchorStart(sent.s.indexOf(sent.trigger) + pointer);
				event.setAnchorEnd(event.getAnchorStart() + sent.trigger.length() -1);
				
				if(!sent.trigger.equals( content.substring(event.getAnchorStart(), event.getAnchorEnd()+1))) {
					Common.bangErrorPOS("");
				}
				
				eventMentions.add(event);
			}
			if(line.startsWith("A")) {
				String tks[] = line.substring(2).split(":");
				String role = tks[0];
				String entityType = tks[1];
				
				HashSet<String> entityTypes = new HashSet<String>(Arrays.asList(entityType.split("\\s+")));
				
				for(String e : entityType.split("\\s+")) {
					if(!Util.semClasses.contains(e.toLowerCase())
							&& !e.equals("TIME")
							&& !e.equals("SUB")
							&& !e.equalsIgnoreCase("MONEY")
							&& !e.equalsIgnoreCase("JOB")
							&& !e.equalsIgnoreCase("CRIME")
							&& !e.equalsIgnoreCase("SEN")
							&& !e.equalsIgnoreCase("NUM")
							) {
						Common.bangErrorPOS(line);
					}
				}
				if(eventType.roleToEntityTypeConstraints.containsKey(role)) {
					HashSet<String> tmp = eventType.roleToEntityTypeConstraints.get(role);
					if(entityTypes.size()!=tmp.size()) {
						System.out.println(line);
						System.out.println(sent.s);
						System.out.println(entityTypes);
						System.out.println(tmp);
//						Common.bangErrorPOS("");
					} else {
						tmp.removeAll(entityTypes);
						if(tmp.size()!=0) {
							System.out.println(line);
							System.out.println(sent.s);
							System.out.println(entityTypes);
							System.out.println(tmp);
//							Common.bangErrorPOS("");
						}
					}
				}
				
				
				if(role.equals("Duration")) {
					role = "Time";
				}
				if(!JointArgumentSeed.roles.contains(role)) {
					Common.bangErrorPOS(line);
				}
				eventType.roleToEntityTypeConstraints.put(role, entityTypes);
				
				if(tks.length==2) {
					continue;
				}
				
				arg = new Argument(role);

				if(!Util.roles.contains(arg.role) && !arg.role.equals("Time") && !arg.role.equals("Duration")) {
					System.out.println(arg.role);
					Common.bangErrorPOS(line);
				}
				sent.args.add(arg);
				
				if(tks.length>=3) {
					arg.extent = tks[2];
					if(!sent.s.contains(arg.extent)) {
						Common.bangErrorPOS(line);
					}
				}
				if(tks.length>=4) {
					arg.head = tks[3];
					if(!sent.s.contains(arg.extent) || !arg.extent.contains(arg.head)) {
						Common.bangErrorPOS(line);
					}
				}
				
				EventMentionArgument eventArg = new EventMentionArgument();
				event.addArgument(eventArg);
				eventArg.setRole(role);
				
				int start = sent.s.indexOf(arg.extent) + pointer;
				int end = start + arg.extent.length() - 1;
				
				eventArg.setStart(start);
				eventArg.setEnd(end);
				
				eventArg.setExtent(arg.extent);
//				System.out.println(arg.extent + "#" + content.substring(start, end + 1));
				
//				eventArg.setStart(start)
			}
			if(line.startsWith("N")) {
				String tks[] = line.split(":");
				
				String head = tks[1];
				String str = sent.s;
				int pp = 0;
				while(str.indexOf(head, pp)!=-1) {
					int start = str.indexOf(head, pp);
					int end = start + head.length() - 1;
					
					EntityMention m = new EntityMention();
					m.head = tks[1];
					m.semClass = tks[2];
					m.headStart = start + pointer;
					m.headEnd = end + pointer;
					
					
					pp = end + 1;
					
					if(!m.head.equals(content.substring(m.headStart, m.headEnd + 1) )) {
						Common.bangErrorPOS("");
					}
				}
			}
		}
		HashMap<String, HashSet<String>> constraints = new HashMap<String, HashSet<String>>();
		
		for(EventType et : eventTypes) {
			HashMap<String, HashSet<String>> cons = et.roleToEntityTypeConstraints;
			for(String key : cons.keySet()) {
				
				if(!JointArgumentSeed.roles.contains(key)) {
					System.out.println(key);
					Common.bangErrorPOS("");
				}
				
				if(!Util.subTypes.contains(et.subtype)) {
					System.out.println(key);
					Common.bangErrorPOS("");
				}
				
//				System.out.println(key + "#" + cons.get(key));
				if(constraints.containsKey(key)) {
					HashSet<String> entityTypes = cons.get(key);
						HashSet<String> tmp = constraints.get(key);
						if(entityTypes.size()!=tmp.size()) {
//							System.out.println(line);
//							System.out.println(sent.s);
							System.out.println(key);
							System.out.println(entityTypes);
							System.out.println(tmp);
//							Common.bangErrorPOS("");
						} else {
							tmp.removeAll(entityTypes);
							if(tmp.size()!=0) {
//								System.out.println(line);
//								System.out.println(sent.s);
								System.out.println(key);
								System.out.println(entityTypes);
								System.out.println(tmp);
//								Common.bangErrorPOS("");
							}
						}
				}
				HashSet<String> entityTypes = cons.get(key);
				HashSet<String> newEntityTypes = new HashSet<String>();
				
				for(String type : entityTypes) {
					if(!SemanticTrainMultiSeed.semClasses.contains(type.toLowerCase())) {
						newEntityTypes.add("val");
					} else {
						newEntityTypes.add(type.toLowerCase());
					}
				}
				
				constraints.put(et.subtype + " " + key, newEntityTypes);
			}
		}
//		for(String key : constraints.keySet()) {
//			System.out.println(key + " " + constraints.get(key));
//		}
		return constraints;
	}
	
	public static void main(String args[]) {
		HashMap<String, HashSet<String>> constraints = loadRoleToSemConstraints();
	}
	
	public static ACEChiDoc getSeedDoc() {
		ACEChiDoc doc = new ACEChiDoc();
		doc.content = getContent();
		doc.parseReults = getParseResults();
		doc.goldEventMentions = getGoldEventMentions();
		ArrayList<EntityMention> entityMentions = getGoldEntityMentions();
		
		doc.goldEntityMentions = new ArrayList<EntityMention>();
		doc.goldTimeMentions = new ArrayList<EntityMention>();
		doc.goldValueMentions = new ArrayList<EntityMention>();
		doc.allGoldNPEndMap = new HashMap<Integer, EntityMention>();
		
		doc.positionMap = new HashMap<Integer, int[]>();
		for(int i=0;i<doc.parseReults.size();i++) {
			ParseResult pr = doc.parseReults.get(i);
			for(int j=0;j<pr.positions.size();j++) {
				int[] p = new int[2];
				p[0] = i;
				p[1] = j;
				
				for(int z=pr.positions.get(j)[0];z<=pr.positions.get(j)[1];z++) {
					doc.positionMap.put(z, p);
				}
			}
		}
		
		for(EntityMention m : entityMentions) {
			if(m.semClass.equalsIgnoreCase("time")) {
				m.type = "Time";
				doc.goldTimeMentions.add(m);
			} else if(Util.semClasses.contains(m.semClass.toLowerCase())) {
				Util.setMentionType(m, doc);
				doc.goldEntityMentions.add(m);
			} else {
				m.semClass = "value";
				m.type = "Value";
				doc.goldValueMentions.add(m);
			}
			doc.allGoldNPEndMap.put(m.end, m);
			
			
		}
		
		doc.semanticRoles = new HashMap<EventMention, SemanticRole>();
		return doc;
	}
	
	public static ArrayList<EventMention> getGoldEventMentions() {
		ArrayList<EventMention> eventMentions = new ArrayList<EventMention>();
		
		String content = getContent();
		ArrayList<String> lines = Common.getLines("seeds.txt");
		EventType eventType = null;
		Sent sent = null;
		Argument arg = null;
		EventMention event = null;
		ArrayList<EventType> eventTypes = new ArrayList<EventType>();
		int sid = 0;
		
		ArrayList<String> sents = new ArrayList<String>();
		
		int pointer = 0;
		
		for(int i=0;i<lines.size();i++) {
			String line = lines.get(i).trim();
			if(line.isEmpty()) {
				continue;
			}
			if(line.startsWith("T")) {
				eventType = new EventType(line.substring(2));
				eventTypes.add(eventType);
				if(!lines.get(i+1).startsWith("S")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
				if(!Util.subTypes.contains(line.substring(2))) {
					Common.bangErrorPOS(line);
				}
			}
			if(line.startsWith("S")) {
				sent = new Sent(line.substring(2));
				eventType.sents.add(sent);
				sid += 1;

				if(sents.size()!=0) {
					pointer += sents.get(sents.size()-1).length() + 1;
				}
				
				sents.add(line.substring(2).trim());
				
				if(!lines.get(i+1).startsWith("E")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
			}
			if(line.startsWith("E")) {
				sent.trigger = line.substring(2);
				
				if(!sent.s.contains(sent.trigger)) {
					System.out.println(sent.trigger);
					Common.bangErrorPOS(lines.get(i-1));
				}
				
				if(!lines.get(i+1).startsWith("A")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
				
				event = new EventMention();
				event.setAnchor(sent.trigger);
				event.subType = eventType.subtype;
				
				event.setAnchorStart(sent.s.indexOf(sent.trigger) + pointer);
				event.setAnchorEnd(event.getAnchorStart() + sent.trigger.length() -1);
				
				if(!sent.trigger.equals( content.substring(event.getAnchorStart(), event.getAnchorEnd()+1))) {
					Common.bangErrorPOS("");
				}
				
				eventMentions.add(event);
			}
			if(line.startsWith("A")) {
				String tks[] = line.substring(2).split(":");
				String role = tks[0];
				String entityType = tks[1];
				
				HashSet<String> entityTypes = new HashSet<String>(Arrays.asList(entityType.split("\\s+")));
				
				for(String e : entityType.split("\\s+")) {
					if(!Util.semClasses.contains(e.toLowerCase())
							&& !e.equals("TIME")
							&& !e.equals("SUB")
							&& !e.equalsIgnoreCase("MONEY")
							&& !e.equalsIgnoreCase("JOB")
							&& !e.equalsIgnoreCase("CRIME")
							&& !e.equalsIgnoreCase("SEN")
							&& !e.equalsIgnoreCase("NUM")
							) {
						Common.bangErrorPOS(line);
					}
				}
				
				eventType.roleToEntityTypeConstraints.put(role, entityTypes);
				
				
				if(tks.length==2) {
					continue;
				}
				if(role.equals("Duration")) {
					role = "Time";
				}
				
				arg = new Argument(role);

				if(!Util.roles.contains(arg.role) && !arg.role.equals("Time") && !arg.role.equals("Duration")) {
					System.out.println(arg.role);
					Common.bangErrorPOS(line);
				}
				sent.args.add(arg);
				
				if(tks.length>=3) {
					arg.extent = tks[2];
					if(!sent.s.contains(arg.extent)) {
						Common.bangErrorPOS(line);
					}
				}
				if(tks.length>=4) {
					arg.head = tks[3];
					if(!sent.s.contains(arg.extent) || !arg.extent.contains(arg.head)) {
						Common.bangErrorPOS(line);
					}
				}
				
				EventMentionArgument eventArg = new EventMentionArgument();
				event.addArgument(eventArg);
				eventArg.setRole(role);
				
				int start = sent.s.indexOf(arg.extent) + pointer;
				int end = start + arg.extent.length() - 1;
				
				eventArg.setStart(start);
				eventArg.setEnd(end);
				
				eventArg.setExtent(arg.extent);
//				System.out.println(arg.extent + "#" + content.substring(start, end + 1));
				
//				eventArg.setStart(start)
			}
			if(line.startsWith("N")) {
				String tks[] = line.split(":");
				
				String head = tks[1];
				String str = sent.s;
				int pp = 0;
				while(str.indexOf(head, pp)!=-1) {
					int start = str.indexOf(head, pp);
					int end = start + head.length() - 1;
					
					EntityMention m = new EntityMention();
					m.head = tks[1];
					m.semClass = tks[2];
					m.headStart = start + pointer;
					m.headEnd = end + pointer;
					
					
					pp = end + 1;
					
					if(!m.head.equals(content.substring(m.headStart, m.headEnd + 1) )) {
						Common.bangErrorPOS("");
					}
				}
			}
		}
		
		return eventMentions;
	}
	
	public static ArrayList<EntityMention> getGoldEntityMentions() {
		String content = getContent();
		ArrayList<String> lines = Common.getLines("seeds.txt");
		EventType eventType = null;
		Sent sent = null;
		Argument arg = null;
		ArrayList<EventType> eventTypes = new ArrayList<EventType>();
		int sid = 0;
		
		ArrayList<String> sents = new ArrayList<String>();
		
		ArrayList<EntityMention> mentions = new ArrayList<EntityMention>();
		
		int pointer = 0;
		
		for(int i=0;i<lines.size();i++) {
			String line = lines.get(i).trim();
			if(line.isEmpty()) {
				continue;
			}
			if(line.startsWith("T")) {
				eventType = new EventType(line.substring(2));
				eventTypes.add(eventType);
				if(!lines.get(i+1).startsWith("S")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
				if(!Util.subTypes.contains(line.substring(2))) {
					Common.bangErrorPOS(line);
				}
			}
			if(line.startsWith("S")) {
				sent = new Sent(line.substring(2));
				eventType.sents.add(sent);
				sid += 1;

				if(sents.size()!=0) {
					pointer += sents.get(sents.size()-1).length() + 1;
				}
				
				sents.add(line.substring(2).trim());
				
				if(!lines.get(i+1).startsWith("E")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
			}
			if(line.startsWith("E")) {
				sent.trigger = line.substring(2);
				
				if(!sent.s.contains(sent.trigger)) {
					System.out.println(sent.trigger);
					Common.bangErrorPOS(lines.get(i-1));
				}
				
				if(!lines.get(i+1).startsWith("A")) {
					System.out.println(line);
					Common.bangErrorPOS(lines.get(i+1));
				}
			}
			if(line.startsWith("A")) {
				String tks[] = line.substring(2).split(":");
				String role = tks[0];
				String entityType = tks[1];
				
				HashSet<String> entityTypes = new HashSet<String>(Arrays.asList(entityType.split("\\s+")));
				
				for(String e : entityType.split("\\s+")) {
					if(!Util.semClasses.contains(e.toLowerCase())
							&& !e.equals("TIME")
							&& !e.equals("SUB")
							&& !e.equalsIgnoreCase("MONEY")
							&& !e.equalsIgnoreCase("JOB")
							&& !e.equalsIgnoreCase("CRIME")
							&& !e.equalsIgnoreCase("SEN")
							&& !e.equalsIgnoreCase("NUM")
							) {
						Common.bangErrorPOS(line);
					}
				}
				
				eventType.roleToEntityTypeConstraints.put(role, entityTypes);
				
				if(tks.length==2) {
					continue;
				}
				arg = new Argument(role);

				if(!Util.roles.contains(arg.role) && !arg.role.equals("Time") && !arg.role.equals("Duration")) {
					System.out.println(arg.role);
					Common.bangErrorPOS(line);
				}
				sent.args.add(arg);
				
				if(tks.length>=3) {
					arg.extent = tks[2];
					if(!sent.s.contains(arg.extent)) {
						Common.bangErrorPOS(line);
					}
				}
				if(tks.length>=4) {
					arg.head = tks[3];
					if(!sent.s.contains(arg.extent) || !arg.extent.contains(arg.head)) {
						Common.bangErrorPOS(line);
					}
				}
			}
			if(line.startsWith("N")) {
				String tks[] = line.split(":");
				
				String head = tks[1];
				String str = sent.s;
				int pp = 0;
				while(str.indexOf(head, pp)!=-1) {
					int start = str.indexOf(head, pp);
					int end = start + head.length() - 1;
					
					EntityMention m = new EntityMention();
					m.head = tks[1];
					m.semClass = tks[2];
					m.headStart = start + pointer;
					m.headEnd = end + pointer;
					m.start = m.headStart;
					m.end = m.headEnd;
					m.extent = head;
					mentions.add(m);
					
					pp = end + 1;
					
					if(!m.head.equals(content.substring(m.headStart, m.headEnd + 1) )) {
						Common.bangErrorPOS("");
					}
				}
			}
		}
		return mentions;
	}
}
