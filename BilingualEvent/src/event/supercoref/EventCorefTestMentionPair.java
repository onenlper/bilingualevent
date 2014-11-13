package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;

import coref.ToSemEval;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Distribution;

public class EventCorefTestMentionPair {

	public static void main(String args[]) throws Exception {
		LinearClassifier<String, String> classifier = LinearClassifier
				.readClassifier("stanfordClassifier.gz");

		ArrayList<String> files = Common.getLines("ACE_English_test0");
		EventCorefFea fea = new EventCorefFea(false, "corefFea");
		double thres = .3;
		ArrayList<ArrayList<EventChain>> answers = new ArrayList<ArrayList<EventChain>>(); 
		ArrayList<String> fileNames= new ArrayList<String>();
		ArrayList<Integer> lengths = new ArrayList<Integer>();
		for (String file : files) {
			ACEDoc doc = new ACEEngDoc(file);
			fileNames.add(doc.fileID);
			lengths.add(doc.content.length());
			
			ArrayList<EventMention> ems = doc.goldEventMentions;

			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();
			Collections.sort(ems);

			ArrayList<EventChain> activeChains2 = new ArrayList<EventChain>();
			
			for (int i = 0; i < ems.size(); i++) {
				EventMention ana = ems.get(i);

				int corefID = -1;
				double maxVal = 0;
				for (int j = activeChains2.size() - 1; j >= 0; j--) {
					EventChain ec = activeChains2.get(j);

					fea.configure(ec, ana, doc, ems);
					String feaStr = fea.getSVMFormatString();

					double val = test(feaStr, classifier);
					if (val > thres && val > maxVal) {
						maxVal = val;
						corefID = j;
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
		}
		ToSemEval.outputSemFormat(fileNames, lengths, "baseline.keys.all", answers);
	}

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
