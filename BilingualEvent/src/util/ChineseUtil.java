package util;

import java.util.ArrayList;

import model.ACEDoc;
import model.ParseResult;
import model.syntaxTree.MyTreeNode;

public class ChineseUtil {

	public static MyTreeNode getNPTreeNode(ArrayList<ParseResult> prs,
			int npSenIdx, int npWordStartIdx, int npWordEndIdx) {
		MyTreeNode NP = null;
		try {
			ArrayList<MyTreeNode> leaves = prs.get(npSenIdx).tree.leaves;
			MyTreeNode leftNp = leaves.get(npWordStartIdx - 1);
			MyTreeNode rightNp = leaves.get(npWordEndIdx - 1);
			// System.out.println(npWordEndIdx +np.getContent());
			ArrayList<MyTreeNode> leftAncestors = leftNp.getAncestors();
			ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
			for (int i = 0; i < leftAncestors.size()
					&& i < rightAncestors.size(); i++) {
				if (leftAncestors.get(i) == rightAncestors.get(i)) {
					NP = leftAncestors.get(i);
				} else {
					break;
				}

			}
		} catch (Exception e) {
			System.out.println("ERROR when finding tree node");
			return null;
		}
		return NP;
	}

	public static MyTreeNode getNPTreeNode(MyTreeNode root, int npWordStartIdx,
			int npWordEndIdx) {
		MyTreeNode NP = null;
		try {
			ArrayList<MyTreeNode> leaves = root.getLeaves();
			MyTreeNode leftNp = leaves.get(npWordStartIdx - 1);
			MyTreeNode rightNp = leaves.get(npWordEndIdx - 1);
			// System.out.println(npWordEndIdx +np.getContent());
			ArrayList<MyTreeNode> leftAncestors = leftNp.getAncestors();
			ArrayList<MyTreeNode> rightAncestors = rightNp.getAncestors();
			for (int i = 0; i < leftAncestors.size()
					&& i < rightAncestors.size(); i++) {
				if (leftAncestors.get(i) == rightAncestors.get(i)) {
					NP = leftAncestors.get(i);
					ArrayList<MyTreeNode> tmpLeaves = leftAncestors.get(i)
							.getLeaves();
					if (tmpLeaves.get(0) == leftNp
							&& tmpLeaves.get(tmpLeaves.size() - 1) == rightNp) {
						break;
					}
				} else {
					break;
				}

			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println(root.toString());
			System.out.println("ERROR when finding tree node");
			return null;
		}
		return NP;
	}

	// find the position of one entity mention in one file
	public static int[] findParseFilePosition(int start, int end,
			ACEDoc document) {
		// sentenceIdx, startWordIdx, startCharIdx, endWordIdx, endCharIdx
		ArrayList<ParseResult> parseResults = document.parseReults;
		int position[] = new int[5];
		String content = document.content;
		int idx = -1;
		int startWordIdx = 0;
		int startCharIdx = 0;
		int endWordIdx = 0;
		int endCharIdx = 0;
		int sentenceIdx = 0;
		boolean find = false;
		for (int i = 0; i < parseResults.size(); i++) {
			ArrayList<String> words = parseResults.get(i).words;
			for (int j = 1; j < words.size(); j++) {
				String word = words.get(j);
				int k = 0;
				for (; k < word.length(); k++) {
					idx = content.indexOf(word.charAt(k), idx + 1);
					if (idx == start) {
						startWordIdx = j;
						startCharIdx = k;
					}
					if (idx == end) {
						endWordIdx = j;
						endCharIdx = k;
						find = true;
						// if(startWordIdx==endWordIdx && startCharIdx==0 &&
						// endCharIdx==word.length()) {
						// wholeWord = true;
						// }
						//
						break;
					}
				}
				if (find) {
					break;
				}
			}
			if (find) {
				sentenceIdx = i;
				break;
			}
		}
		position[0] = sentenceIdx;
		position[1] = startWordIdx;
		position[2] = startCharIdx;
		position[3] = endWordIdx;
		position[4] = endCharIdx;
		return position;
	}

	// find the position of one entity mention in one file
	public static int[] findParseFilePosition2(int start, int end,
			ACEDoc document) {
		ArrayList<ParseResult> parseResults = document.parseReults;
		int position[] = new int[5];
		int startWordIdx = -1;
		int startCharIdx = -1;
		int endWordIdx = -1;
		int endCharIdx = -1;
		int sentenceIdx = -1;
		for (int i = 0; i < parseResults.size(); i++) {
			ParseResult pr = parseResults.get(i);
			ArrayList<int[]> positions = parseResults.get(i).positions;

			StringBuilder sb = new StringBuilder();

			for (int k = 1; k < positions.size(); k++) {
				if (positions.get(k)[0] <= start && positions.get(k)[1]>=start) {
					sentenceIdx = i;
					startWordIdx = k;
				}
				if (positions.get(k)[0] <= end && positions.get(k)[1] >= end) {
					if (i != sentenceIdx && sentenceIdx != -1) {
						System.out
								.println(parseResults.get(sentenceIdx).sentence);
						sb = new StringBuilder();
						for (int j = 1; j < parseResults.get(sentenceIdx).words
								.size(); j++) {
							sb.append(
									parseResults.get(sentenceIdx).positions
											.get(j)[0]).append(",");
							sb.append(
									parseResults.get(sentenceIdx).positions
											.get(j)[1]).append(" ");
						}
						System.out.println(sb.toString().trim());

						System.out.println(parseResults.get(i).sentence);
						sb = new StringBuilder();
						for (int j = 1; j < pr.words.size(); j++) {
							sb.append(pr.positions.get(j)[0]).append(",");
							sb.append(pr.positions.get(j)[1]).append(" ");
						}
						System.out.println("@@" + sb.toString().trim() + "$$");
						System.out.println("#" + start + "," + end + "##");
						endWordIdx = startWordIdx;
						Common.pause("");
						break;
					}
					sentenceIdx = i;
					endWordIdx = k;
					if (startWordIdx == -1) {
						startWordIdx = k;
					}
				}
			}
		}
		position[0] = sentenceIdx;
		position[1] = startWordIdx;
		position[2] = startCharIdx;
		position[3] = endWordIdx;
		position[4] = endCharIdx;

		if(position[0]==-1) {
			System.out.println(document.fileID);
			System.out.println(document.content.substring(start, end+1));
			System.out.println(document.content.substring(start-5, end+6));
			Common.pause("");
		}
		
		if (position[3] == -1) {
			position[3] = position[1];
		}

		return position;
	}
}
