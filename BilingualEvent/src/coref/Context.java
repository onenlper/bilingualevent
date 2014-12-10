package coref;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import model.ACEDoc;
import model.Entity;
import model.EventMention;
import model.EventMentionArgument;
import model.syntaxTree.MyTreeNode;
import util.CollectNegativeContext;
import util.Common;
import util.Util;
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

	private static ArrayList<int[]> subContext;

	public static ArrayList<Integer> normConstant = new ArrayList<Integer>();

	public static ArrayList<int[]> getSubContext() {
		if (subContext != null) {
			return subContext;
		}
		subContext = new ArrayList<int[]>();
		int[] a = { 0, 1 };
		subContext.add(a);
		
		int[] b = { 1, 2 };
		subContext.add(b);
		
		int[] c = { 2, 3 };
		subContext.add(c);

		int[] d = { 3, 4 };
		subContext.add(d);

		int[] e = { 4, 5 };
		subContext.add(e);
		
		int[] f = { 5, 6 };
		subContext.add(f);
		
		normConstant.add(2);
		normConstant.add(2);
		normConstant.add(2);
		normConstant.add(2);
		normConstant.add(2);
		normConstant.add((int) (bins.length));
		
//		int[] f = { 0, 6 };
//		subContext.add(f);
//		normConstant.add(2*2*2*2*2*4);

//		normConstant.add((int) cap);
//		 int[] g = {6, 7};
//		 subContext.add(g);
//		 normConstant.add(2);

		return subContext;
	}

	static short sentCap = 10;

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

	public String getKey(int i) {
		// return this.toString().substring(subContext.get(i)[0],
		// subContext.get(i)[1]);
		String tks[] = this.toString().split("#");
		StringBuilder sb = new StringBuilder();
		for (int m = subContext.get(i)[0]; m < subContext.get(i)[1]; m++) {
			sb.append(tks[m]).append("#");
		}
		return sb.toString();
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

	public static HashMap<String, Double> simi = Common
			.readFile2Map5("trPair.simi");

	public static Context buildContext(EventMention ant, EventMention anaphor,
			ACEDoc doc, ArrayList<EventMention> allCands, int mentionDis) {

		// String pair = ant.getAnchor() + " " + anaphor.getAnchor();
		// double sim = simi.get(pair);

		// System.out.println(ant.getAnchor() + " " + anaphor.getAnchor());
		if (ant.getAnchor().isEmpty() || anaphor.getAnchor().isEmpty()) {
			Common.bangErrorPOS("!!!");
		}

		int id = 0;
		int[] feas = new int[10];

		feas[id++] = isExactMatch(ant, anaphor, doc);
		feas[id++] = isConflictACERole(ant, anaphor);
		feas[id++] = isConflictNumber(ant, anaphor);
		feas[id++] = isConflictValueArgument(ant, anaphor);
		feas[id++] = conflictArg_(ant, anaphor);
		feas[id++] = getEvDis(ant, anaphor);

		
//		feas[id++] = inNegativeContext(ant, anaphor, doc);
		// feas[id++] = getSentDis(ant, anaphor, doc);
		// if(ant.isFake() || sim>0.8) {
		// feas[id++] = 0;
		// } else {
		// feas[id++] = 1;
		// }

		// feas[id++] = compareArgs(ant, anaphor);
		// feas[id++] = getDistance(ant, anaphor, doc);
		// feas[id++] = highPrec(ant, anaphor, doc);
		// feas[id++] = isSameBV(ant, anaphor);
		// feas[id++] = getMentionDiss(mentionDis);

		//
		// feas[id++] = conflictArg(ant, anaphor, doc);
		// feas[id++] = getSimi(ant, anaphor, doc);
		// feas[id++] = conflictPlaceTime(ant, anaphor, doc);
		// feas[id++] = conflictCorefArg(ant, anaphor, doc);
		// feas[id++] = corefArg(ant, anaphor, doc);
		return getContext(feas);
	}

	public static HashSet<String> negative = Common.readFile2Set("negative");
	public static HashSet<String> negativeRight = Common.readFile2Set("negativeRight");
	
	public static short inNegativeContext(EventMention ant, EventMention em,
			ACEDoc doc) {
		if (ant.isFake()) {
			return 0;
		}

		EventMention e1 = ant;
		EventMention e2 = em;

		MyTreeNode node1 = doc.getTreeNode(e1.getAnchorStart());
		MyTreeNode clause1 = CollectNegativeContext.lowestClause(node1, doc);

		HashMap<String, ArrayList<String>> left1 = CollectNegativeContext
				.getLeftWords(clause1, node1);
		HashMap<String, ArrayList<String>> right1 = CollectNegativeContext
				.getRightWords(clause1, node1);

		MyTreeNode node2 = doc.getTreeNode(e2.getAnchorStart());
		MyTreeNode clause2 = CollectNegativeContext.lowestClause(node2, doc);

		HashMap<String, ArrayList<String>> left2 = CollectNegativeContext
				.getLeftWords(clause2, node2);
		HashMap<String, ArrayList<String>> right2 = CollectNegativeContext
				.getRightWords(clause2, node2);

		for(String key : left1.keySet()) {
			if(left2.containsKey(key)) {
				for(String s1 : left1.get(key)) {
					for(String s2 : left2.get(key)) {
						
						String k = "";
						if(s1.compareTo(s2)<0) {
							k = s1 + "#" + s2; 
						} else {
							k = s2 + "#" + s1;
						}
						if(negative.contains(k)) {
							if(coref)
							System.out.println(k);
							return 1;
						}
						
					}
				}
				
			}
		}
		
		for(String key : right1.keySet()) {
			if(right2.containsKey(key)) {
				for(String s1 : right1.get(key)) {
					for(String s2 : right2.get(key)) {
						
						String k = "";
						if(s1.compareTo(s2)<0) {
							k = s1 + "#" + s2; 
						} else {
							k = s2 + "#" + s1;
						}
						if(negativeRight.contains(k)) {
							if(coref)
							System.out.println(k);
							return 1;
						}
						
					}
				}
				
			}
		}
		
		return 0;
	}

	private static short compareArgs(EventMention ant, EventMention em) {
		if (ant.isFake()
				|| ant.getEventMentionArguments().size() == em
						.getEventMentionArguments().size()) {
			return 1;
		} else if (ant.getEventMentionArguments().size() >= em
				.getEventMentionArguments().size()) {
			return 1;
		} else {
			return 2;
		}
	}

	private static short isSameBV(EventMention ant, EventMention em) {
		if (ant.isFake()) {
			return 0;
		}
		if (Util._commonBV_(ant, em)) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short isConflictACERole(EventMention ant, EventMention em) {
		if (ant.isFake()) {
			return 1;
		}
		if (Util._conflictACERoleSemantic_(ant, em)) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short isConflictNumber(EventMention ant, EventMention em) {
		if (ant.isFake()) {
			return 1;
		}
		if (Util._conflictNumber_(ant, em)) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short isConflictValueArgument(EventMention ant,
			EventMention em) {
		if (ant.isFake()) {
			return 1;
		}
		if (Util._conflictValueArgument_(ant, em)
		// || corefDiffRole(ant, em)
		// || extraRole(ant, em)
		// || diffNum(ant, em)
//			|| Util._conflictTimeArgument_(ant, em)	
		) {
			return 0;
		} else {
			return 1;
		}
	}

	public static boolean diffNum(EventMention ant, EventMention em) {
		for (String role : em.argHash.keySet()) {
			if (ant.argHash.containsKey(role)
					&& ant.argHash.get(role).size() != em.argHash.get(role)
							.size()) {
				return true;
			}
		}
		return false;
	}

	public static boolean extraRole(EventMention ant, EventMention em) {
		if (ant.getEventMentionArguments().size() > 1
				&& em.getEventMentionArguments().size() > 1) {
			HashSet<String> roles1 = new HashSet<String>(ant.argHash.keySet());
			HashSet<String> roles2 = new HashSet<String>(em.argHash.keySet());
			boolean match = false;
			for (String r1 : roles1) {
				for (String r2 : roles2) {
					if (r1.equals(r2)) {
						match = true;
					}
				}
			}
			if (!match) {
				return true;
			}
		}
		return false;
	}

	private static boolean corefDiffRole(EventMention ant, EventMention em) {
		for (EventMentionArgument arg1 : ant.getEventMentionArguments()) {
			for (EventMentionArgument arg2 : em.getEventMentionArguments()) {
				if (!arg1.role.equals(arg2.role)) {
					if (arg1.mention.entity == arg2.mention.entity
							&& arg1.mention.entity != null
							&& arg2.mention.entity != null) {

						System.out.println(ant.getAnchor());
						for (EventMentionArgument arg : ant
								.getEventMentionArguments()) {
							System.out.println(arg.getRole() + " # "
									+ arg.mention.head);
						}
						System.out.println("---");
						System.out.println(em.getAnchor());
						for (EventMentionArgument arg : em
								.getEventMentionArguments()) {
							System.out.println(arg.getRole() + " # "
									+ arg.mention.head);
						}
						System.out.println("=====================");

						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean conflictTime(EventMention ant, EventMention em) {
		for (String role : em.argHash.keySet()) {
			if (!role.equals("Time-Within")) {
				continue;
			}
			String t1 = em.argHash.get("Time-Within").get(0).mention.head;

			if (ant.argHash.containsKey(role)) {
				String t2 = ant.argHash.get("Time-Within").get(0).mention.head;
				if (!t1.contains(t2) || !t2.contains(t1))
					return true;
			}
		}
		return false;
	}

	public static List<String> roles = Arrays.asList("Crime", "Victim",
			"Origin", "Adjudicator", "Time-Holds", "Time-Before", "Target",
			"Time-At-End", "Org", "Recipient", "Vehicle", "Plaintiff",
			"Attacker", "Place", "Buyer", "Money", "Giver", "Beneficiary",
			"Agent", "Time-Ending", "Time-After", "Time-Starting", "Seller",
			"Defendant", "Time-Within", "Artifact", "Time-At-Beginning",
			"Prosecutor", "Sentence", "Price", "Position", "Instrument",
			"Destination", "Person", "Entity", "null");

	private static short conflictArg_(EventMention ant, EventMention em) {
		if (ant.isFake()) {
			return 1;
		}
		List<String> discreteRoles = new ArrayList<String>(Arrays.asList(
				"Place", "Org", "Position", "Adjudicator", "Origin", "Giver",
				"Recipient", "Defendant",
				"Agent",
				"Person"
//				"Prosecutor"
				));
		
//		discreteRoles = Arrays.asList(
//				"Place", "Org", "Position", "Adjudicator", "Origin", "Giver", 
//				"Recipient", "Defendant", 
//
//				"Victim",
//				,
////				"Plaintiff",
//				"Attacker", "Buyer", "Beneficiary",
//				"Agent", "Seller",
//				"Artifact",
//				"Destination",
//				"Person",
//				"Entity");
		for (String role : discreteRoles) {
			if (Util.conflictArg_(ant, em, role)) {
				return 0;
			}
		}
		return 1;
	}

	public static boolean highPrecissionNegativeConstraint(EventMention ant,
			EventMention em) {
		if (Util._conflictSubType_(ant, em)) {
			return true;
		}
		if (Util._conflictACERoleSemantic_(ant, em)) {
			return true;
		}
		if (Util._conflictNumber_(ant, em)) {
			return true;
		}
		if (Util._conflictValueArgument_(ant, em)) {
			return true;
		}
		ArrayList<String> discreteRoles = new ArrayList<String>(Arrays.asList(
				"Place", "Org", "Position", "Adjudicator", "Origin", "Giver",
				"Recipient", "Defendant"));
		for (String role : discreteRoles) {
			if (Util.conflictArg_(ant, em, role)) {
				return true;
			}
		}
		return false;
	}

	private static short highPrec(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		if (ant.isFake()) {
			return 0;
		}
		if (Util.highPrecissionNegativeConstraint(ant, anaphor)) {
			return 1;
		} else {
			return 0;
		}
	}

	private static short corefArg(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		if (ant.isFake()) {
			return 0;
		}
		int overlap_num = 0;
		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant.getEventMentionArguments()) {
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
		if (overlap_num == 0) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short getSimi(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		if (ant.isFake()) {
			return 0;
		}
		String eToken = doc.getWord(ant.getAnchorStart());
		String anaToken = doc.getWord(anaphor.getAnchorStart());
		double sim = EventCorefFea.getSimi(eToken.toLowerCase(),
				anaToken.toLowerCase());
		return (short) (sim / 1.0);
	}

	private static short conflictArg(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		int coref_num = 0;
		if (ant.isFake()) {
			return 0;
		}
		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant.getEventMentionArguments()) {
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
		if (coref_num == 0) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short conflictCorefArg(EventMention ant,
			EventMention anaphor, ACEDoc doc) {
		if (ant.isFake()) {
			return 0;
		}
		short coref_num = 0;
		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant.getEventMentionArguments()) {
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
		if (coref_num != 0) {
			return 1;
		} else {
			return 0;
		}
	}

	private static short conflictPlaceTime(EventMention ant,
			EventMention anaphor, ACEDoc doc) {
		if (ant.isFake()) {
			return 1;
		}
		boolean time_conflict = false;
		boolean place_conflict = false;

		for (EventMentionArgument arg1 : anaphor.getEventMentionArguments()) {
			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
			Entity entity1 = doc.entityCorefMap.get(arg1Name);
			String role1 = arg1.getRole();

			for (EventMentionArgument arg2 : ant.getEventMentionArguments()) {
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
		if (time_conflict || place_conflict) {
			return 0;
		} else {
			return 1;
		}
	}

	private static short getMentionDiss(int diss) {
		if (diss == 0) {
			return 0;
		} else if (diss == 1) {
			return 1;
		} else {
			return 2;
		}
	}

	private static short getIsFake(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		if (ant.isFake()) {
			return 0;
		} else {
			return 1;
		}
	}

	static short cap = 20;

	static int[] bins = {7, 10, 11, 14};
	
	public static double[] probs = {.0, .5, .0, .0, .0};
	
//	static int[] bins = {7, 10, 11, 14};
	
	public static short getEvDis(EventMention ant, EventMention anaphor) {
		// if(ant.isFake()) {
		// return 1;
		// }
		short dis = (short) (anaphor.sequenceID - ant.sequenceID);
		short ret = (short) bins.length;
		for(int i =0;i<bins.length;i++) {
			int bin = bins[i];
			if(dis<bin) {
				ret = (short) i;
				break;
			}
		}
		return ret;
	}

	private static short getSentDis(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		// if(ant.isFake()) {
		// return 1;
		// }
		short d1 = 0;
		if (!ant.isFake()) {
			d1 = (short) doc.positionMap.get(ant.getAnchorStart())[0];
		}
		short d2 = (short) doc.positionMap.get(anaphor.getAnchorStart())[1];

		short dis = (short) (d2 - d1);
		if (dis >= sentCap) {
			dis = sentCap;
		}
		return (short) (dis / 40);
	}

	private static short getDistance(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		int diss = 0;
		if (ant.isFake()) {
			diss = doc.positionMap.get(anaphor.getAnchorStart())[0];
		} else {
			diss = doc.positionMap.get(anaphor.getAnchorStart())[0]
					- doc.positionMap.get(ant.getAnchorStart())[0];
		}
		return (short) (Math.log(diss) / Math.log(2));
	}

	static ArrayList<HashSet<String>> clusters = null;

	private static short isExactMatch(EventMention ant, EventMention anaphor,
			ACEDoc doc) {
		if (ant.isFake()) {
			return 1;
		}
		String pair = ant.getAnchor() + " " + anaphor.getAnchor();
		double sim = 0;
		if (!simi.containsKey(pair)) {
			todo.add(pair);
			sim = 0;
			Common.bangErrorPOS("");
		} else
		
		sim = simi.get(pair);

		boolean sameBV = false;
		for (String bv1 : ant.bvs.keySet()) {
			for (String bv2 : anaphor.bvs.keySet()) {
				if (bv1.equals(bv2)) {
					sameBV = true;
				}
			}
		}
		
		if (ant.getAnchor().equalsIgnoreCase(anaphor.getAnchor())
				|| (
						sameBV 
						&& isBVConflict(ant, anaphor, doc)==1
						&& sim > 0.30
						)
				|| sim > 0.85
				) {
			return 1;
		} else {
			return 0;
		}
	}
	
	private static short isBVConflict(EventMention ant, EventMention anaphor, ACEDoc doc) {
		if(ant.isFake()) {
			return 1;
		}
	
		if (ant.getAnchor().equals(anaphor.getAnchor())) {
			return 1;
		}
//		
//		double sim = 0;
//		String pair = ant.getAnchor() + " " + anaphor.getAnchor();
//		if (!simi.containsKey(pair)) {
//			todo.add(pair);
//			sim = 0;
//			Common.bangErrorPOS("");
//		} else
//			sim = simi.get(pair);
//		if(sim>.85) {
//			return 1;
//		}
		boolean sameBV = false;
		for (String bv1 : anaphor.bvs.keySet()) {
			String pattern1 = anaphor.bvs.get(bv1);
			int idx1 = anaphor.getAnchor().indexOf(bv1);
			for (String bv2 : ant.bvs.keySet()) {
				String pattern2 = ant.bvs.get(bv2);
				int idx2 = ant.getAnchor().indexOf(bv2);
				if (bv1.equals(bv2)) {
					sameBV = true;
					if (idx1 != idx2 && ant.getAnchor().length() != 1 && anaphor.getAnchor().length() != 1) {
//						开枪 # 离开
//						System.out.println(anaphor.getAnchor() + " # " + ant.getAnchor());
						return 0;
					}
					if (pattern1.equals(pattern2) &&
							(
									pattern1.equals("verb_BV") || pattern1.equals("BV_verb")
									|| pattern1.equals("adj_BV")
									)) {
//						搜捕 # 逮捕
//						轻伤 # 重伤
//						System.out.println(anaphor.getAnchor() + " # " + ant.getAnchor());
						return 0;
					}
				}
			}
		}
		return 1;
	}
	
	public static String message;

}
