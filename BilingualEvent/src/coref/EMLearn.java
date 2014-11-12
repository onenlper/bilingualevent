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

	// static Parameter numberP;
	// static Parameter genderP;
	// static Parameter semanticP;
	// static Parameter grammaticP;
	// static Parameter animacyP;

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
		// numberP = new Parameter(1.0 / ((double)
		// EMUtil.Number.values().length));
		// genderP = new Parameter(1.0 / ((double)
		// EMUtil.Gender.values().length));
		// semanticP = new Parameter(1.0 / 25318.0);

		// semanticP = new Parameter(1.0/5254.0);

		// grammaticP = new Parameter(1.0 / 4.0);

		// animacyP = new Parameter(
		// 1.0 / ((double) EMUtil.Animacy.values().length));

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
				fake.isFake = true;
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
			} else if (ant.getAnchor().contains(rg.m.getAnchor())) {
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
		for (String line : lines) {
			if (docNo % 10 < percent) {
				ACEDoc d = new ACEEngDoc(line);
				groups.addAll(extractGroups(d));
			}
			docNo++;
		}
		System.out.println(groups.size());
	}

	public static HashMap<String, HashSet<String>> chainMaps = new HashMap<String, HashSet<String>>();

	public static void estep(ArrayList<ResolveGroup> groups) {
		System.out.println("estep starts:");
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
				//
				// double p_number = numberP.getVal(entry.number.name(),
				// rg.number.name());
				// double p_gender = genderP.getVal(entry.gender.name(),
				// rg.gender.name());
				// double p_animacy = animacyP.getVal(entry.animacy.name(),
				// rg.animacy.name());
				// double p_grammatic = grammaticP.getVal(entry.gram.name(),
				// rg.gram.name());
				//
				// double p_semetic = semanticP.getVal(entry.sem, rg.sem);

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
				entry.p *= 1
				// * p_number
				// * p_gender
				// * p_animacy
				// * p_semetic
				// * p_cilin
				// * p_grammatic
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
				// Common.bangErrorPOS("!");
			}

			if (!antName.equals("fake")) {
				HashSet<String> corefs = chainMaps.get(antName);
				corefs.add(rg.anaphorName);
				chainMaps.put(rg.anaphorName, corefs);
			}
		}
		System.out.println(System.currentTimeMillis() - t1);
	}

	public static void mstep(ArrayList<ResolveGroup> groups) {
		System.out.println("mstep starts:");
		long t1 = System.currentTimeMillis();
		// genderP.resetCounts();
		// numberP.resetCounts();
		// animacyP.resetCounts();
		contextVals.clear();
		// semanticP.resetCounts();
		// grammaticP.resetCounts();
		fracContextCount.clear();
		for (ResolveGroup group : groups) {
			for (Entry entry : group.entries) {
				double p = entry.p;
				Context context = entry.context;

				// numberP.addFracCount(entry.number.name(),
				// group.number.name(),
				// p);
				// genderP.addFracCount(entry.gender.name(),
				// group.gender.name(),
				// p);
				// animacyP.addFracCount(entry.animacy.name(),
				// group.animacy.name(), p);
				//
				// semanticP.addFracCount(entry.sem, group.sem, p);
				//
				// grammaticP
				// .addFracCount(entry.gram.name(), group.gram.name(), p);

				Double d = fracContextCount.get(context.toString());
				if (d == null) {
					fracContextCount.put(context.toString(), p);
				} else {
					fracContextCount.put(context.toString(), d.doubleValue()
							+ p);
				}
			}
		}
		// genderP.setVals();
		// numberP.setVals();
		// animacyP.setVals();
		// semanticP.setVals();
		// cilin.setVals();
		// grammaticP.setVals();
		for (String key : fracContextCount.keySet()) {
			double p_context = (EMUtil.alpha + fracContextCount.get(key))
					/ (2.0 * EMUtil.alpha + contextPrior.get(key));
			contextVals.put(key, p_context);
		}
		System.out.println(System.currentTimeMillis() - t1);
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

		// numberP.printParameter("numberP");
		// genderP.printParameter("genderP");
		// animacyP.printParameter("animacyP");
		// semanticP.printParameter("semanticP");
		// grammaticP.printParameter("grammaticP");

		ObjectOutputStream modelOut = new ObjectOutputStream(
				new FileOutputStream("EMModel"));
		// modelOut.writeObject(numberP);
		// modelOut.writeObject(genderP);
		// modelOut.writeObject(animacyP);
		// modelOut.writeObject(semanticP);
		// modelOut.writeObject(grammaticP);
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
