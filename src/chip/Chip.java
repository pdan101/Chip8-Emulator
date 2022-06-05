package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

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
        memory= new char[4096];
        V= new char[16];
        I= 0x0;
        pc= 0x200;

        stack= new char[16];
        stackPointer= 0;

        delay_timer= 0;
        sound_timer= 0;

        keys= new byte[16];

        display= new byte[64 * 32];

        needRedraw= false;
        loadFontset();
    }

    public void run() {
        // fetch opcode
        char opcode= (char) (memory[pc] << 8 | memory[pc + 1]);
        System.out.println(Integer.toHexString(opcode));
        // decode opcode
        switch (opcode & 0xF000) {

        case 0x0000: {
            switch (opcode & 0x00FF) {
            case 0x00E0: { // Clear screen
                for (int i= 0; i < display.length; i++ ) {
                    display[i]= 0;
                }
                pc+= 2;
                needRedraw= true;
                break;
            }

            case 0x00EE: { // Returns from subroutine
                stackPointer-- ;
                pc= (char) (stack[stackPointer] + 2);
                break;
            }

            default: // 0NNN
                System.exit(0);
                break;
            }
            break;
        }

        case 0x1000: { // 1NNN: Jumps to address NNN
            int nnn= opcode & 0x0FFF;
            pc= (char) nnn;
            break;
        }

        case 0x2000: // 2NNN: Calls subroutine at NNN
            stack[stackPointer]= pc;
            stackPointer++ ;
            pc= (char) (opcode & 0x0FFF);
            break;

        case 0x3000: {// 3XNN: Skips the next instruction if VX equals NN
            int x= (opcode & 0x0F00) >> 8;
            int nn= opcode & 0x00FF;
            if (V[x] == nn) {
                pc+= 4;
                // System.out.println("Skipping next instruction (V[" + x +"] == " + nn + ")");
            } else {
                pc+= 2;
                // System.out.println("Not skipping next instruction (V[" + x +"] != " + nn + ")");
            }
            break;
        }

        case 0x4000: { // 4XNN Skip next instruction if VX != NN
            int x= (opcode & 0x0F00) >> 8;
            int nn= opcode & 0x00FF;
            if (V[x] != nn) {
                pc+= 4;
                // System.out.println("Skipping next instruction (V[" + x +"] != " + nn + ")");
            } else {
                pc+= 2;
                // System.out.println("Not skipping next instruction (V[" + x +"] == " + nn + ")");
            }
            break;
        }

        case 0x5000: { // 5XY0 Skip next instruction if VX = VY
            int x= (opcode & 0x0F00) >> 8;
            int y= (opcode & 0x00F0) >> 4;
            if (V[x] == V[y]) {
                pc+= 4;
            } else {
                pc+= 2;
            }
            break;
        }

        case 0x6000: {// 6XNN: Set VX to NN
            int x= (opcode & 0x0F00) >> 8;
            V[x]= (char) (opcode & 0x00FF);
            pc+= 2;
            break;
        }
        case 0x7000: {// 7XNN: Adds NN to VX
            int x= (opcode & 0x0F00) >> 8;
            V[x]= (char) (V[x] + (opcode & 0x00FF) & 0xFF);
            pc+= 2;
            break;
        }

        case 0x8000: // Contains more data in last four bits
            switch (opcode & 0x000F) {
            case 0x0000: { // 8XY0 Set VX = VY
                int x= (opcode & 0x0F00) >> 8;
                int y= (opcode & 0x00F0) >> 4;
                V[x]= V[y];
                pc+= 2;
                break;
            }

            case 0x0001: { // 8XY1 Set VX = VX OR VY
                int x= (opcode & 0x0F00) >> 8;
                int y= (opcode & 0x00F0) >> 4;
                V[x]= (char) ((V[x] | V[y]) & 0xFF);
                pc+= 2;
                break;
            }

            case 0x0002: { // 8XY2 Set VX = VX AND VY
                int x= (opcode & 0x0F00) >> 8;
                int y= (opcode & 0x00F0) >> 4;
                V[x]= (char) (V[x] & V[y]);
                pc+= 2;
                break;
            }

            case 0x0003: { // 8XY3 Set VX = VX XOR VY
                int x= (opcode & 0x0F00) >> 8;
                int y= (opcode & 0x00F0) >> 4;
                V[x]= (char) ((V[x] ^ V[y]) & 0xFF);
                pc+= 2;
                break;
            }

            case 0x0004: { // 8XY4 Set VX = VX + VY, set VF = carry
                int x= (opcode & 0x0F00) >> 8;
                int y= (opcode & 0x00F0) >> 4;
                if (V[y] > 0xFF - V[x]) {
                    V[0xF]= 1;
                } else {
                    V[0xF]= 0;
                }
                V[x]= (char) (V[x] + V[y] & 0xFF);
                pc+= 2;
                break;
            }

            case 0x0005: { // 8XY5 Set VX = VX - VY, set VF = NOT borrow
                int x= (opcode & 0x0F00) >> 8;
                int y= (opcode & 0x00F0) >> 4;
                if (V[x] > V[y]) {
                    V[0xF]= 1;
                } else {
                    V[0xF]= 0;
                }
                V[x]= (char) (V[x] - V[y] & 0xFF);
                pc+= 2;
                break;
            }

            case 0x0006: { // 8XY6 Set VX = VX SHR 1
                int x= (opcode & 0x0F00) >> 8;
                if (V[x] % 2 == 1) {
                    V[0xF]= 1;
                } else {
                    V[0xF]= 0;
                }
                V[x]= (char) (V[x] >> 1);
                pc+= 2;
                break;
            }

            case 0x0007: { // 8XY7 Set VX = VY - VX, set VF = NOT borrow, set VF = NOT borrow
                int x= (opcode & 0x0F00) >> 8;
                int y= (opcode & 0x00F0) >> 4;
                if (V[y] > V[x]) {
                    V[0xF]= 1;
                } else {
                    V[0xF]= 0;
                }
                V[x]= (char) (V[y] - V[x] & 0xFF);
                pc+= 2;
                break;
            }

            case 0x000E: { // 8XYE Set VX = VX SHL 1
                int x= (opcode & 0x0F00) >> 8;
                V[0xF]= (char) (V[x] & 0x80);
                V[x]= (char) (V[x] << 1);
                pc+= 2;
                break;
            }

            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);
                break;
            }
            break;

        case 0x9000: { // 9XY0 Skip next instruction if VX != VY
            int x= (opcode & 0x0F00) >> 8;
            int y= (opcode & 0x00F0) >> 4;
            if (V[x] != V[y]) {
                pc+= 4;
            } else {
                pc+= 2;
            }
            break;
        }

        case 0xA000: // ANNN: Set I to NNN
            I= (char) (opcode & 0x0FFF);
            pc+= 2;
            break;

        case 0xB000: { // BNNN: Jump to location NNN + V[0]
            int nnn= opcode & 0x0FFF;
            pc= (char) (nnn + (V[0] & 0x00FF));
            break;
        }

        case 0xC000: { // CXNN Set VX to random number (b/w 0-255) ANDed with NN
            int x= (opcode & 0x0F00) >> 8;
            int nn= opcode & 0x00FF;
            // int random = (int) (Math.random() * 256) & nn;
            int random= new Random().nextInt(255) & nn;
            V[x]= (char) random;
            pc+= 2;
            break;
        }

        case 0xD000: {// DXYN: Draws a sprite (X, Y) size (8, N). Sprite is located at I
            // Drawing by XOR-ing to the screen
            // Check for collision and set V[0xF]
            // Read the image from I
            int x= V[(opcode & 0x0F00) >> 8];
            int y= V[(opcode & 0x00F0) >> 4];
            int height= opcode & 0x000F;

            V[0xF]= 0;

            for (int _y= 0; _y < height; _y++ ) {
                int line= memory[I + _y];
                for (int _x= 0; _x < 8; _x++ ) {
                    int pixel= line & 0x80 >> _x;
                    if (pixel != 0) {
                        int totalX= x + _x;
                        int totalY= y + _y;

                        totalX= totalX % 64;
                        totalY= totalY % 32;

                        int index= totalY * 64 + totalX;

                        if (display[index] == 1) {
                            V[0xF]= 1;
                        }

                        display[index]^= 1;
                    }
                }
            }

            pc+= 2;
            needRedraw= true;
            break;
        }

        case 0xE000: {
            switch (opcode & 0x00FF) {
            case 0x009E: { // EX9E Skip next instruction if key with the value of VX is pressed
                int x= (opcode & 0x0F00) >> 8;
                if (keys[V[x]] == 1) {
                    pc+= 4;
                } else {
                    pc+= 2;
                }
                break;
            }

            case 0x00A1: { // EXA1 Skip next instruction if key with the value of VX is not pressed
                int x= (opcode & 0x0F00) >> 8;
                if (keys[V[x]] == 0) {
                    pc+= 4;
                } else {
                    pc+= 2;
                }
                break;
            }

            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);
            }
            break;
        }

        case 0xF000: {
            switch (opcode & 0x00FF) {

            case 0x0007: { // FX07 Set VX to delay timer value
                int x= (opcode & 0x0F00) >> 8;
                V[x]= (char) delay_timer;
                pc+= 2;
                break;
            }

            case 0x000A: { // Fx07 Wait for a key press, store the value of the key in VX
                int x= (opcode & 0x0F00) >> 8;
                for (int i= 0; i < keys.length; i++ ) {
                    if (keys[i] == 1) {
                        V[x]= (char) i;
                        pc+= 2;
                        break;
                    }
                }
                break;
            }

            case 0x0015: { // FX15 Set delay timer to V[x]
                int x= (opcode & 0x0F00) >> 8;
                delay_timer= V[x];
                pc+= 2;
                break;
            }

            case 0x0018: { // FX18 Set sound timer = VX
                int x= (opcode & 0x0F00) >> 8;
                sound_timer= V[x];
                pc+= 2;
                break;
            }

            case 0x001E: { // FX1E Set I = I + VX
                int x= (opcode & 0x0F00) >> 8;
                I= (char) (I + V[x]);
                pc+= 2;
                break;
            }

            case 0x0029: { // FX29 Sets I to the location of the sprite for the character VX
                           // (fontset)
                int x= (opcode & 0x0F00) >> 8;
                int character= V[x];
                I= (char) (0x0050 + character * 5);
                pc+= 2;
                break;
            }

            case 0x0033: { // FX33 Store a binary-coded decimal value VX in I, I+1, and I+2
                int vx= V[(opcode & 0x0F00) >> 8];
                int hundreds= (vx - vx % 100) / 100;
                vx-= hundreds * 100;
                int tens= (vx - vx % 10) / 10;
                vx-= tens * 10;
                int ones= vx;
                memory[I]= (char) hundreds;
                memory[I + 1]= (char) tens;
                memory[I + 2]= (char) ones;
                pc+= 2;
                break;
            }

            case 0x0055: { // FX55 Store registers V0 through VX in memory starting at location I
                int x= (opcode & 0x0F00) >> 8;
                for (int i= 0; i <= x; i++ ) {
                    memory[I + i]= V[i];
                }
                pc+= 2;
                break;
            }

            case 0x0065: { // FX65 Fills V0 to VX with values from I
                int x= (opcode & 0x0F00) >> 8;
                for (int i= 0; i <= x; i++ ) {
                    V[i]= memory[I + i];
                }
                I= (char) (I + x + 1);
                pc+= 2;
                break;
            }

            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);
            }
            break;
        }

        default:
            System.err.println("Unsupported Opcode!");
            System.exit(0);

        }
        // execute opcode
        if (sound_timer > 0)
            sound_timer-- ;
        if (delay_timer > 0)
            delay_timer-- ;
    }

    public byte[] getDisplay() {
        return display;
    }

    public boolean needsRedraw() {
        return needRedraw;
    }

    public void removeDrawFlag() {
        needRedraw= false;
    }

    public void loadProgram(String file) throws FileNotFoundException {
        DataInputStream input= null;
        try {
            input= new DataInputStream(new FileInputStream(new File(file)));
            int offset= 0;
            while (input.available() > 0) {
                memory[0x200 + offset]= (char) (input.readByte() & 0xFF);
                offset++ ;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void loadFontset() {
        for (int i= 0; i < ChipData.fontset.length; i++ ) {
            memory[0x50 + i]= (char) (ChipData.fontset[i] & 0xFF);
        }
    }

    public void setKeyBuffer(int[] keyBuffer) {
        for (int i= 0; i < keys.length; i++ ) {
            keys[i]= (byte) keyBuffer[i];
        }
    }
}
