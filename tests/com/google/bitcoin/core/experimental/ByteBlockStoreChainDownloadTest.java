package com.google.bitcoin.core.experimental;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static com.google.bitcoin.core.Utils.*;
import static com.google.bitcoin.core.experimental.SupportMethods.*;
import static com.google.bitcoin.core.experimental.Utils.*;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.StoredBlock;


public class ByteBlockStoreChainDownloadTest {

    static Logger logger = Logger.getLogger("TEST_ByteBlockStoreChainDownload");
	
    CoordChainDownload byteBlockStoreChainDownload;
	
	StoredBlockSerializerImpl serializer;
	
	Block block;
	
	HashStoreForAll hashStore;
	
	BytesInRamBlocks memoryStoredBlocks;
	
	File f;
	
	BytesInDiskBlocksWriter bwds;
	
	@Category(ByteBlockStoreChainDownload_test.class)
    @Test()
    public void genesisBlock_prevHash() throws Exception{
		Block b =NetworkParameters.prodNet().genesisBlock;
	    
	    assertThat(b.getPrevBlockHash(), notNullValue());
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
    @Test()
    public void genesisBlock_hash() throws Exception{
	    Block b = NetworkParameters.prodNet().genesisBlock;
	    assertThat(b.getHash(),notNullValue());
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
    @Test()
    public void genesisBlock_generateBlock() throws Exception{
	    Block gBlock = NetworkParameters.prodNet().genesisBlock;
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
			boolean result =hashStore.contains(storedBlock.getByteHash());
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
			StoredBlock block = storedBlocks.get(i);
			StoredBlock other = memoryStoredBlocks.getStoredBlock(i);
			assertThat("Failure on index "+i,other.getByteHash(),equalTo(
					block.getByteHash()));
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
		
		assertThat(result.getByteHash(),
				equalTo(temp.getByteHash()));
		
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test()
	public void loadA() throws Exception{
		fillWith(200000,byteBlockStoreChainDownload,block);
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		CoordChainDownload bbscd = b2();
		bbscd.load();
		
		assertThat(f.length(),equalTo((long)200000*92));
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void load_checkHashStore() throws Exception{
	    fillWith(200000,byteBlockStoreChainDownload,block);
	    byteBlockStoreChainDownload.persist();
	    byteBlockStoreChainDownload = null;
	    CoordChainDownload bbscd = b2();
	    assertThat(bbscd.hashStore.nRecordedHashes(),
	            equalTo(0));
	    bbscd.load();
	    assertThat(bbscd.hashStore.nRecordedHashes(),
	            equalTo(200000));
	    
	}
	

	/**
	 * 
	 * Remember that your not testing for all blocks in the 
	 * block-chain. Only the most recent blocks added to the
	 * block-chain.
	 * 
	 */
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void load_checkMemoryBytesBlockStore() throws Exception{
		List<StoredBlock> storedBlocks =
			fillWith(200000,byteBlockStoreChainDownload,block);
		byteBlockStoreChainDownload.persist();
		byte[] storedBlocksArrayExpected = byteBlockStoreChainDownload.memoryStoredBlocks.storedBlocksArray;
		byteBlockStoreChainDownload = null;
		CoordChainDownload bbscd = b2();
		assertThat(bbscd.hashStore.nRecordedHashes(),
				equalTo(0));
		
		// This makes sure that BytesInRamBlocks.storedBlocksArray is blank.
		assertThat(bbscd.memoryStoredBlocks.firstHash(),
				equalTo(new byte[32]));
		assertThat(bbscd.memoryStoredBlocks.lastHash(),
				equalTo(new byte[32]));
		
		bbscd.load();
		
		byte[] storedBlocksArrayResult = 
			bbscd.memoryStoredBlocks.storedBlocksArray;
		assertThat(storedBlocksArrayExpected,
				equalTo(storedBlocksArrayResult));
		int threshold = (bbscd.chainLength-BytesInRamBlocks.N_INITIAL_BLOCKS);
		for(int i=threshold;i<200000;i++){
			StoredBlock block = storedBlocks.get(i);
			StoredBlock other = 
				bbscd.memoryStoredBlocks.getStoredBlock(i-threshold);
			assertThat("Failure on index "+i,block.getByteHash(),
					equalTo(other.getByteHash()));
		}
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void load_checkByteDiskStore() throws Exception{
		List<StoredBlock> storedBlocks =
			fillWith(200000,byteBlockStoreChainDownload,block);
		
		
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		CoordChainDownload bbscd = b2();
		
		bbscd.load();
		BytesInDiskBlocksWriter byteDiskStore = 
			bbscd.byteDiskStore;
		
		StoredBlockSerializer serializer = 
			new StoredBlockSerializerImpl(NetworkParameters.prodNet());
		
		for(int i=0;i<200000;i++){
			byte[] expected = serializer.serialize(
				storedBlocks.get(i));
			byte[] result = byteDiskStore.getBlockBytes(i);
			//Remove the previous hash from result.
			byte[] resultWithoutPrevHash = 
				Arrays.copyOfRange(result, 32,result.length);
			
			assertThat("Failed index: "+ i,
					expected,
					equalTo(resultWithoutPrevHash));
		}
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void loadCycles_2() throws Exception{
		List<StoredBlock> storedBlocks = 
			fillWith(50000,byteBlockStoreChainDownload,block);
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		
		CoordChainDownload bbscd = b2();
		bbscd.load();
		
		storedBlocks.addAll(
		   fillWith(50000,bbscd,storedBlocks.get(storedBlocks.size()-1).getHeader()));
		
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void loadCycles_3() throws Exception{
		List<StoredBlock> storedBlocks = 
			fillWith(50000,byteBlockStoreChainDownload,block);
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		
		CoordChainDownload bbscd = b2();
		bbscd.load();
		
		storedBlocks.addAll(
			fillWith(50000,bbscd,storedBlocks.get(storedBlocks.size()-1).getHeader()));
		bbscd.persist();
		bbscd = null;
		bbscd = b2();
		bbscd.load();
		
		storedBlocks.addAll(
			fillWith(50000,bbscd,storedBlocks.get(storedBlocks.size()-1).getHeader()));
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void loadCycles_10() throws Exception{
		List<StoredBlock> storedBlocks = 
			fillWith(15000,byteBlockStoreChainDownload,block);
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		
		for(int i=0;i<9;++i){
			CoordChainDownload bbscd = b2();
			bbscd.load();
			storedBlocks.addAll(
					fillWith(15000,bbscd,storedBlocks.get(storedBlocks.size()-1).getHeader()));
				bbscd.persist();
			bbscd=null;
		}
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void loadCycles_CheckHashStore() throws Exception{
		List<StoredBlock> storedBlocks = 
			fillWith(15000,byteBlockStoreChainDownload,block,0);
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		
		CoordChainDownload bbscd2 = null;
		for(int i=0;i<9;++i){
			CoordChainDownload bbscd = b2();
			bbscd.load();
			storedBlocks.addAll(
					fillWith(15000,bbscd,storedBlocks.get(storedBlocks.size()-1).getHeader()));
				bbscd.persist();
			if(i==8){
				bbscd2 = bbscd;
			}else
				bbscd=null;
		}
		
		HashStoreForAll hashStore = bbscd2.hashStore;
		
		assertThat(hashStore.nRecordedHashes(),equalTo(150000));
		
	}
	
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void loadCycles_CheckHashStoreB() throws Exception{
		List<StoredBlock> storedBlocks = generateBlocks(150000,block);
		int index = 0;
		CoordChainDownload bbscd2 = null;
		
		for(int i=0;i<10;++i){
			CoordChainDownload bbscd = b2();
			bbscd.load();
			for(int j=0;j<15000;j++){
				bbscd.putStoredBlock(storedBlocks.get(index++));
			}
			bbscd.persist();
			if(i==9){
				bbscd2 = bbscd;
			}else
				bbscd=null;
		}
		
		validateUniqueHashes(storedBlocks);
		HashStoreForAll hashStore = bbscd2.hashStore;
		
		int cnt = 0;
		int cnt2 = 0;
		for(int i=0;i<storedBlocks.size();i++){
			StoredBlock b = storedBlocks.get(i);
			if(!hashStore.contains(b.getByteHash())){
				cnt++;
			}
			
			int idex = hashStore.getIndexPosition(b.getByteHash());
			if(idex == HashStoreForAll.expectedAddresses){
				++cnt2;
			}
		}
		println("count collisions  "+cnt2);
		println("count not contains"+cnt);
		println("count collidedHashes " + hashStore.collidedHashes.size());
		println("count coord collsions" + bbscd2.cnt);
	}
	
	public void validateUniqueBlocks(List<StoredBlock> storedBlocks){
		Set<String> hashes = new HashSet<String>();
		for(StoredBlock storedBlock:storedBlocks){
			hashes.add(storedBlock.getHeader().getHashAsString());
		}
		println(hashes.size());
		println(storedBlocks.size());
		assertThat(hashes.size(),equalTo(storedBlocks.size()));
	}
	
	/**
	 * This assumes that HashStoreForAll is using the indexes 
	 * 28,29,30,(31&0xF0) as the sample.
	 */
	public void validateUniqueHashes(List<StoredBlock> storedBlocks){
		Set<String> result = new HashSet<String>();
		for(StoredBlock storedBlock:storedBlocks){
			byte[] buf = new byte[4];
			System.arraycopy(storedBlock.getByteHash(), 28, buf, 0, 4);
			buf[3] = (byte) (buf[3] & 0xF0);
			result.add(bytesToHexString(buf));
		}
		println(result.size());
		println(storedBlocks.size());
		println(result.size()-storedBlocks.size());
	}
	/**
	 * This test was written because HashStoreForAll was throwing
	 * an invalid distance exception, but the invalid distance 
	 * was zero.
	 */
	@Category(ByteBlockStoreChainDownload_test.class)
	@Test
	public void testHashStore() throws Exception{
		fillWith(1,byteBlockStoreChainDownload,block);
		byteBlockStoreChainDownload.persist();
		byteBlockStoreChainDownload = null;
		
		CoordChainDownload bbscd = b2();
		bbscd.load();
	}
	@Before
	public void beforeTest() throws IOException{
		
	    this.block = NetworkParameters.prodNet().genesisBlock;
		this.hashStore = new HashStoreForAll();
		this.serializer = new StoredBlockSerializerImpl(null);
		this.memoryStoredBlocks = new BytesInRamBlocks(serializer);
		this.f = blankQueryDiskFile();
		this.bwds = new BytesInDiskBlocksWriter(
                NetworkParameters.prodNet().genesisBlock,serializer,f);
		this.byteBlockStoreChainDownload = b();
		
			
	}
	
	public CoordChainDownload b() throws IOException{
	    
		return new CoordChainDownload(
				hashStore, memoryStoredBlocks, bwds, 200000);
	}
	
	public CoordChainDownload b2() throws IOException{
        return new CoordChainDownload(
                new HashStoreForAll(), 
                new BytesInRamBlocks(serializer), bwds, 200000);
    }
	
	public static List<StoredBlock> fillWith(int num,CoordChainDownload BBSCD,
			Block header, int height){
		StoredBlock temp = new StoredBlock(header,BigInteger.ONE,height);
		temp = generateBlock(temp,height++);
		List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
		
		for(int i=0;i<num;i++){
			BBSCD.putStoredBlock(temp);
			storedBlocks.add(temp);
			temp = generateBlock(temp,height++);
		}
		return storedBlocks;
	}
	
	public static List<StoredBlock> generateBlocks(int num,Block initialBlock){
		StoredBlock temp = new StoredBlock(initialBlock,BigInteger.ONE,0);
		temp = generateBlock(temp,0);
		List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
		
		for(int i=0;i<num;i++){
			storedBlocks.add(temp);
			temp = generateBlock(temp,i+1);
		}
		
		return storedBlocks;
	}
	
	public static List<StoredBlock> fillWith(int num,CoordChainDownload BBSCD, Block header){
		StoredBlock temp = new StoredBlock(header,BigInteger.ONE,2);
		temp = generateBlock(temp,0);
		List<StoredBlock> storedBlocks = new ArrayList<StoredBlock>();
		
		for(int i=0;i<num;i++){
			BBSCD.putStoredBlock(temp);
			storedBlocks.add(temp);
			temp = generateBlock(temp,i+1);
		}
		return storedBlocks;
	}
	public static StoredBlock generateBlock(StoredBlock previous,int i){
		BigInteger dec = BigInteger.valueOf(i);
		Block block = newBlock();
		block.setPrevBlockHash(previous.getByteHash());
		block.setMerkleRoot(doubleDigest(dec.toByteArray()));
		block.setHash(doubleDigest(dec.add(BigInteger.valueOf(i+1)).toByteArray()));
		
		StoredBlock result = new StoredBlock(block,BigInteger.TEN,i);
		return result;
	}
	static public interface ByteBlockStoreChainDownload_test{};
}
