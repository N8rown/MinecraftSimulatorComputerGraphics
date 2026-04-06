/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
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
    private int startX, startY, startZ;
    private Random r;
    private int VBOTextureHandle;
    private Texture texture;
    
    public void render()
    {
        glPushMatrix();
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, VBOColorHandle);
        glColorPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBindTexture(GL_TEXTURE_2D, 1);
        glTexCoordPointer(2,GL_FLOAT,0,0L);

        glDrawArrays(GL_QUADS, 0, CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE * 24);
        glPopMatrix();
        
    }
    
    public void rebuildMesh( float startX, float startY, float startZ) {
        //Noise Variables
        int largestFeature = 28;//Height variance is between 1 and largest Feature
        double persistence = 0.5; //between 0 and 1;
        int seed = 100010101;
        SimplexNoise noise = new SimplexNoise(largestFeature, persistence, seed);
        //
        VBOColorHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        VBOTextureHandle = glGenBuffers(); 
        FloatBuffer VertexPositionData = BufferUtils.createFloatBuffer(
            (CHUNK_SIZE * CHUNK_SIZE *CHUNK_SIZE) * 6 * 12);
        FloatBuffer VertexColorData = BufferUtils.createFloatBuffer(
            (CHUNK_SIZE * CHUNK_SIZE * CHUNK_SIZE) * 6 * 12);
        FloatBuffer VertexTextureData = BufferUtils.createFloatBuffer(
            (CHUNK_SIZE* CHUNK_SIZE *CHUNK_SIZE)* 6 * 12);

        for (float x = 0; x < CHUNK_SIZE; x += 1) {
            for (float z = 0; z < CHUNK_SIZE; z += 1) {
                //Could Caluculate YMax here and only iterate up to ymax in y for loop
                for(float y = 0; y < CHUNK_SIZE; y++) //Could change chunk size to max height
                {
                    //Noise
                    //int i=(int)(startX+x*((CHUNK_SIZE-startX)/xResolution));
                    //int k=(int)(startZ+z*((CHUNK_SIZE-startZ)/zResolution));
                    //Get Noise is between -1 and 1 and is scaled by 100
                    int maxHeight = (int)(startY + 
                        (int)(100 * noise.getNoise((int)x,(int)y, (int)z)) * CUBE_LENGTH);
                    if (y < maxHeight){
                        VertexPositionData.put(createCube(
                            (float)(startX + x * CUBE_LENGTH),
                            (float)(y*CUBE_LENGTH+ (int)(CHUNK_SIZE*.8)), //Why is y different then x and Z?
                            (float) (startZ + z * CUBE_LENGTH)));
                        VertexColorData.put(createCubeVertexCol(getCubeColor(
                                Blocks[(int) x][(int) y][(int) z])));
                        VertexTextureData.put(createTexCube((float) 0, (float)
                                0,Blocks[(int)(x)][(int) (y)][(int) (z)]));
                    }
                }
            }
        } 
        VertexColorData.flip();
        VertexPositionData.flip();
        VertexTextureData.flip();
        glBindBuffer(GL_ARRAY_BUFFER, VBOVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexPositionData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, VBOColorHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexColorData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ARRAY_BUFFER, VBOTextureHandle);
        glBufferData(GL_ARRAY_BUFFER, VertexTextureData, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

    }
    
    
    
    private float[] createCubeVertexCol(float[] CubeColorArray) {
        float[] cubeColors = new float[CubeColorArray.length * 4 * 6];
        for (int i = 0; i < cubeColors.length; i++) {
            cubeColors[i] = CubeColorArray[i %
            CubeColorArray.length];
        }
        return cubeColors;
    }
    
    
    public static float[] createCube(float x, float y, float z) {
        int offset = CUBE_LENGTH / 2;
        return new float[] {
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
        return new float[] { 1, 1, 1 };
    }
    
    
    public static float[] createTexCube(float x, float y, Block block) {
        float offset = (1024f/16)/1024f;
        switch (block.GetID()) {
            case 0: //Grass
                return new float[]
                {
                    // BOTTOM QUAD(DOWN=+Y) (TOP OF BLOCK)
                    x + offset*3, y + offset*10, 
                    x + offset*2, y + offset*10,
                    x + offset*2, y + offset*9,
                    x + offset*3, y + offset*9,
                    // TOP QUAD!    BOTTOM OF BLOCK
                    x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0,
                    x + offset*3, y + offset*0,
                    // FRONT QUAD
                    x + offset*3, y + offset*0,
                    x + offset*4, y + offset*0,
                    x + offset*4, y + offset*1,
                    x + offset*3, y + offset*1,
                    // BACK QUAD
                    x + offset*4, y + offset*1,
                    x + offset*3, y + offset*1,
                    x + offset*3, y + offset*0,
                    x + offset*4, y + offset*0,
                    // LEFT QUAD
                    x + offset*3, y + offset*0,
                    x + offset*4, y + offset*0,
                    x + offset*4, y + offset*1,
                    x + offset*3, y + offset*1,
                    // RIGHT QUAD
                    x + offset*3, y + offset*0,
                    x + offset*4, y + offset*0,
                    x + offset*4, y + offset*1,
                    x + offset*3, y + offset*1
                };
            case 1: //Dirt
                return new float[]
                {
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0,
                    x + offset*3, y + offset*0,
                    // TOP!
                    x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0,
                    x + offset*3, y + offset*0,
                    // FRONT QUAD
                    x + offset*2, y + offset*0,
                    x + offset*3, y + offset*0,
                    x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1,
                    // BACK QUAD
                    x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1,
                    x + offset*2, y + offset*0,
                    x + offset*3, y + offset*0,
                    // LEFT QUAD
                    x + offset*2, y + offset*0,
                    x + offset*3, y + offset*0,
                    x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1,
                    // RIGHT QUAD
                    x + offset*2, y + offset*0,
                    x + offset*3, y + offset*0,
                    x + offset*3, y + offset*1,
                    x + offset*2, y + offset*1
                };
            case 2: //water
                return new float[]
                {
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset*15, y + offset*12,
                    x + offset*14, y + offset*12,
                    x + offset*14, y + offset*13,
                    x + offset*15, y + offset*13,
                    // TOP!
                    x + offset*15, y + offset*13,
                    x + offset*14, y + offset*13,
                    x + offset*14, y + offset*12,
                    x + offset*15, y + offset*12,
                    // FRONT QUAD
                    x + offset*14, y + offset*12,
                    x + offset*15, y + offset*12,
                    x + offset*15, y + offset*13,
                    x + offset*14, y + offset*13,
                    // BACK QUAD
                    x + offset*15, y + offset*13,
                    x + offset*14, y + offset*13,
                    x + offset*14, y + offset*12,
                    x + offset*15, y + offset*12,
                    // LEFT QUAD
                    x + offset*14, y + offset*12,
                    x + offset*15, y + offset*12,
                    x + offset*15, y + offset*13,
                    x + offset*14, y + offset*13,
                    // RIGHT QUAD
                    x + offset*14, y + offset*12,
                    x + offset*15, y + offset*12,
                    x + offset*15, y + offset*13,
                    x + offset*14, y + offset*13
                };
            case 3: //Sand
                return new float[]
                {
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset*3, y + offset*2,
                    x + offset*2, y + offset*2,
                    x + offset*2, y + offset*1,
                    x + offset*3, y + offset*1,
                    // TOP!
                    x + offset*3, y + offset*2,
                    x + offset*2, y + offset*2,
                    x + offset*2, y + offset*1,
                    x + offset*3, y + offset*1,
                    // FRONT QUAD
                    x + offset*2, y + offset*1,
                    x + offset*3, y + offset*1,
                    x + offset*3, y + offset*2,
                    x + offset*2, y + offset*2,
                    // BACK QUAD
                    x + offset*3, y + offset*2,
                    x + offset*2, y + offset*2,
                    x + offset*2, y + offset*1,
                    x + offset*3, y + offset*1,
                    // LEFT QUAD
                    x + offset*2, y + offset*1,
                    x + offset*3, y + offset*1,
                    x + offset*3, y + offset*2,
                    x + offset*2, y + offset*2,
                    // RIGHT QUAD
                    x + offset*2, y + offset*1,
                    x + offset*3, y + offset*1,
                    x + offset*3, y + offset*2,
                    x + offset*2, y + offset*2
                };
            case 4: //Stone
                return new float[]
                {
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1,
                    x + offset*1, y + offset*0,
                    x + offset*2, y + offset*0,
                    // TOP!
                    x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1,
                    x + offset*1, y + offset*0,
                    x + offset*2, y + offset*0,
                    // FRONT QUAD
                    x + offset*1, y + offset*0,
                    x + offset*2, y + offset*0,
                    x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1,
                    // BACK QUAD
                    x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1,
                    x + offset*1, y + offset*0,
                    x + offset*2, y + offset*0,
                    // LEFT QUAD
                    x + offset*1, y + offset*0,
                    x + offset*2, y + offset*0,
                    x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1,
                    // RIGHT QUAD
                    x + offset*1, y + offset*0,
                    x + offset*2, y + offset*0,
                    x + offset*2, y + offset*1,
                    x + offset*1, y + offset*1
                };
                case 5: //Bedrock
                return new float[]
                {
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset*2, y + offset*2,
                    x + offset*1, y + offset*2,
                    x + offset*1, y + offset*1,
                    x + offset*2, y + offset*1,
                    // TOP!
                    x + offset*2, y + offset*2,
                    x + offset*1, y + offset*2,
                    x + offset*1, y + offset*1,
                    x + offset*2, y + offset*1,
                    // FRONT QUAD
                    x + offset*1, y + offset*1,
                    x + offset*2, y + offset*1,
                    x + offset*2, y + offset*2,
                    x + offset*1, y + offset*2,
                    // BACK QUAD
                    x + offset*2, y + offset*2,
                    x + offset*1, y + offset*2,
                    x + offset*1, y + offset*1,
                    x + offset*2, y + offset*1,
                    // LEFT QUAD
                    x + offset*1, y + offset*1,
                    x + offset*2, y + offset*1,
                    x + offset*2, y + offset*2,
                    x + offset*1, y + offset*2,
                    // RIGHT QUAD
                    x + offset*1, y + offset*1,
                    x + offset*2, y + offset*1,
                    x + offset*2, y + offset*2,
                    x + offset*1, y + offset*2
                };
            default: 
                return new float[]
                { //TNT SINCE DEFUALT REPRESENTS ERRORS
                    // BOTTOM QUAD(DOWN=+Y)
                    x + offset*10, y + offset*1,
                    x + offset*9, y + offset*1,
                    x + offset*9, y + offset*0,
                    x + offset*10, y + offset*0,
                    // TOP!
                    x + offset*11, y + offset*1,
                    x + offset*10, y + offset*1,
                    x + offset*10, y + offset*0,
                    x + offset*11, y + offset*0,
                    // FRONT QUAD
                    x + offset*8, y + offset*0,
                    x + offset*9, y + offset*0,
                    x + offset*9, y + offset*1,
                    x + offset*8, y + offset*1,
                    // BACK QUAD
                    x + offset*9, y + offset*1,
                    x + offset*8, y + offset*1,
                    x + offset*8, y + offset*0,
                    x + offset*9, y + offset*0,
                    // LEFT QUAD
                    x + offset*8, y + offset*0,
                    x + offset*9, y + offset*0,
                    x + offset*9, y + offset*1,
                    x + offset*8, y + offset*1,
                    // RIGHT QUAD
                    x + offset*8, y + offset*0,
                    x + offset*9, y + offset*0,
                    x + offset*9, y + offset*1,
                    x + offset*8, y + offset*1
                };
        }
    }

    
    
    public Chunk(int startX, int startY, int startZ) { //Random
        try{texture = TextureLoader.getTexture("PNG",
            ResourceLoader.getResourceAsStream("terrain.png"));
        }
        catch(Exception e)
        {
            System.out.print("Texterror"); //Texturerror 
        }
        
        r= new Random();
        Blocks = new Block[CHUNK_SIZE][CHUNK_SIZE][CHUNK_SIZE];
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    if (y == 0)
                        Blocks[x][y][z] = 
                            new Block(Block.BlockType.BlockType_Bedrock);
                    else if((r.nextFloat()>0.7f) || (y ==(CHUNK_SIZE - 1))){
                        Blocks[x][y][z] = 
                            new Block(Block.BlockType.BlockType_Grass);
                    }else if(r.nextFloat()>0.4f){
                        Blocks[x][y][z] = new
                            Block(Block.BlockType.BlockType_Dirt);
                    }else if(r.nextFloat()>0.2f){
                        Blocks[x][y][z] = new
                            Block(Block.BlockType.BlockType_Water);
                    }else{
                        Blocks[x][y][z] = new
                            Block(Block.BlockType.BlockType_Default);
                    }
                }
            }
        }
        VBOColorHandle = glGenBuffers();
        VBOVertexHandle = glGenBuffers();
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        rebuildMesh(startX, startY, startZ);
    }
}



