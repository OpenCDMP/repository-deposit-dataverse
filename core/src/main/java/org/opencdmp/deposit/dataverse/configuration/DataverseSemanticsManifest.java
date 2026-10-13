package org.opencdmp.deposit.dataverse.configuration;

import org.opencdmp.commonmodels.enums.PluginEntityType;
import org.opencdmp.commonmodels.enums.SemanticKeyLevel;
import org.opencdmp.commonmodels.enums.SemanticValueFormat;
import org.opencdmp.commonmodels.models.semantics.SemanticKeyUsage;
import org.opencdmp.commonmodels.models.semantics.SemanticObject;
import org.opencdmp.commonmodels.models.semantics.SemanticVocabulary;
import org.opencdmp.commonmodels.models.semantics.SemanticsManifest;

import java.util.ArrayList;
import java.util.List;

/**
 * The semantics manifest the Dataverse deposit publishes with its configuration: one usage per key the deposit reads
 * into the dataset's citation block, plus the objects (author, contact, related publication, keyword, other
 * identifier) their values make up.
 * <p>
 * Every value is "can use": the deposit always writes the title (the plan's label) and fills the description,
 * subject, authors and contacts from the plan or the configuration, so nothing a template or blueprint leaves out
 * breaks it. Keys are read from blueprint and template inputs alike. The manifest is static: the instance's
 * controlled vocabularies (subjects, contributor types, publication identifier and relation types) are only known
 * from its citation schema, so those values are described rather than listed, and the families of keys built from
 * them are published as prefixes.
 */
public final class DataverseSemanticsManifest {

    public static final String PREFIX = "dataverse";

    private static final List<PluginEntityType> ENTITY_TYPES = List.of(PluginEntityType.Plan, PluginEntityType.Description);

    private static final SemanticsManifest MANIFEST = build();

    private DataverseSemanticsManifest() {
    }

    public static SemanticsManifest get() {
        return MANIFEST;
    }

