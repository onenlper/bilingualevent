package model;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.syntaxTree.MyTreeNode;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import util.Common;
import ace.EntityChainReader;
import ace.EventChainReader;
import ace.SourceReader;
import ace.TimeReader;
import ace.ValueReader;

public abstract class ACEDoc {

	public String fileID;
	public int docID;
	public int start;
	public String content;
	public int end;
	public HashMap<Integer, int[]> positionMap;
	public ArrayList<ParseResult> parseReults;
	public String headline;
	public HashMap<EventMention, SemanticRole> semanticRoles;
	protected String apfLine;
	protected String sgmLine;
	
	public ArrayList<EventMention> goldEventMentions;
	public ArrayList<EventChain> goldEventChains;
	public ArrayList<EntityMention> goldEntityMentions;
	public ArrayList<Entity> goldEntities;
	public HashMap<EventMention, HashSet<EventMention>> goldEventCorefMaps;
	public ArrayList<Entity> goldTimeChains;
	public ArrayList<EntityMention> goldTimeMentions;
	public ArrayList<Entity> goldValueChains;
	public ArrayList<EntityMention> goldValueMentions;
	public ArrayList<EventMentionArgument> goldArguments;
	public HashMap<String, Entity> goldSalienceChain;
	public HashMap<String, EntityMention> goldNPMentionMap;
	public HashMap<String, EventMention> goldEventMentionMap;
	public ArrayList<EntityMention> allGoldNPMentions = new ArrayList<EntityMention>();
	public HashMap<Integer, EntityMention> allGoldNPEndMap;

	
	public HashMap<String, Entity> id2EntityMap = new HashMap<String, Entity>();
	public HashMap<String, Entity> entityCorefMap = new HashMap<String, Entity>();
	public HashMap<String, Integer> eventCorefMap = new HashMap<String, Integer>();
	public ArrayList<EventMention> eventMentions;
	public ArrayList<EventChain> eventChains;

	public ACEDoc(String fileID, String sep) {
		System.setProperty("file.encoding", "UTF-8");
		String os = System.getProperty("os.name");
		if(os.startsWith("Windows")) {
			int a = fileID.indexOf("data");
			String stem = fileID.substring(a).replace("/", "\\");
			fileID = "C:\\Users\\USER\\workspace\\BilingualEvent\\data\\LDC2006T06\\" + stem;
//			System.out.println(fileID);
		}
		
		this.content = "";
		this.fileID = fileID;

		this.sgmLine = Common.getLine(fileID + ".sgm").replace("&", "&amp;")
				.replaceAll("\\<QUOTE[^\\>]*\\>", "");
		this.apfLine = Common.getLine(fileID + ".apf.xml");

		this.readGoldContent();

		this.readStanfordParseFile();

		this.readTimeExpressions(sep);
		this.readValues(sep);
		this.readGoldEntityChain(sep);
		this.allGoldNPMentions.addAll(this.goldEntityMentions);
		this.allGoldNPMentions.addAll(this.goldTimeMentions);
		this.allGoldNPMentions.addAll(this.goldValueMentions);
		Collections.sort(this.allGoldNPMentions);

		this.goldNPMentionMap = new HashMap<String, EntityMention>();
		for (EntityMention mention : this.allGoldNPMentions) {
			StringBuilder sb = new StringBuilder();
			sb.append(mention.start).append(",").append(mention.end);
			this.goldNPMentionMap.put(sb.toString(), mention);
		}

		this.readGoldEventChain();
		this.readGoldArguments();
		this.calSalienceEntity();
		this.readSemanticRole();

		this.allGoldNPEndMap = new HashMap<Integer, EntityMention>();
		for (EntityMention mention : this.allGoldNPMentions) {
			this.allGoldNPEndMap.put(mention.end, mention);
		}
		
		this.setEntityCorefMap(this.goldEntities);
		this.setEventCorefMap(this.goldEventChains);
	}

	public void setEntityCorefMap(ArrayList<Entity> entities) {
		entityCorefMap.clear();
		for (Entity e : entities) {
			for (EntityMention m : e.mentions) {
				entityCorefMap.put(m.start + "," + m.end, e);
			}
		}
	}
	
	public void setEventCorefMap(ArrayList<EventChain> chains) {
		this.eventCorefMap.clear();
		for(int i=0;i<chains.size();i++) {
			EventChain ec = chains.get(i);
			for(EventMention m : ec.getEventMentions()) {
				this.eventCorefMap.put(m.getAnchorStart() + "," + m.getAnchorEnd(), i);
			}
		}
	}

	public void calSalienceEntity() {
		goldSalienceChain = new HashMap<String, Entity>();
		for (Entity entity : goldEntities) {
			String type = entity.type;
			if (!goldSalienceChain.containsKey(type)) {
				goldSalienceChain.put(type, entity);
			} else {
				int chainLength = goldSalienceChain.get(type).mentions.size();
				if (entity.mentions.size() > chainLength) {
					goldSalienceChain.put(type, entity);
				}
			}
		}
	}

