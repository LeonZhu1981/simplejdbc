package org.expressme.simplejdbc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

/**
 * Scan classes under specific package.
 * 
 * @author Michael Liao
 */
public class ClasspathScanner {

    public interface ClassFilter {

        boolean accept(Class<?> clazz);

    }

    static final String PROTOCOL_FILE = "file";
    static final String PROTOCOL_JAR = "jar";

    static final String PREFIX_FILE = "file:";

    static final String JAR_URL_SEPERATOR = "!/";
    static final String CLASS_FILE = ".class";

    static final String WEB_INF_CLASSES = "WEB-INF/classes/";
    static final String WEB_INF_LIB = "WEB-INF/lib/";

    private final String packageName;
    private final String packagePath;
    private final ClassFilter filter;

    public ClasspathScanner(String packageName) {
        this(packageName, null);
    }

    public ClasspathScanner(String packageName, ClassFilter filter) {
        this.packageName = packageName;
        this.packagePath = packageName.replace('.', '/') + "/";
        this.filter = filter;
    }

    public Set<Class<?>> scan() {
        URL root = getClass().getClassLoader().getResource("/");
        if (root!=null && PROTOCOL_JAR.equals(root.getProtocol()) && root.getFile().endsWith("!/WEB-INF/classes/")) {
            // this is a war file:
            String warFileName = root.getFile().replace('\\', '/');
            int mark = warFileName.lastIndexOf("!/");
            warFileName = warFileName.substring(0, mark);
            if (warFileName.startsWith(PREFIX_FILE))
                warFileName = warFileName.substring(PREFIX_FILE.length());
            return findInWar(new File(warFileName));
        }
        return scanDirOrJar();
    }

    Set<Class<?>> findInWar(File warFileName) {
        Set<Class<?>> set = new HashSet<Class<?>>();
        JarFile warFile = null;
        try {
            warFile = new JarFile(warFileName);
            Enumeration<JarEntry> en = warFile.entries();
            while (en.hasMoreElements()) {
                JarEntry je = en.nextElement();
                String name = je.getName();
                if (name.startsWith(WEB_INF_CLASSES) && name.endsWith(CLASS_FILE)) {
                    String className = pathToDot(name.substring(WEB_INF_CLASSES.length(), name.length()-CLASS_FILE.length()));
                    add(set, className, true);
                }
                if (name.startsWith(WEB_INF_LIB) && (name.endsWith(".jar") || name.endsWith(".zip"))) {
                    InputStream input = null;
                    try {
                        input = warFile.getInputStream(je);
                        findInJar(set, input);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                    finally {
                        close(input);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            close(warFile);
        }
        return set;
    }

    Set<Class<?>> scanDirOrJar() {
        Set<Class<?>> set = new HashSet<Class<?>>();
        Enumeration<URL> en = null;
        try {
            en = getClass().getClassLoader().getResources(packagePath);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        while (en.hasMoreElements()) {
            URL url = en.nextElement();
            System.out.println(url);
            if (PROTOCOL_FILE.equals(url.getProtocol())) {
                File root = new File(url.getFile());
                findInDirectory(set, root, root, packageName);
            }
            else if (PROTOCOL_JAR.equals(url.getProtocol())) {
                InputStream input = null;
                try {
                    input = new BufferedInputStream(new FileInputStream(getJarFile(url)));
                    findInJar(set, input);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                finally {
                    if (input!=null) {
                        try {
                            input.close();
                        }
                        catch (IOException e) {}
                    }
                }
            }
        }
        return set;
    }

    public File getJarFile(URL url) {
        String file = url.getFile();
        if (file.startsWith(PREFIX_FILE))
            file = file.substring(PREFIX_FILE.length());
        int end = file.indexOf(JAR_URL_SEPERATOR);
        if (end!=(-1))
            file = file.substring(0, end);
        return new File(file);
    }

    void findInJar(Set<Class<?>> results, InputStream input) {
        JarInputStream jarInput = null;
        try {
            jarInput = new JarInputStream(input);
            for (;;) {
                JarEntry je = jarInput.getNextJarEntry();
                if (je==null)
                    break;
                String name = je.getName();
                if (name.startsWith(packagePath) && name.endsWith(CLASS_FILE)) {
                    String className = name.substring(0, name.length() - CLASS_FILE.length());
                    add(results, pathToDot(className));
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            close(jarInput);
        }
    }

    void findInDirectory(Set<Class<?>> results, File rootDir, File dir, String packageName) {
        File[] files = dir.listFiles();
        String rootPath = rootDir.getPath();
        for (File file : files) {
            if (file.isFile()) {
                String classFileName = file.getPath();
                if (classFileName.endsWith(CLASS_FILE)) {
                    String className = classFileName.substring(rootPath.length() - packageName.length(), classFileName.length() - CLASS_FILE.length());
                    add(results, pathToDot(className));
                }
            }
            else if (file.isDirectory()) {
                findInDirectory(results, rootDir, file, packageName);
            }
        }
    }

    void add(Set<Class<?>> results, String className) {
        add(results, className, false);
    }

    void add(Set<Class<?>> results, String className, boolean forceAdd) {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        }
        catch(ClassNotFoundException e) {
            return;
        }
        if (forceAdd || (! results.contains(clazz))) {
            if (filter==null || filter.accept(clazz)) {
                results.add(clazz);
            }
        }
    }

    String pathToDot(String s) {
        return s.replace('/', '.').replace('\\', '.');
    }

    void close(InputStream input) {
        if (input!=null) {
            try {
                input.close();
            }
            catch (IOException e) {
            }
        }
    }

    void close(JarFile jar) {
        if (jar!=null) {
            try {
                jar.close();
            }
            catch (IOException e) {
            }
        }
    }
}
