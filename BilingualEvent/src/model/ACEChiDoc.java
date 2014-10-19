package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import util.Common;

public class ACEChiDoc extends ACEDoc{

	public ACEChiDoc(String fileID) {
		super(fileID, "");
	}

	public void readSemanticRole() {
		this.semanticRoles = new ArrayList<SemanticRole>();
		ArrayList<String> srlInLines = Common.getLines(this.fileID + ".slrin");
		ArrayList<String> srlOutLines = Common.getLines(this.fileID + ".slrout");

		HashMap<Integer, EntityMention> entityMap = new HashMap<Integer, EntityMention>();
		for (EntityMention mention : this.goldEntityMentions) {
			entityMap.put(mention.end, mention);
		}
		for (EntityMention mention : this.goldTimeMentions) {
			entityMap.put(mention.end, mention);
		}
		for (EntityMention mention : this.goldValueMentions) {
			entityMap.put(mention.end, mention);
		}

		for (int i = 0; i < srlInLines.size();) {
			if (srlInLines.get(i).isEmpty()) {
				i++;
				continue;
			}
			ArrayList<SemanticRole> roles = new ArrayList<SemanticRole>();
			int semanticSize = srlOutLines.get(i).split("\\s+").length - 14;
			for (int j = 0; j < semanticSize; j++) {
				SemanticRole role = new SemanticRole();
				roles.add(role);
			}
			int predictIndex = 0;
			while (true) {
				String slrIn = srlInLines.get(i);
				String slrOut = srlOutLines.get(i);
				if (slrIn.trim().isEmpty()) {
					break;
				}

				String tokens[] = slrIn.split("\\s+");
				int start = Integer.parseInt(tokens[2]);
				int end = Integer.parseInt(tokens[3]);

				EventMention word = new EventMention();
				word.setAnchorStart(start);
				word.setAnchorEnd(end);

				tokens = slrOut.split("\\s+");
				word.setAnchor(tokens[1]);

				if (tokens[12].equalsIgnoreCase("Y")) {
					roles.get(predictIndex).predict = word;
					predictIndex++;
				}
				for (int j = 0; j < semanticSize; j++) {
					String label = tokens[14 + j];
					if (label.equalsIgnoreCase("A0") || label.equalsIgnoreCase("A1") || label.equalsIgnoreCase("TMP")) {
						EntityMention entityMention = entityMap.get(end);
//						if (entityMention == null) {
//							for (int m=0; m < this.allMentions.size(); m++) {
//								EntityMention temp = this.allMentions.get(m);
//								if (temp.end >= end && temp.start <= start) {
//									entityMention = temp;
//									break;
//								}
//							}
//						}
						if (entityMention != null) {
							if (label.equalsIgnoreCase("A0")) {
								roles.get(j).arg0.add(entityMention);
							}
							if (label.equalsIgnoreCase("A1")) {
								roles.get(j).arg1.add(entityMention);
							}
							if (label.equalsIgnoreCase("TMP")) {
								roles.get(j).tmp.add(entityMention);
							}
						}
					}
				}
				i++;
			}
			this.semanticRoles.addAll(roles);
		}
		Collections.sort(this.semanticRoles);
	}

	public void readGold4Test() {
		this.eventMentions = this.goldEventMentions;
		this.eventChains = new ArrayList<EventChain>();
		int chainID = 0;
		for (EventMention eventMention : eventMentions) {
			EventChain eventChain = new EventChain();
			eventChain.chainID = chainID++;
			eventChain.addEventMention(eventMention);
			eventChains.add(eventChain);
		}
	}

	public void outputEventChain(String filename) {
		ArrayList<String> lines = new ArrayList<String>();
		for (EventChain eventChain : this.eventChains) {
			StringBuilder sb = new StringBuilder();
			for (EventMention eventMention : eventChain.getEventMentions()) {
				sb.append(eventMention.getAnchorStart()).append(",").append(eventMention.getAnchorEnd()).append(" ");
			}
			lines.add(sb.toString().trim());
		}
		Common.outputLines(lines, filename);
	}

