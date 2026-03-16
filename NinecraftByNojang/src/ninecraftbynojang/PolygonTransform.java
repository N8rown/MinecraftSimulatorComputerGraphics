/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package ninecraftbynojang;
import java.io.InputStream;
import java.util.Scanner;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.input.Keyboard;
import static org.lwjgl.opengl.GL11.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author n8bro
 */
//Basic Point data structure
class Vertex {
    float x, y;
    Vertex(float x, float y) { this.x = x; this.y = y; }
}

//Basic Polygon Structure
class Polygon {
    private final List<Vertex> original; // original points
    private final float[][] transform;   // 3x3 transform matrix
    float r, g, b;

    Polygon() {
        original = new ArrayList<>();
        r = g = b = 1;
        transform = new float[][] {
            {1, 0, 0},
            {0, 1, 0},
            {0, 0, 1}
        };
    }

    // Add a vertex (in object space)
    void addVertex(float x, float y) {
        original.add(new Vertex(x, y));
    }

    // Apply a new transform by post-multiplying the current transform
    void scale(float sx, float sy) {
        multSelf(new float[][] {
            {sx, 0, 0},
            {0, sy, 0},
            {0, 0, 1}
        });
    }

    void translate(float tx, float ty) {
        multSelf(new float[][] {
            {1, 0, tx},
            {0, 1, ty},
            {0, 0, 1}
        });
    }

    void rotate(float radians) {
        float c = (float) Math.cos(radians);
        float s = (float) Math.sin(radians);
        multSelf(new float[][] {
            { c, -s, 0},
            { s,  c, 0},
            { 0,  0, 1}
        });
    }
    void rotateAbout(float radians, float cx, float cy) {
        translate(cx, cy);
        rotate(radians);
        translate(-cx, -cy);
    }

    void scaleAbout(float sx, float sy, float cx, float cy) {
        translate(cx, cy);
        scale(sx, sy);
        translate(-cx, -cy);
    }
    
    // transform = m * transform
    private void multSelf(float[][] m) {
        float[][] a = m;         // new
        float[][] b = transform; // current
        float[][] r = new float[3][3];

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i][j] = a[i][0] * b[0][j] + a[i][1] * b[1][j] + a[i][2] * b[2][j];
            }
        }
        for (int i = 0; i < 3; i++) {
            System.arraycopy(r[i], 0, transform[i], 0, 3);
        }
    }

    // Get transformed vertices (as flat array [x0,y0,x1,y1,...])
    float[] getTransformedVertices() {
        float[] out = new float[original.size() * 2];
        int idx = 0;
        for (Vertex v : original) {
            float x = v.x;
            float y = v.y;
            // Apply transform: [x', y', w]' = M * [x, y, 1]
            float xp = transform[0][0] * x + transform[0][1] * y + transform[0][2] * 1f;
            float yp = transform[1][0] * x + transform[1][1] * y + transform[1][2] * 1f;
            // w is always 1 for affine, but compute if needed
            out[idx++] = xp;
            out[idx++] = yp;
        }
        return out;
    }

    // Optional: reset to identity
    void resetTransform() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                transform[i][j] = (i == j) ? 1f : 0f;
            }
        }
    }
}
public class PolygonTransform {
ArrayList<Polygon> shapes = new ArrayList();

