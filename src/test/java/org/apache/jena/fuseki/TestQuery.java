/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
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

package org.apache.jena.fuseki;

import org.apache.jena.fuseki.DatasetAccessor ;
import org.apache.jena.fuseki.DatasetAccessorFactory ;
import org.junit.AfterClass ;
import org.junit.BeforeClass ;
import org.junit.Test ;

import com.hp.hpl.jena.query.QueryExecution ;
import com.hp.hpl.jena.query.QueryExecutionFactory ;
import com.hp.hpl.jena.query.ResultSet ;
import com.hp.hpl.jena.query.ResultSetFormatter ;
import com.hp.hpl.jena.sparql.resultset.ResultSetCompare ;
import com.hp.hpl.jena.sparql.sse.Item ;
import com.hp.hpl.jena.sparql.sse.SSE ;
import com.hp.hpl.jena.sparql.sse.builders.BuilderResultSet ;

public class TestQuery extends BaseServerTest 
{
    protected static ResultSet rs1 = null ; 
    static {
        Item item = SSE.parseItem("(resultset (?s ?p ?o) (row (?s <x>)(?p <p>)(?o 1)))") ;
        rs1 = BuilderResultSet.build(item) ;
    }
    
    // DRY - test protocol?
    @BeforeClass public static void beforeClass()
    {
        ServerTest.allocServer() ;
        ServerTest.resetServer() ;
        DatasetAccessor du = DatasetAccessorFactory.createHTTP(serviceREST) ;
        du.putModel(model1) ;
        du.putModel(gn1, model2) ;
    }
    
    @AfterClass public static void afterClass()
    {
        DatasetAccessor du = DatasetAccessorFactory.createHTTP(serviceREST) ;
        du.deleteDefault() ;
        ServerTest.freeServer() ;
    }
    
    @Test public void query_01()
    {
        execQuery("SELECT * {?s ?p ?o}", 1) ;
    }

    private void execQuery(String queryString, int exceptedRowCount)
    {
        QueryExecution qExec = QueryExecutionFactory.sparqlService(serviceQuery, queryString) ;
        ResultSet rs = qExec.execSelect() ;
        int x = ResultSetFormatter.consume(rs) ;
        assertEquals(exceptedRowCount, x) ;
    }
    
    private void execQuery(String queryString, ResultSet expectedResultSet)
    {
        QueryExecution qExec = QueryExecutionFactory.sparqlService(serviceQuery, queryString) ;
        ResultSet rs = qExec.execSelect() ;
        boolean b = ResultSetCompare.equalsByTerm(rs, expectedResultSet) ;
        assertTrue("Result sets different", b) ;
    }

}
