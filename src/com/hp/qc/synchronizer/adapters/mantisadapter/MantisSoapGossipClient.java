package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;

import com.hp.qc.synchronizer.adapters.core.AdapterLogger;
import com.hp.qc.synchronizer.adapters.exceptions.AdapterException;
import com.hp.qc.synchronizer.adapters.exceptions.FatalAdapterException;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldDefinitionData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldValueForIssueData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;
import biz.futureware.mantis.rpc.soap.client.MantisConnectLocator;
import biz.futureware.mantis.rpc.soap.client.MantisConnectPortType;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;
import biz.futureware.mantis.rpc.soap.client.ProjectAttachmentData;
import biz.futureware.mantis.rpc.soap.client.ProjectData;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;

//https://sourceforge.net/p/mantisconnect/svn/80/tree/mantisconnect/trunk/clients/java/client-api/src/test/org/mantisbt/connect/service/MantisConnectPortTypeTest.java#l56
public class MantisSoapGossipClient  {
	
	private MantisConnectLocator locator;
	private MantisConnectPortType portType;
	private String user;
	private String pwd;
	private String project;
	private AdapterLogger logger;

	
	public MantisSoapGossipClient(String username, String password, String url_server_mantis, String project_name, AdapterLogger logger) {
		String s = url_server_mantis + "/api/soap/mantisconnect.php";
		URL url = null;
		try {
			url = new URL(s);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		setUp(username, password, url, project_name, logger);
	}

	public void setUp(String username, String password, URL url_server_mantis, String project_name, AdapterLogger logger) {
		try {
			
			locator  = new MantisConnectLocator();
	        portType = locator.getMantisConnectPort(url_server_mantis);
			this.user = username;
			this.pwd = password;
			this.project = project_name;
			this.logger = logger;
			/*
			String proxyHost = System.getProperty("test.http.proxyHost");
			String proxyPortStr = System.getProperty("test.http.proxyPort");
			if (proxyPortStr != null && proxyPortStr.trim().length() > 0) {
				System.setProperty("http.proxyHost", proxyHost);
				System.setProperty("http.proxyPort", proxyPortStr);
			}
			*/
		} catch (Exception e) {
			portType = null;
			throw new FatalAdapterException("Impossible to setup Mantis SOAP connexion : " + e);
		}
	}

	public BigInteger checkConnexion() {
		BigInteger project_id = null;
		try {
			project_id = portType.mc_project_get_id_from_name(user, pwd, project);
			// si project_id = 0 => null
			if ((project_id.compareTo(BigInteger.ZERO) > 0) == false) {
				project_id = null;
			}
		} catch (RemoteException e) {
			throw new FatalAdapterException("Connexion KO: " + e);
		}
		return project_id;
	}
	
	private BigInteger getProjectId() {
		BigInteger project_id = null;
		try {
			project_id = portType.mc_project_get_id_from_name(user, pwd, project);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve project id : " + e);
		}
		return project_id;
	}
	
	public void test() throws RemoteException {
		ObjectRef[] tab;
		tab = portType.mc_enum_access_levels(user, pwd);
		for (int i = 0; i < tab.length; i++) {
			System.out.println(tab[i].getName());
		}
	}

	public void getFirstProjectAttachments() {
		ProjectAttachmentData[] tab = null;
		try {
			tab = portType.mc_project_get_attachments(user, pwd, getProjectId());
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve project attchments : " + e);
		}
		for (int i = 0; i < tab.length; i++) {
			System.out.println(tab[i].getId());
		}
	}

	public ProjectVersionData[] getVersions() {
		ProjectVersionData[] result = null;
		try {
			result = portType.mc_project_get_versions(user, pwd, getProjectId());
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve project version : " + e);
		}
		return result;
	}
	
	public String getMCVersion() {
		String result = null;
		try {
			result = portType.mc_version();
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve mcversion : " + e);
		}
		return result;
	}

	public ObjectRef[] getEnumPriorities()  {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_priorities(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve priorities : " + e);
		}
		return result;
	}

	public ObjectRef[] getEnumSeverities() {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_severities(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve severities : " + e);
		}
		return result;
	}

	public ObjectRef[] getEnumReproducibilities()  {
		ObjectRef[] results = null;
		try {
			results = portType.mc_enum_reproducibilities(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve reproducibilities : " + e);
		}
		return results;
	}

	public ObjectRef[] getEnumProjections(){
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_projections(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve projections : " + e);
		}
		return result;
	}

	public ObjectRef[] getEnumEtas() {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_etas(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve etas : " + e);
		}
		return result;
	}

	public Set<AccountData> getProjectUsers() {
		ObjectRef[] level_id = null;
		AccountData[] usersArray = null;
		AccountData[] tmp = null;
		try {
			level_id = portType.mc_enum_access_levels(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve acces level : " + e);
		}
		for (int i = 0; i < level_id.length; i++) {
			try {
				tmp = portType.mc_project_get_users(user, pwd, getProjectId(), level_id[i].getId());
			} catch (RemoteException e) {
				throw new FatalAdapterException("Impossible to retrieve users : " + e);
			}
			usersArray = (AccountData[]) ArrayUtils.addAll(tmp, usersArray);
		}
		// Suppression des doublons 
		Set<AccountData> users = new HashSet<AccountData>(Arrays.asList(usersArray));
		return users;
	}

	public CustomFieldDefinitionData[] getCustomFields() {
		CustomFieldDefinitionData[] custom_fields = null;
		try {
			custom_fields = portType.mc_project_get_custom_fields(user, pwd, getProjectId());
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve custom fields for issueData : " + e);
		}
		return custom_fields;
	}
	
	public ObjectRef[] getEnumAccessLevels()  {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_access_levels(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve acces level : " + e);
		}
		return result;
	}

	public ObjectRef[] getEnumProjectStatus()  {
		ObjectRef[] results = null;
		try {
			results = portType.mc_enum_project_status(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve project status : " + e);
		}
		return results;
	}

	public ObjectRef[] getEnumStatus()  {
		ObjectRef[] results = null;
		try {
			results = portType.mc_enum_status(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve status : " + e);
		}
		return results;
	}

	public ObjectRef[] getEnumProjectViewstates()  {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_project_view_states(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve project view states : " + e);
		}
		return result;
	}

	public ObjectRef[] getEnumCustomFieldTypes() {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_custom_field_types(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve custom field types : " + e);
		}
		return result;
	}

	public ObjectRef[] getEnum() {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_view_states(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve view states " + e);
		}
		return result;
	}

	public void getConfigString() {
		String result = null;
		try {
			result = portType.mc_config_get_string(user, pwd, "allow_file_upload");
			System.out.println("allow_file_upload = " + result);
			result = portType.mc_config_get_string(user, pwd, "enable_email_notification");
			System.out.println("enable_email_notification = " + result);
		} catch (RemoteException e) {
			throw new AdapterException("Failed to get config : " + e);
		}
		try {
			result = portType.mc_config_get_string(user, pwd, "an_invalid_config");
		} catch (RemoteException e) {
			
		}
	}

	public IssueData[] getProjectIssues() {
		IssueData[] result = null, tmp = null;
		/*
		 * 
		 *  Limite actuellement définie par défaut : 100 pages * 50 tickets = 5000 tickets 
		 *  
		 *  /!\ les tickets présents sur des pages > 100 ne seraient pas pris en compte lors de la syncro /!\
		 * 		cf. PB liés à la limitation API SOAP
		 * 		/appli/projects/mantislot3test/apache_2.2.24/php_5.3.23/conf/php.ini
		 * 		memory_limit = 32M => récupère 75 IssueData
		 * 
		 */
		//100
		for (int page = 1; page <=2 ; page++) {
			try {
				tmp = portType.mc_project_get_issues(user, pwd, getProjectId(), BigInteger.valueOf(page),
						BigInteger.valueOf(50));
			} catch (RemoteException e) {
				throw new FatalAdapterException("Impossible to retrieve issues : " + e);
			}
			result = (IssueData[]) ArrayUtils.addAll(tmp, result);
		}
		return result;
	}
	
	public IssueData getProjectIssueByID(BigInteger defect_id) {
		IssueData result = null;
		try {
			result = portType.mc_issue_get(user, pwd, defect_id);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve IssueData : " + e);
		}
		return result;
	}

	public BigInteger getProjectIssueLastId(){
		BigInteger id = null;
		try {
			id = portType.mc_issue_get_biggest_id(user, pwd, getProjectId());
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve last issue id : " + e);
		}
		return id;
	}

	private ObjectRef retrievePriorityByName(String priority_name) {
		ObjectRef[] priorities = null;
		ObjectRef priority = null;
		try {
			priorities = portType.mc_enum_priorities(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve priotities : " + e);
		}
		for (int i = 0; i < priorities.length; i++) {
			if (priorities[i].getName().equals(priority_name)) {
				priority = priorities[i];
			}
		}
		if (priority == null) {
			logger.info(" Unable to find Priority <" + priority_name + "> in MAntis BT Project");
		}
		return priority;
	}
	
	private ObjectRef retrieveReproducibilityByName(String reproducibility_name) {
		ObjectRef[] reproducibilities = null;
		ObjectRef reproducibility = null;
		try {
			reproducibilities = portType.mc_enum_reproducibilities(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve reproducibilities : " + e);
		}
		for (int i = 0; i < reproducibilities.length; i++) {
			if (reproducibilities[i].getName().equals(reproducibility_name)) {
				reproducibility = reproducibilities[i];
			}
		}
		if (reproducibility == null) {
			logger.info(" Unable to find corresponding Reproducibility for <" + reproducibility_name
					+ ">  in MAntis BT Project");
		}
		return reproducibility;
	}

	private ObjectRef retrieveSeverityByName(String severity_name){
		ObjectRef[] severities = null;
		ObjectRef severity = null;
		try {
			severities = portType.mc_enum_severities(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve severities : " + e);
		}
		for (int i = 0; i < severities.length; i++) {
			if (severities[i].getName().equals(severity_name)) {
				severity = severities[i];
			}
		}
		if (severity == null){
			logger.info(" Unable to find corresponding Severity for <" + severity_name + "> in MAntis BT Project");
		}
		return severity;
	}
	
	private ObjectRef retrieveStatusByName(String status_name) {
		ObjectRef[] status = null;
		ObjectRef status_ = null;
		try {
			status = portType.mc_enum_status(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve status : " + e);
		}
		for (int i = 0; i < status.length; i++) {
			if (status[i].getName().equals(status_name)) {
				status_ = status[i];
			}
		}
		if (status_ == null){
			logger.info(" Unable to find correspondinf Status for <" + status_name + "> in MAntis BT Project");
		}
		return status_;
	}

	private ProjectData retrieveProjectDataByName() {
		ProjectData[] project_datas = null;
		ProjectData[] sub_project_datas = null;
		ProjectData project_data = null;
		try {
			project_datas = portType.mc_projects_get_user_accessible(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve project data : " + e);
		}  
		// recherche dans les projets
		for(int i=0; i<project_datas.length; i++){
			if(project_datas[i].getName().equals(project)){
				project_data = project_datas[i];
			}
			// recherche dans les sous projets
			else {
				sub_project_datas = project_datas[i].getSubprojects();
				for(int j=0; j<sub_project_datas.length; j++){
					if(sub_project_datas[j].getName().equals(project)){
						project_data = sub_project_datas[j];
					}
				}
			}
		}
		if (project_data == null){
			logger.info(" Unable to find ProjectData for project <" + project + "> in MAntis BT Project");
		}
		return project_data;
	}
	
	private AccountData retrieveUserByName(String user_name){
		Set<AccountData> users = null;
		AccountData user = null;
		users = getProjectUsers();
		for(AccountData usr : users){
			if(usr.getName().equals(user_name)){
				user = usr;
			}
		}
		if (user == null){
			logger.info(" Unable to find corresponding user for <" + user_name + "> in MAntis BT Project");
		}
		return user;
	}
	
	private ObjectRef retrieveResolutionByName(String resolution_name){
		ObjectRef[] resolutions = null;
		ObjectRef resolution = null;
		resolutions = getEnumResolutions();
		for (int i=0; i<resolutions.length; i++){
			if(resolutions[i].getName().equals(resolution_name)){
				resolution = resolutions[i];
			}
		}
		if (resolution == null) {
			logger.info(" Unable to find corresponding Resolution for <" + resolution_name + "> in MAntis BT Project");
		}
		return resolution;
	}
	
	
	
	public ObjectRef[] getEnumResolutions() {
		ObjectRef[] result = null;
		try {
			result = portType.mc_enum_resolutions(user, pwd);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve resolutions : " + e);
		}
		return result;
	}
	
	public String[] getEnumCategories() {
		String[] result = null;
		try {
			result = portType.mc_project_get_categories(user, pwd, getProjectId());
		} catch (RemoteException e) {
			throw new FatalAdapterException("Impossible to retrieve categories : " + e);
		}
		return result;
	}

	public String getProjectIssueLastUpadtedByID(BigInteger defect_id) {
		IssueData result = null;
		try {
			result = portType.mc_issue_get(user, pwd, defect_id);
		} catch (RemoteException e) {
			throw new FatalAdapterException(
					"Impossible to retrieve IssueData with id=" + defect_id + "from Mantis BT API" + e);
		}
		// DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
		DateFormat dateFormat = new SimpleDateFormat("HHmmss");
		String date = dateFormat.format(result.getLast_updated().getTime()); 
		return date;	
	}

	public void fetchAttachment(BigInteger issue_id, String name, String file_type, byte[] content) {
		try {
			portType.mc_issue_attachment_add(user, pwd, issue_id, name, file_type, content);
		} catch (RemoteException e) {
			throw new FatalAdapterException("Failed to fetch attachment : " + e);
		}	
	}
	
	public BigInteger fetchIssue(String handler_name, String note, String desc, String raporter_name,
			Date dateSubmitted, String priority_name, String reproducibility_name, String severity_name,
			String status_name, String sum, String resolution_name, String category_name, String os_name,
			String os_version_name, String additionalInformation, String version_name, String target_version_name,
			String plateform_name, String fixed_in_vers, String steps, CustomFieldValueForIssueData[] array_custom_fields) {

		logger.info("Insert Issue in Mantis");
		
		// handler = retrieveUserByName(handler_name);
		AccountData handler = retrieveUserByName("samechanges");

		// raporter = retrieveUserByName(raporter_name);
		AccountData raporter = retrieveUserByName("samechanges");
		Calendar submittedDate = null;
		if (dateSubmitted != null) {
			submittedDate = Calendar.getInstance();
			submittedDate.setTime(dateSubmitted);
		}
		ObjectRef priority = retrievePriorityByName(priority_name);
		ProjectData projectData = retrieveProjectDataByName();
		ObjectRef reproducibility = retrieveReproducibilityByName(reproducibility_name);
		ObjectRef severity = retrieveSeverityByName(severity_name);
		ObjectRef status = retrieveStatusByName(status_name);
		String summary = sum;
		ObjectRef resolution = retrieveResolutionByName(resolution_name);
		
		IssueNoteData[]  IssueNotes = MantisUtils.parseALMMemoForMantisNotes(note);
		
		String os = os_name;
		String os_version = os_version_name;
		String additionalInfo = additionalInformation;
		String version = version_name;
		String target_version = target_version_name;
		String plateform = plateform_name;
		String fixed_in_version = fixed_in_vers;
		CustomFieldValueForIssueData[] custom_fields = array_custom_fields;
		String steps_to_reproduce = steps;
		String category = category_name;
		
		// Le champ Category ne doit pas être "" ou null > Anomalie par défaut
		if (category == null || category.equals("")) {
			category = "Anomalie";
			try {
				category = portType.mc_project_get_categories(user, pwd, getProjectId())[0];
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		// Le champ Description ne doit pas être "" ou null
		String description = desc;
		if (description == null || description.equals("")) {
			description = "MantisConnect - Le champ Decscription ne doit pas être vide";
		}
		BigInteger newId = null;

		try {
			IssueData issue = new IssueData(null, // ID
					null, null, // Calendar : Last Update
					new ObjectRef(projectData.getId(), projectData.getName()), // Project
					category, // String : category
					priority, // ObjectRef : priority
					severity, // ObjectRef : severity
					status, // ObjectRef : status
					raporter, // AccountData : Reporter
					summary, // String : summary
					version, // String : Version
					null, // String : Build
					plateform, // String : Platform
					os, // String : OS
					os_version, // String : OS Build
					reproducibility, // ObjectRef : reproducibility
					submittedDate, // Calendar : Submitted
					null, // BigInteger : Sponsorship
					handler, // AccountData : Handler
					null, // ObjectRef : Projection
					null, // ObjectRef : ETA
					resolution, // ObjectRef : Resolution
					fixed_in_version, // String : Fixed in Version
					target_version, // String : target_version,
					description, // description, // String : Description
					steps_to_reproduce, // String : Steps to reproduce
					additionalInfo, // String : Additional Info
					null, // Attachments []
					null, // RelationshipData[] : Relationships
					IssueNotes, // IssueNotes, // IssueNoteData[] : Notes
					custom_fields, // CustomFieldValueForIssueData[] : // custom_fields
					null, // due date
					null, // monitors
					null, // sticky
					null); // tags
			newId = portType.mc_issue_add(user, pwd, issue);
		} catch (RemoteException e) {
			logger.info("Insert  failed");
			throw new FatalAdapterException(" Failed to insert issue : " + e);
		}
		return newId;
	}

	public void updateIssue(String id, String handler_name, String note, String desc, String raporter_name,
			Date dateSubmitted, String priority_name, String reproducibility_name, String severity_name,
			String status_name, String sum, String resolution_name, String category_name, String os_name,
			String os_version_name, String additionalInformation, String version_name, String target_version_name,
			String plateform_name, String fixed_in_vers, String steps, CustomFieldValueForIssueData[] customFieldsList){
		
		logger.info("Update Issue");
		AccountData handler = null;
		String description = null;
		AccountData raporter = null;
		Calendar submittedDate = null;
		ObjectRef priority = null;
		ProjectData projectData = null;
		ObjectRef reproducibility = null;
		ObjectRef severity = null;
		ObjectRef status = null;
		String summary = null;
		String category = null;
		ObjectRef resolution = null;
		IssueNoteData[] IssueNotes = null;
		String os = null;
		String os_version = null;
		String version = null;
		String additionalInfo = null;
		String target_version = null;
		String plateform = null;
		String fixed_in_version = null;
		String steps_to_reproduce = null;
		CustomFieldValueForIssueData[] array_custom_fields = null;
		
		// maj
		// handler = retrieveUserByName(handler_name);
		handler = retrieveUserByName("samechanges");
		resolution = retrieveResolutionByName(resolution_name);
		description = desc;
		// pour le projet BAS le champ Description ne doit pas être "" ou null
		if (description == null || description.equals("")) {
			description = "MantisConnect - Le champ Description ne doit pas être vide";
		}
		category = category_name;
		// pour le projet BAS le champ Category ne doit pas être "" ou null > Anomalie par défaut
		if (category == null || category.equals("")) {
			category = "Anomalie";
		}
		// maj
		// raporter = retrieveUserByName(raporter_name);
		raporter = retrieveUserByName("samechanges");
		
		// KO le Date de soumission n'est pas modifiable dans Mantis
		if (dateSubmitted != null) {
			submittedDate = Calendar.getInstance();
			submittedDate.setTime(dateSubmitted);
		}
		priority = retrievePriorityByName(priority_name);
		projectData = retrieveProjectDataByName();
		reproducibility = retrieveReproducibilityByName(reproducibility_name);
		severity = retrieveSeverityByName(severity_name);
		status = retrieveStatusByName(status_name);
		summary = sum;
		
		// On supprimer toutes les notes existantes avant de les réinserer
		deleteAllExistingNotes(id);
		IssueNotes = MantisUtils.parseALMMemoForMantisNotes(note);

		os = os_name;
		os_version = os_version_name;
		version = version_name;
		target_version = target_version_name;
		plateform = plateform_name;
		fixed_in_version = fixed_in_vers;
		additionalInfo = additionalInformation;
		steps_to_reproduce = steps;
		array_custom_fields = customFieldsList;
		try {
			IssueData issue = new IssueData(new BigInteger(id), // ID
					null, null, // Calendar : Last Update
					new ObjectRef(projectData.getId(), projectData.getName()), // Project
					category, // String : category
					priority, // ObjectRef : priority
					severity, // ObjectRef : severity
					status, // ObjectRef : status
					raporter, // AccountData : Reporter
					summary, // String : summary
					version, // String : Version
					null, // String : Build
					plateform, // String : Platform
					os, // String : OS
					os_version, // String : OS Build
					reproducibility, // ObjectRef : reproducibility
					submittedDate, // Calendar : Submitted
					null, // BigInteger : Sponsorship
					handler, // AccountData : Handler
					null, // ObjectRef : Projection
					null, // ObjectRef : ETA
					resolution, // ObjectRef : Resolution
					fixed_in_version, // String : Fixed in Version
					target_version, // String : target_version,
					description, // String : Description
					steps_to_reproduce, // String : Steps to reproduce
					additionalInfo, // String : Additional Info
					null, // Attachments []
					null, // RelationshipData[] : Relationships
					IssueNotes, // IssueNoteData[] : Notes
					array_custom_fields, // CustomFieldValueForIssueData[] : // custom_fields
					null, // due date
					null, // monitors
					null, // sticky
					null); // tags
			portType.mc_issue_update(user, pwd, new BigInteger(id), issue);
		} catch (RemoteException e) {
			logger.info("Update failed");
			throw new FatalAdapterException("Failed to update issue : " + e);
		}
	}
	
	private void deleteAllExistingNotes(String id) {
		// portType.mc_issue_update(user, pwd, new BigInteger(id), issue);
		try {
			IssueData issue = portType.mc_issue_get(user, pwd, new BigInteger(id));
			IssueNoteData[] notes = issue.getNotes();
			if (notes != null)
				for (IssueNoteData note : notes) {
					portType.mc_issue_note_delete(user, pwd, note.getId());
				}
		} catch (RemoteException e) {
			throw new AdapterException("Failed to delete notes for IssueData : " + e);
		}
	}

	public void deleteIssue(String id) {
		logger.info("Delete Issue");
		try {
			portType.mc_issue_delete(user, pwd, new BigInteger(id));
		} catch (RemoteException e) {
			logger.info("Delete failed");
			throw new AdapterException("Failed to delete issue : " + e);
		}
	}

	public void getAttachment(BigInteger issue_id) {
		logger.info("Get Attachment for issue " + issue_id);
		try {
			portType.mc_issue_attachment_get(user, pwd, issue_id);
		} catch (RemoteException e) {
			logger.info("Delete failed");
			throw new AdapterException("Failed to get attachments : " + e);
		}
	}
	
	

	public void deleteAttachment(BigInteger attachment_id) {
		logger.info("Delete Attachment with id: " + attachment_id);
		try {
			portType.mc_issue_attachment_delete(user, pwd, attachment_id);
		} catch (RemoteException e) {
			logger.info("Delete failed");
			throw new AdapterException("Failed to delete attachment : " + e);
		}
	}
	

	

}