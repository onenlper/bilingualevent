package active;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventMention;
import model.ParseResult;
import util.Common;

public class InitialSeed {

	public static class Entry implements Comparable<Entry> {

		int count = 0;
		String str;

		int eventCount = 0;

		public Entry(String str) {
			this.str = str;
		}

		public void increase() {
			this.count += 1;
		}

		public void increaseEvent() {
			this.eventCount += 1;
		}

		@Override
		public int compareTo(Entry arg0) {
			// TODO Auto-generated method stub
			return this.count - arg0.count;
		}

	}

	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("ACE_Chinese_train0");
		lines.addAll(Common.getLines("ACE_Chinese_test0"));
		HashMap<String, Entry> entryMap = new HashMap<String, Entry>();

		double eventAmount = 0;
		int entityAmount = 0;
		int chainAmount = 0;
		for (String file : lines) {
			ACEDoc doc = new ACEChiDoc(file);

			entityAmount += doc.goldEntityMentions.size();
			chainAmount += doc.goldEntities.size();
			
			HashMap<Integer, EventMention> eventMap = new HashMap<Integer, EventMention>();
			for (EventMention e : doc.goldEventMentions) {
				eventMap.put(e.getAnchorEnd(), e);
			}
			eventAmount += doc.goldEventMentions.size();
			
			for (ParseResult pr : doc.parseReults) {
				for (int i = 0; i < pr.words.size(); i++) {
					String word = pr.words.get(i);
					String POS = pr.posTags.get(i);
					int end = pr.positions.get(i)[1];
					if (!POS.equals("NN") && !POS.equals("P")
							&& !POS.equals("VV")) {
						continue;
					}
					Entry entry = entryMap.get(word);
					if (entry == null) {
						entry = new Entry(word);
						entryMap.put(word, entry);
					}
					entry.increase();
					if (eventMap.containsKey(end)) {
						entry.increaseEvent();
					}
				}
			}
		}
		ArrayList<Entry> entries = new ArrayList<Entry>();
		entries.addAll(entryMap.values());
		Collections.sort(entries);
		Collections.reverse(entries);

		double tmp = 0;
		for (int i = 0; i < entries.size(); i++) {
			Entry e = entries.get(i);
			tmp += e.eventCount;
			System.out.println(e.count + "\t" + e.eventCount + "\t" + (tmp/eventAmount) 
					+ "\t" + i + "\t" + e.str);
		}
		
		System.out.println(entityAmount);
		System.out.println(chainAmount);
	}

}
