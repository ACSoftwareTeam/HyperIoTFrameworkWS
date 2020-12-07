package it.acsoftware.hyperiot.hadoopmanager.service;

import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.*;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the HadoopManagerSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = HadoopManagerSystemApi.class, immediate = true)
public final class HadoopManagerSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements HadoopManagerSystemApi {

    private Configuration configuration;
    private HadoopManagerUtil hadoopManagerUtil;

    @Override
    public void copyFile(File file, String path, boolean deleteSource) throws IOException {
        FileSystem fileSystem = getHadoopFileSystem();
        fileSystem.create(new Path(path));    // create file
        OutputStream os = fileSystem.create(new Path(path));
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        IOUtils.copyBytes(is, os, configuration); // copy content to file which has been created previously
    }

    @Override
    public void deleteFile(String path) throws IOException {
        FileSystem fileSystem = getHadoopFileSystem();
        fileSystem.delete(new Path(path), false);
    }

    @Reference
    protected void setHadoopManagerUtil(HadoopManagerUtil hadoopManagerUtil) {
        this.hadoopManagerUtil = hadoopManagerUtil;
    }

    /**
     * @return Hadoop FileSystem client from specified configuration
     */
    private FileSystem getHadoopFileSystem() {
        // Set bundle class loader in order to find classes defined inside this bundle:
        // this is required for Configuration to load DistributedFileSystem class
        ClassLoader karafClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader thisClassLoader = this.getClass().getClassLoader();
        Thread.currentThread().setContextClassLoader(thisClassLoader);
        try {
            if (configuration == null) {
                configuration = new Configuration();
                configuration.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
                configuration.set("fs.file.impl", LocalFileSystem.class.getName());
                configuration.set("fs.defaultFS", hadoopManagerUtil.getDefaultFS());
            }
            return FileSystem.get(configuration);
        } catch (Throwable t) {
            getLog().log(Level.SEVERE, t.getMessage(), t);
        } finally {
            Thread.currentThread().setContextClassLoader(karafClassLoader);
        }
        return null;
    }

}
