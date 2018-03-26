/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.jmeter;

// N.B. this must only use standard Java packages

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * Main class for JMeter - sets up initial classpath and the loader.
 *
 */
public final class NewDriver {



    //文件分隔符
    private static final String CLASSPATH_SEPARATOR = File.pathSeparator;
    //操作系统名称
    private static final String OS_NAME = System.getProperty("os.name");// $NON-NLS-1$
    //操作系统名称小写
    private static final String OS_NAME_LC = OS_NAME.toLowerCase(java.util.Locale.ENGLISH);
    //JAVA类路径
    private static final String JAVA_CLASS_PATH = "java.class.path";// $NON-NLS-1$

    /** The class loader to use for loading JMeter classes. */
    //类加载器用来加载Jmeter的类文件
    private static final DynamicClassLoader loader;

    /** The directory JMeter is installed in. */
    //Jmeter的安装字典,可以就是环境变量这些啥的
    private static final String JMETER_INSTALLATION_DIRECTORY;
    //初始化的一个异常列表
    private static final List<Exception> EXCEPTIONS_IN_INIT = new ArrayList<>();

    //新添加一个日志方便调试
    static Logger log = Logger.getLogger("NewDriver.class");

    //static初始化代码块
    static {
        //URL链表
        final List<URL> jars = new LinkedList<>();
        //class.paht路径
        final String initial_classpath = System.getProperty(JAVA_CLASS_PATH);

        // Find JMeter home dir from the initial classpath

        //寻找JmeterHome目录,根据操作系统不同有不同的路径标记
        String tmpDir;
        StringTokenizer tok = new StringTokenizer(initial_classpath, File.pathSeparator);
        log.info(tok.toString());

        if (tok.countTokens() == 1
                || (tok.countTokens()  == 2 // Java on Mac OS can add a second entry to the initial classpath
                    && OS_NAME_LC.startsWith("mac os x")// $NON-NLS-1$
                   )
           ) {

            File jar = new File(tok.nextToken());
            log.info(tok.nextToken());
            try {
                tmpDir = jar.getCanonicalFile().getParentFile().getParent();
                log.info("路径为"+tmpDir);
            } catch (IOException e) {
                tmpDir = null;
            }
        } else {// e.g. started from IDE with full classpath
            tmpDir = System.getProperty("jmeter.home","");// Allow override $NON-NLS-1$ $NON-NLS-2$
            log.info("IDE启动时的路径"+tmpDir);
            if (tmpDir.length() == 0) {
                File userDir = new File(System.getProperty("user.dir"));// $NON-NLS-1$
                tmpDir = userDir.getAbsoluteFile().getParent();
                log.info("user.dir的路径"+tmpDir);
            }
        }
        //jmeter的安装路径
        JMETER_INSTALLATION_DIRECTORY=tmpDir;

        /*
         * Does the system support UNC paths? If so, may need to fix them up
         * later
         */
        //判断系统支持的路径类型
        boolean usesUNC = OS_NAME_LC.startsWith("windows");// $NON-NLS-1$


        //添加必须JAR包对应路径
        // Add standard jar locations to initial classpath
        StringBuilder classpath = new StringBuilder();
        File[] libDirs = new File[] { new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib"),// $NON-NLS-1$ $NON-NLS-2$
                new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "ext"),// $NON-NLS-1$ $NON-NLS-2$
                new File(JMETER_INSTALLATION_DIRECTORY + File.separator + "lib" + File.separator + "junit")};// $NON-NLS-1$ $NON-NLS-2$
        for (File libDir : libDirs) {
            File[] libJars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (libJars == null) {
                new Throwable("Could not access " + libDir).printStackTrace(); // NOSONAR No logging here
                continue;
            }
            Arrays.sort(libJars); // Bug 50708 Ensure predictable order of jars

            for (File libJar : libJars) {
                try {
                    String s = libJar.getPath();
                    //不同系统时的路径处理
                    // Fix path to allow the use of UNC URLs
                    if (usesUNC) {
                        if (s.startsWith("\\\\") && !s.startsWith("\\\\\\")) {// $NON-NLS-1$ $NON-NLS-2$
                            s = "\\\\" + s;// $NON-NLS-1$
                        } else if (s.startsWith("//") && !s.startsWith("///")) {// $NON-NLS-1$ $NON-NLS-2$
                            s = "//" + s;// $NON-NLS-1$
                        }
                    } // usesUNC

                    jars.add(new File(s).toURI().toURL());// See Java bug 4496398
                    classpath.append(CLASSPATH_SEPARATOR);
                    classpath.append(s);
                } catch (MalformedURLException e) { // NOSONAR
                    EXCEPTIONS_IN_INIT.add(new Exception("Error adding jar:"+libJar.getAbsolutePath(), e));
                }
            }
        }

