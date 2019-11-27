package top.anemone.mlBasedSAST.slice.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import org.apache.tools.zip.*;

public class JarUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JarUtil.class);
    private static final int BUFFER_ZISE = 1024 * 1024;

//    public static ClassLoader getWarClassLoader(Path warPath) throws IOException {
//        final Path tmpDir = Files.createTempDirectory("exploded-war");
//        // Delete the temp directory at shutdown
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            try {
//                deleteDirectory(tmpDir);
//            } catch (IOException e) {
//                LOGGER.error("Error cleaning up temp directory " + tmpDir.toString(), e);
//            }
//        }));
//
//        // Extract to war to the temp directory
//        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(warPath))) {
//            JarEntry jarEntry;
//            while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
//                Path fullPath = tmpDir.resolve(jarEntry.getName());
//                if (!jarEntry.isDirectory()) {
//                    Path dirName = fullPath.getParent();
//                    if (dirName == null) {
//                        throw new IllegalStateException("Parent of item is outside temp directory.");
//                    }
//                    if (!Files.exists(dirName)) {
//                        Files.createDirectories(dirName);
//                    }
//                    try (OutputStream outputStream = Files.newOutputStream(fullPath)) {
//                        copy(jarInputStream, outputStream);
//                    }
//                }
//            }
//        }
//
//        final List<URL> classPathUrls = new ArrayList<>();
//        classPathUrls.add(tmpDir.resolve("WEB-INF/classes").toUri().toURL());
//        unZip(warPath.toFile(), scanConfig.getWorkdir().toString(),classPathUrls);
//        classPathUrls.forEach(p -> classPathUrls.add(p));
//        URL[] classPaths = classPathUrls.toArray(new URL[classPathUrls.size()]);
//        URLClassLoader classLoader = new StandaloneClassLoader(classPaths);
//        return classLoader;
//    }

    //    public static ClassLoader getJarClassLoader(Path... jarPaths) throws IOException {
//        final List<URL> classPathUrls = new ArrayList<>(jarPaths.length);
//        for (Path jarPath : jarPaths) {
//            if (!Files.exists(jarPath) || Files.isDirectory(jarPath)) {
//                throw new IllegalArgumentException("Path \"" + jarPath + "\" is not a path to a file.");
//            }
//            classPathUrls.add(jarPath.toUri().toURL());
//            List<URL> jarURLs=new LinkedList<>();
//            unZip(new File(String.valueOf(jarPath)), scanConfig.getWorkdir().toString(), jarURLs);
//            for (URL url : jarURLs) {
//                classPathUrls.add(url);
//            }
//        }
//        URL[] classPaths = classPathUrls.toArray(new URL[classPathUrls.size()]);
//        URLClassLoader classLoader = new StandaloneClassLoader(classPaths);
//        scanConfig.setClasspath(classPaths);
//        return classLoader;
//    }

    public static File jar(Path classpath) throws IOException, InterruptedException {
        File zipFile = new File(classpath + File.separator + ".." + File.separator + "classpath.jar");
        String cmd = String.format("jar cf %s -C %s .", zipFile.getAbsolutePath(), classpath);
        Process process = Runtime.getRuntime().exec(cmd);
        if (process.waitFor() != 0) {
            InputStream inputStream = process.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println(line);
            }
            throw new IOException(cmd + " ERROR");
        }
        return zipFile;
    }


    /**
     * Joana的classloader不能加载BOOT-INF/classes/xxx.class中的类，因此需要移动xxx.class到顶层，同时解出所有依赖jar到lib
     *
     * @param srcFile
     * @param classesDir
     * @param libDir
     * @param appEntryPackage 通过该类定位到内层中的jar，如BOOT/classes/top/anemone/xxx.class, entryPackage=top
     */
    public static void unJar(File srcFile, Path classesDir, Path libDir, Set<String> appEntryPackage) {
        // 开始解压
        LOGGER.info("Transforming jar: "+srcFile);
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(srcFile);
            Enumeration<?> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (appEntryPackage != null && entry.getName().endsWith(".class")) {
                    String newPath = entry.getName();
                    for (String entryPackage : appEntryPackage) {
                        if (entry.getName().contains(entryPackage)) {
                            newPath = entry.getName().substring(entry.getName().indexOf(entryPackage));
                        }
                    }

                    Path parent = Paths.get(classesDir.toString(), newPath).getParent();
                    if (parent != null && (!parent.toFile().exists())) {
                        parent.toFile().mkdirs();
                    }
                    String[] fileName = entry.getName().split("/");
                    File targetFile = Paths.get(parent.toString(), fileName[fileName.length - 1]).toFile();
                    targetFile.createNewFile();
                    // 将压缩文件内容写入到这个文件中
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    byte[] buf = new byte[BUFFER_ZISE];
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    // 关流顺序，先打开的后关闭
                    fos.close();
                    is.close();
                }
                // 如果是文件夹，就创建个文件夹
                if (entry.getName().endsWith("jar") || entry.getName().endsWith("war")) {
                    // 如果是文件，就先创建一个文件，然后用io流把内容copy过去
                    String[] fileName = entry.getName().split("/");
                    File targetFile = Paths.get(libDir.toString(), fileName[fileName.length - 1]).toFile();
                    targetFile.createNewFile();
                    // 将压缩文件内容写入到这个文件中
                    InputStream is = zipFile.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(targetFile);
                    int len;
                    byte[] buf = new byte[BUFFER_ZISE];
                    while ((len = is.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                    }
                    // 关流顺序，先打开的后关闭
                    fos.close();
                    is.close();
                    unJar(targetFile, classesDir, libDir, null);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("unzip error from ZipUtils", e);
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Recursively delete the directory root and all its contents
     *
     * @param root Root directory to be deleted
     */
    public static void deleteDirectory(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                try {
                    Files.delete(file);
                } catch (FileSystemException e){
                    LOGGER.warn(file+" is being used.");
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                try {
                    Files.delete(dir);
                } catch (FileSystemException e){
                    LOGGER.warn(dir+" is being used.");
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Copy inputStream to outputStream. Neither stream is closed by this method.
     */
    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[4096];
        int n;
        while ((n = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, n);
        }
    }
    public static String getPath(){
        String path= JarUtil.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        if(path.endsWith(".jar")){
          path = path.substring(0, path.lastIndexOf("/") + 1);
        }
        return path;
    }
}
