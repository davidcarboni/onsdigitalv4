package com.github.onsdigital.generator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.github.davidcarboni.restolino.json.Serialiser;
import com.github.onsdigital.generator.data.Data;
import com.github.onsdigital.generator.data.DatasetMappingsCSV;
import com.github.onsdigital.generator.datasets.DatasetContent;
import com.github.onsdigital.json.dataset.Dataset;
import com.github.onsdigital.json.markdown.Article;
import com.github.onsdigital.json.markdown.Bulletin;
import com.github.onsdigital.json.markdown.Methodology;
import com.github.onsdigital.json.taxonomy.DataItemLink;
import com.github.onsdigital.json.taxonomy.HomeSection;
import com.github.onsdigital.json.taxonomy.T1;
import com.github.onsdigital.json.taxonomy.T2;
import com.github.onsdigital.json.taxonomy.T3;
import com.github.onsdigital.json.timeseries.Timeseries;

public class TaxonomyGenerator {

	static File root;
	static Set<Timeseries> created = new HashSet<>();
	static List<Folder> oldDatasetsCreated = new ArrayList<>();
	static Set<Timeseries> noData = new TreeSet<>();

	/**
	 * Parses the taxonomy CSV file and generates a file structure..
	 * 
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		Serialiser.getBuilder().setPrettyPrinting();

		// Set up the taxonomy and trigger CSV parsing:
		Data.parse();

		// Walk folder tree:
		root = new File("src/main/taxonomy");
		Folder rootFolder = new Folder();
		rootFolder.name = "Home";
		Set<Folder> folders = Data.folders();
		rootFolder.addChildren(folders);
		createHomePage(rootFolder, root);
		File themeFile;
		File subjectFile;
		File topicFile;
		for (Folder t : folders) {
			themeFile = new File(root, t.filename());
			themeFile.mkdirs();
			System.out.println("Theme  : " + themeFile.getAbsolutePath());
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
				System.out.println("Subjet: \t" + subjectFile.getPath());
				for (Folder o : s.getChildren()) {
					topicFile = new File(subjectFile, o.filename());
					topicFile.mkdirs();
					System.out.println("Topic  :\t\t" + topicFile.getPath());
					createT3(o, topicFile);
					if (o.getChildren().size() == 0) {
						// createContentFolders(o.name, topicFile);
					}
				}
			}
		}

		// Print out metrics and warnings that provide information on whether
		// the process is working as expected:

		// System.out.println(Data.getDateLabels());
		System.out.println("Timeseries with no data: " + noData + " (" + noData.size() + ")");
		System.out.println("You have a grand total of " + created.size() + " timeseries, out of a total possible " + Data.size() + " parsed timeseries.");
		System.out.println("There are a total of " + Data.sizeDatasets() + " CDIDs classified into one or more datasets.");
		Set<String> unmappedDatasets = Data.unmappedDatasets();
		if (unmappedDatasets.size() > 0) {
			System.out.println("To increase this number, please add mappings for the following datasets: " + unmappedDatasets);
			for (String datasetName : unmappedDatasets) {
				if (!"other".equals(datasetName)) {
					System.out.println(" - " + datasetName + " contains " + Data.dataset(datasetName).size());
				}
			}
		}
		if (Data.dataset("other") != null) {
			System.out.println("The 'other' dataset contains " + Data.dataset("other").size() + " timeseries.");
		}

		Set<Folder> mapped = new TreeSet<>();
		for (Folder folder : DatasetMappingsCSV.mappedFolders.values()) {
			mapped.add(folder);
		}
		if (oldDatasetsCreated.size() != mapped.size()) {
			System.out.println(oldDatasetsCreated.size() + " old datasets have been created, from a total of " + Data.sizeDatasetsCount());
			Collections.sort(oldDatasetsCreated);
			for (Folder folder : oldDatasetsCreated) {
				System.out.println(" - " + folder.path());
			}
			System.out.println("Expected the following:");
			for (Folder folder : mapped) {
				System.out.println(" - " + folder.path());
			}
		}

		int yes = 0;
		int no = 0;
		int missing = 0;
		for (Timeseries timeseries : new Data()) {
			if (timeseries.uri == null) {
				missing++;
			} else {
				File path = new File(root, timeseries.uri.toString());
				path = new File(path, "data.json");
				if (path.exists()) {
					yes++;
				} else {
					no++;
				}
			}
			if (!timeseries.cdid().matches("[A-Z0-9]{3,8}")) {
				throw new RuntimeException("CDID " + timeseries + " is not in the expected format.");
			}
		}
		System.out.println(yes + " timeseries have been verified to exist on disk.");
		if (no > 0) {
			System.out.println("Warning: " + no + " timeseries don't actually exist on disk");
		}
		if (missing > 0) {
			System.out.println(missing + " timeseries have no URI set (suggesting they can't be written to the taxonomy)");
		}
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
		if (folder.oldDataset.size() > 0) {
			throw new RuntimeException("A dataset has been mapped to " + folder + " but this folder is the homepage.");
		}
	}

	private static void createT2(Folder folder, File file) throws IOException {
		if (folder.name.equals("Releases") || (folder.parent != null && folder.parent.name.equals("Releases"))) {
			System.out.println("Do not create json for Releases createT2");
		} else {
			T2 t2 = new T2(folder, folder.index);
			if (StringUtils.isNotBlank(folder.lede)) {
				t2.lede = folder.lede;
				t2.more = folder.more;
			}
			t2.index = folder.index;
			buildT2Sections(t2, folder);

			String json = Serialiser.serialise(t2);
			FileUtils.writeStringToFile(new File(file, "data.json"), json);
		}
		if (folder.oldDataset.size() > 0) {
			throw new RuntimeException("A dataset has been mapped to " + folder + " but this folder is a T2.");
		}
	}

	private static void buildT2Sections(T2 t2, Folder folder) throws IOException {
		t2.sections = new ArrayList<HomeSection>();

		for (Folder child : folder.getChildren()) {
			HomeSection section = new HomeSection(child.name, child.filename());
			section.index = child.index;
			t2.sections.add(section);

			if (child.getChildren().size() > 0) { // t2 page at level below
				// Add child of sections to t2 page
				for (Folder grandChild : child.getChildren()) {
					section.items.add(new DataItemLink(grandChild.name, URI.create(grandChild.filename())));
				}
			} else { // T3 page at below level
				for (Timeseries grandChild : child.timeserieses) {
					section.items.add(new DataItemLink(grandChild.name, grandChild.uri));

				}
			}

			// Loading timeseries for t2 is no longer needed
			// List<Folder> t3Folders = getT3Folders(child);
			// Collection<URI> timeserieses = getTimeseries(t3Folders);
			// for (URI timeseries : timeserieses) {
			// section.items.add(timeseries);
			// }
		}
		Collections.sort(t2.sections);
	}

	private static List<Folder> getT3Folders(Folder folder) {
		List<Folder> result = new ArrayList<Folder>();

		// If the folder is t3, add it directly:
		if (folder.getChildren().size() == 0) {
			result.add(folder);
		}

		// If it's a t2, recurse:
		for (Folder child : folder.getChildren()) {
			result.addAll(getT3Folders(child));
		}
		return result;
	}

	private static Set<URI> getTimeseries(List<Folder> t3Folders) throws IOException {
		// Keep keys in the order they are added, but allow for de-duplication:
		Set<URI> result = new LinkedHashSet<>();

		// Add the headlines first so that they will appear first
		for (Folder t3Folder : t3Folders) {
			if (t3Folder.headline != null) {
				result.add(t3Folder.headline.uri);
			}
		}

		// Add the other items in case there aren't enough headline items:
		for (Folder t3Folder : t3Folders) {
			// Limit the number in case we have thousands
			// (this is quite likely at some point)
			int max = 4;
			for (Timeseries timeseries : t3Folder.timeserieses) {
				if (max-- < 0) {
					if (timeseries.uri != null) {
						result.add(timeseries.uri);
					} else {
						System.out.println("No URI defined for " + timeseries + " when scanning to " + t3Folder);
					}
				}
			}
		}

		return result;
	}

	private static void createT3(Folder folder, File file) throws IOException {

		T3 t3 = new T3(folder);
		if (StringUtils.isNotBlank(folder.lede)) {
			t3.lede = folder.lede;
			t3.more = folder.more;
		}
		t3.index = folder.index;

		// Timeseries references:
		if (folder.headline != null && folder.headline.uri != null) {
			t3.headline = new DataItemLink(folder.headline.name, folder.headline.uri);
		} else {
			System.out.println("No headline URI set for " + folder.name);
			if (folder.timeserieses.size() > 0 && folder.timeserieses.get(0).uri != null) {
				Timeseries headline = folder.timeserieses.get(0);
				t3.headline = new DataItemLink(headline.name, headline.uri);
				System.out.println("Using the first item from the timeseries list instead: " + t3.headline);
			}
		}
		List<Timeseries> timeserieses = folder.timeserieses;
		t3.items.clear();
		String baseUri = "/" + folder.filename();
		Folder parent = folder.parent;
		while (parent != null) {
			baseUri = "/" + parent.filename() + baseUri;
			parent = parent.parent;
		}
		baseUri += "/timeseries";
		for (Timeseries timeseries : timeserieses) {
			if (timeseries.uri != null) {
				t3.items.add(new DataItemLink(timeseries.name, timeseries.uri));
			} else {
				System.out.println("No URI set for " + timeseries);
			}
		}

		// creating associated data for t3
		createStatsBulletinHeadline(folder, t3);
		createStatsBulletins(folder, t3);
		createDatasets(folder, t3);

		// Serialise
		String json = Serialiser.serialise(t3);
		FileUtils.writeStringToFile(new File(file, "data.json"), json);

		createBulletin(folder, file, t3);
		createArticle(folder, file, t3);
		createMethodology(folder, file, t3);
		createDataset(folder, file, t3);
		createTimeseries(folder, file, t3);
	}

	private static void createDatasets(Folder folder, T3 t3) throws IOException {
		t3.datasets.clear();
		List<Dataset> datasets = DatasetContent.getDatasets(folder);

		if (datasets != null) {
			for (Dataset dataset : datasets) {
				if (dataset.summary != null) {
					URI datasetUri = toDatasetUri(folder, dataset);
					t3.datasets.add(new DataItemLink(dataset.name, datasetUri));
				}
			}
		}
	}

	private static void createStatsBulletins(Folder folder, T3 t3) throws IOException {
		t3.statsBulletins.clear();

		for (Bulletin bulletin : folder.bulletins) {
			// Summary is used as a trigger to determine whether or not to
			// add this bulletin to the list of related bulletins. This is a
			// bit of a workaround for now.
			if (bulletin.summary != null) {
				URI bulletinUri = toStatsBulletinUri(folder, bulletin);
				t3.statsBulletins.add(new DataItemLink(bulletin.name, bulletinUri));
			}
		}
	}

	private static void createStatsBulletinHeadline(Folder folder, T3 t3) throws IOException {
		// Stats bulletin references:

		URI statsBulletinHeadlineUri = toStatsBulletinUri(folder, folder.headlineBulletin);
		if (statsBulletinHeadlineUri != null) {
			t3.statsBulletinHeadline = new DataItemLink(folder.headlineBulletin.name, statsBulletinHeadlineUri);
		}
	}

	private static URI toStatsBulletinUri(Folder folder, Bulletin bulletin) {
		URI result = null;

		if (bulletin != null) {
			if (bulletin.uri == null) {
				String baseUri = "/" + folder.filename();
				Folder parent = folder.parent;
				while (parent != null) {
					baseUri = "/" + parent.filename() + baseUri;
					parent = parent.parent;
				}
				baseUri += "/bulletins";
				String bulletinFileName = bulletin.fileName;
				if (bulletinFileName == null) {
					System.out.println("No filename for : " + bulletin.name);
				}
				String sanitizedBulletinFileName = bulletinFileName.replaceAll("\\W", "");
				bulletin.uri = URI.create(baseUri + "/" + StringUtils.deleteWhitespace(sanitizedBulletinFileName));
			}
			result = bulletin.uri;
		}

		return result;
	}

	private static URI toDatasetUri(Folder folder, Dataset dataset) {
		URI result = null;

		if (dataset != null) {
			if (dataset.uri == null) {
				String baseUri = "/" + folder.filename();
				Folder parent = folder.parent;
				while (parent != null) {
					baseUri = "/" + parent.filename() + baseUri;
					parent = parent.parent;
				}
				baseUri += "/datasets";
				String datasetFileName = dataset.fileName;
				String sanitizedDatasetFileName = datasetFileName.replaceAll("\\W", "");
				dataset.uri = URI.create(baseUri + "/" + StringUtils.deleteWhitespace(sanitizedDatasetFileName));
			}
			result = dataset.uri;
		}

		return result;
	}

	/**
	 * Creates timeseries data.
	 *
	 * @param folder
	 * @param file
	 * @param t3
	 * @throws IOException
	 */
	private static void createTimeseries(Folder folder, File file, T3 t3) throws IOException {

		int created = 0;

		Set<Timeseries> timeserieses = new HashSet<>(folder.timeserieses);
		if (folder.headline != null) {
			timeserieses.add(folder.headline);
		}

		// Write out timeseries specified by the Alpha Content spreadsheet:
		for (Timeseries timeseries : timeserieses) {

			if (createTimeseries(timeseries, folder, t3)) {
				created++;
			}
		}

		// TODO: Other timeseries mappings are commented out to minimise volume
		// of files for now:
		// // Write out timeseries mapped according to the "old dataset"
		// // taxonomy map:
		// Set<Timeseries> total = new HashSet<Timeseries>(timeserieses);
		// if (folder.oldDataset.size() > 0) {
		// oldDatasetsCreated.add(folder);
		// for (Set<Timeseries> dataset : folder.oldDataset) {
		// for (Timeseries timeseries : dataset) {
		//
		// if (createTimeseries(timeseries, t3)) {
		// created++;
		// }
		// }
		// total.addAll(dataset);
		// }
		//
		// System.out.println("Referenced CDIDs vs. total CDIDs at this node: "
		// + timeserieses.size() + "/" + total.size() + " (" + created +
		// " created)");
		// }
	}

