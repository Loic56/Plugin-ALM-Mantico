package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.hp.qc.synchronizer.adapters.core.AttachmentInfo;
import com.hp.qc.synchronizer.adapters.core.FieldInfo;
import com.hp.qc.synchronizer.adapters.exceptions.EntityNotFoundException;
import com.hp.qc.synchronizer.adapters.exceptions.FatalAdapterException;
import com.hp.qc.synchronizer.adapters.spi.AttachmentHandler;
import com.hp.qc.synchronizer.adapters.spi.Record;

import biz.futureware.mantis.rpc.soap.client.AttachmentData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldDefinitionData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldValueForIssueData;
import biz.futureware.mantis.rpc.soap.client.IssueData;

public abstract class MantisRecord implements Record {

	protected static MantisRecord ghostRecord = null;
	protected boolean deleted = false;
	protected boolean ghostObject = false;

	protected Map<String, Object> recordFields = new HashMap<String, Object>();
	protected Map<String, Object> recordCustomFields = new HashMap<String, Object>();

	protected MantisRecordManager manager;
	protected IssueData MantisIssue;
	protected MantisSoapGossipClient client;

	public MantisRecord() {
	}

	/**
	 * Create new record from given data and persist record to Mantis document
	 * 
	 * @param isFolder
	 *            Is this element a folder record
	 */
	public MantisRecord(MantisRecordManager manager) {
		this.manager = manager;
	}

	public MantisRecord(IssueData issue, MantisRecordManager manager) {
		this.manager = manager;
		this.MantisIssue = issue;
		
		// definition data des custom fields
		MantisSoapGossipClient client = manager.getIssueHelper().getClient();
		CustomFieldDefinitionData[] customFieldDefinitions = null;
	
		customFieldDefinitions = client.getCustomFields();
	
		// construction du recordCustomField
		CustomFieldValueForIssueData[] customFields = issue.getCustom_fields();
		if (customFields != null) {
			for (int i = 0; i < customFields.length; i++) {
				String custom_field_name = customFields[i].getField().getName();
				String value = customFields[i].getValue().trim();
				if (StringUtils.isNotEmpty(value.trim())) {
					for (CustomFieldDefinitionData def : customFieldDefinitions) {
						Object obj = null;
						if (def.getField().getName().equals(custom_field_name)) {
							BigInteger type = def.getType();
							// les valeurs de l'Issue récupérées via SOAP doivent être castées au format ALM 
							// avt d'être ajoutées au recordCustomField
							obj = MantisUtils.cast_for_ALM(type, value);
							custom_field_name = MantisUtils.escapeSpecialsCharacters(custom_field_name);
							// cutom_field_name = "[custom] " + cutom_field_name;
							recordCustomFields.put(custom_field_name, obj);
							break;
						}
					}
				}	
			}
		}
		
		// construction du recordField
		recordFields.put(MantisNodes.ID, issue.getId().toString());
		if (issue.getAttachments() != null) {
			recordFields.put(MantisNodes.ATTACHMENT, issue.getAttachments());
		}
		if (issue.getHandler() != null) {
			recordFields.put(MantisNodes.HANDLER, issue.getHandler().getName());
		}
		if (issue.getReporter() != null) {
			recordFields.put(MantisNodes.REPORTER, issue.getReporter().getName());
		}
		if (issue.getNotes() != null) {
			recordFields.put(MantisNodes.NOTE, issue.getNotes());
		}
		if (issue.getOs() != null) {
			recordFields.put(MantisNodes.OS, issue.getOs());
		}
		if (issue.getOs_build() != null) {
			recordFields.put(MantisNodes.OS_VERSION, issue.getOs_build());
		}
		if (issue.getResolution() != null) {
			recordFields.put(MantisNodes.RESOLUTION, issue.getResolution().getName());
		}
		if (issue.getDescription() != null) {
			recordFields.put(MantisNodes.DESCRIPTION, issue.getDescription());
		}
		if (issue.getDate_submitted() != null) {
			recordFields.put(MantisNodes.SUBMITEDDATE, issue.getDate_submitted().getTime());
		}
		if (issue.getPriority() != null) {
			recordFields.put(MantisNodes.PRIORITY, issue.getPriority().getName());
		}
		if (issue.getCategory() != null) {
			recordFields.put(MantisNodes.CATEGORY, issue.getCategory());
		}
		if (issue.getReproducibility() != null) {
			recordFields.put(MantisNodes.REPRODUCIBILITY, issue.getReproducibility().getName());
		}
		if (issue.getSeverity() != null) {
			recordFields.put(MantisNodes.SEVERITY, issue.getSeverity().getName());
		}
		if (issue.getStatus() != null) {
			recordFields.put(MantisNodes.STATUS, issue.getStatus().getName());
		}
		if (issue.getAdditional_information() != null) {
			recordFields.put(MantisNodes.ADDITIONAL_INFOS, issue.getAdditional_information());
		}
		if (issue.getSteps_to_reproduce()!= null) {
			recordFields.put(MantisNodes.STEPS_TO_REPRODUCE, issue.getSteps_to_reproduce());
		}
		if (issue.getSummary() != null) {
			recordFields.put(MantisNodes.SUMMARY, issue.getSummary());
		}
		if (issue.getVersion() != null) {
			recordFields.put(MantisNodes.VERSION, issue.getVersion());
		}
		if (issue.getTarget_version() != null) {
			recordFields.put(MantisNodes.TARGET_VERSION, issue.getTarget_version());
		}
		if (issue.getPlatform() != null) {
			recordFields.put(MantisNodes.PLATEFORM, issue.getPlatform());
		}
		if (issue.getFixed_in_version() != null) {
			recordFields.put(MantisNodes.FIXED_IN_VERSION, issue.getFixed_in_version());
		}
		if (issue.getLast_updated() != null) {
			recordFields.put(MantisNodes.LAST_UPDATED, issue.getLast_updated());
			DateFormat dateFormat = new SimpleDateFormat("HHmmss");
			String last_update = dateFormat.format(issue.getLast_updated().getTime());
			recordFields.put(MantisNodes.FORMATTED_LAST_UPDATE, last_update);
		}
	}

