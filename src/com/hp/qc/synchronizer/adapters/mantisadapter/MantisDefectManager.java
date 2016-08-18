package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.hp.qc.synchronizer.adapters.core.AdapterLogger;
import com.hp.qc.synchronizer.adapters.core.BaseRecordDataOrderedSet;
import com.hp.qc.synchronizer.adapters.exceptions.AdapterException;
import com.hp.qc.synchronizer.adapters.spi.DefectManager;
import com.hp.qc.synchronizer.adapters.spi.DefectTypeRecord;

import biz.futureware.mantis.rpc.soap.client.IssueData;

public class MantisDefectManager extends MantisRecordManager implements DefectManager {
    
    // Cache of mantis elements
    private Map<String, MantisDefectRecord> nodeCache = new HashMap<String, MantisDefectRecord>();
      

    public MantisDefectManager(MantisIssueHelper issueHelper, AdapterLogger logger) {
        super(issueHelper, logger);
    }
    
	// Pour chaque nouveau ticket à créer dans Mantico
	public DefectTypeRecord create(Map<String, Object> fields) {
		logger.info("Creating new defect");
		MantisDefectRecord defect = new MantisDefectRecord(fields, getIssueHelper());
		String id = defect.getID();
		addToCache(id, defect);
		logger.info("New defect created successfully with id " + id);
		return defect;
	}
    
	// La méthode appelée à chaque syncronisation 
	// Toutes les issue Mantico sont ajoutées à la list utilisée par le syncronizer 
	// 		> des issues ont été modifiées
	// 		> des nouvelles issue créées
	public void getRecordIDs(Date since, String filter, BaseRecordDataOrderedSet idList) throws AdapterException {
		MantisSoapGossipClient client = getIssueHelper().getClient();
		IssueData[] issues = null;
		issues = client.getProjectIssues();
		for (int i = 0; i < (issues.length); ++i) {
			String id = issues[i].getId().toString();
			MantisDefectRecord record = getRecord(id, issues[i]);
			/*
			 * getVersion renvoie un identifiant qui propre à chaque cahque ticket(simpledateformat : HHmmss) 
			 * si ce champ à été modifié entre 2 synchronisations > le ticket est mis à jour
			 */
			idList.add(id, record.getVersion());
			/*
			 * si aucun champs ne peut être utilisé comme identifiant passer null en argument
			 * (ts les tickets seront updatés) => Javadoc
			 * com.hp.qc.synchronizer.adapters.core Interface BaseRecordDataOrderedSet
			 */
		}
	}
    
    private void addToCache(String id, MantisDefectRecord record) {
        nodeCache.put(id, record);
    }
    
    /**
     * Get record from given id and Mantis IssueData. </br>Remarks: This method is supposed to be in the
     * package scope, and is used by other classes in this package.
     * 
     * @param id
     *            Id of IssueData to generate
     * @param issue
     *            the IssueData representing the record
     * @return the instance of the record
     */
	public MantisDefectRecord getRecord(String id, IssueData issue) throws AdapterException {
		// Check if record already in cache
		logger.info("Fetching record with id " + id);
		if (nodeCache.containsKey(id)) {
			return nodeCache.get(id);
		}
		MantisDefectRecord newRecord = new MantisDefectRecord(issue, this);
		addToCache(id, newRecord);
		return newRecord;
	}

    /**
     * Retrieve a record with given id <br/> Remarks: This method is supposed to be in the package
     * scope, and is used by other classes in this package
     */
	public MantisDefectRecord getRecord(String id) {
		if (nodeCache.containsKey(id)) {
			return nodeCache.get(id);
		}
		MantisSoapGossipClient client = getIssueHelper().getClient();
		IssueData issue = null;
		issue = client.getProjectIssueByID(new BigInteger(id));
		MantisDefectRecord record = new MantisDefectRecord(issue, this);
		return record;
	}
   
	public DefectTypeRecord getRecordInterface(String id) {
		MantisDefectRecord record = getRecord(id);
		if (record == null) {
			return null;
		}
		return record;
	}    
}
