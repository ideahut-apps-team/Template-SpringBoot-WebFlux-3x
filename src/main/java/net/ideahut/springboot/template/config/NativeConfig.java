package net.ideahut.springboot.template.config;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.helper.NativeImageHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.module.ModuleApi;
import net.ideahut.springboot.object.StringSet;
import net.ideahut.springboot.serializer.DataMapperBinarySerializer;
import net.ideahut.springboot.serializer.HessianBinarySerializer;
import net.ideahut.springboot.serializer.JdkBinarySerializer;
import net.ideahut.springboot.template.Application;

@Slf4j
@Configuration
@ImportRuntimeHints({NativeConfig.Registrar.class})
public class NativeConfig {

	static class Registrar implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, ClassLoader loader) {
			/*
			 * Tambah class-class yang akan diregistrasi untuk native di sini
			 */
			
		}
		
	}
	
	private static final File serializationFile = new File("serialization.tmp");
	private static final File binaryFile = new File("src/main/resources/serialization.bin");
	private static final File metadataFile = new File("src/main/resources/META-INF/native-image/reachability-metadata.json"); //-
	//private static final File metadataFile = new File("src/main/resources/META-INF/native-image/"); //-
	
	public static void registerToNativeImageAgent(ApplicationContext applicationContext) {
		ObjectHelper.runIf(
			NativeImageHelper.isAgentEnabled(), 
			() -> NativeImageHelper.registerMetadata(collectClasses(applicationContext), serializationFile)
		);
	}
	
	private static Collection<Class<?>> collectClasses(ApplicationContext applicationContext) {
		StringSet names = new StringSet(NativeImageHelper.Module.allModuleClassNames(applicationContext));
		names.addAll(NativeImageHelper.Module.getJsonWebTokenClassNames());
		names.addAll(NativeImageHelper.allClassNameInPackage(
			false, 
			"org.springframework.data.domain" // untuk repo
		));
		names.addAll(NativeImageHelper.allClassNameInPackage(
			Application.Package.APPLICATION + ".app",
			Application.Package.APPLICATION + ".controller",
			Application.Package.APPLICATION + ".interceptor",
			Application.Package.APPLICATION + ".job",
			Application.Package.APPLICATION + ".listener",
			Application.Package.APPLICATION + ".object",
			Application.Package.APPLICATION + ".properties",
			Application.Package.APPLICATION + ".repo",
			Application.Package.APPLICATION + ".service"
		));
		
		Set<Class<?>> classes = new LinkedHashSet<>(NativeImageHelper.convertToClasses(names));
		NativeImageHelper.addToClasses(classes, true, ModuleApi.getDefaultProcessors().toArray(new Class<?>[0]));
		NativeImageHelper.addToClasses(classes, true,
			DataMapperBinarySerializer.class,
			JdkBinarySerializer.class,
			HessianBinarySerializer.class
			//KryoBinarySerializer.class,
			//ForyBinarySerializer.class
		);
		
		return classes;
	}
	
	public static void main(String... args) {
		NativeImageHelper.mergeSerializationToMetadata(
			serializationFile, 
			metadataFile,
			(String type) -> type.indexOf("$HibernateAccessOptimizer$") != -1,
			(String type) -> type.indexOf("$HibernateInstantiator$") != -1
		);
		NativeImageHelper.excludeResourceFromMetadata(
			metadataFile, 
			"META-INF/services/org.hibernate.bytecode.spi.BytecodeProvider"::equals
		);
		NativeImageHelper.beautifyMetadata(metadataFile);
		try {
			FileUtils.copyFile(serializationFile, binaryFile);
		} catch (Exception e) {
			log.error("Copy", e);
		}
	}
	
}
