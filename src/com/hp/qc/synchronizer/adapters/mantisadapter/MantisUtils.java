package com.hp.qc.synchronizer.adapters.mantisadapter;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.hp.qc.synchronizer.adapters.core.AdapterLogger;
import com.hp.qc.synchronizer.schema.EntitySchemaBuilder;
import com.hp.qc.synchronizer.schema.FieldSchema;
import com.hp.qc.synchronizer.schema.FieldSchema.FieldRequiredLevel;
import com.hp.qc.synchronizer.schema.FieldSchema.FieldType;
import com.hp.qc.synchronizer.schema.ListFieldSchema;

import biz.futureware.mantis.rpc.soap.client.AccountData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldDefinitionData;
import biz.futureware.mantis.rpc.soap.client.CustomFieldValueForIssueData;
import biz.futureware.mantis.rpc.soap.client.IssueData;
import biz.futureware.mantis.rpc.soap.client.IssueNoteData;
import biz.futureware.mantis.rpc.soap.client.ObjectRef;
import biz.futureware.mantis.rpc.soap.client.ProjectVersionData;

public class MantisUtils {

	public static final String MANTIS_USER = "User";
	public static final String MANTIS_PWD = "Password";
	public static final String URL_SERVER_MANTIS = "Server Mantis URL";
	public static final String MANTIS_PROJECT = "Project";
	public static final String SEPARATOR = "________________________________________";
	public static final String HEADER_MANTICO = "[insert from mantico]";
	public static final String HEADER_ALM = "[insert from alm]";
	public static final String CUSTOM = "[custom]";
	
	public static String escapeSpecialsCharacters(String str) {
		// À Á Â Ä Å Ã Æ Ç É È Ê Ë Í Ì Î Ï Ñ Ó Ò Ô Ö Ø Õ OE Ú Ù Û Ü Ý Y
		str = str.replaceAll("é", "e");
		str = str.replaceAll("è", "e");
		str = str.replaceAll("î", "i");
		str = str.replaceAll("ï", "i");
		str = str.replaceAll("É", "E");
		str = str.replaceAll("ê", "e");
		str = str.replaceAll("û", "u");
		str = str.replaceAll("à", "a");
		str = str.trim();
		return str;
	}

