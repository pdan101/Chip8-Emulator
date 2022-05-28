package chip;

import java.io.*;

public class Chip {

    private char[] memory;
    private char[] V;
    private char I;
    private char pc;

    private char stack[];
    private int stackPointer;

    private int delay_timer;
    private int sound_timer;

    private byte[] keys;

    private byte[] display;

    private boolean needRedraw;

    public void init() {
        memory = new char[4096];
        V = new char[16];
        I = 0x0;
        pc = 0x200;

        stack = new char[16];
        stackPointer = 0;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];

        display = new byte[64 * 32];

        needRedraw = false;
        loadFontset();
    }

    public void run() {
        //fetch opcode
        char opcode = (char)((memory[pc] << 8) | memory[pc + 1]);
        System.out.println(Integer.toHexString(opcode));
        //decode opcode
        switch(opcode & 0xF000){

            case 0x1000: //1NNN: Jumps to address NNN
                break;

            case 0x2000: //2NNN: Calls subroutine at NNN
                stack[stackPointer] = pc;
                stackPointer++;
                pc = (char)(opcode & 0x0FFF);
                break;

            case 0x3000: //3XNN: Skips the next instruction if VX equals NN
                break;

            case 0x6000: {//6XNN: Set VX to NN
                int x = (opcode & 0x0F00) >> 8;
                V[x] = (char) (opcode & 0x00FF);
                pc += 2;
                break;
            }
            case 0x7000: {//7XNN: Adds NN to VX
                int x = (opcode & 0x0F00) >> 8;
                V[x] = (char) ((V[x] + (opcode & 0x00FF)) & 0xFF);
                pc += 2;
                break;
            }

            case 0x8000: //Contains more data in last four bits
                switch(opcode & 0x000F){
                    case 0x0000:
                        //do something
                        break;
                    default:
                        System.err.println("Unsupported Opcode!");
                        System.exit(0);
                        break;
                }
                break;

            case 0xA000: //ANNN: Set I to NNN
                I = (char)(opcode & 0x0FFF);
                pc += 2;
                break;

            case 0xD000: {//DXYN: Draws a sprite (X, Y) size (8, N). Sprite is located at I
                //Drawing by XOR-ing to the screen
                //Check for collision and set V[0xF]
                //Read the image from I
                int x = V[(opcode & 0x0F00) >> 8];
                int y = V[(opcode & 0x00F0) >> 4];
                int height = opcode & 0x000F;

                V[0xF] = 0;

                for(int _y = 0; _y < height; _y++){
                    int line = memory[I + _y];
                    for(int _x = 0; _x < 8; _x++){
                        int pixel = line & (0x80 >> _x);
                        if(pixel != 0){
                            int totalX = x + _x;
                            int totalY = y + _y;
                            int index = totalY * 64 + totalX;

                            if(display[index] == 1){
                                V[0xF] = 1;
                            }

                            display[index] ^= 1;
                        }
                    }
                }

                pc += 2;
                needRedraw = true;
                break;
            }

            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);


        }
        //execute opcode
    }

    public byte[] getDisplay() {
        return display;
    }

    public boolean needsRedraw() {
        return needRedraw;
    }

    public void removeDrawFlag() {
        needRedraw = false;
    }

    public void loadProgram(String file) throws FileNotFoundException {
        DataInputStream input = null;
        try{
            input = new DataInputStream(new FileInputStream(new File(file)));
            int offset = 0;
            while(input.available() > 0){
                memory[0x200 + offset] = (char)(input.readByte() & 0xFF);
                offset++;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            if(input != null){
                try{
                    input.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void loadFontset() {
        for(int i = 0; i < ChipData.fontset.length; i++){
            memory[0x50 + i] = (char)(ChipData.fontset[i] & 0xFF);
        }
    }
}
