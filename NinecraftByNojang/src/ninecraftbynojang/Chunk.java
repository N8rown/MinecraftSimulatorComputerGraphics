/*
 * Updated Chunk.java - Proper Layered Terrain (Grass, Sand, Water on top)
 */
package ninecraftbynojang;

import java.nio.FloatBuffer;
import java.util.Random;
import org.lwjgl.BufferUtils;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import org.newdawn.slick.opengl.Texture;
import org.newdawn.slick.opengl.TextureLoader;
import org.newdawn.slick.util.ResourceLoader;

public class Chunk {

    static final int CHUNK_SIZE = 30;
    static final int CUBE_LENGTH = 2;

    private Block[][][] Blocks;
    private int VBOVertexHandle;
    private int VBOColorHandle;
    private int VBOTextureHandle;
    private Texture texture;

    private int startX, startY, startZ;
    private Random r;

    public Chunk(int startX, int startY, int startZ) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;

        r = new Random();
        Blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        try {
            texture = TextureLoader.getTexture("PNG",
                    ResourceLoader.getResourceAsStream("terrain.png"));
        } catch (Exception e) {
            System.out.println("Texture loading error: " + e.getMessage());
        }

        rebuildMesh(startX, startY, startZ);
    }

    public void rebuildMesh(float startX, float startY, float startZ) {
        // Noise setup
        int largestFeature = r.nextInt(20, CHUNK_SIZE * 2);
        double persistence = r.nextDouble(0.4, 0.75);
        int seed = r.nextInt(1000000);
        SimplexNoise noise = new SimplexNoise(largestFeature, persistence, seed);

        final int SEA_LEVEL = (int) (CHUNK_SIZE * 0.45f);

        VBOColorHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers();

        FloatBuffer VertexPositionData = BufferUtils.createFloatBuffer(
                CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * 6 * 12);
        FloatBuffer VertexColorData = BufferUtils.createFloatBuffer(
                CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * 6 * 12);
        FloatBuffer VertexTextureData = BufferUtils.createFloatBuffer(
                CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * 6 * 12);

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {

                // Generate surface height
                double noiseVal = noise.getNoise(x, z);
                int surfaceHeight = (int) (CHUNK_SIZE * 0.35 + noiseVal * CHUNK_SIZE * 0.4);
                surfaceHeight = Math.max(5, Math.min(surfaceHeight, CHUNK_SIZE - 2));

                for (int y = 0; y < CHUNK_SIZE; y++) {
                    Block.BlockType type;

                    if (y == 0) {
                        type = Block.BlockType.BlockType_Bedrock;           // Bottom layer
                    } 
                    else if (y < surfaceHeight - 4) {
                        type = Block.BlockType.BlockType_Stone;             // Deep stone
                    } 
                    else if (y < surfaceHeight - 1) {
                        type = Block.BlockType.BlockType_Dirt;              // Dirt layer
                    } 
                    else if (y == surfaceHeight - 1) {
                        // Surface layer
                        if (surfaceHeight <= SEA_LEVEL + 2) {
                            type = Block.BlockType.BlockType_Sand;          // Beach / shallow water
                        } else {
                            type = Block.BlockType.BlockType_Grass;         // Grass on top
                        }
                    } 
                    else if (y <= SEA_LEVEL && y > surfaceHeight - 1) {
                        type = Block.BlockType.BlockType_Water;             // Water above ground
                    } 
                    else {
                        continue; // Air - don't create block
                    }

                    Blocks[x][y][z] = new Block(type);
                    Blocks[x][y][z].setActive(true);

                    // Only render solid blocks + top water surface
                    if (type != Block.BlockType.BlockType_Water || y == SEA_LEVEL) {
                        VertexPositionData.put(createCube(
                                startX + x * CUBE_LENGTH,
                                startY + y * CUBE_LENGTH,
                                startZ + z * CUBE_LENGTH));

                        VertexColorData.put(createCubeVertexCol(getCubeColor(Blocks[x][y][z])));
                        VertexTextureData.put(createTexCube(0, 0, Blocks[x][y][z]));
                    }
                }
            }
        }

        // Upload VBOs
        VertexPositionData.flip();
        VertexColorData.flip();
        VertexTextureData.flip();

        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexPositionData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, VBOColorHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexColorData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexTextureData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void render() {
        glPushMatrix();

        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);

        glBindBuffer(GL_ARRAY_BUFFER, VBOColorHandle);
        glColorPointer(3, GL_FLOAT, 0, 0L);

        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBindTexture(GL_TEXTURE_2D, texture.getTextureID());
        glTexCoordPointer(2, GL_FLOAT, 0, 0L);

        glDrawArrays(GL_QUADS, 0, CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * 24);

        glPopMatrix();
    }

    // ====================== Helper Methods ======================

    private float[] createCubeVertexCol(float[] CubeColorArray) {
        float[] cubeColors = new float[CubeColorArray.length * 4 * 6];
        for (int i = 0; i < cubeColors.length; i++) {
            cubeColors[i] = CubeColorArray[i % CubeColorArray.length];
        }
        return cubeColors;
    }

    public static float[] createCube(float x, float y, float z) {
        int offset = CUBE_LENGTH / 2;
        return new float[]{
            // TOP QUAD
            x + offset, y + offset, z,
            x - offset, y + offset, z,
            x - offset, y + offset, z - CUBE_LENGTH,
            x + offset, y + offset, z - CUBE_LENGTH,
            // BOTTOM QUAD
            x + offset, y - offset, z - CUBE_LENGTH,
            x - offset, y - offset, z - CUBE_LENGTH,
            x - offset, y - offset, z,
            x + offset, y - offset, z,
            // FRONT QUAD
            x + offset, y + offset, z - CUBE_LENGTH,
            x - offset, y + offset, z - CUBE_LENGTH,
            x - offset, y - offset, z - CUBE_LENGTH,
            x + offset, y - offset, z - CUBE_LENGTH,
            // BACK QUAD
            x + offset, y - offset, z,
            x - offset, y - offset, z,
            x - offset, y + offset, z,
            x + offset, y + offset, z,
            // LEFT QUAD
            x - offset, y + offset, z - CUBE_LENGTH,
            x - offset, y + offset, z,
            x - offset, y - offset, z,
            x - offset, y - offset, z - CUBE_LENGTH,
            // RIGHT QUAD
            x + offset, y + offset, z,
            x + offset, y + offset, z - CUBE_LENGTH,
            x + offset, y - offset, z - CUBE_LENGTH,
            x + offset, y - offset, z
        };
    }

    private float[] getCubeColor(Block block) {
        return new float[]{1.0f, 1.0f, 1.0f};
    }

    public static float[] createTexCube(float x, float y, Block block) {
        float offset = (1024f / 16) / 1024f;

        switch (block.GetID()) {
            case 0: // Grass
                return new float[]{
                    // BOTTOM
                    x + offset*3, y + offset*10, x + offset*2, y + offset*10,
                    x + offset*2, y + offset*9, x + offset*3, y + offset*9,
                    // TOP
                    x + offset*3, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0, x + offset*3, y + offset*0,
                    // FRONT
                    x + offset*3, y + offset*0, x + offset*4, y + offset*0,
                    x + offset*4, y + offset*1, x + offset*3, y + offset*1,
                    // BACK
                    x + offset*4, y + offset*1, x + offset*3, y + offset*1,
                    x + offset*3, y + offset*0, x + offset*4, y + offset*0,
                    // LEFT
                    x + offset*3, y + offset*0, x + offset*4, y + offset*0,
                    x + offset*4, y + offset*1, x + offset*3, y + offset*1,
                    // RIGHT
                    x + offset*3, y + offset*0, x + offset*4, y + offset*0,
                    x + offset*4, y + offset*1, x + offset*3, y + offset*1
                };

            case 1: // Dirt
                return new float[]{
                    x + offset*3, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0, x + offset*3, y + offset*0,
                    x + offset*3, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0, x + offset*3, y + offset*0,
                    x + offset*2, y + offset*0, x + offset*3, y + offset*0,
                    x + offset*3, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*3, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0, x + offset*3, y + offset*0,
                    x + offset*2, y + offset*0, x + offset*3, y + offset*0,
                    x + offset*3, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0, x + offset*3, y + offset*0,
                    x + offset*3, y + offset*1, x + offset*2, y + offset*1
                };

            case 2: // Water
                return new float[]{
                    x + offset*15, y + offset*12, x + offset*14, y + offset*12,
                    x + offset*14, y + offset*13, x + offset*15, y + offset*13,
                    x + offset*15, y + offset*13, x + offset*14, y + offset*13,
                    x + offset*14, y + offset*12, x + offset*15, y + offset*12,
                    x + offset*14, y + offset*12, x + offset*15, y + offset*12,
                    x + offset*15, y + offset*13, x + offset*14, y + offset*13,
                    x + offset*15, y + offset*13, x + offset*14, y + offset*13,
                    x + offset*14, y + offset*12, x + offset*15, y + offset*12,
                    x + offset*14, y + offset*12, x + offset*15, y + offset*12,
                    x + offset*15, y + offset*13, x + offset*14, y + offset*13,
                    x + offset*14, y + offset*12, x + offset*15, y + offset*12,
                    x + offset*15, y + offset*13, x + offset*14, y + offset*13
                };

            case 3: // Sand
                return new float[]{
                    x + offset*3, y + offset*2, x + offset*2, y + offset*2,
                    x + offset*2, y + offset*1, x + offset*3, y + offset*1,
                    x + offset*3, y + offset*2, x + offset*2, y + offset*2,
                    x + offset*2, y + offset*1, x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1, x + offset*3, y + offset*1,
                    x + offset*3, y + offset*2, x + offset*2, y + offset*2,
                    x + offset*3, y + offset*2, x + offset*2, y + offset*2,
                    x + offset*2, y + offset*1, x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1, x + offset*3, y + offset*1,
                    x + offset*3, y + offset*2, x + offset*2, y + offset*2,
                    x + offset*2, y + offset*1, x + offset*3, y + offset*1,
                    x + offset*3, y + offset*2, x + offset*2, y + offset*2
                };

            case 4: // Stone
                return new float[]{
                    x + offset*2, y + offset*1, x + offset*1, y + offset*1,
                    x + offset*1, y + offset*0, x + offset*2, y + offset*0,
                    x + offset*2, y + offset*1, x + offset*1, y + offset*1,
                    x + offset*1, y + offset*0, x + offset*2, y + offset*0,
                    x + offset*1, y + offset*0, x + offset*2, y + offset*0,
                    x + offset*2, y + offset*1, x + offset*1, y + offset*1,
                    x + offset*2, y + offset*1, x + offset*1, y + offset*1,
                    x + offset*1, y + offset*0, x + offset*2, y + offset*0,
                    x + offset*1, y + offset*0, x + offset*2, y + offset*0,
                    x + offset*2, y + offset*1, x + offset*1, y + offset*1,
                    x + offset*1, y + offset*0, x + offset*2, y + offset*0,
                    x + offset*2, y + offset*1, x + offset*1, y + offset*1
                };

            case 5: // Bedrock
                return new float[]{
                    x + offset*2, y + offset*2, x + offset*1, y + offset*2,
                    x + offset*1, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*2, x + offset*1, y + offset*2,
                    x + offset*1, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*2, x + offset*1, y + offset*2,
                    x + offset*2, y + offset*2, x + offset*1, y + offset*2,
                    x + offset*1, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*2, x + offset*1, y + offset*2,
                    x + offset*1, y + offset*1, x + offset*2, y + offset*1,
                    x + offset*2, y + offset*2, x + offset*1, y + offset*2
                };

            default: // Default (TNT-like)
                return new float[]{
                    x + offset*10, y + offset*1, x + offset*9, y + offset*1,
                    x + offset*9, y + offset*0, x + offset*10, y + offset*0,
                    x + offset*11, y + offset*1, x + offset*10, y + offset*1,
                    x + offset*10, y + offset*0, x + offset*11, y + offset*0,
                    x + offset*8, y + offset*0, x + offset*9, y + offset*0,
                    x + offset*9, y + offset*1, x + offset*8, y + offset*1,
                    x + offset*9, y + offset*1, x + offset*8, y + offset*1,
                    x + offset*8, y + offset*0, x + offset*9, y + offset*0,
                    x + offset*8, y + offset*0, x + offset*9, y + offset*0,
                    x + offset*9, y + offset*1, x + offset*8, y + offset*1,
                    x + offset*8, y + offset*0, x + offset*9, y + offset*0,
                    x + offset*9, y + offset*1, x + offset*8, y + offset*1
                };
        }
    }
}