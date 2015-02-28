package coref;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.ACEDoc;
import model.Entity;
import model.EntityMention;
import model.EventMention;
import model.ParseResult;
import util.Common;
import util.Util;
import coref.ResolveGroup.Entry;
import entity.semantic.SemanticTrainMultiSeed;

public class EMLearnSeed {

	// static HashMap<Context, Double> p_context_ = new HashMap<Context,
	// Double>();

	static Parameter tenseP;
	static Parameter polarityP;
	static Parameter genericityP;
	static Parameter modalityP;
	static Parameter triggerP;

	static double countl0 = 0;
	static double countl1 = 0;

	static double pl0 = 0;
	static double pl1 = 0;

	static ArrayList<HashMap<String, Double>> multiFracContextsCountl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsCountl1 = new ArrayList<HashMap<String, Double>>();

	static ArrayList<HashMap<String, Double>> multiFracContextsProbl0 = new ArrayList<HashMap<String, Double>>();
	static ArrayList<HashMap<String, Double>> multiFracContextsProbl1 = new ArrayList<HashMap<String, Double>>();

	static HashMap<String, Double> tensePrior = new HashMap<String, Double>();
	static HashMap<String, Double> polarityPrior = new HashMap<String, Double>();
	static HashMap<String, Double> genericityPrior = new HashMap<String, Double>();
	static HashMap<String, Double> modalityPrior = new HashMap<String, Double>();

	static double word2vecSimi = .9;

	static HashMap<String, Double> contextPrior;
	static HashMap<String, Double> contextOverall;
	static HashMap<String, Double> fracContextCount;

	static HashMap<String, Double> contextVals;

	static int maxDistance = 50000;
	// static int maxDistance = 75;

	static int maxDisFeaValue = 10;
	// static int contextSize = 2 * 2 * 2 * 3 * 2 * (maxDisFeaValue + 1);
	public static int qid = 0;

	// static int count = 0;

	public static void init() {
		tenseP = new Parameter(new HashSet<String>(EMUtil.Tense.subList(0,
				EMUtil.Tense.size() - 1)));
		polarityP = new Parameter(new HashSet<String>(EMUtil.Polarity.subList(
				0, EMUtil.Polarity.size() - 1)));
		genericityP = new Parameter(new HashSet<String>(
				EMUtil.Genericity.subList(0, EMUtil.Genericity.size() - 1)));

		modalityP = new Parameter(new HashSet<String>(EMUtil.Modality.subList(
				0, EMUtil.Modality.size() - 1)));
		triggerP = new Parameter(Common.readFile2Set("trSet"));
		// triggerP = new Parameter(new HashSet<String>(Util.subTypes));

		for (int i = 0; i < Context.getSubContext().size(); i++) {
			multiFracContextsCountl0.add(new HashMap<String, Double>());
			multiFracContextsCountl1.add(new HashMap<String, Double>());
			multiFracContextsProbl0.add(new HashMap<String, Double>());
			multiFracContextsProbl1.add(new HashMap<String, Double>());
		}

		contextPrior = new HashMap<String, Double>();
		contextOverall = new HashMap<String, Double>();
		fracContextCount = new HashMap<String, Double>();
		contextVals = new HashMap<String, Double>();
		qid = 0;
		// Context.contextCache.clear();
	}

