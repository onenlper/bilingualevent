package event.supercoref;

import java.util.ArrayList;
import java.util.HashSet;

import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Common.Feature;
import util.YYFeature;
import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import edu.cmu.lti.ws4j.util.WS4JConfiguration;

public class EventCorefFea extends YYFeature {

	EventChain ant;

	EventMention ana;

	EventMention lem;

	ACEDoc doc;

	HashSet<String> singular;
	HashSet<String> plural;

	ArrayList<EventMention> allEventMentions;

	private static ILexicalDatabase db = new NictWordNet();
	private static RelatednessCalculator[] rcs = {
	// new HirstStOnge(db), new LeacockChodorow(db), new Lesk(db),
	new WuPalmer(db),
	// new Resnik(db), new JiangConrath(db), new Lin(db), new Path(db)
	};

	public static double getSimi(String word1, String word2) {
		WS4JConfiguration.getInstance().setMFS(true);
		RelatednessCalculator rc = new WuPalmer(db);
		return rc.calcRelatednessOfWords(word1, word2);
		// for (RelatednessCalculator rc : rcs) {
		// double s = rc.calcRelatednessOfWords(word1, word2);
		// System.out.println(rc.getClass().getName() + "\t" + s);
		// }
	}

	public static void main(String[] args) {
		getSimi("hi", "moderate");
		long t0 = System.currentTimeMillis();
		getSimi("hi", "moderate");
		long t1 = System.currentTimeMillis();
		System.out.println("Done in " + (t1 - t0) + " msec.");
	}

	public EventCorefFea(boolean train, String name) {
		super(train, name);
		this.singular = Common.readFile2Set("dict/singular.unigrams.txt");
		this.plural = Common.readFile2Set("dict/plural.unigrams.txt");
	}

	public void configure(EventChain ant, EventMention ana, ACEDoc doc,
			ArrayList<EventMention> allEventMentions) {
		this.ant = ant;
		this.ana = ana;
		this.doc = doc;
		this.lem = ant.getEventMentions()
				.get(ant.getEventMentions().size() - 1);
		this.allEventMentions = allEventMentions;
	}

	@Override
	public ArrayList<Feature> getCategoryFeatures() {
		// TODO Auto-generated method stub
		// type & subtype

		return null;
	}

