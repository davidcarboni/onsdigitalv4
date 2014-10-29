package com.github.onsdigital.generator;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

import com.github.davidcarboni.ResourceUtils;
import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.generator.timeseries.AlphaContentCSV;
import com.github.onsdigital.json.Article;
import com.github.onsdigital.json.Dataset;
import com.github.onsdigital.json.Release;
import com.github.onsdigital.json.bulletin.Bulletin;
import com.github.onsdigital.json.taxonomy.T1;
import com.github.onsdigital.json.taxonomy.T2;
import com.github.onsdigital.json.taxonomy.T3;
import com.github.onsdigital.json.timeseries.TimeSeries;
import com.github.onsdigital.json.timeseries.TimeSeriesValue;

public class TaxonomyGenerator {

	/**
	 * Parses the taxonomy CSV file and generates a file structure..
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		Serialiser.getBuilder().setPrettyPrinting();
		Reader reader = ResourceUtils.getReader("/Taxonomy.csv");

		String theme = null;
		String subject = null;
		String topic = null;
		// String subTopic = null;
		Folder themeFolder = null;
		Folder subjectFolder = null;
		Folder topicFolder = null;
		// Folder subTopicFolder = null;
		int themeCounter = 0;
		int subjectCounter = 0;
		int topicCounter = 0;
		// int subTopicCounter = 0;

		Set<Folder> folders = new HashSet<>();

		try (CSVReader csvReader = new CSVReader(reader)) {

			// Column positions:s
			String[] headers = csvReader.readNext();
			System.out.println(ArrayUtils.toString(headers));
			int themeIndex = ArrayUtils.indexOf(headers, "Theme");
			int subjectIndex = ArrayUtils.indexOf(headers, "Subject");
			int topicIndex = ArrayUtils.indexOf(headers, "Topic");
			// int subTopicIndex = ArrayUtils.indexOf(headers, "Subtopic");
			int ledeIndex = ArrayUtils.indexOf(headers, "Lede");
			int moreIndex = ArrayUtils.indexOf(headers, "More");
			// System.out.println("Theme=" + themeIndex + " Subject=" +
			// subjectIndex + " Topic=" + topicIndex + " Subtopic=" +
			// subTopicIndex);
			System.out.println("Theme=" + themeIndex + " Subject=" + subjectIndex + " Topic=" + topicIndex + " Lede=" + ledeIndex + " More=" + moreIndex);

			// Theme Subject Topic
			String[] row;
			while ((row = csvReader.readNext()) != null) {

				if (StringUtils.isNotBlank(row[themeIndex])) {
					theme = row[themeIndex];
					themeFolder = new Folder();
					themeFolder.name = theme;
					themeFolder.index = themeCounter++;
					subjectCounter = 0;
					topicCounter = 0;
					// subTopicCounter = 0;
					if (StringUtils.isNotBlank(row[ledeIndex])) {
						themeFolder.lede = row[ledeIndex];
						System.out.println(themeFolder.name + " lede: " + themeFolder.lede);
					} else {
						System.out.println("- No lede");
					}
					if (StringUtils.isNotBlank(row[moreIndex])) {
						themeFolder.more = row[moreIndex];
					}
					folders.add(themeFolder);
					subject = null;
					topic = null;
					// subTopic = null;
				}

				if (StringUtils.isNotBlank(row[subjectIndex])) {
					subject = row[subjectIndex];
					subjectFolder = new Folder();
					subjectFolder.name = subject;
					subjectFolder.parent = themeFolder;
					subjectFolder.index = subjectCounter++;
					topicCounter = 0;
					// subTopicCounter = 0;
					if (StringUtils.isNotBlank(row[ledeIndex])) {
						subjectFolder.lede = row[ledeIndex];
					}
					if (StringUtils.isNotBlank(row[moreIndex])) {
						subjectFolder.more = row[moreIndex];
					}
					themeFolder.addChild(subjectFolder);
					topic = null;
					// subTopic = null;
				}

				if (StringUtils.isNotBlank(row[topicIndex])) {
					topic = row[topicIndex];
					topicFolder = new Folder();
					topicFolder.name = topic;
					topicFolder.parent = subjectFolder;
					topicFolder.index = topicCounter++;
					subjectFolder.addChild(topicFolder);
					// subTopic = null;
					if (StringUtils.isNotBlank(row[ledeIndex])) {
						topicFolder.lede = row[ledeIndex];
					}
					if (StringUtils.isNotBlank(row[moreIndex])) {
						topicFolder.more = row[moreIndex];
					}
				}

				// if (StringUtils.isNotBlank(row[subTopicIndex])) {
				// subTopic = row[subTopicIndex];
				// subTopicFolder = new Folder();
				// subTopicFolder.name = subTopic;
				// subTopicFolder.parent = topicFolder;
				// subTopicFolder.index = subTopicCounter++;
				// topicFolder.children.add(subTopicFolder);
				// }

				String path = StringUtils.join(new String[] { theme, subject, topic }, '/');
				while (StringUtils.endsWith(path, "/")) {
					path = path.substring(0, path.length() - 1);
				}
				System.out.println(path);
			}
		}

		// Walk folder tree:
		File root = new File("src/main/taxonomy");
		Folder rootFolder = new Folder();
		rootFolder.name = "Home";
		rootFolder.addChildren(folders);
		createHomePage(rootFolder, root);
		File themeFile;
		File subjectFile;
		File topicFile;
		for (Folder t : folders) {
			themeFile = new File(root, t.filename());
			themeFile.mkdirs();
			System.out.println(themeFile.getAbsolutePath());
			createT2(t, themeFile);
			for (Folder s : t.getChildren()) {
				subjectFile = new File(themeFile, s.filename());
				subjectFile.mkdirs();
				if (s.getChildren().size() == 0) {
					createT3(s, subjectFile);
					// createContentFolders(s.name, subjectFile);
				} else {
					createT2(s, subjectFile);
				}
				System.out.println("\t" + subjectFile.getPath());
				for (Folder o : s.getChildren()) {
					topicFile = new File(subjectFile, o.filename());
					topicFile.mkdirs();
					System.out.println("\t\t" + topicFile.getPath());
					createT3(o, topicFile);
					if (o.getChildren().size() == 0) {
						// createContentFolders(o.name, topicFile);
					} else if (isReleaseFolder(o)) {
						for (Folder u : o.getChildren()) {
							createRelease(topicFile, u);
						}
					}
				}
			}
		}

		// createTimeseries(Paths.get(root.toURI()));
	}

	// private static void createContentFolders(String name, File file)
	// throws IOException {
	//
	// Folder folder = new Folder();
	// File articles = new File(file, "articles");
	// folder.name = name + ": articles";
	// createIndex(folder, articles);
	// File bulletins = new File(file, "bulletins");
	// folder.name = name + ": bulletins";
	// createIndex(folder, bulletins);
	// File datasets = new File(file, "datasets");
	// folder.name = name + ": datasets";
	// createIndex(folder, datasets);
	// File methodology = new File(file, "methodology");
	// folder.name = name + ": methodology";
	// createIndex(folder, methodology);
	//
	// // This causes "too many open files" because Restolino
	// // attempts to monitor every directory:
	// // createHistory(name, file);
	// }

	/**
	 * Simulate some historical releases.
	 * 
	 * @param name
	 * @param file
	 * @throws IOException
	 */
	private static void createHistory(File file, String json) throws IOException {

		File tempDir = com.google.common.io.Files.createTempDir();
		List<File> historyFolders = historyFolders(file, json);

		// Delete existing history folders:
		for (File historyFolder : historyFolders) {
			FileUtils.deleteQuietly(historyFolder);
		}

		System.out.println("Copying from " + file.getAbsolutePath() + " to " + tempDir.getAbsolutePath());
		FileUtils.copyDirectory(file, tempDir);

		for (File historyFolder : historyFolders) {
			System.out.println("Copying from " + tempDir.getAbsolutePath() + " to " + historyFolder.getAbsolutePath());
			FileUtils.copyDirectory(tempDir, historyFolder);
		}

		FileUtils.deleteDirectory(tempDir);
	}

