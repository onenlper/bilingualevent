package model;

import java.util.ArrayList;
import java.util.HashMap;

import model.stanford.StanfordResult;
import model.stanford.StanfordSentence;
import model.stanford.StanfordToken;
import model.stanford.StanfordXMLReader;
import model.stanford.StanfordXMLReader.StanfordDep;
import util.Common;

public class ACEEngDoc extends ACEDoc {

	public ACEEngDoc(String fileID) {
		super(fileID, " ");
	}

	@Override
	public void readStanfordParseFile() {
		this.positionMap = new HashMap<Integer, int[]>();
		String stdFn = this.fileID + ".source.xml";
		StanfordResult sr = StanfordXMLReader.read(stdFn);
		this.parseReults = standford2ParseResult(sr);
	}

	private ArrayList<ParseResult> standford2ParseResult(StanfordResult sr) {
		ArrayList<ParseResult> parseReults = new ArrayList<ParseResult>();
		
		int index = 0;
		for(int i=0;i<sr.sentences.size();i++) {
			StanfordSentence ss = sr.sentences.get(i);
			ParseResult pr = new ParseResult();
			
			pr.words.add("ROOT");
			pr.posTags.add("ROOT");
			pr.lemmas.add("ROOT");
			int[] rootPosition = {-1, -1};
			pr.positions.add(rootPosition);
			
			pr.tree = ss.parseTree;
			StringBuilder sb = new StringBuilder();
			for(int j=0;j<ss.tokens.size();j++) {
				StanfordToken tk = ss.tokens.get(j);
				int start = this.content.indexOf(tk.word, index);
				
				if(start==-1 || (start-index>5) && index!=0) {
					start = this.content.indexOf(tk.word, index-1);
				}
				
				if(start==-1 || (start-index>20) && index!=0) {
					tk.word = tk.word.replace("``", "\"").replace("''", "\"").replace("`", "'").replace("-LRB-", "(").replace(" ", " ")
							.replace("-RRB-", ")").replace("-LSB-", "[").replace("-RSB-", "]").replace("-LCB-", "{").replace("-RCB-", "}").replace("...", ". . .");
					start = this.content.indexOf(tk.word, index);
				}
				if(start==-1) {
//					System.out.println(pr.words.get(pr.words.size()-2) + "!!");
//					System.out.println(pr.words.get(pr.words.size()-1) + "##");
//					System.out.println(pr.positions.get(pr.positions.size()-2)[0] + "%%" +
//							pr.positions.get(pr.positions.size()-2)[1]);
					
//					System.out.println(this.content.substring(4195, 4205));
//					System.out.println(this.content.charAt(4197) + "");
					System.out.println(index);
					System.out.println(tk.word);
					System.out.println(this.fileID);
					Common.pause("");
				}
				int end = start + tk.word.length()-1;
				
				sb.append(tk.word).append(" ");
				pr.words.add(tk.word);
				pr.posTags.add(tk.POS);
				pr.lemmas.add(tk.lemma);
				
				index = end + 1;
				int[] positions = new int[2];
				positions[0] = start;
				positions[1] = end;
				pr.positions.add(positions);
				
//				if(start!=tk.CharacterOffsetBegin || end!=tk.CharacterOffsetEnd) {
//					System.out.println(tk.word);
//					System.out.println(start + "," + end + ":" + tk.CharacterOffsetBegin + "," + (tk.CharacterOffsetEnd));
////					Common.bangErrorPOS("");
////					Common.pause("");
//				}
				
				int p[] = new int[2];
				p[0] = i;
				p[1] = j+1;
				for(int k=start;k<=end;k++) {
					this.positionMap.put(k, p);
				}
				for(int k=tk.CharacterOffsetBegin;k<=tk.CharacterOffsetEnd;k++) {
					this.positionMap.put(k, p);
				}
			}
			pr.sentence = sb.toString().trim();
			
			for(StanfordDep dep : ss.collapsedCcprocessedDependencies) {
				int first = dep.getGovernorId();
				int second = dep.getDependentId();
				String type = dep.getType();
				pr.depends.add(new Depend(type, first, second));
			}
			pr.dependTree = new DependTree(pr.depends);
			
			parseReults.add(pr);
		}
		return parseReults;
	}

	@Override
	public void readSemanticRole() {
		// TODO Auto-generated method stub
		
	}
	