	@SuppressWarnings("unused")
	public static ArrayList<ResolveGroup> extractGroups(ACEDoc doc,
			ArrayList<EventMention> eventMentions,
			ArrayList<EntityMention> entityMentions) {
		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		Util.assignArgumentWithEntityMentions(eventMentions,
				entityMentions, doc);

		Collections.sort(eventMentions);

		for (int i = 0; i < eventMentions.size(); i++) {
			eventMentions.get(i).sequenceID = i;
		}

		for (int i = 0; i < doc.parseReults.size(); i++) {
			ParseResult pr = doc.parseReults.get(i);
			pr.evms = EMUtil.getEventMentionInOneS(doc, eventMentions,
					i);

			for (EventMention event : pr.evms) {
				// Util.assignSystemAttribute(doc.fileID, event, true);
				trs.add(event.getAnchor());
			}

			ArrayList<EventMention> precedMs = new ArrayList<EventMention>();
			for (int j = maxDistance; j >= 1; j--) {
				if (i - j >= 0) {
					for (EventMention m : doc.parseReults.get(i - j).evms) {
						precedMs.add(m);
					}
				}
			}
			Collections.sort(pr.evms);
			for (int j = 0; j < pr.evms.size(); j++) {
				EventMention m = pr.evms.get(j);
				qid++;

				ArrayList<EventMention> ants = new ArrayList<EventMention>();
				ants.addAll(precedMs);

				if (j > 0) {
					for (EventMention precedM : pr.evms.subList(0, j)) {
						ants.add(precedM);
					}
				}

				EventMention fake = new EventMention();
				fake.setAnchor("Fake");
				fake.extent = "fake";
				fake.setFake();
				ants.add(fake);
				fake.setSubType("null");

				ResolveGroup rg = new ResolveGroup(m, doc, ants);
				Collections.sort(ants);
				Collections.reverse(ants);

				double norm = 0;

				for (EventMention ant : ants) {
					Entry entry = new Entry(ant, null, doc);
					rg.entries.add(entry);
					entry.p_c = EMUtil.getP_C(ant, m, doc);
					// if (entry.p_c != 0) {
					norm += entry.p_c;
					// }
				}
				for (Entry entry : rg.entries) {
					if (entry.isFake) {
						entry.p_c = Entry.p_fake_decay
								/ (Entry.p_fake_decay + norm);
					} else if (entry.p_c != 0) {
						entry.p_c = 1 / (Entry.p_fake_decay + norm);
					}
				}

				countl0 += rg.entries.size() - 1;
				countl1 += 1;

				pl0 = countl0 / (countl0 + countl1);
				pl1 = countl1 / (countl0 + countl1);

				groups.add(rg);
			}
		}
		return groups;
	}

	public static void sortEntries(ResolveGroup rg,
			HashMap<String, HashSet<String>> chainMaps) {
		ArrayList<Entry> goodEntries = new ArrayList<Entry>();
		ArrayList<Entry> fakeEntries = new ArrayList<Entry>();
		ArrayList<Entry> badEntries = new ArrayList<Entry>();
		for (int k = 0; k < rg.entries.size(); k++) {
			Entry entry = rg.entries.get(k);
			EventMention ant = rg.entries.get(k).ant;

			// TODO
			if (entry.isFake) {
				fakeEntries.add(entry);
			} else if ((ant.getAnchor().equalsIgnoreCase(rg.m.getAnchor()) || Util
					._commonBV_(ant, rg.m))) {
				// else if(
				// ant.getSubType().equals(rg.m.getSubType())
				// &&
				// ant.tense.equals(rg.m.tense)
				// && ant.modality.equals(rg.m.modality) &&
				// ant.genericity.equals(rg.m.genericity)
				// && ant.polarity.equals(rg.m.polarity)
				// ) {
				// else if(true) {
				goodEntries.add(entry);
			} else {
				badEntries.add(entry);
			}
		}
		ArrayList<Entry> allEntries = new ArrayList<Entry>();
		allEntries.addAll(goodEntries);
		allEntries.addAll(fakeEntries);
		allEntries.addAll(badEntries);
		for (int k = 0; k < allEntries.size(); k++) {
			allEntries.get(k).seq = k;
		}
	}

	static int percent = 10;

	
	
