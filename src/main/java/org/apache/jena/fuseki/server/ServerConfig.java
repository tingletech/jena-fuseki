/**
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

package org.apache.jena.fuseki.server;

import java.util.List ;

/** This represents a configuration of a SPARQL server.
 */

// Not yet used.

public class ServerConfig
{
    public ServerConfig() {}
    
    /** Port to run the server service on */
    public int port ;
    /** Port for the management interface : -1 for no mamangement interface */ 
    public int mgtPort ;
    /** Port for the pages UI : this can be the same as the services port. */ 
    public int pagesPort ;
    /** Jetty config file - if null, use the built-in configuration of Jetty */
    public String jettyConfigFile ;
    /** The local directory for serving the static pages */ 
    public String pages ;
    /** The list of services */
    public List<DatasetRef> services ;
    /** Enable Accept-Encoding compression */
    public boolean enableCompression = false ;
}

