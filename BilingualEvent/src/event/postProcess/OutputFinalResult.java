package event.postProcess;

import java.util.ArrayList;
import java.util.HashMap;

import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;

public class OutputFinalResult {
	public static void main(String args[]) {
		if(args.length<3) {
			System.out.println("java ~ [pipe|joint] [svm|maxent|svm2] [folder] [separate]");
			System.exit(1);
		}
		Util.part = args[2];
		String model = args[1];
		System.out.println("Output final result:\n==========");
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[2]);
		HashMap<String, HashMap<String, EventMention>> allMentions = null;
		if(args[0].equalsIgnoreCase("pipe") && args[1].equalsIgnoreCase("maxent")) {
			allMentions = pipeMaxEntLine();
		} else if(args[0].equalsIgnoreCase("pipe") && args[1].equalsIgnoreCase("svm")) {
			allMentions = pipeSVMLine();
		} else if(args[0].equalsIgnoreCase("joint") && args[1].equalsIgnoreCase("svm") && args.length==3) {
			allMentions = jointSVMLine("");
		} else if(args[0].equalsIgnoreCase("joint") && args[1].equalsIgnoreCase("maxent")) {
			allMentions = jointMaxEntLine();
		} else if(args[0].equalsIgnoreCase("joint") && args[1].equalsIgnoreCase("svm") && args.length==4) {
			HashMap<String, HashMap<String, EventMention>> allMentions1 = jointSVMLine("far");
			System.out.println("Far performance:");
			CrossValidation.evaluate(model, allMentions1, files, "chi");
			HashMap<String, HashMap<String, EventMention>> allMentions2 = jointSVMLine("near");
			System.out.println("Near performance:");
			CrossValidation.evaluate(model, allMentions2, files, "chi");
			allMentions = allMentions1;
			for(String file : allMentions2.keySet()) {
				HashMap<String, EventMention> values2 = allMentions2.get(file);
				HashMap<String, EventMention> values1 = allMentions.get(file);
				if(values1!=null) {
					for(String key2 : values2.keySet()) {
						if(values1.containsKey(key2)) {
							EventMention eventMention1 = values1.get(key2);
							EventMention eventMention2 = values2.get(key2);
							eventMention1.eventMentionArguments.addAll(eventMention2.eventMentionArguments);
						} else {
							values1.put(key2, values2.get(key2));
						}
					}
				} else {
					allMentions.put(file, values2);
				}
			}
		}
		Util.outputResult(allMentions,  args[0] + "_" + args[1] + "/result" + Util.part);
		System.out.println("Overall performance:");
		CrossValidation.evaluate(model, allMentions, files, "chi");
	}

	public static HashMap<String, HashMap<String, EventMention>> jointMaxEntLine() {
		HashMap<String, HashMap<String, EventMention>> eventMentionses = new HashMap<String, HashMap<String, EventMention>>();
		ArrayList<String> argumentLines = Common.getLines("data/Joint_argumentLines_testmaxent"+Util.part);
		ArrayList<String> argumentRoleLines = Common.getLines("/users/yzcchen/tool/maxent/bin/coling2012/Joint_argument"+Util.part);
		ArrayList<String> relateEMLines = Common.getLines("data/Joint_argumentRelateEM_testmaxent"+Util.part);
		
		for(String line : relateEMLines) {
			String tokens[] = line.split("\\s+");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
			
			EventMention temp = new EventMention();
			temp.setAnchorStart(mentionStart);
			temp.setAnchorEnd(mentionEnd);
			temp.setType(mentionType);
			temp.setSubType(subType);
			temp.confidence = confidence;
			temp.typeConfidence = typeConfidence;
			temp.subTypeConfidence = subTypeConfidence;
			eventMentions.put(temp.toString(), temp);
		}

		for(int i=0;i<argumentLines.size();i++) {
			String argumentLine = argumentLines.get(i);
			String argumentRoleLine = argumentRoleLines.get(i);
			
			String yy[] = Util.getEMLabel(argumentRoleLine);
			
			String roleResult = Util.roles.get(Integer.valueOf(yy[0])-1);
			String roleConfidence = yy[1];
			
			if(roleResult.equalsIgnoreCase("null")) {
				continue;
			}
			
			String tokens[] = argumentLine.split("\\s");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
			
			mention.eventMentionArguments.add(argument);
		}
		return eventMentionses;
	}
	
	public static HashMap<String, HashMap<String, EventMention>> jointSVMLine(String control) {
		HashMap<String, HashMap<String, EventMention>> eventMentionses = new HashMap<String, HashMap<String, EventMention>>();
		
		ArrayList<String> argumentLines = Common.getLines("data/Joint_argumentLines_" + control + "testsvm"+Util.part);
		ArrayList<String> argumentRoleLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/Joint_argument" + control + Util.part);
		ArrayList<String> relateEMLines = Common.getLines("data/Joint_argumentRelateEM_testsvm"+Util.part);
		
		for(String line : relateEMLines) {
			String tokens[] = line.split("\\s+");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
			for(int k=9;k<tokens.length;k++) {
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

		for(int i=0;i<argumentLines.size();i++) {
			String argumentLine = argumentLines.get(i);
			String argumentRoleLine = argumentRoleLines.get(i);
			
			String tokens[] = argumentRoleLine.split("\\s+");
			
			String role = Util.roles.get(Integer.parseInt(tokens[0])-1);
			
			ArrayList<Double> roleConfidences = new ArrayList<Double>();
			
			for(int k=1;k<tokens.length;k++) {
				roleConfidences.add(Double.valueOf(tokens[k]));
			}
			
			String roleResult = Util.roles.get(Integer.parseInt(tokens[0])-1);
			String roleConfidence = tokens[Integer.parseInt(tokens[0])];
			
			tokens = argumentLine.split("\\s");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
	
	
	public static HashMap<String, HashMap<String, EventMention>> pipeSVMLine() {
		HashMap<String, HashMap<String, EventMention>> eventMentionses = new HashMap<String, HashMap<String, EventMention>>();
		
		ArrayList<String> argumentLines = Common.getLines("data/Pipe_argumentLines_testsvm"+Util.part);
		ArrayList<String> argumentRoleLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/Pipe_argumentRole"+Util.part);
		ArrayList<String> argumentIdentLines = Common.getLines("/users/yzcchen/tool/svm_multiclass/coling2012/Pipe_argumentIndent"+Util.part);
		ArrayList<String> relateEMLines = Common.getLines("data/Pipe_argumentRelateEM_testsvm"+Util.part);
		
		for(String line : relateEMLines) {
			String tokens[] = line.split("\\s+");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
			
			EventMention temp = new EventMention();
			temp.setAnchorStart(mentionStart);
			temp.setAnchorEnd(mentionEnd);
			temp.setType(mentionType);
			temp.setSubType(subType);
			temp.confidence = confidence;
			temp.typeConfidence = typeConfidence;
			temp.subTypeConfidence = subTypeConfidence;
			temp.inferFrom = tokens[8];
			eventMentions.put(temp.toString(), temp);
			
		}

		for(int i=0;i<argumentLines.size();i++) {
			String argumentLine = argumentLines.get(i);
			String argumentRoleLine = argumentRoleLines.get(i);
			String argumentIdentLine = argumentIdentLines.get(i);
			
			String tokens[] = argumentIdentLine.split("\\s+");
			
			String identResult = tokens[0];
			String identConfidence = tokens[1];
			
			tokens = argumentRoleLine.split("\\s+");
			
			String roleResult = Util.roles.get(Integer.parseInt(tokens[0])-1);
			String roleConfidence = tokens[Integer.parseInt(tokens[0])];
			
			if(!identResult.equalsIgnoreCase("1")) {
				roleResult = "null";
			}
			
			ArrayList<Double> roleConfidences = new ArrayList<Double>();
			for(int k=1;k<tokens.length;k++) {
				roleConfidences.add(Double.valueOf(tokens[k]));
			}
			
			tokens = argumentLine.split("\\s");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
			argument.confidence = Double.parseDouble(identConfidence);
			argument.roleConfidence = Double.parseDouble(roleConfidence);
			argument.roleConfidences = roleConfidences;
			mention.eventMentionArguments.add(argument);
		}
		return eventMentionses;
	}
	
	
	public static HashMap<String, HashMap<String, EventMention>> pipeMaxEntLine() {
		HashMap<String, HashMap<String, EventMention>> eventMentionses = new HashMap<String, HashMap<String, EventMention>>();
		
		ArrayList<String> argumentLines = Common.getLines("data/Pipe_argumentLines_testmaxent"+Util.part);
		ArrayList<String> argumentRoleLines = Common.getLines("/users/yzcchen/tool/maxent/bin/coling2012/Pipe_argumentRole"+Util.part);
		ArrayList<String> argumentIdentLines = Common.getLines("/users/yzcchen/tool/maxent/bin/coling2012/Pipe_argumentIndent"+Util.part);
		ArrayList<String> relateEMLines = Common.getLines("data/Pipe_argumentRelateEM_testmaxent"+Util.part);
		
		for(String line : relateEMLines) {
			String tokens[] = line.split("\\s+");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
			
			EventMention temp = new EventMention();
			temp.setAnchorStart(mentionStart);
			temp.setAnchorEnd(mentionEnd);
			temp.setType(mentionType);
			temp.setSubType(subType);
			temp.confidence = confidence;
			temp.typeConfidence = typeConfidence;
			temp.subTypeConfidence = subTypeConfidence;
			eventMentions.put(temp.toString(), temp);
			
		}

		for(int i=0;i<argumentLines.size();i++) {
			String argumentLine = argumentLines.get(i);
			String argumentRoleLine = argumentRoleLines.get(i);
			String argumentIdentLine = argumentIdentLines.get(i);
			
			String yy[] = Util.getEMLabel(argumentIdentLine);
			
			String identResult = yy[0];
			String identConfidence = yy[1];
			if(!identResult.equalsIgnoreCase("1")) {
				continue;
			}
			
			yy = Util.getEMLabel(argumentRoleLine);
			
			String roleResult = Util.roles.get(Integer.parseInt(yy[0])-1);
			String roleConfidence = yy[1];
			
			String tokens[] = argumentLine.split("\\s");
			
			String fileID = tokens[0];
			HashMap<String, EventMention> eventMentions = eventMentionses.get(fileID);
			if(eventMentions==null) {
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
			argument.confidence = Double.parseDouble(identConfidence);
			argument.roleConfidence = Double.parseDouble(roleConfidence);
			
			mention.eventMentionArguments.add(argument);
		}
		return eventMentionses;
	}
}