	public static HashMap<String, ArrayList<EntityMention>> loadSVMResult(String part) {
		HashMap<String, ArrayList<EntityMention>> entityMentionses = new HashMap<String, ArrayList<EntityMention>>();
		
		String folder = "./";
		ArrayList<String> mentionStrs = Common.getLines(folder + "mention.test" + part);
		System.out.println(mentionStrs.size());
		ArrayList<String> typeResult = Common.getLines(folder + "multiType.result" + part);
		
		HashMap<String, EntityMention> mMap = new HashMap<String, EntityMention>();
		
		for(int i=0;i<mentionStrs.size();i++) {
			String mentionStr = mentionStrs.get(i);
			String fileKey = mentionStr.split("\\s+")[1];
			String startEndStr = mentionStr.split("\\s+")[0];
			int headStart = Integer.valueOf(startEndStr.split(",")[0]);
			int headEnd = Integer.valueOf(startEndStr.split(",")[1]);
			EntityMention em = new EntityMention();
			em.headStart = headStart;
			em.headEnd = headEnd;
			em.start = headStart;
			em.end = headEnd;
			
			mMap.put(em.toName(), em);
			
			String tks[] = typeResult.get(i).split("\\s+");
			int typeIndex = Integer.valueOf(tks[0]);
			
			for(int k=1;k<tks.length;k++) {
				em.semClassConf.add(Double.parseDouble(tks[k]));
			}
			
			String type = SemanticTrainMultiSeed.semClasses.get(typeIndex - 1);
			if(type.equalsIgnoreCase("none")) {
				continue;
			}
			
			em.semClass = type;
			
			ArrayList<EntityMention> mentions = entityMentionses.get(fileKey);
			if(mentions==null) {
				mentions = new ArrayList<EntityMention>();
				entityMentionses.put(fileKey, mentions);
			}
			if(type.equalsIgnoreCase("val")) {
				em.type = "Value";
			} else if(type.equalsIgnoreCase("time")) {
				em.type = "Time";
			} else {
			}
			mentions.add(em);
		}
		
		return entityMentionses;
	}
	
	private static void extractCoNLL(ArrayList<ResolveGroup> groups) {
		ArrayList<String> lines = Common.getLines("ACE_Chinese_train"
				+ Util.part);

		HashMap<String, ArrayList<EntityMention>> entityMentionses = loadSVMResult("6");

		HashMap<String, HashMap<String, EventMention>> allEventMentions = Util.readResult("joint_svm/result6", "chi");
		
		ArrayList<String> activeLines = Common.getLines("ACE_Chinese_train6");
		HashSet<String> annotated = new HashSet<String>();
		for(String line : activeLines) {
			String tks[] = line.split("\\s+");
			String file = tks[0];
			boolean all = false;
			for(int i=1;i<tks.length;i++) {
				if(tks[i].equals("all")) {
					all = true;
					break;
				}
			}
			if(all) {
				annotated.add(file);
			}
		}
		int annotatedDoc = 0;
		
		
		for (int i=0;i<lines.size();i++) {
			String line = lines.get(i);
			ACEDoc d = new ACEChiDoc(line);

			ArrayList<EntityMention> entityMentions = new ArrayList<EntityMention>(entityMentionses.get(line));
			HashMap<String, EntityMention> mMap = new HashMap<String, EntityMention>();
			for(EntityMention m : entityMentions) {
				m.head = d.content.substring(m.headStart,
						m.headEnd + 1);
				mMap.put(m.toName(), m);
			}
			ArrayList<String> corefLines = Common.getLines("/users/yzcchen/chen3/entityEMACE/EntityEMACE/src/em_coref_train" + Util.part
						+"/" + i + ".entity.coref");
			ArrayList<Entity> entities = new ArrayList<Entity>();
			for(String corefLine : corefLines) {
				Entity e = new Entity();
				String ms[] = corefLine.trim().split("\\s+");
				StringBuilder sb = new StringBuilder();
				for(String m : ms) {
					mMap.get(m).entity = e;
					sb.append(mMap.get(m).head).append(" ");
				}
				entities.add(e);
			}
			d.setEntityCorefMap(entities);
			
			
			for(EntityMention m : entityMentions) {
				if(m.getType()==null) {
					Util.setMentionType(m, d);
				}
			}
			ArrayList<EventMention> eventMentions= new ArrayList<EventMention>();
			if(allEventMentions.containsKey(line)) {
				eventMentions.addAll(allEventMentions.get(line).values());
			}
			
			if(annotated.contains(line)) {
				annotatedDoc += 1;
				ArrayList<EntityMention> goldEntityMentions = new ArrayList<EntityMention>();
				goldEntityMentions.addAll(d.goldEntityMentions);
				goldEntityMentions.addAll(d.goldTimeMentions);
				goldEntityMentions.addAll(d.goldValueMentions);
				groups.addAll(extractGroups(d, d.goldEventMentions, goldEntityMentions));
			} else {
				groups.addAll(extractGroups(d, eventMentions, entityMentions));
			}
		}

		System.out.println(groups.size());
	}

