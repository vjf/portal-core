package org.auscope.portal.core.services.responses.vocab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.auscope.portal.core.services.namespaces.VocabNamespaceContext;
import org.auscope.portal.core.util.DOMUtil;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A factory class for creating instances of skos:Concept and child classes
 *
 * @author Josh Vote
 *
 */
public class ConceptFactory {

    private final Log log = LogFactory.getLog(getClass());
    private static final VocabNamespaceContext nc = new VocabNamespaceContext();

    /**
     * Parses a owl:NamedIndividual element (ignoring any relations)
     *
     * If the specified node is missing requisite data null will be returned
     *
     * @param node
     * @return
     * @throws XPathExpressionException
     */
    protected NamedIndividual attemptParseNamedIndividual(final Node node) throws XPathExpressionException {
        final String urn = (String) DOMUtil.compileXPathExpr("@rdf:about", nc).evaluate(node, XPathConstants.STRING);
        if (urn == null || urn.isEmpty()) {
            return null;
        }

        final NamedIndividual namedIndividual = new NamedIndividual(urn);
        namedIndividual.setLabel((String) DOMUtil.compileXPathExpr("rdfs:label", nc).evaluate(node,
                XPathConstants.STRING));
        namedIndividual.setPreferredLabel((String) DOMUtil.compileXPathExpr("skos:prefLabel", nc).evaluate(node,
                XPathConstants.STRING));
        namedIndividual.setDefinition((String) DOMUtil.compileXPathExpr("skos:definition", nc).evaluate(node,
                XPathConstants.STRING));

        return namedIndividual;
    }

    /**
     * Parses a skos:Concept element (ignoring any relations)
     *
     * If the specified node is missing requisite data null will be returned
     *
     * @param node
     *            an owl:NamedIndividual or skos:ConceptNode node
     * @return
     * @throws XPathExpressionException
     */
    protected Concept attemptParseConcept(final Node node) throws XPathExpressionException {
        final String urn = (String) DOMUtil.compileXPathExpr("@rdf:about", nc).evaluate(node, XPathConstants.STRING);
        if (urn == null || urn.isEmpty()) {
            return null;
        }

        //Build our concept/named individual
        final Concept concept = new Concept(urn);
        concept.setLabel((String) DOMUtil.compileXPathExpr("rdfs:label", nc).evaluate(node, XPathConstants.STRING));
        concept.setPreferredLabel((String) DOMUtil.compileXPathExpr("skos:prefLabel", nc).evaluate(node,
                XPathConstants.STRING));
        concept.setDefinition((String) DOMUtil.compileXPathExpr("skos:definition", nc).evaluate(node,
                XPathConstants.STRING));

        return concept;
    }

    private Concept[] relateConceptByDescription(final Description[] descs, final Map<String, Concept> parsedConceptMap,
            final List<String> traversedUrns) {
        final List<Concept> concepts = new ArrayList<>();

        for (int i = 0; i < descs.length; i++) {
            final String urn = descs[i].getUrn();
            Concept concept = parsedConceptMap.get(urn);
            if (concept == null) {
                concept = new Concept(urn, true);
            }

            if (!descs[i].isHref()) {
                relateConceptByDescription(concept, descs[i], parsedConceptMap, traversedUrns);
            }
            concepts.add(concept);
        }

        return concepts.toArray(new Concept[concepts.size()]);
    }

    /**
     * Given a concept described by desc; populate all relations in concept as defined by desc sourcing concepts from parsedConceptMap
     *
     * @param concept
     * @param desc
     * @param parsedConceptMap
     */
    protected void relateConceptByDescription(final Concept concept, final Description desc, final Map<String, Concept> parsedConceptMap,
            final List<String> traversedUrns) {

        //To deal with cycles in the hierarchy
        if (traversedUrns.contains(desc.getUrn())) {
            return;
        } else {
            traversedUrns.add(desc.getUrn());
        }

        concept.setBroader(relateConceptByDescription(desc.getBroader(), parsedConceptMap, traversedUrns));
        concept.setNarrower(relateConceptByDescription(desc.getNarrower(), parsedConceptMap, traversedUrns));
        concept.setRelated(relateConceptByDescription(desc.getRelated(), parsedConceptMap, traversedUrns));
    }

    /**
     * Parses a list of owl:NamedIndividual and skos:Concept objects from an RDF Document and then arranges them according to the heirarchy defined by
     * rdf:Description elements
     *
     * @param rdf
     *            Must be an rdf:RDF node
     * @return
     */
    public Concept[] parseFromRDF(final Node rdf) {
        //A map of concepts keyed by their URN's
        final Map<String, Concept> parsedConceptMap = new HashMap<>();

        //Parse all of our concepts and named individuals (but ignore all relations)
        try {
            //Parse the contents of all our Concepts and NamedIndividuals
            final XPathExpression getConceptsExpr = DOMUtil.compileXPathExpr("./descendant::skos:Concept", nc);
            final XPathExpression getNamedIndividualsExpr = DOMUtil.compileXPathExpr("./descendant::owl:NamedIndividual", nc);
            final NodeList namedIndividualNodes = (NodeList) getNamedIndividualsExpr.evaluate(rdf, XPathConstants.NODESET);
            final NodeList conceptNodes = (NodeList) getConceptsExpr.evaluate(rdf, XPathConstants.NODESET);

            for (int i = 0; i < conceptNodes.getLength(); i++) {
                final Concept concept = attemptParseConcept(conceptNodes.item(i));
                parsedConceptMap.put(concept.getUrn(), concept);
            }

            for (int i = 0; i < namedIndividualNodes.getLength(); i++) {
                final NamedIndividual ni = attemptParseNamedIndividual(namedIndividualNodes.item(i));
                parsedConceptMap.put(ni.getUrn(), ni);
            }
        } catch (final XPathExpressionException e) {
            log.error("Unable to evaluate inbuilt XPath - requesting concepts/individuals", e);
            throw new RuntimeException();
        }

        //After getting a map of all parsed concepts we populate the relations
        //We can do this by reading the rdf:Description elements
        final List<Concept> topLevelConcepts = new ArrayList<>();
        final DescriptionFactory df = new DescriptionFactory();
        for (final Description description : df.parseFromRDF(rdf)) {
            final Concept concept = parsedConceptMap.get(description.getUrn());
            if (concept != null) {
                relateConceptByDescription(concept, description, parsedConceptMap, new ArrayList<String>());
                topLevelConcepts.add(concept);
            }
        }

        return topLevelConcepts.toArray(new Concept[topLevelConcepts.size()]);
    }
}
