package coref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EntityMention;
import model.EntityMention.Numb;
import model.EventChain;
import model.EventMention;
import model.EventMentionArgument;
import util.Common;
import util.Util;

public class HeursiticBase {

	public static void main(String args[]) {
		if(args.length!=1) {
			System.out.println("java ~ part");
			System.exit(1);
		}
		long t1 = System.currentTimeMillis();
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);
		ArrayList<ArrayList<EventChain>> systemEventChains = new ArrayList<ArrayList<EventChain>>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		ArrayList<String> fileNames = new ArrayList<String>();
		ArrayList<ArrayList<EventChain>> goldEventChains = new ArrayList<ArrayList<EventChain>>();
		
		Util.part = args[0];
		
		for(int i=0;i<files.size();i++) {
			String file = files.get(i);
			
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = i;
			
			fileNames.add(doc.fileID);
			goldEventChains.add(doc.goldEventChains);
			
			lengths.add(doc.content.length());
			
//			ArrayList<EventMention> events = doc.goldEventMentions;
			ArrayList<EventMention> events = Util.loadSystemComponents(doc);
			
			Collections.sort(events);
			
			ArrayList<EventMention> candidates = new ArrayList<EventMention>();
			for (EventMention m : events) {
				candidates.add(m);
			}
			Collections.sort(candidates);
			ArrayList<EventMention> anaphors = new ArrayList<EventMention>();
			for (EventMention m : events) {
				anaphors.add(m);
			}
			Collections.sort(anaphors);
			
			findAntecedent(doc, anaphors, candidates);

			ArrayList<EventChain> systemChain = new ArrayList<EventChain>();
			HashMap<String, EventChain> eventChainMap = new HashMap<String, EventChain>();
			
			for (EventMention m : candidates) {
				EventChain ec = null;
				if (m.antecedent == null) {
					ec = new EventChain();
					systemChain.add(ec);
				} else {
					ec = eventChainMap.get(m.antecedent.toName());
				}
				ec.addEventMention(m);
				eventChainMap.put(m.toName(), ec);
			}
			systemEventChains.add(systemChain);
		}
		try {
			ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys." + Util.part , goldEventChains);
			ToSemEval.outputSemFormat(fileNames, lengths, "base.keys." + Util.part, systemEventChains);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long t2 = System.currentTimeMillis();
//		System.out.println(t2 - t1);
	}

	private static void findAntecedent(ACEDoc doc,
			ArrayList<EventMention> anaphors,
			ArrayList<EventMention> allCandidates) {
		for (EventMention anaphor : anaphors) {
			ArrayList<EventMention> cands = new ArrayList<EventMention>();
			for (int h = allCandidates.size() - 1; h >= 0; h--) {
				EventMention cand = allCandidates.get(h);
				if (cand.getAnchorEnd() < anaphor.getAnchorStart()) {
					cands.add(cand);
				}
			}
			
			for(int i=0;i<cands.size();i++) {
				EventMention cand = cands.get(i);

				if(
						!highPrecissionNegativeConstraint(cand, anaphor, doc) && 
						(cand.getAnchor().equalsIgnoreCase(anaphor.getAnchor()) ||
								_commonBV_(cand, anaphor, doc))
						) {
				
//				if(
//						cand.getAnchor().equalsIgnoreCase(anaphor.getAnchor())
////						doc.getWord(cand.getAnchorStart()).equalsIgnoreCase(doc.getWord(anaphor.getAnchorStart()))
////						&&
////						cand.getSubType().equals(anaphor.getSubType()) 
////						&& cand.tense.equals(anaphor.tense) 
////						&& cand.modality.equals(anaphor.modality) && cand.genericity.equals(anaphor.genericity) 
////						&& cand.polarity.equals(anaphor.polarity)
//						) {
					anaphor.antecedent = cand;
					break;
				}
			}
		}
	}
	
	public static boolean _commonBV_(EventMention ant, EventMention em, ACEDoc doc) {
		if (ant.getAnchor().equals(em.getAnchor())) {
			return false;
		}
		// common character
		boolean common = false;
		loop: for (int i = 0; i < ant.getAnchor().length(); i++) {
			for (int j = 0; j < em.getAnchor().length(); j++) {
				if (ant.getAnchor().charAt(i) == em.getAnchor().charAt(j)) {
					common = true;
					break loop;
				}
			}
		}
		// same meaning
		/*
		 * // String[] sem1 = Common.getSemantic(em.head); // String[] sem2 =
		 * Common.getSemantic(an.head); // if(sem1!=null && sem2!=null) { //
		 * for(String s1 : sem1) { // for(String s2 : sem2) { //
		 * if(s1.equals(s2) && s1.endsWith("=")) { // return true; // } // } //
		 * } // }
		 */

		if (common) {
			if (!conflictBV(ant, em, doc))
				return true;
//			} else {
				// EventMention gEM =
				// RuleCoref.goldEventMentionMap.get(em.toString());
				// EventMention gAn =
				// RuleCoref.goldEventMentionMap.get(an.toString());
				// if (gEM != null && gAn != null && gEM.goldChainID ==
				// gAn.goldChainID) {
				// RuleCoref.printPair(em, an);
				// }
//			}
		}
		return false;
	}
	