	private static List<File> historyFolders(File file, String json) throws IOException {
		List<File> result = new ArrayList<>();

		for (int i = 1; i <= 10; i++) {
			Calendar release = Calendar.getInstance();
			release.add(Calendar.MONTH, -i);
			int year = release.get(Calendar.YEAR);
			int month = release.get(Calendar.MONTH) + 1;
			// Fixed at 21 to avoid the taxonomy being different too often.
			// 21st of September is "International Peace Day".
			int day = 21;
			String releaseFolderName = year + "-" + (month < 10 ? ("0" + month) : (month)) + "-" + day;

			File releaseFolder = new File(file, releaseFolderName);
			result.add(releaseFolder);
		}

		return result;
	}

	private static void createHomePage(Folder folder, File file) throws IOException {
		// The folder needs to be at the root path:
		T1 t1 = new T1(folder);
		t1.fileName = "/";
		if (StringUtils.isNotBlank(folder.lede)) {
			t1.lede = folder.lede;
			t1.more = folder.more;
		}
		String json = Serialiser.serialise(t1);
		FileUtils.writeStringToFile(new File(file, "data.json"), json);
	}

	private static void createT2(Folder folder, File file) throws IOException {
		if (folder.name.equals("Releases") || (folder.parent != null && folder.parent.name.equals("Releases"))) {
			System.out.println("Do not create json for Releases createT2");
		} else {
			T2  t2 = new T2(folder, folder.index );
			if (StringUtils.isNotBlank(folder.lede)) {
				t2.lede = folder.lede;
				t2.more = folder.more;
			}
			t2.index = folder.index;
			String json = Serialiser.serialise(t2);
			FileUtils.writeStringToFile(new File(file, "data.json"), json);
		}
	}

