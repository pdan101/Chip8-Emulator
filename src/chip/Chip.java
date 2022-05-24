package chip;

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
    }

    public void run() {
        //fetch opcode
        char opcode = (char)((memory[pc] << 8) | memory[pc + 1]);
        System.out.println(Integer.toHexString(opcode));
        //decode opcode
        switch(opcode & 0xF000){

            case 0x8000:
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
            default:
                System.err.println("Unsupported Opcode!");
                System.exit(0);
        }
        //execute opcode
    }

    public byte[] getDisplay() {
        return display;
    }
}
