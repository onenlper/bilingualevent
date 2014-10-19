package model.stanford;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import util.Common;

public class StanfordXMLReader extends DefaultHandler {

	// change the index from 0, end position minus 1

	StanfordResult result;
	StanfordSentence sentence;
	StanfordToken token;
	StanfordDep dep;
	ArrayList<StanfordMention> chain;
	StanfordMention mention;

	ArrayList<String> tags = new ArrayList<String>();

	public StanfordXMLReader(StanfordResult sr) {
		this.result = sr;
	}

	int entityId = -1;

	String dependency_type = "";
	
	public void startElement(String uri, String name, String qName, Attributes atts) {
		tags.add(qName);
		String tag2 = "";
		if (tags.size() >= 2) {
			tag2 = tags.get(tags.size() - 2);
		}
		if (qName.equalsIgnoreCase("sentences")) {

		} else if (qName.equalsIgnoreCase("sentence") && tag2.equalsIgnoreCase("sentences")) {
			sentence = new StanfordSentence();
			int id = Integer.valueOf(atts.getValue("id")) - 1;
			sentence.id = id;
			result.addSentence(sentence);
		} else if (qName.equalsIgnoreCase("token")) {
			token = new StanfordToken();
			int id = Integer.valueOf(atts.getValue("id")) - 1;
			token.id = id;
			token.sentenceId = sentence.id;
			this.sentence.addToken(token);
		} else if (qName.equalsIgnoreCase("dep")) {
			dep = new StanfordDep();
			String type = atts.getValue("type");
			dep.type = type;
			if (tags.get(tags.size() - 2).equals("dependencies") && dependency_type.equalsIgnoreCase("basic-dependencies")) {
				sentence.basicDependencies.add(dep);
			} else if (tags.get(tags.size() - 2).equals("dependencies") && dependency_type.equalsIgnoreCase("collapsed-dependencies")) {
				sentence.collapsedDependencies.add(dep);
			} else if (tags.get(tags.size() - 2).equals("dependencies") && dependency_type.equalsIgnoreCase("collapsed-ccprocessed-dependencies")) {
				sentence.collapsedCcprocessedDependencies.add(dep);
			}
		} else if (qName.equalsIgnoreCase("governor")) {
			dep.governorId = Integer.valueOf(atts.getValue("idx"));
		} else if (qName.equalsIgnoreCase("dependent")) {
			dep.dependentId = Integer.valueOf(atts.getValue("idx"));
		} else if (qName.equalsIgnoreCase("coreference") && tag2.equalsIgnoreCase("coreference")) {
			chain = new ArrayList<StanfordMention>();
			entityId++;
			result.addChain(chain);
		} else if (qName.equalsIgnoreCase("mention")) {
			mention = new StanfordMention();
			mention.setRepresentative(false);
			mention.entityId = entityId;
			String representative = atts.getValue("representative");
			if (representative != null && representative.equalsIgnoreCase("true")) {
				mention.setRepresentative(true);
			}
			chain.add(mention);
		} else if (qName.equalsIgnoreCase("Timex")) {
			StanfordTimex timex = new StanfordTimex();
			timex.setTid(atts.getValue("tid"));
			timex.setType(atts.getValue("type"));
			timex.setValue("");
			token.setTimex(timex);
		} else if (qName.equalsIgnoreCase("dependencies")) {
			this.dependency_type = atts.getValue("type");
		}
	}

