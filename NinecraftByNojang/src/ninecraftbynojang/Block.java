/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ninecraftbynojang;

/**
 *
 * @author n8bro
 */
public class Block {
    private boolean isActive;
    private BlockType Type;
    private float x,y,z;
    
    public enum BlockType{
        BlockType_Grass(0),
        BlockType_Dirt(1),
        BlockType_Water(2),
        BlockType_Sand(3),
        BlockType_Stone(4),
        BlockType_Bedrock5);
        private int BlockID;
        BlockType(int i){
            BlockID = i;
        }
        public int getID()
        {
            return BlockID;
        }
        public void setID(int i){
            BlockID = i;
        }
    }
}
