package coref;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import model.ACEChiDoc;
import model.ACEDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;
import coref.ResolveGroup.Entry;

public class ApplyEM {

	String folder;

	Parameter tenseP;
	Parameter polarityP;
	Parameter triggerP;
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

	HashMap<String, Double> tensePrior;
	HashMap<String, Double> polarityPrior;
	HashMap<String, Double> genericityPrior;
	HashMap<String, Double> modalityPrior;
	
	static ArrayList<HashMap<String, Double>> multiFracContextsProbl0;
	static ArrayList<HashMap<String, Double>> multiFracContextsProbl1;
	
	static double pl0 = 0;
	static double pl1 = 0;
	
	@SuppressWarnings("unchecked")
	public ApplyEM(String folder) {
		this.folder = folder;
		System.out.println("Read Model...");
		try {
			ObjectInputStream modelInput = new ObjectInputStream(
					new FileInputStream("EMModel" + Util.part));
			tenseP = (Parameter) modelInput.readObject();
			polarityP = (Parameter) modelInput.readObject();
			triggerP = (Parameter) modelInput.readObject();
			genericityP = (Parameter) modelInput.readObject();
			modalityP = (Parameter) modelInput.readObject();
			
			tensePrior = (HashMap<String, Double>) modelInput.readObject();
			polarityPrior = (HashMap<String, Double>) modelInput.readObject();
			genericityPrior = (HashMap<String, Double>) modelInput.readObject();
			modalityPrior = (HashMap<String, Double>) modelInput.readObject();

			fracContextCount = (HashMap<String, Double>) modelInput
					.readObject();
			contextPrior = (HashMap<String, Double>) modelInput.readObject();

			multiFracContextsProbl0 = (ArrayList<HashMap<String, Double>>)modelInput.readObject();
			multiFracContextsProbl1 = (ArrayList<HashMap<String, Double>>)modelInput.readObject();
			pl0 = (Double)modelInput.readObject();
			pl1 = (Double)modelInput.readObject();
			
			// Context.svoStat = (SVOStat)modelInput.readObject();
			modelInput.close();
			System.out.println("Finish Reading Model..");
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
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + Util.part);
		HashMap<String, ArrayList<EventMention>> corefResults = new HashMap<String, ArrayList<EventMention>>();

		HashMap<String, HashMap<String, HashSet<String>>> goldKeyses = new HashMap<String, HashMap<String, HashSet<String>>>();

		ArrayList<Integer> lengths = new ArrayList<Integer>();
		ArrayList<String> fileNames = new ArrayList<String>();
		ArrayList<ArrayList<EventChain>> goldEventChains = new ArrayList<ArrayList<EventChain>>();
		ArrayList<ArrayList<EventChain>> systemEventChains = new ArrayList<ArrayList<EventChain>>();

//		HashMap<String, HashMap<String, String>> polarityMaps = AttriEvaluate.loadSystemAttri("polarity", Util.part);
//		HashMap<String, HashMap<String, String>> modalityMaps = AttriEvaluate.loadSystemAttri("modality", Util.part);
//		HashMap<String, HashMap<String, String>> genericityMaps = AttriEvaluate.loadSystemAttri("genericity", Util.part);
//		HashMap<String, HashMap<String, String>> tenseMaps = AttriEvaluate.loadSystemAttri("tense", Util.part);
//		
//		HashMap<String, HashMap<String, HashMap<String, Double>>> polarityConfMaps = AttriEvaluate.loadSystemAttriWithConf("polarity", "0");
//		HashMap<String, HashMap<String, HashMap<String, Double>>> modalityConfMaps = AttriEvaluate.loadSystemAttriWithConf("modality", "0");
//		HashMap<String, HashMap<String, HashMap<String, Double>>> genericityConfMaps = AttriEvaluate.loadSystemAttriWithConf("genericity", "0");
//		HashMap<String, HashMap<String, HashMap<String, Double>>> tenseConfMaps = AttriEvaluate.loadSystemAttriWithConf("tense", "0");
//		HashMap<String, HashMap<String, EventMention>> jointSVMLines = EngArgEval.jointSVMLine();
		
		for (int g = 0; g < files.size(); g++) {
			String file = files.get(g);
			// System.out.println(file);
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = g;
			
			goldKeyses.put(doc.fileID, this.getKeys(doc.goldEventChains));

			fileNames.add(doc.fileID);
			goldEventChains.add(doc.goldEventChains);
			lengths.add(doc.content.length());

			ArrayList<EventChain> goldChains = doc.goldEventChains;

			HashMap<String, Integer> chainMap = EMUtil.formChainMap(goldChains);

			ArrayList<EventMention> corefResult = new ArrayList<EventMention>();
			corefResults.put(doc.fileID, corefResult);

			ArrayList<EventMention> events = Util.loadSystemComponents(doc);
//			Util.setSystemAttribute(events, polarityMaps, modalityMaps, genericityMaps, tenseMaps, file);
//			ArrayList<EventMention> events = doc.goldEventMentions;
//			Util.assignArgumentWithEntityMentions(doc.goldEventMentions,
//					doc.goldEntityMentions, doc.goldValueMentions,
//					doc.goldTimeMentions, doc);
				
			Collections.sort(events);
			
			for(int i=0;i<events.size();i++) {
				events.get(i).sequenceID = i;
			}

			ArrayList<EventMention> candidates = new ArrayList<EventMention>();
			for (EventMention m : events) {
				EMLearn.trs.add(m.getAnchor());
				candidates.add(m);
			}

			Collections.sort(candidates);

			ArrayList<EventMention> anaphors = new ArrayList<EventMention>();
			for (EventMention m : events) {
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
					+ Util.part, goldEventChains);
			ToSemEval.outputSemFormat(fileNames, lengths, "sys.keys."
					+ Util.part, systemEventChains);
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
						&& cand.getAnchorEnd() != anaphor.getAnchorEnd()
				// && !predictBadOnes.contains(part.getPartName() + ":" +
				// cand.toName())
				) {
					cands.add(cand);
				}
			}
			EventMention fake = new EventMention();
			fake.extent = "fakkkkke";
			fake.setAnchor("Fake");
			fake.setFake();
			cands.add(fake);
			fake.setSubType("null");

