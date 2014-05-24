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

import org.apache.marmotta.platform.ldp.services.LdpServiceSPARQLImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;

/**
 * LDP Test Cases Manifest test
 *
 * @author Sergio Fernández
 */
public class LdpServiceSPARQLImplTest {

    protected Repository repo;

    @Before
    public final void before() throws RepositoryException, RDFParseException, IOException {
        String path = LdpTestCases.ROOT_PATH + LdpTestCases.FOAF_EXAMPE + ".rdf";
        repo = LdpTestCasesUtils.loadData(path, RDFFormat.RDFXML);
        Assume.assumeNotNull(repo);
    }

    @After
    public final void after() throws RepositoryException {
        if (repo != null) {
            repo.shutDown();
            repo = null;
        }
    }

    @Test
    public void testNotEmpty() throws RepositoryException, RDFParseException, IOException {
        RepositoryConnection conn = repo.getConnection();
        try {
            conn.begin();
            //ValueFactory vf = conn.getValueFactory();
            Assert.assertFalse(conn.isEmpty());
            RepositoryResult<Statement> statements = conn.getStatements(null, RDF.TYPE, conn.getValueFactory().createURI("http://xmlns.com/foaf/0.1/", "Person"), false);
            Assert.assertTrue(statements.hasNext());
            statements.close();
            //TODO: check test cases are actually there
        } finally {
            conn.commit();
            conn.close();
        }
    }
    
    @Test
    public void testResourceExist() throws RepositoryException, RDFParseException, IOException {
        RepositoryConnection conn = repo.getConnection();
        LdpServiceSPARQLImpl service= new LdpServiceSPARQLImpl();
        try {
            conn.begin();
            boolean me = service.exists(conn, "http://www.example.com/ldp#me");
            boolean unknown = service.exists(conn, "http://www.example.com/ldp#unknown");
            Assert.assertTrue(me);
            Assert.assertFalse(unknown);
            //TODO: check test cases are actually there
        } finally {
            conn.commit();
            conn.close();
        }
    }

}
