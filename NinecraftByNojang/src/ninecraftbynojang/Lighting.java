package ninecraftbynojang;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

public class Lighting {

    private final int lightId;

    private FloatBuffer positionBuffer;
    private FloatBuffer colorBuffer;

    private float x, y, z;
    private float w = 1.0f;           // 1.0f = positional light

    public Lighting(int lightId, float startX, float startY, float startZ) {
        this.lightId = lightId;
        this.x = startX;
        this.y = startY;
        this.z = startZ;

        initBuffers();
        setupWhiteLight();
    }

    private void initBuffers() {
        positionBuffer = BufferUtils.createFloatBuffer(4);
        colorBuffer = BufferUtils.createFloatBuffer(4);
    }

    private void setupWhiteLight() {
        colorBuffer.put(1.0f).put(1.0f).put(1.0f).put(0.0f).flip(); // Matches your original code
    }

    /**
     * Call this once during initialization (in initGL())
     */
    public void init() {
        updatePositionBuffer();
        
        GL11.glLight(lightId, GL11.GL_POSITION, positionBuffer);
        GL11.glLight(lightId, GL11.GL_AMBIENT, colorBuffer);
        GL11.glLight(lightId, GL11.GL_DIFFUSE, colorBuffer);
        GL11.glLight(lightId, GL11.GL_SPECULAR, colorBuffer);

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(lightId);
    }

    /**
     * Updates the light position to match camera/player position
     * Call this in your walking methods (forward, backward, strafe, etc.)
     */
    public void updatePosition(float camX, float camY, float camZ) {
        this.x = camX;
        this.y = camY;
        this.z = camZ;
        updatePositionBuffer();
        GL11.glLight(lightId, GL11.GL_POSITION, positionBuffer);
    }

    /**
     * Call this in your lookThrough() method so the light follows camera rotation too
     */
    public void updateForCamera(float camX, float camY, float camZ) {
        updatePosition(camX, camY, camZ);
    }

    private void updatePositionBuffer() {
        positionBuffer.clear();
        positionBuffer.put(x).put(y).put(z).put(w).flip();
    }

    // ==================== Extra useful methods ====================

    public void setColor(float r, float g, float b, float a) {
        colorBuffer.clear();
        colorBuffer.put(r).put(g).put(b).put(a).flip();

        GL11.glLight(lightId, GL11.GL_AMBIENT, colorBuffer);
        GL11.glLight(lightId, GL11.GL_DIFFUSE, colorBuffer);
        GL11.glLight(lightId, GL11.GL_SPECULAR, colorBuffer);
    }

    public void setColor(float r, float g, float b) {
        setColor(r, g, b, 0.0f); // default alpha like your original
    }

    public void disable() {
        GL11.glDisable(lightId);
    }

    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
}