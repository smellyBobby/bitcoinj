package com.google.bitcoin.core.experimental;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static com.google.bitcoin.core.Utils.*;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DiskHashStoreTest {
    public static interface T_ShouldQueryDisk{};
    
    @Category(T_ShouldQueryDisk.class)
	@Test
	public void constructor() throws Exception{
		HashStore4DISK query = new HashStore4DISK(100000);
		
	}
	
    @Category(T_ShouldQueryDisk.class)
    @Test
	public void putOne() throws Exception{
		HashStore4DISK query = new HashStore4DISK(100000);
		byte[] hash = doubleDigest("taoeuhstoeahus".getBytes());
		query.put(hash, 0);
	}
	
    @Category(T_ShouldQueryDisk.class)
    @Test
	public void putGetOneA() throws Exception{
		HashStore4DISK sampleStore = new HashStore4DISK(100000);
		byte[] hash = doubleDigest("tastnhaoesutha".getBytes());
		sampleStore.put(hash, 0);
		
		boolean result = sampleStore.possiblePositive(hash, 0);
		assertThat(result,equalTo(true));
	}
	
    @Category(T_ShouldQueryDisk.class)
    @Test
	public void putGetOneB() throws Exception{
		HashStore4DISK sampleStore = new HashStore4DISK(100000);
		byte[] hashOne = doubleDigest("asteuhsateuh".getBytes());
		byte[] hashTwo = doubleDigest("toaehc.scoeatsuh".getBytes());
		
		sampleStore.put(hashOne,0);
		boolean result = sampleStore.possiblePositive(hashTwo, 0);
		
		assertThat(result,equalTo(false));
	}
}
