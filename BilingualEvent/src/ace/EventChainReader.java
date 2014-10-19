package ace;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import model.EventArgument;
import model.EventChain;
import model.EventMention;
import model.EventMentionArgument;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import util.Common;

public class EventChainReader extends DefaultHandler {
	ArrayList<String> tags = new ArrayList<String>();
	ArrayList<EventChain> eventChains;
	EventMention currentEventMention;
	EventChain currentEventChain;
	EventMentionArgument currentEventMentionArgument;

	int chainID = 0;
	
	public EventChainReader(ArrayList<EventChain> chains) {
		super();
		eventChains = chains;
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {
		tags.add(qName);
		if (qName.equals("event")) {
			EventChain eventChain = new EventChain();
			eventChain.setChainID(chainID++);
			eventChain.setID(atts.getValue("ID"));
			eventChain.setType(atts.getValue("TYPE"));
			eventChain.setSubtype(atts.getValue("SUBTYPE"));
			eventChain.setModality(atts.getValue("MODALITY"));
			eventChain.setPolarity(atts.getValue("POLARITY"));
			eventChain.setGenericity(atts.getValue("GENERICITY"));
			eventChain.setTense(atts.getValue("TENSE"));
			currentEventChain = eventChain;
			eventChains.add(eventChain);
		}
		if (qName.equals("event_argument")) {
			EventArgument eventArgument = new EventArgument();
			eventArgument.setRefID(atts.getValue("REFID"));
			eventArgument.setRole(atts.getValue("ROLE"));
			currentEventChain.getArguments().add(eventArgument);
		}
		if (qName.equals("event_mention")) {
			EventMention eventMention = new EventMention();
			eventMention.setID(atts.getValue("ID"));
			currentEventMention = eventMention;
			currentEventChain.getEventMentions().add(eventMention);
			eventMention.setEventChain(currentEventChain);
			eventMention.setEventChainID(currentEventChain.getChainID());
			
			eventMention.polarity = currentEventChain.getPolarity();
			eventMention.modality = currentEventChain.getModality();
			eventMention.genericity = currentEventChain.getGenericity();
			eventMention.tense = currentEventChain.getTense();
			eventMention.type = currentEventChain.getType();
			eventMention.subType = currentEventChain.getSubtype();
		}
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("extent")
				&& tags.get(tags.size() - 3).equals("event_mention")) {
			currentEventMention.setStart(Integer.valueOf(atts.getValue("START")));
			currentEventMention.setEnd(Integer.valueOf(atts.getValue("END")));
		}
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("ldc_scope")
				&& tags.get(tags.size() - 3).equals("event_mention")) {
			currentEventMention.setLdcScopeStart(Integer.valueOf(atts.getValue("START")));
			currentEventMention.setLdcScopeEnd(Integer.valueOf(atts.getValue("END")));
		}
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("anchor")
				&& tags.get(tags.size() - 3).equals("event_mention")) {
			currentEventMention.setAnchorStart(Integer.valueOf(atts.getValue("START")));
			currentEventMention.setAnchorEnd(Integer.valueOf(atts.getValue("END")));
		}
		if (qName.equals("event_mention_argument")) {
			EventMentionArgument eventMentionArgument = new EventMentionArgument();
			eventMentionArgument.setRefID(atts.getValue("REFID"));
			eventMentionArgument.setRole(atts.getValue("ROLE"));
			currentEventMention.getEventMentionArguments().add(eventMentionArgument);
			currentEventMentionArgument = eventMentionArgument;
			eventMentionArgument.setEventMention(this.currentEventMention);
		}
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("extent")
				&& tags.get(tags.size() - 3).equals("event_mention_argument")
				&& tags.get(tags.size() - 4).equals("event_mention")) {
			currentEventMentionArgument.setStart(Integer.valueOf(atts.getValue("START")));
			currentEventMentionArgument.setEnd(Integer.valueOf(atts.getValue("END")));
		}
	}

	public void characters(char ch[], int start, int length) throws SAXException {
		String qName = tags.get(tags.size()-1);
		String str = (new String(ch, start, length)).replace("\n", " ")
//		.replaceAll("\\s+", "")
		;
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("extent")
				&& tags.get(tags.size() - 3).equals("event_mention_argument")
				&& tags.get(tags.size() - 4).equals("event_mention")) {
			currentEventMentionArgument.setExtent(currentEventMentionArgument.getExtent() + str);
		}
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("anchor")
				&& tags.get(tags.size() - 3).equals("event_mention")) {
			currentEventMention.setAnchor(currentEventMention.getAnchor() + str);
		}
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("ldc_scope")
				&& tags.get(tags.size() - 3).equals("event_mention")) {
			currentEventMention.setLdcScope(currentEventMention.getLdcScope() + str);
		}
		if (qName.equals("charseq") && tags.get(tags.size() - 2).equals("extent")
				&& tags.get(tags.size() - 3).equals("event_mention")) {
			currentEventMention.setExtent(currentEventMention.getExtent() + str);
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		tags.remove(tags.size() - 1);
	}
	
	public static void main(String args[]) {
		if(args.length<1) {
			System.out.println("java ~ [chinese|english]");
		}
		String language = args[0];
		String nl = "";
		if(language.equalsIgnoreCase("chinese")) {
			nl = "";
		} else {
			nl = " ";
		}
 		ArrayList<String> filenames = Common.getLines("ACE_" + args[0]);
		double chains = 0;
		double eventMentions = 0;
		HashMap<String, ArrayList<EventMention>> typeEM = new HashMap<String, ArrayList<EventMention>>();
		HashMap<String, ArrayList<EventMention>> subTypeEM = new HashMap<String, ArrayList<EventMention>>();
		for(String filename : filenames) {
			String apfFile = filename + ".apf.xml";
			System.out.println(apfFile+"\n*******************************************");
			ArrayList<EventChain> eventChains = new ArrayList<EventChain>();
			try {
				InputStream inputStream = new FileInputStream(new File(apfFile));
				SAXParserFactory sf = SAXParserFactory.newInstance();
				SAXParser sp = sf.newSAXParser();
				EventChainReader reader = new EventChainReader(eventChains);
				sp.parse(new InputSource(inputStream), reader);
				chains += eventChains.size();
				for(EventChain eventChain : eventChains) {
					System.out.println(eventChain.getEventMentions().size());
					eventMentions += eventChain.getEventMentions().size();
					for(EventMention em : eventChain.getEventMentions()) {
						String type = eventChain.getType();
						String subType = eventChain.getSubtype();
						em.setType(type);
						em.setSubType(subType);
						ArrayList<EventMention> ems;
						ems = typeEM.get(type);
						if(ems==null) {
							ems = new ArrayList<EventMention>();
							typeEM.put(type, ems);
						}
						ems.add(em);
						if(em.modality.equals("Other"))
						System.out.println(em.getAnchor().replace("\n", nl)+"=>"+em.getLdcScope().replace("\n", nl)
							+"#"+em.getType()+"#"+em.getSubType()+" " + em.modality);
						ems = subTypeEM.get(subType);
						if(ems==null) {
							ems = new ArrayList<EventMention>();
							subTypeEM.put(subType, ems);
						}
						ems.add(em);
					}
					System.out.println("=====================");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
//		for(String type:typeEM.keySet()) {
//			System.out.println("=====================================");
//			for(EventMention em : typeEM.get(type)) {
//				System.out.println(em.getAnchor().replace("\n", nl)+"=>"+em.getExtent().replace("\n", nl)
//					+"#"+em.getType()+"#"+em.getSubType()+" " + em.getChainID());
//			}
//		}
//		for(String subType:subTypeEM.keySet()) {
//			System.out.println("=====================================");
//			for(EventMention em : subTypeEM.get(subType)) {
//				System.out.println(em.getAnchor().replace("\n", nl)+"=>"+em.getExtent().replace("\n", nl)
//					+"#"+em.getType()+"#"+em.getSubType()+" " + em.getChainID());
//			}
//		}
		System.out.println("Chains: " + chains);
		System.out.println("EventMentions: " + eventMentions);
		System.out.println("Avg: " + eventMentions/chains);
	}
}