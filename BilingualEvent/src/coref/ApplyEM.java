package coref;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;
import coref.ResolveGroup.Entry;
import event.postProcess.AttriEvaluate;

public class ApplyEM {

	String folder;

	Parameter tenseP;
	Parameter polarityP;
	Parameter eventSubTypeP;
	Parameter genericityP;
	Parameter modalityP;
	// Parameter cilinP;

	double contextOverall;

	HashMap<String, Double> contextPrior;

	int overallGuessPronoun;

	HashMap<Short, Double> pronounPrior;
	HashMap<Integer, HashMap<Short, Integer>> counts;
	HashMap<Integer, Integer> denomCounts;
	HashMap<Integer, HashSet<Integer>> subSpace;

	HashMap<String, Double> fracContextCount;

	@SuppressWarnings("unchecked")
	public ApplyEM(String folder) {
		this.folder = folder;
		try {
			ObjectInputStream modelInput = new ObjectInputStream(
					new FileInputStream("EMModel"));
			tenseP = (Parameter) modelInput.readObject();
			polarityP = (Parameter) modelInput.readObject();
			eventSubTypeP = (Parameter) modelInput.readObject();
			genericityP = (Parameter) modelInput.readObject();
			modalityP = (Parameter) modelInput.readObject();

			fracContextCount = (HashMap<String, Double>) modelInput
					.readObject();
			contextPrior = (HashMap<String, Double>) modelInput.readObject();

			// Context.svoStat = (SVOStat)modelInput.readObject();
			modelInput.close();

			// ObjectInputStream modelInput2 = new ObjectInputStream(
			// new FileInputStream("giga2/EMModel"));
			// tenseP = (Parameter) modelInput2.readObject();
			// polarityP = (Parameter) modelInput2.readObject();
			// eventSubTypeP = (Parameter) modelInput2.readObject();
			// personP = (Parameter) modelInput2.readObject();
			// personQP = (Parameter) modelInput2.readObject();
			// fracContextCount = (HashMap<String, Double>) modelInput2
			// .readObject();
			// contextPrior = (HashMap<String, Double>)
			// modelInput2.readObject();

			// modelInput2.close();
			// loadGuessProb();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static ArrayList<String> goods = new ArrayList<String>();
	public static ArrayList<String> bads = new ArrayList<String>();

	double good = 0;
	double bad = 0;

	public HashMap<String, HashSet<String>> getKeys(ArrayList<EventChain> ecs) {
		HashMap<String, HashSet<String>> key = new HashMap<String, HashSet<String>>();
		for (EventChain ec : ecs) {
			ArrayList<EventMention> ems = ec.getEventMentions();
			Collections.sort(ems);
			for (int i = 1; i < ems.size(); i++) {
				EventMention em2 = ems.get(i);
				HashSet<String> set = new HashSet<String>();
				for (int j = 0; j < i; j++) {
					EventMention em1 = ems.get(j);
					set.add(em1.toName());
				}
				key.put(em2.toName(), set);
			}
		}
		return key;
	}

	public void test() {
		String dataset = "test";
		ArrayList<String> files = Common.getLines("ACE_English_test0");
		HashMap<String, ArrayList<EventMention>> corefResults = new HashMap<String, ArrayList<EventMention>>();

		HashMap<String, HashMap<String, HashSet<String>>> goldKeyses = new HashMap<String, HashMap<String, HashSet<String>>>();

		ArrayList<Integer> lengths = new ArrayList<Integer>();
		ArrayList<String> fileNames = new ArrayList<String>();
		ArrayList<ArrayList<EventChain>> goldEventChains = new ArrayList<ArrayList<EventChain>>();
		ArrayList<ArrayList<EventChain>> systemEventChains = new ArrayList<ArrayList<EventChain>>();

		HashMap<String, HashMap<String, String>> polarityMaps = AttriEvaluate.loadSystemAttri("polarity", "0");
		HashMap<String, HashMap<String, String>> modalityMaps = AttriEvaluate.loadSystemAttri("modality", "0");
		HashMap<String, HashMap<String, String>> genericityMaps = AttriEvaluate.loadSystemAttri("genericity", "0");
		HashMap<String, HashMap<String, String>> tenseMaps = AttriEvaluate.loadSystemAttri("tense", "0");
		
		HashMap<String, HashMap<String, HashMap<String, Double>>> polarityConfMaps = AttriEvaluate.loadSystemAttriWithConf("polarity", "0");
		HashMap<String, HashMap<String, HashMap<String, Double>>> modalityConfMaps = AttriEvaluate.loadSystemAttriWithConf("modality", "0");
		HashMap<String, HashMap<String, HashMap<String, Double>>> genericityConfMaps = AttriEvaluate.loadSystemAttriWithConf("genericity", "0");
		HashMap<String, HashMap<String, HashMap<String, Double>>> tenseConfMaps = AttriEvaluate.loadSystemAttriWithConf("tense", "0");
		
		for (int g = 0; g < files.size(); g++) {
			String file = files.get(g);
			// System.out.println(file);
			ACEDoc doc = new ACEEngDoc(file);
			goldKeyses.put(doc.fileID, this.getKeys(doc.goldEventChains));

			fileNames.add(doc.fileID);
			goldEventChains.add(doc.goldEventChains);
			lengths.add(doc.content.length());

			ArrayList<EventChain> goldChains = doc.goldEventChains;

			HashMap<String, Integer> chainMap = EMUtil.formChainMap(goldChains);

			ArrayList<EventMention> corefResult = new ArrayList<EventMention>();
			corefResults.put(doc.fileID, corefResult);

			ArrayList<EventMention> goldEvents = doc.goldEventMentions;

			for (EventMention m : goldEvents) {
				ArrayList<EventMention> ms = new ArrayList<EventMention>();
				ms.add(m);
			}
			
			Util.setSystemAttribute(goldEvents, polarityMaps, modalityMaps, genericityMaps, tenseMaps, file);
			Util.setSystemAttributeWithConf(goldEvents, polarityConfMaps, modalityConfMaps, genericityConfMaps, tenseConfMaps, file);
			
			Collections.sort(goldEvents);
			for(int i=0;i<goldEvents.size();i++) {
				goldEvents.get(i).sequenceID = i;
			}

			ArrayList<EventMention> candidates = new ArrayList<EventMention>();
			for (EventMention m : goldEvents) {
				candidates.add(m);
			}

			Collections.sort(candidates);

			ArrayList<EventMention> anaphors = new ArrayList<EventMention>();
			for (EventMention m : goldEvents) {
				anaphors.add(m);
			}

			findAntecedent(doc, chainMap, anaphors, candidates, goldKeyses);

			for (EventMention m : anaphors) {
				if (m.antecedent != null) {
					corefResult.add(m);
				}
			}

			ArrayList<EventChain> systemChain = new ArrayList<EventChain>();
			HashMap<String, EventChain> eventChainMap = new HashMap<String, EventChain>();
			Collections.sort(candidates);
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
		// System.out.println("Good: " + good);
		// System.out.println("Bad: " + bad);
		// System.out.println("Precission: " + good / (good + bad) * 100);

		evaluate(corefResults, goldKeyses);

		// System.out.println(ApplyEM.allL);
		// System.out.println(zeroAnt + "/" + allAnt + ":" + zeroAnt / allAnt);
		// System.out.println("Bad_P_C:" + badP_C);

		// output keys
		try {
			ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys."
					+ this.folder, goldEventChains);
			ToSemEval.outputSemFormat(fileNames, lengths, "sys.keys."
					+ this.folder, systemEventChains);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// output goldKeys

	}

	static double min_amongMax = 1;

	static ArrayList<String> goodAnas = new ArrayList<String>();

	static double allAnt = 0;
	static double zeroAnt = 0;

	static double badP_C = 0;

	static HashMap<String, HashSet<String>> chainMaps = new HashMap<String, HashSet<String>>();

	private void findAntecedent(ACEDoc doc, HashMap<String, Integer> chainMap,
			ArrayList<EventMention> anaphors,
			ArrayList<EventMention> allCandidates,
			HashMap<String, HashMap<String, HashSet<String>>> goldKeys) {
		for (EventMention anaphor : anaphors) {
			anaphor.sentenceID = doc.positionMap.get(anaphor.getAnchorStart())[0];
			anaphor.pr = doc.parseReults.get(doc.positionMap.get(anaphor
					.getAnchorStart())[0]);

			EventMention antecedent = null;
			double maxP = 0;
			Collections.sort(allCandidates);

			ArrayList<EventMention> cands = new ArrayList<EventMention>();

			for (int h = allCandidates.size() - 1; h >= 0; h--) {
				EventMention cand = allCandidates.get(h);
				cand.sentenceID = doc.positionMap.get(cand.getAnchorStart())[0];
				cand.pr = doc.parseReults.get(doc.positionMap.get(cand
						.getAnchorStart())[0]);

				if (cand.getAnchorStart() < anaphor.getAnchorEnd()
						&& anaphor.sentenceID - cand.sentenceID <= EMLearn.maxDistance
						&& cand.end != anaphor.end
				// && !predictBadOnes.contains(part.getPartName() + ":" +
				// cand.toName())
				) {

					cands.add(cand);
				}
			}
			EventMention fake = new EventMention();
			fake.extent = "fakkkkke";
			fake.setFake();
			cands.add(fake);

			ResolveGroup rg = new ResolveGroup(anaphor, doc, cands);
			int seq = 0;
			for (EventMention cand : cands) {
				Entry entry = new Entry(cand, null, doc);
				rg.entries.add(entry);
				entry.p_c = EMUtil.getP_C(cand, anaphor, doc);
				if (entry.p_c != 0) {
					seq += 1;
				}

				if (!chainMaps.containsKey(entry.antName) && !entry.isFake) {
					HashSet<String> set = new HashSet<String>();
					set.add(entry.antName);
					chainMaps.put(entry.antName, set);
				}
			}
			for (Entry entry : rg.entries) {
				if (entry.isFake) {
					entry.p_c = Entry.p_fake_decay / (Entry.p_fake_decay + seq);
				} else if (entry.p_c != 0) {
					entry.p_c = 1 / (Entry.p_fake_decay + seq);
				}
			}

			// ArrayList<Entry> goodEntries = new ArrayList<Entry>();
			// ArrayList<Entry> fakeEntries = new ArrayList<Entry>();
			// ArrayList<Entry> badEntries = new ArrayList<Entry>();
			// for (int i = 0; i < rg.entries.size(); i++) {
			// Entry entry = rg.entries.get(i);
			// if (entry.isFake) {
			// fakeEntries.add(entry);
			// } else if (entry.ant.head.contains(anaphor.head)) {
			// goodEntries.add(entry);
			// } else {
			// badEntries.add(entry);
			// }
			// }
			// ArrayList<Entry> allEntries = new ArrayList<Entry>();
			// allEntries.addAll(goodEntries);
			// allEntries.addAll(fakeEntries);
			// allEntries.addAll(badEntries);
			// for (int i = 0; i < allEntries.size(); i++) {
			// allEntries.get(i).seq = i;
			// }

			EMLearn.sortEntries(rg, chainMaps);

			double probs[] = new double[cands.size()];

			ArrayList<EventMention> goldCorefs = new ArrayList<EventMention>();

			for (int i = 0; i < rg.entries.size(); i++) {
				Entry entry = rg.entries.get(i);
				EventMention cand = cands.get(i);

				boolean coref = chainMap.containsKey(anaphor.toName())
						&& chainMap.containsKey(cand.toName())
						&& chainMap.get(anaphor.toName()).intValue() == chainMap
								.get(cand.toName()).intValue();
				Context.coref = coref;
				Context.gM1 = chainMap.containsKey(cand.toName());
				Context.gM2 = chainMap.containsKey(anaphor.toName());
				entry.context = Context.buildContext(cand, anaphor, doc, cands,
						entry.seq);

				allAnt++;
				if (entry.p_c == 0) {
					if (coref) {
						badP_C++;
					}
					zeroAnt++;
				}
				if (coref && entry.p_c != 0) {
					goldCorefs.add(cand);
				}
			}

			// TODO
			String antName = "";
			if (anaphor.antecedent == null)
				for (int i = 0; i < rg.entries.size(); i++) {
					Entry entry = rg.entries.get(i);
					EventMention cand = entry.ant;
					Context context = entry.context;

					// if(entry.p_c!=0) {
					// entry.p_c = 1.0/(seq+1);
					// }

					// calculate P(overt-pronoun|ant-context)
					// if(entry.p_c!=0) {
					// antecedent = cand;
					// break;
					// }
					// if(entry.p_c==0 && coref &&
					// !cand.head.equals(anaphor.head)) {
					// System.out.println(coref);
					// print(cand, anaphor, part, chainMap);
					// }

					double p_tense = tenseP.getVal(entry.ant.tense,
							anaphor.tense);
					
					double p_modality = modalityP.getVal(entry.ant.modality,
							anaphor.modality);

					double p_polarity = polarityP.getVal(entry.ant.modality,
							anaphor.modality);
					
					
					double p_genericity = genericityP.getVal(
							entry.ant.genericity, anaphor.genericity);

					double p_eventSubType = eventSubTypeP.getVal(
							entry.ant.subType, anaphor.subType);

					
					double p_context = 0.5;
					if (fracContextCount.containsKey(context.toString())) {
						p_context = (1.0 * EMUtil.alpha + fracContextCount
								.get(context.toString()))
								/ (2.0 * EMUtil.alpha + contextPrior
										.get(context.toString()));
					} else {
						p_context = 1.0 / 2;
					}

					double p2nd = p_context * entry.p_c;
					p2nd *= 1 * p_tense * p_polarity 
//							* p_eventSubType
							* p_genericity * p_modality;
					double p = p2nd;
					probs[i] = p;
					if (p > maxP && p != 0) {
						antecedent = cand;
						maxP = p;
						antName = entry.antName;
					}
				}

			// if (antecedent != null && antecedent.isFake) {
			// if (goldCorefs.size() != 0) {
			// // anaphor.antecedent= goldCorefs.get(0);
			// System.out.println("Anaphor: " + anaphor.extent + " "
			// + anaphor.ACEType + " # " + anaphor.ACESubtype
			// + " # " + chainMap.containsKey(anaphor.toName()));
			// System.out.println("Selected: " + antecedent.extent + " "
			// + antecedent.ACEType + " # "
			// + antecedent.ACESubtype + " # "
			// + chainMap.containsKey(antecedent.toName()));
			// System.out.println("True Ante: ");
			// for (Mention m : goldCorefs) {
			// System.out.println(m.extent + " " + m.ACEType + " # "
			// + m.ACESubtype);
			// }
			// System.out.println("---------------------------");
			// }
			// }

			if (antecedent != null && !antecedent.isFake()
					&& anaphor.antecedent == null) {
				HashSet<String> corefs = chainMaps.get(antName);
				corefs.add(rg.anaphorName);
				chainMaps.put(rg.anaphorName, corefs);

				anaphor.antecedent = antecedent;

				boolean coref = chainMap.containsKey(anaphor.toName())
						&& chainMap.containsKey(antecedent.toName())
						&& chainMap.get(anaphor.toName()).intValue() == chainMap
								.get(antecedent.toName()).intValue();

				if (!coref && goldCorefs.size() != 0) {
					// anaphor.antecedent= goldCorefs.get(0);
					// System.out.println("Anaphor: " + anaphor.getAnchor() +
					// " "
					// + anaphor.getType() + " # "
					// + " # " + chainMap.containsKey(anaphor.toName()));
					// System.out.println("Selected: " + antecedent.extent + " "
					// + antecedent.getType() + " # "
					// + chainMap.containsKey(antecedent.toName()));
					// System.out.println("True Ante: ");
					// for (EventMention m : goldCorefs) {
					// }
					// System.out.println("---------------------------");
					// print(antecedent, anaphor, part, chainMap);
				}

				if (!coref && goldCorefs.size() == 0) {
					// anaphor.antecedent= null;
					// System.out.println("Anaphor: " + anaphor.getAnchor() +
					// " "
					// + anaphor.getType()
					// + " # " + chainMap.containsKey(anaphor.toName()));
					// System.out.println("Selected: " + antecedent.getAnchor()
					// + " "
					// + antecedent.getType() + " # "
					// + chainMap.containsKey(antecedent.toName()));
					// System.out.println("True Ante: EMPTY");
					// System.out.println("---------------------------");
					// print(antecedent, anaphor, part, chainMap);
				}

			}
		}
	}

	public static void print(EventMention antecedent, EventMention anaphor,
			ACEDoc doc, HashMap<String, Integer> chainMap) {
		System.out.println(antecedent.extent + " # "
				+ chainMap.containsKey(antecedent.toName()));
		System.out.println(anaphor.extent + " # "
				+ chainMap.containsKey(anaphor.toName()));
		System.out.println("----");
	}

	static int allL = 0;

	static String prefix = "/shared/mlrdir1/disk1/mlr/corpora/CoNLL-2012/conll-2012-train-v0/data/files/data/chinese/annotations/";
	static String anno = "annotations/";
	static String suffix = ".coref";

	// private static ArrayList<Mention> getGoldNouns(ArrayList<Entity>
	// entities,
	// CoNLLPart goldPart) {
	// ArrayList<Mention> goldAnaphors = new ArrayList<Mention>();
	// for (Entity e : entities) {
	// Collections.sort(e.mentions);
	// for (int i = 1; i < e.mentions.size(); i++) {
	// Mention m1 = e.mentions.get(i);
	// String pos1 = goldPart.getWord(m1.end).posTag;
	// if (pos1.equals("PN") || pos1.equals("NR") || pos1.equals("NT")) {
	// continue;
	// }
	// goldAnaphors.add(m1);
	// }
	// }
	// Collections.sort(goldAnaphors);
	// for (Mention m : goldAnaphors) {
	// EMUtil.setMentionAttri(m, goldPart);
	// }
	// return goldAnaphors;
	// }
	//
	// private static ArrayList<Mention> getGoldAnaphorNouns(
	// ArrayList<Entity> entities, CoNLLPart goldPart) {
	// ArrayList<Mention> goldAnaphors = new ArrayList<Mention>();
	// for (Entity e : entities) {
	// Collections.sort(e.mentions);
	// for (int i = 1; i < e.mentions.size(); i++) {
	// Mention m1 = e.mentions.get(i);
	// String pos1 = goldPart.getWord(m1.end).posTag;
	// if (pos1.equals("PN") || pos1.equals("NR") || pos1.equals("NT")) {
	// continue;
	// }
	// HashSet<String> ants = new HashSet<String>();
	// for (int j = i - 1; j >= 0; j--) {
	// Mention m2 = e.mentions.get(j);
	// String pos2 = goldPart.getWord(m2.end).posTag;
	// if (!pos2.equals("PN") && m1.end != m2.end) {
	// ants.add(m2.toName());
	// }
	// }
	// if (ants.size() != 0) {
	// goldAnaphors.add(m1);
	// }
	// }
	// }
	// Collections.sort(goldAnaphors);
	// for (Mention m : goldAnaphors) {
	// EMUtil.setMentionAttri(m, goldPart);
	// }
	// return goldAnaphors;
	// }

	public static void evaluate(
			HashMap<String, ArrayList<EventMention>> anaphorses,
			HashMap<String, HashMap<String, HashSet<String>>> goldKeyses) {
		double gold = 0;
		double system = 0;
		double hit = 0;

		for (String key : anaphorses.keySet()) {
			// System.out.println(key);
			// System.out.println(goldKeyses.keySet().iterator().next());

			ArrayList<EventMention> anaphors = anaphorses.get(key);
			HashMap<String, HashSet<String>> keys = goldKeyses.get(key);
			gold += keys.size();
			system += anaphors.size();
			for (EventMention anaphor : anaphors) {
				EventMention ant = anaphor.antecedent;
				if (keys.containsKey(anaphor.toName())
						&& keys.get(anaphor.toName()).contains(ant.toName())) {
					hit++;
				}
			}
		}

		double r = hit / gold;
		double p = hit / system;
		double f = 2 * r * p / (r + p);
		System.out.println("============");
		System.out.println("Hit: " + hit);
		System.out.println("Gold: " + gold);
		System.out.println("System: " + system);
		System.out.println("============");
		System.out.println("Recall: " + r * 100);
		System.out.println("Precision: " + p * 100);
		System.out.println("F-score: " + f * 100);
	}

	public static void main(String args[]) {
		if (args.length != 1) {
			System.err.println("java ~ folder");
			System.exit(1);
		}
		// EMUtil.loadAlign();
		run(args[0]);
	}

	public static void run(String folder) {
		EMUtil.train = false;
		ApplyEM test = new ApplyEM(folder);
		test.test();
		System.out.println("RUNN: " + folder);
		// Common.pause("!!#");
	}
}
