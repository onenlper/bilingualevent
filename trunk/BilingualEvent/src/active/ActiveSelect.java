package active;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EntityMention;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;
import entity.semantic.SemanticTrainMultiSeed;
import event.argument.JointArgumentSeed;

public class ActiveSelect {

	
	public static HashMap<String, ArrayList<EntityMention>> loadSVMResult(String part) {
		HashMap<String, ArrayList<EntityMention>> entityMentionses = new HashMap<String, ArrayList<EntityMention>>();
		
		String folder = "./";
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test" + part);
		System.out.println(mentionStrs.size());
		ArrayList<String> typeResult = Common.getLines(folder + "multiType.result" + part);
		
		HashMap<String, EntityMention> mMap = new HashMap<String, EntityMention>();
		
		for(int i=0;i<mentionStrs.size();i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headStart = headStart;
			em.headEnd = headEnd;
			em.start = headStart;
			em.end = headEnd;
			
			mMap.put(em.toName(), em);
			
			String tks[] = typeResult.get(i).split("\\s+");
			int typeIndex = Integer.valueOf(tks[0]);
			
			for(int k=1;k<tks.length;k++) {
				em.semClassConf.add(Double.parseDouble(tks[k]));
			}
			
			String type = SemanticTrainMultiSeed.semClasses.get(typeIndex - 1);
			if(type.equalsIgnoreCase("none")) {
				continue;
			}
			
			em.semClass = type;
			
			ArrayList<EntityMention> mentions = entityMentionses.get(fileKey);
			if(mentions==null) {
				mentions = new ArrayList<EntityMention>();
				entityMentionses.put(fileKey, mentions);
			}
			if(type.equalsIgnoreCase("val")) {
				em.type = "Value";
			} else if(type.equalsIgnoreCase("time")) {
				em.type = "Time";
			} else {
			}
			mentions.add(em);
		}
		return entityMentionses;
	}
	
	public static double getActiveScore(EventMention event) {
		double scores = 0;
		double divide = 1;
		scores += event.subTypeConfidences.get(Util.subTypes.indexOf(event.subType));

		if(event.subType.equals("null")) {
//			scores *= 2;
//			return -1;
//			scores *= 3000;
		}
		
		for(EventMentionArgument arg : event.getEventMentionArguments()) {
			
			if(!event.subType.equals("null")) {
				scores += arg.roleConfidences.get(JointArgumentSeed.roles.indexOf(arg.role));
				divide += 1;
			} else {
			}
			
			EntityMention mention = arg.mention;
			scores += mention.semClassConf.get(SemanticTrainMultiSeed.semClasses.indexOf(mention.semClass));
			divide += 1;
		}
		return scores/divide;
	}
	
	public static class Entry implements Comparable<Entry>{
		double score;
		ACEDoc doc;
//		ParseResult pr;
		
		int idx;
		
		public Entry(ACEDoc doc, int idx, double score) {
			this.doc = doc;
			this.idx = idx;
			this.score = score;
		}
		
		@Override
		public int hashCode() {
			StringBuilder sb = new StringBuilder();
			sb.append(this.doc.fileID + " " + this.idx);
			return sb.toString().hashCode();
		}
		
		@Override
		public boolean equals(Object arg0) {
			Entry e2 = (Entry) arg0;
			if(this.doc==e2.doc && this.idx==e2.idx) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public int compareTo(Entry e2) {
			if(this.score>e2.score) {
				return 1;
			} else if(this.score==e2.score) {
				return 0;
			} else {
				return -1;
			}
		}
	}
	
	public static HashMap<String, HashSet<Integer>> loadSelectedEvents(String file) {
		HashMap<String, HashSet<Integer>> selected = new HashMap<String, HashSet<Integer>>();
		ArrayList<String> lines = Common.getLines(file);
		for(String line : lines) {
			String tks[] = line.split("\\s+");
			String fileID = tks[0];
			HashSet<Integer> idxes = selected.get(fileID);
			if(idxes==null) {
				idxes = new HashSet<Integer>();
				selected.put(fileID, idxes);
			}
			for(int i=1;i<tks.length;i++) {
				if(tks[i].equalsIgnoreCase("all")) {
					idxes.add(-1);
				} else {
					idxes.add(Integer.parseInt(tks[i]));
				}
			}
		}
		return selected;
	}
	
