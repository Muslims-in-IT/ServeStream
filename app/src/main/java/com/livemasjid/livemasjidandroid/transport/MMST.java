package com.livemasjid.livemasjidandroid.transport;

import com.livemasjid.livemasjidandroid.bean.UriBean;

public class MMST extends MMS {

	private static final String PROTOCOL = "mmst";
	
	public MMST() {
		super();
	}
	
	public MMST(UriBean uri) {
		super(uri);
	}
	
	public static String getProtocolName() {
		return PROTOCOL;
	}	
	
	protected String getPrivateProtocolName() {
		return PROTOCOL;
	}
}