	private static void addOne(String key, HashMap<String, Double> map) {
		if (map.containsKey(key)) {
			map.put(key, map.get(key) + 1.0);
		} else {
			map.put(key, 1.0);
		}
	}

	public static HashMap<String, HashSet<String>> chainMaps = new HashMap<String, HashSet<String>>();

	public static void estep(ArrayList<ResolveGroup> groups) {
		// System.out.println("estep starts:");
		long t1 = System.currentTimeMillis();
		chainMaps.clear();
		contextPrior.clear();
		for (ResolveGroup rg : groups) {
			for (Entry entry : rg.entries) {
				if (!chainMaps.containsKey(entry.antName) && !entry.isFake) {
					HashSet<String> set = new HashSet<String>();
					set.add(entry.antName);
					chainMaps.put(entry.antName, set);
				}
			}
			sortEntries(rg, chainMaps);
			for (int k = 0; k < rg.entries.size(); k++) {
				Entry entry = rg.entries.get(k);
				// add antecedents
				entry.context = Context.buildContext(entry.ant, rg.m, rg.doc,
						rg.ants, entry.seq);

				Double d = contextPrior.get(entry.context.toString());
				if (d == null) {
					contextPrior.put(entry.context.toString(), 1.0);
				} else {
					contextPrior.put(entry.context.toString(),
							1.0 + d.doubleValue());
				}
			}
			double norm = 0;

			double P_anchor_fake = 0;
			double all = 0;
			for (Entry entry : rg.entries) {
				if (entry.isFake || entry.p_c == 0) {
					continue;
				}
				all += 1;
				P_anchor_fake += triggerP.getVal(entry.ant.getAnchor(),
						rg.m.getAnchor());
			}
			if (all == 0) {
				P_anchor_fake = 0;
			} else {
				P_anchor_fake = P_anchor_fake / all;
			}

			for (Entry entry : rg.entries) {
				Context context = entry.context;

				double p_tense = tenseP.getVal(entry.ant.tense, rg.m.tense);
				double p_polarity = polarityP.getVal(entry.ant.polarity,
						rg.m.polarity);
				double p_modality = modalityP.getVal(entry.ant.modality,
						rg.m.modality);

				double p_genericity = genericityP.getVal(entry.ant.genericity,
						rg.m.genericity);

				double p_anchor = 0;
				if (entry.isFake) {
					p_anchor = P_anchor_fake;
				} else {
					p_anchor = triggerP.getVal(entry.ant.getAnchor(),
							rg.m.getAnchor());
				}

				double p_context = .5;
				// Double d = contextVals.get(context.toString());
				// if (contextVals.containsKey(context.toString())) {
				// p_context = d.doubleValue();
				// } else {
				// p_context = .5;
				// // if(context.toString().startsWith("0")) {
				// // p_context = .1;
				// // }
				// }

				// multiFracContextsCountl0.get(i).clear();
				// multiFracContextsCountl1.get(i).clear();
				// multiFracContextsProbl0.get(i).clear();
				// multiFracContextsProbl1.get(i).clear();

				double p_context_l1 = pl1;
				double p_context_l0 = pl0;

				// System.out.println("pl0:" + p_context_l0);

				for (int i = 0; i < Context.getSubContext().size(); i++) {
					int pos[] = Context.getSubContext().get(i);
					String key = context.getKey(i);
					if (key.equals("-")) {
						System.out.println(context.toString());
						Common.bangErrorPOS("!!!");
					}
					if (multiFracContextsProbl1.get(i).containsKey(key)) {
						p_context_l1 *= multiFracContextsProbl1.get(i).get(key);
					} else {
						p_context_l1 *= Context.normConstant.get(i);
					}

					if (multiFracContextsProbl0.get(i).containsKey(key)) {
						p_context_l0 *= multiFracContextsProbl0.get(i).get(key);
					} else {
						p_context_l0 *= Context.normConstant.get(i);
					}
				}

				p_context = p_context_l1 / (p_context_l1 + p_context_l0);

				// double pdenom = 0;

				entry.p = p_context * entry.p_c * p_anchor;
				// entry.p *= 1 * p_tense
				// * p_polarity
				// // * p_eventSubType
				// * p_genericity * p_modality
				// ;
				norm += entry.p;
			}

			double max = -1;
			int maxIdx = -1;

			String antName = "";

			if (norm != 0) {
				for (Entry entry : rg.entries) {
					entry.p = entry.p / norm;

					if (entry.p > max) {
						max = entry.p;
						antName = entry.antName;
					}
				}
			} else {
				// Common.bangErrorPOS("!");
			}

			if (!antName.equals("fake") && !antName.isEmpty()) {
				HashSet<String> corefs = chainMaps.get(antName);
				corefs.add(rg.anaphorName);
				chainMaps.put(rg.anaphorName, corefs);
			}
		}
		// Common.pause(":");
		// System.out.println(System.currentTimeMillis() - t1);
	}

