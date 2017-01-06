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
	private static String ENG_TEXT_FILE = "train.eng.tok.txt";
	private static String MLG_TEXT_FILE = "train.mlg.tok.txt";

//	private static String MLG_ENG_ALIGNMENT_FILE = "mlg-eng.txt";
//	private static String ENG_TAGGED_FILE = "train.eng.best-pos.txt";
	private static String MLG_ENG_ALIGNMENT_FILE = "test.mlg-eng.txt";
	private static String ENG_TAGGED_FILE = "test.eng.pos.txt";
	
	public static void endLines(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String eachLine;
		File fout = new File(file + ".ended");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(fos));
		System.out.println("Ending each line in file: " + file + " > " + fout.getName());
		while ((eachLine = reader.readLine()) != null) {
			if (!eachLine.endsWith(".")
					&& !eachLine.endsWith("!")
					&& !eachLine.endsWith("?")) {
				eachLine += ".";
			}
			bw.write(eachLine);
			bw.newLine();
		}
		reader.close();
		bw.close();
	}
	
	public static void normalizeHashtag(String file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String eachLine;
		File fout = new File(file + ".norm");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(fos));
		System.out.println("Normalize hashtags in file: " + file + " > " + fout.getName());
		while ((eachLine = reader.readLine()) != null) {
			eachLine = eachLine.replaceAll("#\\s+", "#");
			bw.write(eachLine);
			bw.newLine();
		}
		reader.close();
		bw.close();
	}
	
	public static void main(String[] args) throws IOException {
//		normalizeHashtag(ENG_TEXT_FILE);
//		normalizeHashtag(MLG_TEXT_FILE);
		
		BufferedReader alignReader = new BufferedReader(new FileReader(MLG_ENG_ALIGNMENT_FILE));
		String eachAlignLine;
		ArrayList<String> alignLines = new ArrayList<String>();
		// Store all alignment lines into an array (except the # Sentence pair lines)
		while ((eachAlignLine = alignReader.readLine()) != null) {
			if (!eachAlignLine.startsWith("# Sentence pair")) {
//				System.out.println(eachAlignLine);
				alignLines.add(eachAlignLine);
			}
		}
		alignReader.close();
		
		// Check if alignment lines are empty
		if (alignLines.size() < 1) {
			System.err.println("Got no alignment lines.");
		}

		ArrayList<String> newAlignLines = new ArrayList<String>();
		for (int i = 0; i < alignLines.size() - 1; i++) {
//			System.out.println(alignLines.get(i));
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
//				System.out.println(alignSentence);
				
				// Replace matching token positions with actual matching tokens
				for (int j = 1; j < sentenceArray.length + 1; j++) {
					alignSentence = alignSentence.replaceAll("_" + Integer.toString(j) + "\\b", "_" + Matcher.quoteReplacement(sentenceArray[j-1]));
					alignSentence = alignSentence.replaceAll("\\|" + Integer.toString(j) + "\\|", "\\|" + Matcher.quoteReplacement(sentenceArray[j-1]) + "\\|");
					alignSentence = alignSentence.replaceAll("\\|" + Integer.toString(j) + " ", "\\|" + Matcher.quoteReplacement(sentenceArray[j-1]) + " ");
				}				
				System.out.println(alignSentence);
				newAlignLines.add(alignSentence);
			}
		}
		// Remove previous alignLines array
		alignLines = null;

//		// Separate sentences with only stop marks (. ! or ?)
//		int i = 0; 
//		while (i < newAlignLines.size() - 1) {
//			String line = newAlignLines.get(i);
//			if (newAlignLines.get(i+1) != null
//					&& !line.endsWith("! ")
//					&& !line.endsWith(". ")
//					&& !line.endsWith("? ")) {
//				line = line + newAlignLines.get(i+1);
//				newAlignLines.set(i, line);
//				newAlignLines.remove(i+1);
//				continue;
//			}
//			i++;
//		}
		
//		File alignOut = new File("aligned.txt");
//		FileOutputStream out = new FileOutputStream(alignOut);
//		BufferedWriter buffer = new BufferedWriter (new OutputStreamWriter(out));
//		for (String line : newAlignLines) {
//			buffer.write(line);
//			buffer.newLine();
//		}
//		buffer.close();
		
		// Make a POS map for each tagged sentence (English)
		ArrayList<HashMap<String, ArrayList<String>>> posArray = new ArrayList<HashMap<String, ArrayList<String>>>();
		BufferedReader posReader = new BufferedReader(new FileReader(ENG_TAGGED_FILE));
		String line;
		while ((line = posReader.readLine()) != null) {
			// Store POS of each token as an ordered list
			HashMap<String, ArrayList<String>> posDict = new HashMap<String, ArrayList<String>>();
			// Separate each line by space
			for (String eachWord : line.split("\\s+")) {
				String[] wordPOS = eachWord.split("_");
				if (wordPOS.length < 2) {
					continue;
				}
				// Add new POS for each token
				ArrayList<String> newValues = new ArrayList<String>();
				if (posDict.get(wordPOS[0]) != null) {
					newValues = posDict.get(wordPOS[0]);
				}
				newValues.add(wordPOS[1]);
				posDict.put(wordPOS[0], newValues);
			}
			posArray.add(posDict);
		}
		posReader.close();

		if (newAlignLines.size() != posArray.size()) {
			System.out.println(newAlignLines.size() + " != " + posArray.size());
			System.err.println("Number of aligned sentences and tagged sentences are different. Exit.");
			System.exit(-1);
		}

		// Replace matched token with its POS
		File fout = new File("train.mlg.tagged.txt");
		FileOutputStream fos = new FileOutputStream(fout);
		BufferedWriter bw = new BufferedWriter (new OutputStreamWriter(fos));
		for (int j = 0; j < newAlignLines.size(); j++) {
//			System.out.println(newAlignLines.get(j));
//			System.out.println(posArray.get(j));
			String taggedSentence = "";
			for (String eachWordTag : newAlignLines.get(j).split("\\s+")) {
				String[] tmp = eachWordTag.split("_");
				if (tmp.length < 2) {
					if (taggedSentence == "") {
						taggedSentence = tmp[0];
					} else {
						taggedSentence += " " + tmp[0];
					}
					continue;
				}

				ArrayList<String> newPOSValues = posArray.get(j).get(tmp[1]);
				if (newPOSValues == null) {
					if (taggedSentence == "") {
						taggedSentence = tmp[0];
					} else {
						taggedSentence += " " + tmp[0];
					}
					continue;
				}
				
				if (taggedSentence == "") {
					taggedSentence = tmp[0] + "_" + newPOSValues.get(0);
				} else {
					taggedSentence += " " + tmp[0] + "_" + newPOSValues.get(0);
				}

				// Remove used POS from posArray
				newPOSValues.remove(0);
				HashMap<String, ArrayList<String>> newHashmap = posArray.get(j);
				newHashmap.put(tmp[1], newPOSValues);
				posArray.set(j, newHashmap);
			}
//			System.out.println(taggedSentence);
			newAlignLines.set(j, taggedSentence);
			// Write to file
			bw.write(taggedSentence);
			bw.newLine();
		}
		bw.close();
		System.out.println("Wrote tagged text file to: " + fout.getAbsolutePath());
	}
}