    private static SemanticsManifest build() {
        List<SemanticKeyUsage> usages = new ArrayList<>();

        usages.add(usage("dataverse.dataset.description", "Description", "Each answer is added as a description, beside the plan's description.", "dsDescription[].dsDescriptionValue", SemanticValueFormat.Text, true, "The plan's description", null));
        usages.add(usage("dataverse.dataset.alternative_title", "Alternative title", "Written as an alternative title; only the first answer is kept when the instance allows one.", "alternativeTitle", SemanticValueFormat.Text, true, null, null));
        usages.add(usage("dataverse.dataset.subtitle", "Subtitle", "Written as the subtitle; the first answer is kept.", "subtitle", SemanticValueFormat.Text, false, null, null));
        usages.add(usage("dataverse.dataset.notes", "Notes", "Written as the notes; the first answer is kept.", "notesText", SemanticValueFormat.Text, false, null, null));
        usages.add(usage("dataverse.dataset.subject", "Subject", "Written as a subject; it must be a subject of the Dataverse instance, others are dropped.", "subject[]", SemanticValueFormat.Choice, true, "The organisation's default subject, else the installation's", null));
        usages.add(usage("dataverse.dataset.alternative_url", "Alternative URL", "Written as the alternative URL; the first answer is kept.", "alternativeURL", SemanticValueFormat.Uri, false, "The plan's public page, for a public plan, unless the organisation turned that off", null));
        usages.add(usage("dataverse.dataset.date_of_deposit", "Date of deposit", "Written as the date of deposit; a value that is not a yyyy-MM-dd date is dropped.", "dateOfDeposit", SemanticValueFormat.Date, false, null, null));
        usages.add(usage("dataverse.identifier", "Dataverse", "The alias of the dataverse the dataset is created in; the first answer is kept.", "dataverses/{alias}/datasets", SemanticValueFormat.Text, false, "The organisation's Dataverse collection, else the installation's", null));

        usages.add(usage("dataverse.author.name", "Author › Name", "Each answer row is added as an author, after the plan members credited as authors.", "author[].authorName", SemanticValueFormat.Text, false, "The plan members the organisation credits as authors", "dataverse.author"));
        usages.add(usage("dataverse.author.affiliation", "Author › Affiliation", "Written as the affiliation of the author of its answer row.", "author[].authorAffiliation", SemanticValueFormat.Text, false, "The plan's organisations, for the plan members credited as authors", "dataverse.author"));

        usages.add(usage("dataverse.point_of_contact.name", "Point of contact › Name", "Each answer row is added as a dataset contact, after the plan's contacts.", "datasetContact[].datasetContactName", SemanticValueFormat.Text, false, "The plan's contacts, else the organisation's contact name", "dataverse.point_of_contact"));
        usages.add(usage("dataverse.point_of_contact.email", "Point of contact › Email", "Written as the email of the contact of its answer row.", "datasetContact[].datasetContactEmail", SemanticValueFormat.Email, false, "The plan's contacts, else the organisation's contact e-mail", "dataverse.point_of_contact"));
        usages.add(usage("dataverse.point_of_contact.affiliation", "Point of contact › Affiliation", "Written as the affiliation of the contact of its answer row.", "datasetContact[].datasetContactAffiliation", SemanticValueFormat.Text, false, null, "dataverse.point_of_contact"));

        SemanticKeyUsage publicationIdentifierType = usage("dataverse.related_publication.identifier_type", "Related publication › Identifier type", "Written as the identifier type of the publication; it must be a publication identifier type of the Dataverse instance, others are dropped.", "publication[].publicationIDType", SemanticValueFormat.Choice, false, null, "dataverse.related_publication");
        publicationIdentifierType.setFromReferenceFields(true);
        usages.add(publicationIdentifierType);
        usages.add(usage("dataverse.related_publication.relation_type", "Related publication › Relation type", "Written as the relation type of the publication; it must be a publication relation type of the Dataverse instance, others are dropped.", "publication[].publicationRelationType", SemanticValueFormat.Choice, false, null, "dataverse.related_publication"));
        usages.add(usage("dataverse.related_publication.citation", "Related publication › Citation", "Written as the citation of the publication.", "publication[].publicationCitation", SemanticValueFormat.Text, false, null, "dataverse.related_publication"));
        SemanticKeyUsage publicationIdentifier = usage("dataverse.related_publication.identifier", "Related publication › Identifier", "Written as the identifier of the publication.", "publication[].publicationIDNumber", SemanticValueFormat.Text, false, null, "dataverse.related_publication");
        publicationIdentifier.setFromReferenceFields(true);
        usages.add(publicationIdentifier);
        usages.add(usage("dataverse.related_publication.url", "Related publication › URL", "Written as the URL of the publication.", "publication[].publicationURL", SemanticValueFormat.Uri, false, null, "dataverse.related_publication"));

        usages.add(usage("dataverse.keyword.term", "Keyword › Term", "Written as the term of the keyword.", "keyword[].keywordValue", SemanticValueFormat.Text, false, null, "dataverse.keyword"));
        usages.add(usage("dataverse.keyword.term_uri", "Keyword › Term URI", "Written as the URI of the keyword's term.", "keyword[].keywordTermURI", SemanticValueFormat.Uri, false, null, "dataverse.keyword"));
        usages.add(usage("dataverse.keyword.controlled_vocabulary_name", "Keyword › Controlled vocabulary name", "Written as the name of the vocabulary the keyword comes from.", "keyword[].keywordVocabulary", SemanticValueFormat.Text, false, null, "dataverse.keyword"));
        usages.add(usage("dataverse.keyword.controlled_vocabulary_url", "Keyword › Controlled vocabulary URL", "Written as the URL of the vocabulary the keyword comes from.", "keyword[].keywordVocabularyURI", SemanticValueFormat.Uri, false, null, "dataverse.keyword"));

        usages.add(usage("dataverse.other_identifier.agency", "Other identifier › Agency", "Written as the agency of the identifier.", "otherId[].otherIdAgency", SemanticValueFormat.Text, false, null, "dataverse.other_identifier"));
        usages.add(usage("dataverse.other_identifier.identifier", "Other identifier › Identifier", "Written as the value of the identifier.", "otherId[].otherIdValue", SemanticValueFormat.Text, false, null, "dataverse.other_identifier"));

        SemanticKeyUsage contributorType = usage("dataverse.contributor.type", "Contributor (by type)",
                "Keys are dataverse.contributor.type.<type>, one per contributor type of the Dataverse instance's citation schema, lowercased with anything not a letter or digit as _ (e.g. dataverse.contributor.type.data_collector); each answer is added as a contributor of that type named by the value, after the plan's users, researchers and funders. Types the instance does not have are ignored.",
                "contributor[].contributorName", SemanticValueFormat.Text, true, null, null);
        contributorType.setPrefix(true);
        usages.add(contributorType);

        SemanticKeyUsage publicationType = usage("dataverse.publication.type", "Related publication (by type)",
                "Keys are dataverse.publication.type.<type>, one per publication identifier type of the Dataverse instance's citation schema, lowercased with anything not a letter or digit as _ (e.g. dataverse.publication.type.doi, dataverse.publication.type.arxiv); each answer of an input carrying one is added as a related publication with that identifier type and the value as its identifier. Types the instance does not have are ignored. For older templates, a relation type written under this prefix (e.g. dataverse.publication.type.iscitedby) still sets the relation type, unless an identifier type has the same key; use dataverse.publication.relation_type instead.",
                "publication[].publicationIDNumber", SemanticValueFormat.Text, true, null, null);
        publicationType.setPrefix(true);
        usages.add(publicationType);

        SemanticKeyUsage publicationRelationType = usage("dataverse.publication.relation_type", "Related publication (by type) › Relation type",
                "Keys are dataverse.publication.relation_type.<type>, one per publication relation type of the Dataverse instance's citation schema, lowercased with anything not a letter or digit as _ (e.g. dataverse.publication.relation_type.iscitedby); put on the same input as a dataverse.publication.type key, it sets the relation type of the related publications that input adds. On its own it writes nothing. Types the instance does not have are ignored.",
                "publication[].publicationRelationType", SemanticValueFormat.Text, false, null, null);
        publicationRelationType.setPrefix(true);
        usages.add(publicationRelationType);

        SemanticsManifest manifest = new SemanticsManifest(List.of(new SemanticVocabulary(PREFIX, "Dataverse (citation block)", false)), usages);
        manifest.setObjects(List.of(
                object("dataverse.author", "Author", "author[]"),
                object("dataverse.point_of_contact", "Point of contact", "datasetContact[]"),
                object("dataverse.related_publication", "Related publication", "publication[]"),
                object("dataverse.keyword", "Keyword", "keyword[]"),
                object("dataverse.other_identifier", "Other identifier", "otherId[]")
        ));
        return manifest;
    }

    private static SemanticKeyUsage usage(String key, String label, String purpose, String target, SemanticValueFormat format, boolean multiple, String fallback, String object) {
        SemanticKeyUsage usage = new SemanticKeyUsage(List.of(key), false, SemanticKeyLevel.CanUse, ENTITY_TYPES, label, purpose, target);
        usage.setFormat(format);
        usage.setMultiple(multiple);
        usage.setFallback(fallback);
        usage.setObject(object);
        return usage;
    }

    /** An object the deposit writes one of per answer row of the question or section carrying its keys. */
    private static SemanticObject object(String key, String label, String target) {
        SemanticObject object = new SemanticObject();
        object.setKey(key);
        object.setLabel(label);
        object.setMultiple(true);
        object.setEntityTypes(ENTITY_TYPES);
        object.setTarget(target);
        return object;
    }
}
