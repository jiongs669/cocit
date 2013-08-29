package com.jiongsoft.cocit.utils.log;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Log4jLogAdapter implements LogAdapter {

	public static final String LOG4J_CLASS_NAME = "org.apache.log4j.Logger";

	public boolean canWork() {
		try {
			Class.forName(LOG4J_CLASS_NAME, true, Thread.currentThread().getContextClassLoader());
		} catch (ClassNotFoundException e) {
			return false;
		}

		return isPropertyFileConfigured();
	}

	final private boolean isPropertyFileConfigured() {
		String configureValue = System.getProperty("log4j.defaultInitOverride");

		if (configureValue != null && !"false".equalsIgnoreCase(configureValue))
			return false;

		if (System.getProperty("log4j.configuration") != null)
			return true;

		if (canFindInLog4jManner("log4j.properties"))
			return true;

		return canFindInLog4jManner("log4j.xml");
	}

	/**
	 * 本函数仿照log4j检查配置文件能否找到的逻辑。
	 * <p>
	 * 1. 能否由当前线程的ContextClassLoader的getResource找到；
	 * <p>
	 * 2. 能否由加载Log4jAdapter的ClassLoader的getResource方法找到;
	 * <p>
	 * 3. 能否由ClassLoader.getSystemResource找到；
	 * <p>
	 * 省略了原函数中关于java 1版本的处理。
	 * <p>
	 * 
	 * @param resourceName
	 *            : 被检查的资源名字。
	 * @see org.apache.log4j.helpers.Loader.getResource(String resource)
	 */
	final private boolean canFindInLog4jManner(String resourceName) {

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		if (classLoader.getResource(resourceName) != null)
			return true;

		classLoader = this.getClass().getClassLoader();

		if (classLoader.getResource(resourceName) != null)
			return true;

		return (ClassLoader.getSystemResource(resourceName) != null);
	}

	public ILog getLogger(String className) {
		return new Log4JLogger(className);
	}

	static class Log4JLogger extends AbstractLog {

		public static final String SUPER_FQCN = AbstractLog.class.getName();
		public static final String SELF_FQCN = Log4JLogger.class.getName();

		private Logger logger;

		private static boolean hasTrace;

		static {
			try {
				Level.class.getDeclaredField("TRACE");
				hasTrace = true;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

		Log4JLogger(String className) {
			logger = LogManager.getLogger(className);
			isFatalEnabled = logger.isEnabledFor(Level.FATAL);
			isErrorEnabled = logger.isEnabledFor(Level.ERROR);
			isWarnEnabled = logger.isEnabledFor(Level.WARN);
			isInfoEnabled = logger.isEnabledFor(Level.INFO);
			isDebugEnabled = logger.isEnabledFor(Level.DEBUG);
			if (hasTrace)
				isTraceEnabled = logger.isEnabledFor(Level.TRACE);
		}

		public void debug(Object message, Throwable t) {
			if (isDebugEnabled())
				logger.log(SELF_FQCN, Level.DEBUG, message, t);
		}

		public void error(Object message, Throwable t) {
			if (isErrorEnabled())
				logger.log(SELF_FQCN, Level.ERROR, message, t);

		}

		public void fatal(Object message, Throwable t) {
			if (isFatalEnabled())
				logger.log(SELF_FQCN, Level.FATAL, message, t);
		}

		public void info(Object message, Throwable t) {
			if (isInfoEnabled())
				logger.log(SELF_FQCN, Level.INFO, message, t);
		}

		public void trace(Object message, Throwable t) {
			if (isTraceEnabled())
				logger.log(SELF_FQCN, Level.TRACE, message, t);
			else if ((!hasTrace) && isDebugEnabled())
				logger.log(SELF_FQCN, Level.DEBUG, message, t);
		}

		public void warn(Object message, Throwable t) {
			if (isWarnEnabled())
				logger.log(SELF_FQCN, Level.WARN, message, t);
		}

		@Override
		protected void log(int level, Object message, Throwable tx) {
			switch (level) {
			case LEVEL_FATAL:
				logger.log(SUPER_FQCN, Level.FATAL, message, tx);
				break;
			case LEVEL_ERROR:
				logger.log(SUPER_FQCN, Level.ERROR, message, tx);
				break;
			case LEVEL_WARN:
				logger.log(SUPER_FQCN, Level.WARN, message, tx);
				break;
			case LEVEL_INFO:
				logger.log(SUPER_FQCN, Level.INFO, message, tx);
				break;
			case LEVEL_DEBUG:
				logger.log(SUPER_FQCN, Level.DEBUG, message, tx);
				break;
			case LEVEL_TRACE:
				if (hasTrace)
					logger.log(SUPER_FQCN, Level.TRACE, message, tx);
				break;
			default:
				break;
			}
		}
	}
}
