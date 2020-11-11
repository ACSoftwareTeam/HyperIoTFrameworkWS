/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * Written by Gene <generoso.martello@acsoftware.it>, March 2019
 *
 */
package it.acsoftware.hyperiot.kafka.connector.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate KafkaMQTTConnector Actions
 *
 */
public enum KafkaConnectorAction implements HyperIoTActionName {
	
	// TODO: add enumerations here
	ADMIN_KAFKA_TOPICS_ADD("admin_kafka_topics_add"),
	ADMIN_KAFKA_TOPICS_UPDATE("admin_kafka_topics_alter"),
	ADMIN_KAFKA_TOPICS_DELETE("admin_kafka_topics_delete"),
	ADMIN_KAFKA_TOPICS_METRICS("admin_kafka_topics_metrics"),
	ADMIN_KAFKA_ACL_ADD("admin_kafka_acl_add"),
	ADMIN_KAFKA_ACL_UPDATE("admin_kafka_acl_update"),
	ADMIN_KAFKA_ACL_DELETE("admin_kafka_acl_add"),
	ADMIN_KAFKA_CONNECTOR_NEW("admin_kafka_connector_new"),
	ADMIN_KAFKA_CONNECTOR_DELETE("admin_kafka_connector_delete"),
	ADMIN_KAFKA_CONNECTOR_VIEW("admin_kafka_connector_view"),
	ADMIN_KAFKA_CONNECTOR_LIST("admin_kafka_connector_list"),
	ADMIN_KAFKA_CONNECTOR_UPDATE("admin_kafka_connector_update");

	private String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the KafkaConnector  action
	 */
	KafkaConnectorAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of KafkaConnector action
	 */
	public String getName() {
		return name;
	}

}