	private static boolean createTimeseries(Timeseries timeseries, Folder folder, T3 t3) throws IOException {
		boolean result = false;

		URI uri = timeseries.uri;
		File timeseriesFolder = new File(root, uri.toString());
		File timeseriesFile = new File(timeseriesFolder, "data.json");

		if (uri.toString().contains(t3.fileName)) {
			// Only create the timeseries if it doesn't already exist:
			if (!timeseriesFile.exists()) {
				timeseriesFolder.mkdirs();

				timeseries.setBreadcrumb(t3);
				if (timeseries.months.size() == 0 && timeseries.quarters.size() == 0 && timeseries.years.size() == 0) {
					noData.add(timeseries);
				}

				for (Bulletin bulletin : folder.bulletins) {
					timeseries.relatedBulletins.add(bulletin.uri);
				}

				List<Timeseries> relatedCdids = Data.relatedTimeseries(timeseries);
				if (relatedCdids != null && !relatedCdids.isEmpty()) {
					for (Timeseries relatedCdid : relatedCdids) {
						Timeseries relatedTimeseries = Data.timeseries(relatedCdid.cdid());
						timeseries.relatedTimeseries.add(relatedTimeseries.uri);
					}
				}

				String json = Serialiser.serialise(timeseries);
				FileUtils.writeStringToFile(timeseriesFile, json, Charset.forName("UTF8"));
				created.add(timeseries);
				result = true;
			}
		}
		return result;
	}

