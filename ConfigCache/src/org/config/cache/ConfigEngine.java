package org.config.cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.config.cache.core.IConfig;
import org.config.cache.core.IDecoder;
import org.config.cache.core.IReader;
import org.config.cache.decode.text.AreaDecoder;
import org.config.cache.decode.text.BuildingDecoder;
import org.config.cache.decode.text.BuildingPositionDecoder;
import org.config.cache.decode.text.CityDecoder;
import org.config.cache.decode.text.CityRouteDecoder;
import org.config.cache.decode.text.CityTypeDecoder;
import org.config.cache.decode.text.CountryDecoder;
import org.config.cache.decode.text.DropConfigDecoder;
import org.config.cache.decode.text.GlobalConfigDecoder;
import org.config.cache.decode.text.HeroDecoder;
import org.config.cache.decode.text.ItemDecoder;
import org.config.cache.decode.text.ItemExtendDecoder;
import org.config.cache.decode.text.MissionDecoder;
import org.config.cache.decode.text.MonsterGroupDecoder;
import org.config.cache.decode.text.MonsterRefreshDecoder;
import org.config.cache.decode.text.RoleLevelDecoder;
import org.config.cache.decode.text.ShopItemDecoder;
import org.config.cache.exception.SimpleConfigException;
import org.config.cache.parser.TextListParser;
import org.config.cache.parser.TextMapParser;
import org.config.cache.reader.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 添加一个注册表demo，需要进行如下操作
 * 
 * 1、定义一个配置实体DemoConfig，实现IConfig接口，对应于配置表中每一项
 * 2、定义一个行解析器DemoTextDecoder,实现IDecoder接口，完成对demo表每一行的解析
 * 3、在{ConfigType}枚举中增加一个新项，对应值为配置表的名称
 * 4、在{ConfigEngine}的registerAll 方法中进行注册
 * 
 * @author chenjie
 * 2012-12-10
 */
public final class ConfigEngine {
	
	private static final Logger logger = LoggerFactory.getLogger(ConfigEngine.class);
	
	public static final String TYPE_JSON = "json";
	public static final String TYPE_TEXT = "text";
	
	public static final String DEFAULT_TEXT_DELIM = "\t";
	
	public static final String CONFIG_FOLDER_DIR = "file:///c:/cache/"; //配置文件所在目录
	
	private static class InstanceHolder{
		protected static ConfigEngine instance = new ConfigEngine();
	}
	
	private Map<ConfigType, String> configUrls; //对于有唯一键的用Map缓存

	private Map<ConfigType, IDecoder<IConfig>> decoders;
	
	private ConfigEngine(){}
	
	public static final ConfigEngine getInstance(){
		
		return InstanceHolder.instance;
	}
	
	/**
	 * 初始化
	 */
	public void init(){
		
		this.configUrls = new HashMap<ConfigType, String>();
		this.decoders = new HashMap<ConfigType, IDecoder<IConfig>>();
		this.registerAll();
	}
	
	/**
	 * 注册所有配置表
	 */
	private void registerAll(){
		
		this.register(ConfigType.GLOBAL_CONFIG, GlobalConfigDecoder.class);
		this.register(ConfigType.DROP, DropConfigDecoder.class);
		this.register(ConfigType.MONSTER_GROUP, MonsterGroupDecoder.class);
		this.register(ConfigType.MONSTER_REFRESH, MonsterRefreshDecoder.class);
		this.register(ConfigType.SHOP, ShopItemDecoder.class);
		this.register(ConfigType.MISSION, MissionDecoder.class);
		this.register(ConfigType.ROLE_LEVEL, RoleLevelDecoder.class);
		this.register(ConfigType.COUNTRY, CountryDecoder.class);
		this.register(ConfigType.BUILDING_POSITION, BuildingPositionDecoder.class);
		this.register(ConfigType.CITY_ROUTE, CityRouteDecoder.class);
		this.register(ConfigType.AREA, AreaDecoder.class);
		this.register(ConfigType.CITY, CityDecoder.class);
		this.register(ConfigType.CITY_TYPE, CityTypeDecoder.class);
		this.register(ConfigType.ITEM, ItemDecoder.class);
		this.register(ConfigType.BUILDING, BuildingDecoder.class);
		this.register(ConfigType.ITEMEXTEND, ItemExtendDecoder.class);
		this.register(ConfigType.HERO, HeroDecoder.class);
	}
	
	/**
	 * 获取所有配置项，返回的是
	 * @param configType:配置表类型
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends IConfig> Map<String, T> getConfigMap(ConfigType configType){
		
		IDecoder<T> decoder = (IDecoder<T>) this.decoders.get(configType);
		
		if(decoder == null){
			logger.error(String.format("The decoder of %s is not registered in the ConfigEngine", configType.toString()));
			return null;
		}
		
		String url = this.configUrls.get(configType);
		
		
		Map<String, T> items = this.readTextMap(url, decoder);
		
		return items;
	}
	
	/**
	 * 获取所有配置项
	 * @param configType
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends IConfig> List<T> getConfigList(ConfigType configType){
		
		IDecoder<T> decoder = (IDecoder<T>) this.decoders.get(configType);
		
		if(decoder == null){
			logger.error(String.format("The decoder of %s is not registered in the ConfigEngine", configType.toString()));
			return null;
		}
		
		String url = this.configUrls.get(configType);
		
		
		List<T> items = this.readTextList(url, decoder);
		
		return items;
	}
	
	
	/**
	 * 注册一个配置表，使用默认的地址
	 * @param type
	 * @param clazz
	 * @param decoder
	 */
	private final <T extends IConfig> void register(ConfigType type, Class<?> decoder){
		
		final String url = CONFIG_FOLDER_DIR + type.getValue()+".txt";
		
		this.register(type, decoder, url);
	}
	
	/**
	 * 注册一个配置表
	 * @param type
	 * @param clazz
	 * @param decoder
	 * @param url
	 */
	@SuppressWarnings("unchecked")
	private final <T extends IConfig> void register(ConfigType type, Class<?> decoder, String url){
		
		if(!this.decoders.containsKey(type)){
			
			try {
				
				IDecoder<IConfig> decode = (IDecoder<IConfig>)decoder.newInstance();
				this.decoders.put(type, decode);
				
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
			
		}
		
		if(!this.configUrls.containsKey(type)){
			this.configUrls.put(type, url);
		}
		
		
	}
	
	/**
	 * 读取并解析指定的text文件,并返回Map格式
	 * @param clazz
	 * @param url
	 * @param decoder
	 * @return
	 */
	private final <T extends IConfig> Map<String, T> readTextMap(String url, IDecoder<T> decoder){
		
		IReader reader = new LineReader();
		
		TextMapParser<T> parser = new TextMapParser<T>(reader, decoder);
		
		try {
			
			Map<String, T> maps = parser.parse(url);
			
			return maps;
			
		} catch (SimpleConfigException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 读取并解析指定的text文件,并返回Map格式
	 * @param clazz
	 * @param url
	 * @param decoder
	 * @return
	 */
	private final <T extends IConfig> List<T> readTextList(String url, IDecoder<T> decoder){
		
		IReader reader = new LineReader();
		
		TextListParser<T> parser = new TextListParser<T>(reader, decoder);
		
		try {
			
			List<T> list = parser.parse(url);
			
			return list;
			
		} catch (SimpleConfigException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
