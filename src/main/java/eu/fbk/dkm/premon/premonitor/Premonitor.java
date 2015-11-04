package eu.fbk.dkm.premon.premonitor;

import com.google.common.collect.*;
import eu.fbk.dkm.premon.vocab.PM;
import eu.fbk.dkm.utils.CommandLine;
import eu.fbk.rdfpro.*;
import eu.fbk.rdfpro.util.QuadModel;
import eu.fbk.rdfpro.util.Statements;
import org.openrdf.model.*;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Created by alessio on 29/10/15.
 */

public class Premonitor {

    private static final String DEFAULT_PATH = ".";
    private static final String DEFAULT_LANGUAGE = "en";

    private static final String DEFAULT_PB_FOLDER = "pb";
    private static final String DEFAULT_PB_SOURCE = "pb17";
    private static final String DEFAULT_PB_SOURCE_ON = "pbon5";

    private static final String DEFAULT_NB_FOLDER = "nb";
    private static final String DEFAULT_NB_SOURCE = "nb10";

    private static final URI LEMON_LEXICAL_ENTRY = Statements.VALUE_FACTORY
            .createURI("http://lemon-model.net/lemon#LexicalEntry");

    private static final Logger LOGGER = LoggerFactory.getLogger(Premonitor.class);

    private static final ValueFactory VALUE_FACTORY;
    public static final Map<String, URI> LANGUAGE_CODES_TO_URIS;

    static {
        VALUE_FACTORY = ValueFactoryImpl.getInstance();
        final Map<String, URI> codesToURIs = Maps.newHashMap();
        for (final String language : Locale.getISOLanguages()) {
            final Locale locale = new Locale(language);
            final URI uri = VALUE_FACTORY
                    .createURI("http://lexvo.org/id/iso639-3/", locale.getISO3Language());
            codesToURIs.put(language, uri);
        }
        LANGUAGE_CODES_TO_URIS = ImmutableMap.copyOf(codesToURIs);
    }

