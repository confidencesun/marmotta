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

import org.apache.marmotta.commons.vocabulary.LDP;
import org.apache.marmotta.platform.core.test.base.EmbeddedMarmotta;
import org.apache.marmotta.platform.ldp.api.LdpService;
import org.apache.marmotta.platform.ldp.services.LdpServiceSPARQLImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

/**
 * LDP SPARQL Implementation Test Cases
 *
 * @author Qihong Lin
 */
public class LdpServiceSPARQLImplTest {
	
	public final static String BASE = "http://www.w3.org/TR/ldp-test-cases/";
	public final static String ROOT_PATH = "/testsuite/";
	public final static String FOAF_EXAMPE = "foaf-example";
	
    private static EmbeddedMarmotta marmotta;
    private static LdpService lpdService;
    private static Repository repo;

    @Before
    public final void before() throws RepositoryException, RDFParseException, IOException {
    	 //marmotta = new EmbeddedMarmotta();
    	 //lpdService = marmotta.getService(LdpService.class);
    	 lpdService = new LdpServiceSPARQLImpl();
    	 String path = ROOT_PATH + FOAF_EXAMPE + ".rdf";
         repo = loadData(path, RDFFormat.RDFXML);
         Assume.assumeNotNull(repo);
    }

    @After
    public final void after() throws RepositoryException {
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
    public void testReopsitoryNotEmpty() throws RepositoryException, RDFParseException, IOException {
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
    public void testExists() throws RepositoryException, RDFParseException, IOException {
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
    private Repository loadData(String path, RDFFormat format) throws RDFParseException, RepositoryException, IOException {
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
    
}
