package com.google.bitcoin.core.experimental;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static com.google.bitcoin.core.Utils.*;
import static com.google.bitcoin.core.experimental.SupportMethods.*;

import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;




import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.StoredBlock;

public class BytesInRamBlocksTest {
    public static interface T_StoredBlocksInByteArray{};
    
	BytesInRamBlocks blocksInByteArray;
	
	@Category(T_StoredBlocksInByteArray.class)
	@Test
	public void testNotNull() throws Exception{
		assertThat(blocksInByteArray,notNullValue());
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test
	public void testPutOne() throws Exception{
		Block block = newBlock();
		block.setMerkleRoot(doubleDigest("tshttaaeuaoeu".getBytes()));
		block.setHash(doubleDigest("shsateusasteutseou".getBytes()));
		StoredBlock storedBlock = new StoredBlock(block,BigInteger.TEN,13);
		blocksInByteArray.putWithoutPrevHashCheck(storedBlock, 44);
		
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test
	public void putGetOne() throws Exception{
		Block block = newBlock();
		block.setMerkleRoot(doubleDigest("stohuestahuestoehsu".getBytes()));
		block.setHash(doubleDigest("stoehusasehust".getBytes()));
		StoredBlock storedBlock = new StoredBlock(block,BigInteger.TEN,66);
		blocksInByteArray.putWithoutPrevHashCheck(storedBlock, 44);
		
		StoredBlock result = blocksInByteArray.getStoredBlock(44);
		assertThat(result.getByteHash(),equalTo(block.getHash()));
		
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test
	public void putHashCheck() throws Exception{
		StoredBlock storedBlock = generateBlock(1);
		blocksInByteArray.setInitialPrevHash(storedBlock.getPrevByteHash());
		blocksInByteArray.putWithPrevHashCheck(storedBlock, 0);
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test(expected=ByteBlockStoreException.class)
	public void invalidPrevHash() throws Exception{
		StoredBlock storedBlock = generateBlock(1);
		blocksInByteArray.putWithPrevHashCheck(storedBlock, 0);
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test(expected=ByteBlockStoreException.class)
	public void putInvalidPositionA() throws Exception{
		blocksInByteArray.putWithoutPrevHashCheck(null, -1);
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test(expected=NullPointerException.class)
	public void putInvalidPositionB() throws Exception{
		blocksInByteArray.putWithoutPrevHashCheck(
		        null, BytesInRamBlocks.N_BLOCKS_STORED);
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test
	public void checkPreviousHash() throws Exception{
		StoredBlock temp = generateBlock(-1);
		blocksInByteArray.setInitialPrevHash(temp.getPrevByteHash());
		blocksInByteArray.putWithPrevHashCheck(temp, 0);
		assertThat(Arrays.equals(blocksInByteArray.getPreviousHash(1),temp.getByteHash()),
				equalTo(true));
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test
	public void put50000() throws Exception{
		StoredBlock temp = generateBlock(-1);
		blocksInByteArray.setInitialPrevHash(temp.getPrevByteHash());
		
		for(int i=0;i<50000;i++){
			blocksInByteArray.putWithPrevHashCheck(temp, i);
			temp = generateBlock(temp,i);
		}
	}
	
	@Category(T_StoredBlocksInByteArray.class)
    @Test
	public void putGet50000() throws Exception{
		StoredBlock temp = generateBlock(-1);
		blocksInByteArray.setInitialPrevHash(temp.getPrevByteHash());
		
		for(int i=0;i<50000;i++){
			blocksInByteArray.putWithPrevHashCheck(temp, i);
			temp = generateBlock(temp,i);
		}
		
		temp = generateBlock(-1);
		
		for(int i=0;i<50000;i++){
			StoredBlock result = blocksInByteArray.getStoredBlock(i);
			assertThat("failed index: "+i,result.getByteHash(),
				equalTo(temp.getByteHash())	);
			temp = generateBlock(temp,i);
		}
	}
	
	@Before
	public void beforeTest(){
	    
	    blocksInByteArray = new BytesInRamBlocks(storedBlockSerializer());
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
	public static StoredBlock generateBlock(int i){
		BigInteger dec = BigInteger.valueOf(i);
		Block block = newBlock();
		block.setPrevBlockHash(doubleDigest("stestohusatuh".getBytes()));
		block.setMerkleRoot(doubleDigest(dec.toByteArray()));
		block.setHash(doubleDigest(dec.add(BigInteger.valueOf(i+1)).toByteArray()));
		
		StoredBlock result = new StoredBlock(block,BigInteger.TEN,66);
		return result;
		
	}

	
}
