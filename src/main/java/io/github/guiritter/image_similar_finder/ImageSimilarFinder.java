package io.github.guiritter.image_similar_finder;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import org.joml.Vector3f;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ImageSimilarFinder {

	private static File candidateFolder;
	private static File targetFolder;

	private static Map<String, Vector3f> candidateAverageByImageFilePath = new HashMap<>();
	private static Map<String, Vector3f> targetAverageByImageFilePath = new HashMap<>();

	static {
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
	}

	private static Predicate<ManagerCell> byLowerField(String lowerField) {
		return managerCell -> managerCell.lowerField.compareToIgnoreCase(lowerField) == 0;
	}

	private static final Stream<ManagerCell> flatten(ManagerColumn managerColumn) {
		return managerColumn.cellList.stream();
	}

	private static final IntConsumer treatCell(WikiColumn wikiColumn, ManagerTable managerTable,
			ManagerColumn bridgeColumn) {
		return cellIndex -> {
			var wikiCell = wikiColumn.cellList.get(cellIndex);
			var lowerField = wikiCell.blk;

			var managerCell = managerTable.columnList.stream().flatMap(ImageSimilarFinder::flatten)
					.filter(byLowerField(lowerField)).findAny();

			var upperField = "";

			if (managerCell.isPresent()) {
				upperField = managerCell.get().upperField;
				lowerField = managerCell.get().lowerField;
			}

			while (bridgeColumn.cellList.size() <= cellIndex) {
				bridgeColumn.cellList.add(new ManagerCell());
			}

			var bridgeCell = bridgeColumn.cellList.get(cellIndex);
			bridgeCell.upperField = upperField;
			bridgeCell.lowerField = lowerField;
		};
	}

	private static final IntConsumer treatColumn(WikiTable wikiTable, ManagerTable managerTable,
			ManagerTable bridgeTable) {
		return columnIndex -> {

			while (bridgeTable.columnList.size() <= columnIndex) {
				bridgeTable.columnList.add(new ManagerColumn());
			}

			var wikiColumn = wikiTable.columnList.get(columnIndex);
			var bridgeColumn = bridgeTable.columnList.get(columnIndex);

			IntStream.range(0, wikiColumn.cellList.size()).forEach(treatCell(wikiColumn, managerTable, bridgeColumn));
		};
	};

	private static final void storeAverage(File imageFile, Map<String, Vector3f> averageByImageFilePath) {
		var bufferedImage = ImageIO.read(imageFile);

		var colorModel = bufferedImage.getColorModel();

		var width = bufferedImage.getWidth();
		var height = bufferedImage.getHeight();

		var pixelAmount = width * height;

		int x, y, rgb;

		Color color;

		long r, g, b;

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

		// var jsonMapper = new ObjectMapper().configure(FAIL_ON_UNKNOWN_PROPERTIES, false).enable(INDENT_OUTPUT);
		// WikiTable wikiTable;
		// try {
		// 	wikiTable = jsonMapper.readValue(wikiFile, WikiTable.class);
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// 	return;
		// }
		// var managerFile = candidateFolder.toPath().resolve(wikiFile.getName()).toFile();
		// ManagerTable managerTable;
		// try {
		// 	managerTable = jsonMapper.readValue(managerFile, ManagerTable.class);
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// 	return;
		// }
		// var bridgeTable = new ManagerTable();
		// IntStream.range(0, wikiTable.columnList.size()).forEach(treatColumn(wikiTable, managerTable, bridgeTable));

		// var bridgeFile = bridgeFolder.toPath().resolve(wikiFile.getName()).toFile();
		// try {
		// 	jsonMapper.writeValue(bridgeFile, bridgeTable);
		// } catch (IOException e) {
		// 	e.printStackTrace();
		// }
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
		}

		Consumer<File> storeTargetAverage = (imageFile) -> storeAverage(imageFile, targetAverageByImageFilePath);
		Consumer<File> storeCandidateAverage = (imageFile) -> storeAverage(imageFile, candidateAverageByImageFilePath);

		Stream.of(targetFolder.listFiles()).forEach(storeTargetAverage);
		Stream.of(candidateFolder.listFiles()).forEach(storeCandidateAverage);
		// TODO check one against the other
	}
}
