package net.creeperhost.creeperlauncher;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CreeperLogger
{
    public static CreeperLogger INSTANCE = new CreeperLogger();
    private static Logger logger;
    private String filename = "downloader.log";
    private FileHandler fileHandler;
    private final int limit = 1024 * 10000; //10 MB
    private SimpleFormatter simpleFormatter;

    public CreeperLogger()
    {
        logger = Logger.getLogger("launcher.log");

        simpleFormatter = new SimpleFormatter();
        try
        {
            fileHandler = new FileHandler(filename, limit, 1, true);
            logger.addHandler(fileHandler);
            simpleFormatter = new SimpleFormatter();
            fileHandler.setFormatter(simpleFormatter);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void info(String input)
    {
        logger.info(input);
    }

    public void warning(String input)
    {
        logger.warning(input);
    }

    public void error(String input)
    {
        logger.severe(input);
    }
}