	public static void mstep(ArrayList<ResolveGroup> groups) {
		// System.out.println("mstep starts:");
		long t1 = System.currentTimeMillis();
		polarityP.resetCounts();
		tenseP.resetCounts();
		triggerP.resetCounts();
		contextVals.clear();
		genericityP.resetCounts();
		modalityP.resetCounts();
		fracContextCount.clear();

		for (int i = 0; i < multiFracContextsCountl1.size(); i++) {
			multiFracContextsCountl0.get(i).clear();
			multiFracContextsCountl1.get(i).clear();
			multiFracContextsProbl0.get(i).clear();
			multiFracContextsProbl1.get(i).clear();
		}

		for (ResolveGroup group : groups) {
			for (Entry entry : group.entries) {
				double p = entry.p;
				Context context = entry.context;

//				tenseP.addFracCount(entry.ant.tense, group.m.tense, p);
//				polarityP.addFracCount(entry.ant.polarity, group.m.polarity, p);
//				if (!entry.isFake) {
//					triggerP.addFracCount(entry.ant.getAnchor(),
//							group.m.getAnchor(), p);
//				}
//				genericityP.addFracCount(entry.ant.genericity,
//						group.m.genericity, p);
//
//				modalityP.addFracCount(entry.ant.modality, group.m.modality, p);

				Double d = fracContextCount.get(context.toString());
				if (d == null) {
					fracContextCount.put(context.toString(), p);
				} else {
					fracContextCount.put(context.toString(), d.doubleValue()
							+ p);
				}

				for (int i = 0; i < Context.getSubContext().size(); i++) {
					int ps[] = Context.getSubContext().get(i);
					String key = context.getKey(i);
					double l1 = p;
					double l0 = 1 - p;

					Double cl0 = multiFracContextsCountl0.get(i).get(key);
					if (cl0 == null) {
						multiFracContextsCountl0.get(i).put(key, l0);
					} else {
						multiFracContextsCountl0.get(i).put(key,
								l0 + cl0.doubleValue());
					}

					Double cl1 = multiFracContextsCountl1.get(i).get(key);
					if (cl1 == null) {
						multiFracContextsCountl1.get(i).put(key, l1);
					} else {
						multiFracContextsCountl1.get(i).put(key,
								l1 + cl1.doubleValue());
					}
				}

			}
		}
		polarityP.setVals();
		tenseP.setVals();
		triggerP.setVals();
		genericityP.setVals();
		// cilin.setVals();
		modalityP.setVals();
		for (String key : fracContextCount.keySet()) {
			double p_context = (EMUtil.alpha + fracContextCount.get(key))
					/ (2.0 * EMUtil.alpha + contextPrior.get(key));
			contextVals.put(key, p_context);
		}
		// System.out.println(System.currentTimeMillis() - t1);

		for (int i = 0; i < Context.getSubContext().size(); i++) {

			for (String key : multiFracContextsCountl1.get(i).keySet()) {

				double contextcountl0 = 1;
				if (multiFracContextsCountl0.get(i).containsKey(key)) {
					contextcountl0 += multiFracContextsCountl0.get(i).get(key);
				}
				double pcountl0 = contextcountl0
						/ (countl0 + Context.normConstant.get(i));

				double contextcountl1 = 1;
				if (multiFracContextsCountl1.get(i).containsKey(key)) {
					contextcountl1 += multiFracContextsCountl1.get(i).get(key);
				}
				double pcountl1 = contextcountl1
						/ (countl1 + Context.normConstant.get(i));

				multiFracContextsProbl0.get(i).put(key, pcountl0);
				multiFracContextsProbl1.get(i).put(key, pcountl1);
			}
		}
	}

