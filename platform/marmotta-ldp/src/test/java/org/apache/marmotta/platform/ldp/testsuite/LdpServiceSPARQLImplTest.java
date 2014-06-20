/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.marmotta.platform.ldp.testsuite;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.marmotta.commons.vocabulary.DCTERMS;
import org.apache.marmotta.commons.vocabulary.FOAF;
import org.apache.marmotta.commons.vocabulary.LDP;
import org.apache.marmotta.platform.core.test.base.EmbeddedMarmotta;
import org.apache.marmotta.platform.ldp.api.LdpService;
import org.apache.marmotta.platform.ldp.api.LdpService.InteractionModel;
import org.apache.marmotta.platform.ldp.util.LdpUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LDP SPARQL Implementation Test Cases
 *
 * @author Qihong Lin
 */
public class LdpServiceSPARQLImplTest {
	
	public final static String BASE = "http://www.w3.org/TR/ldp-test-cases/";
	public final static String ROOT_PATH = "/testsuite/";
	public final static String FOAF_EXAMPLE = "foaf-example";
	public final static String FRIEND_EXAMPLE = "EgonWillighagen";
	public final static String IMAGE_EXAMPLE = "Marmotta_Head";
	
    private static EmbeddedMarmotta marmotta;
    private static LdpService lpdService;
    private static Repository repo;
    
    private static Logger log = LoggerFactory.getLogger(LdpServiceSPARQLImplTest.class);
    
    private static URI ldpContext =ValueFactoryImpl.getInstance().createURI(LDP.NAMESPACE);
    private static URI ldpInteractionModelProperty = ValueFactoryImpl.getInstance().createURI(LDP.NAMESPACE, "interactionModel");

    @BeforeClass
    public static final void before() throws RepositoryException, RDFParseException, IOException {
    	 marmotta = new EmbeddedMarmotta();
    	 lpdService = marmotta.getService(LdpService.class);
    	 String path = ROOT_PATH + FOAF_EXAMPLE + ".rdf";
         repo = loadData(path, RDFFormat.RDFXML);
         Assume.assumeNotNull(repo);
    }

    @AfterClass
    public static final void after() throws RepositoryException {
    	 if (marmotta !=null){
    		 marmotta.shutdown();
    	 }
    	 if (repo != null) {
             repo.shutDown();
             repo = null;
         }
         lpdService = null;
    }

