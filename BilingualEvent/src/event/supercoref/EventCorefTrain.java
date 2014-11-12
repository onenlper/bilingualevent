package event.supercoref;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import model.ACEDoc;
import model.ACEEngDoc;
import model.EventChain;
import model.EventMention;
import util.Common;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.Datum;

public class EventCorefTrain {

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_English_train0");
		EventCorefFea fea = new EventCorefFea(true, "corefFea");
		ArrayList<String> trainLines = new ArrayList<String>();
		
		List<Datum<String, String>> trainingData = new ArrayList<Datum<String, String>>();
		
		for(String file : files) {
			ACEDoc doc = new ACEEngDoc(file);
			ArrayList<EventMention> ems = doc.goldEventMentions;
			ArrayList<EventChain> activeChains = new ArrayList<EventChain>();
			Collections.sort(ems);
			
			for(int i=0;i<ems.size();i++) {
				EventMention ana = ems.get(i);
				
				int corefID = -1;
				for(int j=activeChains.size()-1;j>=0;j--) {
					EventChain ec = activeChains.get(j);
					EventMention lem = ec.getEventMentions().get(ec.getEventMentions().size()-1);
					
					boolean coref = doc.eventCorefMap.get(ana.toName())==doc.eventCorefMap.get(lem.toName());
					if(coref) {
						corefID = j;
					}
					fea.configure(ec, ana, doc, ems);
					
					String feaStr = fea.getSVMFormatString();
					String svm = "";
					if(coref) {
						svm = "+1 " + feaStr;	
					} else {
						svm = "-1 " + feaStr;
					}
					trainingData.add(Dataset.svmLightLineToDatum(svm));
					trainLines.add(svm);
				}
					
				if(corefID==-1) {
					EventChain ec = new EventChain();
					ec.addEventMention(ana);
					activeChains.add(ec);
				} else {
					activeChains.get(corefID).addEventMention(ana);
				}
			}
		}
		fea.freeze();
		Common.outputLines(trainLines, "eventTrain");
		
		LinearClassifierFactory<String, String> factory = new LinearClassifierFactory<String, String>();
		factory.useConjugateGradientAscent();
		// Turn on per-iteration convergence updates
		factory.setVerbose(false);
		// Small amount of smoothing
		factory.setSigma(10);

		LinearClassifier<String, String> classifier = factory
				.trainClassifier(trainingData);
//		classifier.dump();
		LinearClassifier.writeClassifier(classifier, "stanfordClassifier.gz");
	}
}
