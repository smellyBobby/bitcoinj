/**
 * Copyright 2011 smellyBobby
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
package com.google.bitcoin.network.peerdiscovery;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.bitcoin.core.NetworkParameters;

public class TEST_PNSeedPeers {

	@Category(PNSeedPeersTests.class)
	@Test 
	public void construction(){
		PNSeedPeers t = new PNSeedPeers(null);
	}
	
	@Category(PNSeedPeersTests.class)
	@Test
	public void getPeer_one() throws Exception{
		PNSeedPeers seedPeers = new PNSeedPeers(NetworkParameters.prodNet());
		assertThat(seedPeers.getPeer(),notNullValue());
	}
	
	@Category(PNSeedPeersTests.class)
	@Test 
	public void getPeer_all() throws Exception{
		PNSeedPeers seedPeers = new PNSeedPeers(NetworkParameters.prodNet());
		for(int i=0;i<PNSeedPeers.pnSeed.length;++i){
			assertThat("Failed on index: "+i,seedPeers.getPeer(),notNullValue());
		}
		assertThat(seedPeers.getPeer(),equalTo(null));
	}
	
	@Category(PNSeedPeersTests.class)
	@Test
	public void getPeers_length() throws Exception{
		PNSeedPeers seedPeers = new PNSeedPeers(NetworkParameters.prodNet());
		InetSocketAddress[] addresses = seedPeers.getPeers();
		assertThat(addresses.length,equalTo(PNSeedPeers.pnSeed.length));
	}
	public static interface PNSeedPeersTests{}
}
