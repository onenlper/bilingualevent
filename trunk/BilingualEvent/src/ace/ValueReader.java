package ace;

import java.util.ArrayList;

import model.Entity;
import model.EntityMention;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ValueReader extends DefaultHandler {
	ArrayList<String> tags = new ArrayList<String>();
	ArrayList<Entity> valueChains;
	Entity currentValueChain;
	EntityMention currentValueMention;

	int chainID = 0;
	
	public ValueReader(ArrayList<Entity> chains) {
		super();
		valueChains = chains;
	}

	boolean valueMention = false;
	
	public void startElement(String uri, String name, String qName, Attributes atts) {
		tags.add(qName);
		if (qName.equals("value")) {
			Entity timeChain = new Entity();
			this.currentValueChain = timeChain;
			valueChains.add(timeChain);
		}
		if (qName.equals("value_mention")) {
			EntityMention em = new EntityMention();
			em.extent = "";
			this.currentValueMention = em;
			this.currentValueChain.addMention(em);
			valueMention = true;
		}
		if (qName.equals("charseq") && valueMention) {
			int start = Integer.valueOf(atts.getValue("START"));
			int end = Integer.valueOf(atts.getValue("END"));
			this.currentValueMention.start = start;
			this.currentValueMention.end = end;
			this.currentValueMention.headStart = start;
			this.currentValueMention.headEnd = end;
		}
	}

	public void characters(char ch[], int start, int length) throws SAXException {
		if(this.valueMention) {
			String str = (new String(ch, start, length));
			this.currentValueMention.extent += str;
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(tags.get(tags.size()-1).equals("value_mention")) {
			this.valueMention = false;
		}
		tags.remove(tags.size() - 1);
	}
}