	// lorsqu'on récupère les
	public static Object cast_for_ALM(BigInteger type, String value) {
		Object obj = null;
		if (type.equals(new BigInteger("1"))) {
			// 1 | Nombre entier
			if (StringUtils.isNotEmpty(value.trim())) {
				obj = Integer.parseInt(value);
			}
		} else if (type.equals(new BigInteger("2"))) {
			if (StringUtils.isNotEmpty(value.trim())) {
				obj = Double.parseDouble(value);
			}
		} else if (type.equals(new BigInteger("3"))) {
			// 3 | Énumération
			obj = value;
		} else if (type.equals(new BigInteger("4"))) {
			// 4 | Courriel
			obj = value;
		} else if (type.equals(new BigInteger("5"))) {
			// 5 | Case à cocher
			obj = value;
		} else if (type.equals(new BigInteger("8"))) {
			if (StringUtils.isNotEmpty(value.trim())) {
				obj = new Date();
				Long l = Long.parseLong(value);
				l = l * 1000;
				((Date) obj).setTime(l);
			}
		} else if (type.equals(new BigInteger("9"))) {
			// 9 | Bouton radio
			obj = value;
		} else {
			obj = value;
		}
		return obj;
	}

	
	// Construction d'un tableau de custom fields à partir du recordCustomField
	public static CustomFieldValueForIssueData[] buildCustomFieldValueForIssueData(
			Map<String, Object> recordCustomFields, MantisSoapGossipClient client) {
		CustomFieldDefinitionData[] customFieldsList = null;
		
		customFieldsList = client.getCustomFields();

		// /!\ Dev Limitation => 10 custom fields /!\
		CustomFieldValueForIssueData[] array_custom_fields = new CustomFieldValueForIssueData[10];
		int count = 0;
		ObjectRef field = null;

		for (Map.Entry<String, Object> item : recordCustomFields.entrySet()) {
			String custom_field_name = item.getKey();
			custom_field_name = custom_field_name.trim();
			Object custom_field_object = item.getValue();
			// Recherche du custom field (name) ds le tableau des
			// CustomFieldDefinitionData
			for (CustomFieldDefinitionData data : customFieldsList) {
				String field_name = data.getField().getName();
				// BigInteger id = data.getField().getId();
				field_name = MantisUtils.escapeSpecialsCharacters(field_name);

				if (field_name.equals(custom_field_name)) {
					field = data.getField();
					// BigInteger type = data.getType();
					// String name = field.getName();
					CustomFieldValueForIssueData customField = new CustomFieldValueForIssueData();
					customField.setField(field);
					String custom_field_value_formatted = null;
					// Cast > pour les update MANTICO les custom fields doivent être castés au format MANTICO
					// Un champ Mantico peut être de ces 3 types : Number/Date/String ds l'IHM du synchronizer
					// Mais Les custom fields doivent être obligatoirement au format String pour les insert via SOAP
					if (custom_field_object != null) {
						// Number / Double
						if (Number.class.isInstance(custom_field_object)) {
							custom_field_value_formatted = String.valueOf(custom_field_object);
						}
						// Date
						else if (custom_field_object instanceof Date) {
							// Cast de la date ALM au format date Mantico
							Long date_in_millisecond = ((Date) custom_field_object).getTime();
							date_in_millisecond = date_in_millisecond / 1000;
							custom_field_value_formatted = String.valueOf(date_in_millisecond);
						}
						// String
						else {
							custom_field_value_formatted = (String) custom_field_object;
						}
					}
					customField.setValue(custom_field_value_formatted);
					array_custom_fields[count] = customField;
					count++;
				}
			}
		}
		CustomFieldValueForIssueData[] new_array = removeNullValuesFromArray(array_custom_fields);
		return new_array;
	}

	// supprime les entrées null dans un tableau
	private static CustomFieldValueForIssueData[] removeNullValuesFromArray(
			CustomFieldValueForIssueData[] array_custom_fields) {

		List<CustomFieldValueForIssueData> list = Arrays.asList(array_custom_fields);
		List<CustomFieldValueForIssueData> listNew = new ArrayList<CustomFieldValueForIssueData>();
		for (CustomFieldValueForIssueData issueData : list) {
			if (issueData != null) {
				listNew.add(issueData);
			}
		}
		array_custom_fields = listNew.toArray(new CustomFieldValueForIssueData[listNew.size()]);
		return array_custom_fields;
	}

	// construction du EntitySchemaBuilder avec les champs natifs > mapping de l'IHM
	public static void buildEntitySchema(EntitySchemaBuilder entSchema, FieldSchema f, AdapterLogger logger,
			MantisSoapGossipClient mantisSoapClient) {
		FieldSchema field = f;

		// Add HANDLER/BG_RESPONSIBLE
		Set<AccountData> users = null;
		users = mantisSoapClient.getProjectUsers();
	
		logger.info("Adding " + MantisNodes.HANDLER);
		ListFieldSchema listField1 = entSchema.addListField(MantisNodes.HANDLER, FieldType.USER_LIST);
		for (AccountData user : users) {
			listField1.addValue(user.getName());
		}
		listField1.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField1.setReadonly(false);
		listField1.setDisplayName(MantisNodes.HANDLER);

		// Add RAPPORTER/BG_DETECTED_BY
		logger.info("Adding " + MantisNodes.REPORTER);
		ListFieldSchema listField2 = entSchema.addListField(MantisNodes.REPORTER, FieldType.USER_LIST);
		for (AccountData user : users) {
			listField2.addValue(user.getName());
		}
		listField2.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField2.setReadonly(false);
		listField2.setDisplayName(MantisNodes.REPORTER);

		// Add COMMENT/NOTE field
		logger.info("Adding " + MantisNodes.NOTE);
		field = entSchema.addField(MantisNodes.NOTE, FieldType.MEMO);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.NOTE);

