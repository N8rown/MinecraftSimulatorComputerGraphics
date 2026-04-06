/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ninecraftbynojang;

/**
 *
 * @author n8bro
 */
public class PerlinNoise { //Partially cnverted non java code
    private int noiseWidth = 128;
    private int noiseHeight = 128;
    private double[][] noise = new double[noiseWidth][noiseHeight];
    
    
    
   
    public void generateNoise()
    { 
        for (int x = 0; x < noiseWidth; x++){
            for (int y = 0; y < noiseHeight; y++){
                noise[x][y] = (rand() % 32768) / 32768.0;
            }
        }
    }
    public static void main(String[] args)
    {
        screen(noiseWidth, noiseHeight, 0, "Random Noise");
        generateNoise();
        ColorRGB color;
        for(int x = 0; x < w; x++){
            for(int y = 0; y < h; y++){
                color.r = color.g = color.b = Uint8(256 * noise[x][y]);
                pset(x, y, color);
            }
            redraw();
            sleep();
            return 0;
        }
    }
    
           


