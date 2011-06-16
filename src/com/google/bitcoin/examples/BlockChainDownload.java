package com.google.bitcoin.examples;

import static com.google.bitcoin.core.Utils.*;
import static com.google.bitcoin.core.Base58.*;

import java.io.File;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.BlockStore;
import com.google.bitcoin.core.BoundedOverheadBlockStore;
import com.google.bitcoin.core.DiskBlockStore;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.IrcDiscovery;
import com.google.bitcoin.core.MemoryBlockStore;
import com.google.bitcoin.core.NetworkConnection;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.SeedPeers;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.BalanceType;

public class BlockChainDownload {

	public static void main(String[] args) throws Exception{
		connectToTestNetwork();
		if(true)return;
		NetworkParameters parameters = NetworkParameters.prodNet();
		
		Wallet wallet = new Wallet(parameters);
		BlockStore blockStore = new MemoryBlockStore(parameters);

		NetworkConnection connection = getConnection(parameters);
		BlockChain chain = new BlockChain(parameters,wallet,blockStore);
		Peer peer = new Peer(parameters,connection,chain);
		peer.start();
		
		CountDownLatch progress = peer.startBlockChainDownload();
        long max = progress.getCount();  // Racy but no big deal.
        if (max > 0) {
            System.out.println("Downloading block chain. " + (max > 1000 ? "This may take a while." : ""));
            long current = max;
            while (current > 0) {
                double pct = 100.0 - (100.0 * (current / (double) max));
                System.out.println(String.format("Chain download %d%% done", (int) pct));
                progress.await(1, TimeUnit.SECONDS);
                current = progress.getCount();
            }
        }
	}
	
	public static void reload_blockChain() throws Exception{
		NetworkParameters parameters = NetworkParameters.testNet();
		Wallet wallet = new Wallet(parameters);
		BlockStore blockStore = memoryStore(parameters);
		NetworkConnection connection = getConnection(parameters);
		
		BlockChain chain = new BlockChain(parameters,wallet,blockStore);
		
		
	}
	static File walletFile = new File("walletfile");
	static File diskFile = new File("diskfile");
	
	public static void connectToTestNetwork() throws Exception{
		NetworkParameters n = NetworkParameters.testNet();
		NetworkConnection conn = testConnection(n);
		Wallet wallet =Wallet.loadFromFile(walletFile);
		ECKey key = wallet.keychain.get(0);
        println(key.toAddress(n));
		
		BoundedOverheadBlockStore blockStore = new BoundedOverheadBlockStore(n,diskFile);
		
		BlockChain chain = new BlockChain(n,wallet,blockStore);
		Peer peer = new Peer(n,conn,chain);
		peer.start();
		
		CountDownLatch progress = peer.startBlockChainDownload();
        long max = progress.getCount(); 
        int cnt = 0;
        if (max > 0) {
            System.out.println("Downloading block chain. " + (max > 1000 ? "This may take a while." : ""));
            long current = max;
            while (current > 0) {
                double pct = 100.0 - (100.0 * (current / (double) max));
                System.out.println(String.format("Chain download %d%% done", (int) pct));
                progress.await(1, TimeUnit.SECONDS);
                current = progress.getCount();
                if(cnt++>50)break;
            }
        }
        cnt=0;
        while(cnt++<100){
        	Thread.sleep(1000);
        	println(wallet.getBalance());
        	println(wallet.getBalance(BalanceType.ESTIMATED));
        }
        println("saving");
        
        wallet.saveToFile(walletFile);
	}
	
	public static void playingWithKeys() throws Exception{
		ECKey ecKey = new ECKey();
		byte[] hash = doubleDigest("tshaoeutsahoesuthaoestu".getBytes());
		byte[] sign = ecKey.sign(hash);
		println(ecKey.verify(hash, sign));
		
	}
	
	public static MemoryBlockStore memoryStore(NetworkParameters parameters){
		return new MemoryBlockStore(parameters);
	}
	public static NetworkConnection getConnection(NetworkParameters n){
		SeedPeers seedPeers = new SeedPeers(n);
		NetworkConnection result = null;
		while(true){
			try{
				result = new NetworkConnection(seedPeers.getPeer().getAddress(),n,0,10000);
			}catch(Exception e){
				println(e);
			}
			if(result!=null)break;
		}
		return result;
		
	}
	
	public static NetworkConnection testConnection(NetworkParameters n){
		IrcDiscovery irc = new IrcDiscovery("#bitcoinTEST");
		NetworkConnection result = null;
		InetSocketAddress[] addrs;
		try{
			addrs = irc.getPeers();
		}catch(Exception e){
			println(e);
			return null;
		}
		println(addrs.length);
		int i=0;
		while(true){
			try{
				result = new NetworkConnection(addrs[++i].getAddress(),n,0,10000);
			}catch(Exception e){
				println(e);
			}
			if(result!=null)break;
		}
		return result;
		
		
	}
	public static void println(Object ob){System.out.println(ob);}
}
