package net.ideahut.springboot.template.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import net.ideahut.springboot.api.ApiAccess;
import net.ideahut.springboot.api.ApiAuth;
import net.ideahut.springboot.api.ApiHeaderValue;
import net.ideahut.springboot.api.ApiParameter;
import net.ideahut.springboot.api.ApiProcessor;
import net.ideahut.springboot.api.ApiRequest;
import net.ideahut.springboot.api.ApiSource;
import net.ideahut.springboot.api.ApiUser;
import net.ideahut.springboot.api.WebFluxApiService;
import net.ideahut.springboot.api.processor.AgentHostJwtApiProcessor;
import net.ideahut.springboot.api.processor.AgentJwtApiProcessor;
import net.ideahut.springboot.api.processor.HostJwtApiProcessor;
import net.ideahut.springboot.api.processor.StandardJwtApiProcessor;
import net.ideahut.springboot.bean.BeanConfigure;
import net.ideahut.springboot.exception.ResultRuntimeException;
import net.ideahut.springboot.helper.ErrorHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.helper.TimeHelper;
import net.ideahut.springboot.object.Message;
import net.ideahut.springboot.object.Result;
import net.ideahut.springboot.object.TimeValue;
import net.ideahut.springboot.redis.RedisCommand;
import net.ideahut.springboot.serializer.BinarySerializer;
import net.ideahut.springboot.template.app.AppConstant;
import net.ideahut.springboot.template.object.UserData;

@Service
class AuthServiceImpl implements AuthService, BeanConfigure {
	
	private static final String AUTH_PREFIX = "AUTH-";
	private static final TimeValue AUTH_EXPIRY = TimeValue.of(TimeUnit.MINUTES, 60L); // 1 jam
	private static final TimeValue ACCESS_EXPIRY = TimeValue.of(TimeUnit.HOURS, 24L); // 1 hari
	private static final List<String> JWT_PROCESSORS = Arrays.asList(
		StandardJwtApiProcessor.API_TYPE,
		AgentJwtApiProcessor.API_TYPE,
		HostJwtApiProcessor.API_TYPE,
		AgentHostJwtApiProcessor.API_TYPE
	);
	private static final String API_ROLE = "USER-FLUX";
	private static final Map<String, UserData> users;
	static {
		Map<String, UserData> usersEi = new HashMap<>();
		usersEi.put("admin", new UserData().setRoleCode("APP-ADMIN").setPassword("admin123").setUserId("1").setUsername("admin"));
		usersEi.put("user", new UserData().setRoleCode("APP-USER").setPassword("user123").setUserId("1").setUsername("user"));
		users = usersEi;
	}
	
	private final BinarySerializer binarySerializer;
	private final RedisCommand<String, byte[]> redisCommand;
	
	private WebFluxApiService apiService;
	private boolean configured = false;
	
	@Autowired
	AuthServiceImpl(
		BinarySerializer binarySerializer,
		@Qualifier(AppConstant.Bean.Redis.ACCESS)
		RedisTemplate<String, byte[]> redisTemplate
	) {
		this.binarySerializer = binarySerializer;
		this.redisCommand = RedisCommand.of(redisTemplate);
	}
	
	@Override
	public void onConfigureBean(ApplicationContext applicationContext) throws Exception {
		this.apiService = applicationContext.getBean(WebFluxApiService.class);
		configured = true;
	}

	@Override
	public boolean isBeanConfigured() {
		return configured;
	}

	@Override
	public ApiAuth login(
		ApiRequest apiRequest, 
		String username, 
		String password
	) throws Exception {
		UserData user = users.get(username);
		ErrorHelper.throwNull(user, () -> "User not found");
		ErrorHelper.throwIf(!user.getPassword().equals(password), () -> "Invalid password");
		
		String apiType = apiRequest.getHeader(apiService.getApiHeaderName().getType(), StandardJwtApiProcessor.API_TYPE);
		ApiProcessor apiProcessor = apiService.getApiProcessor(apiType);
		ErrorHelper.throwNull(apiProcessor, () -> "ApiProcessor not found");
		boolean isJwtCheck = apiRequest.getHeader(boolean.class, "Jwt-Check", false);
		boolean isJwtType = JWT_PROCESSORS.contains(apiType);
		
		Long createdOn = TimeHelper.currentEpochMillis();
		ApiAccess apiAccess = new ApiAccess()
		.setCreatedOn(createdOn)
		.setExpiredOn(createdOn + ACCESS_EXPIRY.toMillis())
		.setApiUser(new ApiUser()
			.setId(user.getUserId())
			.setUsername(user.getUsername())
			.setAttribute(ApiUser.Attribute.ROLE, user.getRoleCode())
		)
		// set appid
		.setAttribute(ApiAccess.Attribute.APP_ID, apiService.getApiName());
		ObjectHelper.callIf(isJwtType && isJwtCheck, () -> apiAccess.setAttribute("check", "true"));
		
		ApiParameter apiParameter = new ApiParameter()
		.setApiType(apiType)
		.setApiName(apiService.getApiName())
		.setApiRequest(apiRequest);
		
		ApiAuth apiAuth = apiProcessor.createApiAuth(apiParameter, apiAccess);
		byte[] bytes = binarySerializer.serialize(ApiAuth.class, apiAuth);
		redisCommand.valueSet(AUTH_PREFIX + apiAuth.getApiKey(), bytes, AUTH_EXPIRY);
		return apiAuth;
	}

