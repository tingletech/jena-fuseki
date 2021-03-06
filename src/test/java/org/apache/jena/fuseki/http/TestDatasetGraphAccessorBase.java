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

package org.apache.jena.fuseki.http;

import org.apache.jena.fuseki.BaseServerTest ;
import org.apache.jena.fuseki.http.DatasetGraphAccessor ;
import org.junit.Test ;

import com.hp.hpl.jena.graph.Graph ;
import com.hp.hpl.jena.sparql.graph.GraphFactory ;

public abstract class TestDatasetGraphAccessorBase extends BaseServerTest
{
    // return a DatasetGraphAccessor backed by an empty dataset
    protected abstract DatasetGraphAccessor getDatasetUpdater() ;
    
    private static void assertNullOrEmpty(Graph graph)
    {
        if ( graph == null ) return ; 
        assertTrue(graph.isEmpty()) ;
    }
    
    @Test public void get_01()
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        Graph graph = updater.httpGet() ;
        assertNullOrEmpty(graph) ;
        Graph graph2 = updater.httpGet(n1) ;
    }
    
    @Test public void get_02()
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        Graph graph = updater.httpGet(n1) ;
        assertNullOrEmpty(graph) ;
    }
    
    @Test public void put_01()
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        updater.httpPut(graph1) ;
        
        Graph graph = updater.httpGet() ;
        assertNotNull("Graph is null", graph) ;
        assertTrue(graph.isIsomorphicWith(graph1)) ;
    }

    
    @Test public void put_02()
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        updater.httpPut(n1, graph1) ;
        
        Graph graph = updater.httpGet() ;
        assertNullOrEmpty(graph) ;
        
        graph = updater.httpGet(n1) ;
        assertNotNull("Graph is null", graph) ;
        assertTrue(graph.isIsomorphicWith(graph1)) ;
    }

    @Test public void post_01()
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        updater.httpPost(graph1) ;
        updater.httpPost(graph2) ;
        Graph graph = updater.httpGet() ;
        
        Graph graph3 = GraphFactory.createDefaultGraph() ;
        graph3.getBulkUpdateHandler().add(graph1) ;
        graph3.getBulkUpdateHandler().add(graph2) ;
        assertTrue(graph.isIsomorphicWith(graph3)) ;
        assertFalse(graph.isIsomorphicWith(graph1)) ;
        assertFalse(graph.isIsomorphicWith(graph2)) ;
    }
    
    @Test public void post_02()
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        updater.httpPost(n1, graph1) ;
        updater.httpPost(n1, graph2) ;
        Graph graph = updater.httpGet(n1) ;
        Graph graph3 = GraphFactory.createDefaultGraph() ;
        graph3.getBulkUpdateHandler().add(graph1) ;
        graph3.getBulkUpdateHandler().add(graph2) ;
        assertTrue(graph.isIsomorphicWith(graph3)) ;
        assertFalse(graph.isIsomorphicWith(graph1)) ;
        assertFalse(graph.isIsomorphicWith(graph2)) ;
        
        graph = updater.httpGet() ;
        assertFalse(graph.isIsomorphicWith(graph3)) ;
    }

    // Default graph
    @Test public void delete_01()
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        updater.httpDelete() ;
        Graph graph = updater.httpGet() ;
        assertTrue(graph.isEmpty()) ;
        
        updater.httpPut(graph1) ;
        graph = updater.httpGet() ;
        assertFalse(graph.isEmpty()) ;
        
        updater.httpDelete() ;
        graph = updater.httpGet() ;
        assertTrue(graph.isEmpty()) ;
    }
    
    // Named graph, no side effects.
    @Test public void delete_02() 
    {
        DatasetGraphAccessor updater = getDatasetUpdater() ;
        //updater.httpDelete(n1) ;
        Graph graph = updater.httpGet(n1) ;
        assertNullOrEmpty(graph) ;

        updater.httpPut(graph2) ;
        updater.httpPut(n1, graph1) ;
        
        updater.httpDelete() ;
        graph = updater.httpGet() ;
        assertTrue(graph.isEmpty()) ;
        updater.httpPut(graph2) ;

        graph = updater.httpGet(n1) ;
        assertFalse(graph.isEmpty()) ;
        
        updater.httpDelete(n1) ;
        graph = updater.httpGet(n1) ;
        assertNullOrEmpty(graph) ;
        graph = updater.httpGet() ;
        assertFalse(graph.isEmpty()) ;
    }

//    @Test public void compound_01() {}
//    @Test public void compound_02() {}
}