        // ClassFinder needs the classpath
        System.setProperty(JAVA_CLASS_PATH, initial_classpath + classpath.toString());

        log.info("设置系统类路径为环境变量,"+initial_classpath + classpath.toString());


        loader = AccessController.doPrivileged(
                (PrivilegedAction<DynamicClassLoader>) () ->
                        new DynamicClassLoader(jars.toArray(new URL[jars.size()]))
        );
    }

    /**
     * Prevent instantiation.
     */
    private NewDriver() {
    }

    /**
     * Generate an array of jar files located in a directory.
     * Jar files located in sub directories will not be added.
     *
     * @param dir to search for the jar files.
     */
    private static File[] listJars(File dir) {
        if (dir.isDirectory()) {
            return dir.listFiles((f, name) -> {
                if (name.endsWith(".jar")) {// $NON-NLS-1$
                    File jar = new File(f, name);
                    return jar.isFile() && jar.canRead();
                }
                return false;
            });
        }
        return new File[0];
    }

    /**
     * Add a URL to the loader classpath only; does not update the system classpath.
     *
     * @param path to be added.
     * @throws MalformedURLException when <code>path</code> points to an invalid url
     */
    public static void addURL(String path) throws MalformedURLException {
        File furl = new File(path);
        loader.addURL(furl.toURI().toURL()); // See Java bug 4496398
        File[] jars = listJars(furl);
        for (File jar : jars) {
            loader.addURL(jar.toURI().toURL()); // See Java bug 4496398
        }
    }

    /**
     * Add a URL to the loader classpath only; does not update the system
     * classpath.
     *
     * @param url
     *            The {@link URL} to add to the classpath
     */
    public static void addURL(URL url) {
        loader.addURL(url);
    }

    /**
     * Add a directory or jar to the loader and system classpaths.
     *
     * @param path
     *            to add to the loader and system classpath
     * @throws MalformedURLException
     *             if <code>path</code> can not be transformed to a valid
     *             {@link URL}
     */
    public static void addPath(String path) throws MalformedURLException {
        File file = new File(path);
        // Ensure that directory URLs end in "/"
        if (file.isDirectory() && !path.endsWith("/")) {// $NON-NLS-1$
            file = new File(path + "/");// $NON-NLS-1$
        }
        loader.addURL(file.toURI().toURL()); // See Java bug 4496398
        StringBuilder sb = new StringBuilder(System.getProperty(JAVA_CLASS_PATH));
        sb.append(CLASSPATH_SEPARATOR);
        sb.append(path);
        File[] jars = listJars(file);
        for (File jar : jars) {
            loader.addURL(jar.toURI().toURL()); // See Java bug 4496398
            sb.append(CLASSPATH_SEPARATOR);
            sb.append(jar.getPath());
        }

        // ClassFinder needs this
        System.setProperty(JAVA_CLASS_PATH,sb.toString());
    }

    /**
     * Get the directory where JMeter is installed. This is the absolute path
     * name.
     *
     * @return the directory where JMeter is installed.
     */
    public static String getJMeterDir() {
        return JMETER_INSTALLATION_DIRECTORY;
    }


    /**
     * The main program which actually runs JMeter.
     *
     * @param args
     *            the command line arguments
     */
    public static void main(String[] args) {
//          String[]  params = new String[]{
//                "-n","-tD:\\apitest\\src\\test\\jmeter\\云商B2B新架构接口性能测试.jmx","-llogFile3.jtl","-e","-oreprot3"
//        };
        if(!EXCEPTIONS_IN_INIT.isEmpty()) {
            System.err.println("Configuration error during init, see exceptions:"+exceptionsToString(EXCEPTIONS_IN_INIT));
        } else {

            Thread.currentThread().setContextClassLoader(loader);
            setLoggingProperties(args);

            try {
                Class<?> initialClass = loader.loadClass("org.apache.jmeter.JMeter");// $NON-NLS-1$
                Object instance = initialClass.newInstance();
                Method startup = initialClass.getMethod("start", new Class[] { new String[0].getClass() });// $NON-NLS-1$
                startup.invoke(instance, new Object[] { args });
            } catch(Throwable e){ // NOSONAR We want to log home directory in case of exception
                e.printStackTrace(); // NOSONAR No logger at this step
                System.err.println("JMeter home directory was detected as: "+JMETER_INSTALLATION_DIRECTORY);
            }
        }
    }

    /**
     * @param exceptionsInInit List of {@link Exception}
     * @return String
     */
    private static String exceptionsToString(List<Exception> exceptionsInInit) {
        StringBuilder builder = new StringBuilder();
        for (Exception exception : exceptionsInInit) {
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            exception.printStackTrace(printWriter); // NOSONAR 
            builder.append(stringWriter.toString())
                .append("\r\n");
        }
        return builder.toString();
    }

    /*
     * Set logging related system properties.
     */
    private static void setLoggingProperties(String[] args) {
        String jmLogFile = getCommandLineArgument(args, 'j', "jmeterlogfile");// $NON-NLS-1$ $NON-NLS-2$

        if (jmLogFile != null && !jmLogFile.isEmpty()) {
            jmLogFile = replaceDateFormatInFileName(jmLogFile);
            System.setProperty("jmeter.logfile", jmLogFile);// $NON-NLS-1$
        } else if (System.getProperty("jmeter.logfile") == null) {// $NON-NLS-1$
            System.setProperty("jmeter.logfile", "jmeter.log");// $NON-NLS-1$ $NON-NLS-2$
        }

        String jmLogConf = getCommandLineArgument(args, 'i', "jmeterlogconf");// $NON-NLS-1$ $NON-NLS-2$
        File logConfFile = null;

        if (jmLogConf != null && !jmLogConf.isEmpty()) {
            logConfFile = new File(jmLogConf);
        } else if (System.getProperty("log4j.configurationFile") == null) {// $NON-NLS-1$
            logConfFile = new File("log4j2.xml");// $NON-NLS-1$
            if (!logConfFile.isFile()) {
                logConfFile = new File(JMETER_INSTALLATION_DIRECTORY, "bin" + File.separator + "log4j2.xml");// $NON-NLS-1$ $NON-NLS-2$
            }
        }

        if (logConfFile != null) {
            System.setProperty("log4j.configurationFile", logConfFile.toURI().toString());// $NON-NLS-1$
        }
    }

    /*
     * Find command line argument option value by the id and name.
     */
    private static String getCommandLineArgument(String [] args, int id, String name) {
        final String shortArgName = "-" + ((char) id);// $NON-NLS-1$
        final String longArgName = "--" + name;// $NON-NLS-1$

        String value = null;

        for (int i = 0; i < args.length; i++) {
            if (shortArgName.equals(args[i]) && i < args.length - 1) {
                if (!args[i + 1].startsWith("-")) {// $NON-NLS-1$
                    value = args[i + 1];
                }
                break;
            } else if (!shortArgName.equals(args[i]) && args[i].startsWith(shortArgName)) {
                value = args[i].substring(shortArgName.length());
                break;
            } else if (longArgName.equals(args[i])) {
                if (!args[i + 1].startsWith("-")) {// $NON-NLS-1$
                    value = args[i + 1];
                }
                break;
            }
        }

        return value;
    }

    /*
     * If the fileName contains at least one set of paired single-quotes, reformat using DateFormat
     */
    private static String replaceDateFormatInFileName(String fileName) {
        try {
            StringBuilder builder = new StringBuilder();

            final Date date = new Date();
            int fromIndex = 0;
            int begin = fileName.indexOf('\'', fromIndex);// $NON-NLS-1$
            int end;

            String format;
            SimpleDateFormat dateFormat;

            while (begin != -1) {
                builder.append(fileName.substring(fromIndex, begin));

                fromIndex = begin + 1;
                end = fileName.indexOf('\'', fromIndex);// $NON-NLS-1$
                if (end == -1) {
                    throw new IllegalArgumentException("Invalid pairs of single-quotes in the file name: " + fileName);// $NON-NLS-1$
                }

                format = fileName.substring(begin + 1, end);
                dateFormat = new SimpleDateFormat(format);
                builder.append(dateFormat.format(date));

                fromIndex = end + 1;
                begin = fileName.indexOf('\'', fromIndex);// $NON-NLS-1$
            }

            if (fromIndex < fileName.length() - 1) {
                builder.append(fileName.substring(fromIndex));
            }

            return builder.toString();
        } catch (Exception ex) {
            System.err.println("Error replacing date format in file name:"+fileName+", error:"+ex.getMessage()); // NOSONAR
        }

        return fileName;
    }
}
