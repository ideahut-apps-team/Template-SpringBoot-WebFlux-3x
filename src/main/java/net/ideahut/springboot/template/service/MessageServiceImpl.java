package net.ideahut.springboot.template.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.ideahut.springboot.bean.BeanConfigure;
import net.ideahut.springboot.bean.BeanReload;
import net.ideahut.springboot.context.RequestContext;
import net.ideahut.springboot.entity.EntityTrxManager;
import net.ideahut.springboot.helper.FrameworkHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.helper.StringHelper;
import net.ideahut.springboot.mapper.DataMapper;
import net.ideahut.springboot.message.MessageHandler;
import net.ideahut.springboot.message.RedisMessageHandler;
import net.ideahut.springboot.message.dto.LanguageDto;
import net.ideahut.springboot.object.ListString;
import net.ideahut.springboot.object.MapString;
import net.ideahut.springboot.object.Message;
import net.ideahut.springboot.object.Option;
import net.ideahut.springboot.redis.RedisCommand;
import net.ideahut.springboot.redis.RedisParam;
import net.ideahut.springboot.template.app.AppConstant;
import net.ideahut.springboot.template.app.AppProperties;

@Service
class MessageServiceImpl implements MessageService, BeanReload, BeanConfigure {
	
	private static final class Keys {
		private Keys() {}
		private static String resources(String prefix) {
			return prefix + "RESOURCES";
		}
		private static String resource(String prefix, String type, String language) {
			return prefix + "RESOURCE-" + type + "-" + language;
		}
	}

	private static final String DEFAULT_LANGUAGE = "id";
	
	private boolean configured = false;
	private RedisCommand<String, byte[]> redisCommand;
	private String redisPrefix;
	private RedisMessageHandler messageHandler;
	
	private List<Option> activeLanguages;
	private AppProperties appProperties;
	private DataMapper dataMapper;
	
	
	@SuppressWarnings("unchecked")
	@Override
	public void onConfigureBean(ApplicationContext applicationContext) throws Exception {
		appProperties = FrameworkHelper.getBean(applicationContext, AppProperties.class);
		dataMapper = FrameworkHelper.getBean(applicationContext, DataMapper.class);
		EntityTrxManager entityTrxManager = FrameworkHelper.getBean(applicationContext, EntityTrxManager.class);
		RedisTemplate<String, byte[]> redisTemplate = FrameworkHelper.getBean(applicationContext, AppConstant.Bean.Redis.PRIMARY, RedisTemplate.class);
		redisCommand = RedisCommand.of(redisTemplate);
		RedisParam<String, byte[]> redisParam = new RedisParam<String, byte[]>()
		.setAppIdEnabled(true)
		.setEncryptEnabled(true)
		.setPrefix("MESSAGE")
		.setRedisTemplate(redisTemplate)
		.prepareDefault();
		redisPrefix = FrameworkHelper.createStorageKeyPrefix(redisParam, applicationContext);
		messageHandler = new RedisMessageHandler()
		.setDefaultLanguage(DEFAULT_LANGUAGE)
		.setEntityTrxManager(entityTrxManager)
		.setLimitReloadData(100)
		.setMaxReloadThread(3)
		.setRedisParam(redisParam);
		messageHandler.afterPropertiesSet();
		messageHandler.onConfigureBean(applicationContext);
		onReloadBean();
		configured = true;
	}

	@Override
	public boolean isBeanConfigured() {
		return configured;
	}

	@Override
	public boolean onReloadBean() throws Exception {
		if (configured && !messageHandler.onReloadBean()) {
			return false;
		}
		activeLanguages = new ArrayList<>();
		for (LanguageDto language : messageHandler.getActiveLanguages().values()) {
			activeLanguages.add(new Option(language.getLanguageCode(), language.getName()));
		}
		clearResources();
		loadResources();
		return true;
	}
	
	@Override
	public List<Option> getActiveLanguages() {
		return activeLanguages;
	}

	@Override
	public String getDefaultLanguage() {
		return DEFAULT_LANGUAGE;
	}

