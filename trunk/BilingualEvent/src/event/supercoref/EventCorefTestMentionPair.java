package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;

import model.ACEChiDoc;
import model.ACEDoc;
import model.Entity;
import model.EventChain;
import model.EventMention;
import util.Common;
import util.Util;
import coref.ToSemEval;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Distribution;

public class EventCorefTestMentionPair {

	public static void main(String args[]) throws Exception {
		if(args.length==1) {
			System.err.println("java ~ part [maxent|prepare|load]");
			Common.bangErrorPOS("");
		}
		run(args);
	}

	private static void run(String[] args) throws Exception {
		Util.part = args[0];
		probs.clear();
		
		LinearClassifier<String, String> classifier = null;
		if(args[1].equals("maxent")) {
			classifier = LinearClassifier
				.readClassifier("stanfordClassifier" + args[0] + ".gz");
		}
		ArrayList<String> predicts = new ArrayList<String>();
		if(args[1].equals("load")) {
			predicts = Common.getLines("eventPredict" + args[0]);
		}
		
		ArrayList<String> files = Common.getLines("ACE_Chinese_test" + args[0]);
		EventCorefFea fea = new EventCorefFea(false, "corefFea" + args[0]);
		double thres = .2;
		
//		double thres = .2;
		ArrayList<String> testLines = new ArrayList<String>();
		
		ArrayList<ArrayList<EventChain>> answers = new ArrayList<ArrayList<EventChain>>(); 
		ArrayList<ArrayList<EventChain>> goldKeys = new ArrayList<ArrayList<EventChain>>(); 
		
		ArrayList<String> fileNames= new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		for (int t=0;t<files.size();t++) {
			String file = files.get(t);
			ACEDoc doc = new ACEChiDoc(file);
			doc.docID = t;
			
			fileNames.add(doc.fileID);
			lengths.add(doc.content.length());
			
//			ArrayList<EventMention> events = doc.goldEventMentions;
			ArrayList<EventMention> events = Util.loadSystemComponents(doc);

			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();
			Collections.sort(events);

			ArrayList<EventChain> activeChains2 = new ArrayList<EventChain>();
			
			for (int i = 0; i < events.size(); i++) {
				EventMention ana = events.get(i);

				int corefID = -1;
				double maxVal = 0;
				for (int j = activeChains2.size() - 1; j >= 0; j--) {
					EventChain ec = activeChains2.get(j);

					fea.configure(ec, ana, doc, events);

					double val = 0;
					
					if(args[1].equals("maxent")) {
						String feaStr = fea.getSVMFormatString();
						val = test(feaStr, classifier);
					} else if(args[1].equals("load")) {
						String line = predicts.remove(0);
						String tks[] = line.split("\\s+");
						val = Double.parseDouble(tks[1]);
					} else if(args[1].equals("prepare")) {
						String feaStr = fea.getSVMFormatString();
						testLines.add("1 " + feaStr);
					}
					
					probs.add(file + " " + ec.getEventMentions().get(0).toName() + " " + ana.toName() + " " + val);
					
					if (val > thres && val > maxVal) {
						maxVal = val;
						corefID = j;
//						break;
					}
				}

				if (corefID == -1) {
					EventChain ec = new EventChain();
					ec.addEventMention(ana);
					activeChains.add(ec);
				} else {
					for(int g=0;g<activeChains.size();g++) {
						EventChain chain = activeChains.get(g);
						for(int h=0;h<chain.getEventMentions().size();h++) {
							EventMention m = chain.getEventMentions().get(h);
							if(m==activeChains2.get(corefID).getEventMentions().get(0)) {
								chain.addEventMention(ana);
							}
						}
					}
				}
				EventChain ec = new EventChain();
				ec.addEventMention(ana);
				activeChains2.add(ec);
			}
			// process activeChains
			answers.add(activeChains);
			goldKeys.add(doc.goldEventChains);
		}
		
		if(!args[1].equals("prepare")) {
			ToSemEval.outputSemFormat(fileNames, lengths, "gold.keys." + args[0], goldKeys);
			ToSemEval.outputSemFormat(fileNames, lengths, "baselineMP.keys." + args[0], answers);
			Common.outputLines(probs, "eventProbs" + args[0]);
		} else {
			Common.outputLines(testLines, "eventTest" + args[0]);
		}
	}

	static ArrayList<String> probs = new ArrayList<String>();
	
	public static double test(String str,
			LinearClassifier<String, String> classifier) {

		Datum<String, String> testIns = Dataset.svmLightLineToDatum(str);
		Counter<String> scores = classifier.scoresOf(testIns);
		Distribution<String> distr = Distribution
				.distributionFromLogisticCounter(scores);
		double prob = distr.getCount("+1");
		return prob;
	}
}
