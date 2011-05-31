package com.google.bitcoin.blockstore.experimental;

import static com.bitcoin.core.test.support.CommonSettings.*;
import static com.bitcoin.core.test.support.Support.*;
import static com.google.bitcoin.core.Utils.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.genesis.GenesisBlock;


public class TEST_ByteBlockStoreChainDownload {

    static Logger logger = Logger.getLogger("TEST_ByteBlockStoreChainDownload");
	
    ByteBlockStoreChainDownload byteBlockStoreChainDownload;
	
	StoredBlockSerializerImpl serializer;
	
	Block block;
	
	HashStore hashStore;
	
	MemoryBytesStoredBlocks memoryStoredBlocks;
	
	File f;
	
	ByteWriteDiskStore bwds;
	
	@Category(ByteBlockStoreChainDownload_test.class)
    @Test()
    public void genesisBlock_prevHash() throws Exception{
	    GenesisBlock b = newGenesisBlock();
	    assertThat(b.getPrevShaHash(), notNullValue());
	    assertThat(b.getPrevBlockHash(),notNullValue());
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
    @Test()
    public void genesisBlock_hash() throws Exception{
	    GenesisBlock b = newGenesisBlock();
	    assertThat(b.getHash(),notNullValue());
	    assertThat(b.getShaHash(),notNullValue());
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
    @Test()
    public void genesisBlock_generateBlock() throws Exception{
	    GenesisBlock gBlock = newGenesisBlock();
	    StoredBlock temp = new StoredBlock(gBlock,BigInteger.ONE,2);
	    StoredBlock tempTwo = generateBlock(temp,22);
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void notNull() throws Exception{
		assertThat(byteBlockStoreChainDownload,notNullValue());
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void putOne() throws Exception{
		StoredBlock temp = new StoredBlock(block,BigInteger.ONE,2);
		
		temp = generateBlock(temp,22);
		byteBlockStoreChainDownload.putStoredBlock(temp);
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void put100000() throws Exception{
		StoredBlock temp = new StoredBlock(block,BigInteger.ONE,2);
		temp = generateBlock(temp,22);
		for(int i=0;i<100000;i++){
			byteBlockStoreChainDownload.putStoredBlock(temp);
			temp = generateBlock(temp,i);
		}
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void put200000() throws Exception{
		StoredBlock temp = new StoredBlock(block,BigInteger.ONE,2);
		temp = generateBlock(temp,22);
		for(int i=0;i<200000;i++){
			byteBlockStoreChainDownload.putStoredBlock(temp);
			temp = generateBlock(temp,i);
		}
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void put200000_checkHashStore(){
		StoredBlock temp = new StoredBlock(block,BigInteger.ONE,2);
		temp = generateBlock(temp,22);
		List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
		
		for(int i=0;i<200000;i++){
			byteBlockStoreChainDownload.putStoredBlock(temp);
			storedBlocks.add(temp);
			temp = generateBlock(temp,i);
		}
		
		for(int i=0;i<storedBlocks.size();i++){
			StoredBlock storedBlock = storedBlocks.get(i);
			boolean result =hashStore.contains(storedBlock.getHeaderHash());
			assertThat("Failed index "+i,result,equalTo(true));
		}
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void put200000_checkMemoryBytesStoredBlocks(){
		StoredBlock temp = new StoredBlock(block,BigInteger.ONE,2);
		temp = generateBlock(temp,0);
		List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
		
		for(int i=0;i<200000;i++){
			byteBlockStoreChainDownload.putStoredBlock(temp);
			storedBlocks.add(temp);
			temp = generateBlock(temp,i+1);
		}
		
		for(int i=0;i<200000;i++){
			if(i<152000)continue;
			StoredBlock block = storedBlocks.get(i);
			StoredBlock other = memoryStoredBlocks.getStoredBlock(i-152000);
			assertThat("Failure on index "+i,other.getShaHash(),equalTo(
					block.getShaHash()));
		}
		
	
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void put200000_checkByteDiskStore(){
		StoredBlock temp = new StoredBlock(block,BigInteger.ONE,2);
		temp = generateBlock(temp,0);
		List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
		
		for(int i=0;i<200000;i++){
			byteBlockStoreChainDownload.putStoredBlock(temp);
			storedBlocks.add(temp);
			temp = generateBlock(temp,i+1);
		}
		byteBlockStoreChainDownload.persist();
		assertThat(f.length(),equalTo((long)200000*92));
	}

	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void testSerializer(){
		StoredBlock temp = new StoredBlock(block,BigInteger.ONE,2);
		temp = generateBlock(temp,2);
		byte[] arr = serializer.serialize(temp);
		byte[] real = new byte[92+32];
		System.arraycopy(arr, 0, real, 32, arr.length);
		System.arraycopy(temp.getPrevByteHash(), 0, real, 0, 32);
		StoredBlock result = serializer.deserialize(real);
		
		assertThat(result.getShaHash(),
				equalTo(temp.getShaHash()));
		
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void loadA() throws Exception{
		fillWith(200000,byteBlockStoreChainDownload,block);
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		logger.info(f.length()+" ");
		ByteBlockStoreChainDownload bbscd = b2();
		logger.info(f.length()+" ");
		bbscd.load();
		logger.info(f.length()+" ");
		
		assertThat(f.length(),equalTo((long)200000*92));
	}
	
	@Before
	public void beforeTest() throws IOException{
		
	    this.block = newGenesisBlock();
		this.hashStore = new HashStore();
		this.serializer = new StoredBlockSerializerImpl(null);
		this.memoryStoredBlocks = new MemoryBytesStoredBlocks(serializer);
		this.f = queryDiskFile();
		this.bwds = new ByteWriteDiskStore(
                newGenesisBlock(),serializer,f);
		this.byteBlockStoreChainDownload = b();
		
			
	}
	
	public ByteBlockStoreChainDownload b() throws IOException{
	    
		return new ByteBlockStoreChainDownload(
				hashStore, memoryStoredBlocks, bwds, 200000);
	}
	
	public ByteBlockStoreChainDownload b2() throws IOException{
        return new ByteBlockStoreChainDownload(
                new HashStore(), memoryStoredBlocks, bwds, 200000);
    }
	public static void fillWith(int num,ByteBlockStoreChainDownload BBSCD, Block header){
		StoredBlock temp = new StoredBlock(header,BigInteger.ONE,2);
		temp = generateBlock(temp,0);
		List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
		
		for(int i=0;i<200000;i++){
			BBSCD.putStoredBlock(temp);
			storedBlocks.add(temp);
			temp = generateBlock(temp,i+1);
		}
	}
	public static StoredBlock generateBlock(StoredBlock previous,int i){
		BigInteger dec = BigInteger.valueOf(i);
		Block block = newBlock();
		block.setPrevHash(previous.getHeaderHash());
		block.setMerkleRoot(doubleDigest(dec.toByteArray()));
		block.setHash(doubleDigest(dec.add(BigInteger.valueOf(i+1)).toByteArray()));
		
		StoredBlock result = new StoredBlock(block,BigInteger.TEN,i);
		return result;
	}
	static public interface ByteBlockStoreChainDownload_test{};
}
