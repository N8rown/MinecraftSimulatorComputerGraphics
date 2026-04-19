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
    
    public enum BlockType {
        BlockType_Default(-1),
        BlockType_Grass(0),
        BlockType_Dirt(1),
        BlockType_Water(2),
        BlockType_Sand(3),
        BlockType_Stone(4),
        BlockType_Bedrock(5);
        private int BlockID;
        BlockType(int i){
            BlockID=i;
        }
        public int GetID()
        {
            return BlockID;
        }
        public void SetID(int i){
            BlockID = i;
        }
    }
    
    public Block(BlockType type)
    {
        Type = type;
    }
    public void setCoords(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public boolean isActive()
    {
        return isActive;
    }
    public void setActive(boolean setTo)
    {
        isActive = setTo;
    }
    public int GetID()
    {
        return Type.GetID();
    }
    public BlockType getType()
    {
        return Type;
    }
}
