import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

public class Main {
	// Sentence by sentence alignment file
	private static String MLG_ENG_ALIGNMENT_FILE = "mlg-eng.txt";
	// Tagged source language file
	private static String ENG_TAGGED_FILE = "train.eng.best-pos.txt";
	// Output tagged target language file
	private static String OUTPUT_FILE = "train.mlg.tagged.txt";
	
	public static void main(String[] args) throws IOException {
		BufferedReader alignReader = new BufferedReader(new FileReader(MLG_ENG_ALIGNMENT_FILE));
		String eachAlignLine;
		ArrayList<String> alignLines = new ArrayList<String>();
		// Store all alignment lines into an array (except the # Sentence pair lines)
		while ((eachAlignLine = alignReader.readLine()) != null) {
			if (!eachAlignLine.startsWith("# Sentence pair")) {
				alignLines.add(eachAlignLine);
			}
		}
		alignReader.close();
		
		// Check if alignment lines are empty
		if (alignLines.size() < 1) {
			System.err.println("Got no alignment lines.");
		}

		ArrayList<String> newAlignLines = new ArrayList<String>();
		System.out.println("Cleaning aligned sentences...");
		for (int i = 0; i < alignLines.size() - 1; i++) {
			if (i % 2 == 0) {
				String[] sentenceArray = alignLines.get(i).split("\\s+");
				String alignSentence = alignLines.get(i+1);
				
				// Remove tokens with no match
				alignSentence = alignSentence.replaceAll("NULL\\s\\(\\{(.*?)\\}\\)\\s?", "");
				alignSentence = alignSentence.replaceAll("\\s+\\(\\{\\s\\}\\)", "");
				// Remove white spaces right after ({ and right before })
				alignSentence = alignSentence.replaceAll("\\(\\{\\s+([^})]+)\\s+\\}\\)", "\\(\\{$1\\}\\)");
				// Replace white spaces inside ({..}) with the character |
				alignSentence = alignSentence.replaceAll("\\s+(?=[^()]*\\}\\))", "\\|");
				// Replace ({ with _ and remove })
				alignSentence = alignSentence.replaceAll("\\s+\\(\\{([^})]*)\\}\\)", "_$1");
				
				// Replace matching token positions with actual matching tokens
				for (int j = 1; j < sentenceArray.length + 1; j++) {
					alignSentence = alignSentence.replaceAll("_" + Integer.toString(j) + "\\b", "_" + Matcher.quoteReplacement(sentenceArray[j-1]));
					alignSentence = alignSentence.replaceAll("\\|" + Integer.toString(j) + "\\|", "\\|" + Matcher.quoteReplacement(sentenceArray[j-1]) + "\\|");
					alignSentence = alignSentence.replaceAll("\\|" + Integer.toString(j) + " ", "\\|" + Matcher.quoteReplacement(sentenceArray[j-1]) + " ");
				}				
				newAlignLines.add(alignSentence);
			}
		}
		// Remove previous alignLines array
		alignLines = null;
		
		// Make a POS map for each tagged sentence (English)
		HashMap<String, ArrayList<String>> posMap = new HashMap<String, ArrayList<String>>();
		BufferedReader posReader = new BufferedReader(new FileReader(ENG_TAGGED_FILE));
		String line;
		System.out.println("Generating POS map for source language..."); // English
		while ((line = posReader.readLine()) != null) {
			for (String eachWord : line.split("\\s+")) {
				String[] wordPOS = eachWord.split("_");
				if (wordPOS.length < 2) {
					continue;
				}
				// Add new POS for each token
				ArrayList<String> newValues = new ArrayList<String>();
				if (posMap.get(wordPOS[0]) != null) {
					newValues = posMap.get(wordPOS[0]);
				}
				newValues.add(wordPOS[1]);
				posMap.put(wordPOS[0], newValues);
			}
		}
		posReader.close();

		// Replace matched token with its POS
		File fout = new File(OUTPUT_FILE);
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(fos));
		System.out.println("Tagging target language file...");
		for (int j = 0; j < newAlignLines.size(); j++) {
			String taggedSentence = "";
			for (String eachWordTag : newAlignLines.get(j).split("\\s+")) {
				String[] tokenMatchingTokens = eachWordTag.split("_");
				// If no POS is found for the token
				if (tokenMatchingTokens.length < 2) {
					if (taggedSentence == "") {
						taggedSentence = eachWordTag;
					} else {
						taggedSentence += " " + eachWordTag;
					}
					continue;
				}

				String[] matchingTokens = tokenMatchingTokens[1].split("\\|");
				String firstMatchingToken;
				if (matchingTokens.length == 0) {
					firstMatchingToken = tokenMatchingTokens[1];
				} else {
					firstMatchingToken = matchingTokens[0];
				}

				// Get POS only for the first matching token
				ArrayList<String> newPOSValues = posMap.get(firstMatchingToken);
				// If no matching POS is found for the matching token, append only the token
				if (newPOSValues == null || newPOSValues.size() == 0) {
					if (taggedSentence == "") {
						taggedSentence = tokenMatchingTokens[0];
					} else {
						taggedSentence += " " + tokenMatchingTokens[0];
					}
					continue;
				}
				// If matching POS for matching token found, append the token + the first POS of the matching token
				if (taggedSentence == "") {
					taggedSentence = tokenMatchingTokens[0] + "_" + newPOSValues.get(0);
				} else {
					taggedSentence += " " + tokenMatchingTokens[0] + "_" + newPOSValues.get(0);
				}

				// Remove the first POS of every matching token
				if (matchingTokens.length == 0) {
					ArrayList<String> newPOSArray = posMap.get(firstMatchingToken);
					if (newPOSArray != null) {
						newPOSArray.remove(0);
						posMap.put(firstMatchingToken, newPOSArray);
					}
				} else {
					for (String eachMatchedToken : matchingTokens) {
						ArrayList<String> newPOSArray = posMap.get(eachMatchedToken);
						if (newPOSArray != null && newPOSArray.size() > 0) {
							newPOSArray.remove(0);
							posMap.put(eachMatchedToken, newPOSArray);
						}
					}
				}
			}

//			System.out.println(taggedSentence);
			newAlignLines.set(j, taggedSentence);

			// Write each line to file
			bw.write(taggedSentence);
			bw.newLine();
		}

		bw.close();
		System.out.println("Wrote tagged text file to: " + fout.getAbsolutePath());
	}
}
