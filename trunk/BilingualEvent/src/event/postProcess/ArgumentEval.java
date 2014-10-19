package event.postProcess;

import java.util.ArrayList;
import java.util.HashMap;

import model.ACEChiDoc;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;

public class ArgumentEval {
	
	static HashMap<String, ArrayList<EventMentionArgument>> systemArgumentses;
	
	public static void loadSVMSystem() {
		systemArgumentses = new HashMap<String, ArrayList<EventMentionArgument>>();
		ArrayList<String> lines = Common.getLines("data/Chinese_argumentIndent_" + mode + "_system_svm"  + Util.part);
		ArrayList<String> predicts = Common.getLines("/users/yzcchen/tool/svm_multiclass/" + mode + "_argumentIndent" + Util.part);
		ArrayList<String> roles = Common.getLines("/users/yzcchen/tool/svm_multiclass/" + mode + "_argumentRole" + Util.part);
		for(int i=0;i<lines.size();i++) {
			String line = lines.get(i);
			String roleLine = roles.get(i);
			
			String tokens[] = roleLine.split("\\s+");
			
			String role = Util.roles.get(Integer.valueOf(tokens[0])-1);
			
			String predict= "";
			String predictLine = predicts.get(i);
			tokens = predictLine.split("\\s+");
			predict = tokens[0];
			
			if(predict.equalsIgnoreCase("1")) {
				tokens = line.split("\\s+");
				String fileID = tokens[0];
				int start = Integer.valueOf(tokens[1]);
				int end = Integer.valueOf(tokens[2]);
				double confidence = Double.valueOf(tokens[3]);
				if(confidence<0) {
					continue;
				}
				String type = tokens[4];
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);
				em.setType(type);
				
				int argStart = Integer.valueOf(tokens[tokens.length-2]);
				int argEnd = Integer.valueOf(tokens[tokens.length-1]);
				
				EventMentionArgument argument = new EventMentionArgument();
				argument.setStart(argStart);
				argument.setEnd(argEnd);
				argument.setRole(role);
				argument.setEventMention(em);
				
				if(systemArgumentses.containsKey(fileID)) {
					systemArgumentses.get(fileID).add(argument);
				} else {
					ArrayList<EventMentionArgument> arguments = new ArrayList<EventMentionArgument>();
					arguments.add(argument);
					systemArgumentses.put(fileID, arguments);
				}
			}
		}
	}

	public static void loadMaxEntSystem() {
		systemArgumentses = new HashMap<String, ArrayList<EventMentionArgument>>();
		ArrayList<String> lines = Common.getLines("data/Chinese_argumentIndent_" + mode + "_system_maxent" + Util.part);
		ArrayList<String> predicts = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode + "_argumentIndent" + Util.part);
		ArrayList<String> roles = Common.getLines("/users/yzcchen/tool/maxent/bin/" + mode + "_argumentRole" + Util.part);
		for(int i=0;i<lines.size();i++) {
			String line = lines.get(i);
			String roleLine = roles.get(i);
			
			String tokens[] = roleLine.split("\\s+");
			
			String role = "";
			double maxValue = 0;
			for(int k=0;k<tokens.length/2;k++) {
				String pre = tokens[k*2];
				double val = Double.valueOf(tokens[k*2+1]);
				if(val>maxValue) {
					role = pre;
					maxValue = val;
				}
			}
			role = Util.roles.get(Integer.valueOf(role)-1);
			
			maxValue = -1;
			String predict= "";
			String predictLine = predicts.get(i);
			tokens = predictLine.split("\\s+");
			for(int k=0;k<tokens.length/2;k++) {
				String pre = tokens[k*2];
				double val = Double.valueOf(tokens[k*2+1]);
				if(val>maxValue) {
					predict = pre;
					maxValue = val;
				}
			}
			if(predict.equalsIgnoreCase("1")) {
				tokens = line.split("\\s+");
				String fileID = tokens[0];
				int start = Integer.valueOf(tokens[1]);
				int end = Integer.valueOf(tokens[2]);
				double confidence = Double.valueOf(tokens[3]);
				if(confidence<0.5) {
					continue;
				}
				String type = tokens[4];
				EventMention em = new EventMention();
				em.setAnchorStart(start);
				em.setAnchorEnd(end);
				em.setType(type);
				
				int argStart = Integer.valueOf(tokens[tokens.length-2]);
				int argEnd = Integer.valueOf(tokens[tokens.length-1]);
				
				EventMentionArgument argument = new EventMentionArgument();
				argument.setStart(argStart);
				argument.setEnd(argEnd);
				argument.setRole(role);
				argument.setEventMention(em);
				
				if(systemArgumentses.containsKey(fileID)) {
					systemArgumentses.get(fileID).add(argument);
				} else {
					ArrayList<EventMentionArgument> arguments = new ArrayList<EventMentionArgument>();
					arguments.add(argument);
					systemArgumentses.put(fileID, arguments);
				}
			}
		}
	}
	
	static String mode;
	
	public static void main(String args[]) {
		if(args.length!=3) {
			System.out.println("java ~ [test|development] [svm|maxent] [folder]");
			System.exit(1);
		}
		Util.part = args[2];
		mode = args[0];
		if(args[1].equals("maxent")) {
			loadMaxEntSystem();
		} else {
			loadSVMSystem();
		}
		ArrayList<String> files = Common.getLines("ACE_Chinese_" + mode + Util.part);
		double gold = 0;
		double system = 0;
		double hit = 0;
		double hitRole = 0;
		for(String file : files) {
			ArrayList<EventMentionArgument> golds = (new ACEChiDoc(file)).goldArguments;
			ArrayList<EventMentionArgument> systems = systemArgumentses.get(file);
			if(golds!=null) {
				gold += golds.size();
			}
			if(systems!=null) {
				system += systems.size();
				if(golds!=null) {
					for(EventMentionArgument g : golds) {
						for(EventMentionArgument s : systems) {
							if(g.equals(s)
									&& g.getEventMention().getType().equals(s.getEventMention().getType())) {
								hit++;
								if(g.getRole().equals(s.getRole())) {
									hitRole++;
								}
							}
						}
					}
				}
			}
		}
		System.out.println("====Argument Identify====");
		double p = hit/system;
		double r = hit/gold;
		double f = 2*p*r/(p+r);
		System.out.println("Gold: \t" + gold);
		System.out.println("System:\t" + system);
		System.out.println("Hit:\t" + hit);
		System.out.println(mode);
		System.out.println("R: " + r*100);
		System.out.println("P: " + p*100);
		System.out.println("F: " + f*100);
		
		System.out.println("====Argument Role====");
		double p2 = hitRole/system;
		double r2 = hitRole/gold;
		double f2 = 2*p2*r2/(p2+r2);
		System.out.println("Gold: \t" + gold);
		System.out.println("System:\t" + system);
		System.out.println("RoleHit:" + hitRole);
		System.out.println(mode);
		System.out.println("R: " + r2*100);
		System.out.println("P: " + p2*100);
		System.out.println("F: " + f2*100);
	}
}
