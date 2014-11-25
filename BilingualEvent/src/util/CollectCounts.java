package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventMention;

public class CollectCounts {

	
	static class Pair implements Comparable<Pair>{

		int count = 0;
		
		String key;
		
		double coref = 0.0;
		double notcoref = 0.0;
		
		public int hashCode() {
			return this.key.hashCode();
		}
		
		public boolean equals(Pair p1) {
			return p1.key.equals(this.key);
		}
		
		public Pair(String key) {
			this.key = key;
		}

		public void increase() {
			this.count += 1;
		}
		
		@Override
		public int compareTo(Pair arg0) {
			// TODO Auto-generated method stub
			return this.count - arg0.count;
		}
	}
	
	public static void main(String args[]) {
		ArrayList<String> lines = Common.getLines("ACE_Chinese_train0");
		HashMap<String, Pair> pairs = new HashMap<String, Pair>();
		
		for(String line : lines) {
			ACEDoc doc = new ACEChiDoc(line);
			ArrayList<EventMention> eventMentions = doc.goldEventMentions;
			
			for(int i=0;i<eventMentions.size();i++) {
				EventMention event1 = eventMentions.get(i);
				for(int j=i+1;j<eventMentions.size();j++) {
					EventMention event2 = eventMentions.get(j);
					
					if(event1.subType.equals(event2.subType)) {
						
						String tr1 = event1.getAnchor();
						String tr2 = event2.getAnchor();
						
						if(tr1.equals(tr2)) {
							continue;
						}
						
						String key = "";
						if(tr1.compareTo(tr2)>0) {
							key = tr1 + "#" + tr2;
						} else {
							key = tr2 + "#" + tr1;
						}
						Pair pair = pairs.get(key);
						if(pair == null) {
							pair = new Pair(key);
							pairs.put(key, pair);
						}
						pair.increase();
						
						boolean coref = event1.getEventChain()==event2.getEventChain();
						if(coref) {
							pair.coref += 1;
						} else {
							pair.notcoref += 1;
						}
					}
				}
			}
		}
		ArrayList<Pair> pairList = new ArrayList<Pair>();
		pairList.addAll(pairs.values());
		Collections.sort(pairList);
		Collections.reverse(pairList);
		double allCount = 0;
		int cap = 3;
		double capCount = 0;
		int highRatio = 0;
		ArrayList<String> output = new ArrayList<String>();
		int lowRatio = 0;
		ArrayList<Pair> negativeLinks = new ArrayList<Pair>();
		
		double positiveRatioTh = 0.5;
		double negativeRatioTh = 0.0;
		int freq = 0;
		
		ArrayList<HashSet<String>> seedClusters = new ArrayList<HashSet<String>>();
		ArrayList<String> outputNegativeLinks = new ArrayList<String>();
		for(Pair p : pairList) {
			double ratio = p.coref/(p.coref + p.notcoref);
			allCount += p.count;
			if(p.count>=cap) {
				freq += 1;
				
				capCount += p.count;
				if(ratio>positiveRatioTh) {
					output.add(p.key.replace("#", " "));
					System.out.println(p.key + ":\t" + p.count + "\t" + ratio + "###");
					
					String tks[] = p.key.split("#");
					String t1 = tks[0];
					String t2 = tks[1];
					boolean find = false;
					for(int i=0;i<seedClusters.size();i++) {
						HashSet<String> set = seedClusters.get(i);
						if(set.contains(t1) || set.contains(t2)) {
							set.add(t1);
							set.add(t2);
							find = true;
							break;
						}
					}
					if(!find) {
						HashSet<String> set = new HashSet<String>();
						set.add(t1);
						set.add(t2);
						seedClusters.add(set);
					}
					
					highRatio += 1;
				} else {
					System.out.println(p.key + ":\t" + p.count + "\t" + ratio);
				}
				if(ratio==0) {
					lowRatio += 1;
					outputNegativeLinks.add(p.key);
				}
			}
			if(ratio<.2) {
				negativeLinks.add(p);
			}
		}
		
		System.out.println("All Count:\t" + allCount);
		System.out.println("Cap Count:\t" + capCount);
		System.out.println("High Ratio: " + highRatio);
		
		System.out.println("lowRatio Ratio: " + lowRatio);
		
		Common.outputLines(output, "test");
		System.out.println(freq + "##");
		Common.outputHashSetList(seedClusters, "seedClusters");
		Common.outputLines(outputNegativeLinks, "negativePairs");
	}
}
