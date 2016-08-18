package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.math.BigInteger;

import com.hp.qc.synchronizer.adapters.core.AdapterConnectionData;
import com.hp.qc.synchronizer.adapters.core.AdapterLogger;
import com.hp.qc.synchronizer.adapters.core.EntityType;
import com.hp.qc.synchronizer.adapters.exceptions.AdapterConnectionException;
import com.hp.qc.synchronizer.adapters.exceptions.AdapterException;
import com.hp.qc.synchronizer.adapters.spi.AdapterConnection;
import com.hp.qc.synchronizer.adapters.spi.DefectManager;
import com.hp.qc.synchronizer.adapters.spi.RequirementManager;
import com.hp.qc.synchronizer.schema.EntitySchemaBuilder;
import com.hp.qc.synchronizer.schema.EntityTypesBuilder;
import com.hp.qc.synchronizer.schema.FieldSchema;

public class MantisAdapterConnection implements AdapterConnection {

	public static final String URL_SERVER_MANTIS = "URL du Serveur Mantis";
	public static final String MANTIS_PROJECT = "Projet";
	public static final String MANTIS_USER = "User";
	public static final String MANTIS_PWD = "Password";
	
	private boolean connected;
	private String mantisServerUrl;
	private String userName;
	private String password;
	private final AdapterLogger logger;
	private String mantisProject;
	private MantisDefectManager defectManager = null;
	private MantisSoapGossipClient mantisSoapClient;

	public MantisAdapterConnection(AdapterLogger logger) {
		userName = null;
		password = null;
		connected = false;
		this.logger = logger;
	}

	public void connect(AdapterConnectionData connData) {
		logger.info("Checking connection ...");
		userName = connData.getUsername();
		password = connData.getPassword();
		mantisServerUrl = connData.getPropertyByName(URL_SERVER_MANTIS);
		mantisProject = connData.getPropertyByName(MANTIS_PROJECT);

		if ((userName == null) || (userName == "")) {
			throw new AdapterConnectionException("Connection data is missing a parameter: " + MANTIS_USER);
		}
		if ((password == null) || (password == "")) {
			throw new AdapterConnectionException("Connection data is missing a parameter: " + MANTIS_PWD);
		}
		if ((mantisServerUrl == null) || (mantisServerUrl == "")) {
			throw new AdapterConnectionException("Connection data is missing a parameter: " + URL_SERVER_MANTIS);
		}
		if ((mantisProject == null) || (mantisProject == "")) {
			throw new AdapterConnectionException("Connection data is missing a parameter: " + MANTIS_PROJECT);
		}
		
		// Connection à l'API SOAP de Mantis > vérification des paramètres 
		mantisSoapClient = new MantisSoapGossipClient(userName, password, mantisServerUrl, mantisProject, logger);
		BigInteger check = mantisSoapClient.checkConnexion();
		if (check != null) {
			connected = true;
			logger.info("Connection successful");
		} else {
			connected = false;
			logger.info("Connection failed. ");
			throw new AdapterConnectionException("Failed to connect to project. ");
		}
	}

	public void declareEntityTypes(EntityTypesBuilder entityTypesbuilder) throws AdapterException {
		logger.info("declareEntityTypes() called");
		entityTypesbuilder.declareType(MantisNodes.DEFECT, EntityType.DEFECT);
		logger.info("declareEntityTypes() completed");
	}

	// Construction de l'EntitySchemaBuilder (cf. champs IHM Mantico)
	public void buildEntitySchema(EntitySchemaBuilder entSchema, String entityType) throws AdapterException {
		logger.info((new StringBuilder()).append("buildSchema() called for type ").append(entityType).toString());
		logger.info("buildSchema() called for type " + entityType);

		if (!MantisRecord.isRequirementType(entityType) && !MantisRecord.isDefectType(entityType)) {
			throw new AdapterException("Unsupported entity type " + entityType);
		}
		if (MantisRecord.isRequirementType(entityType)) {
			throw new AdapterException("nsupported entity type " + entityType);
		} else if (MantisRecord.isDefectType(entityType)) {
			FieldSchema field = null;
			
			// Ajout des champs natifs à l'EntitySchemaBuilder
			MantisUtils.buildEntitySchema(entSchema, field, logger, mantisSoapClient);
			
			// Ajout des champs custom à l'EntitySchemaBuilder
			MantisUtils.buildEntitySchemaWithCustomFields(entSchema, field, logger, mantisSoapClient);
		}
		logger.info("buildSchema() completed");
	}

	public void disconnect() {
		logger.info("Disconnecting and commit changes");
		connected = false;
		logger.info("Disconnected");
	}

	public DefectManager getDefectManager(String entityName) throws AdapterException {
		logger.info("Generating defects manager for type " + entityName);
		assertConnectionValid();
		if (MantisDefectRecord.isDefectType(entityName)) {
			if (defectManager == null) {
				// Create manager
				MantisIssueHelper helper = new MantisIssueHelper(mantisSoapClient);
				defectManager = new MantisDefectManager(helper, logger);
			}
			return defectManager;
		}
		return null;
	}

	private void assertConnectionValid() {
		if (!isConnected()) {
			throw new AdapterException("Not connected");
		}
	}

	public boolean isConnected() {
		return connected;
	}

	public void checkUserPermitted() throws AdapterException {
		logger.info("validateUserPermissions() called");
	}

	// Validation de l'integrity check
	public boolean hasCreatePermissions(String entityType) throws AdapterException {
		return true;
	}

	public boolean hasDeletePermissions(String entityType) throws AdapterException {
		return true;
	}

	public boolean hasUpdatePermissions(String entityType) throws AdapterException {
		return true;
	}

	public String getMantisProject() {
		return mantisProject;
	}

	// Requirement non implémenté
	@Override
	public RequirementManager getRequirementManager(String paramString) throws AdapterException {
		return null;
	}
}
