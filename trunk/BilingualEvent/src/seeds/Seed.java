package seeds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import model.EntityMention;
import util.Common;
import util.Util;

public class Seed {

	public static class EventType{
		
		String subtype;
		
		ArrayList<String> roles;
		
		HashMap<String, HashSet<String>> roleToEntityTypeConstraints;
		
		ArrayList<Sent> sents;
		
		public EventType(String type) {
			this.subtype = type;
			this.sents = new ArrayList<Sent>();
			this.roleToEntityTypeConstraints = new HashMap<String, HashSet<String>>();
		}
	}
	
	public static class Sent {
		String s;
		
		String trigger;
		
		ArrayList<Argument> args;
		
		public Sent(String str) {
			this.s = str;
			this.args = new ArrayList<Argument>();
		}
	}
	
	public static class Argument {
		String extent;
		String role;
		String head;
		
		public Argument(String role, String extent, String head) {
			this.role = role;
			this.extent = extent;
			this.head = head;
		}
		
		public Argument(String role) {
			this.role = role;
		}
		
		public Argument(String role, String extent) {
			this.role = role;
			this.extent = extent;
		}
	}
	
	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("seeds.txt");
		EventType eventType = null;
		Sent sent = null;
		Argument arg = null;
		ArrayList<EventType> eventTypes = new ArrayList<EventType>();
		int sid = 0;
		
		ArrayList<String> sents = new ArrayList<String>();
		
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
				EntityMention m = new EntityMention();
				m.head = tks[1];
				m.semClass = tks[2];
				
				if(!Util.semClasses.contains(m.semClass.toLowerCase()) && !m.semClass.equals("TIME")
						&& !m.semClass.equals("MONEY") && !m.semClass.equals("CRIME")
						) {
					Common.bangErrorPOS(line);
				}
			}
		}
		System.out.println(eventTypes.size());
		for(EventType t : eventTypes) {
			System.out.println(t.subtype);
		}
		System.out.println(sid);
//		System.out.println(eventTypes);
		Common.outputLines(sents, "sents");
	}
	
}
