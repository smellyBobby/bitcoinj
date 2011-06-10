package com.google.bitcoin.core.experimental;



/**
 * My Beloved. 
 * 
 * Notes:
 * 
 * This class should be considered muted. When a hash is stored
 * here, the same hash should not call put(...) again. Otherwise
 * this will cause nRecordedHashes() to become inconsistent.
 * 
 * @author Micheal Swiggs
 *
 */
public class HashStore4ALL {

	public static int hashSize = 32;
	
	public static int storeSize = 262143;
	
	public static int expectedAddresses = 0xFFFFF;
	
	byte[][] hashes;
	byte[] maxDistances;
	byte[] blank;
	
	double indexScale;
	int heldBack = 2;
	int threshold = 50;
	
	int numStoredRecordedHashes = 0;
	public HashStore4ALL(){
		assert expectedAddresses>storeSize;
		hashes = new byte[storeSize][6];
		indexScale = (.98)*(double)storeSize/(double)expectedAddresses;
		maxDistances = new byte[storeSize];
		validate();
	}
	/**
	 * This returns the number of hashes currently stored
	 * including collisions. So for each collision this will
	 * still increment by one.
	 * 
	 * @return - number of recorded hashes.
	 */
	public int nRecordedHashes() {
		return numStoredRecordedHashes;
	}
	
	private int getPosition(byte[] hash,PositionCalleeBase positionCallee){
		
		int number = mapIndex(hash);
		int start = (int)number;
		int index = (int)number;
		int change = 1;
		int sign = 1;
		boolean decrementOnly = false;
		boolean incrementOnly = false;
		
		while(true){
			if(positionCallee.validIndex(index,start,hash) ){
				
				return index;
			}
			int nextIndex = index;
			if(incrementOnly){
				++nextIndex;
			}else if(decrementOnly){
				--nextIndex;
			}else{
				nextIndex += (change*sign);
			}
			
			if(nextIndex >= storeSize){
				if(incrementOnly)
					throw new RuntimeException("incre/decre");
				decrementOnly = true;
			}
			if(nextIndex < 0){
				if(decrementOnly)
					throw new RuntimeException("decre//incre");
				incrementOnly = true;
			}
			
			if(!incrementOnly && !decrementOnly){
				index += (change*sign);
				++change;
				sign*=-1;
			}
			if(incrementOnly){
				++index;
			}
			if(decrementOnly){
				--index;
			}
		}
	}

