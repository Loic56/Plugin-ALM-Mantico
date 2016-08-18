package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.util.Date;
import java.util.Map;

import com.hp.qc.synchronizer.adapters.core.AdapterLogger;
import com.hp.qc.synchronizer.adapters.spi.RecordManager;

public abstract class MantisRecordManager implements RecordManager {

	protected AdapterLogger logger;
	protected MantisIssueHelper issueHelper;

	public MantisRecordManager(MantisIssueHelper issueHelper, AdapterLogger logger) {
		this.logger = logger;
		this.issueHelper = issueHelper;
	}

	public AdapterLogger getLogger() {
		return this.logger;
	}

	public String checkEndpointParams(Map<String, String> params) {
		return null;
	}

	public Date getEndpointTime() {
		return new Date();
	}

	public void setEndpointParams(Map<String, String> params) {
	}

	public MantisIssueHelper getIssueHelper() {
		return issueHelper;
	}
}
