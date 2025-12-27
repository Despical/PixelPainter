package dev.despical.pixelpainter.utils.data;

import org.bukkit.Location;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class DataHolder {

    public MaterialData materialData;
    public Location location;

    private boolean hasFaces = false;

    public DataHolder(Location location, MaterialData materialData) {
        this.location = location;
        this.materialData = materialData;
    }

    public DataHolder(Location b, MaterialData md, boolean hasFaces) {
        this.location = b;
        this.materialData = md;
        this.hasFaces = hasFaces;
    }

    public boolean hasFaces() {
        return hasFaces;
    }
}
