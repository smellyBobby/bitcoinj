package com.google.bitcoin.core.experimental;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static com.google.bitcoin.core.Utils.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;


import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.StoredBlock;


public class ByteWriteDiskStoreTest {
	public static interface T_ByteWriteDiskStore{};
	
	static Logger logger = Logger.getLogger("TEST_ByteWriteDiskStore");
	static int BLOCK_SIZE = StoredBlocksMethods.BLOCK_SIZE;

	BytesInDiskBlocksWriter byteWriteDiskStore;

	Block block;
	
	File f;
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void notNull(){
		assertThat(byteWriteDiskStore,notNullValue());
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void tester(){
		StoredBlock bblock = new StoredBlock(block,BigInteger.ONE,2);
		
		StoredBlock next = generateBlock(bblock,44);
		
		assertThat(next.getPrevByteHash(),equalTo(block.getHash()));
		
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void putOne() throws Exception{
		StoredBlock next = new StoredBlock(block,BigInteger.ONE,2);
		next = generateBlock(next,2);
		byteWriteDiskStore.add(next);
	}
	@Category(T_ByteWriteDiskStore.class)
	@Test(expected=ByteBlockStoreException.class)
	public void invalidPrevHash() throws Exception{
		StoredBlock next = new StoredBlock(block,BigInteger.ONE,2);
		next = generateBlock(next,5);
		next = generateBlock(next,4);
		byteWriteDiskStore.add(next);
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void put100_000() throws Exception{
		StoredBlock next = new StoredBlock(block,BigInteger.ONE,2);
		next = generateBlock(next,2);
		
		for(int i=0;i<100000;i++){
			byteWriteDiskStore.add(next);
			next = generateBlock(next,i);
		}
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void put200_000() throws Exception{
		StoredBlock next = new StoredBlock(block,BigInteger.ONE,2);
		next = generateBlock(next,2);
		
		for(int i=0;i<200000;++i){
			byteWriteDiskStore.add(next);
			next = generateBlock(next,i);
		}
		byteWriteDiskStore.persist();
		
		
		assertThat(f.length(),
				equalTo((long)200000*StoredBlocksMethods.BLOCK_SIZE));
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void getHashesOne() throws Exception{
		fillWith(1,byteWriteDiskStore,block);
		List<StoredBlock> blocks = getBlocks(1,block);
		
		byteWriteDiskStore.persist();
		byte[] result = byteWriteDiskStore.getHashes(0,1);
		boolean t = Arrays.equals(result, blocks.get(0).getByteHash());
		
		assertThat(t,equalTo(true));
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void getHashesA() throws Exception{
		fillWith(200000,byteWriteDiskStore,block);
		byteWriteDiskStore.persist();
		byte[] result = byteWriteDiskStore.getHashes(0, 100000);
		assertThat(result.length,equalTo(100000*32));
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void Z_getHashesPerformance() throws Exception{
		fillWith(200000,byteWriteDiskStore,block);
		byteWriteDiskStore.persist();
		long start = System.currentTimeMillis();
		byte[] result = byteWriteDiskStore.getHashes(0, 100000);
		long end = System.currentTimeMillis();
		long time = end-start;
		logger.info(" "+time);
		assertThat("\nThe time it took to load 100000 \n hashes from disk was: ",
				(long)time, equalTo((long)0));
	}
	
	@Category(T_ByteWriteDiskStore.class)
	@Test
	public void getHashesB() throws Exception{
		fillWith(200000,byteWriteDiskStore,block);
		List<StoredBlock> blocks = getBlocks(200000,block);
		
		byte[] result = byteWriteDiskStore.getHashes(0,100000);
		for(int i=0;i<100000;i++){
			StoredBlock temp = blocks.get(i);
			byte[] buf = new byte[32];
			System.arraycopy(result, i*32, buf, 0, 32);
			boolean t = Arrays.equals(temp.getByteHash(),
					buf);
			assertThat("Failed on index: "+i,t,equalTo(true));
		}
		
	}
	
	@Before
	public void beforeTest() throws IOException{
	    this.block = NetworkParameters.prodNet().genesisBlock;
	    this.f = blankQueryDiskFile();
	    this.byteWriteDiskStore = new BytesInDiskBlocksWriter(
	            new GenesisBlock(null), storedBlockSerializer(), f);
	}
	public static void fillWith(int num,BytesInDiskBlocksWriter bwds,
			Block initialHeader){
		StoredBlock next = new StoredBlock(initialHeader,BigInteger.ONE,2);
		next = generateBlock(next,2);
		
		for(int i=0;i<num;++i){
			bwds.add(next);
			next = generateBlock(next,i);
		}
	}
	
	public static List<StoredBlock> getBlocks(int num,Block initialHeader){
		StoredBlock next = new StoredBlock(initialHeader,BigInteger.ONE,2);
		next = generateBlock(next,2);
		List<StoredBlock> result = new ArrayList<StoredBlock>();
		
		for(int i=0;i<num;++i){
			result.add(next);
			next = generateBlock(next,i);
		}
		
		return result;
	}
	public static StoredBlock generateBlock(StoredBlock previous,int i){
		BigInteger dec = BigInteger.valueOf(i);
		Block block = newBlock();
		block.setPrevBlockHash(previous.getByteHash());
		block.setMerkleRoot(doubleDigest(dec.toByteArray()));
		block.setHash(doubleDigest(dec.add(BigInteger.valueOf(i+1)).toByteArray()));
		
		StoredBlock result = new StoredBlock(block,BigInteger.TEN,66);
		return result;
	}
	


	
}
