package coref;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEDoc;
import model.EventMention;
import util.Common;

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
		if(ant.isFake) {
//			feas[id++] = -anaphor.head.hashCode();
//			return getContext(feas);
		}
		feas[id++] = isExactMatch(ant, anaphor, doc);
		return getContext(feas);
	}
	

	private static short getMentionDiss(int diss) {
		if(diss==0) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short getIsFake(EventMention ant, EventMention anaphor, ACEDoc doc) {
		if (ant.isFake) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short getDistance(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		int diss = 0;
		if (ant.isFake) {
			diss = doc.positionMap.get(ant.getAnchorEnd())[0];
		} else {
			diss = doc.positionMap.get(anaphor.getAnchorEnd())[0];
		}
		return (short) (Math.log(diss) / Math.log(2));
	}

	private static short isExactMatch(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		if (ant.getAnchor().equalsIgnoreCase(anaphor.getAnchor())) {
			return 1;
		} else {
			return 0;
		}
	}

	public static String message;

}