			ResolveGroup rg = new ResolveGroup(anaphor, doc, cands);
			int norm = 0;
			for (EventMention cand : cands) {
				Entry entry = new Entry(cand, null, doc);
				rg.entries.add(entry);
				entry.p_c = EMUtil.getP_C(cand, anaphor, doc);
//				if (entry.p_c != 0) {
					norm += entry.p_c;
//				}

				if (!chainMaps.containsKey(entry.antName) && !entry.isFake) {
					HashSet<String> set = new HashSet<String>();
					set.add(entry.antName);
					chainMaps.put(entry.antName, set);
				}
			}
			for (Entry entry : rg.entries) {
				if (entry.isFake) {
					entry.p_c = Entry.p_fake_decay / (Entry.p_fake_decay + norm);
				} else if (entry.p_c != 0) {
					entry.p_c = 1 / (Entry.p_fake_decay + norm);
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
				
				double P_anchor_fake = 0;
				double all = 0;
				for(Entry entry : rg.entries) {
					if(entry.isFake || entry.p_c==0) {
						continue;
					}
					all += 1;
					P_anchor_fake += triggerP.getVal(
						entry.ant.getAnchor(), rg.m.getAnchor());
				}
				if(all==0) {
					P_anchor_fake = 0;
				} else {
					P_anchor_fake = P_anchor_fake/all;
				}
				
				
				for (int i = 0; i < rg.entries.size(); i++) {
					Entry entry = rg.entries.get(i);
					EventMention cand = entry.ant;
					Context context = entry.context;

					boolean coref = chainMap.containsKey(anaphor.toName())
							&& chainMap.containsKey(cand.toName())
							&& chainMap.get(anaphor.toName()).intValue() == chainMap
									.get(cand.toName()).intValue();
					Context.coref = coref;
					if(coref && Context.extraRole(cand, anaphor)) {
//						System.out.println(cand.getAnchor());
//						for(EventMentionArgument arg : cand.getEventMentionArguments()) {
//							System.out.println(arg.getRole() + " # " + arg.mention.head);
//						}
//						System.out.println("---");
//						System.out.println(anaphor.getAnchor());
//						for(EventMentionArgument arg : anaphor.getEventMentionArguments()) {
//							System.out.println(arg.getRole() + " # " + arg.mention.head);
//						}
//						System.out.println("=====================");
					}
					
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

					double p_tense = tenseP.getVal(entry.ant.tense, anaphor.tense);
					double p_modality = modalityP.getVal(entry.ant.modality, anaphor.modality);
					double p_polarity = polarityP.getVal(entry.ant.polarity, anaphor.polarity);
					double p_genericity = genericityP.getVal(entry.ant.genericity, anaphor.genericity);
					
//					double p_tense = this.getProb(EMUtil.Tense, cand.tenseConf, anaphor.tenseConf, tenseP, tensePrior);
//					double p_modality = this.getProb(EMUtil.Modality, cand.modalityConf, anaphor.modalityConf, modalityP, modalityPrior);
//					double p_polarity = this.getProb(EMUtil.Polarity, cand.polarityConf, anaphor.polarityConf, polarityP, polarityPrior);
//					double p_genericity = this.getProb(EMUtil.Genericity, cand.genericityConf, anaphor.genericityConf, genericityP, genericityPrior);
					
//					if(p_tense!=p_tense2) {
//						System.out.println(cand.isFake() + "tense: " + p_tense + "\t" + p_tense2);
//					}
//					if(p_modality!=p_modality2) {
//						System.out.println(cand.isFake() + "modality: " + p_modality + "\t" + p_modality2);	
//					}
//					if(p_polarity!=p_polarity2) {
//						System.out.println(cand.isFake() + "polarity: " + p_polarity + "\t" + p_polarity2);	
//					}
//					if(p_genericity!=p_genericity2) {
//						System.out.println(cand.isFake() + "genericity: " + p_genericity + "\t" + p_genericity2);	
//					}
//					System.out.println(cand.isFake() + "tense: " + p_tense + "\t" + p_tense2);
//					System.out.println(cand.isFake() + "modality: " + p_modality + "\t" + p_modality2);
//					System.out.println(cand.isFake() + "polarity: " + p_polarity + "\t" + p_polarity2);	
//					System.out.println(cand.isFake() + "genericity: " + p_genericity + "\t" + p_genericity2);
//					System.out.println("===========");
					
					double p_anchor = 0;
					if(entry.isFake){
						p_anchor = P_anchor_fake;
					} else {
						p_anchor = triggerP.getVal(
							entry.ant.getAnchor(), anaphor.getAnchor());
					}
					double p_context = 0.5;
					if (fracContextCount.containsKey(context.toString())) {
						p_context = (1.0 * EMUtil.alpha + fracContextCount
								.get(context.toString()))
								/ (2.0 * EMUtil.alpha + contextPrior
										.get(context.toString()));
					} else {
						p_context = 1.0 / 2;
					}

					double p_context_l1 = pl1;
					double p_context_l0 = pl0;
					for(int g=0;g<Context.getSubContext().size();g++) {
						int pos[] = Context.getSubContext().get(g);
						String key = context.getKey(g);
						if(multiFracContextsProbl1.get(g).containsKey(key)) {
							p_context_l1 *= multiFracContextsProbl1.get(g).get(key);
						} else {
							p_context_l1 *= Context.normConstant.get(g);
						}
						
						if(multiFracContextsProbl0.get(g).containsKey(key)) {
							p_context_l0 *= multiFracContextsProbl0.get(g).get(key);
						} else {
							p_context_l0 *= Context.normConstant.get(g);
						}
					}
					
					p_context = p_context_l1/(p_context_l1 + p_context_l0);
					
					double p2nd = p_context
							* entry.p_c
							* p_anchor
							;
//					p2nd *= 1 * p_tense 
//							* p_polarity 
////							* p_eventSubType
//							* p_genericity * p_modality
//							;
					double p = p2nd;
					probs[i] = p;
					if (p > maxP && p != 0) {
						antecedent = cand;
						maxP = p;
						antName = entry.antName;
					}
					
//					if (
//							!Util.highPrecissionNegativeConstraint(cand, rg.m) && 
//							(cand.getAnchor().equalsIgnoreCase(rg.m.getAnchor()) ||
//									Util._commonBV_(cand, rg.m))
//							) {
//						antecedent = cand;
//						antName = entry.antName;
//						break;
//					}
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
//			antecedent = fake;
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

				if(!coref) {
//					System.out.println(antecedent.getAnchor() + " @ " + chainMap.containsKey(antecedent.toName()));
//					for(EventMentionArgument arg : antecedent.getEventMentionArguments()) {
//						System.out.println(arg.getRole() + " # " + arg.mention.head);
//					}
//					System.out.println("---");
//					System.out.println(anaphor.getAnchor() + " @ " + chainMap.containsKey(anaphor.toName()));
//					for(EventMentionArgument arg : anaphor.getEventMentionArguments()) {
//						System.out.println(arg.getRole() + " # " + arg.mention.head);
//					}
//					System.out.println("=====================");
					
				}
				
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
//					 anaphor.antecedent= null;
//					 System.out.println("Anaphor: " + anaphor.getAnchor() +
//					 " "
//					 + anaphor.getType()
//					 + " # " + chainMap.containsKey(anaphor.toName()));
//					 System.out.println("Selected: " + antecedent.getAnchor()
//					 + " "
//					 + antecedent.getType() + " # "
//					 + chainMap.containsKey(antecedent.toName()));
//					 System.out.println("True Ante: EMPTY");
//					 System.out.println("---------------------------");
//					 print(antecedent, anaphor, part, chainMap);
				}

			}
		}
	}
	
	private double getProb(ArrayList<String> attris, HashMap<String, Double> candConf, HashMap<String, Double> anaConf, 
			Parameter parameter, HashMap<String, Double> prior) {
		System.out.println(candConf);
		
		double p_cand_tense = 0;
		for(String tc : attris) {
			double p1 = 0;
			double p2 = 0;
			if(candConf.containsKey(tc)) {
				p1 = candConf.get(tc);
			}
			if(prior.containsKey(tc)) {
				p2 = prior.get(tc);
			}
			p_cand_tense += p1 * p2;
		}
		
		double p = 0;
		for(String ta : attris.subList(0, attris.size()-1)) {
			double p_ana_tense = 0;
			if(anaConf.containsKey(ta)) {
				p_ana_tense = anaConf.get(ta);
			}
			
			double p_ta_cand = 0;
			
			for(String tc : attris) {
				double p1 = 0;
				double p2 = 0;
				double p3 = 0;
				
				if(candConf.containsKey(tc)) {
					p1 = candConf.get(tc);
				}
				p2 = parameter.getVal(tc, ta);
				
				if(prior.containsKey(tc)) {
					p3 = prior.get(tc);
				}
				p_ta_cand += p1 * p2 * p3;
			}
			
			p += p_ana_tense*p_ta_cand/p_cand_tense;
		}
		return p;
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
		Util.part = args[0];
		// EMUtil.loadAlign();
		run(args[0]);
	}

	public static void run(String folder) {
		Util.polarityMaps = null;
		Util.tenseMaps = null;
		Util.generecityMaps = null;
		Util.modalityMaps = null;
		
		EMUtil.train = false;
		ApplyEM test = new ApplyEM(folder);
		test.test();
		System.out.println("RUNN: " + folder);
		// Common.pause("!!#");
	}
}
