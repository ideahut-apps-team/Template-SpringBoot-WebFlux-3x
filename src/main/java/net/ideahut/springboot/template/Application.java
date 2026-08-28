package net.ideahut.springboot.template;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.definition.LauncherDefinition;
import net.ideahut.springboot.helper.FrameworkHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.launcher.WebFluxLauncher;
import net.ideahut.springboot.launcher.WebLauncher;
import net.ideahut.springboot.template.app.AppProperties;
import net.ideahut.springboot.template.config.NativeConfig;

/*
 * Main Class, untuk eksekusi aplikasi
 */

@Slf4j
@SpringBootApplication
public class Application extends WebFluxLauncher {
	
	/*
	 * PACKAGE
	 */
	public static class Package {
		private Package() {}
		public static final String LIBRARY		= FrameworkHelper.PACKAGE;
		public static final String APPLICATION	= "net.ideahut.springboot.template";
	}
	
	private static boolean ready = false;
	private static void setReady(boolean b) { ready = b; }
	public static boolean isReady() { return ready; }
	
	/*
	 * MAIN
	 */
	public static void main(String... args) {
		WebLauncher.runApp(Application.class, args);
	}
	
	/*
	 * DEFINITION
	 */
	@Override
	public LauncherDefinition onDefinition(ApplicationContext applicationContext) {
		return FrameworkHelper.getBean(applicationContext, AppProperties.class).getLauncher();
	}
	
	/*
	 * READY
	 */
	@Override
	public void onReady(ApplicationContext applicationContext) {
		setReady(true);
		NativeConfig.registerToNativeImageAgent(applicationContext);
	}
	
	/*
	 * ERROR
	 */
	@Override
	public void onError(ApplicationContext applicationContext, Throwable throwable) {
		log.error("Application", throwable);
		System.exit(0);
	}
	
	/*
	 * LOG
	 */
	@Override
	public void onLog(
		LauncherDefinition.Log.Type type, 
		LauncherDefinition.Log.Level level, 
		String message, 
		Throwable throwable
	) {
		level = ObjectHelper.useOrDefault(level, () -> LauncherDefinition.Log.Level.DEBUG);
		log.atLevel(org.slf4j.event.Level.valueOf(level.name())).log(message, throwable);
	}
	
}
