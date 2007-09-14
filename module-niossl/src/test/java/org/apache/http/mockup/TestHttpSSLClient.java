/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.mockup;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.SSLClientIOEventDispatch;
import org.apache.http.nio.NHttpClientHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOEventDispatch;
import org.apache.http.params.HttpParams;

public class TestHttpSSLClient {

    private final SSLContext sslcontext;
    private final ConnectingIOReactor ioReactor;
    private final HttpParams params;
    
    private volatile IOReactorThread thread;

    
    public TestHttpSSLClient(final HttpParams params) throws Exception {
        super();
        this.params = params;
        this.ioReactor = new DefaultConnectingIOReactor(2, this.params);
        
        ClassLoader cl = getClass().getClassLoader();
        URL url = cl.getResource("test.keystore");
        KeyStore keystore  = KeyStore.getInstance("jks");
        keystore.load(url.openStream(), "nopassword".toCharArray());
        TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(keystore);
        TrustManager[] trustmanagers = tmfactory.getTrustManagers(); 
        this.sslcontext = SSLContext.getInstance("TLS");
        this.sslcontext.init(null, trustmanagers, null);
    }
    
    public HttpParams getParams() {
        return this.params;
    }
    
    private void execute(final NHttpClientHandler clientHandler) throws IOException {
        IOEventDispatch ioEventDispatch = new SSLClientIOEventDispatch(
                clientHandler, 
                this.sslcontext,
                this.params);
        
        this.ioReactor.execute(ioEventDispatch);
    }
    
    public void openConnection(final InetSocketAddress address, final Object attachment) {
        this.ioReactor.connect(address, null, attachment, null);
    }
 
    public void start(final NHttpClientHandler clientHandler) {
        this.thread = new IOReactorThread(clientHandler);
        this.thread.start();
    }
    
    public void shutdown() throws IOException {
        this.ioReactor.shutdown();
        try {
            this.thread.join(500);
        } catch (InterruptedException ignore) {
        }
    }
    
    private class IOReactorThread extends Thread {

        private final NHttpClientHandler clientHandler;
        
        public IOReactorThread(final NHttpClientHandler clientHandler) {
            super();
            this.clientHandler = clientHandler;
        }
        
        public void run() {
            try {
                execute(this.clientHandler);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }    
    
}