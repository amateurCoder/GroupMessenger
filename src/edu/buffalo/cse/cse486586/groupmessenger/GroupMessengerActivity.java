package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

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
	static final String REMOTE_PORT0 = "11108";
	static final String REMOTE_PORT1 = "11112";
	static final String REMOTE_PORT2 = "11116";
	static final String REMOTE_PORT3 = "11120";
	static final String REMOTE_PORT4 = "11124";
	static final String REMOTE_PORT5 = "11128";

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
//		Log.d(TAG, "Enter onCreate");

		Resources.setCount(0);
		Resources.setMessageCount(0);

		TelephonyManager tel = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(
				tel.getLine1Number().length() - 4);

		Resources.myPort = portStr;

		final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

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
				// TODO Auto-generated method stub
				// Create Telephony service and Server/Client Async threads
//				Log.d(TAG, "Button clicked");
				final EditText editText = (EditText) findViewById(R.id.editText1);
				String msg = editText.getText().toString();
				editText.setText("");

				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
						msg, myPort, MessageType.NEW_MESSAGE.toString());

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
			}

			return null;
		}

		private void readMessage(ServerSocket serverSocket) throws IOException {
			while (true) {
				Socket socket = serverSocket.accept();
				ObjectInputStream objectInputStream = new ObjectInputStream(
						socket.getInputStream());
				
				Message message = null;
				try {
					message = (Message) objectInputStream.readObject();
					objectInputStream.close();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (null != message) {
					if (message.getMessageType() == MessageType.NEW_MESSAGE) {
						// Incrementing the count
//						Log.d(TAG, "Message Received:" + message.getMsg());
						Resources.count++;
						Resources.proposedSequence = Resources.count;
						
						//Send sequenceNumber as Max(PROPOSED,AGREED))
						Resources.proposedSequence = max(Resources.proposedSequence, Resources.agreedSequence)+1;

						//Save message in a map: <Message_id,msg>
						storeMessageId(message.getMessageId(), message.getMsg());
						
						new ClientTask().executeOnExecutor(
								AsyncTask.SERIAL_EXECUTOR, message.getMsg(), Resources.myPort,
								MessageType.PROPOSED_SEQUENCE.toString(), Integer.toString(Resources.proposedSequence));
						
						//Save message in a priority queue, ordered by priority (smallest (proposed) sequence number) and Mark message as undeliverable
						//Create a priority queue with objects containing sequence number, sender's avd number and message
						QueueObject queueObject = new QueueObject(message.getMessageId(), Resources.proposedSequence, message.getPortNumber(), message.getMsg());
						saveInPriorityQueue(queueObject);
						
					} else if (message.getMessageType() == MessageType.PROPOSED_SEQUENCE) {
						/* Save the proposed sequence along with the sender port addresses
						 Storing the message related information in the format: "<messageId,<portNumber,sequence>>"*/
//						Log.d(TAG, "Message Received1:" + message.getMsg());
						storeSequnceNumber(message.getMessageId(),
								message.getPortNumber(),
								message.getSequenceNumber());
						
						/* From the messageMap, find maximum of the sequence numbers sent by each avds for that message and send that as AGREED_SEQUENCE along with message(again) */
						Message maxSequenceMessage = getMaxSequenceMessage(message.getMessageId());
						new ClientTask().executeOnExecutor(
								AsyncTask.SERIAL_EXECUTOR, Resources.messageIdMap.get(message.getMessageId()), Resources.myPort,
								MessageType.AGREED_SEQUENCE.toString(),Integer.toString(maxSequenceMessage.getSequenceNumber()));//,SEND AGREED SEQUENCE NUMBER, Message id);
					} else if (message.getMessageType() == MessageType.AGREED_SEQUENCE) {
//						Log.d(TAG, "Message Received2:" + message.getMsg());

						//Setting agreed sequence as maximum of Agreed sequence and received proposed sequence
						Resources.agreedSequence = max(Resources.agreedSequence, message.getSequenceNumber());

						//if Resources.agreedSequence is not equal Resources.proposedSequence
						if(Resources.agreedSequence!=Resources.proposedSequence){
							//Retrieve the object from the queue with the messageId and update the sequence number as agreedSequence.
							//Re-order the message in the priority queue
							for (QueueObject qObj : Resources.priorityQueue ){
								if(qObj.getMessageId().equals(message.getMessageId())){
									QueueObject newObject = new QueueObject(qObj.getMessageId(), Resources.agreedSequence, qObj.getPortNumber(), qObj.getMsg());
									Resources.priorityQueue.remove(qObj);
									Resources.priorityQueue.add(newObject);
									break;
								}
							}
						}
						
						QueueObject deliveredObject = Resources.priorityQueue.poll();
						//Save it into content provider using publishProgress
						publishProgress(deliveredObject.getMsg());
					}
				}
				socket.close();
			}
			
		}

		private Message getMaxSequenceMessage(String messageId) {
			// TODO Return the Maximum sequence number from the messageMap
			Map<String, Integer> value = Resources.messageMap.get(messageId);
			Message message = getMaximumSequence(value);
			return message;
		}

		private Message getMaximumSequence(Map<String, Integer> value) {
			int maxSequenceNumber=0;
			Message message = new Message();
			for (String portNumber:value.keySet()){
				if(value.get(portNumber) > maxSequenceNumber){
					maxSequenceNumber = value.get(portNumber);
					message.setSequenceNumber(maxSequenceNumber);
					message.setPortNumber(portNumber.toString());
				}
			}
			return message;
		}

		private void saveInPriorityQueue(QueueObject queueObject) {
			Resources.priorityQueue.add(queueObject);
		}

		private void storeMessageId(String messageId, String msg) {
			if(!Resources.messageIdMap.containsKey(messageId)){
				Resources.messageIdMap.put(messageId, msg);
			}
		}

		private int max(int proposedSequence, int agreedSequence) {
			if(proposedSequence>=agreedSequence){
				return proposedSequence;
			}
			return agreedSequence;
		}

		private void storeSequnceNumber(String messageId, String portNumber,
				int sequenceNumber) {
			Map<String, Integer> messageValue;
			// If some information regarding this message id is already present
			if (Resources.messageMap.containsKey(messageId)) {
				messageValue = Resources.messageMap.get(messageId);
				// Storing the proposed sequence for the same message but different avd
				if (!messageValue.containsKey(portNumber)) {
					messageValue.put(portNumber, sequenceNumber);
				}
				Resources.messageMap.put(messageId, messageValue);
			} else {
				messageValue = new HashMap<String, Integer>();
				messageValue.put(portNumber, sequenceNumber);
				Resources.messageMap.put(messageId, messageValue);
			}

		}

		protected void onProgressUpdate(String... strings) {

			String msg = strings[0].trim();
			TextView remoteTextView = (TextView) findViewById(R.id.textView1);
			remoteTextView.append(msg + "\t\n");

			// Send to content provider
			ContentValues entry = new ContentValues();
			entry.put(KEY_FIELD, Resources.providerCount++);
			entry.put(VALUE_FIELD, msg);
			Log.d(TAG, "Message:"+(Resources.providerCount-1)+":"+msg);
			getContentResolver().insert(buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger.provider"),entry);
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
			String[] remotePorts = { REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2,
					REMOTE_PORT3, REMOTE_PORT4 /*,REMOTE_PORT5 */};

			Message message = new Message();

			String msgToSend = msgs[0];
			String myPort = msgs[1];

			if (msgs[2] == MessageType.NEW_MESSAGE.toString()) {
				message.setMessageType(MessageType.NEW_MESSAGE);
				Resources.messageCount++;
			} else if (msgs[2] == MessageType.PROPOSED_SEQUENCE.toString()) {
				message.setMessageType(MessageType.PROPOSED_SEQUENCE);
				
				//Send max of proposed and agreed sequence - implemented at Server side
				message.setSequenceNumber(Integer.parseInt(msgs[3]));
			} else if (msgs[2] == MessageType.AGREED_SEQUENCE.toString()) {
				//Sends the message again with the AGREED sequence number
				message.setSequenceNumber(Integer.parseInt(msgs[3]));
			}

			try {
				Socket socket = null;
				message.setMsg(msgToSend);
				message.setMessageId(myPort + Resources.messageCount);
				message.setPortNumber(myPort);
				ObjectOutputStream objectOutputStream;
				
				for (int i = 0; i < remotePorts.length; i++) {

					socket = new Socket(InetAddress.getByAddress(new byte[] {
							10, 0, 2, 2 }), Integer.parseInt(remotePorts[i]));

					objectOutputStream = new ObjectOutputStream(
							socket.getOutputStream());
					objectOutputStream.writeObject(message);
					objectOutputStream.close();
					socket.close();
				}
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
			return null;
		}
	}

}