	public static boolean conflictBV(EventMention em, EventMention an, ACEDoc doc) {
		if (em.getAnchor().equals(an.getAnchor())) {
			return false;
		}
		for (String bv1 : em.bvs.keySet()) {
			String pattern1 = em.bvs.get(bv1);
			int idx1 = em.getAnchor().indexOf(bv1);
			for (String bv2 : an.bvs.keySet()) {
				String pattern2 = an.bvs.get(bv2);
				int idx2 = an.getAnchor().indexOf(bv2);
				if (bv1.equals(bv2)) {
					if (idx1 != idx2 && em.getAnchor().length() != 1 && an.getAnchor().length() != 1) {
						return true;
					}
					if (pattern1.equals(pattern2) && (pattern1.equals("verb_BV") || pattern2.equals("BV_verb"))) {
						return true;
					}

					if (pattern1.equals(pattern2) && pattern1.equals("adj_BV")) {
						return true;
					}

					if ((pattern1.equals("adj_BV#BV") && pattern1.equals("BV"))
							|| (pattern1.equals("BV") && pattern1.equals("adj_BV#BV"))) {
						return true;
					}
					return false;
				}
			}
		}
		return true;
	}
	
	public static boolean highPrecissionNegativeConstraint(EventMention ant, EventMention em, ACEDoc doc) {
		if (_conflictSubType_(ant, em, doc)) {
			return true;
		}
		if (_conflictACERoleSemantic_(ant, em, doc)) {
			return true;
		}
		if (_conflictNumber_(ant, em, doc)) {
			return true;
		}		
		if (_conflictValueArgument_(ant, em, doc)) {
			return true;
		}
		ArrayList<String> discreteRoles = new ArrayList<String>(Arrays.asList("Place", "Org", "Position",
				"Adjudicator", "Origin", "Giver", "Recipient", "Defendant"));
		for (String role : discreteRoles) {
			if (conflictArg_(ant, em, doc, role)) {
				return true;
			}
		}
		
//		if (_conflictOverlap_(ant, em, doc)) {
//			System.out.println("Conflict Overlap: " + ant.getAnchor() + "#" + em.getAnchor());
//			return true;
//		}
//		if (_conflictPersonArgument_(ant, em, doc)) {
//			return true;
//		}
//		if (_conflictDestination_(ant, em, doc)) {
//			return true;
//		}
		
//		if(_conflictModify_(ant, em, doc)) {
//			return true;
//		}
		
		return false;
	}
	
	private static boolean conflictArg_(EventMention ant, EventMention em, ACEDoc doc, String role) {
		if (em.argHash.containsKey(role) && ant.argHash.containsKey(role)) {
			boolean conflict = false;
			for (EventMentionArgument arg1 : em.argHash.get(role)) {
				EntityMention m1 = arg1.mention;
				for (EventMentionArgument arg2 : ant.argHash.get(role)) {
					EntityMention m2 = arg2.mention;
					if (m1.entity != m2.entity) {
						conflict = true;
					} else {
						return false;
					}
				}
			}
			return conflict;
		}
		return false;
	}
	
	public static boolean _conflictSubType_(EventMention ant, EventMention em, ACEDoc doc) {
		boolean conflict = !em.subType.equals(ant.subType) && !em.getAnchor().equals(ant.getAnchor());
		return conflict;
	}
	
	public static boolean _conflictOverlap_(EventMention ant, EventMention em, ACEDoc doc) {
		return ant.getAnchorEnd() >= em.getAnchorStart();
	}
	
	public static boolean _conflictNumber_(EventMention ant, EventMention em, ACEDoc doc) {
		return em.number != ant.number;
	}
	
	public static boolean _conflictPersonArgument_(EventMention ant, EventMention em, ACEDoc doc) {
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);

					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					if (!arg1.get(0).mention.semClass.equalsIgnoreCase("per")
							|| !arg2.get(0).mention.semClass.equalsIgnoreCase("per")) {
						continue;
					}

