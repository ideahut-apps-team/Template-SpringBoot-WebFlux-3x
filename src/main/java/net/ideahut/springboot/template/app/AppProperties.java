package net.ideahut.springboot.template.app;

import java.io.Serializable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import net.ideahut.springboot.definition.AdminDefinition;
import net.ideahut.springboot.definition.AgroalDefinition;
import net.ideahut.springboot.definition.ApiDefinition;
import net.ideahut.springboot.definition.CacheGroupDefinition;
import net.ideahut.springboot.definition.CrudDefinition;
import net.ideahut.springboot.definition.DatabaseAuditDefinition;
import net.ideahut.springboot.definition.DatasourceDefinition;
import net.ideahut.springboot.definition.DbcpDefinition;
import net.ideahut.springboot.definition.FilterDefinition;
import net.ideahut.springboot.definition.GridDefinition;
import net.ideahut.springboot.definition.HibernateDefinition;
import net.ideahut.springboot.definition.HikariDefinition;
import net.ideahut.springboot.definition.KafkaDefinition;
import net.ideahut.springboot.definition.LauncherDefinition;
import net.ideahut.springboot.definition.RestDefinition;
import net.ideahut.springboot.entity.EntityForeignKeyParam;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.mail.MailProperties;
import net.ideahut.springboot.redis.RedisProperties;
import net.ideahut.springboot.task.TaskProperties;

/*
 * Class properties yang definisinya sama dengan application.yaml
 */
@Configuration
@ConfigurationProperties(prefix = AppProperties.PREFIX)
@Setter
@Getter
public class AppProperties {
	
	public static final String PREFIX =  "config";
	
	// Base URL untuk diakses public
	private String publicBaseUrl;
	
	// Log semua error yang terjadi
	private Boolean logAllError;
	
	// Binary serializer
	private String binarySerializer;
	
	// Start scheduler pada saat startup
	private Boolean autoStartScheduler;
	
	// Direktori file message berdasarkan bahasa
	private String messagePath;
	
	// Lokasi file report (jrxml / jasper)
	private String reportPath;
	
	// Launcher log, bean configure, & init
	private LauncherDefinition launcher;
	
	// Definisi headers (cors), result time, & trace (log)
	private FilterDefinition filter;
	
	// Parameter untuk menghandle anotasi @ForeignKeyEntity
	// Ini solusi jika terjadi error saat membuat native image dimana entity memiliki @ManyToOne & @OneToMany
	// tapi package-nya berbeda dengan package project (error ByteCodeProvider saat runtime)
	private EntityForeignKeyParam foreignKey;
	
	// CRUD
	private CrudDefinition crud;
	
	// Audit
	private DatabaseAuditDefinition audit;
	
	// Task Handler
	private Task task;
	
	// Redis
	private Redis redis;
	
	// Admin
	private AdminDefinition admin;
	
	// API
	private ApiDefinition api;
	
	// Rest
	private RestDefinition rest;
	
	// Cache
	private CacheGroupDefinition cache;
	
	// Grid
	private GridDefinition grid;
	
	// Mail
	private MailProperties mail;
	
	// Kafka
	private KafkaDefinition kafka;
	
	// TrxManager
	private TrxManager trxManager;
	
	
	@Setter
	@Getter
	public static class Redis {
		private RedisProperties.Connection primary;
		private RedisProperties.Connection access;
	}
	
	@Setter
	@Getter
	public static class Task {
		private TaskProperties primary;
		private TaskProperties audit;
		private TaskProperties rest;
		private TaskProperties webAsync;
	}
	
	@Getter
	@Setter
	public static class TrxDatasource implements Serializable {
		private static final long serialVersionUID = -593375044933457535L;
		public enum Type {
			BASIC,
			HIKARI,
			AGROAL,
			DBCP
		}
		private Type type;
		private DatasourceDefinition basic;
		private HikariDefinition hikari;
		private AgroalDefinition agroal;
		private DbcpDefinition dbcp;
		public static DatasourceDefinition getDefinition(TrxDatasource datasource) {
			if (datasource != null) {
				Type type = ObjectHelper.useOrDefault(datasource.getType(), () -> Type.BASIC);
				switch (type) {
				case HIKARI:
					return datasource.getHikari();
				case AGROAL:
					return datasource.getAgroal();
				case DBCP:
					return datasource.getDbcp();
				default:
					return datasource.getBasic();
				}
			}
			return null;
		}
	}
	
	@Setter
	@Getter
	public static class TrxAudit extends HibernateDefinition {
		private static final long serialVersionUID = 4540326408526410239L;
		private String id;
		private TrxDatasource datasource;
	}
	
	@Setter
	@Getter
	public static class TrxMain extends HibernateDefinition {
		private static final long serialVersionUID = -4930835621825241465L;
		private TrxDatasource datasource;
		private TrxAudit audit;
	}
	
	@Setter
	@Getter
	public static class TrxManager {
		private TrxMain primary;
		private TrxMain secondary;
	}
	
}
