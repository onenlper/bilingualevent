package coref;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEDoc;
import model.Entity;
import model.EventMention;
import model.EventMentionArgument;
import event.supercoref.EventCorefFea;

public class Context implements Serializable {

	/**
         * 
         */
	private static final long serialVersionUID = 1L;

	public String feaL;

	public static HashMap<String, Context> contextCache = new HashMap<String, Context>();

	public static boolean coref = false;
	
	public static boolean gM1 = false;
	public static boolean gM2 = false;
	
	public static Context getContext(int[] feas) {
		// long feaL = 0;
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < feas.length; i++) {
			// if (feas[i] >= 10) {
			// Common.bangErrorPOS("Can't larger than 10:" + feas[i]
			// + "  Fea:" + i);
			// }
			// feaL += Math.pow(10, i) * feas[i];
			sb.append(feas[i]).append("#");
		}
		if (contextCache.containsKey(sb.toString())) {
			return contextCache.get(sb.toString());
		} else {
			Context c = new Context(sb.toString());
			contextCache.put(sb.toString(), c);
			return c;
		}
	}

	private Context(String feaL) {
		this.feaL = feaL;
	}

	public int hashCode() {
		return this.toString().hashCode();
	}

	public boolean equals(Object obj) {
		Context c2 = (Context) obj;
		return (this.feaL == c2.feaL);
	}

	public String toString() {
		return this.feaL;
	}

	static short[] feas = new short[18];

	public static HashSet<String> todo = new HashSet<String>();
	
	public static Context buildContext(EventMention ant, EventMention anaphor,
			ACEDoc doc, ArrayList<EventMention> allCands, int mentionDis) {
		int id = 0;
		int[] feas = new int[10];
		feas[id++] = getMentionDiss(mentionDis);
		feas[id++] = isExactMatch(ant, anaphor, doc);
//		feas[id++] = getDistance(ant, anaphor, doc);
//		feas[id++] = conflictArg(ant, anaphor, doc);
//		feas[id++] = getSimi(ant, anaphor, doc);
//		feas[id++] = conflictPlaceTime(ant, anaphor, doc);
//		feas[id++] = conflictCorefArg(ant, anaphor, doc);
//		feas[id++] = corefArg(ant, anaphor, doc);
		return getContext(feas);
	}
	
	private static short corefArg(EventMention ant, EventMention anaphor, ACEDoc doc) {
		if(ant.isFake()) {
			return 0;
		}
		int overlap_num = 0;
		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant
					.getEventMentionArguments()) {
				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
				Entity entity2 = doc.entityCorefMap.get(arg2Name);
				String role2 = arg2.getRole();

				if (role1.equals(role2)) {
					if (entity1 == null && entity2 == null
							&& arg1.getExtent().equals(arg2.getExtent())) {
						overlap_num += 1;
					} else if (entity1 != null && entity2 != null
							&& entity1 == entity2) {
						overlap_num += 1;
					}
				}
			}
		}
		if(overlap_num==0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	private static short getSimi(EventMention ant, EventMention anaphor, ACEDoc doc) {
		if(ant.isFake()) {
			return 0;
		}
		String eToken = doc.getWord(ant.getAnchorStart());
		String anaToken = doc.getWord(anaphor.getAnchorStart());
		double sim = EventCorefFea.getSimi(eToken.toLowerCase(), anaToken.toLowerCase());
		return (short) (sim/1.0);
	}
	
	private static short conflictArg(EventMention ant, EventMention anaphor, ACEDoc doc) {
		int coref_num = 0;
		if(ant.isFake()) {
			return 0;
		}
		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant
					.getEventMentionArguments()) {
				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
				Entity entity2 = doc.entityCorefMap.get(arg2Name);
				String role2 = arg2.getRole();

				if (!role1.equals(role2)) {
					if (entity1 == null && entity2 == null
							&& arg1.getExtent().equals(arg2.getExtent())) {
						coref_num += 1;
					} else if (entity1 != null && entity2 != null
							&& entity1 == entity2) {
						coref_num += 1;
					}
				}
			}
		}
		if(coref_num==0) {
			return 0;
		} else {
			return 1;
		}
	}
	
	private static short conflictCorefArg(EventMention ant, EventMention anaphor, ACEDoc doc) {
		if(ant.isFake()) {
			return 0;
		}
		short coref_num = 0;
		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant
					.getEventMentionArguments()) {
				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
				Entity entity2 = doc.entityCorefMap.get(arg2Name);
				String role2 = arg2.getRole();

				if (!role1.equals(role2)) {
					if (entity1 == null && entity2 == null
							&& arg1.getExtent().equals(arg2.getExtent())) {
						coref_num += 1;
					} else if (entity1 != null && entity2 != null
							&& entity1 == entity2) {
						coref_num += 1;
					}
				}
			}
		}
		if(coref_num!=0) {
			return 1;
		} else {
			return 0;
		}
	}
	
	private static short conflictPlaceTime(EventMention ant, EventMention anaphor, ACEDoc doc) {
		if(ant.isFake()) {
			return 1;
		}
		boolean time_conflict = false;
		boolean place_conflict = false;
		
		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant
					.getEventMentionArguments()) {
				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
				Entity entity2 = doc.entityCorefMap.get(arg2Name);
				String role2 = arg2.getRole();

				if (role1.equals(role2) && role1.equals("Time-Within")) {
					if (entity1 == null && entity2 == null
							&& arg1.getExtent().equals(arg2.getExtent())) {
						
					} else if (entity1 != null && entity2 != null
							&& entity1 == entity2) {
						
					} else {
						time_conflict = true;
					}
				}
				
				if (role1.equals(role2) && role1.equals("Place")) {
					if (entity1 == null && entity2 == null
							&& arg1.getExtent().equals(arg2.getExtent())) {
						
					} else if (entity1 != null && entity2 != null
							&& entity1 == entity2) {
						
					} else {
						place_conflict = true;
					}
				}
			}
		}
		if(time_conflict || place_conflict) {
			return 0;
		} else {
			return 1;
		}
	}
	
	
	private static short getMentionDiss(int diss) {
		if(diss==0) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short getIsFake(EventMention ant, EventMention anaphor, ACEDoc doc) {
		if (ant.isFake()) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short getDistance(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		int diss = 0;
		if (ant.isFake()) {
			diss = doc.positionMap.get(anaphor.getAnchorStart())[0];
		} else {
			diss = doc.positionMap.get(anaphor.getAnchorStart())[0] - doc.positionMap.get(ant.getAnchorStart())[0];
		}
		return (short) (Math.log(diss) / Math.log(2));
	}

	private static short isExactMatch(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		if(ant.isFake()) {
			return 0;
		}
		if(ant.getAnchor().equalsIgnoreCase(anaphor.getAnchor())) {
			return 1;
		} else {
			return 0;
		}
//		int p1[] = doc.positionMap.get(ant.getAnchorStart());
//		int p2[] = doc.positionMap.get(anaphor.getAnchorStart());
//		
//		String lemma1 = doc.parseReults.get(p1[0]).lemmas.get(p1[1]);
//		String lemma2 = doc.parseReults.get(p2[0]).lemmas.get(p2[1]);
//		
//		if (ant.getAnchor().equalsIgnoreCase(anaphor.getAnchor()) != lemma1.equalsIgnoreCase(lemma2)) {
////			Common.pause(":!!!");
//		}
//		
////		if (ant.getAnchor().equalsIgnoreCase(anaphor.getAnchor())) {
//		if(lemma1.equalsIgnoreCase(lemma2)) {
//			return 1;
//		} else {
//			return 0;
//		}
	}

	public static String message;

}
