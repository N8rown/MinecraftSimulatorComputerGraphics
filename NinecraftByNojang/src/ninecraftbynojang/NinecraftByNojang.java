/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ninecraftbynojang;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.util.glu.GLU;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;

/**
 *
 * @author n8bro
 */
public class NinecraftByNojang { //MAIN CLASS
    private FPcameraController fp;
    private DisplayMode displayMode;
    //Lighting Globals
    private FloatBuffer lightPosition;
    private FloatBuffer whiteLight;
    
    public void start() {
        fp = new FPcameraController(0f,-40f,0f); //Moved to start
        try {
            createWindow();
            initGL();
            fp.gameLoop();//render();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void createWindow() throws Exception{
        Display.setFullscreen(false);
        DisplayMode d[] =
        Display.getAvailableDisplayModes();
        for (int i = 0; i < d.length; i++) {
            if (d[i].getWidth() == 640
             && d[i].getHeight() == 480
             && d[i].getBitsPerPixel() == 32) 
            {
                displayMode = d[i];
                break;
            }
        }
        Display.setDisplayMode(displayMode);
        Display.setTitle("Ninecraft");
        Display.create();
    }
    
    private void initGL() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        GLU.gluPerspective(100.0f, (float)displayMode.getWidth()/(float)
        displayMode.getHeight(), 0.1f, 300.0f);
        glMatrixMode(GL_MODELVIEW);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_TEXTURE_2D);
        glEnableClientState (GL_TEXTURE_COORD_ARRAY);
        //Added Lighting code
        initLightArrays();
        glLight(GL_LIGHT0, GL_POSITION, lightPosition); //sets our light’s position
        glLight(GL_LIGHT0, GL_SPECULAR, whiteLight);//sets our specular light
        glLight(GL_LIGHT0, GL_DIFFUSE, whiteLight);//sets our diffuse light
        glLight(GL_LIGHT0, GL_AMBIENT, whiteLight);//sets our ambient light
        glEnable(GL_LIGHTING);//enables our lighting
        glEnable(GL_LIGHT0);//enables light0

    }
    private void initLightArrays() {
        lightPosition = BufferUtils.createFloatBuffer(4);
        lightPosition.put(0.0f).put(-40.0f).put(0.0f).put(1.0f).flip();
        whiteLight = BufferUtils.createFloatBuffer(4);
        whiteLight.put(1.0f).put(1.0f).put(1.0f).put(0.0f).flip();
    }

    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        NinecraftByNojang basic = new NinecraftByNojang();
        basic.start();
    }
    
}
