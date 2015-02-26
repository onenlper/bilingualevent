package event.preProcess;

import java.util.ArrayList;
import java.util.HashSet;

import entity.semantic.ACECommon;

import model.ACEChiDoc;
import model.EventMention;
import model.ParseResult;
import model.stanford.StanfordResult;
import model.stanford.StanfordXMLReader;
import seeds.SeedUtil;
import util.ChineseUtil;
import util.Common;
import util.Util;

public class KnownTriggerSeed {
	public static void main(String args[]) {
		if (args.length != 1) {
			System.out.println("java ~ [folder]");
			System.exit(1);
		}
		Util.part = args[0];
		System.out.println("Build errata table...\n===========");

		HashSet<String> errataTable1 = new HashSet<String>();
		HashSet<String> errataTable2 = new HashSet<String>();

		String content = SeedUtil.getContent();
		StanfordResult sr = StanfordXMLReader.read("sents.xml");
		ArrayList<ParseResult> parseResults = ACECommon.standford2ParseResult(
				sr, content);

		ArrayList<EventMention> mentions = SeedUtil.getGoldEventMentions();

		for (EventMention mention : mentions) {
			int position[] = ChineseUtil.findParseFilePosition(
					mention.getAnchorStart(), mention.getAnchorEnd(), content,
					parseResults);

			ArrayList<String> words = parseResults.get(position[0]).words;
			String word = words.get(position[1]);
			if (position[1] == position[3] && position[2] == 0
					&& position[4] == word.length() - 1) {

			} else if (position[1] == position[3]) {
				// System.out.println("Inconsistent: " + mention.getAnchor() +
				// "&" + word);
				errataTable1.add(mention.getAnchor() + "_" + word);
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append(mention.getAnchor());
				for (int i = position[1]; i <= position[3]; i++) {
					sb.append("_").append(words.get(i));
				}
				errataTable2.add(sb.toString());
			}
			// trigger.add(mention.getAnchor().replaceAll("\\s+", ""));
		}

		// Common.outputHashSet(trigger, "chinese_trigger_known");
		Common.outputHashSet(errataTable1, "errata1" + Util.part);
		Common.outputHashSet(errataTable2, "errata2" + Util.part);
	}
}