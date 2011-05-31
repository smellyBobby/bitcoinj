package com.google.bitcoin.blockstore.experimental;

import static com.google.bitcoin.core.Utils.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TEST_ShouldQueryDisk {
    public static interface T_ShouldQueryDisk{};
    
    @Category(T_ShouldQueryDisk.class)
	@Test
	public void constructor() throws Exception{
		ShouldQueryDisk query = new ShouldQueryDisk(100000);
		
	}
	
    @Category(T_ShouldQueryDisk.class)
    @Test
	public void putOne() throws Exception{
		ShouldQueryDisk query = new ShouldQueryDisk(100000);
		byte[] hash = doubleDigest("taoeuhstoeahus".getBytes());
		query.put(hash, 0);
	}
	
    @Category(T_ShouldQueryDisk.class)
    @Test
	public void putGetOneA() throws Exception{
		ShouldQueryDisk sampleStore = new ShouldQueryDisk(100000);
		byte[] hash = doubleDigest("tastnhaoesutha".getBytes());
		sampleStore.put(hash, 0);
		
		boolean result = sampleStore.possiblePositive(hash, 0);
		assertThat(result,equalTo(true));
	}
	
    @Category(T_ShouldQueryDisk.class)
    @Test
	public void putGetOneB() throws Exception{
		ShouldQueryDisk sampleStore = new ShouldQueryDisk(100000);
		byte[] hashOne = doubleDigest("asteuhsateuh".getBytes());
		byte[] hashTwo = doubleDigest("toaehc.scoeatsuh".getBytes());
		
		sampleStore.put(hashOne,0);
		boolean result = sampleStore.possiblePositive(hashTwo, 0);
		
		assertThat(result,equalTo(false));
	}
}