	protected abstract String[] getEntityFieldNames();

	
	public static boolean isFolder(String entityName) {
		return MantisNodes.FOLDER.toUpperCase().equals(entityName.toUpperCase());
	}

	
	public static boolean isRequirementType(String entityName) {
		return MantisNodes.REQUIREMENT.toUpperCase().equals(entityName.toUpperCase());
	}

	
	public static boolean isDefectType(String entityType) {
		return MantisNodes.DEFECT.toUpperCase().equals(entityType.toUpperCase());
	}

	
	public void delete()  {
		String id = (String) this.recordFields.get(MantisNodes.ID);
		MantisIssueHelper helper = manager.getIssueHelper();
		MantisSoapGossipClient client = helper.getClient();
		client.deleteIssue(id);
		deleted = true;
	}

	
	// Méthode appelée quand une issue est modifiée dans Mantico > Créé un update côté ALM
	public Map<String, Object> fetchData(Map<String, FieldInfo> fields) {
		assertObjectStateIsValid();
		Map<String, Object> fetchedFields = new HashMap<String, Object>();
		String currFieldName = null;
		for (FieldInfo fi : fields.values()) {
			currFieldName = fi.getName();
			
			if (this.recordFields.containsKey(currFieldName)) {
				// Notes doit pouvoir être mappé sur un champ de type MEMO
				if (currFieldName.equals(MantisNodes.NOTE)) {
					String id = (String) this.recordFields.get(MantisNodes.ID);
					MantisSoapGossipClient client = manager.getIssueHelper().getClient();
					IssueData issue = null;
					issue = client.getProjectIssueByID(new BigInteger(id));
					String notes_formatted = MantisUtils.formatMantisNoteForALM(issue);
					fetchedFields.put(currFieldName, notes_formatted);
				} else if(currFieldName.equals(MantisNodes.ATTACHMENT)){ 
					Object attachments = getAttachementHandler(currFieldName).fetchAttachmentData();
					fetchedFields.put(currFieldName, attachments);
				} else {
					fetchedFields.put(currFieldName, this.recordFields.get(currFieldName));
				}
			} else if (currFieldName.contains(MantisUtils.CUSTOM)) {
				fetchedFields.put(currFieldName, this.recordCustomFields.get(currFieldName.substring(9)));
			}
		}

		return fetchedFields;
	}

	
	