	private static void createT3(Folder folder, File file) throws IOException {

		if (isReleaseFolder(folder)) {
			System.out.println("Do not create json for Releases createT3");
		} else {
			T3 t3 = new T3(folder);
			if (StringUtils.isNotBlank(folder.lede)) {
				t3.lede = folder.lede;
				t3.more = folder.more;
			}
			t3.index = folder.index;

			// Timeseries references:
			URI headline = toUri(folder, AlphaContentCSV.getHeadlineTimeSeries(folder));
			if (headline != null) {
				t3.headline = headline;
			}
			List<TimeSeries> timeserieses = AlphaContentCSV.getTimeSeries(folder);
			t3.items.clear();
			String baseUri = "/" + folder.filename();
			Folder parent = folder.parent;
			while (parent != null) {
				baseUri = "/" + parent.filename() + baseUri;
				parent = parent.parent;
			}
			baseUri += "/timeseries";
			for (TimeSeries timeseries : timeserieses) {
				t3.items.add(toUri(folder, timeseries));
			}

			// Serialise
			String json = Serialiser.serialise(t3);
			FileUtils.writeStringToFile(new File(file, "data.json"), json);

			createArticle(folder, file);
			createBulletin(folder, file);
			createDataset(folder, file);
			createTimeseries(folder, file);
		}
	}

	private static URI toUri(Folder folder, TimeSeries timeseries) {
		URI result = null;

		if (timeseries != null) {
			String baseUri = "/" + folder.filename();
			Folder parent = folder.parent;
			while (parent != null) {
				baseUri = "/" + parent.filename() + baseUri;
				parent = parent.parent;
			}
			baseUri += "/timeseries";
			result = URI.create(baseUri + "/" + timeseries.fileName);
		}

		return result;
	}

	/**
	 * Creates timeseries data.
	 *
	 * @param folder
	 * @param file
	 * @throws IOException
	 */
	private static void createTimeseries(Folder folder, File file) throws IOException {

		List<TimeSeries> timeserieses = AlphaContentCSV.getTimeSeries(folder);

		if (timeserieses.size() > 0) {
			File timeseriesesFolder = new File(file, "timeseries");
			timeseriesesFolder.mkdir();
			for (TimeSeries timeseries : timeserieses) {
				File timeseriesFolder = new File(timeseriesesFolder, timeseries.fileName);
				Set<TimeSeriesValue> data = TimeseriesData.getData(timeseries.cdid);
				if (data != null) {
					timeseries.data = new ArrayList<>(data);
					System.out.println(data.size() + " timeseries values for " + timeseries.cdid);
				} else {
					System.out.println("No data for " + timeseries.cdid);
				}
				String json = Serialiser.serialise(timeseries);
				FileUtils.writeStringToFile(new File(timeseriesFolder, "data.json"), json, Charset.forName("UTF8"));
			}
		}
	}