	@Override
	public ArrayList<String> getStrFeatures() {
		// TODO Auto-generated method stub
		ArrayList<String> feas = new ArrayList<String>();

		feas.add("type_subtype#" + this.ana.getType() + "-"
				+ this.ana.getSubType());

		String anaPOS = doc.getPostag(this.ana.getAnchorStart());
		String anaToken = doc.getWord(this.ana.getAnchorStart());
//		String anaLemma = doc.getLemma(this.ana.getAnchorStart());

		if (anaPOS.startsWith("N") || anaPOS.startsWith("PRP")) {
			feas.add("nominal#" + "yes");
			if (this.singular.contains(anaToken.toLowerCase())) {
				feas.add("nom_number#" + "singular");
			} else if (this.plural.contains(anaToken.toLowerCase())) {
				feas.add("nom_number#" + "plura");
			} else {
				feas.add("nom_number#" + "unknown");
			}
			if (anaPOS.startsWith("PRP")) {
				feas.add("pronominal#" + "yes");
			} else {
				feas.add("pronominal#" + "no");
			}
		} else {
			feas.add("nominal#" + "no");
		}
		boolean exact_match = false;
		boolean partial_match = false;

		double maxSim = 0;

		for (EventMention e : ant.getEventMentions()) {
			String eToken = doc.getWord(e.getAnchorStart());
//			String eLemma = doc.getLemma(e.getAnchorStart());

			if (e.getAnchor().equalsIgnoreCase(ana.getAnchor())) {
				exact_match = true;
			}
			if (e.getAnchor().contains(ana.getAnchor())) {
				partial_match = true;
			}
			double sim = getSimi(eToken.toLowerCase(), anaToken.toLowerCase());
			maxSim = Math.max(sim, maxSim);
		}

		if (exact_match) {
			feas.add("exact_match#" + "yes");
		} else {
			feas.add("exact_match#" + "no");
		}

		if (partial_match) {
			feas.add("partial_match#" + "yes");
		} else {
			feas.add("partial_match#" + "no");
		}
		int sim = (int) (maxSim / 0.2);
		feas.add("trigger_sim" + Integer.toString(sim));

		String lem_tr = doc.getWord(this.lem.getAnchorStart());
		feas.add("trigger_pair#" + anaToken + "-" + lem_tr);

		String lem_posTag = doc.getPostag(this.lem.getAnchorStart());
		feas.add("pos_pair#" + anaPOS + "-" + lem_posTag);

//		int anaPosition[] = doc.positionMap.get(this.ana.getAnchorStart());
//		int lemPosition[] = doc.positionMap.get(this.lem.getAnchorStart());
//
//		int tokenDis = 0;
//		if (anaPosition[0] == lemPosition[0]) {
//			tokenDis = anaPosition[1] - lemPosition[1];
//		} else {
//			tokenDis += doc.parseReults.get(lemPosition[0]).words.size()
//					- lemPosition[1];
//			tokenDis += anaPosition[1] - 1;
//			for (int k = lemPosition[0] + 1; k < anaPosition[0]; k++) {
//				tokenDis += doc.parseReults.get(k).words.size();
//			}
//		}
//		feas.add("token_dis#" + (tokenDis / 5));
//		feas.add("sentence_dis#" + (anaPosition[0] - lemPosition[0]));
//
//		int k1 = 0;
//		int k2 = 0;
//		for (int i = 0; i < this.allEventMentions.size(); i++) {
//			if (this.allEventMentions.get(i).equals(lem)) {
//				k1 = i;
//			}
//			if (this.allEventMentions.get(i).equals(this.ana)) {
//				k2 = i;
//			}
//		}
//		feas.add("event_dis#" + (k2 - k1));
//		int overlap_num = 0;
//		HashSet<String> overlap_roles = new HashSet<String>();
//
//		for (EventMentionArgument arg1 : this.ana.getEventMentionArguments()) {
//			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
//			Entity entity1 = doc.entityCorefMap.get(arg1Name);
//			String role1 = arg1.getRole();
//
//			for (EventMentionArgument arg2 : this.ant
//					.getAllEventMentionArgument()) {
//				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
//				Entity entity2 = doc.entityCorefMap.get(arg2Name);
//				String role2 = arg2.getRole();
//
//				if (role1.equals(role2)) {
//					if (entity1 == null && entity2 == null
//							&& arg1.getExtent().equals(arg2.getExtent())) {
//						overlap_num += 1;
//						overlap_roles.add(role2);
//					} else if (entity1 != null && entity2 != null
//							&& entity1 == entity2) {
//						overlap_num += 1;
//						overlap_roles.add(role2);
//					}
//				}
//			}
//		}
//		feas.add("overlap_num#" + overlap_num);
//		for (String role : overlap_roles) {
//			feas.add("overlap_role#" + role);
//		}
//
//		int act_num = 0;
//		HashSet<String> act_roles = new HashSet<String>();
//		for (EventMentionArgument arg1 : this.ana.getEventMentionArguments()) {
//			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
//			Entity entity1 = doc.entityCorefMap.get(arg1Name);
//			String role1 = arg1.getRole();
//			boolean find = false;
//			for (EventMentionArgument arg2 : this.ant
//					.getAllEventMentionArgument()) {
//				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
//				Entity entity2 = doc.entityCorefMap.get(arg2Name);
//				String role2 = arg2.getRole();
//				
//				if(role1.equals(role2)) {
//					if (entity1 == null && entity2 == null
//							&& arg1.getExtent().equals(arg2.getExtent())) {
//						find = true;
//					} else if (entity1 != null && entity2 != null
//							&& entity1 == entity2) {
//						find = true;
//					}
//				}
//			}
//			if (!find) {
//				act_num += 1;
//				act_roles.add(role1);
//			}
//		}
//		feas.add("act_num#" + act_num);
//		for (String role : act_roles) {
//			feas.add("act_role#" + role);
//		}
//
//		int prior_num = 0;
//		HashSet<String> prior_roles = new HashSet<String>();
//		for (EventMentionArgument arg2 : this.ant.getAllEventMentionArgument()) {
//			String arg2Name = arg2.getStart() + "," + arg2.getEnd();
//			Entity entity2 = doc.entityCorefMap.get(arg2Name);
//			String role2 = arg2.getRole();
//			boolean find = false;
//			
//			for (EventMentionArgument arg1 : this.ana
//					.getEventMentionArguments()) {
//				String arg1Name = arg1.getStart() + "," + arg1.getEnd();
//				Entity entity1 = doc.entityCorefMap.get(arg1Name);
//				String role1 = arg1.getRole();
//				if(role1.equals(role2)) {
//					if (entity1 == null && entity2 == null
//							&& arg1.getExtent().equals(arg2.getExtent())) {
//						find = true;
//					} else if (entity1 != null && entity2 != null
//							&& entity1 == entity2) {
//						find = true;
//					}
//				}
//			}
//			if (!find) {
//				prior_num += 1;
//				prior_roles.add(role2);
//			}
//		}
//		feas.add("prior_num#" + prior_num);
//		for (String role : prior_roles) {
//			feas.add("prior_role#" + role);
//		}
//		
//		int coref_num = 0;
//		for (EventMentionArgument arg1 : this.ana.getEventMentionArguments()) {
//			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
//			Entity entity1 = doc.entityCorefMap.get(arg1Name);
//			String role1 = arg1.getRole();
//
//			for (EventMentionArgument arg2 : this.ant
//					.getAllEventMentionArgument()) {
//				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
//				Entity entity2 = doc.entityCorefMap.get(arg2Name);
//				String role2 = arg2.getRole();
//
//				if (!role1.equals(role2)) {
//					if (entity1 == null && entity2 == null
//							&& arg1.getExtent().equals(arg2.getExtent())) {
//						coref_num += 1;
//					} else if (entity1 != null && entity2 != null
//							&& entity1 == entity2) {
//						coref_num += 1;
//					}
//				}
//			}
//		}
//		feas.add("coref_num#" + coref_num);
//		
//		boolean time_conflict = false;
//		boolean place_conflict = false;
//		
//		for (EventMentionArgument arg1 : this.ana.getEventMentionArguments()) {
//			String arg1Name = arg1.getStart() + "," + arg1.getEnd();
//			Entity entity1 = doc.entityCorefMap.get(arg1Name);
//			String role1 = arg1.getRole();
//
//			for (EventMentionArgument arg2 : this.ant
//					.getAllEventMentionArgument()) {
//				String arg2Name = arg2.getStart() + "," + arg2.getEnd();
//				Entity entity2 = doc.entityCorefMap.get(arg2Name);
//				String role2 = arg2.getRole();
//
//				if (role1.equals(role2) && role1.equals("Time-Within")) {
//					if (entity1 == null && entity2 == null
//							&& arg1.getExtent().equals(arg2.getExtent())) {
//						
//					} else if (entity1 != null && entity2 != null
//							&& entity1 == entity2) {
//						
//					} else {
//						time_conflict = true;
//					}
//				}
//				
//				if (role1.equals(role2) && role1.equals("Place")) {
//					if (entity1 == null && entity2 == null
//							&& arg1.getExtent().equals(arg2.getExtent())) {
//						
//					} else if (entity1 != null && entity2 != null
//							&& entity1 == entity2) {
//						
//					} else {
//						place_conflict = true;
//					}
//				}
//			}
//		}
//		feas.add("time_conflict#" + time_conflict);
//		feas.add("place_conflict#" + place_conflict);
		
//		feas.add("mod#" + this.ana.modality);
//		feas.add("pol#" + this.ana.polarity);
//		feas.add("gen#" + this.ana.genericity);
//		feas.add("ten#" + this.ana.tense);
//		
//		feas.add("mod_conflict#" + this.ana.modality.equals(this.lem.modality));
//		feas.add("pol_conflict#" + this.ana.polarity.equals(this.lem.polarity));
//		feas.add("gen_conflict#" + this.ana.genericity.equals(this.lem.genericity));
//		feas.add("ten_conflict#" + this.ana.tense.equals(this.lem.tense));
		
		return feas;
	}

}
