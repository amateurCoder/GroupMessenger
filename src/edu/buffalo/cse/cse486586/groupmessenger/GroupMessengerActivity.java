package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Queue;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 * 
 */
public class GroupMessengerActivity extends Activity {
	static final String TAG = GroupMessengerActivity.class.getSimpleName();

	static final int SERVER_PORT = 10000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_group_messenger);

		/*
		 * TODO: Use the TextView to display your messages. Though there is no
		 * grading component on how you display the messages, if you implement
		 * it, it'll make your debugging easier.
		 */
		TextView tv = (TextView) findViewById(R.id.textView1);
		tv.setMovementMethod(new ScrollingMovementMethod());

		/*
		 * Registers OnPTestClickListener for "button1" in the layout, which is
		 * the "PTest" button. OnPTestClickListener demonstrates how to access a
		 * ContentProvider.
		 */
		findViewById(R.id.button1).setOnClickListener(
				new OnPTestClickListener(tv, getContentResolver()));

		/*
		 * TODO: You need to register and implement an OnClickListener for the
		 * "Send" button. In your implementation you need to get the message
		 * from the input box (EditText) and send it to other AVDs in a
		 * total-causal order.
		 */
		Log.d(TAG, "Enter onCreate");

		// Resources.setCount(0);
		// Resources.setMessageCount(0);

		// Initialization for Causal ordering
		/*
		 * for (int i = 0; i < Resources.remotePorts.length; i++) {
		 * Resources.COMap.put(Resources.remotePorts[i], 0); Log.d(TAG,
		 * "CO Initialization" +
		 * String.valueOf(Integer.parseInt(Resources.remotePorts[i])/2)); }
		 */

		TelephonyManager tel = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(
				tel.getLine1Number().length() - 4);

		final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

		Resources.myPort = myPort;

		try {
			ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
			new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
					serverSocket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		findViewById(R.id.button4).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// Create Telephony service and Server/Client Async threads
				Log.d(TAG, "Button clicked");

				final EditText editText = (EditText) findViewById(R.id.editText1);
				String msg = editText.getText().toString();
				editText.setText("");

				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
						"", msg, myPort, MessageType.NEW_MESSAGE.toString());

			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
		return true;
	}

	private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

		private static final String VALUE_FIELD = "value";
		private static final String KEY_FIELD = "key";

		@Override
		protected Void doInBackground(ServerSocket... sockets) {
			ServerSocket serverSocket = sockets[0];

			try {
				readMessage(serverSocket);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		private void readMessage(ServerSocket serverSocket) throws IOException,
				ClassNotFoundException {
			Message message = null;
			while (true) {
				Socket socket = serverSocket.accept();
				ObjectInputStream objectInputStream = new ObjectInputStream(
						socket.getInputStream());
				message = (Message) objectInputStream.readObject();
				objectInputStream.close();

				if (null != message) {
					if (message.getMessageType() == MessageType.NEW_MESSAGE) {
						// Incrementing the count
						Log.d(TAG, "Message Received:" + message.getMsg()
								+ ";Message Id:" + message.getMessageId()
								+ ";Message Type:" + message.getMessageType()
								+ ";From:" + message.getPortNumber()
								+ ";My port:" + Resources.myPort);
						Resources.count++;
						Resources.proposedSequence = Resources.count;

						// Send sequenceNumber as Max(PROPOSED,AGREED))
						Resources.proposedSequence = max(
								Resources.proposedSequence,
								Resources.agreedSequence) + 1;

						// Save message in a map: <Message_id,msg>
						storeMessageId(message.getMessageId(), message.getMsg());

						new ClientTask().executeOnExecutor(
								AsyncTask.SERIAL_EXECUTOR,
								message.getMessageId(), message.getMsg(),
								Resources.myPort,
								MessageType.PROPOSED_SEQUENCE.toString(),
								Integer.toString(Resources.proposedSequence),
								message.getPortNumber());

						/*
						 * Save message in a priority queue, ordered by priority
						 * (smallest (proposed) sequence number) and Mark
						 * message as undeliverable
						 */
						/*
						 * Create a priority queue with objects containing
						 * sequence number, sender's avd number and message
						 */
						QueueObject queueObject = new QueueObject(
								message.getMessageId(),
								Resources.proposedSequence,
								message.getPortNumber(), message.getMsg());
						saveInPriorityQueue(queueObject);

						// Causal ordering
						/*
						 * saveMessageInHoldBackQueue(message); if
						 * ((message.getCausalOrderingMap().get(
						 * message.getPortNumber()) == Resources.COMap
						 * .get(message.getPortNumber()) + 1) &&
						 * isCOMaintainedForOtherProcesses(message)) {
						 * Resources.isCausalOrdering = true; }
						 */

					} else if (message.getMessageType() == MessageType.PROPOSED_SEQUENCE) {
						/*
						 * Save the proposed sequence along with the sender port
						 * addresses Storing the message related information in
						 * the format: "<messageId,<portNumber,sequence>>"
						 */

						Log.d(TAG, "Message Received:" + message.getMsg()
								+ ";Message Id:" + message.getMessageId()
								+ ";Message Type:" + message.getMessageType()
								+ ";From:" + message.getPortNumber()
								+ ";My port:" + Resources.myPort + ";Sequence:" + message.getSequenceNumber());
						storeSequenceNumber(message.getMessageId(),
								message.getPortNumber(),
								message.getSequenceNumber());

						/*
						 * From the messageMap, find maximum of the sequence
						 * numbers sent by each avds for that message and send
						 * that as AGREED_SEQUENCE along with message(again)
						 */
						// if response from all the processes have been received

						Log.d(TAG,
								"Message map size for message id:"
										+ message.getMessageId()
										+ " is "
										+ Resources.messageMap.get(
												message.getMessageId()).size());
						Log.d(TAG, "The length of remote ports array is:"
								+ Resources.remotePorts.length);

						if (haveAllProcessesResponded(message.getMessageId())) {
							Message maxSequenceMessage = getMaxSequenceMessage(message
									.getMessageId());
							
							Resources.agreedSequence = max(Resources.proposedSequence, maxSequenceMessage.getSequenceNumber());
							/* SEND AGREED SEQUENCE NUMBER Message id); */
							new ClientTask().executeOnExecutor(
									AsyncTask.SERIAL_EXECUTOR, message
											.getMessageId(),
									Resources.messageIdMap.get(message
											.getMessageId()), Resources.myPort,
									MessageType.AGREED_SEQUENCE.toString(),
									Integer.toString(Resources.agreedSequence));
						}

					} else if (message.getMessageType() == MessageType.AGREED_SEQUENCE) {
						Log.d(TAG, "Message Received:" + message.getMsg()
								+ ";Message Id:" + message.getMessageId()
								+ ";Message Type:" + message.getMessageType()
								+ ";From:" + message.getPortNumber()
								+ ";My port:" + Resources.myPort);
						/*
						 * Setting agreed sequence as maximum of Agreed sequence
						 * and received proposed sequence
						 */
						Resources.agreedSequence = max(
								Resources.agreedSequence,
								message.getSequenceNumber());

						/*
						 * if Resources.agreedSequence is not equal to
						 * Resources.proposedSequence
						 */

						Log.d(TAG, "PROPOSED SEQUENCE:"
								+ Resources.proposedSequence);
						Log.d(TAG, "AGREED SEQUENCE:"
								+ Resources.agreedSequence);

						Queue<QueueObject> modifiedPriorityQueue = new PriorityQueue<QueueObject>();
						Log.d(TAG, "PRIORITY QUEUE SIZE:"
								+ Resources.priorityQueue.size());
						while (Resources.priorityQueue.size() != 0) {
							QueueObject obj = Resources.priorityQueue.poll();
							if (obj.getMessageId().equals(
									message.getMessageId())) {
								obj.setProposedsequenceNumber(Resources.agreedSequence);
								Resources.messageDeliveryMark.put(
										message.getMessageId(), true);
							}
							modifiedPriorityQueue.add(obj);
						}

						Log.d(TAG, "MODIFIED QUEUE SIZE:"
								+ modifiedPriorityQueue.size());

						Resources.priorityQueue.clear();
						Resources.priorityQueue = modifiedPriorityQueue;
						Log.d(TAG, "PRIORITY QUEUE SIZE AFTER MODIFICATION:"
								+ Resources.priorityQueue.size());

						QueueObject queueObject = Resources.priorityQueue
								.peek();

						if (queueObject != null
								&& Resources.messageDeliveryMark
										.get(queueObject.getMessageId()) != null) {
							// Transfer to the tail of the delivery queue
							Resources.deliveryQueue.add(Resources.priorityQueue
									.poll());

							// }
						}
						// if (Resources.isCausalOrdering) {
						// Total ordering operation
						Log.d(TAG, "DELIVERY QUEUE SIZE:"
								+ Resources.deliveryQueue.size());
						while (Resources.deliveryQueue.size() > 0) {
							QueueObject deliveredObject = Resources.deliveryQueue
									.poll();
							if (null != deliveredObject) {
								publishProgress(deliveredObject.getMsg());
							}
						}

						/*
						 * //Causal ordering operation int COCount
						 * =Resources.COMap.get(message.getPortNumber());
						 * COCount++;
						 * Resources.COMap.put(message.getPortNumber(),
						 * COCount);
						 */

						// Save it into content provider using publishProgress

						// }
					}
				}
				socket.close();
			}

		}

		private boolean haveAllProcessesResponded(String messageId) {
			// Is there an entry for all the ports in the messageMap for a
			// particular message.
			if (Resources.messageMap.containsKey(messageId)) {
				if (Resources.messageMap.get(messageId).size() == Resources.remotePorts.length) {
					return true;
				}
			}
			return false;
		}

		private boolean isCOMaintainedForOtherProcesses(Message message) {
			// Checking if the causal ordering is maintained for other processes
			// as well
			Map<String, Integer> tempMap = message.getCausalOrderingMap();
			for (String key : tempMap.keySet()) {
				// If CO is not maintained for any of the processes
				if (tempMap.get(key) > Resources.COMap.get(key)) {
					return false;
				}
			}
			return true;
		}

		private void saveMessageInHoldBackQueue(Message message) {
			Resources.holdBackQueue.add(message);
		}

		private Message getMaxSequenceMessage(String messageId) {
			// TODO Return the Maximum sequence number from the messageMap
			Map<String, Integer> value = Resources.messageMap.get(messageId);
			Message message = getMaximumSequence(value);
			if(message!=null){
				Resources.messageMap.remove(messageId);
			}
			return message;
		}

		private Message getMaximumSequence(Map<String, Integer> value) {
			int maxSequenceNumber = 0;
			Message message = new Message();
			for (String portNumber : value.keySet()) {
				if (value.get(portNumber) > maxSequenceNumber) {
					maxSequenceNumber = value.get(portNumber);
					message.setSequenceNumber(maxSequenceNumber);
					message.setPortNumber(portNumber.toString());
				}
			}

			// Getting the lowest process id in case the proposed are same
			for (String portNumber : value.keySet()) {
				if (message.getSequenceNumber() == value.get(portNumber)
						&& !(message.getPortNumber().equals(portNumber))) {
					if (Integer.parseInt(portNumber) < Integer.parseInt(message
							.getPortNumber())) {
						message.setPortNumber(portNumber);
						message.setSequenceNumber(value.get(portNumber));
					}
				}
			}

			// remove chosen message from the map
			/*Iterator<Entry<String, Integer>> it = value.entrySet()
					.iterator();
			while (it.hasNext()) {
				if (message.getPortNumber().equals(it.next().getKey())) {
					it.remove();
				}
			}
*/
			return message;
		}

		private void saveInPriorityQueue(QueueObject queueObject) {
			// Log.d(TAG, "SAVING");
			Resources.priorityQueue.add(queueObject);
			// Log.d(TAG, "LENGTH:" + Resources.priorityQueue.size() +
			// "Message:"
			// + Resources.priorityQueue.element().getMsg());
		}

		private void storeMessageId(String messageId, String msg) {
			if (!Resources.messageIdMap.containsKey(messageId)) {
				Resources.messageIdMap.put(messageId, msg);
			}
		}

		private int max(int proposedSequence, int agreedSequence) {
			if (proposedSequence >= agreedSequence) {
				return proposedSequence;
			}
			return agreedSequence;
		}

		private void storeSequenceNumber(String messageId, String portNumber,
				int sequenceNumber) {
			Map<String, Integer> messageValue;
			// If some information regarding this message id is already present
			if (Resources.messageMap.containsKey(messageId)) {
				messageValue = Resources.messageMap.get(messageId);
				// Storing the proposed sequence for the same message but
				// different avd
				if (!messageValue.containsKey(portNumber)) {
					messageValue.put(portNumber, sequenceNumber);
					Resources.messageMap.put(messageId, messageValue);
				}
			} else {
				messageValue = new HashMap<String, Integer>();
				messageValue.put(portNumber, sequenceNumber);
				Resources.messageMap.put(messageId, messageValue);
			}

		}

		protected void onProgressUpdate(String... strings) {

			String msg = strings[0].trim();
			TextView remoteTextView = (TextView) findViewById(R.id.textView1);

			// Send to content provider
			ContentValues entry = new ContentValues();
			entry.put(KEY_FIELD, Resources.providerCount++);
			entry.put(VALUE_FIELD, msg);
			Log.d(TAG, "Message:" + (Resources.providerCount - 1) + ":" + msg);
			remoteTextView.append(Resources.providerCount + ":" + msg + "\t\n");
			getContentResolver()
					.insert(buildUri("content",
							"edu.buffalo.cse.cse486586.groupmessenger.provider"),
							entry);
		}

	}

	private Uri buildUri(String scheme, String authority) {
		Uri.Builder uriBuilder = new Uri.Builder();
		uriBuilder.authority(authority);
		uriBuilder.scheme(scheme);
		return uriBuilder.build();
	}

	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {

			Message message = new Message();
			boolean isProposed = false;
			String destPort = null;
			String msgId = msgs[0];
			String msgToSend = msgs[1];
			String myPort = msgs[2];

			if (msgs[3] == MessageType.NEW_MESSAGE.toString()) {
				message.setMessageType(MessageType.NEW_MESSAGE);
				Resources.messageCount++;
				msgId = myPort + Resources.messageCount;

				// Updating Causal ordering map (COMap)
				/*
				 * int counterCO = Resources.COMap.get(myPort); counterCO++;
				 * Resources.COMap.put(myPort, counterCO);
				 * 
				 * message.setCausalOrderingMap(Resources.COMap);
				 */

			} else if (msgs[3] == MessageType.PROPOSED_SEQUENCE.toString()) {
				message.setMessageType(MessageType.PROPOSED_SEQUENCE);

				// Modifying port list such that message will be sent only to
				// that avd who sent original message.
				destPort = msgs[5];
				isProposed = true;

				// Send max of proposed and agreed sequence - implemented at
				// Server side
				message.setSequenceNumber(Integer.parseInt(msgs[4]));

			} else if (msgs[3] == MessageType.AGREED_SEQUENCE.toString()) {
				// Sends the message again with the AGREED sequence number
				message.setMessageType(MessageType.AGREED_SEQUENCE);
				message.setSequenceNumber(Integer.parseInt(msgs[4]));
			}

			Socket socket = null;
			message.setMsg(msgToSend);
			message.setMessageId(msgId);
			message.setPortNumber(myPort);

			ObjectOutputStream objectOutputStream;
			if (isProposed) {
				try {
					socket = new Socket(InetAddress.getByAddress(new byte[] {
							10, 0, 2, 2 }), Integer.parseInt(destPort));

					Log.d(TAG, "Message Sent:" + message.getMsg()
							+ ";Message Id:" + message.getMessageId()
							+ ";Message Type:" + message.getMessageType()
							+ ";My port:" + Resources.myPort + ";To port:"
							+ destPort + ";Sequence:" + message.getSequenceNumber());

					objectOutputStream = new ObjectOutputStream(
							socket.getOutputStream());
					objectOutputStream.writeObject(message);
					objectOutputStream.close();
					socket.close();
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else {
				for (int i = 0; i < Resources.remotePorts.length; i++) {

					try {
						socket = new Socket(
								InetAddress.getByAddress(new byte[] { 10, 0, 2,
										2 }),
								Integer.parseInt(Resources.remotePorts[i]));

						Log.d(TAG, "Message Sent:" + message.getMsg()
								+ ";Message Id:" + message.getMessageId()
								+ ";Message Type:" + message.getMessageType()
								+ ";My port:" + Resources.myPort + ";To port:"
								+ Resources.remotePorts[i] + ";Sequence:" + message.getSequenceNumber());

						objectOutputStream = new ObjectOutputStream(
								socket.getOutputStream());
						objectOutputStream.writeObject(message);
						objectOutputStream.close();
						socket.close();
					} catch (NumberFormatException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (UnknownHostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return null;
		}

	}
}