	public static void main(String args[]) {
//		check();
		ArrayList<String> files = Common.getLines("ACE_English_all");
		double all = 0;
		double single = 0;
		
		int ss = 0;
		
		ArrayList<String> lines = new ArrayList<String>();
		double eventAmount = 0;
		double chainAmount = 0;
		double allChainLength = 0;
		
		double nonSingletonChains = 0;
		double nonSingletonMentions = 0;
		
		for(String file : files) {
			ACEDoc doc = new ACEEngDoc(file);
			ArrayList<EventChain> chains = doc.goldEventChains;
			
			for(EventChain c : chains) {
				allChainLength += c.eventMentions.size();
				eventAmount += c.getEventMentions().size();
				
				if(c.getEventMentions().size()!=1) {
					nonSingletonChains += 1;
					nonSingletonMentions += c.eventMentions.size();
				}
				StringBuilder sb = new StringBuilder();
				
				for(EventMention em : c.getEventMentions()) {
					sb.append(em.getAnchor()).append("      #########      ");
				}
				System.out.println(sb.toString().trim());
			}
			chainAmount += chains.size();
//			for(ParseResult pr : doc.parseReults) {
//				StringBuilder sb = new StringBuilder();
//				for(String word : pr.words) {
//					sb.append(word).append(" ");
//				}
//				lines.add(sb.toString().trim());
//			}
//			Common.outputLine(doc.content, file + ".source");
//			for(EventMention em : doc.goldEventMentions) {
//				if(em.anchor.trim().split("\\s+").length==1) {
//					single += 1;
//				} else {
//					System.out.println(em.anchor);
//				}
//			}
//			all += doc.goldEventMentions.size();
//			ss += doc.parseReults.size();
//			for(EventMention em: doc.goldEventMentions) {
//				if(em.anchor.contains("-") || (em.anchorStart!=0 && doc.content.charAt(em.anchorStart-1)=='-')) {
//					System.out.println(em.anchor);
//				}
//			}
		}
		System.out.println("All Events: " + eventAmount);
		System.out.println("Events per Doc: " + eventAmount/files.size());
		
		System.out.println("All Chains: " + chainAmount);
		System.out.println("Chains per Doc: " + chainAmount/files.size());
		System.out.println("Mentions per Chain: " + eventAmount/chainAmount);
		
		System.out.println("Non Singleton Mentions: " + nonSingletonMentions);
		System.out.println("Non Singleton Chains: " + nonSingletonChains);
		System.out.println("Non Singleton Mentions per chain: " + nonSingletonMentions/nonSingletonChains);
		
//		System.out.println(single/all);
//		System.out.println(ss);
//		Common.outputLines(lines, "brown_input");
	}

	private static void check() {
		ArrayList<String> files = Common.getLines("ACE_English_all");
		for(String file : files) {
//			if (!file.equals("/users/yzcchen/ACL12/data/ACE2005/English/un/timex2norm/alt.books.tom-clancy_20050130.1848")) {
//				continue;
//			}
			ACEEngDoc doc = new ACEEngDoc(file);
			String content = doc.content;
			
			for(Entity entity : doc.goldEntities) {
				for(EntityMention m : entity.mentions) {
					String text = content.substring(m.start, m.end+1).replace("\n", " ").replace("&amp;", "&");
					if(!m.extent.equals(text)) {
						System.out.println(m.extent + " # " + text);
						System.out.println(m.start + " # " + m.end);
						System.out.println(text + " :: " + text.length());
						Common.pause("");
					}
					
					String head = content.substring(m.headStart, m.headEnd +1).replace("\n", " ").replace("&amp;", "&");
					if(!m.head.equals(head)) {
						System.out.println(m.head + " # " + head);
						Common.pause("");
					}
				}
			}

			for(EntityMention m : doc.goldValueMentions) {
				String text = content.substring(m.start, m.end+1).replace("\n", " ").replace("&amp;", "&");
				if(!m.extent.equals(text)) {
					System.out.println(m.extent + " # " + text);
				}
			}
			
			for(EntityMention m : doc.goldTimeMentions) {
				String text = content.substring(m.start, m.end+1).replace("\n", " ").replace("&amp;", "&");
				if(!m.extent.equals(text)) {
					System.out.println(m.extent + " # " + text);
				}
			}
			
			for(EventMention evm : doc.goldEventMentions) {
				String text = content.substring(evm.anchorStart, evm.anchorEnd + 1).replace("\n", " ").replace("&amp;", "&");
				if(!evm.anchor.equals(text)) {
					System.out.println(evm.anchor + " # " + text);
					Common.pause("");
				}
			}
		}
	}
	
}