    public static void main(String[] args) {

        List<Statement> statements = new ArrayList<>();
        RDFHandler handler = RDFHandlers.wrap(statements);

        try {
            final CommandLine cmd = CommandLine.parser().withName("./premonitor")
                    .withHeader("Transform linguistic resources into RDF").withOption("i", "input",
                            String.format("input folder (default %s)", DEFAULT_PATH), "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
                    .withOption("w", "output", "Output file (mandatory)", "FILE",
                            CommandLine.Type.FILE, true, false, true).withOption("l", "language",
                            String.format("Language for literals, default %s", DEFAULT_LANGUAGE),
                            "ISO-CODE", CommandLine.Type.STRING, true, false, false)
                    .withOption("s", "single", "Extract single lemma", "LEMMA",
                            CommandLine.Type.STRING, true, false, false)

                    .withOption(null, "pb-non-verbs",
                            "Extract also non-verbs (only for OntoNotes)")
                    .withOption(null, "pb-ontonotes",
                            "Specify that this is an OntoNotes version of ProbBank")
                    .withOption(null, "pb-examples", "Extract examples for PropBank")
                    .withOption(null, "pb-folder", "PropBank frames folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
                    .withOption(null, "pb-no-def", "Skip PropBank definitions")
                    .withOption(null, "pb-source",
                            String.format("PropBank source, default %s/%s", DEFAULT_PB_SOURCE,
                                    DEFAULT_PB_SOURCE_ON), "SOURCE", CommandLine.Type.STRING, true,
                            false, false)

                    .withOption(null, "nb-examples", "Extract examples for NomBank")
                    .withOption(null, "nb-folder", "NomBank frames folder", "FOLDER",
                            CommandLine.Type.DIRECTORY_EXISTING, true, false, false)
                    .withOption(null, "nb-no-def", "Skip NomBank definitions").withOption(null,
                            "nb-source",
                            String.format("NomBank source, default %s", DEFAULT_NB_SOURCE),
                            "SOURCE", CommandLine.Type.STRING, true, false, false)

                            //					.withOption(null, "use-wn-lex", "Use WordNet LexicalEntries when available")
                            //					.withOption(null, "namespace", String.format("Namespace, default %s", DEFAULT_NAMESPACE), "URI", CommandLine.Type.STRING, true, false, false)
                    .withOption(null, "wordnet", "WordNet RDF triple file", "FILE",
                            CommandLine.Type.FILE_EXISTING, true, false, false)
                            //					.withOption(null, "framenet", "FrameNet RDF triple file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)
                            //					.withOption(null, "verbnet", "VerbNet RDF triple file", "FILE", CommandLine.Type.FILE_EXISTING, true, false, false)

                    .withOption("c", "closure", "Emits the RDFS closure of generated RDF quads")
                    .withOption("d", "divide",
                            "Divides the output in multiple files based on provenance")
                    .withOption("x", "stats",
                            "Generates also VOID statistics for produced RDF files")

                    .withLogger(LoggerFactory.getLogger("eu.fbk")).parse(args);

            File outputFile = cmd.getOptionValue("output", File.class);
            File inputFolder = new File(DEFAULT_PATH);
            if (cmd.hasOption("input")) {
                inputFolder = cmd.getOptionValue("input", File.class);
            }

            String language = DEFAULT_LANGUAGE;
            if (cmd.hasOption("language")) {
                language = cmd.getOptionValue("language", String.class);
            }

            final HashSet<URI> wnURIs = new HashSet<>();
            if (cmd.hasOption("wordnet")) {
                File wnRDF = cmd.getOptionValue("wordnet", File.class);
                if (wnRDF != null) {
                    LOGGER.info("Loading WordNet");
                    RDFSource source = RDFSources
                            .read(true, true, null, null, wnRDF.getAbsolutePath());
                    source.emit(new AbstractRDFHandler() {

                        @Override public void handleStatement(Statement statement)
                                throws RDFHandlerException {
                            if (statement.getPredicate().equals(RDF.TYPE) && statement.getObject()
                                    .equals(LEMON_LEXICAL_ENTRY)) {
                                if (statement.getSubject() instanceof URI) {
                                    synchronized (wnURIs) {
                                        wnURIs.add((URI) statement.getSubject());
                                    }
                                }
                            }
                        }
                    }, 1);
                    LOGGER.info("Loaded {} URIs", wnURIs.size());
                }
            }

            Properties properties = new Properties();

            if (cmd.hasOption("single")) {
                properties.setProperty("only-one", cmd.getOptionValue("single", String.class));
            }

            // Resource-dependent part: PropBank

            File pbFolder = new File(
                    inputFolder.getAbsolutePath() + File.separator + DEFAULT_PB_FOLDER);
            if (cmd.hasOption("pb-folder")) {
                pbFolder = cmd.getOptionValue("pb-folder", File.class);
            }

            if (pbFolder.exists()) {
                LOGGER.info("Starting PropBank extraction");

                if (cmd.hasOption("pb-non-verbs")) {
                    properties.setProperty("pb-non-verbs", "1");
                }

                if (cmd.hasOption("pb-no-def")) {
                    properties.setProperty("pb-no-def", "1");
                }

                if (cmd.hasOption("pb-examples")) {
                    properties.setProperty("pb-examples", "1");
                }

                String source = DEFAULT_PB_SOURCE;
                if (cmd.hasOption("pb-ontonotes")) {
                    properties.setProperty("pb-ontonotes", "1");
                    source = DEFAULT_PB_SOURCE_ON;
                }
                if (cmd.hasOption("pb-source")) {
                    source = cmd.getOptionValue("pb-source", String.class);
                }
                properties.put("pb-source", source);

                PropbankConverter propbankConverter = new PropbankConverter(pbFolder, handler,
                        properties, language, wnURIs);

                try {
                    propbankConverter.convert();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                LOGGER.info("Skipping PropBank (folder {} does not exist)",
                        pbFolder.getAbsolutePath());
            }

            // Resource-dependent part: NomBank

            File nbFolder = new File(
                    inputFolder.getAbsolutePath() + File.separator + DEFAULT_NB_FOLDER);
            if (cmd.hasOption("nb-folder")) {
                nbFolder = cmd.getOptionValue("nb-folder", File.class);
            }

            if (nbFolder.exists()) {
                LOGGER.info("Starting NomBank extraction");

                if (cmd.hasOption("nb-no-def")) {
                    properties.setProperty("nb-no-def", "1");
                }

                if (cmd.hasOption("nb-examples")) {
                    properties.setProperty("nb-examples", "1");
                }

                String source = DEFAULT_NB_SOURCE;
                if (cmd.hasOption("nb-source")) {
                    source = cmd.getOptionValue("nb-source", String.class);
                }
                properties.put("nb-source", source);

                NombankConverter nombankConverter = new NombankConverter(nbFolder, handler,
                        properties, language, wnURIs);

                try {
                    nombankConverter.convert();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                LOGGER.info("Skipping NomBank (folder {} does not exist)",
                        nbFolder.getAbsolutePath());
            }

            try {
                final boolean computeClosure = cmd.hasOption("c");
                final boolean divideByProvenance = cmd.hasOption("d");
                final boolean emitStatistics = cmd.hasOption("x");
                emit(outputFile, statements, computeClosure, true, divideByProvenance,
                        emitStatistics);
            } catch (Exception e) {
                LOGGER.error("IO error, some files might not have been properly saved ({})",
                        e.getMessage());
                throw new RDFHandlerException(e);
            }

            //			RDFSource rdfSource = RDFSources.wrap(statements);
            //			try {
            //				RDFHandler rdfHandler = RDFHandlers.write(null, 1000, outputFile.getAbsolutePath());
            //				RDFProcessors
            //						.sequence(RDFProcessors.prefix(null), RDFProcessors.unique(false))
            //						.apply(rdfSource, rdfHandler, 1);
            //			} catch (Exception e) {
            //				LOGGER.error("Input/output error, the file {} has not been saved ({})", outputFile.getAbsolutePath(), e.getMessage());
            //				throw new RDFHandlerException(e);
            //			}
            //			LOGGER.info("File {} saved", outputFile.getAbsolutePath());

        } catch (Throwable ex) {
            CommandLine.fail(ex);
        }

    }

    private static void emit(File outputPath, Iterable<Statement> stmts, boolean computeClosure,
            boolean mergeContexts, boolean divideByProvenance, boolean emitStatistics)
            throws RDFHandlerException {

        // Split the output path in base + ext, using last '.' character
        final String outputStr = outputPath.getAbsolutePath();
        final int index =
                outputStr.length() - (outputPath.getName().length() - outputPath.getName()
                        .indexOf('.'));
        final String outputBase = outputStr.substring(0, index);
        final String outputExt = outputStr.substring(index);

        // Perform inference and context merging, if requested
        if (computeClosure || mergeContexts) {
            List<Statement> processedStmts = Lists.newArrayList();
            RDFProcessor processor = RDFProcessors.IDENTITY;
            if (computeClosure) {
                final RDFSource tbox = RDFSources.read(false, true, null, null,
                        "classpath:/eu/fbk/dkm/premon/premonitor/tbox.ttl");
                processor = RDFProcessors.sequence(processor, RDFProcessors
                        .rdfs(tbox, null, true, true, "rdfs2", "rdfs3", "rdfs4a", "rdfs4b",
                                "rdfs8"));
            }
            if (mergeContexts) {
                processor = RDFProcessors.sequence(processor, RDFProcessors.unique(true));
            }
            processor.apply(RDFSources.wrap(stmts),
                    RDFHandlers.synchronize(RDFHandlers.wrap(processedStmts)), 1);
            stmts = processedStmts;
        }

        // Split by provenance, calling back this method for each dataset
        if (divideByProvenance) {
            // Get a QuadModel with the statements to divide
            final QuadModel model = stmts instanceof QuadModel ?
                    (QuadModel) stmts :
                    QuadModel.create(stmts);

            // Extract the graphs associated to each provenance node
            final Multimap<Value, Resource> sourceToGraphs = HashMultimap.create();
            final Multimap<Resource, Value> graphToSources = HashMultimap.create();
            for (Statement stmt : model.filter(null, DCTERMS.SOURCE, null, PM.META)) {
                sourceToGraphs.put(stmt.getObject(), stmt.getSubject());
                graphToSources.put(stmt.getSubject(), stmt.getObject());
            }

            // Generate a dataset for each provenance node
            for (Map.Entry<Value, Collection<Resource>> entry : sourceToGraphs.asMap()
                    .entrySet()) {

                // Select the RDF content of the dataset
                final List<Statement> dataset = Lists.newArrayList();
                for (Resource graph : entry.getValue()) {
                    for (Value source : graphToSources.get(graph)) {
                        dataset.add(Statements.VALUE_FACTORY
                                .createStatement(graph, DCTERMS.SOURCE, source, PM.META));
                    }
                }
                for (Resource graph : entry.getValue()) {
                    dataset.addAll(model.filter(null, null, null, graph));
                }

                // Assign a name to the dataset
                final String str = (entry.getKey() instanceof URI ?
                        ((URI) entry.getKey()).getLocalName() :
                        entry.getKey().stringValue()).toLowerCase();

                // Delegate
                emit(new File(outputBase + "-" + str + outputExt), dataset, false, false, false,
                        emitStatistics);
            }

        } else {
            // Emit the dataset
            LOGGER.info("Writing dataset to {}", outputPath.getAbsolutePath());
            RDFProcessors.prefix(null).apply(RDFSources.wrap(stmts),
                    RDFHandlers.write(null, 1000, outputPath.getAbsolutePath()), 1);

            // Emit statistics, if requested
            if (emitStatistics) {
                final String name = outputBase + ".stats" + outputExt;
                LOGGER.info("Writing statistics to {}", name);
                RDFProcessors.stats(null, null, null, null, false)
                        .apply(RDFSources.wrap(stmts), RDFHandlers.write(null, 1000, name), 1);
            }
        }
    }

}
