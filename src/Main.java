import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
	// Sentence by sentence alignment file
	private static String MLG_ENG_ALIGNMENT_FILE = "mlg-eng.txt";
	// Tagged source language file
	private static String ENG_TAGGED_FILE = "corpus.eng.tagged.txt";
	// Output tagged target language file
	private static String OUTPUT_FILE = "corpus.mlg.tagged.txt";
	
	// Malagasy and English POS tags
	private static String[] tagset = {",", ":", ".", "...", "\"", "@-@", "ADJ", "ADV", "C", "CONJ",
			"DT", "FOC", "-LRB-", "N", "NEG", "PCL", "PN", "PREP", "PRO", "-RRB-", "T", "V", "X",
			"#", "$", "AFX", "CC", "CD", "EX", "FW", "HYPH", "IN", "JJ", "JJR", "JJS", "LS", "MD",
			"NIL", "NN", "NNP", "NNPS", "NNS", "PDT", "POS", "PRP", "PRP$", "RB", "RBR", "RBS",
			"RP", "SYM", "TO", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP", "WP$",
			"WRB", "``", "''"};

	// Hashmap for converting POS tags to universal POS tags
	private static HashMap<String, String> posTagMap = new HashMap<String, String>() {{
		// Malagasy -> Universal
		put(",", ".");
		put(":", ".");
		put(".", ".");
		put("...", ".");
		put("\"", ".");
		put("@-@", ".");
		put("ADJ", "ADJ");
		put("ADV", "ADV");
		put("C", "CONJ");
		put("CONJ", "CONJ");
		put("DT", "DET");
		put("FOC", "DET");
		put("-LRB-", ".");
		put("N", "NOUN");
		put("NEG", "ADV");
		put("PCL", "PRT");
		put("PN", "NOUN");
		put("PREP", "ADP");
		put("PRO", "PRON");
		put("-RRB-", ".");
		put("T", "VERB");
		put("V", "VERB");
		put("X", "X");
		// Penn Treebank -> Universal
		put("#", "PRT"); // SYM: To find (Symbol)
		put("$", "PRT"); // SYM: To find (Symbol)
		put("AFX", "ADJ");
		put("CC", "CONJ");
		put("CD", "NUM");
		put("EX", "ADV");
		put("FW", "X");
		put("HYPH", ".");
		put("IN", "ADP");
		put("JJ", "ADJ");
		put("JJR", "ADJ");
		put("JJS", "ADJ");
		put("LS", ".");
		put("MD", "VERB");
		put("NIL", "X");
		put("NN", "NOUN");
		put("NNP", "NOUN");
		put("NNPS", "NOUN");
		put("NNS", "NOUN");
		put("PDT", "DET");
		put("POS", "PRT");
		put("PRP", "PRON");
		put("PRP$", "DET");
		put("RB", "ADV");
		put("RBR", "ADV");
		put("RBS", "ADV");
		put("RP", "PRT");
		put("SYM", "X"); // SYM: to find
		put("TO", "PRT");
		put("UH", "ADV");
		put("VB", "VERB");
		put("VBD", "VERB");
		put("VBG", "VERB");
		put("VBN", "VERB");
		put("VBP", "VERB");
		put("VBZ", "VERB");
		put("WDT", "DET");
		put("WP", "PRON");
		put("WP$", "DET");
		put("WRB", "ADV");
		put("``", ".");
		put("''", ".");
	}};
	
	public static void doPOSProjection(String alignmentFile, String sourceLangFile, String outputFile) throws IOException {
		BufferedReader alignReader = new BufferedReader(new FileReader(alignmentFile));
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
				// Replace ({ with |_ and remove })
				alignSentence = alignSentence.replaceAll("\\s+\\(\\{([^})]*)\\}\\)", "\\|_$1");
				
				// Replace matching token positions with actual matching tokens
				for (int j = 1; j < sentenceArray.length + 1; j++) {
					alignSentence = alignSentence.replaceAll("\\|_" + Integer.toString(j) + "\\b", "\\|_" + Matcher.quoteReplacement(sentenceArray[j-1]));
					alignSentence = alignSentence.replaceAll("\\|" + Integer.toString(j) + "\\|", "\\|" + Matcher.quoteReplacement(sentenceArray[j-1]) + "\\|");
					alignSentence = alignSentence.replaceAll("\\|" + Integer.toString(j) + " ", "\\|" + Matcher.quoteReplacement(sentenceArray[j-1]) + " ");
				}
//				System.out.println(alignSentence);
				newAlignLines.add(alignSentence);
			}
		}
		// Remove previous alignLines array
		alignLines = null;
		
		// Make a POS map for each tagged sentence (English)
		ArrayList<HashMap<String, ArrayList<String>>> posMapCollection = new ArrayList<HashMap<String, ArrayList<String>>>();
		BufferedReader posReader = new BufferedReader(new FileReader(sourceLangFile));
		String line;
		System.out.println("Generating POS map for source language..."); // English
		while ((line = posReader.readLine()) != null) {
			HashMap<String, ArrayList<String>> posMap = new HashMap<String, ArrayList<String>>();
			for (String eachWord : line.split("\\s+")) {
				int tagSepIndex = eachWord.lastIndexOf('_');
				if (tagSepIndex == -1
						|| eachWord.substring(0, tagSepIndex).trim().isEmpty()
						|| eachWord.substring(tagSepIndex).trim().isEmpty()) {
					continue;
				}
				String[] wordPOS = {eachWord.substring(0, tagSepIndex), eachWord.substring(tagSepIndex + 1)};
//				String[] wordPOS = eachWord.split("_");
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
			posMapCollection.add(posMap);
		}
		posReader.close();

		// Check number of lines
		if (newAlignLines.size() != posMapCollection.size()) {
			System.out.println("The number of lines in sentence alignment file and source language file are different.");
			System.out.println(newAlignLines.size() + " != " + posMapCollection.size());
			return;
		}
		
		// Replace matched token with its POS
		File fout = new File(outputFile);
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(fos));
		System.out.println("Tagging target language file...");
		for (int j = 0; j < newAlignLines.size(); j++) {
			String taggedSentence = "";
			for (String eachWordTag : newAlignLines.get(j).split("\\s+")) {
				String[] tokenMatchingTokens = eachWordTag.split("\\|_");
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
				ArrayList<String> newPOSValues = posMapCollection.get(j).get(firstMatchingToken);
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
					taggedSentence = tokenMatchingTokens[0] + "|" + newPOSValues.get(0);
				} else {
					taggedSentence += " " + tokenMatchingTokens[0] + "|" + newPOSValues.get(0);
				}

				// Remove the first POS of every matching token
				if (matchingTokens.length > 1) {
					HashMap<String, ArrayList<String>> newPOSMap = posMapCollection.get(j);
					for (String eachMatchingToken : matchingTokens) {
						ArrayList<String> newPOSArray = newPOSMap.get(eachMatchingToken);
						if (newPOSArray != null && newPOSArray.size() > 0) {
							newPOSArray.remove(0);
							newPOSMap.put(eachMatchingToken, newPOSArray);
						}
					}
					posMapCollection.set(j, newPOSMap);
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
	
	/* Convert current POS tags to universal POS tags
	 * This assumes that your tag separator is the character "|"
	 */
	public static String convertTagsToUniversal(String fileName) throws IOException {
		String output = "";
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line;
		System.out.println("Converting tags..");
		while ((line = reader.readLine()) != null) {
			Pattern pattern = Pattern.compile("\\|([^|_\\s]+)");
			Matcher matcher = pattern.matcher(line);
			while (matcher.find()) {
				line = matcher.replaceFirst("\\|_" + posTagMap.getOrDefault(matcher.group(1), matcher.group(1)));
				matcher = pattern.matcher(line);
			}

			output += line.replaceAll("([^\\s])\\|_([^|\\s])", "$1|$2");
			// Add new line only if current line has text
			if (line.trim().length() > 0) {
				output += "\n";
			}
		}
		reader.close();
		System.out.println("Finished tag conversion.");
		return output;
	}
	
	/* Convert current input into a dataset for model training
	 * Dataset has the form of:
	 * tok_1 tok_2 ||| tag_1 tag_2
	 */
	public static void makeDataset(String input, String output) throws IOException {
		File fout = new File(output);
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(fos));
		
		BufferedReader reader = new BufferedReader(new StringReader(input));
		String line;
		System.out.println("Formatting and making dataset...");
		while ((line = reader.readLine()) != null) {
			String sentence = "";
			String tagSentence = "";
			String[] wordTagPairs = line.split("\\s+");
			for (String eachWordTagPair : wordTagPairs) {
				String[] wordTagPair = eachWordTagPair.split("\\|");
				if (wordTagPair.length > 1) {
					sentence += wordTagPair[0] + " ";
					tagSentence += wordTagPair[1] + " ";
				} else {
					sentence += eachWordTagPair + " ";
					tagSentence += "X ";
				}
			}
			sentence = sentence.trim();
			tagSentence = tagSentence.trim();
			bw.write(sentence + " ||| " + tagSentence);
			bw.newLine();
		}
		bw.close();
		System.out.println("Finished making dataset. Output to: " + fout.getAbsolutePath());
	}
	
	public static void printIncorrectTags(String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		String line;
		while ((line = reader.readLine()) != null) {
			String[] wordTagPairs = line.split("\\s+");
			for (String eachWordTagPair : wordTagPairs) {
				int tagSepIndex = eachWordTagPair.lastIndexOf('|');
				if (tagSepIndex == -1
						|| eachWordTagPair.substring(0, tagSepIndex).trim().isEmpty()
						|| eachWordTagPair.substring(tagSepIndex + 1).trim().isEmpty()) {
					continue;
				}
				String[] wordTagPair = {eachWordTagPair.substring(0, tagSepIndex), eachWordTagPair.substring(tagSepIndex + 1)};
				if (wordTagPair.length > 1 && posTagMap.get(wordTagPair[1]) == null) {
					System.out.println("Incorrect tag: " + wordTagPair[1]);
					System.out.println(line);
				}
			}
		}
		System.out.print("Finished finding incorrect tags.");
		reader.close();
	}

	public static void main(String[] args) throws IOException {
//		doPOSProjection(MLG_ENG_ALIGNMENT_FILE, ENG_TAGGED_FILE, OUTPUT_FILE);
//		printIncorrectTags(OUTPUT_FILE);

//		doPOSProjection("test.mlg-eng.txt", "corpus.eng.tagged.txt", "test.mlg.tagged.txt");
//		printIncorrectTags("test.mlg.tagged.txt");

//		String[] mlgFiles = {"gold.txt", "test.txt"};
//		for (String eachFile : mlgFiles) {
//			String converted = convertTagsToUniversal(eachFile);
//			makeDataset(converted, "dataset-" + eachFile);
//		}

		String convertedPro = convertTagsToUniversal(OUTPUT_FILE);
		makeDataset(convertedPro, "dataset-all-projected.txt");
	}
}
