/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.marmotta.platform.ldp.services;

import info.aduna.iteration.Iteration;
import info.aduna.iteration.Iterations;
import info.aduna.iteration.UnionIteration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;
import org.apache.marmotta.commons.vocabulary.DCTERMS;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.apache.marmotta.platform.core.api.config.ConfigurationService;
import org.apache.marmotta.platform.ldp.api.LdpBinaryStoreService;
import org.apache.marmotta.platform.ldp.api.LdpService;
import org.apache.marmotta.platform.ldp.exceptions.IncompatibleResourceTypeException;
import org.apache.marmotta.platform.ldp.exceptions.InvalidInteractionModelException;
import org.apache.marmotta.platform.ldp.exceptions.InvalidModificationException;
import org.apache.marmotta.platform.ldp.patch.InvalidPatchDocumentException;
import org.apache.marmotta.platform.ldp.patch.RdfPatchUtil;
import org.apache.marmotta.platform.ldp.patch.model.PatchLine;
import org.apache.marmotta.platform.ldp.patch.parser.ParseException;
import org.apache.marmotta.platform.ldp.patch.parser.RdfPatchParserImpl;
import org.apache.marmotta.platform.ldp.util.LdpUtils;
import org.apache.marmotta.platform.ldp.webservices.LdpWebService;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.event.base.InterceptingRepositoryConnectionWrapper;
import org.openrdf.repository.event.base.RepositoryConnectionInterceptorAdapter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.Rio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDP Service SPARQL implementation
 *
 * @author Qihong Lin
 */
@Alternative
@ApplicationScoped
public class LdpServiceSPARQLImpl implements LdpService {

    private static final Logger log = LoggerFactory.getLogger(LdpServiceSPARQLImpl.class);

    @Inject
    private ConfigurationService configurationService;

    @Inject
    private LdpBinaryStoreService binaryStore;

    private final URI ldpContext, ldpInteractionModelProperty;

    public LdpServiceSPARQLImpl() {
        ldpContext = ValueFactoryImpl.getInstance().createURI(LDP.NAMESPACE);
        ldpInteractionModelProperty = ValueFactoryImpl.getInstance().createURI(LDP.NAMESPACE, "interactionModel");
    }
    
    //Done
	@Override
    public void init(RepositoryConnection connection, URI root) throws RepositoryException {
        final ValueFactory valueFactory = connection.getValueFactory();
        final Literal now = valueFactory.createLiteral(new Date());
        if (!exists(connection, root)) {
        	
            String updateString = " INSERT DATA { GRAPH <" + LDP.NAMESPACE + "> { "
            		+ " <" + root.stringValue() + "> <" + RDFS.LABEL.stringValue() + "> " + valueFactory.createLiteral("Marmotta's LDP Root Container").toString() + " . "
            		+ " <" + root.stringValue() + "> rdf:type <" + LDP.Resource.stringValue() + "> . "
            		+ " <" + root.stringValue() + "> rdf:type <" + LDP.RDFSource.stringValue() + "> . "
            		+ " <" + root.stringValue() + "> rdf:type <" + LDP.Container.stringValue() + "> . "
            		+ " <" + root.stringValue() + "> rdf:type <" + LDP.BasicContainer.stringValue() + "> . "
            		+ " <" + root.stringValue() + "> <" + DCTERMS.created.stringValue() + "> " + now.toString() + " . "
            		+ " <" + root.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . } }";
            
