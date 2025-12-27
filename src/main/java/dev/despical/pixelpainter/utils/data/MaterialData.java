package dev.despical.pixelpainter.utils.data;

import dev.despical.pixelpainter.utils.RGBBlockColor;
import dev.despical.pixelpainter.utils.image.ImageRelativeBlockDirection;
import org.bukkit.Material;

/**
 * @author Despical
 * <p>
 * Created at 4.08.2022
 */
public class MaterialData implements Comparable<MaterialData> {

    private final byte data;
    private Material material;
    private ImageRelativeBlockDirection direction = null;

    public MaterialData(Material m, byte data, ImageRelativeBlockDirection direction) {
        this.setMaterial(m);
        this.data = data;
        this.direction = direction;
    }

    public MaterialData(Material material, byte data) {
        this(material, data, null);
    }

    public MaterialData(Material material) {
        this(material, ((byte) 0), null);
    }


    public static MaterialData getMatDataByTypes(Material mat, byte data) {
        return getMatDataByTypes(mat, data, null);
    }

    public static MaterialData getMatDataByTypes(Material mat, byte data, ImageRelativeBlockDirection direction) {
        for (MaterialData key : RGBBlockColor.materialValue.keySet())
            if (key.getData() == data && key.getMaterial() == mat && ((direction == null && !key.hasDirection()) || direction == key.getDirection()))
                return key;

        return null;
    }

    public boolean hasDirection() {
        return direction != null;
    }

    public ImageRelativeBlockDirection getDirection() {
        return direction;
    }

    public byte getData() {
        return data;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material m) {
        this.material = m;
    }

    @Override
    public int compareTo(MaterialData o) {
        if (this.material == o.material) {
            String dir1 = this.direction == null ? "" : this.direction.name();
            String dir2 = o.direction == null ? "" : o.direction.name();
            return dir1.compareTo(dir2);
        }

        return this.material.name().compareTo(o.material.name());
    }
}
