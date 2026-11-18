package net.ideahut.springboot.template.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import net.ideahut.springboot.api.ApiAccessInternal;
import net.ideahut.springboot.api.ApiHandler;
import net.ideahut.springboot.api.ApiHandlerImpl;
import net.ideahut.springboot.api.ApiSupport;
import net.ideahut.springboot.api.ApiSupportImpl;
import net.ideahut.springboot.api.ApiTokenSysParam;
import net.ideahut.springboot.api.ApiTokenSysParamImpl;
import net.ideahut.springboot.api.WebFluxApiService;
import net.ideahut.springboot.api.WebFluxApiServiceImpl;
import net.ideahut.springboot.definition.ApiDefinition;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.module.ModuleApi;
import net.ideahut.springboot.redis.RedisParam;
import net.ideahut.springboot.rest.RestHandler;
import net.ideahut.springboot.serializer.BinarySerializer;
import net.ideahut.springboot.sysparam.SysParamHandler;
import net.ideahut.springboot.task.TaskHandler;
import net.ideahut.springboot.template.app.AppConstant;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.service.AuthService;

@Configuration
class ApiConfig {
	
	/*
	 * API HANDLER (CRUD & REQUEST)
	 */
	@Bean
	ApiHandler apiHandler(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		BinarySerializer binarySerializer,
		@Qualifier(AppConstant.Bean.Redis.PRIMARY)
		RedisTemplate<String, byte[]> redisTemplate,
		@Qualifier(AppConstant.Bean.Task.PRIMARY)
		TaskHandler taskHandler,
		ApiTokenSysParam apiTokenSysParam,
		RestHandler restHandler
	) {
		ApiDefinition.Handler handler = ObjectHelper.callOrElse(
			appProperties.getApi() != null && appProperties.getApi().getHandler() != null, 
			() -> appProperties.getApi().getHandler(), 
			ApiDefinition.Handler::new
		);
		
		ApiHandlerImpl bean = new ApiHandlerImpl()
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// Jumlah thread pada saat reload bean (menyiapkan data crud & request mapping), default: 3 thread
		.setConfigureThreads(handler.getConfigureThreads())
		
		// Default rest timeout
		.setDefaultRestTimeout(handler.getDefaultRestTimeout())
		
		// Flag apakah bisa menerima request dari Api Source / Provider lain
		.setEnableConsumer(handler.getEnableConsumer())
		
		// Flag apakah Api Crud aktif atau tidak
		.setEnableCrud(handler.getEnableCrud())
		
		// Flag apakah data Crud & Request Mapping di database dihapus jika tidak tersedia di aplikasi
		.setEnableSync(handler.getEnableSync())
		
		// Daftar Entity class dan nama trxManager yang terkait dengan ApiHandler
		// default semua class di package net.ideahut.springboot.api.entity
		.setEntityClass(null)
		
		// EntityTrxManager
		.setEntityTrxManager(entityTrxManager)
		
		// Custom function yang akan dieksekusi jika enableSync = true
		// secara default sudah dihandle
		.setOnSyncEntities(null)
		
		// RedisTemplate dan definisi penamaan key-nya
		.setRedisParam(
			new RedisParam<String, byte[]>(handler.getCacheKeyParam())
			.setRedisTemplate(redisTemplate)
		)
		
		// Optional, jika bean 'RequestMappingHandlerMapping' lebih dari satu atau namanya berbeda 
		.setRequestMappingHandlerMappingBeanName(null)
		
		// RestHandler yang akan mengirim rest request
		.setRestHandler(restHandler)
		
		// TaskHandler, untuk asinkronus
		.setTaskHandler(taskHandler);
		
		// Simpan ApiToken ke SysParam, jika defaultApiTokenStorage tidak diset
		// systemCode = API_TOKEN dan ParamCode harus diisi terlebih dahulu dengan ApiName yang tokennya akan disimpan
		ObjectHelper.callOrElse(
			handler.getApiTokenSysParamClass() != null, 
			() -> bean.setApiTokenSysParamClass(handler.getApiTokenSysParamClass()), 
			() -> bean.setApiTokenSysParamObject(apiTokenSysParam)
		);
		
		// Untuk menyimpan ApiToken, jika null akan disimpan ke SysParam
		bean
		.setDefaultApiTokenStorageClass(handler.getDefaultApiTokenStorageClass())
		.setDefaultApiTokenStorageObject(null);
		
		// Untuk custom RestClient, seperti menambahkan sertifikat, timeout, pakai proxy, dll.
		// Secara default sudah ada, hanya diperlukan jika custom
		bean
		.setDefaultRestClientCreatorClass(handler.getDefaultRestClientCreatorClass())
		.setDefaultRestClientCreatorObject(null);
		
		return bean;
	}
	
	
	/*
	 * API SERVICE
	 */
	@Bean
	WebFluxApiService apiService(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		ApiHandler apiHandler,
		@Qualifier(AppConstant.Bean.Redis.ACCESS)
		RedisTemplate<String, byte[]> redisTemplate,
		ApiSupport apiSupport,
		ApiAccessInternal apiAccessInternal
	) {
		ApiDefinition.Service service = ObjectHelper.callOrElse(
			appProperties.getApi() != null && appProperties.getApi().getService() != null, 
			() -> appProperties.getApi().getService(), 
			ApiDefinition.Service::new
		);
		
		return new WebFluxApiServiceImpl()
				
		// Untuk mendapatkan ApiAccess, jika token diterbitkan oleh service ini sendiri
		.setApiAccessInternal(apiAccessInternal)
		
		// Untuk mendapatkan ApiAccess ke service yang menerbitkan token
		// Secara default sudah dihandle, fungsi ini diperlukan jika ada custom
		.setApiAccessRemote(null)
		
		// ApiHandler
		.setApiHandler(apiHandler)
		
		// ApiName, akan didaftarkan ke database di setiap service yang terhubung
		// Jika tidak diset akan digunakan ID dari application context (property 'spring.application.name')
		.setApiName(service.getApiName())
		
		// Daftar ApiRoleGate yang digunakan
		.setApiRoleGates(ModuleApi.getDefaultApiRoleGates())
		
		// Secara default sudah ada, hanya diperlukan untuk mendapatkan ApiSource custom
		// misalnya daftar ApiSource tersimpan di file
		.setApiSourceLoader(null)
		
		// Untuk menangani token, jwt, signature, dan validasi
		.setApiSupport(apiSupport)
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// Cache expiry, untuk data access & token baik yang null maupun tidak
		.setCacheExpiry(service.getCacheExpiry())
		
		// Flag untuk debug saat mendapatkan ApiAccess dan call endpoint
		.setDebug(service.getDebug())
		
		// Custom Header name
		.setHeaderName(null)
		
		// RedisTemplate dan definisi penamaan key-nya
		// Untuk menyimpan ApiAccess & ApiToken
		.setRedisParam(
			new RedisParam<String, byte[]>(service.getCacheKeyParam())
			.setRedisTemplate(redisTemplate)
		)
		
		// Flag apakah error di ApiAccess dikembalikan null atau di-throw error
		.setThrowApiAccessError(service.getThrowApiAccessError());
	}
	
	
	/*
	 * API SUPPORT
	 */
	@Bean
	ApiSupport apiSupport(
		AppProperties appProperties
	) {
		ApiDefinition.Support support = ObjectHelper.callOrElse(
			appProperties.getApi() != null && appProperties.getApi().getSupport() != null, 
			() -> appProperties.getApi().getSupport(), 
			ApiDefinition.Support::new
		);
		
		return new ApiSupportImpl()
		
		// Untuk membuat ApiToken dari sisi server di ApiProviderGate.
		.setApiConsumerJwtParam(support.getApiConsumerJwtParam())
		
		// Untuk membuat token di ApiRoleGate
		.setApiRoleGateJwtParam(support.getApiRoleGateJwtParam())
		
		// Batas atas & bawah timestamp yang dikirim oleh klien
		// Contoh: jika diset 1 menit, berarti request yang dikirim valid jika timestamp client dalam range -+ 1 menit
		.setApiSourceGateTimeSpan(support.getApiSourceGateTimeSpan())
		
		// index redisTemplate untuk meyimpan consumer token jika menggunakan MultipleRedisTemplate, default index 1
		.setApiSourceGateTokenStorageIndex(support.getApiSourceGateTokenStorageIndex());
	}
	
	
	/*
	 * API TOKEN SYSPARAM
	 */
	@Bean
	ApiTokenSysParam apiTokenSysParam(
		SysParamHandler sysParamHandler
	) {
		return new ApiTokenSysParamImpl()
		.setSysParamHandler(sysParamHandler);
	}
	
	
	/*
	 * API ACCESS INTERNAL
	 */
	@Bean
	ApiAccessInternal apiAccessInternal(
		AuthService authService
	) {
		return authService::getApiAccessForInternal;
	}
	
}