	public static void main(String args[]) {
		Util.part = "6";
		Util.seed = true;
		int top = 500;
		
		HashMap<String, HashSet<Integer>> lastSelecteds = loadSelectedEvents("ACE_Chinese_train6");
		
		
		HashMap<String, HashSet<String>> knownTrigger = Common.readFile2Map6("chinese_trigger_known" + Util.part);
		HashSet<String> train_words = Common.readFile2Set("train_words" + Util.part);
		
		HashMap<String, ArrayList<EntityMention>> entityMentionses = loadSVMResult("6");
		ArrayList<String> files = Common.getLines("ACE_Chinese_test6");
		int amount = 0;
		HashSet<Entry> entries = new HashSet<Entry>();
		for (int k = 0; k < files.size(); k++) {
//			if(k%10==0)
//				System.err.println(k + "/" + files.size());
			String file = files.get(k);
			ACEDoc doc = new ACEChiDoc(file);

			doc.docID = k;
			
			ArrayList<EntityMention> allNouns = new ArrayList<EntityMention>(
					entityMentionses.get(file));
			
			ArrayList<EventMention> events = Util.loadSystemComponentsSeed(doc, allNouns);
			
			amount += events.size();
			
			HashSet<Integer> lastSelected = new HashSet<Integer>();
			if(lastSelecteds.containsKey(file)) {
				lastSelected = lastSelecteds.get(file);
			}
			if(lastSelected.contains(-1)) {
				continue;
			}
			
			for(EventMention event : events) {
				if(lastSelected.contains(event.getAnchorEnd()) || lastSelected.contains(-1)) {
					continue;
				}
				
				if(train_words.contains(event.getAnchor()) && !knownTrigger.containsKey(event.getAnchor())) {
					continue;
				}
				
				double score = getActiveScore(event);
				Entry entry = new Entry(doc, event.getAnchorEnd(), score);
				entries.add(entry);
			}
		}
		ArrayList<Entry> sortedEntry = new ArrayList<Entry>(entries);
		Collections.sort(sortedEntry);
//		Collections.reverse(sortedEntry);
		
		HashMap<String, HashSet<Integer>> selectedEvents = new HashMap<String, HashSet<Integer>>();
		int selectedAmount = 0;
		for(int i=0;i<top && i<sortedEntry.size();i++) {
			Entry entry = sortedEntry.get(i);
			String fileID = entry.doc.fileID;
			HashSet<Integer> sents = selectedEvents.get(fileID);
			if(sents == null) {
				sents = new HashSet<Integer>();
				selectedEvents.put(fileID, sents);
			}
			sents.add(entry.idx);
			selectedAmount += 1;
		}
		

		ArrayList<String> selectedLines = new ArrayList<String>();
		for(String key : files) {
			HashSet<Integer> idxes = new HashSet<Integer>();
			if(selectedEvents.containsKey(key)) {
				idxes.addAll(selectedEvents.get(key));
			}
			
			if(lastSelecteds.containsKey(key)) {
				idxes.addAll(lastSelecteds.get(key));
			}
			
			StringBuilder sb = new StringBuilder();
			sb.append(key).append(" ");
			for(Integer idx : idxes) {
				if(idx==-1) {
					sb = new StringBuilder();
					sb.append(key).append(" all");
				} else {
					sb.append(idx).append(" ");
				}
			}
//			System.out.println(sb.toString().trim());
			selectedLines.add(sb.toString().trim());
		}
		
		System.out.println(selectedAmount + " new annotated events");
		Common.outputLines(selectedLines, "ACE_Chinese_train6");
	}

	private static void selectDoc() {
//		double allScore = 0;
//		for(EventMention event : events) {
//			if(lastSelected.contains(event.getAnchorEnd())) {
//				continue;
//			}
//			
//			if(train_words.contains(event.getAnchor()) && !knownTrigger.containsKey(event.getAnchor())) {
////				continue;
//			}
//			
//			double score = getActiveScore(event);
//			allScore += score;
//		}
//		Entry entry = new Entry(doc, -1, allScore/events.size());
//		entries.add(entry);
	}
	
//	private static void selectEvent(
//			HashMap<String, HashSet<String>> knownTrigger,
//			HashSet<String> train_words, HashSet<Entry> entries, ACEDoc doc,
//			ArrayList<EventMention> events, HashSet<Integer> lastSelected) {
//		for(EventMention event : events) {
//			if(lastSelected.contains(event.getAnchorEnd()) || lastSelected.contains(-1)) {
//				continue;
//			}
//			
//			if(train_words.contains(event.getAnchor()) && !knownTrigger.containsKey(event.getAnchor())) {
//				continue;
//			}
//			
//			double score = getActiveScore(event);
//			Entry entry = new Entry(doc, event.getAnchorEnd(), score);
//			entries.add(entry);
//		}
//	}
}
