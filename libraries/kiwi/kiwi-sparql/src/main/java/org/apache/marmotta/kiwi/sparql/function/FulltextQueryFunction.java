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
package org.apache.marmotta.kiwi.sparql.function;

import org.apache.marmotta.kiwi.vocabulary.FN_MARMOTTA;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.evaluation.ValueExprEvaluationException;
import org.openrdf.query.algebra.evaluation.function.Function;
import org.openrdf.query.algebra.evaluation.function.FunctionRegistry;

/**
 * A SPARQL function for doing a full-text search on the content of a string using a query language with boolean operators.
 * The query syntax is the syntax of PostgreSQL (see http://www.postgresql.org/docs/9.1/static/datatype-textsearch.html)
 * Should be implemented directly in the database, as the in-memory implementation is non-functional.
 * <p/>
 * The function can be called either as:
 * <ul>
 *     <li>fn:fulltext-query(?var, 'query') - using a generic stemmer and dictionary</li>
 *     <li>
 *         fn:fulltext-query(?var, 'query', 'language') - using a language-specific stemmer and dictionary
 *         (currently only supported by PostgreSQL with the language values 'english', 'german', 'french', 'italian', 'spanish'
 *         and some other languages as supported by PostgreSQL).
 *     </li>*
 * </ul>
 * Note that for performance reasons it might be preferrable to create a full-text index for your database. Please
 * consult your database documentation on how to do this.
 *
 * @author Sebastian Schaffert (sschaffert@apache.org)
 */
public class FulltextQueryFunction implements Function {

    // auto-register for SPARQL environment
    static {
        if(!FunctionRegistry.getInstance().has(FN_MARMOTTA.QUERY_FULLTEXT.toString())) {
            FunctionRegistry.getInstance().add(new FulltextQueryFunction());
        }
    }

    @Override
    public Value evaluate(ValueFactory valueFactory, Value... args) throws ValueExprEvaluationException {
        throw new UnsupportedOperationException("cannot evaluate in-memory, needs to be supported by the database");
    }

    @Override
    public String getURI() {
        return FN_MARMOTTA.QUERY_FULLTEXT.toString();
    }
}