	PositionCalleeInit<Integer> putPositionCallee = new PositionCalleeInit<Integer>() {

		int _pDistance = 0;
		int _pStart = -1;
		boolean _collision = false;
		boolean _beyondMode = false;
		int _index = -10;
		@Override
		public void init() {
			_collision = false;
			_beyondMode = false;
			_index = -10;
		}
		
		public boolean validIndex(int index, int start, byte[] hash) {
			if (!isValidSpot(index))
				return false;
			if(_beyondMode)return checkForCollision(index,start,hash);
			if (!isNull(index)) {
				if(arrayMatch(hash,index)){
					_collision = true;
					update(index,start);
					return true;
				}
				return false;
			}
			_index = index;
			update(index,start);
			int maxDistance = maxDistances[start];
			if(!(_pDistance>maxDistance)){
				_beyondMode = true;
				return false;
			}
			return true;
		}
		private void update(int index,int start){
			processPutDistance(index, start);
			_pDistance = index - start;
			_pDistance = _pDistance > 0 ? _pDistance : _pDistance * -1;
			_pStart = start;
		}
		
		private boolean checkForCollision(int index,int start,byte[] hash){
			int maxDistance = maxDistances[start];
			int distance = index - start;
			distance = distance>0?distance:distance*-1;
			if(distance>maxDistance){
				return true;
			}
			if(isNull(index)) return false;
			if(!arrayMatch(hash,index))return false;
			_collision = true;
			return true;
		}
		
		public Integer getResult() {

			int i = maxDistances[_pStart] & 0xFF;
			if (_pDistance > 255)
				throw new RuntimeException("too far");
			if (_pDistance > i)
				maxDistances[_pStart] = (byte) _pDistance;
			if(_collision) return -1;
			return _index;
		}
		
	};
	/**
	 * This will store a sample of the hash and it's 
	 * respective position in the block chain. It will 
	 * return -1 when there are no collisions. Otherwise
	 * it will return it will return the place in the 
	 * block chain corresponding with the hash already 
	 * stored. Otherwise it will return the collision 
	 * address, which simply identifies that a collision
	 * has occurred before. 
	 * 
	 * Also this should not be repeatedly called with the same
	 * hash,position. If this occurs it will cause a collision
	 * and break other methods of this class.
	 * 
	 * The caller must obey the contract:
	 * CALLONCE(hash,....);
	 * 
	 * @param hash    - hash to be stored.
	 * @param position- place in the block-chain this hash belongs.
	 * @return -1 for success, otherwise something else.
	 */
	public int put(byte[] hash,int position){
		isNotCollisionAddress(position);
		putPositionCallee.init();
		int falseIndex = getPosition(hash,putPositionCallee);
		int indexResult = putPositionCallee.getResult();
		numStoredRecordedHashes++;
		if(indexResult!=-1){
			hashes[indexResult][0] = hash[28];
			hashes[indexResult][1] = hash[29];
			hashes[indexResult][2] = hash[30];
			hashes[indexResult][3] = 
				(byte) ((hash[31] & 0xF0 ) | ((position>>16) & 0x0F));
			hashes[indexResult][4] =
				(byte) ((position>>8) & 0xFF);
			hashes[indexResult][5] = 
				(byte) position;
			return -1;
		}else{
			int result = ((hashes[falseIndex][3] & 0x0F)<<16) |
					((hashes[falseIndex][4] & 0xFF)<<8) |
					((hashes[falseIndex][5] & 0xFF)<<0 );
			hashes[falseIndex][3] = 
				(byte) ((hashes[falseIndex][3] & 0xF0) | 
				(0x0F));
			hashes[falseIndex][4] = (byte)0xFF;
			hashes[falseIndex][5] = (byte)0xFF;
			return result;
		}
	}
	
	PositionCalleeBase retrieveCallee = 
		new PositionCalleeBase(){
		@Override
		public boolean validIndex(int index, int start,byte[] hash) {
			int maxDistance = maxDistances[start] & 0xFF;
			if((index-start)>maxDistance ||
				(start-index) > maxDistance){
				throw new RuntimeException("Invalid distance "+
						maxDistance);
			}
			if(isNull(index))return false;
			if(!arrayMatch(hash, index))return false;
			return true;
		}
	};
	
	public byte[] get(byte[] hash){
		int position = getPosition(hash,retrieveCallee);
		return hashes[position];
	}
	
	PositionCalleeInit<Boolean> containsCallee = 
		new PositionCalleeInit<Boolean>(){
			boolean _contains;
			@Override
			public Boolean getResult() {
				return _contains;
			}

			@Override
			public boolean validIndex(int index, int start, byte[] hash) {
				int maxDistance = maxDistances[start] & 0xFF;
				if((index-start)>maxDistance ||
					(start-index) > maxDistance){
					_contains = false;
					return true;
				}
				if(isNull(index))return false;
				if(!arrayMatch(hash, index))return false;
				_contains = true;
				return true;
			}

			@Override
			public void init() {
				_contains=false;
				
			}
		
	};
	public boolean contains(byte[] hash){
		containsCallee.init();
		int p = getPosition(hash,containsCallee);
		return containsCallee.getResult();
	}
	public int getIndexPosition(byte[] hash){
		int position = getPosition(hash,retrieveCallee);
		return 
		((hashes[position][3] & 0x0F) << 16) |
		((hashes[position][4] & 0xFF) <<  8) |
		(hashes[position][5] & 0xFF);
	}
	private boolean notNull(int index){
		return !isNullB(index);
	}
	