	@Override
	public JsonNode getResource(String type) {
		String language = getRequestLanguage();
		ObjectNode node = dataMapper.createObjectNode();
		node.putArray("languages").addAll(dataMapper.convert(activeLanguages, ArrayNode.class));
		node.put("active", language);
		String ckey = Keys.resource(redisPrefix, type, language);
		byte[] bytes = redisCommand.valueGet(ckey);
		if (bytes != null) {
			node.set("message", dataMapper.read(bytes, JsonNode.class));
		}
		return node;
	}
	
	@Override
	public String getText(String code, boolean checkArgs, String... args) {
		getRequestLanguage();
		return messageHandler.getText(code, checkArgs, args);
	}

	@Override
	public String getText(String code, String... args) {
		return messageHandler.getText(code, args);
	}

	@Override
	public String getText(String code) {
		return messageHandler.getText(code);
	}

	@Override
	public Message getMessage(String code, boolean checkArgs, String... args) {
		return messageHandler.getMessage(code, checkArgs, args);
	}

	@Override
	public Message getMessage(String code, String... args) {
		return messageHandler.getMessage(code, args);
	}

	@Override
	public Message getMessage(String code) {
		return messageHandler.getMessage(code);
	}

	@Override
	public MapString getMap(String... codes) {
		return messageHandler.getMap(codes);
	}

	@Override
	public ListString getList(String... codes) {
		getRequestLanguage();
		return messageHandler.getList(codes);
	}

	
	private String getRequestLanguage() {
		RequestContext context = RequestContext.currentContext();
		String language = context.getAttribute(MessageHandler.Attribute.LANGUAGE + "-ACTIVE");
		if (!StringHelper.isBlank(language)) {
			return language;
		}
		language = context.getAttribute(MessageHandler.Attribute.LANGUAGE);
		if (!isValidLanguage(language)) {
			language = DEFAULT_LANGUAGE;
		}
		context.setAttribute(MessageHandler.Attribute.LANGUAGE + "-ACTIVE", language);
		return language;
	}
	
	private boolean isValidLanguage(String language) {
		if (StringHelper.isBlank(language)) {
			return false;
		}
		Option option = getActiveLanguages()
		.stream()
		.filter(o -> language.equals(o.getValue()))
		.findAny()
		.orElse(null);
		return option != null;
	}
	
	private Map<String, byte[]> getResources(String type) {
		Map<String, byte[]> map = new HashMap<>();
		String path = FrameworkHelper.replacePath(StringHelper.removeEnd(appProperties.getMessagePath(), "/"));
		Resource[] resources = FrameworkHelper.getResources(path + "/" + type + "/*.json");
		for (Resource resource : resources) {
			String filename = resource.getFilename();
			if (filename != null && resource.isReadable()) {
				String language = filename.replace(".json", "");
				if (isValidLanguage(language)) {
					byte[] bytes = FrameworkHelper.toByteArray(resource);
					String ckey = Keys.resource(redisPrefix, type, language);
					// support format yaml, json, & xml
					// validasi format sebelum disimpan ke redis dalam format json
					JsonNode node = FrameworkHelper.loadConfiguration(bytes, JsonNode.class);
					bytes = dataMapper.writeAsBytes(node, DataMapper.JSON);
					map.put(ckey, bytes);
				}
			}
		}
		return map;
	}
	
	private void loadResources() {
		Map<String, byte[]> cvalues = new HashMap<>();
		cvalues.putAll(getResources("mobile"));
		cvalues.putAll(getResources("portal"));
		Set<byte[]> bkeys = new LinkedHashSet<>();
		for (String key : cvalues.keySet()) {
			bkeys.add(StringHelper.toBytes(key));
		}
		if (!bkeys.isEmpty()) {
			redisCommand.listRightPush(Keys.resources(redisPrefix), bkeys);
			redisCommand.valueMultiSet(cvalues);
		}
	}
	
	private void clearResources() {
		String key = Keys.resources(redisPrefix);
		Long size = redisCommand.listSize(key);
		if (size != null && size > 0) {
			List<byte[]> bkeys = redisCommand.listLeftPop(key, ObjectHelper.useOrDefault(size, 0L));
			if (bkeys != null && !bkeys.isEmpty()) {
				List<String> skeys = bkeys.stream().map(StringHelper::toString).toList();
				redisCommand.keyDelete(skeys);
			}
		}
	}

}
