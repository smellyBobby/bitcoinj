/**
 * Copyright 2011 Micheal Swiggs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.bitcoin.core;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import java.net.InetSocketAddress;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.bitcoin.core.NetworkParameters;

public class PnSeedPeersTest {

	@Category(PNSeedPeersTests.class)
	@Test 
	public void construction(){
		PnSeedPeers t = new PnSeedPeers(null);
	}
	
	@Category(PNSeedPeersTests.class)
	@Test
	public void getPeer_one() throws Exception{
		PnSeedPeers seedPeers = new PnSeedPeers(NetworkParameters.prodNet());
		assertThat(seedPeers.getPeer(),notNullValue());
	}
	
	@Category(PNSeedPeersTests.class)
	@Test 
	public void getPeer_all() throws Exception{
		PnSeedPeers seedPeers = new PnSeedPeers(NetworkParameters.prodNet());
		for(int i=0;i<PnSeedPeers.pnSeed.length;++i){
			assertThat("Failed on index: "+i,seedPeers.getPeer(),notNullValue());
		}
		assertThat(seedPeers.getPeer(),equalTo(null));
	}
	
	@Category(PNSeedPeersTests.class)
	@Test
	public void getPeers_length() throws Exception{
		PnSeedPeers seedPeers = new PnSeedPeers(NetworkParameters.prodNet());
		InetSocketAddress[] addresses = seedPeers.getPeers();
		assertThat(addresses.length,equalTo(PnSeedPeers.pnSeed.length));
	}
	public static interface PNSeedPeersTests{}
}