					if (arg1.get(0).mention.entity != arg2.get(0).mention.entity) {
//						if (personCompatible(arg1.get(0).mention, arg2.get(0).mention, part)) {
//						} else {
							conflict = true;
//							break loop;
//						}
					}
				}
			}
		}
		// arg0, arg1
		if (em.srlArgs.containsKey("A0") && ant.srlArgs.containsKey("A0")) {
			EntityMention m1 = em.srlArgs.get("A0").get(0);
			EntityMention m2 = ant.srlArgs.get("A0").get(0);
			if (m1.semClass.equalsIgnoreCase("per") && m2.semClass.equalsIgnoreCase("per")
					&& !personCompatible(m1, m2, doc)) {
				conflict = true;
			}
		}
		if (em.srlArgs.containsKey("A1") && ant.srlArgs.containsKey("A1")) {
			EntityMention m1 = em.srlArgs.get("A1").get(0);
			EntityMention m2 = ant.srlArgs.get("A1").get(0);
			if (m1.semClass.equalsIgnoreCase("per") && m2.semClass.equalsIgnoreCase("per")
					&& !personCompatible(m1, m2, doc)) {
				conflict = true;
			}
		}
		if (conflict) {
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean personCompatible(EntityMention em, EntityMention ant, ACEDoc doc) {
		return em.entity==ant.entity;
//		if (em.gender == Gender.MALE && ant.gender == Gender.FEMALE) {
//			return false;
//		}
//		if (em.gender == Gender.FEMALE && ant.gender == Gender.MALE) {
//			return false;
//		}
//		EntityMention m = getRepresent(em);
//		EntityMention an = getRepresent(ant);
//		if (m.mentionType == MentionType.Proper && an.mentionType == MentionType.Proper && !m.head.equals(an.head)) {
//			return false;
//		}
//		String value1 = "";
//		for (int k=ant.headEnd;k>=0 && k>=ant.headEnd-2;k--) {
//			if (part.getWord(k).posTag.equals("CD")) {
//				value1 = part.getWord(k).word;
//			}
//		}
//		String value2 = "";
//		for (int k=em.headEnd;k>=0 && k>=em.headEnd-2;k--) {
//			if (part.getWord(k).posTag.equals("CD")) {
//				value2 = part.getWord(k).word;
//			}
//		}
//		HashMap<String, Integer> cluster = new HashMap<String, Integer>();
//		cluster.put("3", 3);
//		cluster.put("三", 3);
//		if (!value1.isEmpty()
//				&& !value2.isEmpty()
//				&& !value1.equals(value2)
//				&& !(cluster.containsKey(value1) && cluster.containsKey(value2) && cluster.get(value1) == cluster
//						.get(value2)) && em.ner.equals("CARDINAL") && ant.ner.equals("CARDINAL")) {
//			return false;
//		}
//		return true;
	}
	
	public static boolean _conflictValueArgument_(EventMention ant, EventMention em, ACEDoc doc) {
		boolean conflict = false;
		loop: for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);
					boolean extra1 = false;
					boolean extra2 = false;
					for (EventMentionArgument a1 : arg1) {
						EntityMention m1 = a1.mention;
						if (!m1.semClass.equalsIgnoreCase("value")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a2 : arg2) {
							EntityMention m2 = a2.mention;
							if (!m2.semClass.equalsIgnoreCase("value")) {
								continue;
							}
							if (m2.head.contains(m1.head)) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra1 = true;
							break;
						}
					}

					for (EventMentionArgument a2 : arg2) {
						EntityMention m2 = a2.mention;
						if (!m2.semClass.equalsIgnoreCase("value")) {
							continue;
						}
						boolean extra = true;
						for (EventMentionArgument a1 : arg1) {
							EntityMention m1 = a1.mention;
							if (!m1.semClass.equalsIgnoreCase("value")) {
								continue;
							}
							if (m1.head.contains(m2.head)) {
								extra = false;
								break;
							}
						}
						if (extra) {
							extra2 = true;
							break;
						}
					}
					if (extra1 && extra2) {
						conflict = true;
						break loop;
					}
				}
			}
		}
		if (conflict) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean _conflictDestination_(EventMention ant, EventMention em, ACEDoc doc) {
		if (em.argHash.containsKey("Destination") && ant.argHash.containsKey("Destination")) {
			boolean conflict = false;
			for (EventMentionArgument arg1 : em.argHash.get("Destination")) {
				EntityMention m1 = arg1.mention;
				if (m1.ner.equalsIgnoreCase("other")) {
					continue;
				}
				for (EventMentionArgument arg2 : ant.argHash.get("Destination")) {
					EntityMention m2 = arg2.mention;
					if (m2.ner.equalsIgnoreCase("other")) {
						continue;
					}
					if (m1.entity != m2.entity) {
						conflict = true;
					} else {
						return false;
					}
				}
			}
			return conflict;
		}
		return false;
	}
	
	public static boolean _conflictModify_(EventMention ant, EventMention em, ACEDoc doc) {
		if (!em.modifyList.containsAll(ant.modifyList) && !ant.modifyList.containsAll(em.modifyList)) {
			return true;
		}
		return false;
	}
	
	public static boolean _conflictACERoleSemantic_(EventMention ant, EventMention em, ACEDoc doc) {
		boolean conflict = false;
		for (String role1 : em.argHash.keySet()) {
			for (String role2 : ant.argHash.keySet()) {
				if (role1.equalsIgnoreCase(role2)) {
					ArrayList<EventMentionArgument> arg1 = em.argHash.get(role1);
					ArrayList<EventMentionArgument> arg2 = ant.argHash.get(role2);

					if (arg1.size() != 1 || arg2.size() != 1) {
						continue;
					}
					EntityMention m1 = arg1.get(0).mention;
					EntityMention m2 = arg2.get(0).mention;
					if (!m1.semClass.equals(m2.semClass)) {
						conflict = true;
					}
				}
			}
		}
		return conflict;
	}

}
