package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.Serializable;
import java.util.Map;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;
	private String msg;
	private String portNumber;
	private int sequenceNumber;
	private MessageType messageType;
	private String messageId;
	private Map<String, Integer> causalOrderingMap;

	public Map<String, Integer> getCausalOrderingMap() {
		return causalOrderingMap;
	}

	public void setCausalOrderingMap(Map<String, Integer> causalOrderingMap) {
		this.causalOrderingMap = causalOrderingMap;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}

	public MessageType getMessageType() {
		return messageType;
	}

	public void setMessageType(MessageType messageType) {
		this.messageType = messageType;
	}

	public String getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(String portNumber) {
		this.portNumber = portNumber;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
}
