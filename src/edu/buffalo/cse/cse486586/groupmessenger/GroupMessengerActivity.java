package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
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
//	static final String REMOTE_PORT4 = "11124";
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

		TelephonyManager tel = (TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE);
		String portStr = tel.getLine1Number().substring(
				tel.getLine1Number().length() - 4);

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
				final EditText editText = (EditText) findViewById(R.id.editText1);
				String msg = editText.getText().toString() + "\n";
				editText.setText("");

				new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
						msg, myPort);

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

				InputStreamReader inputStreamReader = new InputStreamReader(
						socket.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(
						inputStreamReader);
				String msg = bufferedReader.readLine();

				Log.d("", "MESSAGE Received:" + msg);
				publishProgress(msg);

				bufferedReader.close();
				inputStreamReader.close();
				socket.close();
			}
		}

		protected void onProgressUpdate(String... strings) {

			String msg = strings[0].trim();
			TextView remoteTextView = (TextView) findViewById(R.id.textView1);
			remoteTextView.append(msg + "\t\n");

			// Check from which avd the message came
			// Send to content provider
		}

	}

	private class ClientTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... msgs) {
			// String[] remotePorts = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2,
			// REMOTE_PORT3, REMOTE_PORT4};
			String[] remotePorts = { REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT5 };
			try {
				Socket socket = null;
				OutputStreamWriter outputStreamWriter = null;
				String msgToSend = msgs[0];
				for (int i = 0; i < remotePorts.length; i++) {

					socket = new Socket(InetAddress.getByAddress(new byte[] {
							10, 0, 2, 2 }), Integer.parseInt(remotePorts[i]));
					outputStreamWriter = new OutputStreamWriter(
							socket.getOutputStream());
					outputStreamWriter.write(msgToSend);
					outputStreamWriter.close();
				}
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
			return null;
		}

	}

}