	public void readStanfordParseFile() {
		this.parseReults = new ArrayList<ParseResult>();
		ArrayList<String> lines = Common.getLines(fileID + ".parse2");
		int idx = -1;
		String content = this.content;
		boolean first = true;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String sentence = line;
			StringBuilder sb = new StringBuilder();
			int j = i + 2;
			while (!lines.get(j).trim().isEmpty()) {
				sb.append(lines.get(j));
				// System.out.println(lines.get(j));
				j++;
			}
			String treeStr = sb.toString();
			int k = j + 1;
			ArrayList<Depend> depends = new ArrayList<Depend>();
			while (!lines.get(k).trim().isEmpty()) {
				line = lines.get(k);
				// System.out.println(line);
				int pos = line.indexOf('(');
				String type = line.substring(0, pos);
				String tokens[] = line.split(" ");
				pos = tokens[0].lastIndexOf('-');
				String t1 = tokens[0].substring(pos + 1, tokens[0].length() - 1);
				pos = tokens[1].lastIndexOf('-');
				String t2 = tokens[1].substring(pos + 1, tokens[1].length() - 1);
				Depend depend = new Depend(type, Integer.valueOf(t1), Integer.valueOf(t2));
				depends.add(depend);
				k++;
			}
			i = k;
			ParseResult pr = new ParseResult(sentence, Common.constructTree(treeStr), depends);

			if (first) {
				StringBuilder headlineSB = new StringBuilder();
				String tokens[] = sentence.split(" ");
				for (String token : tokens) {
					if (token.isEmpty()) {
						continue;
					}
					int pos = token.lastIndexOf('/');
					String word = token.substring(0, pos);
					headlineSB.append(word);
				}
				this.headline = headlineSB.toString();
				first = false;
			}

			ArrayList<String> words = pr.words;
			ArrayList<int[]> positions = new ArrayList<int[]>();
			int empty[] = { -1, -1 };
			positions.add(empty);
			for (int n = 1; n < words.size(); n++) {
				String token = words.get(n);
				int[] p = new int[2];
				idx = content.indexOf(token.charAt(0), idx + 1);
				p[0] = idx;
				for (int m = 1; m < token.length(); m++) {
					idx = content.indexOf(token.charAt(m), idx + 1);
				}
				p[1] = idx;
				positions.add(p);
				// System.out.println(p[0] + " " + p[1] + " " + token);
			}
			pr.positions = positions;
			parseReults.add(pr);
		}
	}

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("/users/yzcchen/workspace/Coling2012/src/ACE_Chinese_all");
		for(String file : files) {
//			if (!file.equals("/users/yzcchen/ACL12/data/ACE2005/English/un/timex2norm/alt.books.tom-clancy_20050130.1848")) {
//				continue;
//			}
			System.out.println(file);
			ACEDoc doc = new ACEChiDoc(file);
			String content = doc.content;
			
			for(Entity entity : doc.goldEntities) {
				for(EntityMention m : entity.mentions) {
					String text = content.substring(m.start, m.end+1).replace("\n", "").replace("&amp;", "&");
					if(!m.extent.equals(text)) {
						System.out.println(m.extent + " # " + text);
						System.out.println(m.start + " # " + m.end);
						System.out.println(text + " :: " + text.length());
						Common.pause("");
					}
					
					String head = content.substring(m.headStart, m.headEnd +1).replace("\n", "").replace("&amp;", "&");
					if(!m.head.equals(head)) {
						System.out.println(m.head + " # " + head);
						Common.pause("");
					}
				}
			}

			for(EntityMention m : doc.goldValueMentions) {
				String text = content.substring(m.start, m.end+1).replace("\n", "").replace("&amp;", "&").replaceAll("\\s+", "");
				if(!m.extent.equals(text)) {
					System.out.println(m.extent + " # " + text);
					Common.pause("");
				}
			}
			
			for(EntityMention m : doc.goldTimeMentions) {
				String text = content.substring(m.start, m.end+1).replace("\n", "").replace("&amp;", "&").replaceAll("\\s+", "");
				if(!m.extent.equals(text)) {
					System.out.println(m.extent + " # " + text);
					Common.pause("");
				}
			}
			
			for(EventMention evm : doc.goldEventMentions) {
				String text = content.substring(evm.anchorStart, evm.anchorEnd + 1).replace("\n", " ").replace("&amp;", "&").replaceAll("\\s+", "");
				if(!evm.anchor.replaceAll("\\s+", "").equals(text)) {
					System.out.println(evm.anchor + " # " + text);
					Common.pause("");
				}
			}
		}
	}
	
}
