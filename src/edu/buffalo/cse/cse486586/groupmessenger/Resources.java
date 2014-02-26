package edu.buffalo.cse.cse486586.groupmessenger;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class Resources {
	static int count;
	static String myPort;
	static Map<String, Map<String,Integer>> messageMap = new HashMap<String, Map<String,Integer>>();
	static Map<String, String> messageIdMap = new HashMap<String, String>();
	static Queue<QueueObject> priorityQueue = new PriorityQueue<QueueObject>();
	static int messageCount=0;
	static int proposedSequence=0;
	static int agreedSequence=0;
	
	static int providerCount = 0;

	public static int getMessageCount() {
		return messageCount;
	}

	public static void setMessageCount(int messageCount) {
		Resources.messageCount = messageCount;
	}

	public static String getMyPort() {
		return myPort;
	}

	public static void setMyPort(String myPort) {
		Resources.myPort = myPort;
	}

	public int getCount() {
		return count;
	}

	public static void setCount(int count) {
		Resources.count = count;
	}
}
