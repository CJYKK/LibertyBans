/* 
 * LibertyBans-core
 * Copyright © 2020 Anand Beh <https://www.arim.space>
 * 
 * LibertyBans-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * LibertyBans-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with LibertyBans-core. If not, see <https://www.gnu.org/licenses/>
 * and navigate to version 3 of the GNU Affero General Public License.
 */
package space.arim.libertybans.core.database;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import space.arim.omnibus.util.ThisClass;

public final class IOUtils {
	
	private static final Class<?> THIS_CLASS = ThisClass.get();
	
	private IOUtils() {}

	private static InputStream getResource(String resourceName) throws IOException {
		URL url = THIS_CLASS.getResource('/' + resourceName);
		if (url == null) {
			throw new IllegalArgumentException("Resource " + resourceName + " not found");
		}
		return url.openStream();
	}
	
	/**
	 * Blocking operation which reads all the content of a resource
	 * 
	 * @param resourceName the resource name
	 * @return the resource content
	 * @throws IllegalStateException if an IO error occurred
	 */
	static ByteArrayOutputStream readResource(String resourceName) {
		try (InputStream is = getResource(resourceName)) {

			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			is.transferTo(baos);
			return baos;
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to read internal resource " + resourceName, ex);
		}
	}
	
	/**
	 * Blocking operation which reads all SQL queries from the specified resource name. <br>
	 * This is otherwise equivalent to reading a resource file, excluding lines starting with
	 * {@literal "--"} and blank lines, and splitting the result by ";". <br>
	 * <br>
	 * The returned list is mutable.
	 * 
	 * @param resourceName the resource name
	 * @return a mutable list of SQL queries
	 * @throws IllegalStateException if an IO error occurred
	 */
	static List<String> readSqlQueries(String resourceName) {
		try (InputStream inputStream = getResource(resourceName);
				InputStreamReader inputReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(inputReader)) {

			List<String> result = new ArrayList<>();
			StringBuilder currentBuilder = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("--") || line.isBlank()) {
					continue;
				}
				currentBuilder.append(line);

				if (line.endsWith(";")) {
					result.add(currentBuilder.substring(0, currentBuilder.length() - 1));
					currentBuilder = new StringBuilder();
					continue;
				} else {
					currentBuilder.append('\n');
				}
			}
			return result;
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to read internal resource " + resourceName, ex);
		}
	}
	
	public static class ThreadFactoryImpl implements ThreadFactory {
		
		private final String prefix;
		private int threadId = 1;
		
		private static final Logger logger = LoggerFactory.getLogger(ThisClass.get());
		
		public ThreadFactoryImpl(String prefix) {
			this.prefix = prefix;
		}
		
		private synchronized int nextId() {
			return threadId++;
		}
		
		@Override
		public Thread newThread(Runnable r) {
			String name = prefix + nextId();
			logger.debug("Spawning new thread {}", name);
			return new Thread(r, name);
		}
		
	}
	
	public static class SafeExecutorWrapper implements Executor {
		
		private final Executor delegate;
		
		public SafeExecutorWrapper(Executor delegate) {
			this.delegate = delegate;
		}

		@Override
		public void execute(Runnable command) {
			delegate.execute(command);
		}
		
	}
	
}
