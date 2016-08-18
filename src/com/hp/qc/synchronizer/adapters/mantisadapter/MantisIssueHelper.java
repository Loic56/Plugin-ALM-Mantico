package com.hp.qc.synchronizer.adapters.mantisadapter;

public class MantisIssueHelper {

	private final MantisSoapGossipClient client;

	public MantisIssueHelper(MantisSoapGossipClient client) {
		this.client = client;
	}

	public MantisSoapGossipClient getClient() {
		return client;
	}

}