		// Add ID field
		logger.info("Adding " + MantisNodes.ID);
		field = entSchema.addField(MantisNodes.ID, FieldType.STRING);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.ID);

		// Add DESCRIPTION field
		logger.info("Adding " + MantisNodes.DESCRIPTION);
		field = entSchema.addField(MantisNodes.DESCRIPTION, FieldType.MEMO);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.DESCRIPTION);

		// Add BG_DETECTION_DATE/SUBMITEDDATE field
		logger.info("Adding " + MantisNodes.SUBMITEDDATE);
		field = entSchema.addField(MantisNodes.SUBMITEDDATE, FieldType.DATE);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.SUBMITEDDATE);

		// Add BG_VTS/LAST_UPDATED field
		logger.info("Adding " + MantisNodes.LAST_UPDATED);
		field = entSchema.addField(MantisNodes.LAST_UPDATED, FieldType.DATE);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.LAST_UPDATED);

		// Add PRIORITY field
		ObjectRef[] priorities = null;
		priorities = mantisSoapClient.getEnumPriorities();

		logger.info("Adding " + MantisNodes.PRIORITY);
		ListFieldSchema listField3 = entSchema.addListField(MantisNodes.PRIORITY, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < priorities.length; i++) {
			listField3.addValue(priorities[i].getName());
		}
		listField3.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField3.setReadonly(false);
		listField3.setDisplayName(MantisNodes.PRIORITY);

		// Add REPRODUCIBILITY field
		ObjectRef[] reproducibilities = null;
		logger.info("Adding " + MantisNodes.REPRODUCIBILITY);
		reproducibilities = mantisSoapClient.getEnumReproducibilities();

		ListFieldSchema listField4 = entSchema.addListField(MantisNodes.REPRODUCIBILITY, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < reproducibilities.length; i++) {
			listField4.addValue(reproducibilities[i].getName());
		}
		listField4.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField4.setReadonly(false);
		listField4.setDisplayName(MantisNodes.REPRODUCIBILITY);

		// Add SEVERITY field
		ObjectRef[] severities = null;
		logger.info("Adding " + MantisNodes.SEVERITY);
		severities = mantisSoapClient.getEnumSeverities();
	
		ListFieldSchema listField5 = entSchema.addListField(MantisNodes.SEVERITY, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < severities.length; i++) {
			listField5.addValue(severities[i].getName());
		}
		listField5.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField5.setReadonly(false);
		listField5.setDisplayName(MantisNodes.SEVERITY);

		// Add STATUS field
		ObjectRef[] status = null;
		logger.info("Adding " + MantisNodes.STATUS);
		status = mantisSoapClient.getEnumStatus();
	
		ListFieldSchema listField6 = entSchema.addListField(MantisNodes.STATUS, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < status.length; i++) {
			listField6.addValue(status[i].getName());
		}
		listField6.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField6.setReadonly(false);
		listField6.setDisplayName(MantisNodes.STATUS);

		// Add SUMMARY field
		logger.info("Adding " + MantisNodes.SUMMARY);
		field = entSchema.addField(MantisNodes.SUMMARY, FieldType.STRING);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.SUMMARY);

		// Add RESOLUTION field
		ObjectRef[] resolutions = null;
		logger.info("Adding " + MantisNodes.RESOLUTION);
		resolutions = mantisSoapClient.getEnumResolutions();
		ListFieldSchema listField7 = entSchema.addListField(MantisNodes.RESOLUTION, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < resolutions.length; i++) {
			listField7.addValue(resolutions[i].getName());
		}
		listField7.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField7.setReadonly(false);
		listField7.setDisplayName(MantisNodes.RESOLUTION);

		// Add PLATEFORM field
		logger.info("Adding " + MantisNodes.PLATEFORM);
		field = entSchema.addField(MantisNodes.PLATEFORM, FieldType.STRING);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.PLATEFORM);

		// Add OS field
		logger.info("Adding " + MantisNodes.OS);
		field = entSchema.addField(MantisNodes.OS, FieldType.STRING);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.OS);

		// Add OS_VERSION field
		logger.info("Adding " + MantisNodes.OS_VERSION);
		field = entSchema.addField(MantisNodes.OS_VERSION, FieldType.STRING);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.OS_VERSION);

		// Add ADDITIONAL_INFOS field
		logger.info("Adding " + MantisNodes.ADDITIONAL_INFOS);
		field = entSchema.addField(MantisNodes.ADDITIONAL_INFOS, FieldType.MEMO);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.ADDITIONAL_INFOS);

		// Add CATEGORIES field
		String[] categories = null;
		logger.info("Adding " + MantisNodes.CATEGORY);
		categories = mantisSoapClient.getEnumCategories();
		ListFieldSchema listField8 = entSchema.addListField(MantisNodes.CATEGORY, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < categories.length; i++) {
			listField8.addValue(categories[i]);
		}
		listField8.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField8.setReadonly(false);
		listField8.setDisplayName(MantisNodes.CATEGORY);

		// Add VERSION field
		ProjectVersionData[] versions = null;
		logger.info("Adding " + MantisNodes.VERSION);
		versions = mantisSoapClient.getVersions();
		ListFieldSchema listField11 = entSchema.addListField(MantisNodes.VERSION, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < versions.length; i++) {
			listField11.addValue(versions[i].getName());
		}
		listField11.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField11.setReadonly(false);
		listField11.setDisplayName(MantisNodes.VERSION);

		// Add TARGET_VERSION field
		ListFieldSchema listField12 = entSchema.addListField(MantisNodes.TARGET_VERSION, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < versions.length; i++) {
			listField12.addValue(versions[i].getName());
		}
		listField12.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField12.setReadonly(false);
		listField12.setDisplayName(MantisNodes.TARGET_VERSION);

		// Add FIXED_IN_VERSION field
		ListFieldSchema listField13 = entSchema.addListField(MantisNodes.FIXED_IN_VERSION, FieldType.SINGLE_VAL_LIST);
		for (int i = 0; i < versions.length; i++) {
			listField13.addValue(versions[i].getName());
		}
		listField13.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		listField13.setReadonly(false);
		listField13.setDisplayName(MantisNodes.FIXED_IN_VERSION);

		// Add ATTACHMENT field
		logger.info("Adding " + MantisNodes.ATTACHMENT);
		field = entSchema.addField(MantisNodes.ATTACHMENT, FieldType.ATTACHMENT);
		field.setRequiredLevel(FieldRequiredLevel.OPTIONAL);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.ATTACHMENT);

		// Add STEPS TO REPRODUCE field
		logger.info("Adding " + MantisNodes.STEPS_TO_REPRODUCE);
		field = entSchema.addField(MantisNodes.STEPS_TO_REPRODUCE, FieldType.MEMO);
		field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
		field.setReadonly(false);
		field.setDisplayName(MantisNodes.STEPS_TO_REPRODUCE);
	}

	// construction du EntitySchemaBuilder : ajout des champs custom > mapping de l'IHM
	public static void buildEntitySchemaWithCustomFields(EntitySchemaBuilder entSchema, FieldSchema f,
			AdapterLogger logger, MantisSoapGossipClient mantisSoapClient) {

		FieldSchema field = f;
		CustomFieldDefinitionData[] custom_fields = null;

		custom_fields = mantisSoapClient.getCustomFields();
	
		for (CustomFieldDefinitionData custom_field : custom_fields) {
			// on récupère le type/classe du custom field
			BigInteger type = custom_field.getType();
			FieldType field_type = getCustomFieldType(custom_field);

			String name = custom_field.getField().getName();
			name = MantisUtils.escapeSpecialsCharacters(name);
			name = "[custom] " + name;
			logger.info("Adding " + name + " > " + type);

			if (field_type.equals(FieldType.SINGLE_VAL_LIST)) {
				String possibles_values = custom_field.getPossible_values();
				String[] parts = possibles_values.split(Pattern.quote("|"));
				ListFieldSchema listField_custom = entSchema.addListField(name, FieldType.SINGLE_VAL_LIST);
				for (int i = 0; i < parts.length; i++) {
					if (parts[i] != null && !StringUtils.isEmpty(parts[i].trim())) {
						listField_custom.addValue(parts[i]);
					}
				}
				listField_custom.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
				listField_custom.setReadonly(false);
				field.setMaxLength(getFieldLength(field_type));
				listField_custom.setDisplayName(name);
			} else {
				field = entSchema.addField(name, field_type);
				field.setRequiredLevel(FieldRequiredLevel.RECOMMENDED);
				field.setReadonly(false);
				field.setDisplayName(name);
				field.setMaxLength(getFieldLength(field_type));

			}
		}
	}

	private static int getFieldLength(FieldType field_type) {
		int length;
		if (field_type.equals(FieldType.NUMBER)) {
			length = 5;
		} else
			length = 255;
		return length;
	}

	private static FieldType getCustomFieldType(CustomFieldDefinitionData field) {
		FieldType field_type = null;
		BigInteger type = field.getType();
		String possibles_values = field.getPossible_values();
		String[] parts = possibles_values.split(Pattern.quote("|"));
		// Si le custom field a plusieurs valeurs possible > List
		if (parts.length > 1) {
			field_type = FieldType.SINGLE_VAL_LIST;
		}
		/*
		 * id: 0 / name: Chaîne de caractères 
		 * id: 1 / name: Nombre entier 
		 * id: 2 / name: Nombre réel 
		 * id: 3 / name: Énumération 
		 * id: 4 / name: Courriel
		 * id: 5 / name: Case à cocher 
		 * id: 6 / name: Liste 
		 * id: 7 / name: Liste à sélection multiple 
		 * id: 8 / name: Date 
		 * id: 9 / name: Bouton radio
		 */
		else if (type.equals(new BigInteger("0"))) {
			field_type = FieldType.STRING;
		} else if (type.equals(new BigInteger("1"))) {
			field_type = FieldType.NUMBER;
		} else if (type.equals(new BigInteger("2"))) {
			field_type = FieldType.DOUBLE;
		} else if (type.equals(new BigInteger("3"))) {
			field_type = FieldType.SINGLE_VAL_LIST;
		} else if (type.equals(new BigInteger("4"))) {
			field_type = FieldType.STRING;
		} else if (type.equals(new BigInteger("5"))) {
			field_type = FieldType.STRING;
		} else if (type.equals(new BigInteger("6"))) {
			field_type = FieldType.SINGLE_VAL_LIST;
		} else if (type.equals(new BigInteger("7"))) {
			field_type = FieldType.SINGLE_VAL_LIST;
		} else if (type.equals(new BigInteger("8"))) {
			field_type = FieldType.DATE;
		} else if (type.equals(new BigInteger("9"))) {
			field_type = FieldType.STRING;
		}
		return field_type;
	}

	// Parse un champ MEMO ALM > positionne des en-tête ALM ou Mantis
	public static IssueNoteData[] parseALMMemoForMantisNotes(String note) {
		IssueNoteData[] issue_tab = new IssueNoteData[] {};
		List<IssueNoteData> issueNoteList = new ArrayList<IssueNoteData>();
		if ((note != null) && (!note.equals(""))) {
			String[] array = note.split(SEPARATOR, -1); // Parsing
			for (String str : array) {
				String[] lines = str.split(System.getProperty("line.separator"));
				if (str.contains(HEADER_MANTICO)) {
					// remplace l'entete créé par les insert Mantis par une ligne vide
					for (int i = 0; i < lines.length; i++) {
						if (lines[i].startsWith(HEADER_MANTICO) || lines[i].startsWith("> Date: ")
								|| lines[i].startsWith("> User:")) {
							lines[i] = "";
						}
					}
				}
				// Si aucun header déjà présent > il s'agit d'une nouvelle note venant d'ALM
				else if (!str.contains(HEADER_MANTICO) && !str.contains(HEADER_ALM)) {
					str = HEADER_ALM + "\n " + str;
					lines = str.split(System.getProperty("line.separator"));
				}
				// suppression des lignes vides
				ArrayList<String> l = new ArrayList<String>();
				for (String s : lines)
					if (StringUtils.isNotEmpty(s.trim())) {
						l.add(s);
					}
				lines = l.toArray(new String[l.size()]);
				// ajoute carriage return
				StringBuilder builder = new StringBuilder();
				for (String s : lines) {
					builder.append(s);
					builder.append("\n");
				}
				str = builder.toString();
				IssueNoteData noteData = new IssueNoteData();
				noteData.setText(str);
				issueNoteList.add(noteData);
			}
			issue_tab = issueNoteList.toArray(new IssueNoteData[issueNoteList.size()]);
		}
		return issue_tab;
	}

	// formattage des notes à pousser côté ALM
	public static String formatMantisNoteForALM(IssueData issue) {
		String result = "";
		IssueNoteData[] notes = issue.getNotes();
		int count = 1;
		for (IssueNoteData note : notes) {
			if (note.getText().contains(HEADER_ALM)) {
				String note_str = note.getText();
				String[] lines = note_str.split("\n");
				// remplace l'entete créé par les insert ALM par une ligne vide
				for (int i = 0; i < lines.length; i++) {
					if (lines[i].startsWith(HEADER_ALM)) {
						lines[i] = "";
					}
				}
				// suppression des lignes vides
				ArrayList<String> l = new ArrayList<String>();
				for (String s : lines)
					if (StringUtils.isNotEmpty(s.trim())) {
						l.add(s);
					}
				lines = l.toArray(new String[l.size()]);
				// ajoute carriage return
				StringBuilder builder = new StringBuilder();
				builder.append("\n");
				for (String s : lines) {
					builder.append(s);
					builder.append("\n");
				}
				String str = builder.toString();
				String close_note = "";
				if (count != notes.length) {
					close_note = SEPARATOR + "\n";
				}
				result = result + str + close_note;
			} else {
				// ajout d'un header pour identifier dans ALM si la note vient
				// de mantis
				String note_str = note.getText();
				// formattage
				String[] lines = note_str.split("\n");
				// suppression des lignes vides
				ArrayList<String> l = new ArrayList<String>();
				for (String s : lines)
					if (StringUtils.isNotEmpty(s.trim())) {
						l.add(s);
					}
				lines = l.toArray(new String[l.size()]);
				// ajoute carriage return
				StringBuilder builder = new StringBuilder();
				builder.append("\n");
				for (String s : lines) {
					builder.append(s);
					builder.append("\n");
				}
				String str = builder.toString();

				String header = "\n" + HEADER_MANTICO;
				Calendar cal = note.getDate_submitted();
				SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
				String date = "\n> Date: " + sdf.format(cal.getTime());
				String user = "\n> User: " + note.getReporter().getName();
				String note_text = "\n" + str;
				String close_note = "";
				if (count != notes.length) {
					close_note = SEPARATOR + "\n";
				}
				result = result + header + date + user + note_text + close_note;
			}
			count++;
		}
		return result;
	}

	public static Object findInRecordFields(Map<String, Object> recordFields, String fieldName) {
		Object val = null;
		if (recordFields.containsKey(fieldName)) {
			val = recordFields.get(fieldName);
		}
		return val;
	}
}
