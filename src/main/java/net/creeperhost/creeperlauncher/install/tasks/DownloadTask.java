package net.creeperhost.creeperlauncher.install.tasks;

import net.creeperhost.creeperlauncher.CreeperLogger;
import net.creeperhost.creeperlauncher.IntegrityCheckException;
import net.creeperhost.creeperlauncher.api.DownloadableFile;
import net.creeperhost.modpackserverdownloader.Main;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.util.concurrent.*;

public class DownloadTask implements IInstallTask
{
    private final Path destination;
    private boolean canChecksum = false;
    private boolean checksumComplete;
    private String sha1;
    static int nThreads = Integer.parseInt(Main.getDefaultThreadLimit(""));
    private static final Executor threadPool = new ThreadPoolExecutor(nThreads, nThreads, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    private int tries = 0;
    private final DownloadableFile file;

    public DownloadTask(DownloadableFile file, Path destination)
    {
        this.file = file;
        this.destination = destination;
    }

    @Override
    public CompletableFuture<Void> execute()
    {
        return CompletableFuture.runAsync(() ->
        {
            boolean complete = false;
            while (!complete && tries < 3)
            {
                try
                {
                    ++tries;
                    file.prepare();
                    complete = true;
                } catch (SocketTimeoutException err)
                {
                    if (tries == 3)
                    {
                        throw new IntegrityCheckException(err.getMessage(), -2, "", null, 0, 0, file.getUrl(), destination.toString());
                    }
                } catch (IOException e)
                {
                    CreeperLogger.INSTANCE.error("Unable to download " + file.getName());
                    return;
                }
            }
            complete = false;
            tries = 0;
            while (!complete && tries < 3)
            {
                /*if (tries == 0)
                {
                    for (String checksum : file.getExpectedSha1())
                    {
                        if (CreeperLauncher.localCache.exists(checksum))
                        {
                            if (checksum != null)
                            {
                                File cachedFile = CreeperLauncher.localCache.get(checksum);
                                if (destination.toFile().exists()) break;
                                try
                                {
                                    destination.toFile().getParentFile().mkdirs();
                                    Files.copy(cachedFile.toPath(), destination);
                                    FTBModPackInstallerTask.currentBytes.addAndGet(cachedFile.length());
                                    complete = true;
                                    break;
                                } catch (IOException ignored)
                                {
                                }
                            }
                        }
                    }
                }*/ // comment out cache
                if (!complete)
                {
                    try
                    {
                        ++tries;
                        file.download(destination, false, false);
                        file.validate(false, true);
                        try
                        {
                            //CreeperLauncher.localCache.put(file.getLocalFile(), file.getSha1()); // comment out cache
                        } catch (Exception err)
                        {
                            CreeperLogger.INSTANCE.error(err.toString());
                            err.printStackTrace();
                        }
                        complete = true;
                    } catch (Throwable e)
                    {
                        if (e instanceof IntegrityCheckException)
                        {
                            throw (IntegrityCheckException) e;
                        }
                        if (tries == 3)
                        {
                            if (e instanceof IntegrityCheckException)
                            {
                                throw (IntegrityCheckException) e;
                            } else
                            {
                                throw new IntegrityCheckException(e, -1, "", null, 0, 0, file.getUrl(), destination.toString()); // TODO: make this better
                            }
                        }
                    }
                }
            }
        }, threadPool);

    }

    @Override
    public Double getProgress()
    {
        if (Main.currentBytes.get() == 0 || Main.overallBytes.get() == 0)
            return 0.00d;
        return ((Main.currentBytes.get() / Main.overallBytes.get()) * 100d);
    }
}
