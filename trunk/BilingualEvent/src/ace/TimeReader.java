package ace;

import java.util.ArrayList;

import model.Entity;
import model.EntityMention;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TimeReader extends DefaultHandler {
	ArrayList<String> tags = new ArrayList<String>();
	ArrayList<Entity> timeChains;
	Entity currentTimeChain;
	EntityMention currentTimeMention;

	int chainID = 0;
	
	public TimeReader(ArrayList<Entity> chains) {
		super();
		timeChains = chains;
	}

	boolean timeMention = false;
	
	public void startElement(String uri, String name, String qName, Attributes atts) {
		tags.add(qName);
		if (qName.equals("timex2")) {
			Entity timeChain = new Entity();
			this.currentTimeChain = timeChain;
			timeChains.add(timeChain);
		}
		if (qName.equals("timex2_mention")) {
			EntityMention em = new EntityMention();
			em.extent = "";
			this.currentTimeMention = em;
			this.currentTimeChain.addMention(em);
			timeMention = true;
		}
		if (qName.equals("charseq") && timeMention) {
			int start = Integer.valueOf(atts.getValue("START"));
			int end = Integer.valueOf(atts.getValue("END"));
			this.currentTimeMention.start = start;
			this.currentTimeMention.end = end;
			this.currentTimeMention.headStart = start;
			this.currentTimeMention.headEnd = end;
		}
	}

	public void characters(char ch[], int start, int length) throws SAXException {
		if(this.timeMention) {
			String str = (new String(ch, start, length));
			this.currentTimeMention.extent += str;
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(tags.get(tags.size()-1).equals("timex2_mention")) {
			this.timeMention = false;
		}
		tags.remove(tags.size() - 1);
	}
}