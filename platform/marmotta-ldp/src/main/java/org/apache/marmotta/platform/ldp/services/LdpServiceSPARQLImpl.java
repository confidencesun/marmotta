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

import info.aduna.iteration.Iterations;
import info.aduna.iteration.UnionIteration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;

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
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
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
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParserRegistry;
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

    private URI buildURI(String resource) {
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
    	try {
			BooleanQuery query= connection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
			return query.evaluate();
		} catch (MalformedQueryException e) {
			return false;
		} catch (QueryEvaluationException e){
			return false;
		}
    }

    //Done
    @Override
    public boolean hasType(RepositoryConnection connection, URI resource, URI type) throws RepositoryException {
    	String queryString = "ASK FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ resource.stringValue()+ "> rdf:type " + "<" + type.stringValue() + "> . }";
    	try {
			BooleanQuery query= connection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
			return query.evaluate();
		} catch (MalformedQueryException e) {
			return false;
		} catch (QueryEvaluationException e){
			return false;
		}
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
    	try {
			BooleanQuery query= connection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
			return query.evaluate();
		} catch (MalformedQueryException e) {
			return false;
		} catch (QueryEvaluationException e){
			return false;
		}
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
    	try {
			BooleanQuery query= connection.prepareBooleanQuery(QueryLanguage.SPARQL, queryString);
			return query.evaluate();
		} catch (MalformedQueryException e) {
			return false;
		} catch (QueryEvaluationException e){
			return false;
		}
    }

    //Done
    @Override
    public URI getRdfSourceForNonRdfSource(final RepositoryConnection connection, URI uri) throws RepositoryException {
    	String queryString = "SELECT ?o FROM <"+ LDP.NAMESPACE +"> WHERE { <"+ uri.stringValue()+ "> <" + DCTERMS.isFormatOf.stringValue() + "> ?o . ?o rdf:type <" + LDP.RDFSource.stringValue() + "> . FILTER ( isIRI(?o) )}";
    	try {
			TupleQuery query= connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = query.evaluate();
			if (! result.hasNext()){
				return null;
			}else {
				return buildURI(result.next().getValue("o").stringValue());
			}
		} catch (MalformedQueryException e) {
			return null;
		} catch (QueryEvaluationException e){
			return null;
		}
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
    	try {
			TupleQuery query= connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
			TupleQueryResult result = query.evaluate();
			if (! result.hasNext()){
				return null;
			}else {
				return buildURI(result.next().getValue("o").stringValue());
			}
		} catch (MalformedQueryException e) {
			return null;
		} catch (QueryEvaluationException e){
			return null;
		}
    }

    @Override
    public void exportResource(RepositoryConnection connection, String resource, OutputStream output, RDFFormat format) throws RepositoryException, RDFHandlerException {
        exportResource(connection, buildURI(resource), output, format);
    }

    @Override
    public void exportResource(RepositoryConnection connection, URI resource, OutputStream output, RDFFormat format) throws RepositoryException, RDFHandlerException {
        // TODO: this should be a little more sophisticated...
        // TODO: non-membership triples flag / Prefer-header
        RDFWriter writer = Rio.createWriter(format, output);
        UnionIteration<Statement, RepositoryException> union = new UnionIteration<>(
                connection.getStatements(resource, null, null, false, ldpContext),
                connection.getStatements(null, null, null, false, resource)
        );
        try {
            LdpUtils.exportIteration(writer, resource, union);
        } finally {
            union.close();
        }
    }

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

    @Override
    public void exportBinaryResource(RepositoryConnection connection, URI resource, OutputStream out) throws RepositoryException, IOException {
        exportBinaryResource(connection, resource.stringValue(), out);
    }

    @Override
    public String getMimeType(RepositoryConnection connection, String resource) throws RepositoryException {
        return getMimeType(connection, buildURI(resource));
    }

    @Override
    public String getMimeType(RepositoryConnection connection, URI uri) throws RepositoryException {
        final RepositoryResult<Statement> formats = connection.getStatements(uri, DCTERMS.format, null, false, ldpContext);
        try {
            if (formats.hasNext()) return formats.next().getObject().stringValue();
        } finally {
            formats.close();
        }
        return null;
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
        		+ " <" + container.stringValue() + "> rdf:type <" + LDP.RDFSource.stringValue() + "> . "
        		+ " <" + container.stringValue() + "> rdf:type <" + LDP.Container.stringValue() + "> . "
        		+ " <" + container.stringValue() + "> rdf:type <" + LDP.BasicContainer.stringValue() + "> . "
        		+ " <" + container.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . "
        		+ " <" + resource.stringValue() + "> rdf:type <" + LDP.Resource.stringValue() + "> . "
        		+ " <" + resource.stringValue() + "> rdf:type <" + LDP.RDFSource.stringValue() + "> . "
        		+ " <" + resource.stringValue() + "> <" + ldpInteractionModelProperty.stringValue() + "> <" + interactionModel.getUri().stringValue() + "> . "
        		+ " <" + resource.stringValue() + "> <" + DCTERMS.created.stringValue() + "> " + now.toString() + " . "
        		+ " <" + resource.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> " + now.toString() + " . ";
        
        // Add the bodyContent
        final RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(type);
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
        	updateString += " <" + container.stringValue() + "> <" + LDP.contains.stringValue() + "> <" +resource.stringValue() + "> . ";
        }
        
        updateString += " } WHERE { OPTIONAL { <" + container.stringValue() + "> <" + DCTERMS.modified.stringValue() + "> ?date } }";
        log.debug(updateString);
		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, updateString);
			update.execute();
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}
        
        if(rdfFormat==null){
        	final URI binaryResource = valueFactory.createURI(resource.stringValue() + LdpUtils.getExtension(type));
            binaryStore.store(binaryResource, stream);
            return binaryResource.stringValue();
        } else {
        	connection.add(stream, resource.stringValue(), rdfFormat, resource);
        	return resource.stringValue();
        }
        
    }

    @Override
    public EntityTag generateETag(RepositoryConnection connection, String resource) throws RepositoryException {
        return generateETag(connection, buildURI(resource));
    }

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
            final RepositoryResult<Statement> stmts = connection.getStatements(uri, DCTERMS.modified, null, true, ldpContext);
            try {
                // TODO: ETag is the last-modified date (explicitly managed) thus only weak.
                Date latest = null;
                while (stmts.hasNext()) {
                    Value o = stmts.next().getObject();
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
            } finally {
                stmts.close();
            }
        }
    }

    @Override
    public Date getLastModified(RepositoryConnection connection, String resource) throws RepositoryException {
        return getLastModified(connection, buildURI(resource));
    }

    @Override
    public Date getLastModified(RepositoryConnection connection, URI uri) throws RepositoryException {
        final RepositoryResult<Statement> stmts = connection.getStatements(uri, DCTERMS.modified, null, true, ldpContext);
        try {
            Date latest = null;
            while (stmts.hasNext()) {
                Value o = stmts.next().getObject();
                if (o instanceof Literal) {
                    Date d = ((Literal)o).calendarValue().toGregorianCalendar().getTime();
                    if (latest == null || d.after(latest)) {
                        latest = d;
                    }
                }
            }
            return latest;
        } finally {
            stmts.close();
        }
    }

    @Override
    public void patchResource(RepositoryConnection connection, String resource, InputStream patchData, boolean strict) throws RepositoryException, ParseException, InvalidModificationException, InvalidPatchDocumentException {
        patchResource(connection, buildURI(resource), patchData, strict);
    }

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
        connection.remove(uri, DCTERMS.modified, null, ldpContext);
        connection.add(uri, DCTERMS.modified, now, ldpContext);

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
        
		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, updateString);
			update.execute(); 
			connection.commit();
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}
        

		updateString = " DELETE { GRAPH <" + LDP.NAMESPACE + "> { ?o1 ?p ?o2 . <" + resource.stringValue() + "> <" + DCTERMS.isFormatOf.stringValue() + "> ?o1 } } "
        		+ " WHERE { GRAPH <" + LDP.NAMESPACE + "> { <" + resource.stringValue() + "> <" + DCTERMS.isFormatOf.stringValue() + "> ?o1 . OPTIONAL { ?o1 ?p ?o2 } } }";
		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, updateString);
			update.execute(); 
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}

        // Delete LDP-NR (binary)
        binaryStore.delete(resource);

        // Delete the resource meta
        updateString = " DELETE WHERE { GRAPH <" + LDP.NAMESPACE + "> { <" + resource.stringValue() + "> ?p ?o } } ";
        
		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, updateString);
			update.execute(); 
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}

		updateString = " CLEAR GRAPH <" + resource.stringValue() + "> ";

		try {
			Update update = connection.prepareUpdate(QueryLanguage.SPARQL, updateString);
			update.execute(); 
		} catch (MalformedQueryException e) {
			throw new RepositoryException(e);
		} catch (UpdateExecutionException e) {
			throw new RepositoryException(e);
		}

        return true;
    }

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
                } else if (LDP.Resource.stringValue().equals(href)) {
                    log.debug("LDPC Interaction Model detected");
                    return InteractionModel.LDPC;
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

    @Override
    public InteractionModel getInteractionModel(RepositoryConnection connection, String resource) throws RepositoryException {
        return getInteractionModel(connection, buildURI(resource));
    }

    @Override
    public InteractionModel getInteractionModel(RepositoryConnection connection, URI uri) throws RepositoryException {
        if (connection.hasStatement(uri, ldpInteractionModelProperty, InteractionModel.LDPC.getUri(), true, ldpContext)) {
            return InteractionModel.LDPC;
        } else if (connection.hasStatement(uri, ldpInteractionModelProperty, InteractionModel.LDPR.getUri(), true, ldpContext)) {
            return InteractionModel.LDPR;
        }

        log.info("No LDP Interaction Model specified for <{}>, defaulting to {}", uri.stringValue(), InteractionModel.LDPC);
        // Default Interaction Model is LDPC
        return InteractionModel.LDPC;
    }

	@Override
	public String updateResource(RepositoryConnection con, String resource,
			InputStream stream, String type) throws RepositoryException,
			IncompatibleResourceTypeException, RDFParseException, IOException,
			InvalidModificationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String updateResource(RepositoryConnection con, URI resource,
			InputStream stream, String type) throws RepositoryException,
			IncompatibleResourceTypeException, IOException, RDFParseException,
			InvalidModificationException {
		// TODO Auto-generated method stub
		return null;
	}
	
}
