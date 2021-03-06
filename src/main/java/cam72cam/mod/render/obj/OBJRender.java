package cam72cam.mod.render.obj;

import cam72cam.mod.ModCore;
import cam72cam.mod.math.Vec3d;
import cam72cam.mod.model.obj.Material;
import cam72cam.mod.model.obj.OBJModel;
import cam72cam.mod.model.obj.Vec2f;
import cam72cam.mod.render.OpenGL;
import cam72cam.mod.render.VBA;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * VBA/VBO Backed object renderer
 */
public class OBJRender {
    public OBJModel model;
    public Map<String, OBJTextureSheet> textures = new HashMap<>();
    private VBA vba;

    public OBJRender(OBJModel model) {
        this(model, null);
    }

    public OBJRender(OBJModel model, Collection<String> textureNames) {
        this(model, textureNames, 30);
    }
    public OBJRender(OBJModel model, Collection<String> textureNames, int cacheSeconds) {
        this.model = model;
        if (textureNames != null && textureNames.size() > 1) {
            for (String name : textureNames) {
                this.textures.put(name, new OBJTextureSheet(model, name, cacheSeconds));
            }
        } else {
            this.textures.put(null, new OBJTextureSheet(model, "", cacheSeconds));
        }
    }

    public OpenGL.With bindTexture() {
        return bindTexture(null);
    }

    public OpenGL.With bindTexture(boolean icon) {
        return bindTexture(null, icon);
    }

    public OpenGL.With bindTexture(String texName) {
        return bindTexture(texName, false);
    }

    public OpenGL.With bindTexture(String texName, boolean icon) {
        if (this.textures.get(texName) == null) {
            texName = null; // Default
        }

        OBJTextureSheet tex = this.textures.get(texName);

        if (icon) {
            return tex.bindIcon();
        } else {
            return tex.bind();
        }
    }

    public void draw() {
        createVBA().draw();
    }

    public void drawGroups(Iterable<String> groups) {
        createVBA().draw(groups);
    }

    public VBA createVBA() {
        if (vba != null) {
            return vba;
        }

        List<Integer> tris = new ArrayList<Integer>();
        Map<String, Pair<Integer, Integer>> groupIdx = new LinkedHashMap<>();

        for (String group : model.groups.keySet()) {
            if (group.matches(".*EXHAUST_\\d.*") || group.matches(".*CHIMNEY_\\d.*") || group.matches(".*PRESSURE_VALVE_\\d.*") || group.matches(".*CHIMINEY_\\d.*")) {
                //Skip particle emitters
                continue;
            }
            groupIdx.put(group, Pair.of(tris.size(), model.groups.get(group).length));
            for (int face : model.groups.get(group)) {
                tris.add(face);
            }
        }

        vba = new VBA(tris.size(), groupIdx);

        for (int face : tris) {
            String mtlName = model.faceMTLs[face];
            Material currentMTL = model.materials.get(mtlName);
            float r = 0;
            float g = 0;
            float b = 0;
            float a = 1;

            OBJTextureSheet texture = textures.get(null);

            if (currentMTL != null) {
                if (currentMTL.Kd != null) {
                    float mult = 1 - model.darken * 5;

                    if (texture.isFlatMaterial(mtlName)) {
                        r = 1;
                        g = 1;
                        b = 1;
                    } else {
                        r = currentMTL.Kd.get(0);
                        g = currentMTL.Kd.get(1);
                        b = currentMTL.Kd.get(2);
                    }

                    r = Math.max(0, r * mult);
                    g = Math.max(0, g * mult);
                    b = Math.max(0, b * mult);
                    a = currentMTL.Kd.get(3);
                }
            } else {
                ModCore.warn(String.format("Missing group %s", mtlName));
            }

            for (int[] point : model.points(face)) {
                Vec2f vt = point[1] != -1 ? model.vertexTextures(point[1]) : null;

                if (vt != null) {
                    vt = new Vec2f(
                            texture.convertU(mtlName, vt.x - model.offsetU[face]),
                            texture.convertV(mtlName, -(vt.y) - model.offsetV[face])
                    );
                } else {
                    vt = new Vec2f(
                            texture.convertU(mtlName, 0),
                            texture.convertV(mtlName, 0)
                    );
                }

                boolean hasVn = point[2] != -1;
                vba.addPoint(
                        model.vertex(point[0], OBJModel.Vert.X),
                        model.vertex(point[0], OBJModel.Vert.Y),
                        model.vertex(point[0], OBJModel.Vert.Z),
                        hasVn,
                        hasVn ? model.vertexNormal(point[2], OBJModel.Vert.X) : 0,
                        hasVn ? model.vertexNormal(point[2], OBJModel.Vert.Y) : 0,
                        hasVn ? model.vertexNormal(point[2], OBJModel.Vert.Z) : 0,
                        vt, r, g, b, a);
            }
        }

        model.vertexNormals = null;
        model.vertexTextures = null;
        model.offsetU = null;
        model.offsetV = null;
        model.faceMTLs = null;
        model.faceVerts = null;
        model.vertices = null;

        return vba;
    }

    public void free() {
        for (OBJTextureSheet texture : textures.values()) {
            texture.freeGL();
        }
        if (vba != null) {
            vba.free();
        }
    }

}