            update( connection, updateString);
        }
    }
	
	//Done
	@Override
    public String getResourceUri(UriInfo uriInfo) {
        final UriBuilder uriBuilder = getResourceUriBuilder(uriInfo);
        uriBuilder.path(uriInfo.getPathParameters().getFirst("local"));
        // uriBuilder.path(uriInfo.getPath().replaceFirst("/$", ""));
        String uri = uriBuilder.build().toString();
        log.debug("Request URI: {}", uri);
        return uri;
    }

	//Done
	@Override
    public UriBuilder getResourceUriBuilder(UriInfo uriInfo) {
        final UriBuilder uriBuilder;
        if (configurationService.getBooleanConfiguration("ldp.force_baseuri", false)) {
            log.trace("UriBuilder is forced to configured baseuri <{}>", configurationService.getBaseUri());
            uriBuilder = UriBuilder.fromUri(java.net.URI.create(configurationService.getBaseUri()));
        } else {
            uriBuilder = uriInfo.getBaseUriBuilder();
        }
        uriBuilder.path(LdpWebService.PATH);
        return uriBuilder;
    }

    private URI buildURI(String resource) {
    	if (resource==null){
    		return null;
    	}
        return ValueFactoryImpl.getInstance().createURI(resource);
    }

    //Done
    @Override
    public boolean exists(RepositoryConnection connection, String resource) throws RepositoryException {
    	return exists(connection, buildURI(resource));
    }

    //Done
    @Override
    public boolean exists(RepositoryConnection connection, URI resource) throws RepositoryException {    	
    	String queryString = "ASK FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ resource.stringValue()+ "> ?p ?o . }";
    	return ask(connection, queryString);
    }

    //Done
    @Override
    public boolean hasType(RepositoryConnection connection, URI resource, URI type) throws RepositoryException {
    	String queryString = "ASK FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ resource.stringValue()+ "> rdf:type " + "<" + type.stringValue() + "> . }";
    	return ask(connection, queryString);
    }

    //Done
    @Override
    public List<Statement> getLdpTypes(RepositoryConnection connection, String resource) throws RepositoryException {   	
    	return getLdpTypes(connection, buildURI(resource));
    }

    //Done
    @Override
    public List<Statement> getLdpTypes(RepositoryConnection connection, URI resource) throws RepositoryException {
    	String queryString = "CONSTRUCT   { <"+ resource.stringValue()+ "> rdf:type " + " ?type . } FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ resource.stringValue()+ "> rdf:type " + " ?type . FILTER ( isIRI(?type) &&  strStarts ( str (?type) , '" + LDP.NAMESPACE + "') )}";
    	List<Statement> list = new ArrayList<Statement>();
    	try {
			GraphQuery query= connection.prepareGraphQuery(QueryLanguage.SPARQL, queryString);
			GraphQueryResult result = query.evaluate();
			list = Iterations.asList(result);
			return list;
		} catch (MalformedQueryException e) {
			return list;
		} catch (QueryEvaluationException e){
			return list;
		}
    }

    //Done
    @Override
    public boolean isRdfSourceResource(RepositoryConnection connection, String resource) throws RepositoryException {
    	return isRdfSourceResource(connection, buildURI(resource));
    }

    //Done
    @Override
    public boolean isRdfSourceResource(RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString = "ASK FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> rdf:type " + "<" + LDP.RDFSource.stringValue() + "> . }";
    	return ask(connection, queryString);
    }

    //Done
    @Override
    public boolean isNonRdfSourceResource(RepositoryConnection connection, String resource) throws RepositoryException {
        return isNonRdfSourceResource(connection, buildURI(resource));
    }

    //Done
    @Override
    public boolean isNonRdfSourceResource(RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString = "ASK FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> rdf:type " + "<" + LDP.NonRDFSource.stringValue() + "> . }";
    	return ask(connection, queryString);
    }

    //Done
    @Override
    public URI getRdfSourceForNonRdfSource(final RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString = "SELECT ?o FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + DCTERMS.isFormatOf.stringValue() + "> ?o . ?o rdf:type <" + LDP.RDFSource.stringValue() + "> . FILTER ( isIRI(?o) )}";
    	return selectObjectAsURI(connection, queryString); 
    }

    //Done
    @Override
    public URI getRdfSourceForNonRdfSource(RepositoryConnection connection, String resource) throws RepositoryException {
        return getRdfSourceForNonRdfSource(connection, buildURI(resource));
    }

    //Done
    @Override
    public URI getNonRdfSourceForRdfSource(RepositoryConnection connection, String resource) throws RepositoryException {
        return getNonRdfSourceForRdfSource(connection, buildURI(resource));
    }

    //Done
    @Override
    public URI getNonRdfSourceForRdfSource(final RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString = "SELECT ?o FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + DCTERMS.hasFormat.stringValue() + "> ?o . ?o rdf:type <" + LDP.NonRDFSource.stringValue() + "> . FILTER ( isIRI(?o) )}";
    	return selectObjectAsURI(connection, queryString); 
    }

    //Done
    @Override
    public void exportResource(RepositoryConnection connection, String resource, OutputStream output, RDFFormat format) throws RepositoryException, RDFHandlerException {
        exportResource(connection, buildURI(resource), output, format);
    }

    //Done
    @Override
    public void exportResource(RepositoryConnection connection, URI resource, OutputStream output, RDFFormat format) throws RepositoryException, RDFHandlerException {
    	String queryString1 = "CONSTRUCT   { <"+ resource.stringValue()+ "> ?p ?o . } FROM <"+ LDP.NAMESPACE +"> WHERE  { <"+ resource.stringValue()+ "> ?p ?o . } ";
    	Iteration<Statement, RepositoryException> result1 = null;
    	
    	String queryString2 = "CONSTRUCT   { ?s ?p ?o . } FROM <"+ resource.stringValue() +"> WHERE  { ?s ?p ?o . } ";
    	Iteration<Statement, RepositoryException> result2 = null;
    	
    	try {
			GraphQuery query1= connection.prepareGraphQuery(QueryLanguage.SPARQL, queryString1);
			GraphQueryResult r1 = query1.evaluate();
			result1 = new  IterationExceptionAdpter<Statement>(r1);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (QueryEvaluationException e){
			throw new RepositoryException(e);
		}
    	
    	try {
			GraphQuery query2= connection.prepareGraphQuery(QueryLanguage.SPARQL, queryString2);
			GraphQueryResult r2 = query2.evaluate();
			result2 = new  IterationExceptionAdpter<Statement>(r2);
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (QueryEvaluationException e){
			throw new RepositoryException(e);
		}

    	// TODO: this should be a little more sophisticated...
        // TODO: non-membership triples flag / Prefer-header
        RDFWriter writer = Rio.createWriter(format, output);
    	UnionIteration<Statement, RepositoryException> union = new UnionIteration<>(result1, result2);

        try {
            LdpUtils.exportIteration(writer, resource, union);
        } finally {
            union.close();
        }
    }

    //Ignored
    @Override
    public void exportBinaryResource(RepositoryConnection connection, String resource, OutputStream out) throws RepositoryException, IOException {
        //TODO: check (resource, dct:format, type)
        try (InputStream in = binaryStore.read(resource)) {
            if (in != null) {
                IOUtils.copy(in, out);
            } else {
                throw new IOException("Cannot read resource " + resource);
            }
        }
    }

    //Ignored
    @Override
    public void exportBinaryResource(RepositoryConnection connection, URI resource, OutputStream out) throws RepositoryException, IOException {
        exportBinaryResource(connection, resource.stringValue(), out);
    }

    //Done
    @Override
    public String getMimeType(RepositoryConnection connection, String resource) throws RepositoryException {
        return getMimeType(connection, buildURI(resource));
    }

    //Done
    @Override
    public String getMimeType(RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString = "SELECT ?o FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + DCTERMS.format.stringValue() + "> ?o . }";
    	return selectObjectAsString(connection, queryString);    
    }
    
    //Done
    @Override
    public String addResource(RepositoryConnection connection, String container, String resource, String type, InputStream stream) throws RepositoryException, IOException, RDFParseException {
        return addResource(connection, buildURI(container), buildURI(resource), InteractionModel.LDPC, type, stream);
    }

    //Done
    @Override
    public String addResource(RepositoryConnection connection, URI container, URI resource, String type, InputStream stream) throws RepositoryException, IOException, RDFParseException {
        return addResource(connection, container, resource, InteractionModel.LDPC, type, stream);
    }

    //Done
    @Override
    public String addResource(RepositoryConnection connection, String container, String resource, InteractionModel interactionModel, String type, InputStream stream) throws RepositoryException, IOException, RDFParseException {
        return addResource(connection, buildURI(container), buildURI(resource), interactionModel, type, stream);
    }

    //Done
    @Override
    public String addResource(RepositoryConnection connection, URI container, URI resource, InteractionModel interactionModel, String type, InputStream stream) throws RepositoryException, IOException, RDFParseException {
        ValueFactory valueFactory = connection.getValueFactory();

        // Add container triples (Sec. 5.2.3.2)
        // container and meta triples!

        final Literal now = valueFactory.createLiteral(new Date());
        
        String updateString = "WITH <" + LDP.NAMESPACE + "> "
        		+ " DELETE { <" + container.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date }"
        		+ " INSERT { <" + container.stringValue() + "> rdf:type <" + LDP.Resource.stringValue() + "> . "        		
        		+ " <" + container.stringValue() + "> rdf:type <" + LDP.Container.stringValue() + "> . "
        		+ " <" + container.stringValue() + "> rdf:type <" + LDP.BasicContainer.stringValue() + "> . "
        		+ " <" + container.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . "
        		+ " <" + resource.stringValue() + "> rdf:type <" + LDP.Resource.stringValue() + "> . "
        		+ " <" + resource.stringValue() + "> rdf:type <" + LDP.RDFSource.stringValue() + "> . "
        		+ " <" + resource.stringValue() + "> <" + ldpInteractionModelProperty.stringValue() + "> <" + interactionModel.getUri().stringValue() + "> . "
        		+ " <" + resource.stringValue() + "> <" + DCTERMS.created.stringValue() + "> " + now.toString() + " . "
        		+ " <" + resource.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . ";
        
        // Add the bodyContent
        // TODO: find a better way to ingest n-triples (text/plain) while still supporting regular text files
        final RDFFormat rdfFormat = ("text/plain".equals(type) ? null : Rio.getParserFormatForMIMEType(type));
        if (rdfFormat == null) {
            final Literal format = valueFactory.createLiteral(type);
            final URI binaryResource = valueFactory.createURI(resource.stringValue() + LdpUtils.getExtension(type));
            updateString += " <" + container.stringValue() + "> <" + LDP.contains.stringValue() + "> <" +binaryResource.stringValue() + "> . "
            		+ " <" + binaryResource.stringValue() + "> <" + DCTERMS.created.stringValue() + "> " + now.toString() + " . "
            		+ " <" + binaryResource.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . "
            		+ " <" + binaryResource.stringValue() + "> rdf:type <" + LDP.Resource.stringValue() + "> . "
            		+ " <" + binaryResource.stringValue() + "> rdf:type <" + LDP.NonRDFSource.stringValue() + "> . "
            		+ " <" + binaryResource.stringValue() + "> <" + DCTERMS.format.stringValue() + "> " +format.toString() + " . "
            		+ " <" + binaryResource.stringValue() + "> <" + DCTERMS.isFormatOf.stringValue() + "> <" +resource.stringValue() + "> . "
            		+ " <" + resource.stringValue() + "> <" + DCTERMS.hasFormat.stringValue() + "> <" +binaryResource.stringValue() + "> . ";

        } else {
        	log.debug("POST creates new LDP-SR, data provided as {}", rdfFormat.getName());
        	updateString += " <" + container.stringValue() + "> rdf:type <" + LDP.RDFSource.stringValue() + "> . " 
        			+ " <" + container.stringValue() + "> <" + LDP.contains.stringValue() + "> <" +resource.stringValue() + "> . ";
        }
        
        updateString += " } WHERE { OPTIONAL { <" + container.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date } }";

        update(connection, updateString);
        
        if(rdfFormat==null){
        	final URI binaryResource = valueFactory.createURI(resource.stringValue() + LdpUtils.getExtension(type));
            binaryStore.store(binaryResource, stream);
            return binaryResource.stringValue();
        } else {
        	connection.add(stream, resource.stringValue(), rdfFormat, resource);
        	return resource.stringValue();
        }
        
    }

    //Done
    @Override
    public EntityTag generateETag(RepositoryConnection connection, String resource) throws RepositoryException {
        return generateETag(connection, buildURI(resource));
    }

    //Done
    @Override
    public EntityTag generateETag(RepositoryConnection connection, URI uri) throws RepositoryException {
        if (isNonRdfSourceResource(connection, uri)) {
            final String hash = binaryStore.getHash(uri.stringValue());
            if (hash != null) {
                return new EntityTag(hash, false);
            } else {
                return null;
            }
        } else {
            String queryString = "SELECT ?o FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + DCTERMS.modified.stringValue() + "> ?o . }";
            
        	try {
    			TupleQuery query= connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
    			TupleQueryResult result = query.evaluate();

                // TODO: ETag is the last-modified date (explicitly managed) thus only weak.
                Date latest = null;
                while (result.hasNext()) {
                    Value o = result.next().getValue("o");
                    if (o instanceof Literal) {
                        Date d = ((Literal)o).calendarValue().toGregorianCalendar().getTime();
                        if (latest == null || d.after(latest)) {
                            latest = d;
                        }
                    }
                }
                if (latest != null) {
                    return new EntityTag(String.valueOf(latest.getTime()), true);
                } else {
                    return null;
                }
    		} catch (MalformedQueryException e) {
    			return null;
    		} catch (QueryEvaluationException e){
    			return null;
    		}
        	
        }
    }

    //Done
    @Override
    public Date getLastModified(RepositoryConnection connection, String resource) throws RepositoryException {
        return getLastModified(connection, buildURI(resource));
    }

    //Done
    @Override
    public Date getLastModified(RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString = "SELECT ?o FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + DCTERMS.modified.stringValue() + "> ?o . }";
    	
    	try {
			TupleQuery query= connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = query.evaluate();
            Date latest = null;
            while (result.hasNext()) {
                Value o = result.next().getValue("o");
                if (o instanceof Literal) {
                    Date d = ((Literal)o).calendarValue().toGregorianCalendar().getTime();
                    if (latest == null || d.after(latest)) {
                        latest = d;
                    }
                }
            }
            return latest;
		} catch (MalformedQueryException e) {
			return null;
		} catch (QueryEvaluationException e){
			return null;
		}
    }

    //Not tested
    @Override
    public void patchResource(RepositoryConnection connection, String resource, InputStream patchData, boolean strict) throws RepositoryException, ParseException, InvalidModificationException, InvalidPatchDocumentException {
        patchResource(connection, buildURI(resource), patchData, strict);
    }

    //Not tested
    @Override
    public void patchResource(RepositoryConnection connection, URI uri, InputStream patchData, boolean strict) throws RepositoryException, ParseException, InvalidModificationException, InvalidPatchDocumentException {
        final Literal now = connection.getValueFactory().createLiteral(new Date());

        log.trace("parsing patch");
        List<PatchLine> patch = new RdfPatchParserImpl(patchData).parsePatch();

        // we are allowed to restrict the patch contents (Sec. ???)
        log.trace("checking for invalid patch statements");
        for (PatchLine patchLine : patch) {
            if (LDP.contains.equals(patchLine.getStatement().getPredicate())) {
                throw new InvalidModificationException("must not change <" + LDP.contains.stringValue() + "> via PATCH");
            }
        }

        log.debug("patching <{}> ({} changes)", uri.stringValue(), patch.size());

        RdfPatchUtil.applyPatch(connection, patch, uri);

        log.trace("update resource meta");        
        
        String updateString = "WITH <" + LDP.NAMESPACE + "> "
        		+ " DELETE { <" + uri.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date }"
        		+ " INSERT { <" + uri.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . } "
        		+ " WHERE { <" + uri.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date }";
        
        update(connection, updateString);
    }

    //Done
    @Override
    public boolean deleteResource(RepositoryConnection connection, String resource) throws RepositoryException {
        return deleteResource(connection, buildURI(resource));
    }

    //Done
    @Override
    public boolean deleteResource(RepositoryConnection connection, URI resource) throws RepositoryException {
        final Literal now = connection.getValueFactory().createLiteral(new Date());
        
        String updateString = " DELETE { GRAPH <" + LDP.NAMESPACE + "> { ?s <" + DCTERMS.modified.stringValue() + "> ?date . ?s <" + LDP.contains.stringValue() + "> <" + resource.stringValue() + "> } } "
        		+ " INSERT { GRAPH <" + LDP.NAMESPACE + "> { ?s <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " } } "
        		+ " WHERE { GRAPH <" + LDP.NAMESPACE + "> { ?s <" + LDP.contains.stringValue() + "> <" + resource.stringValue() + "> } }";
        update(connection, updateString);
        
		updateString = " DELETE { GRAPH <" + LDP.NAMESPACE + "> { ?o1 ?p ?o2 . <" + resource.stringValue() + "> <" + DCTERMS.isFormatOf.stringValue() + "> ?o1 } } "
        		+ " WHERE { GRAPH <" + LDP.NAMESPACE + "> { <" + resource.stringValue() + "> <" + DCTERMS.isFormatOf.stringValue() + "> ?o1 . OPTIONAL { ?o1 ?p ?o2 } } }";
		update(connection, updateString);

        // Delete LDP-NR (binary)
        binaryStore.delete(resource);

        // Delete the resource meta
        updateString = " DELETE WHERE { GRAPH <" + LDP.NAMESPACE + "> { <" + resource.stringValue() + "> ?p ?o } } ";
        update(connection, updateString);

		updateString = " CLEAR GRAPH <" + resource.stringValue() + "> ";
		update(connection, updateString);

		updateString = " INSERT DATA { GRAPH <" + LDP.NAMESPACE + "> { <" + resource.stringValue() + "> rdf:type <" + LDP.Resource.stringValue() + "> } } ";
		update(connection, updateString);
				
        return true;
    }

    //Ignored
    @Override
    public InteractionModel getInteractionModel(List<Link> linkHeaders) throws InvalidInteractionModelException {
        if (log.isTraceEnabled()) {
            log.trace("Checking Link-Headers for LDP Interaction Models");
            for (Link link: linkHeaders) {
                log.trace(" - {}", link);
            }
        }
        for (Link link: linkHeaders) {
            if ("type".equalsIgnoreCase(link.getRel())) {
                final String href = link.getUri().toASCIIString();
                if (LDP.Resource.stringValue().equals(href)) {
                    log.debug("LDPR Interaction Model detected");
                    return InteractionModel.LDPR;
                } else if (LDP.Container.stringValue().equals(href) || LDP.BasicContainer.stringValue().equals(href)) {
                    log.debug("LDPC Interaction Model detected");
                    return InteractionModel.LDPC;
                } else if (LDP.DirectContainer.stringValue().equals(href) || LDP.IndirectContainer.stringValue().equals(href)) {
                    log.warn("only Basic Container interaction is supported");
                } else {
                    log.debug("Invalid/Unknown LDP Interaction Model: {}", href);
                    throw new InvalidInteractionModelException(href);
                }
            }
        }
        log.debug("No LDP Interaction Model specified, defaulting to {}", InteractionModel.LDPC);
        // Default Interaction Model is LDPC
        return InteractionModel.LDPC;
    }

    
    //Done
    @Override
    public InteractionModel getInteractionModel(RepositoryConnection connection, String resource) throws RepositoryException {
        return getInteractionModel(connection, buildURI(resource));
    }
    
    //Done
    @Override
    public InteractionModel getInteractionModel(RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString1 = "ASK FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + ldpInteractionModelProperty.stringValue() + "> <" + InteractionModel.LDPC.getUri().stringValue() + "> . }";
    	boolean isLDPC = ask(connection, queryString1);
    	
    	String queryString2 = "ASK FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + ldpInteractionModelProperty.stringValue() + "> <" + InteractionModel.LDPR.getUri().stringValue() + "> . }";
    	boolean isLDPR = ask(connection, queryString2);
    	    	
    	if (isLDPC) {
            return InteractionModel.LDPC;
        } else if (isLDPR) {
            return InteractionModel.LDPR;
        }

        log.info("No LDP Interaction Model specified for <{}>, defaulting to {}", uri.stringValue(), InteractionModel.LDPC);
        // Default Interaction Model is LDPC
        return InteractionModel.LDPC;
    }

    @Override
    public String updateResource(RepositoryConnection connection, final String resource, InputStream stream, final String type) throws RepositoryException, IncompatibleResourceTypeException, RDFParseException, IOException, InvalidModificationException {
        return updateResource(connection, buildURI(resource), stream, type);
    }

    @Override
    public String updateResource(final RepositoryConnection connection, final URI resource, InputStream stream, final String type) throws RepositoryException, IncompatibleResourceTypeException, IOException, RDFParseException, InvalidModificationException {
        return updateResource(connection, resource, stream, type, false);
    }

    @Override
    public String updateResource(RepositoryConnection connection, final String resource, InputStream stream, final String type, final boolean overwrite) throws RepositoryException, IncompatibleResourceTypeException, RDFParseException, IOException, InvalidModificationException {
        return updateResource(connection, buildURI(resource), stream, type, false);
    }

	@Override
	public String updateResource(RepositoryConnection con, final URI resource, InputStream stream, final String type, final boolean overwrite)
			throws RepositoryException, IncompatibleResourceTypeException,
			IOException, RDFParseException, InvalidModificationException {
        final ValueFactory valueFactory = con.getValueFactory();
        final Literal now = valueFactory.createLiteral(new Date());

        String updateString = "WITH <" + LDP.NAMESPACE + "> "
        		+ " DELETE { <" + resource.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date }"
        		+ " INSERT { <" + resource.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . } "
        		+ " WHERE { <" + resource.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date }";
        update(con, updateString);


        final RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(type);
        // Check submitted format vs. real resource type (RDF-S vs. Non-RDF)
        if (rdfFormat == null && isNonRdfSourceResource(con, resource)) {
            log.debug("Updating <{}> as LDP-NR (binary) - {}", resource, type);

            final Literal format = valueFactory.createLiteral(type);

            updateString = "WITH <" + LDP.NAMESPACE + "> "
            		+ " DELETE { <" + resource.stringValue() + "> <" + DCTERMS.format.stringValue() + "> ?format}"
            		+ " INSERT { <" + resource.stringValue() + "> <" + DCTERMS.format.stringValue() + "> " + format.toString() + " . } "
            		+ " WHERE { <" + resource.stringValue() + "> <" + DCTERMS.format.stringValue() + "> ?format }";
            update(con, updateString);

            final URI ldp_rs = getRdfSourceForNonRdfSource(con, resource);
            if (ldp_rs != null) {
            	
                updateString = "WITH <" + LDP.NAMESPACE + "> "
                		+ " DELETE { <" + ldp_rs.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date }"
                		+ " INSERT { <" + ldp_rs.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . } "
                		+ " WHERE { <" + ldp_rs.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date }";
                update(con, updateString);
        		
                log.trace("Updated Meta-Data of LDP-RS <{}> for LDP-NR <{}>; Modified: {}", ldp_rs, resource, now);
            } else {
                log.debug("LDP-RS for LDP-NR <{}> not found", resource);
            }
            log.trace("Meta-Data for <{}> updated; Format: {}, Modified: {}", resource, format, now);

            binaryStore.store(resource, stream);//TODO: exceptions control

            log.trace("LDP-NR <{}> updated", resource);
            return resource.stringValue();
        } else if (rdfFormat != null && isRdfSourceResource(con, resource)) {
            log.debug("Updating <{}> as LDP-RS - {}", resource, rdfFormat.getDefaultMIMEType());

    		updateString = " CLEAR GRAPH <" + resource.stringValue() + "> ";
    		update(con, updateString);
    		
            final InterceptingRepositoryConnectionWrapper filtered = new InterceptingRepositoryConnectionWrapper(con.getRepository(), con);
            final Set<URI> deniedProperties = new HashSet<>();
            filtered.addRepositoryConnectionInterceptor(new RepositoryConnectionInterceptorAdapter() {
                @Override
                public boolean add(RepositoryConnection conn, Resource subject, URI predicate, Value object, Resource... contexts) {
                    if (resource.equals(subject) && SERVER_MANAGED_PROPERTIES.contains(predicate)) {
                        deniedProperties.add(predicate);
                        return true;
                    }
                    return false;
                }
            });

            filtered.add(stream, resource.stringValue(), rdfFormat, resource);

            if (!deniedProperties.isEmpty()) {
                final URI prop = deniedProperties.iterator().next();
                log.debug("Invalid property modification in update: <{}> is a server controlled property", prop);
                throw new InvalidModificationException(String.format("Must not update <%s> using PUT", prop));
            }
            log.trace("LDP-RS <{}> updated", resource);
            return resource.stringValue();
        } else if (rdfFormat == null) {
            final String mimeType = getMimeType(con, resource);
            log.debug("Incompatible replacement: Can't replace {} with {}", mimeType, type);
            throw new IncompatibleResourceTypeException(mimeType, type);
        } else {
            log.debug("Incompatible replacement: Can't replace a LDP-RS with {}", type);
            throw new IncompatibleResourceTypeException("RDF", type);
        }
	}
	
	private boolean ask(RepositoryConnection connection, String queryString) throws RepositoryException {
    	try {
			BooleanQuery query= connection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
			return query.evaluate();
		} catch (MalformedQueryException e) {
			return false;
		} catch (QueryEvaluationException e){
			return false;
		}
	} 
	
	private String selectObjectAsString(RepositoryConnection connection, String queryString) throws RepositoryException {
    	try {
			TupleQuery query= connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = query.evaluate();
			if (! result.hasNext()){
				return null;
			}else {
				return result.next().getValue("o").stringValue();
			}
		} catch (MalformedQueryException e) {
			return null;
		} catch (QueryEvaluationException e){
			return null;
		}
	}
	
	private URI selectObjectAsURI (RepositoryConnection connection, String queryString) throws RepositoryException {
		return buildURI( selectObjectAsString(connection, queryString) );
	}
	
	private void update(RepositoryConnection connection, String updateString) throws RepositoryException {
        log.debug(updateString);
		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, updateString);
			update.execute();
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}
	}


}

class IterationExceptionAdpter<T> implements Iteration<T,RepositoryException>{

	private final Iteration<T, QueryEvaluationException> it;
	
	IterationExceptionAdpter(Iteration<T, QueryEvaluationException> it){
		this.it = it;
	}
	
	@Override
	public boolean hasNext() throws RepositoryException {
		try {
			return it.hasNext();
		} catch(QueryEvaluationException e){
			throw new RepositoryException(e);
		}
	}

	@Override
	public T next() throws RepositoryException {
		try {
			return it.next();
		} catch(QueryEvaluationException e){
			throw new RepositoryException(e);
		}
	}

	@Override
	public void remove() throws RepositoryException {
		try {
			it.remove();
		} catch(QueryEvaluationException e){
			throw new RepositoryException(e);
		}
	}
	
}