	private static void createArticle(Folder folder, File file) throws IOException {
		File articles = new File(file, "articles");
		articles.mkdir();

		ArticlesCsv.buildFolders();
		Set<Folder> articleFolders = ArticlesCsv.folders;

		for (Folder articleFolder : articleFolders) {
			if (StringUtils.deleteWhitespace(articleFolder.name.toLowerCase()).equals(file.getName())) {

				for (Folder topicFolder : articleFolder.getChildren()) {
					File topicFile = new File(articles, StringUtils.deleteWhitespace(topicFolder.name.toLowerCase()));
					topicFile.mkdir();
					Article article = new Article();
					article.title = topicFolder.name;
					String json = Serialiser.serialise(article);
					File articleJsonFile = new File(topicFile, "data.json");
					FileUtils.writeStringToFile(articleJsonFile, json);
					createVersions(topicFile, json);
					createHistory(topicFile, json);
				}
			}
		}
	}

	private static void createBulletin(Folder folder, File file) throws IOException {

		// Create a dummy bulletin:
		File bulletins = new File(file, "bulletins");
		bulletins.mkdir();

		BulletinsCsv.buildFolders();
		Set<Folder> bulletinFolders = BulletinsCsv.folders;

		for (Folder bulletinFolder : bulletinFolders) {
			if (StringUtils.deleteWhitespace(bulletinFolder.name.toLowerCase()).equals(file.getName())) {

				for (Folder topicFolder : bulletinFolder.getChildren()) {
					File topicFile = new File(bulletins, StringUtils.deleteWhitespace(topicFolder.name.toLowerCase()));
					topicFile.mkdir();
					Bulletin bulletin = new Bulletin();
					bulletin.title = topicFolder.name;
					String json = Serialiser.serialise(bulletin);
					File bulletinJsonFile = new File(topicFile, "data.json");
					FileUtils.writeStringToFile(bulletinJsonFile, json);
					createVersions(topicFile, json);
					createHistory(topicFile, json);
				}
			}
		}
	}

	private static void createDataset(Folder folder, File file) throws IOException {
		File datasets = new File(file, "datasets");
		datasets.mkdir();

		DatasetsCsv.buildFolders();
		Set<Folder> datasetFolders = DatasetsCsv.folders;

		for (Folder datasetFolder : datasetFolders) {
			if (StringUtils.deleteWhitespace(datasetFolder.name.toLowerCase()).equals(file.getName())) {

				for (Folder topicFolder : datasetFolder.getChildren()) {
					File topicFile = new File(datasets, StringUtils.deleteWhitespace(topicFolder.name.toLowerCase()));
					topicFile.mkdir();
					Dataset dataset = new Dataset();
					dataset.title = topicFolder.name;
					String json = Serialiser.serialise(dataset);
					File datasetJsonFile = new File(topicFile, "data.json");
					FileUtils.writeStringToFile(datasetJsonFile, json);
					createVersions(topicFile, json);
					createHistory(topicFile, json);
				}
			}
		}
	}

	private static boolean isReleaseFolder(Folder o) {
		return o.parent.parent != null && o.parent.parent.name.equals("Releases");
	}

	private static void createRelease(File topicFile, Folder u) throws IOException {
		File subTopicFile = new File(topicFile, u.filename());
		subTopicFile.mkdir();
		System.out.println("\t\t" + subTopicFile.getPath());
		Release release = new Release(u);
		release.name = u.name;
		String json = Serialiser.serialise(release);
		FileUtils.writeStringToFile(new File(subTopicFile, "data.json"), json);
	}

	private static void createVersions(File file, String json) throws IOException {
		// Create the version directory:
		File versionsFolder = new File(file, "versions");
		versionsFolder.mkdir();

		// create a few versions
		for (int i = 1; i < 4; i++) {
			File version = new File(versionsFolder, String.valueOf(i));
			version.mkdir();
			FileUtils.writeStringToFile(new File(version, "data.json"), json);
		}
	}
}
