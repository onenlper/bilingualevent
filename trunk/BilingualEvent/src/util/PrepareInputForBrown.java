package util;

import java.util.ArrayList;

import model.ACEChiDoc;
import model.ACEDoc;
import model.ParseResult;

public class PrepareInputForBrown {

	public static void main(String args[]) {
		ArrayList<String> files = Common.getLines("ACE_Chinese_train0");
		ArrayList<String> output = new ArrayList<String>();
		for(String file : files) {
			ACEDoc doc = new ACEChiDoc(file);
			for(ParseResult pr : doc.parseReults) {
				StringBuilder sb = new StringBuilder();
				for(int i=1;i<pr.words.size();i++) {
					sb.append(pr.words.get(i)).append(" ");
				}
				output.add(sb.toString());
			}
		}
		Common.outputLines(output, "brown_input.txt");
	}
}
