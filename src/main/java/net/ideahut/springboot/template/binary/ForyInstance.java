package net.ideahut.springboot.template.binary;

import java.util.Collection;

import org.apache.fory.BaseFory;
import org.apache.fory.Fory;
import org.apache.fory.ThreadLocalFory;
import org.apache.fory.ThreadSafeFory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.springboot.helper.FrameworkHelper;
import net.ideahut.springboot.helper.ObjectHelper;
import net.ideahut.springboot.object.StringSet;

/*
 * MASIH EXPERIMENTAL !!!
 * <buildArg>--initialize-at-run-time=net.ideahut.springboot.template.binary.ForyInstance</buildArg>
 */

@Slf4j
public class ForyInstance {

	private ForyInstance() {}

	private static ThreadSafeFory fory;

	private static void register(Fory f, String path) {
		try {
			Resource resource = new ClassPathResource(path);
			byte[] bytes = FrameworkHelper.toByteArray(resource);
			if (bytes.length != 0) {
				StringSet names = FrameworkHelper.defaultDataMapper().read(bytes, StringSet.class);
				Collection<Class<?>> types = FrameworkHelper.convertToClasses(names, ObjectHelper.getClassLoader(), false, null);
				for (Class<?> type : types) {
					f.register(type);
				}
			}
		} catch (Exception e) {
			log.warn("Register '{}': {}", path, e.getMessage());
		}
	}
	
	static {
		fory = new ThreadLocalFory(classLoader -> {
			Fory f = Fory.builder().build();
			register(f, "initialization.bin");
			register(f, "serialization.bin");
			f.ensureSerializersCompiled();
			return f;
		});
	}

	public static BaseFory getInstance() {
		return fory;
	}

}