	@Override
	public ApiAccess logout(
		ApiRequest apiRequest
	) {
		ApiAccess apiAccess = apiService.getApiAccess(apiRequest);
		return ObjectHelper.callIf(
			apiAccess != null, 
			() -> {
				ErrorHelper.throwIf(!isInternalApiAccess(apiAccess), () -> "Invalid ApiPublisher");
				ApiAccess theAccess = ObjectHelper.useOrDefault(apiAccess, null);
				redisCommand.keyDelete(AUTH_PREFIX + theAccess.getApiKey());
				apiService.removeApiAccess(null, theAccess.getApiKey());
				return apiAccess;
			}
		);
	}

	@Override
	public ApiAccess info(
		ApiRequest apiRequest
	) {
		ApiParameter apiParameter = apiService.getApiParameter(apiRequest);
		String apiKey = apiParameter != null ? apiParameter.getApiKey() : null;
		return ObjectHelper.callIf(
			!StringHelper.isBlank(apiKey), 
			() -> {
				byte[] bytes = redisCommand.valueGet(AUTH_PREFIX + apiKey);
				return ObjectHelper.callIf(
					bytes != null, 
					() -> {
						ApiAuth apiAuth = binarySerializer.deserialize(ApiAuth.class, bytes);
						ErrorHelper.throwIf(!isInternalApiAccess(apiAuth.getApiAccess()), () -> "Invalid ApiPublisher");
						return apiAuth.getApiAccess();
					}
				);
			}
		);
	}

	/*
	 * ApiAccess untuk request dari ApiProvider lain
	 */
	@Override
	public ApiAccess getApiAccessForExternal(
		ApiRequest apiRequest
	) {
		ApiParameter apiParameter = apiService.getApiParameter(apiRequest);
		String apiKey = ObjectHelper.useOrDefault(apiParameter.getApiKey(), "");
		ErrorHelper.throwBlank(apiKey, () -> "ApiKey required");
		ApiHeaderValue apiHeaderValue = apiRequest.getApiHeaderValue();
		String from = ObjectHelper.useOrDefault(apiHeaderValue.getFrom(), "");
		ErrorHelper.throwBlank(from, () -> StringHelper.format( "Header '{}' required", apiHeaderValue.getApiHeaderName().getFrom()));
		ApiSource apiSource = apiService.getApiSource(from);
		ErrorHelper.throwNull(apiSource, () -> "ApiSource not found");
		Message message = apiService.getApiTokenService().validateSignature(apiSource, apiHeaderValue);
		ErrorHelper.throwIf(message != null, () -> ResultRuntimeException.of(Result.error(message)));
		byte[] bytes = redisCommand.valueGet(AUTH_PREFIX + apiKey);
		return ObjectHelper.callIf(
			bytes != null, 
			() -> {
				ApiAccess apiAccess = binarySerializer.deserialize(ApiAuth.class, bytes).getApiAccess();
				return apiAccess.setApiRole(API_ROLE);
			}
		);
	}

	/*
	 * ApiAccess untuk request dari internal service
	 */
	@Override
	public ApiAccess getApiAccessForInternal(ApiParameter apiParameter) {
		String apiKey = apiParameter != null && apiParameter.getApiKey() != null ? apiParameter.getApiKey() : "";
		ErrorHelper.throwBlank(apiKey, () -> "ApiKey required");
		byte[] bytes = redisCommand.valueGet(AUTH_PREFIX + apiKey);
		return ObjectHelper.callIf(
			bytes != null, 
			() -> {
				ApiAccess apiAccess = binarySerializer.deserialize(ApiAuth.class, bytes).getApiAccess();
				return apiAccess.setApiRole(API_ROLE);
			}
		);
	}
	
	@Override
	public String createConsumerToken(ApiRequest apiRequest) {
		ApiHeaderValue apiHeaderValue = apiRequest.getApiHeaderValue();
		String from = ObjectHelper.useOrDefault(apiHeaderValue.getFrom(), "");
		ErrorHelper.throwBlank(from, () -> StringHelper.format( "Header '{}' required", apiHeaderValue.getApiHeaderName().getFrom()));
		ApiSource apiSource = apiService.getApiSource(from);
		ErrorHelper.throwNull(apiSource, () -> "ApiSource not found");
		return apiService.getApiTokenService().createConsumerApiToken(apiSource, apiRequest);
	}
	
	private boolean isInternalApiAccess(ApiAccess apiAccess) {
		return ObjectHelper.callOrElse(
			apiAccess != null, 
			() -> apiService.getApiName().equals(ObjectHelper.useOrDefault(apiAccess, null).getAttribute(ApiAccess.Attribute.APP_ID)), 
			() -> false
		);
	}

}