	public void characters(char ch[], int start, int length) throws SAXException {
		String tag = tags.get(tags.size() - 1);
		String tag2 = "";
		if (tags.size() >= 2) {
			tag2 = tags.get(tags.size() - 2);
		}
		if (tag.equalsIgnoreCase("word")) {
			token.setWord(new String(ch, start, length));
		} else if (tag.equalsIgnoreCase("lemma")) {
			token.setLemma(new String(ch, start, length));
		} else if (tag.equalsIgnoreCase("CharacterOffsetBegin")) {
			token.setCharacterOffsetBegin(Integer.valueOf(new String(ch, start, length)));
		} else if (tag.equalsIgnoreCase("CharacterOffsetEnd")) {
			token.setCharacterOffsetEnd(Integer.parseInt(new String(ch, start, length)) - 1);
		} else if (tag.equalsIgnoreCase("POS")) {
			token.setPOS(new String(ch, start, length));
		} else if (tag.equalsIgnoreCase("NER")) {
			token.setNer(new String(ch, start, length));
		} else if (tag.equalsIgnoreCase("parse")) {
			sentence.parse += new String(ch, start, length);
		} else if (tag.equalsIgnoreCase("governor")) {
			dep.setGovernor(new String(ch, start, length));
		} else if (tag.equalsIgnoreCase("dependent")) {
			dep.setDependent(new String(ch, start, length));
		} else if (tag2.equalsIgnoreCase("mention") && tag.equalsIgnoreCase("sentence")) {
			mention.setSentenceId(Integer.valueOf(new String(ch, start, length)) -1);
		} else if (tag2.equalsIgnoreCase("mention") && tag.equalsIgnoreCase("start")) {
			mention.setStartId(Integer.valueOf(new String(ch, start, length)));
		} else if (tag2.equalsIgnoreCase("mention") && tag.equalsIgnoreCase("end")) {
			mention.setEndId(Integer.valueOf(new String(ch, start, length)) -2);
		} else if (tag2.equalsIgnoreCase("mention") && tag.equalsIgnoreCase("head")) {
			mention.setHeadId(Integer.valueOf(new String(ch, start, length)) -1);
		} else if (tag.equalsIgnoreCase("NormalizedNER")) {
			token.setNormalizedNER(new String(ch, start, length));
		} else if (tag.equalsIgnoreCase("Timex")) {
			token.getTimex().setValue(new String(ch, start, length));
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		String tag = tags.get(tags.size() - 1);
		if (tag.equalsIgnoreCase("parse")) {
			sentence.parseTree = Common.constructTree(sentence.parse);
		}
		tags.remove(tags.size() - 1);
	}

	public static class StanfordDep {
		String type;
		String governor;
		String dependent;
		int governorId;
		int dependentId;

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getGovernor() {
			return governor;
		}

		public void setGovernor(String governor) {
			this.governor = governor;
		}

		public String getDependent() {
			return dependent;
		}

		public void setDependent(String dependent) {
			this.dependent = dependent;
		}

		public int getGovernorId() {
			return governorId;
		}

		public void setGovernorId(int governorId) {
			this.governorId = governorId;
		}

		public int getDependentId() {
			return dependentId;
		}

		public void setDependentId(int dependentId) {
			this.dependentId = dependentId;
		}
	}

	private static HashMap<String, StanfordResult> cache = new HashMap<String, StanfordResult>();

	public static StanfordResult read(String file) {
//		file = Common.changeSurffix(file, "stanford");
		if (cache.containsKey(file)) {
			return cache.get(file);
		}
		StanfordResult stanfordResult = new StanfordResult();
		try {
			InputStream inputStream = new FileInputStream(file);
			SAXParserFactory sf = SAXParserFactory.newInstance();
			SAXParser sp = sf.newSAXParser();
			StanfordXMLReader reader = new StanfordXMLReader(stanfordResult);
			sp.parse(new InputSource(inputStream), reader);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		cache.put(file, stanfordResult);
		return stanfordResult;
	}

	public static void main(String args[]) throws Exception {
		String file = "/shared/mlrdir1/disk1/home/yzcchen/ACL12/data/ACE2005/Chinese/TRANSLATE/XIN20001020.0200.0006.english.xml";
		StanfordResult stanfordResult = new StanfordResult();
		InputStream inputStream = new FileInputStream(new File(file));
		SAXParserFactory sf = SAXParserFactory.newInstance();
		SAXParser sp = sf.newSAXParser();
		StanfordXMLReader reader = new StanfordXMLReader(stanfordResult);
		sp.parse(new InputSource(inputStream), reader);
		StanfordCoreference sc = stanfordResult.coreference;
		for (ArrayList<StanfordMention> chain : sc.coreferenceChains) {
			for (StanfordMention mention : chain) {
				System.out.println(mention.sentenceId + " " + mention.startId + " " + mention.endId);
			}
			System.out.println("++++++++++++++++++++++++++++++++++++++++++++==");
		}
	}

}