	/*
	 * La méthode getVersion() sert a récupérer le timestamp associé a un
	 * ticket(non-Javadoc)
	 * 
	 * @see com.hp.qc.synchronizer.adapters.spi.Record#getVersion() permet au
	 * synchronizer de déterminer quels tickets sont à updater/créer
	 */
	public String getVersion() {
		assertObjectStateIsValid();
		String lastModified = null;
		if (this.recordFields.containsKey(MantisNodes.FORMATTED_LAST_UPDATE)) {
			lastModified = (String) this.recordFields.get(MantisNodes.FORMATTED_LAST_UPDATE);
		}
		return lastModified;
	}

	
	
	// Méthode appelée pour les Update côté Mantis
	public void update(Map<String, Object> fields) {
		assertObjectStateIsValid();
		boolean changed = false;
		// Champs natifs > construction du recordField
		for (String fieldName : getEntityFieldNames()) {
			Object fieldValue = fields.get(fieldName);
			if (fieldValue != null) {
				if (fields.get(fieldName).getClass() == Date.class) {
					this.recordFields.put(fieldName, fieldValue);
				} else {
					this.recordFields.put(fieldName, fieldValue.toString());
				}
				changed = true;
			}
		}
		// Champs custom > construction du recordCustomField
		for (Map.Entry<String, Object> field : fields.entrySet()) {
			String cutom_field_name = field.getKey();
			if (cutom_field_name.contains("[custom]")) {
				// on supprime le [custom] de la clé d'identification
				this.recordCustomFields.put(cutom_field_name.substring(9), field.getValue());
				changed = true;
			}
		}
		if (changed) {
			updateMantisElement();
		}
	}