	private boolean isNullB(int i){
		byte[] hash = hashes[i];
		return 0x0==
			(hash[0] | hash[1] | hash[2] | hash[3]|hash[5]);
		
	}
	private boolean arrayMatch(byte[] one,int i){
		byte[] hash = hashes[i];
		return 0x00 ==(
			(one[28] ^ hash[0]) |
			(one[29] ^ hash[1]) |
			(one[30] ^ hash[2]) |
			((one[31] & 0xF0) ^ (hash[3] & 0xF0)));
		
	}

	private boolean isNull(int index){
		return isNullB(index);
	}
	public int mapIndex(byte[] hash){
		
		int index = 
			((hash[29] & 0x0F)  << 16) |
			((hash[30] & 0xFF) <<  8) |
			((hash[31] & 0xFF) <<  0);
	    	
		return (int) (index*indexScale);
	}
	
	private void validate(){
		//Test that expectedItems is good mask;
		int buf = expectedAddresses;
		int left = (byte)(expectedAddresses >> 16)&0xFF;
		int middle = (byte)(expectedAddresses >> 8)&0xFF;
		int right = (byte)(expectedAddresses >> 0)&0xFF;
		
		if(buf==0)
			throw new RuntimeException("expected items cannot equal zero.");
		if((right!=255))
			throw new RuntimeException("byteRight must equal 255 instead of: "+right);
		
		if((middle!=255))
			throw new RuntimeException("byteMiddle should equal 255 instead of: "+middle);
		
		int initialLeft = left;
		for(int i=0;i<9;i++){
			if(i==9)
				throw new RuntimeException("byteRight is invalid "+ initialLeft);
			if((left&0x1)==0){
				if(left!=0)
					throw new RuntimeException("byteRight is invalid: "+  initialLeft);
			}
			left = left>>1;
		}
		//confirmed that expected items is a good mask.
		
		
		//check that mapIndex implementation matches expectedAddresses.
		byte[] testHash = new byte[32];
		testHash[29] = (byte) 0xFF;
		testHash[30] = (byte) 0xFF;
		testHash[31] = (byte) 0xFF;
		
		double tempIndexScale = indexScale;
		indexScale = 1;
		int result = mapIndex(testHash);
		if(result!=expectedAddresses)
			throw new RuntimeException(
				"mapIndex implementation does not match expectedItems\n"
				+"expectedAddresses :"+expectedAddresses
				+"\nresult: "+ result);
		
		indexScale = tempIndexScale;
		
	}
	
	private void processPutDistance(int index,int start){
		int distance = start-index;
		distance = distance > 0?distance:distance * -1;
		if(distance>=threshold && heldBack < 50000){
			heldBack+=heldBack;
		}
	}

	public boolean isValidSpot(int index){
		return (index % heldBack) != 0;
	}

	public void isNotCollisionAddress(int position){
		if(position>=expectedAddresses)
			throw new HashStoreException("\nAddress cannot be equalOrEqualTo"+
					Integer.toHexString(expectedAddresses) + " "+
					"\n" +expectedAddresses+
					".\nThis is the collision address.");
	}
	public static class HashStoreException extends RuntimeException{

		public HashStoreException(String string) {
			super(string);
		}

		/**
		 * 
		 */
		private static final long serialVersionUID = -7845105683973493923L;
		
	}
	static interface PositionCalleeBase{
		public boolean validIndex(int index,int start,byte[] hash);
	}
	static interface PositionCalleeWithResult<T>
		extends PositionCalleeBase{
		public T getResult();
	}
	
	static interface PositionCalleeInit<T>
		extends PositionCalleeWithResult<T>{
		public void init();
	}
	
	static interface PositionCalleeInitArg<T,A>
		extends PositionCalleeWithResult<T>{
		public void init(A arg);
	}
	
	static void println(Object ob){
		System.out.println(ob);
	}
	
}