package eu.fbk.premon.vocab;

import org.openrdf.model.Namespace;
import org.openrdf.model.URI;
import org.openrdf.model.impl.NamespaceImpl;
import org.openrdf.model.impl.ValueFactoryImpl;

/**
 * Constants for the VARTRANS vocabulary, variation and translation module of W3C LEMON draft.
 */
public class VARTRANS {

    /** Recommended prefix for the vocabulary namespace: "vartrans". */
    public static final String PREFIX = "vartrans";

    /** Vocabulary namespace: "http://www.w3.org/ns/lemon/vartrans.owl#". */
    public static final String NAMESPACE = "http://www.w3.org/ns/lemon/vartrans.owl#";

    /** Immutable {@link Namespace} constant for the vocabulary namespace. */
    public static final Namespace NS = new NamespaceImpl(PREFIX, NAMESPACE);

    // Classes

    /** Class vartrans:LexicalRelation. */
    public static final URI LEXICAL_RELATION = createURI("LexicalRelation");

    /** Class vartrans:LexicoSemanticRelation. */
    public static final URI LEXICO_SEMANTIC_RELATION = createURI("LexicoSemanticRelation");

    /** Class vartrans:SenseRelation. */
    public static final URI SENSE_RELATION = createURI("SenseRelation");

    /** Class vartrans:TerminologicalRelation. */
    public static final URI TERMINOLOGICAL_RELATION = createURI("TerminologicalRelation");

    /** Class vartrans:Translation. */
    public static final URI TRANSLATION_C = createURI("Translation");

    /** Class vartrans:TranslationSet. */
    public static final URI TRANSLATION_SET = createURI("TranslationSet");

    // Object properties

    /** Object property vartrans:category. */
    public static final URI CATEGORY = createURI("category");

    /** Object property vartrans:lexicalRel. */
    public static final URI LEXICAL_REL = createURI("lexicalRel");

    /** Object property vartrans:member. */
    public static final URI MEMBER = createURI("member");

    /** Object property vartrans:relates. */
    public static final URI RELATES = createURI("relates");

    /** Object property vartrans:senseRel. */
    public static final URI SENSE_REL = createURI("senseRel");

    /** Object property vartrans:source. */
    public static final URI SOURCE = createURI("source");

    /** Object property vartrans:target. */
    public static final URI TARGET = createURI("target");

    /** Object property vartrans:translatableAs. */
    public static final URI TRANSLATABLE_AS = createURI("translatableAs");

    /** Object property vartrans:translation. */
    public static final URI TRANSLATION_P = createURI("translation");

    // Utility methods

    private static URI createURI(final String localName) {
        return ValueFactoryImpl.getInstance().createURI(NAMESPACE, localName);
    }

    private VARTRANS() {
    }

}
