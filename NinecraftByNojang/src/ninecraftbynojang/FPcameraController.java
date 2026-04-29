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
    private boolean rightMouseWasDown = false;
    private boolean FKeyWasDown = false;
    private Block.BlockType selectedBlock = Block.BlockType.BlockType_Grass;
    
    // Physics variables
    private float velocityY = 0.0f;
    private boolean isOnGround = false;
    private boolean isInWater = false;
    private boolean isFlying = false; // Fly mode toggle
    private static final float GRAVITY = 9.8f * 2;
    private static final float JUMP_POWER = 7.0f * 1.5f;
    private static final float WATER_BUOYANCY = 4.0f;
    private static final float WATER_DRAG = 0.95f;
    private static final float FLY_SPEED = 8.0f;
    private static final float PLAYER_HEIGHT = 2.5f;
    private static final float PLAYER_WIDTH = 0.6f;
    
    public FPcameraController(float x, float y, float z)
    {
        //instantiate position Vector3f to the x y z params.
        position = new Vector3f(x, y, z);
        lPosition = new Vector3f(x,y,z);
        lPosition.x = 0f;
        lPosition.y = 0f; 
        lPosition.z = 80f;
        velocityY = 0; // Start with no velocity
        isFlying = false; 
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
        // Limit pitch to prevent over-rotation
        if (pitch > 90) pitch = 90;
        if (pitch < -90) pitch = -90;
    }
    
    //moves the camera forward relative to its current rotation (yaw)
    public void walkForward(float distance)
    {
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw));
        Vector3f newPos = new Vector3f(position.x - xOffset, position.y, position.z + zOffset);
        if (isFlying || checkCollision(newPos)) {
            position.x -= xOffset;
            position.z += zOffset;
        }
    }
    
    //moves the camera backward relative to its current rotation (yaw)
    public void walkBackwards(float distance)
    {   
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw));
        Vector3f newPos = new Vector3f(position.x + xOffset, position.y, position.z - zOffset);
        if (isFlying || checkCollision(newPos)) {
            position.x += xOffset;
            position.z -= zOffset;
        }
    }
    //strafes the camera left relative to its current rotation (yaw)
    public void strafeLeft(float distance)
    {
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw-90));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw-90));
        Vector3f newPos = new Vector3f(position.x - xOffset, position.y, position.z + zOffset);
        if (isFlying || checkCollision(newPos)) {
            position.x -= xOffset;
            position.z += zOffset;
        }
    }
    //strafes the camera right relative to its current rotation (yaw)
    public void strafeRight(float distance)
    {
        float xOffset = distance * (float)Math.sin(Math.toRadians(yaw+90));
        float zOffset = distance * (float)Math.cos(Math.toRadians(yaw+90));
        Vector3f newPos = new Vector3f(position.x - xOffset, position.y, position.z + zOffset);
        if (isFlying || checkCollision(newPos)) {
            position.x -= xOffset;
            position.z += zOffset;
        }
    }
    
    // Fly up (decrease Y)
    public void flyUp(float distance)
    {
        Vector3f newPos = new Vector3f(position.x, position.y - distance, position.z);
        if (isFlying || checkCollision(newPos)) {
            position.y -= distance;
            if (!isFlying) velocityY = 0;
        }
    }
    
    // Fly down (increase Y)
    public void flyDown(float distance)
    {
        Vector3f newPos = new Vector3f(position.x, position.y + distance, position.z);
        if (isFlying || checkCollision(newPos)) {
            position.y += distance;
            if (!isFlying) velocityY = 0;
        }
    }
    
    //moves the camera up (for water swimming)
    public void moveUp(float distance)
    {
        Vector3f newPos = new Vector3f(position.x, position.y - distance, position.z);
        if (checkCollision(newPos)) {
            position.y -= distance;
            velocityY = 0;
        }
    }
    
    //moves the camera down (for water swimming)
    public void moveDown(float distance)
    {
        Vector3f newPos = new Vector3f(position.x, position.y + distance, position.z);
        if (checkCollision(newPos)) {
            position.y += distance;
            velocityY = 0;
        }
    }
    
    public void applyGravity(float deltaTime) {
        // Skip gravity if flying
        if (isFlying) {
            velocityY = 0;
            isOnGround = false;
            return;
        }
        
        // Check if player is in water
        checkWaterStatus();
        
        if (isInWater) {
            // In water: float upward (decrease Y)
            velocityY -= WATER_BUOYANCY * deltaTime;
            velocityY *= WATER_DRAG;
            
            Vector3f newPos = new Vector3f(position.x, position.y + velocityY * deltaTime, position.z);
            if (checkCollision(newPos)) {
                position.y += velocityY * deltaTime;
            } else {
                velocityY = 0;
            }
        } else {
            // Normal gravity: pull downward (increase Y)
            velocityY += GRAVITY * deltaTime;
            
            Vector3f newPos = new Vector3f(position.x, position.y + velocityY * deltaTime, position.z);
            if (checkCollision(newPos)) {
                position.y += velocityY * deltaTime;
                isOnGround = false;
            } else {
                // Hit something (ground or ceiling)
                velocityY = 0;
                isOnGround = true;
            }
        }
        
        // Clamp Y position to reasonable bounds
        if (position.y > 100) position.y = 100;
        if (position.y < -100) position.y = -100;
    }
    
    private void checkWaterStatus() {
        // Check around player's position for water blocks
        float checkY = -position.y; // Convert to block coordinate space
        
        // Get block at player's feet and head positions
        Vector3f feetPos = new Vector3f(-position.x, -position.y, -position.z);
        Vector3f headPos = new Vector3f(-position.x, -position.y + PLAYER_HEIGHT, -position.z);
        
        isInWater = false;
        
        if (currentChunk.containsWorldPoint(feetPos.x, feetPos.y, feetPos.z)) {
            int[] blockPos = currentChunk.worldToBlock(feetPos.x, feetPos.y, feetPos.z);
            if (blockPos != null) {
                Block block = currentChunk.getBlock(blockPos[0], blockPos[1], blockPos[2]);
                if (block != null && block.getType() == Block.BlockType.BlockType_Water) {
                    isInWater = true;
                }
            }
        }
        
        if (!isInWater && currentChunk.containsWorldPoint(headPos.x, headPos.y, headPos.z)) {
            int[] blockPos = currentChunk.worldToBlock(headPos.x, headPos.y, headPos.z);
            if (blockPos != null) {
                Block block = currentChunk.getBlock(blockPos[0], blockPos[1], blockPos[2]);
                if (block != null && block.getType() == Block.BlockType.BlockType_Water) {
                    isInWater = true;
                }
            }
        }
    }
    
    public void jump() {
        if (isFlying) {
            // In fly mode, jump toggles flying up
            flyUp(FLY_SPEED * 0.1f);
        } else if (isOnGround && !isInWater) {
            velocityY = -JUMP_POWER; // Negative because up is negative Y
            isOnGround = false;
        } else if (isInWater) {
            // Can "swim up" in water
            velocityY = -JUMP_POWER * 0.7f;
        }
    }
    
    public void toggleFlyMode() {
        isFlying = !isFlying;
        if (isFlying) {
            velocityY = 0; // Reset velocity when entering fly mode
            System.out.println("Fly mode ON");
        } else {
            System.out.println("Fly mode OFF");
        }
    }
    
    private boolean checkCollision(Vector3f newPosition) {
        // Skip collision detection in fly mode
        if (isFlying) return true;
        
        // Check collision with blocks using a bounding box
        float halfWidth = PLAYER_WIDTH / 2;
        
        // Convert to world block coordinates (positive Y is down)
        float playerBottomY = -newPosition.y - PLAYER_HEIGHT; // Bottom of player in world coords
        float playerTopY = -newPosition.y; // Top of player in world coords
        
        // Check corners and centers of player bounding box
        Vector3f[] checkPoints = {
            // Bottom corners
            new Vector3f(-newPosition.x + halfWidth, playerBottomY, -newPosition.z + halfWidth),
            new Vector3f(-newPosition.x - halfWidth, playerBottomY, -newPosition.z + halfWidth),
            new Vector3f(-newPosition.x + halfWidth, playerBottomY, -newPosition.z - halfWidth),
            new Vector3f(-newPosition.x - halfWidth, playerBottomY, -newPosition.z - halfWidth),
            // Top corners
            new Vector3f(-newPosition.x + halfWidth, playerTopY, -newPosition.z + halfWidth),
            new Vector3f(-newPosition.x - halfWidth, playerTopY, -newPosition.z + halfWidth),
            new Vector3f(-newPosition.x + halfWidth, playerTopY, -newPosition.z - halfWidth),
            new Vector3f(-newPosition.x - halfWidth, playerTopY, -newPosition.z - halfWidth),
            // Centers for better detection
            new Vector3f(-newPosition.x, playerBottomY + PLAYER_HEIGHT/2, -newPosition.z),
            new Vector3f(-newPosition.x, playerBottomY, -newPosition.z),
            new Vector3f(-newPosition.x, playerTopY, -newPosition.z)
        };
        
        for (Vector3f point : checkPoints) {
            if (currentChunk.containsWorldPoint(point.x, point.y, point.z)) {
                int[] blockPos = currentChunk.worldToBlock(point.x, point.y, point.z);
                if (blockPos != null) {
                    Block block = currentChunk.getBlock(blockPos[0], blockPos[1], blockPos[2]);
                    if (block != null && block.isActive() && block.getType() != Block.BlockType.BlockType_Water) {
                        return false;
                    }
                }
            }
        }
        
        return true;
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
        float reach = 5.0f;
        float step = 0.2f;

        Vector3f look = getLookVector();

        // Camera position in world space (positive Y is down)
        float camX = -position.x;
        float camY = -position.y;
        float camZ = -position.z;

        for (float t = 0; t <= reach; t += step) {
            float worldX = camX + look.x * t;
            float worldY = camY + look.y * t;
            float worldZ = camZ + look.z * t;

            if (currentChunk.containsWorldPoint(worldX, worldY, worldZ)) {
                int[] blockPos = currentChunk.worldToBlock(worldX, worldY, worldZ);
                if (blockPos != null) {
                    Block block = currentChunk.getBlock(blockPos[0], blockPos[1], blockPos[2]);
                    if (block != null && block.isActive() && block.getType() != Block.BlockType.BlockType_Water) {
                        currentChunk.breakBlock(blockPos[0], blockPos[1], blockPos[2]);
                        return;
                    }
                }
            }
        }
    }
    
    private void placeBlockInFront() {
        float reach = 5.0f;
        float step = 0.2f;

        Vector3f look = getLookVector();

        float camX = -position.x;
        float camY = -position.y;
        float camZ = -position.z;
        
        float lastValidWorldX = camX;
        float lastValidWorldY = camY;
        float lastValidWorldZ = camZ;
        boolean hitBlock = false;

        for (float t = 0; t <= reach; t += step) {
            float worldX = camX + look.x * t;
            float worldY = camY + look.y * t;
            float worldZ = camZ + look.z * t;

            if (currentChunk.containsWorldPoint(worldX, worldY, worldZ)) {
                int[] blockPos = currentChunk.worldToBlock(worldX, worldY, worldZ);
                if (blockPos != null) {
                    Block block = currentChunk.getBlock(blockPos[0], blockPos[1], blockPos[2]);
                    if (block != null && block.isActive() && block.getType() != Block.BlockType.BlockType_Water) {
                        hitBlock = true;
                        break;
                    }
                }
                lastValidWorldX = worldX;
                lastValidWorldY = worldY;
                lastValidWorldZ = worldZ;
            } else {
                break;
            }
        }
        
        if (hitBlock) {
            int[] placePos = currentChunk.worldToBlock(lastValidWorldX, lastValidWorldY, lastValidWorldZ);
            if (placePos != null) {
                currentChunk.placeBlock(placePos[0], placePos[1], placePos[2], selectedBlock);
            }
        }
    }
    
    public void gameLoop()
    {
        float dx, dy;
        float dt = 0.0f;
        float lastTime = 0.0f;
        float currentTime;
        float mouseSensitivity = 0.09f;
        float movementSpeed = 5.0f;
        
        currentChunk = new Chunk(0,0,0);
        
        
        Mouse.setGrabbed(true);
        
        System.out.println("=== CONTROLS ===");
        System.out.println("F - Toggle Fly Mode");
        System.out.println("Space - Jump / Fly Up");
        System.out.println("Shift - Sprint / Fly Down");
        System.out.println("WASD - Move");
        System.out.println("Left Click - Break Block");
        System.out.println("Right Click - Place Block");
        System.out.println("1-5 - Select Placed Block Type");
        System.out.println("=================");
        
        while (!Display.isCloseRequested() &&
                !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
        {
            currentTime = Sys.getTime();
            dt = (currentTime - lastTime) / 1000.0f;
            if (dt > 0.05f) dt = 0.05f;
            lastTime = currentTime;
            
            dx = Mouse.getDX();
            dy = Mouse.getDY();
            
            float currentSpeed = movementSpeed;
            if (!isFlying && isInWater) {
                currentSpeed = movementSpeed * 0.5f;
            } else if (isFlying) {
                currentSpeed = FLY_SPEED;
            }
            
            float moveAmount = currentSpeed * dt;
            
            // Movement
            if (Keyboard.isKeyDown(Keyboard.KEY_W)) {
                walkForward(moveAmount);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
                walkBackwards(moveAmount);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
                strafeLeft(moveAmount);
            }
            if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
                strafeRight(moveAmount);
            }
            
            // Fly mode vertical movement
            if (isFlying) {
                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                    flyUp(moveAmount);
                }
                if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                    flyDown(moveAmount);
                }
            } else {
                // Normal mode controls
                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                    jump();
                }
                if (!isFlying && isInWater) {
                    if (Keyboard.isKeyDown(Keyboard.KEY_R)) {
                        moveUp(moveAmount * 2);
                    }
                    if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                        moveDown(moveAmount);
                    }
                }
            }
            
            // Sprint (only when not flying)
            if (!isFlying && Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) && !isInWater) {
                movementSpeed = 8.0f;
            } else if (!isFlying && !Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                movementSpeed = 5.0f;
            }
            
            // Toggle fly mode - check key press
            boolean fKeyDown = Keyboard.isKeyDown(Keyboard.KEY_F);
            if (fKeyDown && !FKeyWasDown) {
                toggleFlyMode();
            }
            FKeyWasDown = fKeyDown;
            
            // Block selection
            selectBlock(Keyboard.KEY_1, Block.BlockType.BlockType_Grass, "GRASS");
            selectBlock(Keyboard.KEY_2, Block.BlockType.BlockType_Dirt, "DIRT");
            selectBlock(Keyboard.KEY_3, Block.BlockType.BlockType_Sand, "SAND");
            selectBlock(Keyboard.KEY_4, Block.BlockType.BlockType_Stone, "STONE");
            selectBlock(Keyboard.KEY_5, Block.BlockType.BlockType_Default, "TNT");
            
            
            // Block breaking
            boolean leftMouseDown = Mouse.isButtonDown(0);
            if (leftMouseDown && !leftMouseWasDown) {
                breakBlockInFront();
            }
            leftMouseWasDown = leftMouseDown;
            
            // Block placing (right click)
            boolean rightMouseDown = Mouse.isButtonDown(1);
            if (rightMouseDown && !rightMouseWasDown) {
                placeBlockInFront();
            }
            rightMouseWasDown = rightMouseDown;
            
            
            // Apply gravity (if not flying)
            applyGravity(dt);
            
            // Update mouse look
            yaw(dx * mouseSensitivity);
            pitch(dy * mouseSensitivity);
            
            glLoadIdentity();
            lookThrough();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            currentChunk.render();
            Display.update();
            Display.sync(60);
        }
        Display.destroy();
    }
    
    void selectBlock(int key, Block.BlockType block, String type)
    {
        if(Keyboard.isKeyDown(key) && selectedBlock != block)
        {
            System.out.println("SELECTED BLOCK: " + type);
            selectedBlock = block;
        }
    }
}
