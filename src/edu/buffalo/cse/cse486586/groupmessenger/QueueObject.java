package edu.buffalo.cse.cse486586.groupmessenger;

public class QueueObject implements Comparable<QueueObject>{
	private String messageId;
	private int proposedsequenceNumber;
	private String portNumber;
	private String msg;

	public QueueObject(String messageId, int proposedSequenceNumber, String portNumber, String msg) {
		this.messageId = messageId;
		this.proposedsequenceNumber = proposedSequenceNumber;
		this.portNumber = portNumber;
		this.msg = msg;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	
	public int getProposedsequenceNumber() {
		return proposedsequenceNumber;
	}

	public void setProposedsequenceNumber(int proposedsequenceNumber) {
		this.proposedsequenceNumber = proposedsequenceNumber;
	}

	public String getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(String portNumber) {
		this.portNumber = portNumber;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	@Override
	public int compareTo(QueueObject anotherObject) {
		if(this.proposedsequenceNumber==anotherObject.proposedsequenceNumber){
			return this.portNumber.compareTo(anotherObject.portNumber);
		}
		return this.proposedsequenceNumber-anotherObject.proposedsequenceNumber;
	}

}
