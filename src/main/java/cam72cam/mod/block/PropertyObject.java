package cam72cam.mod.block;

import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * Hack the planet!<br/>
 *
 * More seriously, this is the property that allows us to pass the TE into the renderer
 */
public class PropertyObject implements IUnlistedProperty<Object> {
    private final String name;

    public PropertyObject(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isValid(Object value) {
        return true;
    }

    @Override
    public Class<Object> getType() {
        return Object.class;
    }

    @Override
    public String valueToString(Object value) {
        return value.toString();
    }
}