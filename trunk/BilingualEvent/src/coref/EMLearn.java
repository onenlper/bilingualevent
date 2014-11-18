package coref;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventMention;
import model.ParseResult;
import util.Common;
import coref.ResolveGroup.Entry;

public class EMLearn {

	// static HashMap<Context, Double> p_context_ = new HashMap<Context,
	// Double>();

	static Parameter tenseP;
	static Parameter polarityP;
	static Parameter genericityP;
	static Parameter modalityP;
	static Parameter eventSubTypeP;

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
		eventSubTypeP = new Parameter(new HashSet<String>(
				EMUtil.EventSubType.subList(0, EMUtil.EventSubType.size() - 1)));

		contextPrior = new HashMap<String, Double>();
		contextOverall = new HashMap<String, Double>();
		fracContextCount = new HashMap<String, Double>();
		contextVals = new HashMap<String, Double>();
		qid = 0;
		// Context.contextCache.clear();
	}

	@SuppressWarnings("unused")
	public static ArrayList<ResolveGroup> extractGroups(ACEDoc doc) {
		ArrayList<ResolveGroup> groups = new ArrayList<ResolveGroup>();

		for (int i = 0; i < doc.goldEventMentions.size(); i++) {
			doc.goldEventMentions.get(i).sequenceID = i;
		}

		for (int i = 0; i < doc.parseReults.size(); i++) {
			ParseResult pr = doc.parseReults.get(i);
			pr.evms = EMUtil.getEventMentionInOneS(doc, doc.goldEventMentions,
					i);
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
				fake.extent = "fakkkkke";
				fake.setFake();
				ants.add(fake);

				ResolveGroup rg = new ResolveGroup(m, doc, ants);
				Collections.sort(ants);
				Collections.reverse(ants);

				int seq = 0;

				for (EventMention ant : ants) {
					Entry entry = new Entry(ant, null, doc);
					rg.entries.add(entry);
					entry.p_c = EMUtil.getP_C(ant, m, doc);
					if (entry.p_c != 0) {
						seq += 1;
					}
				}
				for (Entry entry : rg.entries) {
					if (entry.isFake) {
						entry.p_c = Entry.p_fake_decay
								/ (Entry.p_fake_decay + seq);
					} else if (entry.p_c != 0) {
						entry.p_c = 1 / (Entry.p_fake_decay + seq);
					}
				}

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
			} else if (ant.getAnchor().equalsIgnoreCase(rg.m.getAnchor())) {
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

	private static void extractCoNLL(ArrayList<ResolveGroup> groups) {
		ArrayList<String> lines = Common.getLines("ACE_English_train0");
		int docNo = 0;

		HashMap<String, Double> tenseCounts = new HashMap<String, Double>();
		HashMap<String, Double> polarityCounts = new HashMap<String, Double>();
		HashMap<String, Double> genericityCounts = new HashMap<String, Double>();
		HashMap<String, Double> modalityCounts = new HashMap<String, Double>();

		double all = 0;

		for (String line : lines) {
			if (docNo % 10 < percent) {
				ACEDoc d = new ACEEngDoc(line);
				groups.addAll(extractGroups(d));

				for (EventMention m : d.goldEventMentions) {
					addOne(m.tense, tenseCounts);
					addOne(m.polarity, polarityCounts);
					addOne(m.genericity, genericityCounts);
					addOne(m.modality, modalityCounts);
				}
				all += d.goldEventMentions.size();
			}
			docNo++;
		}

		double fakeCount = 222;

		buildPrior(tenseCounts, tensePrior, all, fakeCount);
		buildPrior(polarityCounts, polarityPrior, all, fakeCount);
		buildPrior(genericityCounts, genericityPrior, all, fakeCount);
		buildPrior(modalityCounts, modalityPrior, all, fakeCount);

		System.out.println(groups.size());
	}

	private static void buildPrior(HashMap<String, Double> countMap,
			HashMap<String, Double> priorMap, double all, double fake) {
		for (String key : countMap.keySet()) {
			double count = countMap.get(key);
			priorMap.put(key, count / (all + fake));
		}
		priorMap.put("Fake", fake / (all + fake));
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
			for (Entry entry : rg.entries) {
				Context context = entry.context;

				double p_tense = tenseP.getVal(entry.ant.tense, rg.m.tense);
				double p_polarity = polarityP.getVal(entry.ant.polarity,
						rg.m.polarity);
				double p_modality = modalityP.getVal(entry.ant.modality,
						rg.m.modality);

				double p_genericity = genericityP.getVal(entry.ant.genericity,
						rg.m.genericity);

				double p_eventSubType = eventSubTypeP.getVal(
						entry.ant.getSubType(), rg.m.getSubType());

				double p_context = .5;
				Double d = contextVals.get(context.toString());
				if (contextVals.containsKey(context.toString())) {
					p_context = d.doubleValue();
				} else {
					p_context = .5;
					// if(context.toString().startsWith("0")) {
					// p_context = .1;
					// }
				}

				entry.p = p_context * entry.p_c;
				entry.p *= 1 * p_tense
						* p_polarity
				// * p_eventSubType
						* p_genericity * p_modality
				;

				norm += entry.p;
			}

			double max = 0;
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
				Common.bangErrorPOS("!");
			}

			if (!antName.equals("fake")) {
				HashSet<String> corefs = chainMaps.get(antName);
				corefs.add(rg.anaphorName);
				chainMaps.put(rg.anaphorName, corefs);
			}
		}
		// System.out.println(System.currentTimeMillis() - t1);
	}

	public static void mstep(ArrayList<ResolveGroup> groups) {
		// System.out.println("mstep starts:");
		long t1 = System.currentTimeMillis();
		polarityP.resetCounts();
		tenseP.resetCounts();
		eventSubTypeP.resetCounts();
		contextVals.clear();
		genericityP.resetCounts();
		modalityP.resetCounts();
		fracContextCount.clear();
		for (ResolveGroup group : groups) {
			for (Entry entry : group.entries) {
				double p = entry.p;
				Context context = entry.context;

				tenseP.addFracCount(entry.ant.tense, group.m.tense, p);
				polarityP.addFracCount(entry.ant.polarity, group.m.polarity, p);
				eventSubTypeP.addFracCount(entry.ant.getSubType(),
						group.m.getSubType(), p);

				genericityP.addFracCount(entry.ant.genericity,
						group.m.genericity, p);

				modalityP.addFracCount(entry.ant.modality, group.m.modality, p);

				Double d = fracContextCount.get(context.toString());
				if (d == null) {
					fracContextCount.put(context.toString(), p);
				} else {
					fracContextCount.put(context.toString(), d.doubleValue()
							+ p);
				}
			}
		}
		polarityP.setVals();
		tenseP.setVals();
		eventSubTypeP.setVals();
		genericityP.setVals();
		// cilin.setVals();
		modalityP.setVals();
		for (String key : fracContextCount.keySet()) {
			double p_context = (EMUtil.alpha + fracContextCount.get(key))
					/ (2.0 * EMUtil.alpha + contextPrior.get(key));
			contextVals.put(key, p_context);
		}
		// System.out.println(System.currentTimeMillis() - t1);
	}

	public static void main(String args[]) throws Exception {
		run();
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

		tenseP.printParameter("tenseP");
		polarityP.printParameter("polarityP");
		eventSubTypeP.printParameter("eventSubTypeP");
		genericityP.printParameter("genericityP");
		modalityP.printParameter("modalityP");

		ObjectOutputStream modelOut = new ObjectOutputStream(
				new FileOutputStream("EMModel"));
		modelOut.writeObject(tenseP);
		modelOut.writeObject(polarityP);
		modelOut.writeObject(eventSubTypeP);
		modelOut.writeObject(genericityP);
		modelOut.writeObject(modalityP);

		modelOut.writeObject(tensePrior);
		modelOut.writeObject(polarityPrior);
		modelOut.writeObject(genericityPrior);
		modelOut.writeObject(modalityPrior);

		modelOut.writeObject(fracContextCount);
		modelOut.writeObject(contextPrior);

		// modelOut.writeObject(Context.svoStat);

		modelOut.close();

		Common.outputHashMap(contextVals, "contextVals");
		Common.outputHashMap(fracContextCount, "fracContextCount");
		Common.outputHashMap(contextPrior, "contextPrior");
		// ObjectOutputStream svoStat = new ObjectOutputStream(new
		// FileOutputStream(
		// "/dev/shm/svoStat"));
		// svoStat.writeObject(Context.svoStat);
		// svoStat.close();

		// System.out.println(EMUtil.missed);

		ApplyEM.run("all");
		// ApplyEM.run("nw");
		// ApplyEM.run("mz");
		// ApplyEM.run("wb");
		// ApplyEM.run("bn");
		// ApplyEM.run("bc");
		// ApplyEM.run("tc");
	}

}
