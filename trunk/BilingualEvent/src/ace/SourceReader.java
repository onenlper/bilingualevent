package ace;

import java.util.ArrayList;

import model.ACEDoc;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SourceReader extends DefaultHandler {
	ACEDoc document;

	ArrayList<String> tags = new ArrayList<String>();

	public SourceReader(ACEDoc sgm) {
		this.document = sgm;
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {
		tags.add(qName);
		if (qName.equalsIgnoreCase("TEXT")) {
			document.start = document.content.length();
		}
	}

	public void characters(char ch[], int start, int length) throws SAXException {
		String topTag = tags.get(tags.size() - 1);
		String str = new String(ch, start, length);
		
		str = str.replace("•", "&#8226;");
		if (topTag.equalsIgnoreCase("DOCID")) {
			document.content += str;
		} else {
			if (str.endsWith(" ")) {
//				str = str.substring(0, str.length() - 1) + "\r";
			}
			document.content += str;
		}
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		tags.remove(tags.size() - 1);
		if (qName.equalsIgnoreCase("TEXT")) {
			document.end = document.content.length() - 1;
		}
	}
}