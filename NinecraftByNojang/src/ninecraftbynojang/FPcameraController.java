package ninecraftbynojang;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author n8bro
 */
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.Sys;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

public class FPcameraController {
    //3d vector to store the camera's position in
    private Vector3f position = null;
    private Vector3f lPosition = null;
    //the rotation around the Y axis of the camera
    private float yaw = 0.0f;
    //the rotation around the X axis of the camera
    private float pitch = 0.0f;
    
    private Chunk currentChunk;// NEEDS TO BE INITIALIZED
    private boolean leftMouseWasDown = false;
    
    public FPcameraController(float x, float y, float z)
    {
        //instantiate position Vector3f to the x y z params.
        position = new Vector3f(x, y, z);
        lPosition = new Vector3f(x,y,z);
        lPosition.x = 0f;
        lPosition.y = 0f; 
        lPosition.z = 80f;
    }
    //increment the camera's current yaw rotation
    public void yaw(float amount)
    {
        //increment the yaw by the amount param
        yaw += amount;
    }
    //increment the camera's current yaw rotation
    public void pitch(float amount)
    {
        //increment the pitch by the amount param
        pitch -= amount;
    }
    
    //moves the camera forward relative to its current rotation (yaw)
    public void walkForward(float distance)
    {
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw));
        position.x -= xOffset;
        position.z += zOffset;
    }
    
    //moves the camera backward relative to its current rotation (yaw)
    public void walkBackwards(float distance)
    {   
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw));
        position.x += xOffset;
        position.z -= zOffset;
    }
    //strafes the camera left relative to its current rotation (yaw)
    public void strafeLeft(float distance)
    {
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw-90));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw-90));
        position.x -= xOffset;
        position.z += zOffset;
    }
    //strafes the camera right relative to its current rotation (yaw)
    public void strafeRight(float distance)
    {
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw+90));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw+90));
        position.x -= xOffset;
        position.z += zOffset;
    }
    //moves the camera up relative to its current rotation (yaw)
    public void moveUp(float distance)
    {
        position.y -= distance;
    }
    //moves the camera down
    public void moveDown(float distance)
    {
        position.y += distance;
    }
    
    //translates and rotate the matrix so that it looks through the camera
    //this does basically what gluLookAt() does
    public void lookThrough()
    {
        //roatate the pitch around the X axis
        glRotatef(pitch, 1.0f, 0.0f, 0.0f);
        //roatate the yaw around the Y axis
        glRotatef(yaw, 0.0f, 1.0f, 0.0f);
        //translate to the position vector's location
        glTranslatef(position.x, position.y, position.z);
        //Lighting  addition
        FloatBuffer lightPosition = BufferUtils.createFloatBuffer(4);
        lightPosition.put(lPosition.x).put(lPosition.y).put(lPosition.z).put(1.0f).flip();
        glLight(GL_LIGHT0, GL_POSITION, lightPosition);

    }
    private Vector3f getLookVector() {
        float cosPitch = (float)Math.cos(Math.toRadians(pitch));
        float sinPitch = (float)Math.sin(Math.toRadians(pitch));
        float sinYaw = (float)Math.sin(Math.toRadians(yaw));
        float cosYaw = (float)Math.cos(Math.toRadians(yaw));

        return new Vector3f( sinYaw * cosPitch, -sinPitch, -cosYaw * cosPitch);
    }

    private void breakBlockInFront() {
        float reach = 6.0f;
        float step = 0.25f;

        Vector3f look = getLookVector();

        // Because your camera uses glTranslatef(position.x, position.y, position.z),
        // the world-space camera location is the negative of position.
        float camX = -position.x;
        float camY = -position.y;
        float camZ = -position.z;

        for (float t = 0; t <= reach; t += step) {
            float worldX = camX + look.x * t;
            float worldY = camY + look.y * t;
            float worldZ = camZ + look.z * t;

            boolean inside = currentChunk.containsWorldPoint(worldX, worldY, worldZ);

            if (!inside) {
                continue;
            }
            
            int[] blockPos = currentChunk.worldToBlock(worldX, worldY, worldZ);
            if (blockPos == null) {
                continue;
            }

            int bx = blockPos[0];
            int by = blockPos[1];
            int bz = blockPos[2];
            Block block = currentChunk.getBlock(bx, by, bz);

            if (block == null) {
                continue;
            }

            if (block.isActive()) {
                boolean broken = currentChunk.breakBlock(bx, by, bz);

                if (broken) {
                    return;
                }
            }
        }
    }
    public void gameLoop()
    {
        float dx, dy, time;
        float dt = 0.0f; //length of frame
        float lastTime = 0.0f; // when the last frame was
        float mouseSensitivity = 0.09f;
        float movementSpeed = .35f;
        currentChunk = new Chunk(0,0,0); //Each block is 2 Wide. This 
        //Chunk is (-30 to 30, -30 to 30, -30 to 30)
        //hide the mouse
        Mouse.setGrabbed(true);
        // keep looping till the display window is closed the ESC key is down
        while (!Display.isCloseRequested() &&
                !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
        {
            time = Sys.getTime();
            lastTime = time;
            //distance in mouse movement from the last getDX() call.
            dx = Mouse.getDX();
            //distance in mouse movement from the last getDY() call.
            dy = Mouse.getDY();
            //when passing in the distance to move
            //we times the movementSpeed with dt this is a time scale
            //so if its a slow frame u move more then a fast frame
            //so on a slow computer you move just as fast as on a fast computer
            
            //POSSIBLY MOVE CAMERA MOVEMENT HERE INSTEAD

            if (Keyboard.isKeyDown(Keyboard.KEY_W))//move forward
            {
                walkForward(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S))//move backwards
            {
                walkBackwards(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_A))//strafe left 
            {
                strafeLeft(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D))//strafe right 
            {
                strafeRight(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_SPACE))//move up 
            {
                moveUp(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                moveDown(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
                moveDown(movementSpeed);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_CAPITAL)) {
                movementSpeed = 0.7f;
            }
            else
                movementSpeed = 0.35f;
            boolean leftMouseDown = Mouse.isButtonDown(0);
            if (leftMouseDown && !leftMouseWasDown) {
                breakBlockInFront();
            }
            leftMouseWasDown = leftMouseDown;
            
            yaw(dx * mouseSensitivity);
            pitch(dy * mouseSensitivity);
            
            //set the modelview matrix back to the identity
            glLoadIdentity();
            //look through the camera before you draw anything
            lookThrough();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            //you would draw your scene here.
            currentChunk.render(); //CHANGED TO CHUNK RENDER
            //draw the buffer to the screen
            Display.update();
            Display.sync(60);
        }
        Display.destroy();
    }
}

