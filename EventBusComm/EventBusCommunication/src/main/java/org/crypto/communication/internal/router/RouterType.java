package org.crypto.communication.internal.router;

public enum RouterType {
	ROUND_ROBIN(1),
	STICKEY_SESSION(2)
	;
	private int type;
	RouterType(int type) {
		// TODO Auto-generated constructor stub
		this.type = type;
	}
	
	public int getType() {
		return this.type;
	}
}
