package com.zishanfu.geosparksim.osm;

import com.vividsolutions.jts.geom.Coordinate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.apache.log4j.Logger;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

/** Road network loader Download road network data and compress it */
public class OsmLoader {
    private Coordinate geo1;
    private Coordinate geo2;
    private String path;
    private String newFileName;
    private static final Logger LOG = Logger.getLogger(OsmLoader.class);

    public OsmLoader(Coordinate geo1, Coordinate geo2, String path) {
        this.geo1 = geo1;
        this.geo2 = geo2;
        this.path = path;
    }

    public void osmDownloader() {
        String OSM_URL = "http://overpass-api.de/api/map?bbox=";
        URL url = null;

        double left = Math.min(geo1.y, geo2.y);
        double right = Math.max(geo1.y, geo2.y);
        double top = Math.max(geo1.x, geo2.x);
        double bottom = Math.min(geo1.x, geo2.x);

        OSM_URL += left + "," + bottom + "," + right + "," + top;

        try {
            url = new URL(OSM_URL);
        } catch (MalformedURLException e) {
            LOG.warn("The OSM download URL is incorrect.", e);
        }

        newFileName = String.format("%s/%s.osm", path, "map");

        try {
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());
            FileOutputStream fos = new FileOutputStream(newFileName);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();
            rbc.close();
        } catch (IOException e) {
            LOG.warn("Error happens when downloading OSM.", e);
        }
    }

    public void parquetizer() {
        File osmFile = new File(newFileName);
        XmlReader xmlReader = new XmlReader(osmFile, true, CompressionMethod.None);
        xmlReader.setSink(new OsmParquetSink(path));
        xmlReader.run();
    }
}
