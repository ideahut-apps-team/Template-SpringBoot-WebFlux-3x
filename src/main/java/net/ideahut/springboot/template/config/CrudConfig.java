package net.ideahut.springboot.template.config;

import java.util.Set;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import net.ideahut.springboot.api.ApiAccess;
import net.ideahut.springboot.api.ApiService;
import net.ideahut.springboot.crud.CrudAction;
import net.ideahut.springboot.crud.CrudHandler;
import net.ideahut.springboot.crud.CrudHandlerImpl;
import net.ideahut.springboot.crud.CrudPermission;
import net.ideahut.springboot.crud.CrudProperties;
import net.ideahut.springboot.crud.CrudResource;
import net.ideahut.springboot.definition.CrudDefinition;
import net.ideahut.springboot.entity.EntityInfo;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.entity.TrxManagerInfo;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.mapper.DataMapper;
import net.ideahut.springboot.template.Application;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.support.CrudSupport;

@Configuration
class CrudConfig {
	
	/*
	 * CRUD HANDLER
	 */
	@Bean
	CrudHandler crudHandler(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		DataMapper dataMapper
	) {
		CrudDefinition crud = ObjectHelper.useOrDefault(
			appProperties.getCrud(), 
			CrudDefinition::new
		);
		
		return new CrudHandlerImpl()
				
		// semua query menggunakan sql (native) atau tidak
		.setAlwaysUseNative(crud.getAlwaysUseNative())
		
		// default maksimum jumlah data saat retrieve (PAGE, LIST, MAP)
		.setDefaultMaxLimit(crud.getDefaultMaxLimit())
		
		// EntityTrxManager
		.setEntityTrxManager(entityTrxManager)
		
		// Informasi aksi disertakan di respon atau tidak
		.setInfoEnabled(crud.getInfoEnabled())
		
		// Daftar filter specific yang akan disertakan saat query
		.setSpecificValueGetters(CrudSupport.getSpecificValueGetters())
		
		// Flag apakah bulk diaktifkan atau tidak
		.setBulkEnabled(crud.getBulkEnabled())
		
		// Maksimum jumlah operasi / aksi CRUD yang dibolehkan, diisi 0 untuk tak terbatas
		.setMaxBulkSize(crud.getMaxBulkSize())
		
		// Maksimum jumlah dependensi / layer CRUD yang dibolehkan, diisi 0 untuk tak terbatas
		.setMaxBulkLayer(crud.getMaxBulkLayer());
		
	}
	
	/*
	 * CRUD RESOURCE
	 */
	@Bean
	CrudResource crudResource(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		ApiService apiService
	) {
		CrudDefinition crud = ObjectHelper.useOrDefault(
			appProperties.getCrud(), 
			CrudDefinition::new
		);
		
		if (Boolean.TRUE.equals(crud.getEnableApiService())) {
			 // CrudResource  diambil menggunakan ApiService (PRODUCTION)
			 // - Parameter manager yang didefinisikan di CrudRequest tidak akan digunakan, karena sudah ada di table
			 // - Parameter name = crudCode
			return (manager, name) -> {
				ApiAccess apiAccess = ApiAccess.fromContext();
				CrudProperties properties = apiService.getApiCrudProperties(apiAccess, name);
				ErrorHelper.throwNull(properties, () -> StringHelper.format("CrudProperties not found, name: {}", name));
				return properties;
			};
		} else {
			// CrudResource berdasarkan nama class yang didefinisikan di CrudRequest (DEVELOPMENT)
			return (manager, name) -> {
				Class<?> clazz = ObjectHelper.safeClassOf(Application.Package.APPLICATION + ".entity." + name);
				ErrorHelper.throwNull(clazz, () -> StringHelper.format("Entity class not found, name: {}", name));
				TrxManagerInfo trxManagerInfo = TrxManagerInfo.getTrxManagerInfo(entityTrxManager, manager);
				ErrorHelper.throwNull(trxManagerInfo, () -> StringHelper.format("TrxManagerInfo not found, manager: {}", manager));
				EntityInfo entityInfo = trxManagerInfo.getEntityInfo(clazz);
				return new CrudProperties()
				.setEntityInfo(entityInfo)
				.setMaxLimit(crud.getDefaultMaxLimit())
				.setUseNative(crud.getAlwaysUseNative());
			};
		}
		
	}
	
	/*
	 * CRUD PRERMISSION
	 */
	@Bean
	CrudPermission crudPermission(
		AppProperties appProperties	
	) {
		CrudDefinition crud = ObjectHelper.useOrDefault(
			appProperties.getCrud(), 
			CrudDefinition::new
		);
		
		if (!Boolean.FALSE.equals(crud.getEnablePermission())) {
			// Cek berdasarkan action (CREATE, UPDATE, DELETE, dll)
			return (action, request) -> {
				CrudProperties properties = request.getProperties();
				Set<CrudAction> actions = properties.getActions();
				return actions != null && actions.contains(action);
			};
		} else {
			// Semua request diijinkan
			return (action, request) -> true;
		}
		
	}
	
}
