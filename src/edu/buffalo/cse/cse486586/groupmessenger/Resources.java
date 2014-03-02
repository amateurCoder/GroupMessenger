package edu.buffalo.cse.cse486586.groupmessenger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

public class Resources {

	static final String REMOTE_PORT0 = "11108";
	static final String REMOTE_PORT1 = "11112";
	static final String REMOTE_PORT2 = "11116";
	static final String REMOTE_PORT3 = "11120";
	static final String REMOTE_PORT4 = "11124";
	static final String REMOTE_PORT5 = "11128";

	static final String[] remotePorts = { REMOTE_PORT0, REMOTE_PORT1,
			REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4 /* ,REMOTE_PORT5 */};
	static int count = 0;
	static String myPort;
	static Map<String, Map<String, Integer>> messageMap = new HashMap<String, Map<String, Integer>>();
	static Map<String, String> messageIdMap = new HashMap<String, String>();
	static Map<String, Boolean> messageDeliveryMark = new HashMap<String, Boolean>();
	static Queue<QueueObject> priorityQueue = new PriorityQueue<QueueObject>();
	static Queue<QueueObject> deliveryQueue = new LinkedList<QueueObject>();
	
	static int messageCount = 0;
	static int proposedSequence = 0;
	static int agreedSequence = 0;

	static int providerCount = 0;

	static int COUNTER = 0;

	static Map<String, Integer> COMap = new HashMap<String, Integer>();
	static Queue<Message> holdBackQueue = new LinkedList<Message>();
	static boolean isCausalOrdering;

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