	public void readGoldContent() {
		if (this.sgmLine.charAt(0) == '\n') {
			this.content += "\n";
		}
		InputStream inputStream;
		try {
			inputStream = new ByteArrayInputStream(this.sgmLine.getBytes());
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			SourceReader reader = new SourceReader(this);
			sp.parse(new InputSource(inputStream), reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readTimeExpressions(String sep) {
		this.goldTimeChains = new ArrayList<Entity>();
		try {
			InputStream inputStream = new ByteArrayInputStream(
					this.apfLine.getBytes());
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			TimeReader reader = new TimeReader(goldTimeChains);
			sp.parse(new InputSource(inputStream), reader);
			this.goldTimeMentions = new ArrayList<EntityMention>();
			for (Entity entity : goldTimeChains) {
				this.goldTimeMentions.addAll(entity.getMentions());
			}
			for (EntityMention expression : this.goldTimeMentions) {
				expression.extent = expression.extent.replaceAll("\n", sep)
						.replaceAll("\\s+", sep);
				expression.head = expression.extent;
				expression.type = "Time";
				expression.entity.type = "Time";
				expression.entity.subType = "Time";
				expression.semClass = "time";
				expression.subType = "time";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readValues(String sep) {
		this.goldValueChains = new ArrayList<Entity>();
		try {
			InputStream inputStream = new ByteArrayInputStream(
					this.apfLine.getBytes());
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			ValueReader reader = new ValueReader(goldValueChains);
			sp.parse(new InputSource(inputStream), reader);
			this.goldValueMentions = new ArrayList<EntityMention>();
			for (Entity entity : goldValueChains) {
				this.goldValueMentions.addAll(entity.getMentions());
			}
			for (EntityMention value : goldValueMentions) {
				value.extent = value.extent.replaceAll("\n", sep).replaceAll(
						"\\s+", sep);
				value.head = value.extent;
				value.type = "Value";
				value.entity.type = "Value";
				value.entity.subType = "Value";
				value.semClass = "value";
				value.subType = "value";
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readGoldEntityChain(String sep) {
		this.goldEntities = new ArrayList<Entity>();
		this.goldEntityMentions = new ArrayList<EntityMention>();
		InputStream inputStream;
		try {
			inputStream = new ByteArrayInputStream(this.apfLine.getBytes());

			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			EntityChainReader reader = new EntityChainReader(goldEntities);
			sp.parse(new InputSource(inputStream), reader);
			for (Entity entity : goldEntities) {
				this.goldEntityMentions.addAll(entity.getMentions());
				for (EntityMention m : entity.mentions) {
					this.id2EntityMap.put(m.refID, entity);
				}
			}
			for (EntityMention mention : this.goldEntityMentions) {
				mention.extent = mention.extent.replaceAll("\n", sep);
				mention.head = mention.head.replaceAll("\n", sep);
				mention.semClass = mention.entity.getType();
				mention.subType = mention.entity.getSubType();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readGoldEventChain() {
		this.goldEventMentions = new ArrayList<EventMention>();
		this.goldEventChains = new ArrayList<EventChain>();
		this.goldEventCorefMaps = new HashMap<EventMention, HashSet<EventMention>>();

		InputStream inputStream;
		try {
			inputStream = new ByteArrayInputStream(this.apfLine.getBytes());
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			EventChainReader reader = new EventChainReader(goldEventChains);
			sp.parse(new InputSource(inputStream), reader);

			for (EventChain eventChain : goldEventChains) {
				goldEventMentions.addAll(eventChain.getEventMentions());
			}

			for (EventMention mention : goldEventMentions) {
				mention.document = this;
				// // remove duplicate arguments
				// HashSet<EventMentionArgument> eventMentionArgHash = new
				// HashSet<EventMentionArgument>();
				// eventMentionArgHash.addAll(mention.eventMentionArguments);
				//
				// mention.eventMentionArguments.clear();
				// mention.eventMentionArguments.addAll(eventMentionArgHash);

				for (EventMentionArgument argument : mention.eventMentionArguments) {
					argument.setEntityMention(this.goldNPMentionMap
							.get(argument.start + "," + argument.end));
				}

				for (EventChain eventChain : this.goldEventChains) {
					for (EventMention em : eventChain.getEventMentions()) {
						HashSet<EventMention> ems = new HashSet<EventMention>();
						for (EventMention em2 : eventChain.getEventMentions()) {
							if (!em.equals(em2)) {
								ems.add(em2);
							}
						}
						this.goldEventCorefMaps.put(em, ems);
					}
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readGoldEventMention() {

		InputStream inputStream;
		try {
			inputStream = new ByteArrayInputStream(this.apfLine.getBytes());
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			ArrayList<EventChain> eventChains2 = new ArrayList<EventChain>();
			EventChainReader reader = new EventChainReader(eventChains2);
			sp.parse(new InputSource(inputStream), reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void readGoldArguments() {
		this.goldEventMentionMap = new HashMap<String, EventMention>();
		this.goldArguments = new ArrayList<EventMentionArgument>();
		for (EventMention eventMention : this.goldEventMentions) {
			this.goldArguments.addAll(eventMention.getEventMentionArguments());
			this.goldEventMentionMap.put(eventMention.toString(), eventMention);
		}
	}

	public abstract void readStanfordParseFile();

	public abstract void readSemanticRole();

	public MyTreeNode getTreeNode(int idx) {
		int position[] = this.positionMap.get(idx);
		return this.parseReults.get(position[0]).tree.leaves.get(position[1]);
	}
	
	public String getWord(int idx) {
		int position[] = this.positionMap.get(idx);
		return this.parseReults.get(position[0]).words.get(position[1]);
	}

	public String getLemma(int idx) {
		int position[] = this.positionMap.get(idx);
		return this.parseReults.get(position[0]).lemmas.get(position[1]);
	}

	public String getPostag(int idx) {
		int position[] = this.positionMap.get(idx);
//		System.out.println(this.parseReults);
//		System.out.println(idx);
//		System.out.println(this.positionMap.values());
		return this.parseReults.get(position[0]).posTags.get(position[1]);
	}

	public ParseResult getParseResult(int idx) {
		int position[] = this.positionMap.get(idx);
		return this.parseReults.get(position[0]);
	}
}