    @Test
    public void testReopsitoryNotEmpty() throws RepositoryException {
        RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            Assert.assertFalse(conn.isEmpty());
            RepositoryResult<Statement> statements = conn.getStatements(null, RDF.TYPE, conn.getValueFactory().createURI("http://xmlns.com/foaf/0.1/", "Person"), false);
            Assert.assertTrue(statements.hasNext());
            Assert.assertEquals(LDP.NAMESPACE, statements.next().getContext().stringValue());
            statements.close();
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testExists() throws RepositoryException {
    	RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            boolean me = lpdService.exists(conn, "http://www.example.com/ldp#me");
            boolean unknown = lpdService.exists(conn, "http://www.example.com/ldp#unknown");
            Assert.assertTrue(me);
            Assert.assertFalse(unknown);
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testHasType() throws RepositoryException {
    	RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            ValueFactory valueFactory = conn.getValueFactory();
            boolean has1 = lpdService.hasType(conn, valueFactory.createURI("http://www.example.com/ldp#me"), FOAF.Person);
            boolean has2 = lpdService.hasType(conn, valueFactory.createURI("http://www.example.com/ldp#me"), valueFactory.createURI("http://www.w3.org/ns/ldp#TestMeClass"));
            boolean not = lpdService.hasType(conn, valueFactory.createURI("http://www.example.com/ldp#me"), valueFactory.createURI("http://www.example.com/ldp#unknown"));
            Assert.assertTrue(has1);
            Assert.assertTrue(has2);
            Assert.assertFalse(not);
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testGetLdpTypes() throws RepositoryException {
    	RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            List<Statement> list = lpdService.getLdpTypes(conn,"http://www.example.com/ldp#me");
            Assert.assertTrue(list.size()>0);
            list = lpdService.getLdpTypes(conn,"http://www.example.com/ldp#unknown");
            Assert.assertEquals(0, list.size());
            list = lpdService.getLdpTypes(conn,"http://dx.doi.org/10.1021/ol703129z");
            Assert.assertEquals(0, list.size());
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testIsRdfSourceResource() throws RepositoryException {
    	RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            boolean is = lpdService.isRdfSourceResource(conn, "http://www.example.com/ldp#me");
            boolean not1 = lpdService.isRdfSourceResource(conn, "http://www.example.com/ldp#unknown");
            boolean not2 = lpdService.isRdfSourceResource(conn, "http://dx.doi.org/10.1021/ol703129");
            Assert.assertTrue(is);
            Assert.assertFalse(not1);
            Assert.assertFalse(not2);
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testIsNonRdfSourceResource() throws RepositoryException {
    	RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            boolean is = lpdService.isNonRdfSourceResource(conn, "http://www.ch.ic.ac.uk/rzepa/rzepa_2005.jpg");
            boolean not1 = lpdService.isNonRdfSourceResource(conn, "http://www.example.com/ldp#unknown");
            boolean not2 = lpdService.isNonRdfSourceResource(conn, "http://dx.doi.org/10.1021/ol703129");
            Assert.assertTrue(is);
            Assert.assertFalse(not1);
            Assert.assertFalse(not2);
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testGetRdfSourceForNonRdfSource() throws RepositoryException {
    	RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            URI myImg = lpdService.getRdfSourceForNonRdfSource(conn, "http://www.ch.ic.ac.uk/rzepa/rzepa_2005.jpg");
            URI none1 = lpdService.getRdfSourceForNonRdfSource(conn, "http://www.example.com/ldp#unknown");
            URI none2 = lpdService.getRdfSourceForNonRdfSource(conn, "http://dx.doi.org/10.1021/ol703129");
            Assert.assertNotNull(myImg);
            Assert.assertEquals("http://www.example.com/ldp#myImage", myImg.stringValue());
            Assert.assertNull(none1);
            Assert.assertNull(none2);
        } finally {
            conn.commit();
            conn.close();
        }
    }

    @Test
    public void testGetNonRdfSourceForRdfSource() throws RepositoryException {
    	RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            URI myImg = lpdService.getNonRdfSourceForRdfSource(conn, "http://www.example.com/ldp#myImage");
            URI none1 = lpdService.getNonRdfSourceForRdfSource(conn, "http://www.example.com/ldp#unknown");
            URI none2 = lpdService.getNonRdfSourceForRdfSource(conn, "http://dx.doi.org/10.1021/ol703129");
            Assert.assertNotNull(myImg);
            Assert.assertEquals("http://www.ch.ic.ac.uk/rzepa/rzepa_2005.jpg", myImg.stringValue());
            Assert.assertNull(none1);
            Assert.assertNull(none2);
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testAddResource1() throws RepositoryException, RDFParseException, IOException{
    	RepositoryConnection conn = repo.getConnection();
    	InputStream is=null;
    	String container = "http://www.example.com/ldp#friendsContainer";
    	String resource = "http://www.example.com/ldp#EgonWillighagen";
    	String type = "application/rdf+xml";
    	try {
            conn.begin();
            String imagePath = ROOT_PATH + FRIEND_EXAMPLE + ".rdf";
            is = LdpServiceSPARQLImplTest.class.getResourceAsStream(imagePath);
            
            //Assert.assertEquals(802, is.available());
            
            String uri = lpdService.addResource(conn, container, resource , InteractionModel.LDPR, type, is);
            Assert.assertEquals(resource, uri);
    	} finally {
            conn.commit();
            conn.close();
            if (is != null){
            	is.close();
            }
        }
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Container, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.BasicContainer, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), DCTERMS.modified, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), ldpInteractionModelProperty, InteractionModel.LDPR.getUri(), true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.created, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.modified, null, true, ldpContext) );
            
            Assert.assertTrue ( conn.hasStatement(buildURI (container), LDP.contains, buildURI (resource), true, ldpContext) );
            
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), RDF.TYPE, FOAF.Person, true, buildURI (resource)) ) ;
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), FOAF.name, ValueFactoryImpl.getInstance().createLiteral("Egon Willighagen"), true, buildURI (resource)) );
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), RDFS.SEEALSO, buildURI("http://blueobelisk.sourceforge.net/people/egonw/foaf.xml"), true, buildURI (resource)) );
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), FOAF.knows, buildURI("http://www.example.com/ldp#me"), true, buildURI (resource)) );
            

    	} finally {
            conn.commit();
            conn.close();
    	}
    }
    
    @Test
    public void testAddResource2() throws RepositoryException, RDFParseException, IOException{
    	RepositoryConnection conn = repo.getConnection();
    	InputStream is=null;
    	String container = "http://www.example.com/ldp#friendsContainer";
    	String resource = "http://www.example.com/ldp#myImage";
    	String type = "image/png";
        final Literal format = ValueFactoryImpl.getInstance().createLiteral(type);
        final URI binaryResource = ValueFactoryImpl.getInstance().createURI(resource + LdpUtils.getExtension(type));
        
    	try {
            conn.begin();
            String imagePath = ROOT_PATH + IMAGE_EXAMPLE + ".png";
            is = LdpServiceSPARQLImplTest.class.getResourceAsStream(imagePath);
            
            //Assert.assertEquals(802, is.available());
            
            String uri = lpdService.addResource(conn, container, resource , InteractionModel.LDPR, type , is);
            Assert.assertEquals(binaryResource.stringValue(), uri);
    	} finally {
            conn.commit();
            conn.close();
            if (is != null){
            	is.close();
            }
        }
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Container, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.BasicContainer, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), DCTERMS.modified, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), ldpInteractionModelProperty, InteractionModel.LDPR.getUri(), true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.created, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.modified, null, true, ldpContext) );

            Assert.assertTrue ( conn.hasStatement(buildURI (container), LDP.contains, binaryResource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.created, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.modified, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, RDF.TYPE, LDP.NonRDFSource, true, ldpContext) );

            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.format, format, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.isFormatOf, buildURI (resource), true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.hasFormat, binaryResource, true, ldpContext) );

    	} finally {
            conn.commit();
            conn.close();
    	}
    }
    
    @Test
    public void testDeleteResource1() throws RepositoryException, RDFParseException, IOException{
    	RepositoryConnection conn = repo.getConnection();
    	InputStream is=null;
    	String container = "http://www.example.com/ldp#friendsContainer";
    	String resource = "http://www.example.com/ldp#EgonWillighagen";
    	String type = "application/rdf+xml";
    	try {
            conn.begin();
            String imagePath = ROOT_PATH + FRIEND_EXAMPLE + ".rdf";
            is = LdpServiceSPARQLImplTest.class.getResourceAsStream(imagePath);
            
            //Assert.assertEquals(802, is.available());
            
            String uri = lpdService.addResource(conn, container, resource , InteractionModel.LDPR, type, is);
            Assert.assertEquals(resource, uri);
    	} finally {
            conn.commit();
            conn.close();
            if (is != null){
            	is.close();
            }
        }
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Container, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.BasicContainer, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), DCTERMS.modified, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), ldpInteractionModelProperty, InteractionModel.LDPR.getUri(), true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.created, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.modified, null, true, ldpContext) );
            
            Assert.assertTrue ( conn.hasStatement(buildURI (container), LDP.contains, buildURI (resource), true, ldpContext) );
            
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), RDF.TYPE, FOAF.Person, true, buildURI (resource)) ) ;
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), FOAF.name, ValueFactoryImpl.getInstance().createLiteral("Egon Willighagen"), true, buildURI (resource)) );
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), RDFS.SEEALSO, buildURI("http://blueobelisk.sourceforge.net/people/egonw/foaf.xml"), true, buildURI (resource)) );
            Assert.assertTrue ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), FOAF.knows, buildURI("http://www.example.com/ldp#me"), true, buildURI (resource)) );

    	} finally {
            conn.commit();
            conn.close();
    	}
    	
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue(lpdService.deleteResource(conn, resource));
    	}
    	finally {
            conn.commit();
            conn.close();
        }
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Container, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.BasicContainer, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), DCTERMS.modified, null, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), ldpInteractionModelProperty, InteractionModel.LDPR.getUri(), true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), DCTERMS.created, null, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), DCTERMS.modified, null, true, ldpContext) );
            
            Assert.assertFalse ( conn.hasStatement(buildURI (container), LDP.contains, buildURI (resource), true, ldpContext) );
            
            Assert.assertFalse ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), RDF.TYPE, FOAF.Person, true, buildURI (resource)) ) ;
            Assert.assertFalse ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), FOAF.name, ValueFactoryImpl.getInstance().createLiteral("Egon Willighagen"), true, buildURI (resource)) );
            Assert.assertFalse ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), RDFS.SEEALSO, buildURI("http://blueobelisk.sourceforge.net/people/egonw/foaf.xml"), true, buildURI (resource)) );
            Assert.assertFalse ( conn.hasStatement(buildURI ("http://www.example.com/ldp#EgonWillighagen"), FOAF.knows, buildURI("http://www.example.com/ldp#me"), true, buildURI (resource)) );
    	}
    	finally {
            conn.commit();
            conn.close();
        }
    	
    }
    
    @Test
    public void testDeleteResource2() throws RepositoryException, RDFParseException, IOException{
    	RepositoryConnection conn = repo.getConnection();
    	InputStream is=null;
    	String container = "http://www.example.com/ldp#friendsContainer";
    	String resource = "http://www.example.com/ldp#myImage";
    	String type = "image/png";
        final Literal format = ValueFactoryImpl.getInstance().createLiteral(type);
        final URI binaryResource = ValueFactoryImpl.getInstance().createURI(resource + LdpUtils.getExtension(type));
        
    	try {
            conn.begin();
            String imagePath = ROOT_PATH + IMAGE_EXAMPLE + ".png";
            is = LdpServiceSPARQLImplTest.class.getResourceAsStream(imagePath);
            
            //Assert.assertEquals(802, is.available());
            
            String uri = lpdService.addResource(conn, container, resource , InteractionModel.LDPR, type , is);
            Assert.assertEquals(binaryResource.stringValue(), uri);
    	} finally {
            conn.commit();
            conn.close();
            if (is != null){
            	is.close();
            }
        }
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Container, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.BasicContainer, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), DCTERMS.modified, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), ldpInteractionModelProperty, InteractionModel.LDPR.getUri(), true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.created, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.modified, null, true, ldpContext) );

            Assert.assertTrue ( conn.hasStatement(buildURI (container), LDP.contains, binaryResource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.created, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.modified, null, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, RDF.TYPE, LDP.NonRDFSource, true, ldpContext) );

            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.format, format, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(binaryResource, DCTERMS.isFormatOf, buildURI (resource), true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (resource), DCTERMS.hasFormat, binaryResource, true, ldpContext) );

    	} finally {
            conn.commit();
            conn.close();
    	}
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue(lpdService.deleteResource(conn, binaryResource));
    	}
    	finally {
            conn.commit();
            conn.close();
        }
    	
    	conn = repo.getConnection();
    	try {
            conn.begin();
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.Container, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), RDF.TYPE, LDP.BasicContainer, true, ldpContext) );
            Assert.assertTrue ( conn.hasStatement(buildURI (container), DCTERMS.modified, null, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), RDF.TYPE, LDP.RDFSource, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), ldpInteractionModelProperty, InteractionModel.LDPR.getUri(), true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), DCTERMS.created, null, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), DCTERMS.modified, null, true, ldpContext) );

            Assert.assertFalse ( conn.hasStatement(buildURI (container), LDP.contains, binaryResource, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(binaryResource, DCTERMS.created, null, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(binaryResource, DCTERMS.modified, null, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(binaryResource, RDF.TYPE, LDP.Resource, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(binaryResource, RDF.TYPE, LDP.NonRDFSource, true, ldpContext) );

            Assert.assertFalse ( conn.hasStatement(binaryResource, DCTERMS.format, format, true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(binaryResource, DCTERMS.isFormatOf, buildURI (resource), true, ldpContext) );
            Assert.assertFalse ( conn.hasStatement(buildURI (resource), DCTERMS.hasFormat, binaryResource, true, ldpContext) );
    	}
    	finally {
            conn.commit();
            conn.close();
        }
    	
    }
    
    
    /**
     * Load test cases' data
     *
     * @param path path to the manifest file
     * @param format serialization format used in the file
     * @return In-Memory repository with the data
     * @throws org.openrdf.rio.RDFParseException
     * @throws org.openrdf.repository.RepositoryException
     * @throws java.io.IOException
     */
    private static Repository loadData(String path, RDFFormat format) throws RDFParseException, RepositoryException, IOException {
        Repository repo = new SailRepository(new MemoryStore());
        repo.initialize();
        RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            conn.clear();
            InputStream is = LdpServiceSPARQLImplTest.class.getResourceAsStream(path);
            if (is == null) {
                throw new IOException("File not found at: " + path);
            } else {
                try {
                    conn.add(is, BASE, format, ValueFactoryImpl.getInstance().createURI(LDP.NAMESPACE));
                } finally {
                    is.close();
                }
            }
            conn.commit();
        } finally {
            conn.close();
        }
        return repo;
    }
    
    private URI buildURI(String resource) {
        return ValueFactoryImpl.getInstance().createURI(resource);
    }
    
}