	static HashSet<String> trs = new HashSet<String>();

	public static void main(String args[]) throws Exception {
		Context.todo = Common.readFile2Set("trPair");
		if (args.length != 1) {
			System.out.println("java ~ 0");
			Common.bangErrorPOS("");
		}
		Util.part = args[0];
		run();
		// Common.outputHashSet(trs, "trSet" + Util.part);
		// Common.outputHashSet(Context.todo, "trPair");
	}

	private static void run() throws IOException, FileNotFoundException {
		init();

		EMUtil.train = true;

		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		extractCoNLL(groups);
		int it = 0;
		while (it < 20) {
			System.out.println("Iteration: " + it);
			estep(groups);
			mstep(groups);
			it++;
		}

		tenseP.printParameter("tenseP" + Util.part);
		polarityP.printParameter("polarityP" + Util.part);
		triggerP.printParameter("triggerP" + Util.part);
		genericityP.printParameter("genericityP" + Util.part);
		modalityP.printParameter("modalityP" + Util.part);

		ObjectOutputStream modelOut = new ObjectOutputStream(
				new FileOutputStream("EMModel" + Util.part));
		modelOut.writeObject(tenseP);
		modelOut.writeObject(polarityP);
		modelOut.writeObject(triggerP);
		modelOut.writeObject(genericityP);
		modelOut.writeObject(modalityP);

		modelOut.writeObject(tensePrior);
		modelOut.writeObject(polarityPrior);
		modelOut.writeObject(genericityPrior);
		modelOut.writeObject(modalityPrior);

		modelOut.writeObject(fracContextCount);
		modelOut.writeObject(contextPrior);

		modelOut.writeObject(multiFracContextsProbl0);
		modelOut.writeObject(multiFracContextsProbl1);
		modelOut.writeObject(pl0);
		modelOut.writeObject(pl1);

		for (int i = 0; i < multiFracContextsProbl1.size(); i++) {
			ArrayList<String> output = new ArrayList<String>();
			for (String key : multiFracContextsProbl1.get(i).keySet()) {
				output.add(key + ":\t"
						+ multiFracContextsProbl1.get(i).get(key));
			}
			Common.outputLines(output, "probl1_" + Util.part + ".sub" + i);
		}

		for (int i = 0; i < multiFracContextsProbl0.size(); i++) {
			ArrayList<String> output = new ArrayList<String>();
			for (String key : multiFracContextsProbl0.get(i).keySet()) {
				output.add(key + ":\t"
						+ multiFracContextsProbl0.get(i).get(key));
			}
			Common.outputLines(output, "probl0_" + Util.part + ".sub" + i);
		}
		// modelOut.writeObject(Context.svoStat);

		modelOut.close();

		Common.outputHashMap(contextVals, "contextVals" + Util.part);
		Common.outputHashMap(fracContextCount, "fracContextCount" + Util.part);
		Common.outputHashMap(contextPrior, "contextPrior" + Util.part);
		// ObjectOutputStream svoStat = new ObjectOutputStream(new
		// FileOutputStream(
		// "/dev/shm/svoStat"));
		// svoStat.writeObject(Context.svoStat);
		// svoStat.close();

		// System.out.println(EMUtil.missed);

		ApplyEMSeed.run("all");
		// ApplyEM.run("nw");
		// ApplyEM.run("mz");
		// ApplyEM.run("wb");
		// ApplyEM.run("bn");
		// ApplyEM.run("bc");
		// ApplyEM.run("tc");
	}

}