    /**
     * Entry point for initializing the window, OpenGL state,
     * and starting the render loop.
     */
    public void start() {
        try {
            createWindow();
            initGL();
            render();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates and initializes the display window.
     *
     * @throws Exception if the display cannot be created
     */
    private void createWindow() throws Exception {
        Display.setFullscreen(false);
        Display.setDisplayMode(new DisplayMode(640, 480));
        Display.setTitle("CS4450 Assignment1");
        Display.create();
    }

    /**
     * Initializes OpenGL settings including background color
     * and a 2D orthographic projection.
     */
    private void initGL() {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();

        // Set up a 2D coordinate system matching window size
        int width = 640;
        int height = 480;

        glOrtho(-width/2f, width/2f, -height/2f, height/2f, 1, -1);

        glMatrixMode(GL_MODELVIEW);
        glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
    }

    public void loadFile(String name) throws Exception {
        InputStream in = PolygonTransform.class.getResourceAsStream(name);
        if (in == null) throw new RuntimeException("Could not find resource: " + name);

        Scanner inputFile = new Scanner(in);
        inputFile.useDelimiter("[,\\s]+");

        float x, y;
        String buffered = null;

        while (true) {
            String token;

            if (buffered != null) {
                token = buffered;
                buffered = null;
            } else {
                if (!inputFile.hasNext()) break;
                token = inputFile.next();
            }

            if (token.isBlank()) continue;
            if (!token.equals("P")) continue;

            Polygon shape = new Polygon();
            shape.r = Float.parseFloat(inputFile.next());
            shape.g = Float.parseFloat(inputFile.next());
            shape.b = Float.parseFloat(inputFile.next());

            // vertices until T
            while (inputFile.hasNext()) {
                token = inputFile.next();
                if (token.equals("T")) break;
                if (token.equals("P")) break;

                x = Float.parseFloat(token);
                y = Float.parseFloat(inputFile.next());
                shape.addVertex(x, y);
            }

            // transforms until next P or EOF
            while (inputFile.hasNext()) {
                token = inputFile.next();

                if (token.equals("P")) {
                    buffered = token; // buffer the P we just read
                    break;
                }

                if (token.equals("r")) {
                    float angleDeg = Float.parseFloat(inputFile.next());
                    float cx = Float.parseFloat(inputFile.next()); //x to rotate around
                    float cy = Float.parseFloat(inputFile.next());//y to rotate around
                    shape.rotateAbout((float) Math.toRadians(angleDeg), cx, cy);
                } else if (token.equals("s")) {
                    float sx = Float.parseFloat(inputFile.next());
                    float sy = Float.parseFloat(inputFile.next());
                    float cx = Float.parseFloat(inputFile.next());
                    float cy = Float.parseFloat(inputFile.next());
                    shape.scaleAbout(sx, sy, cx, cy);
                } else if (token.equals("t")) {
                    float tx = Float.parseFloat(inputFile.next());
                    float ty = Float.parseFloat(inputFile.next());
                    shape.translate(tx, ty);
                }
            }
        shapes.add(shape);
        }
    inputFile.close();
    }

    public void loadShapes()
    {
        float x0, y0, x1, y1;
        for (Polygon shape: shapes)
        {
            glColor3f(shape.r, shape.g, shape.b);
            float[] points = shape.getTransformedVertices();
            fillPolygon(points);
            for(int i = 0; i  < points.length; i+=2)
            {
                int j = (i + 2) % points.length;  // next vertex (wrap to 0 at end)

                x0 = points[i];
                y0 = points[i + 1];
                x1 = points[j];
                y1 = points[j + 1];
                drawLine(x0, y0, x1, y1);
            }
            
        }
    }
    /**
     * Main rendering loop .
     * <p>
     * Continuously clears the screen and renders shapes
     * until the user closes the window with q.
     * </p>
     */
    public void render() {
    try {
        loadFile("/coordinates.txt"); // load once
    } catch (Exception e) {
        e.printStackTrace();
        return;
    }

    while (!Display.isCloseRequested()) {
        if (Keyboard.isKeyDown(Keyboard.KEY_Q)) break;

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glLoadIdentity();

        glPointSize(1);
        loadShapes();

        Display.update();
        Display.sync(60);
    }

    Keyboard.destroy();
    Display.destroy();
}


    /**
     * Draws a line between two points using Bresenham's line algorithm.
     * <p>
     * This implementation assumes a positive slope with x2 &gt; x1.
     * </p>
     *
     * @param x1 starting x-coordinate
     * @param y1 starting y-coordinate
     * @param x2 ending x-coordinate
     * @param y2 ending y-coordinate
     */
    public void drawLine(float fx0, float fy0, float fx1, float fy1) {
        // Convert once (pixel grid)
        int x0 = Math.round(fx0);
        int y0 = Math.round(fy0);
        int x1 = Math.round(fx1);
        int y1 = Math.round(fy1);
        glBegin(GL_POINTS);
        
        int t;
        boolean isSteep = Math.abs(y1 - y0) > Math.abs(x1 - x0);
        if (isSteep) { // swap x and y
            t = x0; x0 = y0; y0 = t;
            t = x1; x1 = y1; y1 = t;
        }
     
        // ensure we draw left -> right
        if (x0 > x1) { //smaller x comes first first
            t = x0; x0 = x1; x1 = t;
            t = y0; y0 = y1; y1 = t;
        }
        
        float currentX, currentY, dX, dY, D, right, upRight;
        dX = Math.abs(x1 - x0);
        dY = Math.abs(y1 - y0);
        
        currentX = x0;
        currentY = y0;
        
        int sy = (y1 > y0) ? 1 : -1; //slope positive or negative


        D = 2 * dY - dX;
        right = 2 * dY;
        upRight = 2 * (dY - dX);

        do { //Accomodates the first point 
            if(isSteep)
                 glVertex2f(currentY, currentX);
            else 
                glVertex2f(currentX, currentY);
            
            if (D > 0) {
                D += upRight;
                currentX++;
                currentY+= sy;
            } else {
                D += right;
                currentX++;
            }
        } while (currentX <= x1);

        glEnd();
    }

    /**
     * Draws a circle using parametric equations with sine and cosine.
     *
     * @param centerX x-coordinate of the circle center
     * @param centerY y-coordinate of the circle center
     * @param radius radius of the circle
     */
    public void drawCircle(int centerX, int centerY, int radius) {
        glBegin(GL_POINTS);

        double maxTheta = 2.0 * Math.PI;
        double step = 0.01;

        for (double theta = 0; theta < maxTheta; theta += step) {
            int x = centerX + (int) (Math.cos(theta) * radius);
            int y = centerY + (int) (Math.sin(theta) * radius);
            glVertex2f(x, y);
        }

        glEnd();
    }

    /**
     * Draws an ellipse
     *
     * @param centerX x-coordinate of the ellipse center
     * @param centerY y-coordinate of the ellipse center
     * @param radiusX horizontal radius
     * @param radiusY vertical radius
     */
    public void drawEllipse(int centerX, int centerY, int radiusX, int radiusY) {
        glBegin(GL_POINTS);

        double maxTheta = 2.0 * Math.PI;
        double step = 0.01;

        for (double theta = 0; theta < maxTheta; theta += step) {
            int x = centerX + (int) (Math.cos(theta) * radiusX);
            int y = centerY + (int) (Math.sin(theta) * radiusY);
            glVertex2f(x, y);
        }

        glEnd();
    }
    public void fillPolygon(float[] points) {
    if (points.length < 6) return; // need at least 3 vertices

    // Find Y bounds
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;

    for (int i = 1; i < points.length; i += 2) {
        int y = Math.round(points[i]);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
    }

    // For each scanline
    for (int y = minY; y <= maxY; y++) {

        ArrayList<Integer> intersections = new ArrayList<>();

        // Check every edge
        for (int i = 0; i < points.length; i += 2) {

            int j = (i + 2) % points.length;

            int x0 = Math.round(points[i]);
            int y0 = Math.round(points[i + 1]);
            int x1 = Math.round(points[j]);
            int y1 = Math.round(points[j + 1]);

            // Ignore horizontal edges
            if (y0 == y1) continue;

            // Check if scanline crosses edge
            if ((y >= Math.min(y0, y1)) && (y < Math.max(y0, y1))) {

                float xIntersect =
                        x0 + (float)(y - y0) * (x1 - x0) / (float)(y1 - y0);

                intersections.add(Math.round(xIntersect));
            }
        }

        // Sort intersections left → right
        intersections.sort(Integer::compare);

        // Fill between pairs
        for (int i = 0; i < intersections.size(); i += 2) {
            if (i + 1 < intersections.size()) {
                drawLine(
                    intersections.get(i), y,
                    intersections.get(i + 1), y
                );
            }
        }
    }
}
    
    public static void main(String[] args) {
        PolygonTransform basicObject = new PolygonTransform();
        basicObject.start();
        
    }
}
