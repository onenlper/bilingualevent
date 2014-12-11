package coref;

import java.io.Serializable;
import java.util.ArrayList;

import model.ACEDoc;
import model.EventMention;

public class ResolveGroup implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

//	String pronoun;
	
	String anaphor;
	String head;
	ArrayList<Entry> entries;

	String sem = "unknown";
	String cilin = "null";
	ArrayList<EventMention> ants;
	String anaphorName;
	ACEDoc doc;
	EventMention m;
	
	public ResolveGroup(EventMention em, ACEDoc doc, ArrayList<EventMention> ants) {
		this.doc = doc;
		this.ants = ants;
		this.m = em;
		this.anaphor = m.extent;
		this.entries = new ArrayList<Entry>();
		
		this.anaphorName = doc.fileID + ":" + m.toName();
	}

	public static class Entry implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Context context;
		String head;
		
		
		String antName;
		
		String sem = "unknown";
		
		public static double p_fake_decay = 1;
		
		String cilin = "null";
		
		boolean isFake = false;
		double p;
		EventMention ant;
		double p_c;
		int seq;
		
		public Entry(EventMention ant, Context context, ACEDoc doc) {
			this.ant = ant;
			if(ant.isFake()) {
				this.antName = "fake";
			} else {
				this.antName = doc.fileID + ":" + ant.toName();
			}
			this.context = context;
			this.isFake = ant.isFake();
		}
	}
}
