package io.github.guiritter.image_similar_finder;

import static java.lang.System.exit;
import static java.lang.System.out;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.joml.Vector3f;

public class ImageSimilarFinder {

	private static File candidateFolder;
	private static Map<String, Vector3f> candidateAverageByImageFilePath = new HashMap<>();
	
	private static File targetFolder;
	private static Map<String, Vector3f> targetAverageByImageFilePath = new HashMap<>();
	
	public static final Vector3f vectorMaximum = new Vector3f(255, 255, 255);
	public static final Vector3f vectorMinimum = new Vector3f(0, 0, 0);

	public static final float vectorDistanceMaximum = vectorMinimum.distance(vectorMaximum);

	static {
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
	}

	private static final void findSimilar(String targetImageFilePath, Vector3f targetAverage) {
		var vectorDistanceSmallest = vectorDistanceMaximum;
		float vectorDistance;
		var candidateImageFilePathSmallest = "";

		for (var candidate : candidateAverageByImageFilePath.entrySet()) {
			vectorDistance = targetAverage.distance(candidate.getValue());

			if (vectorDistance < vectorDistanceSmallest) {
				vectorDistanceSmallest = vectorDistance;
				candidateImageFilePathSmallest = candidate.getKey();
			}
		}

		out.format("for\n%s\nmost similar is\n%s\n\n", targetImageFilePath, candidateImageFilePathSmallest);
	}

	private static final void storeCandidateAverage(File imageFile) {
		storeAverage(imageFile, candidateAverageByImageFilePath);
	}

	private static final void storeTargetAverage(File imageFile) {
		storeAverage(imageFile, targetAverageByImageFilePath);
	}

	private static final void storeAverage(File imageFile, Map<String, Vector3f> averageByImageFilePath) {
		out.format("ImageSimilarFinder.storeAverage %s\n", imageFile.getAbsolutePath());

		BufferedImage bufferedImage;

		try {
			bufferedImage = ImageIO.read(imageFile);
		} catch (IOException e) {
			e.printStackTrace();
			exit(0);
			return;
		}

		var width = bufferedImage.getWidth();
		var height = bufferedImage.getHeight();

		var pixelAmount = width * height;

		int x, y, rgb;

		Color color;

		long r = 0, g = 0, b = 0;

		for (y = 0; y < height; y++) {
			for (x = 0; x < width; x++) {
				rgb = bufferedImage.getRGB(x, y);
				color = new Color(rgb, true);
				r += color.getRed();
				g += color.getGreen();
				b += color.getBlue();
			}
		}

		var vector = new Vector3f(
			((float) r) / ((float) pixelAmount),
			((float) g) / ((float) pixelAmount),
			((float) b) / ((float) pixelAmount)
		);

		averageByImageFilePath.put(imageFile.getAbsolutePath(), vector);
	}

	public static void main(String args[]) throws IOException {
		if (args.length > 0) {
			targetFolder = new File(args[0]);
			candidateFolder = new File(args[1]);
		} else {
			JFileChooser chooser = new JFileChooser();
			chooser.setFileSelectionMode(DIRECTORIES_ONLY);
			chooser.setDialogTitle("Choose the target data folder");
			if (chooser.showOpenDialog(null) != APPROVE_OPTION) {
				return;
			}
			targetFolder = chooser.getSelectedFile();
			if (targetFolder == null) {
				return;
			}
			chooser.setDialogTitle("Choose the candidate data folder");
			if (chooser.showOpenDialog(null) != APPROVE_OPTION) {
				return;
			}
			candidateFolder = chooser.getSelectedFile();
			if (candidateFolder == null) {
				return;
			}
		}

		Stream.of(targetFolder.listFiles()).forEach(ImageSimilarFinder::storeTargetAverage);
		Stream.of(candidateFolder.listFiles()).forEach(ImageSimilarFinder::storeCandidateAverage);
		targetAverageByImageFilePath.forEach(ImageSimilarFinder::findSimilar);
	}
}
