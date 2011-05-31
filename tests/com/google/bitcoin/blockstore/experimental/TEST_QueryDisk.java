package com.google.bitcoin.blockstore.experimental;

import static com.google.bitcoin.core.Utils.*;
import static com.bitcoin.core.test.support.Support.*;
import static com.bitcoin.core.test.support.CommonSettings.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.StoredBlock;

public class TEST_QueryDisk {
    public static interface T_QueryDisk{};
	
	QueryDisk queryDisk;
	
	@Before
	public void beforeTest() throws IOException{
	    queryDisk = new QueryDisk(blankQueryDiskFile(), storedBlockSerializer());
	}
	
	@Category(T_QueryDisk.class)
	@Test
	public void notNull() {
		assertThat(queryDisk,notNullValue());
	}
	
	@Category(T_QueryDisk.class)
    @Test
	public void putOne() throws Exception{
		StoredBlock storedBlock = generateBlock(2);
		queryDisk.setInitialPrevHash(storedBlock.getPrevByteHash());
		queryDisk.put(storedBlock, 0);
	}
	
    @Category(T_QueryDisk.class)
    @Test
	public void putGetOne() throws Exception{
		StoredBlock storedBlock = generateBlock(2);
		queryDisk.setInitialPrevHash(storedBlock.getPrevByteHash());
		queryDisk.put(storedBlock, 0);
		
		StoredBlock result = queryDisk.load( 0);
		assertThat(result.getShaHash(),equalTo(storedBlock.getShaHash()));
	}
    
    @Category(T_QueryDisk.class)
    @Test(expected=MemoryBlocksException.class)
	public void invalidPrevHash() throws Exception{
		StoredBlock storedBlock = generateBlock(2);
		queryDisk.setInitialPrevHash(storedBlock.getHeaderHash());
		
		storedBlock = generateBlock(44);
		queryDisk.put(storedBlock, 0);
	}
	
    @Category(T_QueryDisk.class)
    @Test
	public void put50000() throws Exception{
		StoredBlock temp = generateBlock(-1);
		queryDisk.setInitialPrevHash(temp.getPrevByteHash());
		
		for(int i=0;i<50000;i++){
			queryDisk.put(temp, i);
			temp = generateBlock(temp,i);
		}
	}
	
    @Category(T_QueryDisk.class)
    @Test
	public void putGet50000() throws Exception{
		StoredBlock temp = generateBlock(-1);
		queryDisk.setInitialPrevHash(temp.getPrevByteHash());
		
		for(int i=0;i<50000;i++){
			queryDisk.put(temp, i);
			temp = generateBlock(temp,i);
		}
		
		temp = generateBlock(-1);
		
		for(int i=0;i<50000;i++){
			StoredBlock result = queryDisk.load(i);
			assertThat("failed index: "+ i,result.getShaHash(),
					equalTo(temp.getShaHash()));
			temp = generateBlock(temp,i);
		}
	}
	
    @Category(T_QueryDisk.class)
    @Test
	public void putGet1_000_000() throws Exception{
		StoredBlock temp = generateBlock(-1);
		queryDisk.setInitialPrevHash(temp.getPrevByteHash());
		
		for(int i=0;i<1000000;i++){
			queryDisk.put(temp, i);
			temp = generateBlock(temp,i);
		}
		
		temp = generateBlock(-1);
		for(int i=0;i<1000000;i++){
			StoredBlock result = queryDisk.load(i);
			assertThat("Failed index: "+i,
					result.getShaHash(),
					equalTo(temp.getShaHash()));
			temp = generateBlock(temp,i);
		}
	}
	public static StoredBlock generateBlock(StoredBlock previous,int i){
		BigInteger dec = BigInteger.valueOf(i);
		Block block = newBlock();
		block.setPrevHash(previous.getHeaderHash());
		block.setMerkleRoot(doubleDigest(dec.toByteArray()));
		block.setHash(doubleDigest(dec.add(BigInteger.valueOf(i+1)).toByteArray()));
		
		StoredBlock result = new StoredBlock(block,BigInteger.TEN,66);
		return result;
	}
	public static StoredBlock generateBlock(int i){
		BigInteger dec = BigInteger.valueOf(i);
		Block block = newBlock();
		block.setPrevHash(doubleDigest("stestohusatuh".getBytes()));
		block.setMerkleRoot(doubleDigest(dec.toByteArray()));
		block.setHash(doubleDigest(dec.add(BigInteger.valueOf(i+1)).toByteArray()));
		
		StoredBlock result = new StoredBlock(block,BigInteger.TEN,66);
		return result;
		
	}
	
}
