package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

import com.hp.qc.synchronizer.adapters.spi.DefectTypeRecord;

import biz.futureware.mantis.rpc.soap.client.IssueData;

public class MantisDefectRecord extends MantisRecord implements DefectTypeRecord {
    
	private static final String fieldNames[] = { 
			MantisNodes.HANDLER, 
			MantisNodes.NOTE, 
			MantisNodes.ID,
			MantisNodes.DESCRIPTION, 
			MantisNodes.REPORTER, 
			MantisNodes.SUBMITEDDATE, 
			MantisNodes.LAST_UPDATED,
			MantisNodes.FORMATTED_LAST_UPDATE,
			MantisNodes.VERSION,
			MantisNodes.TARGET_VERSION,
			MantisNodes.PLATEFORM,
			MantisNodes.PRIORITY, 
			MantisNodes.CATEGORY, 
			MantisNodes.REPRODUCIBILITY, 
			MantisNodes.SEVERITY,
			MantisNodes.STATUS, 
			MantisNodes.RESOLUTION,
			MantisNodes.SUMMARY,
			MantisNodes.OS, 
			MantisNodes.OS_VERSION, 
			MantisNodes.FIXED_IN_VERSION,
			MantisNodes.ADDITIONAL_INFOS,
			MantisNodes.STEPS_TO_REPRODUCE,
			MantisNodes.ATTACHMENT};
 
    /**
     * Create empty interface object which represent a non existing object
     */
	private MantisDefectRecord() {
		ghostObject = true;
	}

    /**
     * @param fields
     * @param defectsDocument
     */
    public MantisDefectRecord(Map<String, Object> fields, MantisIssueHelper helper) {  
    	this.client = helper.getClient();
    	createNewElement(fields, helper);
    }
    
    /**
     * Fill in data of record from issue from Mantis
     */
    public MantisDefectRecord(IssueData issue, MantisDefectManager manager) {
        super(issue, manager);
    }
    
	private void createNewElement(Map<String, Object> fields, MantisIssueHelper helper) {
		boolean changed = false;
		for (Map.Entry<String, Object> field : fields.entrySet()) {
			String field_name = field.getKey();
			Object field_value = field.getValue();
			// Champs custom
			if (field_name.contains("[custom]")) {
				this.recordCustomFields.put(field_name.substring(9), field_value);
				changed = true;
			} else {
			// Champs natifs
				if (field_value != null) {
					if (field_value.getClass() == Date.class) {
						this.recordFields.put(field_name, field_value);
					} else {
						this.recordFields.put(field_name, field_value.toString());
					}
					changed = true;
				}
			}
		}
		if (changed) {
			BigInteger id = insertMantisTicket(helper, fields);
			this.recordFields.put(MantisNodes.ID, id.toString());
			this.recordFields.put(MantisNodes.FORMATTED_LAST_UPDATE, helper.getClient().getProjectIssueLastUpadtedByID(id));
		}
	}
     
	/**
	 * Create ghost record : empty / not linked record which will fail on any
	 * action this to be compatible with definition of interface object
	 */
	public static MantisRecord getGhostRecord() {
		if (ghostRecord == null) {
			ghostRecord = new MantisDefectRecord();
		}
		return ghostRecord;
	}

	@Override
	protected String[] getEntityFieldNames() {
		return fieldNames;
	}
}