	private static void createBulletin(Folder folder, File file, T3 t3) throws IOException {
		if (folder.bulletins.size() > 0) {
			File bulletinsFolder = new File(file, "bulletins");
			bulletinsFolder.mkdir();
			for (Bulletin bulletin : folder.bulletins) {
				bulletin.setBreadcrumb(t3);
				File bulletinFolder = new File(bulletinsFolder, StringUtils.deleteWhitespace(bulletin.fileName));
				String json = Serialiser.serialise(bulletin);
				FileUtils.writeStringToFile(new File(bulletinFolder, "data.json"), json, Charset.forName("UTF8"));
			}
		}
	}

	private static void createArticle(Folder folder, File file, T3 t3) throws IOException {
		if (folder.articles.size() > 0) {
			File articlesFolder = new File(file, "articles");
			articlesFolder.mkdir();
			for (Article article : folder.articles) {
				article.setBreadcrumb(t3);
				File bulletinFolder = new File(articlesFolder, StringUtils.deleteWhitespace(article.fileName));
				String json = Serialiser.serialise(article);
				FileUtils.writeStringToFile(new File(bulletinFolder, "data.json"), json, Charset.forName("UTF8"));
			}
		}
	}

	private static void createMethodology(Folder folder, File file, T3 t3) throws IOException {
		if (folder.methodology.size() > 0) {
			File methodologyFolder = new File(file, "methodology");
			methodologyFolder.mkdir();
			for (Methodology methodology : folder.methodology) {
				methodology.setBreadcrumb(t3);
				File bulletinFolder = new File(methodologyFolder, StringUtils.deleteWhitespace(methodology.fileName));
				String json = Serialiser.serialise(methodology);
				FileUtils.writeStringToFile(new File(bulletinFolder, "data.json"), json, Charset.forName("UTF8"));
			}
		}
	}

	private static void createDataset(Folder folder, File file, T3 t3) throws IOException {
		List<Dataset> datasets = DatasetContent.getDatasets(folder);

		if (datasets != null && datasets.size() > 0) {
			File datasetsFolder = new File(file, "datasets");
			datasetsFolder.mkdir();
			for (Dataset dataset : datasets) {
				dataset.setBreadcrumb(t3);
				String datasetFileName = dataset.fileName.replaceAll("\\W", "");
				File datasetFolder = new File(datasetsFolder, StringUtils.deleteWhitespace(datasetFileName.toLowerCase()));
				String json = Serialiser.serialise(dataset);
				FileUtils.writeStringToFile(new File(datasetFolder, "data.json"), json, Charset.forName("UTF8"));
			}
		}
	}

}
