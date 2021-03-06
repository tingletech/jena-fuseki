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

package org.apache.jena.fuseki.validation;

import static org.openjena.riot.SysRIOT.fmtMessage ;

import java.io.IOException ;
import java.io.PrintStream ;
import java.io.Reader ;
import java.io.StringReader ;

import javax.servlet.ServletOutputStream ;
import javax.servlet.http.HttpServletRequest ;
import javax.servlet.http.HttpServletResponse ;

import org.openjena.atlas.io.IO ;
import org.openjena.atlas.lib.Sink ;
import org.openjena.atlas.lib.SinkWrapper ;
import org.openjena.riot.ErrorHandler ;
import org.openjena.riot.Lang ;
import org.openjena.riot.RiotException ;
import org.openjena.riot.RiotReader ;
import org.openjena.riot.lang.LangRIOT ;
import org.openjena.riot.system.RiotLib ;
import org.openjena.riot.tokens.Tokenizer ;
import org.openjena.riot.tokens.TokenizerFactory ;

import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.sparql.core.Quad ;
import com.hp.hpl.jena.sparql.serializer.SerializationContext ;
import com.hp.hpl.jena.sparql.util.FmtUtils ;

public class DataValidator extends ValidatorBase
{
    public DataValidator() 
    { }
  
    static final String paramLineNumbers      = "linenumbers" ;
    static final String paramFormat           = "outputFormat" ;
    static final String paramIndirection      = "url" ;
    static final String paramData             = "data" ;
    static final String paramSyntax           = "languageSyntax" ;
    //static final String paramSyntaxExtended   = "extendedSyntax" ;
    
    @Override
    protected void execute(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
    {
        try {
//            if ( log.isInfoEnabled() )
//                log.info("data validation request") ;
            
            Tokenizer tokenizer = createTokenizer(httpRequest, httpResponse) ;
            if ( tokenizer == null )
                return ;
            
            String syntax = httpRequest.getParameter(paramSyntax) ;
            if ( syntax == null || syntax.equals("") )
                syntax = Lang.NQUADS.getName() ;

            Lang language = Lang.get(syntax) ;
            if ( language == null )
            {
                httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown syntax: "+syntax) ;
                return ;
            }

            ServletOutputStream outStream = httpResponse.getOutputStream() ;
            ErrorHandlerMsg errorHandler = new ErrorHandlerMsg(outStream) ;
            
            PrintStream stdout = System.out ;
            PrintStream stderr = System.err ;
            System.setOut(new PrintStream(outStream)) ;
            System.setErr(new PrintStream(outStream)) ;

            // Headers
            setHeaders(httpResponse) ;

            outStream.println("<html>") ;
            printHead(outStream, "Jena Data Validator Report") ;
            outStream.println("<body>") ;
            
            outStream.println("<h1>RIOT Parser Report</h1>") ;
            outStream.println("<p>Line and column numbers refer to original input</p>") ;
            outStream.println("<p>&nbsp;</p>") ;
            try {
                LangRIOT parser = setupParser(tokenizer, language, errorHandler, outStream) ;
                startFixed(outStream) ;
                RiotException exception = null ;
                try {
                    parser.parse() ;
                    System.out.flush() ;
                    System.err.flush() ;
                } catch (RiotException ex) { exception = ex ; }
            } finally 
            {
                finishFixed(outStream) ;
                System.out.flush() ;
                System.err.flush() ;
                System.setOut(stdout) ;
                System.setErr(stdout) ;
            }
            
            outStream.println("</body>") ;
            outStream.println("</html>") ;
        } catch (Exception ex)
        {
            serviceLog.warn("Exception in validationRequest",ex) ;
        }
    }
    
    static final long LIMIT = 50000 ;
    
    
    private LangRIOT setupParser(Tokenizer tokenizer, Lang language, ErrorHandler errorHandler, final ServletOutputStream outStream)
    {
        Sink<Quad> sink = new Sink<Quad>()
        {
            SerializationContext sCxt = new SerializationContext() ;
            @Override
            public void send(Quad quad)
            {
                // Clean up!
                StringBuilder sb = new StringBuilder() ;

                sb.append(formatNode(quad.getSubject())) ;
                sb.append("  ") ;
                sb.append(formatNode(quad.getPredicate())) ;
                sb.append("  ") ;
                sb.append(formatNode(quad.getObject())) ;
                
                if ( ! quad.isTriple() )
                {
                    sb.append("  ") ;
                    sb.append(formatNode(quad.getGraph())) ;
                }

                String $ = htmlQuote(sb.toString()) ;
                try { 
                    outStream.print($) ;
                    outStream.println(" .") ;
                } catch (IOException ex) { IO.exception(ex) ; }
            }
            @Override
            public void close() {}
            @Override
            public void flush() {}
            String formatNode(Node n) { return FmtUtils.stringForNode(n, sCxt) ; }
        } ;
        
        Sink<Quad> sink2 = new SinkWrapper<Quad>(sink){
            long count = 0 ;
            @Override public void close() {}
            @Override public void flush() {}
            @Override 
            public void send(Quad quad)
            { 
                super.send(quad) ;
                count++ ;
                if ( count > LIMIT )
                    throw new RiotException("Limit exceeded") ;
            }
        } ;
        // Language?
        LangRIOT parser = RiotReader.createParserQuads(tokenizer, language, null, sink) ;
        // Don't resolve IRIs.  Do checking.
        parser.setProfile(RiotLib.profile(null, false, true, errorHandler)) ;
        return parser ;
    }

    // Error handler that records messages
    private static class ErrorHandlerMsg implements ErrorHandler
    {
        private ServletOutputStream out ;

        ErrorHandlerMsg(ServletOutputStream out) { this.out = out ; }
        
        @Override
        public void warning(String message, long line, long col)
        { output(message, line, col, "Warning", "warning") ; }
    
        // Attempt to continue.
        @Override
        public void error(String message, long line, long col)
        { output(message, line, col, "Error", "error") ; }
    
        @Override
        public void fatal(String message, long line, long col)
        { output(message, line, col, "Fatal", "error") ; throw new RiotException(fmtMessage(message, line, col)) ; }
        
        private void output(String message, long line, long col, String typeName, String className)
        {
            try {
                String str = fmtMessage(message, line, col) ;
                //String str = typeName+": "+message ;
                str = htmlQuote(str) ;
                out.print("<div class=\""+className+"\">") ;
                out.print(str) ;
                out.print("</div>") ;
            } catch (IOException ex) { IO.exception(ex) ; }
        }
    }
    
    private Tokenizer createTokenizer(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception
    {
        Reader reader = null ;  
//        String[] args = httpRequest.getParameterValues(paramIndirection) ;
//        
//        if ( args == null || args.length == 0 )
//        {
//            reader = httpRequest.getReader() ;
//        } 
//        else if ( args.length > 1 )
//        {
//            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Too many parameters for '"+paramIndirection+"='") ;
//            return null ;
//        }
////        else
////        {
////            reader = // get from afar.
////        }
        
        String[] args = httpRequest.getParameterValues(paramData) ;
        if ( args == null || args.length == 0 )
        {
            // Not a form?
            reader = httpRequest.getReader() ;
        }
        else if ( args.length > 1 )
        {
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Too many parameters for '"+paramData+"='") ;
            return null ;
        }
        else
        {
            reader = new StringReader(args[0]) ;
        }
        
        if ( reader == null )
        {
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Can't find data to validate") ;
            return null ;
        }
        
        return TokenizerFactory.makeTokenizer(reader) ;
    }
}