	public BigInteger insertMantisTicket(MantisIssueHelper helper, Map<String, Object> fields) {
		MantisSoapGossipClient client = helper.getClient();
		String handler = (String) MantisUtils.findInRecordFields(recordFields, MantisNodes.HANDLER);
		String note = (String)MantisUtils.findInRecordFields(recordFields,  MantisNodes.NOTE);
		String description = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.DESCRIPTION);
		String raporter = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.REPORTER);
		Date dateSubmitted = (Date)MantisUtils.findInRecordFields(recordFields, MantisNodes.SUBMITEDDATE);
		String priority = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.PRIORITY);
		String reproducibility =(String) MantisUtils.findInRecordFields(recordFields, MantisNodes.REPRODUCIBILITY);
		String severity = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.SEVERITY);
		String status = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.STATUS);
		String summary = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.SUMMARY);
		String resolution = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.RESOLUTION);
		String category = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.CATEGORY);
		String os = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.OS);
		String os_version = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.OS_VERSION);
		String additionalInfos = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.ADDITIONAL_INFOS);
		String version = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.VERSION);
		String target_version = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.TARGET_VERSION);
		String plateform = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.PLATEFORM);
		String fixed_in_version = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.FIXED_IN_VERSION);
		String steps_to_reproduce = (String)MantisUtils.findInRecordFields(recordFields, MantisNodes.STEPS_TO_REPRODUCE);
		
		CustomFieldValueForIssueData[] array_custom_fields = MantisUtils.buildCustomFieldValueForIssueData(recordCustomFields, client);
		
		BigInteger id = client.fetchIssue(handler, note, description, raporter, dateSubmitted, priority,
				reproducibility, severity, status, summary, resolution, category, os, os_version, additionalInfos,
				version, target_version, plateform, fixed_in_version, steps_to_reproduce, array_custom_fields);

		return id;
	}
	
	public void updateAndChangeType(String newEntityType, Map<String, Object> fields) {
		assertObjectStateIsValid();
		this.update(fields);
	}

	/**
	 * Updates Mantis element with values from fields collection Will update
	 * last modified time as well
	 */
	protected void updateMantisElement() {
		assertObjectStateIsValid();
		MantisIssueHelper helper = manager.getIssueHelper();
		updateMantisTicket(recordFields, recordCustomFields, helper);
	}

	
	
	
	private void updateMantisTicket(Map<String, Object> recordFields, Map<String, Object> recordCustomFields,
			MantisIssueHelper helper) {
		MantisSoapGossipClient client = helper.getClient();
		// Champs natifs
		String id = (String) recordFields.get(MantisNodes.ID);
		String handler = (String) recordFields.get(MantisNodes.HANDLER);
		String note = (String) recordFields.get(MantisNodes.NOTE);
		String description = (String) recordFields.get(MantisNodes.DESCRIPTION);
		String raporter = (String) recordFields.get(MantisNodes.REPORTER);
		Date dateSubmitted = (Date) recordFields.get(MantisNodes.SUBMITEDDATE);
		String priority = (String) recordFields.get(MantisNodes.PRIORITY);
		String reproducibility = (String) recordFields.get(MantisNodes.REPRODUCIBILITY);
		String severity = (String) recordFields.get(MantisNodes.SEVERITY);
		String status = (String) recordFields.get(MantisNodes.STATUS);
		String summary = (String) recordFields.get(MantisNodes.SUMMARY);
		String resolution = (String) recordFields.get(MantisNodes.RESOLUTION);
		String category = (String) recordFields.get(MantisNodes.CATEGORY);
		String os = (String) recordFields.get(MantisNodes.OS);
		String os_version = (String) recordFields.get(MantisNodes.OS_VERSION);
		String additionalInfos = (String) recordFields.get(MantisNodes.ADDITIONAL_INFOS);
		String version = (String) recordFields.get(MantisNodes.VERSION);
		String target_version = (String) recordFields.get(MantisNodes.TARGET_VERSION);
		String plateform = (String) recordFields.get(MantisNodes.PLATEFORM);
		String fixed_in_version = (String) recordFields.get(MantisNodes.FIXED_IN_VERSION);
		String steps_to_reproduce = (String) recordFields.get(MantisNodes.STEPS_TO_REPRODUCE);
		
		// Champs custom
		CustomFieldValueForIssueData[] array_custom_fields = MantisUtils
				.buildCustomFieldValueForIssueData(recordCustomFields, client);

		client.updateIssue(id, handler, note, description, raporter, dateSubmitted, priority, reproducibility,
				severity, status, summary, resolution, category, os, os_version, additionalInfos, version,
				target_version, plateform, fixed_in_version, steps_to_reproduce, array_custom_fields);
		
		recordFields.put(MantisNodes.FORMATTED_LAST_UPDATE, client.getProjectIssueLastUpadtedByID(new BigInteger(id)));

	}
	
	
	
	public MantisRecordManager getManager() {
		return manager;
	}

	public void setManager(MantisRecordManager manager) {
		this.manager = manager;
	}

	public String getID() {
		return (String) recordFields.get(MantisNodes.ID);
	}

	public String getType() {
		return MantisNodes.REQUIREMENT;
	}

	/**
	 * Check if current object state is valid and throw exception if it isn't
	 */
	protected void assertObjectStateIsValid() {
		if (deleted) {
			throw new EntityNotFoundException("Entity has been deleted");
		}
		if (ghostObject) {
			throw new EntityNotFoundException("Entity does not exist");
		}
	}

	

	public MantisSoapGossipClient getClient() {
		return client;
	}

	
	
	
	
	public AttachmentHandler getAttachementHandler(String fieldName) {
		assertObjectStateIsValid();
		System.out.println("getAttachementHandler");
		// contexte > create
		MantisSoapGossipClient client  = getClient();
		// contexte > upadte
		if(client == null){
			client = getManager().getIssueHelper().getClient();
		}
		return new MantisAttachment(client);
	}

	
	// This is example of where AttachmentHanlder should be implemented
	public class MantisAttachment implements AttachmentHandler {
		MantisSoapGossipClient client = null;
		
		public MantisAttachment(MantisSoapGossipClient cli) {
			this.client = cli;
		}

		// recupere les info des attachment
		public AttachmentInfo[] fetchAttachmentData() {
			System.out.println("fetchAttachmentData");
			String id = (String) recordFields.get(MantisNodes.ID);
			BigInteger issue_id = new BigInteger(id);
			IssueData issue  = null;
			issue = client.getProjectIssueByID(issue_id);
			AttachmentData[] attachments = issue.getAttachments();
			AttachmentInfo[] infos = new AttachmentInfo[attachments.length];
			int count = 0;
			for (AttachmentData data : attachments) {
				infos[count++] = new AttachmentInfo(data.getFilename(), data.getSize().longValue());
			}
			return infos;
		}

		// insert un attachment dans Mantico
		public void addAttachment(String filePath, String fileName, String description) {
			System.out.println("filePath:" + filePath + "\nfileName:" + fileName + "\ndescription:" + description);
			File file = new File(filePath + "\\" + fileName);
			byte[] content = null;
			// lecture du fichier tmp qui vient d'être téléchqargé depuis ALM sur le poste local
			try {
				content = FileUtils.readFileToByteArray(file);
			} catch (IOException e) {
				e.printStackTrace();
			}
			String id = (String) recordFields.get(MantisNodes.ID);
			BigInteger issue_id = new BigInteger(id);
			String file_type = new MimetypesFileTypeMap().getContentType(file);
			client.fetchAttachment(issue_id, fileName, file_type, content);

		}

		// supprime un attachment ds Mantico
		public void deleteAttachment(String fileName) {
			System.out.println("fileName:" + fileName);
			String id = (String) recordFields.get(MantisNodes.ID);
			BigInteger issue_id = new BigInteger(id);
			IssueData issue  = null;
			issue = client.getProjectIssueByID(issue_id);
			AttachmentData[] attachments = issue.getAttachments();
			for(AttachmentData attachment : attachments){
				if (attachment.getFilename().equals(fileName) ){
					client.deleteAttachment(attachment.getId());
					break;
				}
			}
		}

		// download un attachment depuis Mantico sur le poste local (fichier tmp)
		public void fetchAttachment(String filePath, String fileName) {
			String fileFullPath = (new StringBuilder()).append(filePath).append(File.separator).append(fileName).toString();
			System.out.println(fileFullPath);
			
			String id = (String) recordFields.get(MantisNodes.ID);
			BigInteger issue_id = new BigInteger(id);
			IssueData issue  = null;
	
			issue = client.getProjectIssueByID(issue_id);
		
			AttachmentData[] attachments = issue.getAttachments();
			for (AttachmentData att : attachments) {
				try {
					String username = "samechanges";
					String password = "NupruhUcuz3z";
					String authString = username+":"+password;
					byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
					String authStringEnc = new String(authEncBytes);
					
					URL url = new URL(att.getDownload_url().toString());
					URLConnection urlConnection = url.openConnection();
					urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
					InputStream in = urlConnection.getInputStream();
					FileOutputStream fos = new FileOutputStream(new File(fileFullPath));
					
					int length = -1;
					byte[] buffer = new byte[1024]; 
					
					while ((length = in.read(buffer)) > -1) {
						fos.write(buffer, 0, length);
					}
					fos.close();
					in.close();
				} catch (MalformedURLException e) {
					throw new FatalAdapterException("Failed to download attachments");
				} catch (IOException e) {
					throw new FatalAdapterException("Failed to download attachments");
				}
			}

		}

		public void updateAttachment(String filePath, String fileName, String description) {
			System.out.println("filePath:" + filePath + "\nfileName:" + fileName + "description:" + description);
		}
	}
}
