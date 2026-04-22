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

    static final int CHUNK_SIZE = 32;
    static final int CUBE_LENGTH = 2;

    private Block[][][] Blocks;
    // Solid VBOs
    private int solidVBOVertexHandle;
    private int solidVBOColorHandle;
    private int solidVBOTextureHandle;

    // Water VBOs
    private int waterVBOVertexHandle;
    private int waterVBOColorHandle;
    private int waterVBOTextureHandle;

    private int solidVertexCount;
    private int waterVertexCount;
    
    private Texture texture;

    public int startX, startY, startZ;
    public int maxX, maxY, maxZ;
    private Random r;

    public Chunk(int startX, int startY, int startZ) {
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.maxX = startX + CHUNK_SIZE * CUBE_LENGTH;
        this.maxY = startY + CHUNK_SIZE * CUBE_LENGTH;
        this.maxZ = startZ + CHUNK_SIZE * CUBE_LENGTH;
        
        r = new Random();
        Blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];

        try {
            texture = TextureLoader.getTexture("PNG",
                    ResourceLoader.getResourceAsStream("terrain.png"));
        } catch (Exception e) {
            System.out.println("Texture loading error: " + e.getMessage());
        }

        buildChunk(startX, startY, startZ);
        rebuildMesh();
    }
     public void buildChunk(float startX, float startY, float startZ) {
        // Noise setup
        int largestFeature = r.nextInt(20, CHUNK_SIZE * 2);
        double persistence = r.nextDouble(0.4, 0.75);
        int seed = r.nextInt(1000000);
        SimplexNoise noise = new SimplexNoise(largestFeature, persistence, seed);

        final int SEA_LEVEL = (int) (CHUNK_SIZE * 0.45f);

        

        // Clear old block references
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Blocks[x][y][z] = null;
                }
            }
        }

        // Generate terrain + water
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                double noiseVal = noise.getNoise(x, z);
                int surfaceHeight = (int) (CHUNK_SIZE * 0.35 + noiseVal * CHUNK_SIZE * 0.4);
                surfaceHeight = Math.max(5, Math.min(surfaceHeight, CHUNK_SIZE - 2));

                // Generate terrain first
                for (int y = 0; y < surfaceHeight; y++) {
                    Block.BlockType type;

                    if (y == 0) {
                        type = Block.BlockType.BlockType_Bedrock;
                    } else if (y < surfaceHeight - 4) {
                        type = Block.BlockType.BlockType_Stone;
                    } else if (y < surfaceHeight - 1) {
                        type = Block.BlockType.BlockType_Dirt;
                    } else {
                        if (surfaceHeight <= SEA_LEVEL + 2) {
                            type = Block.BlockType.BlockType_Sand;
                        } else {
                            type = Block.BlockType.BlockType_Grass;
                        }
                    }

                    Blocks[x][y][z] = new Block(type);
                    Blocks[x][y][z].setActive(true);
                }

                // Fill water downward from sea level until reaching terrain
                // If terrain is below sea level, water occupies every air block above it
                if (surfaceHeight <= SEA_LEVEL) {
                    for (int y = surfaceHeight; y <= SEA_LEVEL && y < CHUNK_SIZE; y++) {
                        if (Blocks[x][y][z] == null) {
                            Blocks[x][y][z] = new Block(Block.BlockType.BlockType_Water);
                            Blocks[x][y][z].setActive(true);
                        }
                    }
                }
            }
        }
    }
    public void rebuildMesh()
    {
        // Clean up old VBOs if rebuild is called again
        if (solidVBOVertexHandle != 0) glDeleteBuffers(solidVBOVertexHandle);
        if (solidVBOColorHandle != 0) glDeleteBuffers(solidVBOColorHandle);
        if (solidVBOTextureHandle != 0) glDeleteBuffers(solidVBOTextureHandle);

        if (waterVBOVertexHandle != 0) glDeleteBuffers(waterVBOVertexHandle);
        if (waterVBOColorHandle != 0) glDeleteBuffers(waterVBOColorHandle);
        if (waterVBOTextureHandle != 0) glDeleteBuffers(waterVBOTextureHandle);
        
        solidVBOColorHandle = glGenBuffers();
        solidVBOVertexHandle = glGenBuffers();
        solidVBOTextureHandle = glGenBuffers();

        waterVBOColorHandle = glGenBuffers();
        waterVBOVertexHandle = glGenBuffers();
        waterVBOTextureHandle = glGenBuffers();

        int maxCubeCount = CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE;
        /*24 = 24 vertices per full cube = 6 faces × 4 vertices each
          3 = x,y,z per vertex
          4 = r,g,b,a per vertex
          2 = u,v per vertex*/
        FloatBuffer solidVertexPositionData = BufferUtils.createFloatBuffer(maxCubeCount * 24 * 3);
        FloatBuffer solidVertexColorData = BufferUtils.createFloatBuffer(maxCubeCount * 24 * 4);
        FloatBuffer solidVertexTextureData = BufferUtils.createFloatBuffer(maxCubeCount * 24 * 2);

        FloatBuffer waterVertexPositionData = BufferUtils.createFloatBuffer(maxCubeCount * 24 * 3);
        FloatBuffer waterVertexColorData = BufferUtils.createFloatBuffer(maxCubeCount * 24 * 4);
        FloatBuffer waterVertexTextureData = BufferUtils.createFloatBuffer(maxCubeCount * 24 * 2);

        solidVertexCount = 0;
        waterVertexCount = 0;
        // Build VBO buffers(Mesh the block)
        //Back face culling could eb implemented here i believe
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    Block block = Blocks[x][y][z];

                    if (block == null || !block.isActive()) {
                        continue;
                    }

                    float worldX = startX + x * CUBE_LENGTH;
                    float worldY = startY + y * CUBE_LENGTH;
                    float worldZ = startZ + z * CUBE_LENGTH;

                    if (block.getType() == Block.BlockType.BlockType_Water) {
                        waterVertexPositionData.put(createCube(worldX, worldY, worldZ));
                        waterVertexColorData.put(createCubeVertexCol(getCubeColor(block)));
                        waterVertexTextureData.put(createTexCube(0, 0, block));
                        waterVertexCount += 24;
                    } else {
                        solidVertexPositionData.put(createCube(worldX, worldY, worldZ));
                        solidVertexColorData.put(createCubeVertexCol(getCubeColor(block)));
                        solidVertexTextureData.put(createTexCube(0, 0, block));
                        solidVertexCount += 24;
                    }
                }
            }
        }

        // Flip buffers
        solidVertexPositionData.flip();
        solidVertexColorData.flip();
        solidVertexTextureData.flip();

        waterVertexPositionData.flip();
        waterVertexColorData.flip();
        waterVertexTextureData.flip();

        // Upload solid buffers
        glBindBuffer(GL_ARRAY_BUFFER, solidVBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, solidVertexPositionData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, solidVBOColorHandle);
        glBufferData(GL_ARRAY_BUFFER, solidVertexColorData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, solidVBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, solidVertexTextureData, GL_STATIC_DRAW);

        // Upload water buffers
        glBindBuffer(GL_ARRAY_BUFFER, waterVBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, waterVertexPositionData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, waterVBOColorHandle);
        glBufferData(GL_ARRAY_BUFFER, waterVertexColorData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, waterVBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, waterVertexTextureData, GL_STATIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void render() {
        glPushMatrix();

        glBindTexture(GL_TEXTURE_2D, texture.getTextureID());
        
        // Render solid blocks first
        if (solidVertexCount > 0) {
            glBindBuffer(GL_ARRAY_BUFFER, solidVBOVertexHandle);
            glVertexPointer(3, GL_FLOAT, 0, 0L);

            glBindBuffer(GL_ARRAY_BUFFER, solidVBOColorHandle);
            glColorPointer(4, GL_FLOAT, 0, 0L);

            glBindBuffer(GL_ARRAY_BUFFER, solidVBOTextureHandle);
            glTexCoordPointer(2, GL_FLOAT, 0, 0L);

            glDrawArrays(GL_QUADS, 0, solidVertexCount);
        }

        // Render water second
        if (waterVertexCount > 0) { 
            glEnable(GL_BLEND);
            //glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); //Move to init
            glDepthMask(false);
            //glDisable(GL_CULL_FACE);

            glBindBuffer(GL_ARRAY_BUFFER, waterVBOVertexHandle);
            glVertexPointer(3, GL_FLOAT, 0, 0L);

            glBindBuffer(GL_ARRAY_BUFFER, waterVBOColorHandle);
            glColorPointer(4, GL_FLOAT, 0, 0L);

            glBindBuffer(GL_ARRAY_BUFFER, waterVBOTextureHandle);
            glTexCoordPointer(2, GL_FLOAT, 0, 0L);

            glDrawArrays(GL_QUADS, 0, waterVertexCount);

            glDepthMask(true);
            glDisable(GL_BLEND);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopMatrix();
    }


    // ====================== Helper Methods ======================

    private float[] createCubeVertexCol(float[] cubeColorArray) {
        float[] cubeColors = new float[24 * 4]; // 24 vertices, RGBA
        for (int i = 0; i < 24; i++) {
            cubeColors[i * 4] = cubeColorArray[0];
            cubeColors[i * 4 + 1] = cubeColorArray[1];
            cubeColors[i * 4 + 2] = cubeColorArray[2];
            cubeColors[i * 4 + 3] = cubeColorArray[3];
        }
        return cubeColors;
    }

    public static float[] createCube(float x, float y, float z) {
        int offset = CUBE_LENGTH / 2;
        return new float[]{
            // TOP Of Block QUAD
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
        if (block.getType() == Block.BlockType.BlockType_Water) {
            return new float[]{1.0f, 1.0f, 1.0f, 0.1f}; //0.45 is the transparency
        }
        return new float[]{1.0f, 1.0f, 1.0f, 1.0f};
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
    public boolean containsWorldPoint(float worldX, float worldY, float worldZ) {
        float minX = Math.min(startX, startX + (CHUNK_SIZE - 1) * CUBE_LENGTH) - CUBE_LENGTH / 2.0f;
        float maxXv = Math.max(startX, startX + (CHUNK_SIZE - 1) * CUBE_LENGTH) + CUBE_LENGTH / 2.0f;

        float minY = Math.min(startY, startY + (CHUNK_SIZE - 1) * CUBE_LENGTH) - CUBE_LENGTH / 2.0f;
        float maxYv = Math.max(startY, startY + (CHUNK_SIZE - 1) * CUBE_LENGTH) + CUBE_LENGTH / 2.0f;

        float minZ = Math.min(startZ, startZ + (CHUNK_SIZE - 1) * CUBE_LENGTH) - CUBE_LENGTH / 2.0f;
        float maxZv = Math.max(startZ, startZ + (CHUNK_SIZE - 1) * CUBE_LENGTH) + CUBE_LENGTH / 2.0f;

        boolean result =
            worldX >= minX && worldX <= maxXv &&
            worldY >= minY && worldY <= maxYv &&
            worldZ >= minZ && worldZ <= maxZv;
        return result;
    }
    public int[] worldToBlock(float worldX, float worldY, float worldZ) {
        if (!containsWorldPoint(worldX, worldY, worldZ)) {
            return null;
        }

        int blockX = Math.round((worldX - startX) / (float)CUBE_LENGTH);
        int blockY = Math.round((worldY - startY) / (float)CUBE_LENGTH);
        int blockZ = Math.round((worldZ - startZ) / (float)CUBE_LENGTH);

        if (blockX < 0 || blockX >= CHUNK_SIZE ||
            blockY < 0 || blockY >= CHUNK_SIZE ||
            blockZ < 0 || blockZ >= CHUNK_SIZE) 
        {
            return null;
        }
        return new int[]{blockX, blockY, blockZ};
    }
    
    public boolean breakBlock(int x, int y, int z) {
        Blocks[x][y][z] = null;
        rebuildMesh();
        return true;
    }
    public Block getBlock(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) {
            return null;
        }
        return Blocks[x][y][z];
    }
}
