package eu.fbk.dkm.pb2rdf;

import org.openrdf.model.URI;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * Created by alessio on 09/10/15.
 */

public class LEMON {

	static final ValueFactoryImpl factory = ValueFactoryImpl.getInstance();

	static final String NAMESPACE = "http://lemon-model.net/lemon#";
	static final URI LEXICAL_ENTRY = factory.createURI(NAMESPACE, "LexicalEntry");
	static final URI LEXICAL_SENSE = factory.createURI(NAMESPACE, "LexicalSense");
	static final URI ARGUMENT = factory.createURI(NAMESPACE, "Argument");
	static final URI SENSE_DEFINITION = factory.createURI(NAMESPACE, "SenseDefinition");
	static final URI USAGE_EXAMPLE = factory.createURI(NAMESPACE, "UsageExample");

	static final URI SENSE = factory.createURI(NAMESPACE, "sense");
	static final URI VALUE = factory.createURI(NAMESPACE, "value");
	static final URI DEFINITION = factory.createURI(NAMESPACE, "definition");
	static final URI EXAMPLE = factory.createURI(NAMESPACE, "example");

}
